package org.opendcs.logging;

import java.util.ArrayList;
import java.util.List;

public final record LoggedThrowable(String className, String message, List<ThrowableStep> steps, LoggedThrowable cause)
{

    public static record ThrowableStep(String className, String methodName, String fileName, int lineNumber)
    {

    }

    public static LoggedThrowable from(Throwable ex)
    {
        if (ex == null)
        {
            return null;// end of chain.
        }
        return new LoggedThrowable(ex.getClass().getName(), ex.getMessage(),
                                   map(ex.getStackTrace()), from(ex.getCause()));
    }

    private static List<LoggedThrowable.ThrowableStep> map(StackTraceElement[] stackTrace)
    {
        ArrayList<LoggedThrowable.ThrowableStep> steps = new ArrayList<>();
        for(StackTraceElement ste: stackTrace)
        {
            steps.add(
                new LoggedThrowable.ThrowableStep(
                    ste.getClassName(), ste.getMethodName(),
                    ste.getFileName(), ste.getLineNumber()));
        }
        return steps;
    }
}
