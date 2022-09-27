package opendcs.util.functional;

/**
 * Function interval to allow for checked exceptions.
 */
@FunctionalInterface
public interface BiThrowingConsumer<T,E extends Exception,R>
{
    public void accept(T value, R item) throws E;
}