import java.util.List;
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

    /**
     * count of tasks, used for task name generation
     */
    private final AtomicInteger taskCount = new AtomicInteger(0);

    private final TaskPushDownTable PUSH_DOWN_TABLE = new TaskPushDownTable();

    public SaggioTask() {
    }

    /**
     * build a initial task with must attributes
     *
     * @param name task name
     * @param taskFunction main task function
     * @param callback callback function after task function's execution
     * @param <T> type of taskFunction result's data
     * @return initial task
     */
    public <T> TransferableTask<T> build(String name, TaskFunction<T> taskFunction, TaskCallback<T> callback) {
        return new TransferableTask<>(name, taskFunction, callback, this);
    }

    /**
     * build a initial task with must attributes
     *
     * @param name task name
     * @param prevFunc function executed before task function's execution
     * @param taskFunction main task function
     * @param callback callback function after task function's execution
     * @param <T> type of taskFunction result's data
     * @return initial task
     */
    public <T> TransferableTask<T> build(String name, PrevTaskFunction prevFunc, TaskFunction<T> taskFunction, TaskCallback<T> callback) {
        TransferableTask<T> task = new TransferableTask<>(name, taskFunction, callback, this);
        task.setPrevFunc(prevFunc);
        return task;
    }

    /**
     * generate a initial copy of the given task
     *
     * @param name task name
     * @param srcTask the source task to be copied
     * @param <T> type of task result's data
     * @return initial task
     */
    public <T> TransferableTask<T> buildFrom(String name, TransferableTask<T> srcTask) {
        return build(name, srcTask.getPrevFunc(), srcTask.getTaskFunc(), srcTask.getCallback());
    }

    /**
     * run a serial-tasks from the given tasks
     *
     * @param tasks start tasks
     * @param executor thread pool
     * @param context TaskContext
     */
    public void run(List<TransferableTask<?>> tasks, ThreadPoolExecutor executor, TaskContext context) {
        for (TransferableTask<?> task : tasks) {
            executor.execute(() -> task.begin(executor, context));
        }
    }

    protected int generateId() {
        return taskCount.getAndIncrement();
    }

    public TaskPushDownTable getPushDownTable() {
        return PUSH_DOWN_TABLE;
    }
}
