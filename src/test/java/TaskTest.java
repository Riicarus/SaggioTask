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
        ThreadPoolExecutor workExecutor = new ThreadPoolExecutor(20, 50, 100, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(10));
        ThreadPoolExecutor taskExecutor = new ThreadPoolExecutor(20, 50, 100, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(10));

        TaskScheduler scheduler = new TaskScheduler();
        TaskNode<String> nodeA = scheduler.begin(() -> new TaskResult<>("taskA", "A1"), System.out::println);
        TaskNode<String> nodeB = scheduler.begin(() -> new TaskResult<>("taskB", "B1"), System.out::println);
        TaskNode<String> nodeC = scheduler.begin(() -> new TaskResult<>("taskC", "C1"), System.out::println);
        TaskNode<String> nodeD = scheduler.and(nodeA, "A1", () -> new TaskResult<>("taskD", "D1"), System.out::println);
        scheduler.and(nodeB, "B1", nodeD);
        scheduler.any(nodeC, "C1", nodeD);

//        TaskNode<String> nodeE = scheduler.serial(nodeD, "D1", () -> new TaskResult<>("taskE", "E1"), System.out::println);
//        TaskNode<String> nodeF = scheduler.serial(nodeE, "E1", () -> new TaskResult<>("taskF", "F1"), System.out::println);
//        TaskNode<String> nodeG = scheduler.serial(nodeE, "E1", () -> new TaskResult<>("taskG", "G1"), System.out::println);
//        TaskNode<String> nodeH = scheduler.serial(nodeE, "E1", () -> new TaskResult<>("taskH", "H1"), System.out::println);
//        TaskNode<String> nodeI = scheduler.serial(nodeE, "E1", () -> new TaskResult<>("taskI", "I1"), System.out::println);

        scheduler.work(workExecutor, taskExecutor);
    }

}
