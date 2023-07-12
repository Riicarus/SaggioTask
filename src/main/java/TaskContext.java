import java.util.HashMap;

/**
 * [FEATURE INFO]<br/>
 * Context for a saggio task. <br/>
 * The context does not provide any concurrent ensure, so this may need some user's work.
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
