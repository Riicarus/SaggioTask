/**
 * [FEATURE INFO]<br/>
 * type of a condition
 *
 * @author Riicarus
 * @create 2023-6-1 2:36
 * @since 1.0.0
 */
public enum ConditionType {

    ANY(0),
    AND(1);

    private final int value;

    ConditionType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
