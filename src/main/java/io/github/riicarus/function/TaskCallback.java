package io.github.riicarus.function;

import io.github.riicarus.TaskContext;
import io.github.riicarus.TaskResult;

/**
 * [FEATURE INFO]<br/>
 *
 * @author Riicarus
 * @create 2023-5-25 20:24
 * @since 1.0.0
 */
@FunctionalInterface
public interface TaskCallback<T> {

    void execute(TaskResult<T> result, TaskContext context);

}
