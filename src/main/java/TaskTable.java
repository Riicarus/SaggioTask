import java.util.HashMap;
import java.util.HashSet;

/**
 * [FEATURE INFO]<br/>
 *
 * @author Riicarus
 * @create 2023-5-25 20:08
 * @since 1.0.0
 */
public class TaskTable {

    // Map<TaskNode<?>, Map<State, TaskNode<?>>>
    private final HashMap<TaskNode<?>, HashMap<String, HashSet<TaskNode<?>>>> taskMap = new HashMap<>();

    public HashSet<TaskNode<?>> getNext(TaskNode<?> task, String state) {
        if (taskMap.get(task) == null) {
            return null;
        }

        return taskMap.get(task).get(state);
    }

    public void add(TaskNode<?> now, String state, TaskNode<?> next) {
        if (!taskMap.containsKey(now)) {
            HashMap<String, HashSet<TaskNode<?>>> map = new HashMap<>();
            HashSet<TaskNode<?>> nodes = new HashSet<>();
            nodes.add(next);
            map.put(state, nodes);
            taskMap.put(now, map);
            return;
        }

        if (taskMap.get(now).get(state) == null) {
            HashSet<TaskNode<?>> nodes = new HashSet<>();
            nodes.add(next);
            taskMap.get(now).put(state, nodes);
            return;
        }

        taskMap.get(now).get(state).add(next);
    }
}
