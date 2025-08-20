package opendcs.util.functional;

/**
 * Functional interface that allows throwing an exception
 */
@FunctionalInterface
public interface ThrowingSupplier<ReturnType,ErrorType extends Exception> {
    ReturnType get() throws ErrorType;
}
