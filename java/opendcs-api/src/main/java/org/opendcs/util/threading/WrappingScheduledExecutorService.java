package org.opendcs.util.threading;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

/**
 * Allow wrapping additional 
 * WrappingScheduledExecutorService
 */
public abstract class WrappingScheduledExecutorService extends WrappingExecutorService implements ScheduledExecutorService
{

    protected WrappingScheduledExecutorService(ScheduledExecutorService service)
    {
        super(service);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit)
    {
        return ((ScheduledExecutorService)this.delegate).schedule(command, delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit)
    {
        return ((ScheduledExecutorService)this.delegate).schedule(callable, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit)
    {
        return ((ScheduledExecutorService)this.delegate).scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit)
    {
        return ((ScheduledExecutorService)this.delegate).scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }    

    public static class ErrorLoggingScheduledExecutorService extends WrappingScheduledExecutorService
    {
        private final Logger log; // NOSONAR. Intentionally non-static, should log as the usage class

        public ErrorLoggingScheduledExecutorService(ScheduledExecutorService delegate, Logger log)
        {
            super(delegate);
            this.log = log;
        }

        @Override
        public void wrappedExecute(Runnable command)
        {
            try
            {
                command.run();
            }
            catch (Throwable ex) // NOSONAR. Preventing error swallowing, must catching everything.
            {
                log.atError().setCause(ex).log("Error running task.");
                throw ex;
            }
        }
        
    }
}
