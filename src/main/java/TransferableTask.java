import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * [FEATURE INFO]<br/>
 * task interface
 *
 * @author Riicarus
 * @create 2023-6-12 22:45
 * @since 1.0.0
 */
public class TransferableTask<T> implements Transferable<TransferableTask<?>>, Task<T> {

    protected final TaskSynchronizer<T> sync;

    // push-down table maintainer
    private final SaggioTask saggioTask;

    // core attributes
    protected final String name;
    protected TaskType type;
    protected final HashSet<TransferableTask<?>> prevTasks = new HashSet<>();

    // task's executable function interfaces
    private PrevTaskFunction prevFunc;
    private final TaskFunction<T> taskFunc;
    private TaskCallback<T> callback;

    protected boolean useCustomizedTimeout = false;
    protected int timeout = 3000;
    protected TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    public TransferableTask(String name, TaskFunction<T> taskFunc, SaggioTask saggioTask) {
        this.sync = new TaskSynchronizer<>(this);
        this.name = name;
        this.taskFunc = taskFunc;
        this.saggioTask = saggioTask;
    }

    public TransferableTask(String name, TaskFunction<T> taskFunc, TaskCallback<T> callback, SaggioTask saggioTask) {
        this.sync = new TaskSynchronizer<>(this);
        this.name = name;
        this.taskFunc = taskFunc;
        this.callback = callback;
        this.saggioTask = saggioTask;
    }

    public TransferableTask(String name, PrevTaskFunction prevFunc, TaskFunction<T> taskFunc, TaskCallback<T> callback, SaggioTask saggioTask) {
        this.sync = new TaskSynchronizer<>(this);
        this.name = name;
        this.prevFunc = prevFunc;
        this.taskFunc = taskFunc;
        this.callback = callback;
        this.saggioTask = saggioTask;
    }

    /**
     * link previous task to current one, meaning that if all previous tasks executed successfully and returned the given state, current task will be executed.
     *
     * @param prev previous task
     * @param fromState transfer state
     * @return current task
     */
    @Override
    public TransferableTask<?> and(TransferableTask<?> prev, String fromState) {
        if (type == null) {
            type = TaskType.AND;
        } else if (type.equals(TaskType.ANY)) {
            throw new RuntimeException("Type is already set to 'ANY', can not add task of type 'AND', task: " + this);
        }

        saggioTask.getPushDownTable().add(prev, fromState, this);
        this.addPrevTask(prev);

        return this;
    }

    /**
     * link previous task to current one, meaning that if any previous tasks executed successfully and returned the given state, current task will be executed.
     *
     * @param prev previous task
     * @param fromState transfer state
     * @return current task
     */
    @Override
    public TransferableTask<?> any(TransferableTask<?> prev, String fromState) {
        if (type == null) {
            type = TaskType.ANY;
        } else if (type.equals(TaskType.AND)) {
            throw new RuntimeException("Type is already set to 'AND', can not add task of type 'ANY', task: " + this);
        }

        saggioTask.getPushDownTable().add(prev, fromState, this);
        this.addPrevTask(prev);

        return this;
    }

    protected void addPrevTask(TransferableTask<?>... tasks) {
        prevTasks.addAll(Arrays.asList(tasks));
    }

    protected void begin(ThreadPoolExecutor executor, TaskContext context) {
        sync.begin(executor, context);
    }

    /**
     * task's executed method, the task will wait
     * till all/any prev tasks arriving according to the task type,
     * or end to meet the timeout or being interrupted.
     *
     * @param executor thread pool
     * @param context TaskContext
     * @param isBegin is begin() execute, that is, the task will be immediately executed without any waiting
     */
    protected void execute(ThreadPoolExecutor executor, TaskContext context, boolean isBegin) {
        sync.execute(executor, context, isBegin);
    }

    /**
     * execute prevFunc, taskFunc and callback
     *
     * @param context TaskContext
     * @return TaskResult
     */
    @Override
    public TaskResult<T> doExecute(TaskContext context) {
        if (prevFunc != null) {
            prevFunc.execute(context);
        }

        TaskResult<T> result;
        try {
            result = taskFunc.execute(context);

            if (callback != null) {
                callback.execute(result, context);
            }

            sync.updateSuccessStatus();

            return result;
        } catch (InterruptedException e) {
            System.out.println("Executing task[" + this + "] has been interrupted, caused by: " + e.getCause());

            sync.handleInterrupted(context);

            return new TaskResult<>(null, null);
        }
    }

    @Override
    public HashSet<TransferableTask<?>> getPrevTasks() {
        return prevTasks;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public TaskType getType() {
        return type;
    }

    @Override
    public PrevTaskFunction getPrevFunc() {
        return prevFunc;
    }

    @Override
    public TaskFunction<T> getTaskFunc() {
        return taskFunc;
    }

    @Override
    public TaskCallback<T> getCallback() {
        return callback;
    }

    public TransferableTask<?> setTimeout(int timeout, TimeUnit timeUnit) {
        this.timeout = timeout;
        this.timeUnit = timeUnit;
        this.useCustomizedTimeout = true;
        return this;
    }

    public boolean isUseCustomizedTimeout() {
        return useCustomizedTimeout;
    }

    public int getTimeout() {
        return timeout;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setPrevFunc(PrevTaskFunction prevFunc) {
        this.prevFunc = prevFunc;
    }

    public void setCallback(TaskCallback<T> callback) {
        this.callback = callback;
    }

    protected void setType(TaskType type) {
        this.type = type;
    }

    public SaggioTask getSaggioTask() {
        return saggioTask;
    }

    public TaskSynchronizer<T> getSync() {
        return sync;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransferableTask<?> that = (TransferableTask<?>) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "TransferableTask{" +
                "name='" + name + '\'' +
                ", type=" + type +
                '}';
    }

    protected static class TaskSynchronizer<T> implements TaskSync {
        // locks and concurrent-working arrangement attributes
        final Semaphore canWork = new Semaphore(0);
        final Lock joinLock = new ReentrantLock();
        final AtomicInteger notArrivedCount = new AtomicInteger();
        volatile boolean executed = false;
        volatile boolean canceled = false;
        /**
         * The thread which is executing execute() method, may be block by canWork.acquire() and will be set to null when executed.
         */
        volatile Thread currentThread;

        final TransferableTask<T> task;

        public TaskSynchronizer(TransferableTask<T> task) {
            if (task == null) {
                throw new RuntimeException("TaskSynchronizer's task can not be null");
            }

            this.task = task;
        }

        /**
         * wait till all/any prev taskFunc tasks arriving according to the task type,
         * or end to meet the timeout or being interrupted
         *
         * @param context TaskContext
         * @return if task can work
         */
        @Override
        public boolean waitToWork(TaskContext context) {
//            System.out.println("enter wait, thread: " + Thread.currentThread());

            int _timeout = task.isUseCustomizedTimeout() ? task.getTimeout() : context.getConfig().getTimeout();
            TimeUnit _timeUnit = task.isUseCustomizedTimeout() ? task.getTimeUnit() : context.getConfig().getTimeUnit();

            try {
                if (!canWork.tryAcquire(_timeout, _timeUnit)) {
                    // if acquire timeout
                    System.out.println("TransferableTask[" + task + "] acquiring semaphore has been stopped, caused by: timeout--" + _timeout + " " + _timeUnit);

                    handleWaitTimeout(context);
                    return false;
                }

                return true;
            } catch (InterruptedException e) {
                System.out.println("TransferableTask[" + task + "] acquiring semaphore has been interrupted");

                handleInterrupted(context);
                return false;
            }
        }

        /**
         * task executes as the serial-tasks' beginning node
         *
         * @param executor thread pool
         * @param context TaskContext
         */
        public void begin(ThreadPoolExecutor executor, TaskContext context) {
            canWork.release();

            task.execute(executor, context, true);
        }

        /**
         * task's executed method, the task will wait
         * till all/any prev tasks arriving according to the task type,
         * or end to meet the timeout or being interrupted.
         *
         * @param executor thread pool
         * @param context TaskContext
         * @param isBegin is begin() execute, that is, the task will be immediately executed without any waiting
         */
        @Override
        public void execute(ThreadPoolExecutor executor, TaskContext context, boolean isBegin) {
            currentThread = Thread.currentThread();
            currentThread.setName("task-" + task.name);

            if (!isBegin) {
                joinLock.unlock();
            }

            if (!waitToWork(context)) {
                return;
            }

            stopPrevAny(context);

            TaskResult<T> result = task.doExecute(context);

            next(result.getState(), executor, context);
        }

        /**
         * join() is the way to execute the current task or update current task's status, according to current task's working state. <br/>
         * Executed by previous task.
         *
         * @param executor thread pool
         * @param context TaskContext
         */
        @Override
        public void join(ThreadPoolExecutor executor, TaskContext context) {
            boolean isBoot = false;

            // optimize
            if (isExecuted() || isCanceled() || (isExecuting() && TaskType.ANY.equals(task.getType()))) {
                return;
            }

            try {
                joinLock.lock();

                if (isExecuted() || isCanceled()) {
                    return;
                }

                // if the task is not executed
                if (!isExecuting()) {
                    isBoot = true;
                    notArrivedCount.set(task.getPrevTasks().size());

                    if (TaskType.ANY.equals(task.getType())) {
                        canWork.release();
                    } else if (TaskType.AND.equals(task.getType())) {
                        if (notArrivedCount.decrementAndGet() == 0) {
                            canWork.release();
                        }
                    }
                    task.execute(executor, context, false);
                } else {
                    if (TaskType.AND.equals(task.getType())) {
                        if (notArrivedCount.decrementAndGet() == 0) {
                            canWork.release();
                        }
                    }
                }
            } finally {
                if (!isBoot) {
                    joinLock.unlock();
                }
            }
        }

        /**
         * jump to and execute all next tasks
         *
         * @param state  transfer state from this to next
         * @param executor thread pool
         * @param context TaskContext
         */
        @Override
        public void next(String state, ThreadPoolExecutor executor, TaskContext context) {
            HashSet<TransferableTask<?>> nextTasks = task.getSaggioTask().getPushDownTable().getNextTasks(this.task, state);
            if (nextTasks == null || nextTasks.isEmpty()) {
                return;
            }

            executedAllNext(nextTasks, executor, context);
        }

        /**
         * execute all next tasks
         *
         * @param nextTasks tasks need to be executed
         * @param executor thread pool
         * @param context TaskContext
         */
        private void executedAllNext(HashSet<TransferableTask<?>> nextTasks, ThreadPoolExecutor executor, TaskContext context) {
            for (TransferableTask<?> nextTask : nextTasks) {
                executor.execute(() -> nextTask.getSync().join(executor, context));
            }
        }

        public void updateSuccessStatus() {
            executed = true;
            currentThread = null;
        }

        /**
         * handle task's waiting/executing interrupted
         *
         * @param context TaskContext
         */
        @Override
        public void handleInterrupted(TaskContext context) {
            // if the destination task is executing/waiting interrupted, the prev tasks are not needed to be executed.
            stopPrevAnd(context);
            stopPrevAny(context);
            stopAfterPrev(context);

            currentThread = null;
            canceled = true;
        }

        /**
         * handle task waiting timeout
         *
         * @param context TaskContext
         */
        public void handleWaitTimeout(TaskContext context) {
            // if the destination task is waiting timeout, the prev tasks are not needed to be executed.
            stopPrevAnd(context);
            stopPrevAny(context);
            stopAfterPrev(context);

            currentThread = null;
            canceled = true;
        }

        /**
         * handle task arrived to current task whose type is ANY
         *
         * @param context TaskContext
         */
        public void handleAnyArrived(TaskContext context) {
            stopPrevAny(context);
        }

        /**
         * stop the task's all previous executing tasks if current task's type is ANY and need to stop according to the setting stopIfNextStopped
         *
         * @param context TaskContext
         */
        private void stopPrevAny(TaskContext context) {
            if (!context.getConfig().isStopIfNextStopped()) {
                return;
            }

            if (TaskType.ANY.equals(task.getType())) {
                for (TransferableTask<?> prevTask : task.getPrevTasks()) {
                    if (prevTask.getSync().isExecuting()) {
                        try {
                            System.out.println("Executing task[" + prevTask + "] will be interrupted, caused by: stopPrevAny(), by task: " + task);
                            prevTask.getSync().getCurrentThread().interrupt();
                        } catch (NullPointerException ignored) {
                        }
                    }
                }
            }
        }

        /**
         * stop the task's all previous executing tasks if current task's type is AND and need to stop according to the setting stopIfNextStopped
         *
         * @param context TaskContext
         */
        private void stopPrevAnd(TaskContext context) {
            if (!context.getConfig().isStopIfNextStopped()) {
                return;
            }

            if (TaskType.AND.equals(task.getType())) {
                for (TransferableTask<?> prevTask : task.getPrevTasks()) {
                    if (prevTask.getSync().isExecuting()) {
                        try {
                            System.out.println("Executing task[" + task + "] will be interrupted, caused by: stopPrevAnd(), by task: " + task);
                            prevTask.getSync().getCurrentThread().interrupt();
                        } catch (NullPointerException ignored) {
                        }
                    }
                }
            }
        }

        /**
         * stop the task's all next executing tasks' prev tasks if needed to stop according to the setting stopIfNextStopped
         *
         * @param context TaskContext
         */
        private void stopAfterPrev(TaskContext context) {
            if (!context.getConfig().isStopIfNextStopped()) {
                return;
            }

            final HashMap<String, HashSet<TransferableTask<?>>> nextTasks = task.getSaggioTask().getPushDownTable().getNextTasks(this.task);
            if (nextTasks == null) {
                return;
            }

            for (HashSet<TransferableTask<?>> tasks : nextTasks.values()) {
                for (TransferableTask<?> task : tasks) {
                    if (TaskType.AND.equals(task.getType())) {
                        if (task.getSync().isExecuting()) {
                            try {
                                System.out.println("Executing task[" + task + "] will be interrupted, caused by: tryStopAfterPrev(), by task: " + this.task);
                                task.getSync().getCurrentThread().interrupt();
                            } catch (NullPointerException ignored) {
                            }
                        }
                    }

                    if (TaskType.ANY.equals(task.getType())) {
                        if (task.getSync().isExecuting() && task.getSync().getNotArrivedCount() == 1) {
                            // if only current task has not arrived, stop its next task
                            try {
                                System.out.println("Executing task[" + task + "] will be interrupted, caused by: tryStopAfterPrev(), by task: " + this.task);
                                task.getSync().getCurrentThread().interrupt();
                            } catch (NullPointerException ignored) {
                            }
                        }
                    }
                }
            }
        }

        @Override
        public boolean isExecuting() {
            return currentThread != null && !canceled;
        }

        @Override
        public boolean isExecuted() {
            return executed && !canceled;
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public Thread getCurrentThread() {
            return currentThread;
        }

        public int getNotArrivedCount() {
            return notArrivedCount.get();
        }
    }
}
