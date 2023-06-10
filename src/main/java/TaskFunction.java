/**
 * [FEATURE INFO]<br/>
 *
 * @author Riicarus
 * @create 2023-5-25 20:01
 * @since 1.0.0
 */
public interface TaskFunction<T> {

    TaskResult<T> execute(TaskContext context) throws InterruptedException;
}
