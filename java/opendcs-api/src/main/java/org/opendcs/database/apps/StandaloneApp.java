package org.opendcs.database.apps;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.apps.exceptions.OpenDcsApplicationException;
import org.opendcs.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Replaces {@link decodes.tsdb.TsDbAppTemplate} as a new base class for applications.
 *
 * applicationExecutor and taskExecutor <b>can</b> the same instance.
 * if taskExector is not set this will be done.
 *
 * If appExecutor is not set a default pool will be used.
 *
 * The design is such that init, loop, and terminate do <b>NOT</b> need to synchronize objects.
 * if one is running the anothers cannot be. 
 * 
 * StandaloneApp
 */
public abstract class StandaloneApp
{
    private static final Logger log = LoggerFactory.getLogger(StandaloneApp.class);
    /**
     * Database instance for this applciation.
     */
    protected final OpenDcsDatabase db;

    /**
     * ExecutorService for driving the application loop
     */
    private final ScheduledExecutorService appExecutor;

    /**
     * ExecutorService for the tasks of this application.
     *
     */
    protected final ScheduledExecutorService taskExecutor;

    private ScheduledFuture<?> loopFuture;
    private final long shutdownWait;
    private final long loopWait;

    protected StandaloneApp(AppBuilder<?,?> builder)
    {
        this.db = builder.db;
        this.loopWait = builder.loopWait;
        this.shutdownWait = builder.shutdownWait;
        this.appExecutor = builder.appExecutor != null ? builder.appExecutor : Executors.newScheduledThreadPool(2);
        this.taskExecutor = builder.taskExecutor != null ? builder.taskExecutor : this.appExecutor;
    }

    /**
     * One time initialization of the application
     */
    public abstract void init();

    /**
     * Application task loop. Called on a thread from the provided executor.
     * method called every 10 milliseconds and call handles cancel
     * state and thread interruption.
     *
     * Application loop should perform any required operations quickly and return. Most likely passing
     * operations on to the task executor.
     *
     * The only restriction is that {@link stop} will wait up to {@link AppBuilder#shutdownWait} milliseconds
     * and then call {@link java.util.concurrent.Future#cancel(true)} to force termination.
     */
    public abstract void loop();

    public abstract void terminate();

    public boolean isRunning()
    {
        return loopFuture != null ? !loopFuture.isDone() : false;
    }

    /**
     * Start applicated on the even appExecutor
     * @return Result of true if success, otherwise the cause of failure.
     */
    public Result<Boolean, OpenDcsApplicationException> start()
    {
        if (loopFuture != null && !loopFuture.isDone())
        {
            return Result.failure(new OpenDcsApplicationException("Application is already running."));
        }
        try
        {
            loopFuture = CompletableFuture
                    .runAsync(this::init, appExecutor)
                    .handleAsync((v, ex) ->
                    {
                        if (ex != null)
                        {
                            log.atError().setCause(ex).log("Error in Application loop. Terminating.");
                            return null;
                        }
                        else
                        {
                            return appExecutor.scheduleWithFixedDelay(
                                () ->
                                {
                                    try
                                    {
                                        this.loop();
                                    }
                                    catch (Throwable ex2) // NOSONAR. We have to catch everything here.
                                    {
                                        log.atError().setCause(ex).log("Error in application loop.");
                                        throw ex2;
                                    }
                                },0,                     loopWait, TimeUnit.MILLISECONDS);
                        }
                    }, appExecutor)
                    .exceptionally(ex ->
                    {
                        log.atError().setCause(ex).log("Failed to schedule application loop.");
                        return null;
                    })
                    .get();
            if (loopFuture == null)
            {
                return Result.failure(new OpenDcsApplicationException("Unable to schedule loop."));
            }
        }
        catch (ExecutionException |InterruptedException ex)
        {
            return Result.failure(new OpenDcsApplicationException("Unable to start application."));
        }
        return Result.success(true);
    }

    /**
     * Terminate the application. Regardless of failure cause application should have been
     * terminated completely. However, as we cannot gaurantee that the applications terminate function
     * finished it's work, and there could be left over tasks left in the provided executors
     * it is possible remaining work will be performed.
     *
     * Stop can only garauntee that the loop method is no longer running.
     * @return Result of true if the termination was clean. Otherwise the cause of failure.
     */
    public Result<Boolean, OpenDcsApplicationException> stop()
    {
        Future<?> terminateFuture = null;
        try
        {
            loopFuture.cancel(true); // disable the application loop
            try
            {
                loopFuture.get(shutdownWait, TimeUnit.MILLISECONDS);
            }
            catch (CancellationException ex)
            {
                /* this is expected */
            }
            terminateFuture = taskExecutor.submit(this::terminate);
            terminateFuture.get(shutdownWait, TimeUnit.MILLISECONDS);
            return Result.success(true);
        }
        catch (InterruptedException | ExecutionException | TimeoutException ex)
        {
            if (terminateFuture != null)
            {
                terminateFuture.cancel(true);
            }
            return Result.failure(
                new OpenDcsApplicationException(
                    String.format(
                        "Application loop did not terminate in %f seconds. Terminating Forcibly.",
                        shutdownWait/1000.0),
                    ex));
        }
    }

    /**
     * Base builder to handle required elements. Applications can and should
     * expand from this builder with additional information and objects require
     * for their operations
     * AppBuilder
     * @param <T> Application Class
     * @param <B> Application Builder Class
     */
    public static abstract class AppBuilder<T extends StandaloneApp, B extends AppBuilder<T,B>>
    {
        private OpenDcsDatabase db;
        private ScheduledExecutorService appExecutor;
        private ScheduledExecutorService taskExecutor;
        private long shutdownWait = 30000;
        private long loopWait = 200;

        protected abstract B self();

        public B withDatabase(OpenDcsDatabase db)
        {
            this.db = db;
            return self();
        }

        /**
         * Scheduled Executor Service pool for the application. If a task executor service is not provided
         * the minimum size of the appExector pool should be 2.
         * @param appExecutor
         * @return
         */
        public B withAppExecutor(ScheduledExecutorService appExecutor)
        {
            this.appExecutor = appExecutor;
            return self();
        }

        /**
         * How long to way after calling {@link StandaloneApp#terminate()} before forcibly
         * terminating the terminate execution.
         * @param amount
         * @param unit
         * @return
         */
        public B withShutdownWait(long amount, TimeUnit unit)
        {
            this.shutdownWait = unit.toMillis(amount);
            return self();
        }

        /**
         * How long to wait between executions of the application main loop.
         * Default is 200 miliseconds. A given application builder may use a different default.
         * @param amount amount of time unit
         * @param unit the time unit
         * @return
         */
        public B withLoopWait(long amount, TimeUnit unit)
        {
            this.loopWait = unit.toMillis(amount);
            return self();
        }

        /**
         * ScheduledExectorService for tasks. Will be used for both Scheduled and non-scheduled tasked.
         * Should be sized according to needs and resource ability.
         * @param taskExecutor
         * @return
         */
        public B withTaskExecutor(ScheduledExecutorService taskExecutor)
        {
            this.taskExecutor = taskExecutor;
            return self();
        }

        /**
         * Construct new instance of StandaloneApp of type T.
         *
         * NOTE: The constructor of Standalone app will handle
         * default executors if none are set. See the class
         * behavior to determine if the default behavior is exceptable
         * to the application operations and set values as required
         * in your build method implementation.
         *
         *
         * @return
         * @throws OpenDcsApplicationException if the configuration of the builder is invalid
         */
        public abstract T build() throws OpenDcsApplicationException;
    }
}
