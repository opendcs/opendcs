package opendcs.util.logging;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JulUtils {
    public static final int BEFORE_CUR_THREAD_STACK_CALL = 2;
    /**
     * Log a stack strace element from a given starting place.
     * 
     * NOTE since we don't know where this is called we acquire the trace ourself 
     * so we don't have to worry about any logic of what goes to what thread.
     * @param log an open Logger handle
     * @param level what level to log at
     * @param elements the actual stack trace element
     * @param startIndex where in the trace to start from
     */
    public static void logStackTrace(Logger log, Level level, StackTraceElement []elements, int startIndex)
    {
        Objects.requireNonNull(log,"We can't do anything without a valid Logger.");
        Objects.requireNonNull(level, "A java.util.logging Level must be provided.");
        Objects.requireNonNull(elements, "A valid (empty is valid) stack trace should be provided to this function.");
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < elements.length; i++)
        {
            sb.append(elements[i].toString()).append(System.lineSeparator());
        }
        log.log(level,sb.toString());
    }
}
