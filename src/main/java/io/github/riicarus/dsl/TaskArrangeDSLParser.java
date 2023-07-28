package io.github.riicarus.dsl;

import io.github.riicarus.SaggioTask;
import io.github.riicarus.TaskType;
import io.github.riicarus.TransferableTask;
import io.github.riicarus.exception.TaskArrangeDSLGrammarException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Parser for task arrangement dsl. <br/>
 * <p>
 * Parse dsl like: <br/>
 * - task#0 & taskA, taskB, taskC <br/>
 * - taskA#A-D, taskB#B-D, taskC#C-D | taskD <br/>
 *
 * @author Riicarus
 * @create 2023-7-28 7:14
 * @since 1.1.0
 */
public class TaskArrangeDSLParser {

    // reserved words
    public static final String AND_LINKER = " & ";
    public static final String ANY_LINKER = " \\| ";
    public static final String TASK_LINKER = ", ";
    public static final String STATE_LINKER = "#";
    public static final String TIMEOUT_LINKER = "@";

    private SaggioTask saggioTask;

    public TaskArrangeDSLParser(SaggioTask saggioTask) {
        if (saggioTask == null) {
            throw new RuntimeException("SaggioTask of DSLParser can not be null.");
        }
        this.saggioTask = saggioTask;
    }

    public void parse(String str) {
        if (str == null) {
            return;
        }

        HashMap<String, String> items = splitParts(str);
        TaskType type = getTaskType(str);

        List<String> fromTaskNamesWithState = Arrays.asList(items.get("from").split(TASK_LINKER));
        List<String> toTaskNames = Arrays.asList(items.get("to").split(TASK_LINKER));
        int timeout = Integer.parseInt(items.get("timeout"));

        List<FromTaskEntry> fromTaskEntries = splitFromTask(fromTaskNamesWithState);
        List<TransferableTask<?>> toTasks = splitToTask(toTaskNames);

        arrangeTasks(fromTaskEntries, toTasks, type, timeout);
    }

    private HashMap<String, String> splitParts(String str) {
        // example: task#0 & taskA, taskB, taskC @1000

        // split by ANY_LINKER or AND_LINKER
        // res: ["task#0", "taskA, taskB, taskC"]
        HashMap<String, String> items = new HashMap<>();

        int timeoutLinkerIdx = str.lastIndexOf(TIMEOUT_LINKER);
        String timeoutStr = timeoutLinkerIdx == -1 ? "0" : str.substring(timeoutLinkerIdx + 1).trim();
        items.put("timeout", timeoutStr);

        String subStr = timeoutLinkerIdx == -1 ? str : str.substring(0, timeoutLinkerIdx).trim();
        String[] subItems;

        if (subStr.contains(ANY_LINKER)) {
            subItems = subStr.split(ANY_LINKER);
        } else if (subStr.contains(AND_LINKER)) {
            subItems = subStr.split(AND_LINKER);
        } else {
            throw new TaskArrangeDSLGrammarException("You've got a grammar syntax in the task arrangement dsl: Can not find task relation linker.");
        }

        if (subItems.length != 2) {
            throw new TaskArrangeDSLGrammarException("You've got a grammar syntax in the task arrangement dsl: Some needed part(s) of dsl missed.");
        }

        items.put("from", subItems[0]);
        items.put("to", subItems[1]);

        return items;
    }

    private List<FromTaskEntry> splitFromTask(List<String> fromTaskNamesWithState) {
        List<FromTaskEntry> fromTaskEntries = new ArrayList<>();

        for (String taskNameWithStatus : fromTaskNamesWithState) {
            String[] parts = taskNameWithStatus.split(STATE_LINKER);
            if (parts.length != 2) {
                throw new TaskArrangeDSLGrammarException("You've got a grammar syntax in the task arrangement dsl: Some needed part(s) of from task symbol missed.");
            }

            TransferableTask<?> task = saggioTask.getTask(parts[0]);
            if (task == null) {
                throw new TaskArrangeDSLGrammarException("You've got a grammar syntax in the task arrangement dsl: Can not find task of name: " + parts[0] + ".");
            }
            fromTaskEntries.add(new FromTaskEntry(task, parts[1]));
        }

        return fromTaskEntries;
    }

    private List<TransferableTask<?>> splitToTask(List<String> toTaskNames) {
        List<TransferableTask<?>> toTasks = new ArrayList<>();

        for (String toTaskName : toTaskNames) {
            TransferableTask<?> task = saggioTask.getTask(toTaskName);
            if (task == null) {
                throw new TaskArrangeDSLGrammarException("You've got a grammar syntax in the task arrangement dsl: Can not find task of name: " + toTaskName + ".");
            }
            toTasks.add(task);
        }

        return toTasks;
    }

    private void arrangeTasks(List<FromTaskEntry> fromTaskEntries, List<TransferableTask<?>> toTasks, TaskType type, int timeout) {
        if (timeout <= 0) {
            timeout = TransferableTask.DEFAULT_TIMEOUT;
        }

        if (TaskType.ANY.equals(type)) {
            for (TransferableTask<?> toTask : toTasks) {
                for (FromTaskEntry fromTaskEntry : fromTaskEntries) {
                    toTask.any(fromTaskEntry.task, fromTaskEntry.state)
                            .setTimeout(timeout, TimeUnit.MILLISECONDS);
                }
            }
        } else if (TaskType.AND.equals(type)) {
            for (TransferableTask<?> toTask : toTasks) {
                for (FromTaskEntry fromTaskEntry : fromTaskEntries) {
                    toTask.and(fromTaskEntry.task, fromTaskEntry.state)
                            .setTimeout(timeout, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    private TaskType getTaskType(String str) {
        if (str.contains(AND_LINKER)) {
            return TaskType.AND;
        } else if (str.contains(ANY_LINKER)) {
            return TaskType.ANY;
        } else {
            throw new TaskArrangeDSLGrammarException("You've got a grammar syntax in the task arrangement dsl: Can not find task relation linker.");
        }
    }

    public TaskArrangeDSLParser setSaggioTask(SaggioTask saggioTask) {
        this.saggioTask = saggioTask;
        return this;
    }

    private record FromTaskEntry(TransferableTask<?> task, String state) {
    }
}
