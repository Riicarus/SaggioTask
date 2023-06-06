import java.util.HashMap;
import java.util.HashSet;

/**
 * [FEATURE INFO]<br/>
 * push down table of task condition.
 *
 * @author Riicarus
 * @create 2023-6-1 2:45
 * @since 1.0.0
 */
public class TaskPushDownTable {

    private final HashMap<TaskCondition<?>, HashMap<String, HashSet<TaskCondition<?>>>> pushDownTable = new HashMap<>();

    public HashSet<TaskCondition<?>> getNextConditions(TaskCondition<?> condition, String state) {
        HashMap<String, HashSet<TaskCondition<?>>> nextConditions;

        if ((nextConditions = pushDownTable.get(condition)) != null) {
            return nextConditions.get(state);
        }

        return null;
    }

    public void add(TaskCondition<?> now, String state, TaskCondition<?> then) {
        HashMap<String, HashSet<TaskCondition<?>>> nextConditions;
        HashSet<TaskCondition<?>> conditionsOfState;

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
