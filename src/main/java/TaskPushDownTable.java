import java.util.HashMap;
import java.util.HashSet;

/**
 * [FEATURE INFO]<br/>
 * push down table of tasks.
 *
 * @author Riicarus
 * @create 2023-6-1 2:45
 * @since 1.0.0
 */
public class TaskPushDownTable {

    private final HashMap<TransferableTask<?>, HashMap<String, HashSet<TransferableTask<?>>>> pushDownTable = new HashMap<>();

    public HashSet<TransferableTask<?>> getNextTasks(TransferableTask<?> task, String state) {
        HashMap<String, HashSet<TransferableTask<?>>> nextTasks;

        if ((nextTasks = pushDownTable.get(task)) != null) {
            return nextTasks.get(state);
        }

        return null;
    }

    public HashMap<String, HashSet<TransferableTask<?>>> getNextTasks(TransferableTask<?> task) {
        return pushDownTable.get(task);
    }

    public void add(TransferableTask<?> now, String state, TransferableTask<?> then) {
        HashMap<String, HashSet<TransferableTask<?>>> nextTasks;
        HashSet<TransferableTask<?>> tasksOfState;

        if ((nextTasks = pushDownTable.get(now)) == null) {
            nextTasks = new HashMap<>();
            tasksOfState = new HashSet<>();
            tasksOfState.add(then);
            nextTasks.put(state, tasksOfState);
            pushDownTable.put(now, nextTasks);
            return;
        }

        if ((tasksOfState = nextTasks.get(state)) == null) {
            tasksOfState = new HashSet<>();
            nextTasks.put(state, tasksOfState);
            return;
        }

        tasksOfState.add(then);
    }

}
