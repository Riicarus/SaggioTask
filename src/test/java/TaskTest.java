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

        TaskCondition<String> condition0 = SaggioTask.build("0", () -> new TaskResult<>("success", "0"), System.out::println);
        TaskCondition<String> conditionA = SaggioTask.build("A", () -> new TaskResult<>("success", "A-D1"), System.out::println);
        TaskCondition<String> conditionB = SaggioTask.build("B", () -> new TaskResult<>("success", "B-D1"), System.out::println);
        TaskCondition<String> conditionC = SaggioTask.build("C", () -> new TaskResult<>("success", "C-D1"), System.out::println);
        TaskCondition<String> conditionD = SaggioTask.build("D", () -> new TaskResult<>("success", "D-E1"), System.out::println);
        conditionA.fromAnd(condition0, "0");
        conditionB.fromAnd(condition0, "0");
        conditionC.fromAnd(condition0, "0");
        conditionD.fromAnd(conditionA, "A-D1")
                .fromAnd(conditionB, "B-D1")
                .fromAnd(conditionC, "C-D1");

        SaggioTask.run(condition0, taskExecutor);
    }

}
