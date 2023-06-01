/**
 * [FEATURE INFO]<br/>
 *
 * @author Riicarus
 * @create 2023-5-25 20:01
 * @since 1.0.0
 */
public interface Task<T> {

    TaskResult<T> execute();
}
