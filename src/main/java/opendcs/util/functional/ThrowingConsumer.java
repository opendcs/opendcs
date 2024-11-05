package opendcs.util.functional;

/**
 * Function interface to allow for checked exceptions.
 */
@FunctionalInterface
public interface ThrowingConsumer<T,E extends Exception>
{
    public void accept(T value) throws E;
}
