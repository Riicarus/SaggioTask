import java.util.HashSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * [FEATURE INFO]<br/>
 * worker of a task
 *
 * @author Riicarus
 * @create 2023-5-25 20:04
 * @since 1.0.0
 */
public class TaskWorker {

    private final HashSet<TaskNode<?>> any = new HashSet<>();
    private final HashSet<TaskNode<?>> and = new HashSet<>();

    private final Lock joinLock = new ReentrantLock();
    private final Lock stateLock = new ReentrantLock();

    private final Semaphore canWork = new Semaphore(0);

    private boolean anyFlag;

    private TaskNode<?> taskNode;

    private final TaskScheduler scheduler;

    private final ThreadPoolExecutor workerExecutor;

    private final ThreadPoolExecutor taskExecutor;

    public TaskWorker(TaskNode<?> taskNode, TaskScheduler scheduler, ThreadPoolExecutor workerExecutor, ThreadPoolExecutor taskExecutor) {
        this.taskNode = taskNode;
        this.and.addAll(taskNode.getAnd());
        this.any.addAll(taskNode.getAny());
        this.anyFlag = any.isEmpty();
        this.scheduler = scheduler;
        this.workerExecutor = workerExecutor;
        this.taskExecutor = taskExecutor;
    }

    public void nextTask(String state) {
        TaskNode<?> prevNode = taskNode;
        HashSet<TaskNode<?>> nextNodes = scheduler.getNext(taskNode, state);

        if (nextNodes == null || nextNodes.isEmpty()) {
            return;
        }

        int count = 0;

        for (TaskNode<?> nextNode : nextNodes) {
            // if the task node has already been executed, skip to next.
            if (nextNode.isExecuted()) {
                continue;
            }

            if (count == 0) {
                // dispatch the first not-executed one to current worker.
                taskNode = nextNode;
                and.clear();
                any.clear();
                and.addAll(taskNode.getAnd());
                any.addAll(taskNode.getAny());

                if (taskNode.isExecuting()) {
                    // if the task node is executing, then make the worker join to then node's worker.
//                    taskExecutor.execute(() -> System.out.println("From " + prevNode + ", TaskNode: " + taskNode + " has worker"));
                    taskNode.getWorker().join(prevNode);
                } else {
                    and.remove(prevNode);
                    any.remove(prevNode);
                    this.anyFlag = any.isEmpty();
                }
            } else {
                // dispatch each task node to a new worker and execute them to thread pool.
                TaskWorker worker = new TaskWorker(nextNode, scheduler, workerExecutor, taskExecutor);
                workerExecutor.execute(worker::work);
            }

            count++;
        }

        work();
    }

    public void work() {
        stateLock.lock();

        if (taskNode.isExecuting() || taskNode.isExecuted()) {
            return;
        }

        taskNode.setExecuting(true);
        taskNode.setWorker(this);

        stateLock.unlock();

        if (taskNode.getType().equals(TaskType.SERIAL)) {
//            System.out.println("Thread: " + Thread.currentThread());

            taskExecutor.execute(taskNode);
        } else if (taskNode.getType().equals(TaskType.ANY)) {
//            System.out.println("Thread: " + Thread.currentThread());

            taskExecutor.execute(taskNode);
        } else if (taskNode.getType().equals(TaskType.AND) || taskNode.getType().equals(TaskType.MULTI)) {
            try {
//                System.out.println("Thread: " + Thread.currentThread() + " is waiting...");
                canWork.acquire();
//                System.out.println("Thread: " + Thread.currentThread());

                taskExecutor.execute(taskNode);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * join() is the process when a worker jump from prevNode to its nextNode. <br/>
     * It happens when the nextNode already has a worker.<br/>
     * The node's task type can only be any, and or multi.
     *
     * @param prevNode previous node
     */
    private void join(TaskNode<?> prevNode) {
//        taskExecutor.execute(() -> System.out.println("Before Join: prev: " + prevNode + ", next: " + taskNode));
        joinLock.lock();

        if (taskNode.isExecuted()) {
            return;
        }

//        taskExecutor.execute(() -> System.out.println("Inner Join: prev: " + prevNode + ", next: " + taskNode));

        if (taskNode.getAnd().contains(prevNode)) {
            and.remove(prevNode);
//            System.out.println("Thread: " + Thread.currentThread() + " Removes task node from and: " + prevNode);
        } else if (taskNode.getAny().contains(prevNode)) {
            any.remove(prevNode);
//            System.out.println("Thread: " + Thread.currentThread() + " Removes task node from any: " + prevNode);
            anyFlag = true;
        } else {
            throw new RuntimeException("Task join exception, prev: " + prevNode + ", next: " + taskNode);
        }

        if (taskNode.getType().equals(TaskType.AND)) {
            if (and.isEmpty()) {
                canWork.release();
            }
        } else if (taskNode.getType().equals(TaskType.MULTI)) {
            if (and.isEmpty() && anyFlag) {
//                System.out.println("released");
                canWork.release();
            }
        } else if (taskNode.getType().equals(TaskType.ANY)) {
            if (anyFlag) {
                canWork.release();
            }
        }

        joinLock.unlock();
    }
}
