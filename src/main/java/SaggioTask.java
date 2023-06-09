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
     * count of task conditions, used for task name generation
     */
    private final AtomicInteger taskConditionCount = new AtomicInteger(0);

    private final TaskPushDownTable PUSH_DOWN_TABLE = new TaskPushDownTable();

    public SaggioTask() {
    }

    /**
     * build a initial task condition with must attributes
     *
     * @param name task name
     * @param task main task function
     * @param callback callback function after task function's execution
     * @param <T> type of task result's data
     * @return initial task condition
     */
    public <T> TaskCondition<T> build(String name, Task<T> task, TaskCallback<T> callback) {
        return new TaskCondition<>(name, task, callback, this);
    }

    /**
     * build a initial task condition with must attributes
     *
     * @param name task name
     * @param prevFunc function executed before task function's execution
     * @param task main task function
     * @param callback callback function after task function's execution
     * @param <T> type of task result's data
     * @return initial task condition
     */
    public <T> TaskCondition<T> build(String name, PrevTaskFunction prevFunc, Task<T> task, TaskCallback<T> callback) {
        TaskCondition<T> condition = new TaskCondition<>(name, task, callback, this);
        condition.setPrevFunc(prevFunc);
        return condition;
    }

    /**
     * generate a initial copy of given task condition
     *
     * @param name task name
     * @param srcCondition the source condition to be copied
     * @param <T> type of task result's data
     * @return initial task condition
     */
    public <T> TaskCondition<T> buildFrom(String name, TaskCondition<T> srcCondition) {
        return build(name, srcCondition.getPrevFunc(), srcCondition.getTask(), srcCondition.getCallback());
    }

    /**
     * run a serial-tasks from the given conditions
     *
     * @param conditions start conditions
     * @param executor thread pool
     * @param context TaskContext
     */
    public void run(List<TaskCondition<?>> conditions, ThreadPoolExecutor executor, TaskContext context) {
        for (TaskCondition<?> condition : conditions) {
            executor.execute(() -> condition.begin(executor, context));
        }
    }

    protected int generateId() {
        return taskConditionCount.getAndIncrement();
    }

    public TaskPushDownTable getPushDownTable() {
        return PUSH_DOWN_TABLE;
    }
}
