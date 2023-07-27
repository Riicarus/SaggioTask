package exception;

/**
 * Runtime exception when task arrangement failed
 *
 * @author Riicarus
 * @create 2023-7-27 8:10
 * @since 1.0.0
 */
public class TaskArrangeException extends RuntimeException {

    public TaskArrangeException(String message) {
        super(message);
    }

    public TaskArrangeException(String message, Throwable cause) {
        super(message, cause);
    }
}
