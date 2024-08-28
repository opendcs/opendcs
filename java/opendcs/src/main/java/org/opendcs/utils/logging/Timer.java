package org.opendcs.utils.logging;

import java.time.Duration;
import java.time.Instant;

import opendcs.util.functional.ThrowingRunnable;

public class Timer 
{
    
    private Timer()
    {
    }

    public static <ErrorType extends Exception> Duration elapsedTime(ThrowingRunnable<ErrorType> task) throws ErrorType
    {        
        Instant start = Instant.now();
        task.run();
        Instant end = Instant.now();
        return Duration.between(start, end);
    }
}
