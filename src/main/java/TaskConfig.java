import java.util.concurrent.TimeUnit;

/**
 * [FEATURE INFO]<br/>
 * config for a task
 *
 * @author Riicarus
 * @create 2023-6-8 19:17
 * @since 1.0.0
 */
public class TaskConfig {

    private int timeout = 3000;
    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    private boolean stopIfNextStopped = true;

    public int getTimeout() {
        return timeout;
    }

    public TaskConfig setTimeout(int timeout, TimeUnit timeUnit) {
        this.timeout = timeout;
        this.timeUnit = timeUnit;
        return this;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    @SuppressWarnings("all")
    public boolean isStopIfNextStopped() {
        return stopIfNextStopped;
    }

    public void setStopIfNextStopped(boolean stopIfNextStopped) {
        this.stopIfNextStopped = stopIfNextStopped;
    }
}
