package opendcs.util.functional;

/**
 * function interface to allow returning result with a checked exception.
 */
@FunctionalInterface
public interface ThrowingFunction<T,R,E extends Exception> {    
    public R accept(T value) throws E;
}
