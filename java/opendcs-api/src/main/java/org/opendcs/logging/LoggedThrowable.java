package org.opendcs.logging;

import java.util.Collections;
import java.util.List;

public final class LoggedThrowable
{
    public final String className;
    public final String message;
    public final List<ThrowableStep> steps;
    public final LoggedThrowable cause;

    public LoggedThrowable(String className, String message, List<ThrowableStep> steps, LoggedThrowable cause)
    {
        this.className = className;
        this.message = message;
        this.steps = Collections.unmodifiableList(steps);
        this.cause = cause;
    }

    public static class ThrowableStep
    {
        public final String className;
        public final String methodName;
        public final String fileName;
        public final int lineNumber;

        public ThrowableStep(String className, String methodName, String fileName, int lineNumber)
        {
            this.className = className;
            this.methodName = methodName;
            this.fileName = fileName;
            this.lineNumber = lineNumber;
        }
    }
}
