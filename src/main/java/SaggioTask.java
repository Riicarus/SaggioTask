import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * [FEATURE INFO]<br/>
 * task api
 *
 * @author Riicarus
 * @create 2023-6-1 14:48
 * @since 1.0.0
 */
public class SaggioTask {

    private final AtomicInteger taskConditionCount = new AtomicInteger(0);

    private final TaskPushDownTable PUSH_DOWN_TABLE = new TaskPushDownTable();

    public SaggioTask() {
    }

    public <T> TaskCondition<T> build(String name, Task<T> task, TaskCallback<T> callback) {
        return new TaskCondition<>(name, task, callback, this);
    }

    public void run(TaskCondition<?> condition, ThreadPoolExecutor executor, TaskContext context) {
        executor.execute(() -> condition.begin(executor, context));
    }

    protected int generateId() {
        return taskConditionCount.getAndIncrement();
    }

    public TaskPushDownTable getPushDownTable() {
        return PUSH_DOWN_TABLE;
    }
}
