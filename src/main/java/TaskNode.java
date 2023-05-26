import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * [FEATURE INFO]<br/>
 *
 * @author Riicarus
 * @create 2023-5-26 2:07
 * @since 1.0.0
 */
public class TaskNode<T> implements Runnable {

    private final int id;

    private final Set<TaskNode<?>> any = new HashSet<>();

    private final Set<TaskNode<?>> and = new HashSet<>();

    private TaskType type;

    private final Task<T> task;

    private TaskCallback<T> callback;

    private volatile TaskWorker worker;
    private volatile boolean executing = false;
    private volatile boolean executed = false;

    public TaskNode(int id, TaskType type, Task<T> task, TaskCallback<T> callback) {
        this.id = id;
        this.type = type;
        this.task = task;
        this.callback = callback;
    }

    public TaskNode(int id, TaskType type, Task<T> task) {
        this.id = id;
        this.type = type;
        this.task = task;
    }

    @Override
    public void run() {
        TaskResult<T> result = task.run();

        if (callback != null) {
            callback.execute(result);
        }

        executed = true;

        next(result.getState());
    }

    public void next(String state) {
        worker.nextTask(state);
    }

    public int getId() {
        return id;
    }

    public Set<TaskNode<?>> getAny() {
        return any;
    }

    public void addAny(TaskNode<?> node) {
        any.add(node);
    }

    public Set<TaskNode<?>> getAnd() {
        return and;
    }

    public void addAnd(TaskNode<?> node) {
        and.add(node);
    }

    public TaskType getType() {
        return type;
    }

    public void upgradeType(TaskType newType) {
        if (type.equals(TaskType.SERIAL) || type.equals(TaskType.AND)) {
            if (newType.equals(TaskType.SERIAL) || newType.equals(TaskType.AND)) {
                type = TaskType.AND;
            } else {
                type = TaskType.MULTI;
            }

//            System.out.println("type upgraded: " + type);

            return;
        }

        if (type.equals(TaskType.ANY)) {
            if (!newType.equals(TaskType.ANY)) {
                type = TaskType.MULTI;
            }
        }
//        System.out.println("type upgraded: " + type);

        // if type is multi, do nothing
    }

    public Task<T> getTask() {
        return task;
    }

    public TaskCallback<T> getCallback() {
        return callback;
    }

    public void setCallback(TaskCallback<T> callback) {
        this.callback = callback;
    }

    public TaskWorker getWorker() {
        return worker;
    }

    public void setWorker(TaskWorker worker) {
        this.worker = worker;
    }

    public boolean isExecuting() {
        return executing;
    }

    public void setExecuting(boolean executing) {
        this.executing = executing;
    }

    public boolean isExecuted() {
        return executed;
    }

    public void setExecuted(boolean executed) {
        this.executed = executed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskNode<?> taskNode = (TaskNode<?>) o;
        return id == taskNode.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TaskNode{" +
                "id=" + id +
                ", type=" + type +
                ", executed=" + executed +
                '}';
    }
}
