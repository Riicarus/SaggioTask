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

        Task<String> task0 = saggioTask.build("0", (ctx) -> new TaskResult<>("success", "0"), (res, ctx) -> System.out.println(res));
        Task<String> taskA = saggioTask.build("A", (ctx) -> new TaskResult<>("success", "A-D1"), (res, ctx) -> System.out.println(res));
        Task<String> taskB = saggioTask.build("B", (ctx) -> {
            Thread.sleep(4000);
            return new TaskResult<>("success", "B-D1");
        }, (res, ctx) -> System.out.println(res));
        Task<String> taskC = saggioTask.build("C", (ctx) -> {
            Thread.sleep(4000);
            return new TaskResult<>("success", "C-DE1");
        }, (res, ctx) -> System.out.println(res));
        Task<String> taskD = saggioTask.build("D", (ctx) -> new TaskResult<>("success", "D-E1"), (res, ctx) -> System.out.println(res));
        taskA.fromAnd(task0, "0");
        taskB.fromAnd(task0, "0");
        taskC.fromAnd(task0, "0");
        taskD.fromAny(taskA, "A-D1")
                .fromAny(taskB, "B-D1")
                .fromAny(taskC, "C-DE1")
                .setTimeout(5000, TimeUnit.MILLISECONDS);

        Task<String> taskE = saggioTask.build("E", (ctx) -> new TaskResult<>("success", "E-F1"), (res, ctx) -> System.out.println(res));
        taskE.fromAnd(taskC, "C-DE1")
                .fromAnd(taskD, "D-E1");

        TaskContext context = new TaskContext();
        context.getConfig().setTimeout(1000, TimeUnit.MILLISECONDS);
//        context.getConfig().setStopIfNextStopped(false);
        context.getConfig().setStopIfNextStopped(false);

        List<Task<?>> startTasks = new ArrayList<>();
        startTasks.add(task0);
        saggioTask.run(startTasks, taskExecutor, context);
    }

}
