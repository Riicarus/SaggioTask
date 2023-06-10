/**
 * [FEATURE INFO]<br/>
 * type of a condition
 *
 * @author Riicarus
 * @create 2023-6-1 2:36
 * @since 1.0.0
 */
public enum TaskType {

    ANY(0),
    AND(1);

    private final int value;

    TaskType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
