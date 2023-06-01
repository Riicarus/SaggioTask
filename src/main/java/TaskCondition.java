import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
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

    public TaskCondition(String name, Task<T> task, TaskCallback<T> callback) {
        this.name = name + "-" + SaggioTask.generateId();
        this.task = task;
        this.callback = callback;
    }

    public TaskCondition<?> fromAny(TaskCondition<?> prev, String state) {
        if (type == null) {
            type = ConditionType.ANY;
        } else if (type.equals(ConditionType.AND)) {
            throw new RuntimeException("Type is already set to 'AND', can not add condition of type 'ANY', condition: " + this);
        }

        TaskPushDownTable.add(prev, state, this);
        this.addPrevCondition(prev);

        return this;
    }

    public TaskCondition<?> fromAnd(TaskCondition<?> prev, String state) {
        if (type == null) {
            type = ConditionType.AND;
        } else if (type.equals(ConditionType.ANY)) {
            throw new RuntimeException("Type is already set to 'ANY', can not add condition of type 'AND', condition: " + this);
        }

        TaskPushDownTable.add(prev, state, this);
        this.addPrevCondition(prev);

        return this;
    }

    protected void begin(ThreadPoolExecutor executor) {
        canWork.release();

        execute(executor, true);
    }

    protected void execute(ThreadPoolExecutor executor) {
        execute(executor, false);
    }

    protected void execute(ThreadPoolExecutor executor, boolean isBegin) {
        currentThread = Thread.currentThread();

        if (!isBegin) {
            joinLock.unlock();
        }

        try {
            canWork.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }

        TaskResult<T> result = task.execute();
        callback.execute(result);
        currentThread = null;
        executed = true;

        next(result.getState(), executor);
    }

    private void next(String state, ThreadPoolExecutor executor) {
        HashSet<TaskCondition<?>> nextConditions = TaskPushDownTable.getNextConditions(this, state);
        if (nextConditions == null || nextConditions.isEmpty()) {
            return;
        }

        executedAllNext(nextConditions, executor);
    }

    private void executedAllNext(HashSet<TaskCondition<?>> nextConditions, ThreadPoolExecutor executor) {
        for (TaskCondition<?> nextCondition : nextConditions) {
            executor.execute(() -> nextCondition.join(this, executor));
        }
    }

    private void join(TaskCondition<?> prevCondition, ThreadPoolExecutor executor) {
        boolean isBoot = false;

        // optimize
        if (isExecuted() || (isExecuting() && ConditionType.ANY.equals(type))) {
            return;
        }

        try {
            joinLock.lock();

//            System.out.println(prevCondition + " join to " + this + " , thread: " + Thread.currentThread());

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
//                        System.out.println(this + " released");
                    }
                }

                execute(executor);
            } else {
                if (ConditionType.AND.equals(type)) {
                    if (notArrivedCount.decrementAndGet() == 0) {
                        canWork.release();
//                        System.out.println(this + " released");
                    }
                }
            }
        } finally {
            if (!isBoot) {
                joinLock.unlock();
            }
        }
    }

    public String getName() {
        return name;
    }

    public ConditionType getType() {
        return type;
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

    private void addPrevCondition(TaskCondition<?> ...conditions) {
        prevConditions.addAll(Arrays.stream(conditions).toList());
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
