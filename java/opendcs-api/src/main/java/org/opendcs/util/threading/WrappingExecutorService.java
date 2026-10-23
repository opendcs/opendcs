package org.opendcs.util.threading;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Utility class to add additional behavior to ExecutorServices, such as logging.
 * WrappingExecutorService
 */
public abstract class WrappingExecutorService extends AbstractExecutorService
{
    protected final ExecutorService delegate;

    public WrappingExecutorService(ExecutorService service)
    {
        
        this.delegate = service;
    }

    @Override
    public void execute(Runnable command)
    {
        delegate.execute(() -> wrappedExecute(command));
    }

    /**
     * Actual wrapper to run. Implementors should call
     * command.run() directly within their behavior wrapper. The implemented execute of this class
     * will call wrappedExecute(command) in a runnable passed to the delgate.
     * @param command additional behavior
     */
    public abstract void wrappedExecute(Runnable command);

    /**    (non-Javadoc)
     * 
     * @see java.util.concurrent.ExecutorService#shutdown()
     */
    @Override
    public void shutdown()
    {
        this.delegate.shutdown();
    }

    /**    (non-Javadoc)
     * 
     * @see java.util.concurrent.ExecutorService#shutdownNow()
     */
    @Override
    public List<Runnable> shutdownNow()
    {
        return this.delegate.shutdownNow();
    }

    /**    (non-Javadoc)
     * 
     * @see java.util.concurrent.ExecutorService#isShutdown()
     */
    @Override
    public boolean isShutdown()
    {
        return this.delegate.isShutdown();
    }

    /**    (non-Javadoc)
     * 
     * @see java.util.concurrent.ExecutorService#isTerminated()
     */
    @Override
    public boolean isTerminated()
    {
        return this.delegate.isTerminated();
    }

    /**    (non-Javadoc)
     * 
     * @see java.util.concurrent.ExecutorService#awaitTermination(long, java.util.concurrent.TimeUnit)
     */
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException
    {
        return this.delegate.awaitTermination(timeout, unit);
    }

    
    
}
