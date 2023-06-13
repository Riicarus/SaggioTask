/**
 * [FEATURE INFO]<br/>
 *
 * @author Riicarus
 * @create 2023-6-12 23:04
 * @since 1.0.0
 */
public interface Transferable<T> {

    T and(T prev, String state);

    T any(T prev, String state);

}
