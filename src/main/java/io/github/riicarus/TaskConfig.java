package io.github.riicarus;

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

    /**
     * Indicating that whether the task should be recursively stopped when one task fails or meets a timeout.
     */
    private boolean recursivelyStop = true;

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
    public boolean isRecursivelyStop() {
        return recursivelyStop;
    }

    public TaskConfig setRecursivelyStop(boolean recursivelyStop) {
        this.recursivelyStop = recursivelyStop;
        return this;
    }
}
