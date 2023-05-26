/**
 * [FEATURE INFO]<br/>
 *
 * @author Riicarus
 * @create 2023-5-25 20:02
 * @since 1.0.0
 */
public enum TaskType {
    /*
    * serial + serial = and
    * serial + and = and
    * serial + any = multi
    * serial + multi = multi
    *
    * and + serial = and
    * and + and = and
    * and + any = multi
    * and + multi = multi
    *
    * any + serial = multi
    * any + and = multi
    * and + any = any
    * any + multi = multi
    *
    * multi + * = multi
    * */
    SERIAL(0),
    AND(1),
    ANY(2),
    MULTI(3),
    ;

    private final int value;

    TaskType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
