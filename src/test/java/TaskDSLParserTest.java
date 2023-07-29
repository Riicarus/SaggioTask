import io.github.riicarus.SaggioTask;
import io.github.riicarus.TaskContext;
import io.github.riicarus.TaskResult;
import io.github.riicarus.TransferableTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Riicarus
 * @create 2023-7-28 8:02
 * @since 1.1.0
 */
public class TaskDSLParserTest {

    public static void main(String[] args) {
        SaggioTask saggioTask = new SaggioTask();

        TransferableTask<String> task = saggioTask.build("task",
                (ctx) -> new TaskResult<>("success", "0"),
                (res, ctx) -> System.out.println(res));
        TransferableTask<String> task0 = saggioTask.build("task0",
                (ctx) -> new TaskResult<>("success", "aa"),
                (res, ctx) -> System.out.println(res));
        TransferableTask<String> taskA = saggioTask.build("taskA",
                (ctx) -> new TaskResult<>("success", "A-D1"),
                (res, ctx) -> System.out.println(res));
        TransferableTask<String> taskB = saggioTask.build("taskB",
                (ctx) -> {
                    Thread.sleep(5000);
                    return new TaskResult<>("success", "B-D1");
                },
                (res, ctx) -> System.out.println(res));
        TransferableTask<String> taskC = saggioTask.build("taskC",
                (ctx) -> {
                    Thread.sleep(4000);
                    return new TaskResult<>("success", "C-DE1");
                },
                (res, ctx) -> System.out.println(res));
        TransferableTask<String> taskD = saggioTask.build("taskD",
                (ctx) -> new TaskResult<>("success", "D-E1"),
                (res, ctx) -> System.out.println(res));

        saggioTask.arrange("task#0, task0#aa & taskA, taskB, taskC @1000");

        ThreadPoolExecutor taskExecutor = new ThreadPoolExecutor(20, 50, 100, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(10));
        TaskContext context = new TaskContext();
        context.getConfig().setRecursivelyStop(true);

        List<TransferableTask<?>> startTasks = new ArrayList<>();
        startTasks.add(task0);
        startTasks.add(task);
        saggioTask.run(startTasks, taskExecutor, context);
    }

}
