package io.github.riicarus.function;

import io.github.riicarus.TaskContext;
import io.github.riicarus.TaskResult;

/**
 * [FEATURE INFO]<br/>
 *
 * @author Riicarus
 * @create 2023-5-25 20:01
 * @since 1.0.0
 */
@FunctionalInterface
public interface TaskFunction<T> {

    TaskResult<T> execute(TaskContext context) throws InterruptedException;
}
