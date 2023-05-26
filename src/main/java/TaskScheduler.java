import java.util.HashSet;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * [FEATURE INFO]<br/>
 *
 * @author Riicarus
 * @create 2023-5-25 20:04
 * @since 1.0.0
 */
public class TaskScheduler {

    private final TaskTable taskTable = new TaskTable();

    private final AtomicInteger taskIdGenerator = new AtomicInteger(0);

    public final TaskNode<?> beginNode = new TaskNode<>(generateId(), TaskType.SERIAL, () -> new TaskResult<>("begin", "begin"));

    private ThreadPoolExecutor workerExecutor;

    private ThreadPoolExecutor taskExecutor;

    public TaskScheduler() {
        taskTable.add(beginNode, null, null);
    }

    public int generateId() {
        return taskIdGenerator.getAndIncrement();
    }

    public HashSet<TaskNode<?>> getNext(TaskNode<?> taskNode, String state) {
        return taskTable.getNext(taskNode, state);
    }

    public <T> TaskNode<T> begin(Task<T> then, TaskCallback<T> callback) {
        return serial(beginNode, "begin", then, callback);
    }

    public <T> TaskNode<T> serial(TaskNode<?> fromNode, String state, Task<T> then, TaskCallback<T> callback) {
        TaskNode<T> thenNode = new TaskNode<>(generateId(), TaskType.SERIAL, then, callback);
        thenNode.addAnd(fromNode);
        taskTable.add(fromNode, state, thenNode);

        return thenNode;
    }
    
    public <T> TaskNode<T> serial(TaskNode<?> fromNode, String state, TaskNode<T> thenNode) {
        thenNode.upgradeType(TaskType.SERIAL);
        thenNode.addAnd(fromNode);

        taskTable.add(fromNode, state, thenNode);
        return thenNode;
    }

    public <T> TaskNode<T> any(TaskNode<?> fromNode, String state, Task<T> then, TaskCallback<T> callback) {
        TaskNode<T> thenNode = new TaskNode<>(generateId(), TaskType.ANY, then, callback);
        thenNode.addAny(fromNode);
        taskTable.add(fromNode, state, thenNode);
        return thenNode;
    }

    public <T> TaskNode<T> any(TaskNode<?> fromNode, String state, TaskNode<T> thenNode) {
        thenNode.upgradeType(TaskType.ANY);

        thenNode.addAny(fromNode);
        taskTable.add(fromNode, state, thenNode);
        return thenNode;
    }

    public <T> TaskNode<T> and(TaskNode<?> fromNode, String state, Task<T> then, TaskCallback<T> callback) {
        TaskNode<T> thenNode = new TaskNode<>(generateId(), TaskType.AND, then, callback);
        thenNode.addAnd(fromNode);
        taskTable.add(fromNode, state, thenNode);
        return thenNode;
    }

    public <T> TaskNode<T> and(TaskNode<?> fromNode, String state, TaskNode<T> thenNode) {
        thenNode.upgradeType(TaskType.AND);
        thenNode.addAnd(fromNode);

        taskTable.add(fromNode, state, thenNode);
        return thenNode;
    }

    public void work(ThreadPoolExecutor workerExecutor, ThreadPoolExecutor taskExecutor) {
        TaskWorker worker = new TaskWorker(beginNode, this, workerExecutor, taskExecutor);
        workerExecutor.execute(worker::work);
    }
}
