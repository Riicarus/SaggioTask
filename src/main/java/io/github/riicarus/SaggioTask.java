package io.github.riicarus;

import io.github.riicarus.function.PrevTaskFunction;
import io.github.riicarus.function.TaskCallback;
import io.github.riicarus.function.TaskFunction;

import java.util.HashMap;
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

    private final HashMap<String, TransferableTask<?>> TASKS = new HashMap<>();

    public SaggioTask() {
    }

    /**
     * build an initial task with needed attributes
     *
     * @param name task name
     * @param taskFunc main task function
     * @param <T> type of taskFunc result's data
     * @return initial task
     */
    public <T> TransferableTask<T> build(String name, TaskFunction<T> taskFunc) {
        TransferableTask<T> task = new TransferableTask<>(name, taskFunc, this);
        TASKS.put(task.getName(), task);

        return task;
    }

    /**
     * build an initial task with needed attributes
     *
     * @param name task name
     * @param taskFunc main task function
     * @param callback callback function after task function's execution
     * @param <T> type of taskFunc result's data
     * @return initial task
     */
    public <T> TransferableTask<T> build(String name, TaskFunction<T> taskFunc, TaskCallback<T> callback) {
        TransferableTask<T> task = new TransferableTask<>(name, taskFunc, callback, this);
        TASKS.put(task.getName(), task);

        return task;
    }

    /**
     * build an initial task with needed attributes
     *
     * @param name task name
     * @param prevFunc function executed before task function's execution
     * @param taskFunc main task function
     * @param callback callback function after task function's execution
     * @param <T> type of taskFunc result's data
     * @return initial task
     */
    public <T> TransferableTask<T> build(String name, PrevTaskFunction prevFunc, TaskFunction<T> taskFunc, TaskCallback<T> callback) {
        TransferableTask<T> task = new TransferableTask<>(name, taskFunc, callback, this);
        task.setPrevFunc(prevFunc);

        TASKS.put(task.getName(), task);

        return task;
    }

    /**
     * generate an initial copy of the given task
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

    protected TaskPushDownTable getPushDownTable() {
        return PUSH_DOWN_TABLE;
    }

    public TransferableTask<?> getTask(String name) {
        return TASKS.get(name);
    }
}
