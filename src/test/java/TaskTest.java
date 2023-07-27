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
 * [FEATURE INFO]<br/>
 *
 * @author Riicarus
 * @create 2023-5-26 3:41
 * @since 1.0.0
 */
public class TaskTest {

    public static void main(String[] args) {
        ThreadPoolExecutor taskExecutor = new ThreadPoolExecutor(20, 50, 100, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(10));

        SaggioTask saggioTask = new SaggioTask();

        TransferableTask<String> task0 = saggioTask.build("0",
                (ctx) -> new TaskResult<>("success", "0"),
                (res, ctx) -> System.out.println(res));
        TransferableTask<String> taskA = saggioTask.build("A",
                (ctx) -> new TaskResult<>("success", "A-D1"),
                (res, ctx) -> System.out.println(res));
        TransferableTask<String> taskB = saggioTask.build("B",
                (ctx) -> {
                    Thread.sleep(5000);
                    return new TaskResult<>("success", "B-D1");
                },
                (res, ctx) -> System.out.println(res));
        TransferableTask<String> taskC = saggioTask.build("C",
                (ctx) -> {
                    Thread.sleep(4000);
                    return new TaskResult<>("success", "C-DE1");
                },
                (res, ctx) -> System.out.println(res));
        TransferableTask<String> taskD = saggioTask.build("D",
                (ctx) -> new TaskResult<>("success", "D-E1"),
                (res, ctx) -> System.out.println(res));
        taskA.and(task0, "0");
        taskB.and(task0, "0");
        taskC.and(task0, "0");
        taskD.any(taskA, "A-D1")
                .any(taskB, "B-D1")
                .any(taskC, "C-DE1")
                .setTimeout(5000, TimeUnit.MILLISECONDS);

        TransferableTask<String> taskE = saggioTask.build("E", (ctx) -> new TaskResult<>("success", "E-F1"), (res, ctx) -> System.out.println(res));
        taskE.and(taskC, "C-DE1")
                .and(taskD, "D-E1")
                .setTimeout(4, TimeUnit.SECONDS);

        TaskContext context = new TaskContext();
        context.getConfig().setTimeout(1000, TimeUnit.MILLISECONDS);
        context.getConfig().setRecursivelyStop(true);

        List<TransferableTask<?>> startTasks = new ArrayList<>();
        startTasks.add(task0);
        saggioTask.run(startTasks, taskExecutor, context);
    }

}
