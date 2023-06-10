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

    private final HashMap<Task<?>, HashMap<String, HashSet<Task<?>>>> pushDownTable = new HashMap<>();

    public HashSet<Task<?>> getNextTasks(Task<?> condition, String state) {
        HashMap<String, HashSet<Task<?>>> nextConditions;

        if ((nextConditions = pushDownTable.get(condition)) != null) {
            return nextConditions.get(state);
        }

        return null;
    }

    public HashMap<String, HashSet<Task<?>>> getNextTasks(Task<?> condition) {
        return pushDownTable.get(condition);
    }

    public void add(Task<?> now, String state, Task<?> then) {
        HashMap<String, HashSet<Task<?>>> nextConditions;
        HashSet<Task<?>> conditionsOfState;

        if ((nextConditions = pushDownTable.get(now)) == null) {
            nextConditions = new HashMap<>();
            conditionsOfState = new HashSet<>();
            conditionsOfState.add(then);
            nextConditions.put(state, conditionsOfState);
            pushDownTable.put(now, nextConditions);
            return;
        }

        if ((conditionsOfState = nextConditions.get(state)) == null) {
            conditionsOfState = new HashSet<>();
            nextConditions.put(state, conditionsOfState);
            return;
        }

        conditionsOfState.add(then);
    }

}
