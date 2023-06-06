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

        TaskCondition<String> condition0 = saggioTask.build("0", () -> new TaskResult<>("success", "0"), System.out::println);
        TaskCondition<String> conditionA = saggioTask.build("A", () -> new TaskResult<>("success", "A-D1"), System.out::println);
        TaskCondition<String> conditionB = saggioTask.build("B", () -> {
            Thread.sleep(4000);
            return new TaskResult<>("success", "B-D1");
        }, System.out::println);
        TaskCondition<String> conditionC = saggioTask.build("C", () -> {
            Thread.sleep(4000);
            return new TaskResult<>("success", "C-D1");
        }, System.out::println);
        TaskCondition<String> conditionD = saggioTask.build("D", () -> new TaskResult<>("success", "D-E1"), System.out::println);
        conditionA.fromAnd(condition0, "0");
        conditionB.fromAnd(condition0, "0");
        conditionC.fromAnd(condition0, "0");
        conditionD.fromAnd(conditionA, "A-D1")
                .fromAnd(conditionB, "B-D1")
                .fromAnd(conditionC, "C-D1");

        saggioTask.run(condition0, taskExecutor);
    }

}
