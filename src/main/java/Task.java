import java.util.HashSet;

/**
 * [FEATURE INFO]<br/>
 * task interface
 *
 * @author Riicarus
 * @create 2023-6-26 17:53
 * @since 1.0.1
 */
public interface Task<T> {

    // core attributes
    String getName();
    TaskType getType();
    HashSet<TransferableTask<?>> getPrevTasks();

    // executable functions
    PrevTaskFunction getPrevFunc();
    TaskFunction<T> getTaskFunc();
    TaskCallback<T> getCallback();

    // execute method
    TaskResult<T> doExecute(TaskContext context);

}
