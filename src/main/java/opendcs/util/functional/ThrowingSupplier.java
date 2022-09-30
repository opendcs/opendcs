package opendcs.util.functional;

/**
 * function interface to allow returning result with a checked exception.
 */
@FunctionalInterface
public interface ThrowingSupplier<R,E extends Exception> {    
    public R get() throws E;
}
