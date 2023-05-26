/**
 * [FEATURE INFO]<br/>
 *
 * @author Riicarus
 * @create 2023-5-25 21:27
 * @since 1.0.0
 */
public class TaskResult<T> {

    private T t;

    private String state;

    public TaskResult(T t, String state) {
        this.t = t;
        this.state = state;
    }

    public T getT() {
        return t;
    }

    public void setT(T t) {
        this.t = t;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "TaskResult{" +
                "t=" + t +
                ", state='" + state + '\'' +
                '}';
    }
}
