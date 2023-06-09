import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * [FEATURE INFO]<br/>
 * condition of a task
 *
 * @author Riicarus
 * @create 2023-6-1 2:34
 * @since 1.0.0
 */
public class TaskCondition<T> {

    private final String name;

    private ConditionType type;

    private PrevTaskFunction prevFunc;

    private final Task<T> task;

    private final TaskCallback<T> callback;

    private final HashSet<TaskCondition<?>> prevConditions = new HashSet<>();

    private final AtomicInteger notArrivedCount = new AtomicInteger();

    private final Semaphore canWork = new Semaphore(0);

    private final Lock joinLock = new ReentrantLock();

    private volatile boolean executed = false;

    /**
     * The thread which is executing execute() method, may be block by canWork.acquire() and will be set to null when executed.
     */
    private volatile Thread currentThread;

    private final SaggioTask saggioTask;

    private boolean useCustomizedTimeout = false;

    private int timeout = 3000;

    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    public TaskCondition(String name, Task<T> task, TaskCallback<T> callback, SaggioTask saggioTask) {
        this.name = name + "-" + saggioTask.generateId();
        this.task = task;
        this.callback = callback;
        this.saggioTask = saggioTask;
    }

    public TaskCondition<?> fromAny(TaskCondition<?> prev, String state) {
        if (type == null) {
            type = ConditionType.ANY;
        } else if (type.equals(ConditionType.AND)) {
            throw new RuntimeException("Type is already set to 'AND', can not add condition of type 'ANY', condition: " + this);
        }

        saggioTask.getPushDownTable().add(prev, state, this);
        this.addPrevCondition(prev);

        return this;
    }

    public TaskCondition<?> fromAnd(TaskCondition<?> prev, String state) {
        if (type == null) {
            type = ConditionType.AND;
        } else if (type.equals(ConditionType.ANY)) {
            throw new RuntimeException("Type is already set to 'ANY', can not add condition of type 'AND', condition: " + this);
        }

        saggioTask.getPushDownTable().add(prev, state, this);
        this.addPrevCondition(prev);

        return this;
    }

    protected void begin(ThreadPoolExecutor executor, TaskContext context) {
        canWork.release();

        execute(executor, context, true);
    }

    protected void execute(ThreadPoolExecutor executor, TaskContext context) {
        execute(executor, context, false);
    }

    protected void execute(ThreadPoolExecutor executor, TaskContext context, boolean isBegin) {
        currentThread = Thread.currentThread();

        if (!isBegin) {
            joinLock.unlock();
        }

        if (!waitToWork(context)) {
            return;
        }

        // if one any arrived, then other prev any conditions are not needed to be executed.
        stopPrevAny(context);

        TaskResult<T> result = doExecute(context);

        next(result.getState(), executor, context);
    }

    private boolean waitToWork(TaskContext context) {
        int _timeout = useCustomizedTimeout ? timeout : context.getConfig().getTimeout();
        TimeUnit _timeUnit = useCustomizedTimeout ? timeUnit : context.getConfig().getTimeUnit();

        try {
            if (!canWork.tryAcquire(_timeout, _timeUnit)) {
                // if acquire timeout
                System.out.println("Condition[" + this + "] acquiring semaphore has been stopped, caused by: timeout--" + _timeout + " " + _timeUnit);

                // if the destination condition is waiting out of time, the prev conditions are not needed to be executed.
                stopPrevAnd(context);
                stopPrevAny(context);
                tryStopAfterPrev(context);
                return false;
            }

            return true;
        } catch (InterruptedException e) {
            System.out.println("Condition[" + this + "] acquiring semaphore has been interrupted, caused by: " + e.getCause());

            // if the destination condition is waiting interrupted, the prev conditions are not needed to be executed.
            stopPrevAnd(context);
            stopPrevAny(context);
            tryStopAfterPrev(context);
            currentThread = null;
            return false;
        }
    }

    private TaskResult<T> doExecute(TaskContext context) {
        if (prevFunc != null) {
            prevFunc.execute(context);
        }

        TaskResult<T> result;
        try {
            result = task.execute(context);
            callback.execute(result, context);
            currentThread = null;
            executed = true;

            return result;
        } catch (InterruptedException e) {
            System.out.println("Executing condition[" + this + "] has been interrupted, caused by: " + e.getCause());

            // if the destination condition is executing interrupted, the prev conditions are not needed to be executed.
            stopPrevAnd(context);
            stopPrevAny(context);
            tryStopAfterPrev(context);

            currentThread = null;
            return new TaskResult<>(null, null);
        }

    }

    private void next(String state, ThreadPoolExecutor executor, TaskContext context) {
        HashSet<TaskCondition<?>> nextConditions = saggioTask.getPushDownTable().getNextConditions(this, state);
        if (nextConditions == null || nextConditions.isEmpty()) {
            return;
        }

        executedAllNext(nextConditions, executor, context);
    }

    private void executedAllNext(HashSet<TaskCondition<?>> nextConditions, ThreadPoolExecutor executor, TaskContext context) {
        for (TaskCondition<?> nextCondition : nextConditions) {
            executor.execute(() -> nextCondition.join(this, executor, context));
        }
    }

    private void join(TaskCondition<?> prevCondition, ThreadPoolExecutor executor, TaskContext context) {
        boolean isBoot = false;

        // optimize
        if (isExecuted() || (isExecuting() && ConditionType.ANY.equals(type))) {
            return;
        }

        try {
            joinLock.lock();

            if (isExecuted()) {
                return;
            }

            // if the condition is not executed
            if (!isExecuting()) {
                isBoot = true;
                notArrivedCount.set(prevConditions.size());

                if (ConditionType.ANY.equals(type)) {
                    canWork.release();
                } else if (ConditionType.AND.equals(type)) {
                    if (notArrivedCount.decrementAndGet() == 0) {
                        canWork.release();
                    }
                }
                execute(executor, context);
            } else {
                if (ConditionType.AND.equals(type)) {
                    if (notArrivedCount.decrementAndGet() == 0) {
                        canWork.release();
                    }
                }
            }
        } finally {
            if (!isBoot) {
                joinLock.unlock();
            }
        }
    }

    private void stopPrevAny(TaskContext context) {
        if (!context.getConfig().isStopIfNextStopped()) {
            return;
        }

        if (ConditionType.ANY.equals(type)) {
            for (TaskCondition<?> prevCondition : prevConditions) {
                if (prevCondition.isExecuting()) {
                    try {
                        System.out.println("Executing condition[" + this + "] has been interrupted, caused by: stopPrevAny()");
                        prevCondition.getCurrentThread().interrupt();
                    } catch (NullPointerException ignored) {
                    }
                }
            }
        }
    }

    private void stopPrevAnd(TaskContext context) {
        if (!context.getConfig().isStopIfNextStopped()) {
            return;
        }

        if (ConditionType.AND.equals(type)) {
            for (TaskCondition<?> prevCondition : prevConditions) {
                if (prevCondition.isExecuting()) {
                    try {
                        System.out.println("Executing condition[" + this + "] has been interrupted, caused by: stopPrevAnd()");
                        prevCondition.getCurrentThread().interrupt();
                    } catch (NullPointerException ignored) {
                    }
                }
            }
        }
    }

    private void tryStopAfterPrev(TaskContext context) {
        if (!context.getConfig().isStopIfNextStopped()) {
            return;
        }

        final HashMap<String, HashSet<TaskCondition<?>>> nextConditions = saggioTask.getPushDownTable().getNextConditions(this);
        if (nextConditions == null) {
            return;
        }

        for (HashSet<TaskCondition<?>> conditions : nextConditions.values()) {
            for (TaskCondition<?> condition : conditions) {
                if (ConditionType.AND.equals(condition.getType())) {
                    if (condition.isExecuting()) {
                        try {
                            System.out.println("Executing condition[" + this + "] has been interrupted, caused by: tryStopAfterPrev()");
                            condition.getCurrentThread().interrupt();
                        } catch (NullPointerException ignored) {
                        }
                    }
                }

                if (ConditionType.ANY.equals(condition.getType())) {
                    if (condition.isExecuting() && condition.notArrivedCount.get() == 1) {
                        // if only current task has not arrived, stop its next task
                        try {
                            System.out.println("Executing condition[" + this + "] has been interrupted, caused by: tryStopAfterPrev()");
                            condition.getCurrentThread().interrupt();
                        } catch (NullPointerException ignored) {
                        }
                    }
                }
            }
        }
    }

    public String getName() {
        return name;
    }

    public ConditionType getType() {
        return type;
    }

    public void setPrevFunc(PrevTaskFunction prevFunc) {
        this.prevFunc = prevFunc;
    }

    protected PrevTaskFunction getPrevFunc() {
        return prevFunc;
    }

    protected Task<T> getTask() {
        return task;
    }

    protected TaskCallback<T> getCallback() {
        return callback;
    }

    public HashSet<TaskCondition<?>> getPrevConditions() {
        return prevConditions;
    }

    public boolean isExecuted() {
        return executed;
    }

    public boolean isExecuting() {
        return currentThread != null;
    }

    public Thread getCurrentThread() {
        return currentThread;
    }

    private void addPrevCondition(TaskCondition<?>... conditions) {
        prevConditions.addAll(Arrays.stream(conditions).toList());
    }

    public int getTimeout() {
        return timeout;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeout(int timeout, TimeUnit timeUnit) {
        this.timeout = timeout;
        this.timeUnit = timeUnit;
        this.useCustomizedTimeout = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskCondition<?> that = (TaskCondition<?>) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "TaskCondition{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", executing=" + isExecuting() +
                ", executed=" + executed +
                '}';
    }
}
