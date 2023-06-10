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
 * task
 *
 * @author Riicarus
 * @create 2023-6-1 2:34
 * @since 1.0.0
 */
public class Task<T> {

    // core attributes
    private final String name;
    private TaskType type;
    private final HashSet<Task<?>> prevTasks = new HashSet<>();

    // task's executable function interfaces
    private PrevTaskFunction prevFunc;
    private final TaskFunction<T> taskFunc;
    private final TaskCallback<T> callback;

    // locks and concurrent-working arrangement attributes
    private final Semaphore canWork = new Semaphore(0);
    private final Lock joinLock = new ReentrantLock();
    private final AtomicInteger notArrivedCount = new AtomicInteger();
    private volatile boolean executed = false;
    /**
     * The thread which is executing execute() method, may be block by canWork.acquire() and will be set to null when executed.
     */
    private volatile Thread currentThread;

    // push-down table maintainer
    private final SaggioTask saggioTask;

    // customized setting attributes
    private boolean useCustomizedTimeout = false;
    private int timeout = 3000;
    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    public Task(String name, TaskFunction<T> taskFunc, TaskCallback<T> callback, SaggioTask saggioTask) {
        this.name = name + "-" + saggioTask.generateId();
        this.taskFunc = taskFunc;
        this.callback = callback;
        this.saggioTask = saggioTask;
    }

    /**
     * link previous task to current one, meaning that if any previous tasks executed successfully and returned the given state, current task will be executed.
     *
     * @param prev previous task
     * @param state transfer state
     * @return current task
     */
    public Task<?> fromAny(Task<?> prev, String state) {
        if (type == null) {
            type = TaskType.ANY;
        } else if (type.equals(TaskType.AND)) {
            throw new RuntimeException("Type is already set to 'AND', can not add task of type 'ANY', task: " + this);
        }

        saggioTask.getPushDownTable().add(prev, state, this);
        this.addPrevTask(prev);

        return this;
    }

    /**
     * link previous task to current one, meaning that if all previous tasks executed successfully and returned the given state, current task will be executed.
     *
     * @param prev previous task
     * @param state transfer state
     * @return current task
     */
    public Task<?> fromAnd(Task<?> prev, String state) {
        if (type == null) {
            type = TaskType.AND;
        } else if (type.equals(TaskType.ANY)) {
            throw new RuntimeException("Type is already set to 'ANY', can not add task of type 'AND', task: " + this);
        }

        saggioTask.getPushDownTable().add(prev, state, this);
        this.addPrevTask(prev);

        return this;
    }

    /**
     * task executes as the serial-tasks' beginning node
     *
     * @param executor thread pool
     * @param context TaskContext
     */
    protected void begin(ThreadPoolExecutor executor, TaskContext context) {
        canWork.release();

        execute(executor, context, true);
    }

    /**
     * task's executed method, the task will wait
     * till all/any prev tasks arriving according to the task type,
     * or end to meet the timeout or being interrupted.
     *
     * @param executor thread pool
     * @param context TaskContext
     */
    protected void execute(ThreadPoolExecutor executor, TaskContext context) {
        execute(executor, context, false);
    }

    /**
     * task's executed method, the task will wait
     * till all/any prev tasks arriving according to the task type,
     * or end to meet the timeout or being interrupted.
     *
     * @param executor thread pool
     * @param context TaskContext
     * @param isBegin is begin() execute, is is, the task will be immediately executed without any waiting
     */
    protected void execute(ThreadPoolExecutor executor, TaskContext context, boolean isBegin) {
        currentThread = Thread.currentThread();

        if (!isBegin) {
            joinLock.unlock();
        }

        if (!waitToWork(context)) {
            return;
        }

        // if one any arrived, then other prev any tasks are not needed to be executed.
        stopPrevAny(context);

        TaskResult<T> result = doExecute(context);

        next(result.getState(), executor, context);
    }

    /**
     * wait till all/any prev taskFunc tasks arriving according to the task type,
     * or end to meet the timeout or being interrupted
     *
     * @param context TaskContext
     * @return if can work
     */
    private boolean waitToWork(TaskContext context) {
        int _timeout = useCustomizedTimeout ? timeout : context.getConfig().getTimeout();
        TimeUnit _timeUnit = useCustomizedTimeout ? timeUnit : context.getConfig().getTimeUnit();

        try {
            if (!canWork.tryAcquire(_timeout, _timeUnit)) {
                // if acquire timeout
                System.out.println("Task[" + this + "] acquiring semaphore has been stopped, caused by: timeout--" + _timeout + " " + _timeUnit);

                // if the destination task is waiting out of time, the prev tasks are not needed to be executed.
                stopPrevAnd(context);
                stopPrevAny(context);
                stopAfterPrev(context);
                return false;
            }

            return true;
        } catch (InterruptedException e) {
            System.out.println("Task[" + this + "] acquiring semaphore has been interrupted, caused by: " + e.getCause());

            // if the destination task is waiting interrupted, the prev tasks are not needed to be executed.
            stopPrevAnd(context);
            stopPrevAny(context);
            stopAfterPrev(context);
            currentThread = null;
            return false;
        }
    }

    /**
     * execute prevFunc, taskFunc and callback
     *
     * @param context TaskContext
     * @return TaskResult
     */
    private TaskResult<T> doExecute(TaskContext context) {
        if (prevFunc != null) {
            prevFunc.execute(context);
        }

        TaskResult<T> result;
        try {
            result = taskFunc.execute(context);
            callback.execute(result, context);
            currentThread = null;
            executed = true;

            return result;
        } catch (InterruptedException e) {
            System.out.println("Executing task[" + this + "] has been interrupted, caused by: " + e.getCause());

            // if the destination task is executing interrupted, the prev tasks are not needed to be executed.
            stopPrevAnd(context);
            stopPrevAny(context);
            stopAfterPrev(context);

            currentThread = null;
            return new TaskResult<>(null, null);
        }

    }

    /**
     * jump to and execute all next taskFunc tasks
     *
     * @param state  transfer state from this to next
     * @param executor thread pool
     * @param context TaskContext
     */
    private void next(String state, ThreadPoolExecutor executor, TaskContext context) {
        HashSet<Task<?>> nextTasks = saggioTask.getPushDownTable().getNextTasks(this, state);
        if (nextTasks == null || nextTasks.isEmpty()) {
            return;
        }

        executedAllNext(nextTasks, executor, context);
    }

    /**
     * execute all next tasks
     *
     * @param nextTasks tasks need to be executed
     * @param executor thread pool
     * @param context TaskContext
     */
    private void executedAllNext(HashSet<Task<?>> nextTasks, ThreadPoolExecutor executor, TaskContext context) {
        for (Task<?> nextTask : nextTasks) {
            executor.execute(() -> nextTask.join(this, executor, context));
        }
    }

    /**
     * join() is the way to execute the next task or update next task's status, according to next task's working state.
     *
     * @param prevTask previous taskFunc task
     * @param executor thread pool
     * @param context TaskContext
     */
    private void join(Task<?> prevTask, ThreadPoolExecutor executor, TaskContext context) {
        boolean isBoot = false;

        // optimize
        if (isExecuted() || (isExecuting() && TaskType.ANY.equals(type))) {
            return;
        }

        try {
            joinLock.lock();

            if (isExecuted()) {
                return;
            }

            // if the task is not executed
            if (!isExecuting()) {
                isBoot = true;
                notArrivedCount.set(prevTasks.size());

                if (TaskType.ANY.equals(type)) {
                    canWork.release();
                } else if (TaskType.AND.equals(type)) {
                    if (notArrivedCount.decrementAndGet() == 0) {
                        canWork.release();
                    }
                }
                execute(executor, context);
            } else {
                if (TaskType.AND.equals(type)) {
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

    /**
     * stop the task's all previous executing tasks if current task's type is ANY and need to stop according to the setting stopIfNextStopped
     *
     * @param context TaskContext
     */
    private void stopPrevAny(TaskContext context) {
        if (!context.getConfig().isStopIfNextStopped()) {
            return;
        }

        if (TaskType.ANY.equals(type)) {
            for (Task<?> prevTask : prevTasks) {
                if (prevTask.isExecuting()) {
                    try {
                        System.out.println("Executing task[" + this + "] has been interrupted, caused by: stopPrevAny()");
                        prevTask.getCurrentThread().interrupt();
                    } catch (NullPointerException ignored) {
                    }
                }
            }
        }
    }

    /**
     * stop the task's all previous executing tasks if current task's type is AND and need to stop according to the setting stopIfNextStopped
     *
     * @param context TaskContext
     */
    private void stopPrevAnd(TaskContext context) {
        if (!context.getConfig().isStopIfNextStopped()) {
            return;
        }

        if (TaskType.AND.equals(type)) {
            for (Task<?> prevTask : prevTasks) {
                if (prevTask.isExecuting()) {
                    try {
                        System.out.println("Executing task[" + this + "] has been interrupted, caused by: stopPrevAnd()");
                        prevTask.getCurrentThread().interrupt();
                    } catch (NullPointerException ignored) {
                    }
                }
            }
        }
    }

    /**
     * stop the task's all next executing tasks' prev tasks if need to stop according to the setting stopIfNextStopped
     *
     * @param context TaskContext
     */
    private void stopAfterPrev(TaskContext context) {
        if (!context.getConfig().isStopIfNextStopped()) {
            return;
        }

        final HashMap<String, HashSet<Task<?>>> nextTasks = saggioTask.getPushDownTable().getNextTasks(this);
        if (nextTasks == null) {
            return;
        }

        for (HashSet<Task<?>> tasks : nextTasks.values()) {
            for (Task<?> task : tasks) {
                if (TaskType.AND.equals(task.getType())) {
                    if (task.isExecuting()) {
                        try {
                            System.out.println("Executing task[" + this + "] has been interrupted, caused by: tryStopAfterPrev()");
                            task.getCurrentThread().interrupt();
                        } catch (NullPointerException ignored) {
                        }
                    }
                }

                if (TaskType.ANY.equals(task.getType())) {
                    if (task.isExecuting() && task.notArrivedCount.get() == 1) {
                        // if only current task has not arrived, stop its next task
                        try {
                            System.out.println("Executing task[" + this + "] has been interrupted, caused by: tryStopAfterPrev()");
                            task.getCurrentThread().interrupt();
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

    public TaskType getType() {
        return type;
    }

    public void setPrevFunc(PrevTaskFunction prevFunc) {
        this.prevFunc = prevFunc;
    }

    protected PrevTaskFunction getPrevFunc() {
        return prevFunc;
    }

    protected TaskFunction<T> getTask() {
        return taskFunc;
    }

    protected TaskCallback<T> getCallback() {
        return callback;
    }

    public HashSet<Task<?>> getPrevTasks() {
        return prevTasks;
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

    private void addPrevTask(Task<?>... tasks) {
        prevTasks.addAll(Arrays.stream(tasks).toList());
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
        Task<?> that = (Task<?>) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Task{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", executing=" + isExecuting() +
                ", executed=" + executed +
                '}';
    }
}
