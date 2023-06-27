import java.util.concurrent.ThreadPoolExecutor;

/**
 * [FEATURE INFO]<br/>
 * task synchronizer interface
 *
 * @author Riicarus
 * @create 2023-6-27 8:28
 * @since 1.0.1
 */
public interface TaskSync {

    /**
     * wait till all/any prev taskFunc tasks arriving according to the task type,
     * or end to meet the timeout or being interrupted
     *
     * @param context TaskContext
     * @return if can work
     */
    boolean waitToWork(TaskContext context);

    /**
     * task's executed method, the task will wait
     * till all/any prev tasks arriving according to the task type,
     * or end to meet the timeout or being interrupted.
     *
     * @param executor thread pool
     * @param context TaskContext
     * @param isBegin is begin() execute, is is, the task will be immediately executed without any waiting
     */
    void execute(ThreadPoolExecutor executor, TaskContext context, boolean isBegin);

    /**
     * join() is the way to execute the current task or update current task's status, according to current task's working state. <br/>
     * Executed by previous task.
     *
     * @param executor thread pool
     * @param context TaskContext
     */
    void join(ThreadPoolExecutor executor, TaskContext context);

    /**
     * jump to and execute next task(s)
     *
     * @param state  transfer state from this to next
     * @param executor thread pool
     * @param context TaskContext
     */
    void next(String state, ThreadPoolExecutor executor, TaskContext context);

    /**
     * handle task's waiting/executing interrupted
     *
     * @param context TaskContext
     */
    void handleInterrupted(TaskContext context);

    /**
     * get if the task is executing
     *
     * @return if the task is executing
     */
    boolean isExecuting();

    /**
     * get if the task is executed
     *
     * @return if the task is executed
     */
    boolean isExecuted();

    /**
     * get if the task is canceled
     *
     * @return if the task is canceled
     */
    boolean isCanceled();

    /**
     * get the thread witch is executing the task if the task is in executing status
     *
     * @return thread witch is executing the task
     */
    Thread getCurrentThread();
}
