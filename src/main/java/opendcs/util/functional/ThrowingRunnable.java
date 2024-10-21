package opendcs.util.functional;

@FunctionalInterface
public interface ThrowingRunnable<ErrorType extends Exception>
{
    void run() throws ErrorType;
}
