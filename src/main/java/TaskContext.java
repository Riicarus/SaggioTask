import java.util.HashMap;

/**
 * [FEATURE INFO]<br/>
 * context for a saggio task
 *
 * @author Riicarus
 * @create 2023-6-8 19:17
 * @since 1.0.0
 */
public class TaskContext {

    private final TaskConfig config = new TaskConfig();

    private final HashMap<String, Object> data = new HashMap<>();

    public TaskContext() {
    }

    public TaskConfig getConfig() {
        return config;
    }

    public Object get(String key) {
        return data.get(key);
    }

    public void set(String key, Object value) {
        data.put(key, value);
    }
}
