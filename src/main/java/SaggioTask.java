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

    private static final AtomicInteger taskConditionCount = new AtomicInteger(0);

    public static <T> TaskCondition<T> build(String name, Task<T> task, TaskCallback<T> callback) {
        return new TaskCondition<>(name, task, callback);
    }

    public static void run(TaskCondition<?> condition, ThreadPoolExecutor executor) {
        executor.execute(() -> condition.begin(executor));
    }

    protected static int generateId() {
        return taskConditionCount.getAndIncrement();
    }
}
