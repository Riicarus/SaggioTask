/**
 * [FEATURE INFO]<br/>
 * function execute before task execution
 *
 * @author Riicarus
 * @create 2023-6-8 23:34
 * @since 1.0.0
 */
@FunctionalInterface
public interface PrevTaskFunction {

    void execute(TaskContext context);

}
