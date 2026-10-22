package org.opendcs.database.apps;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.apps.exceptions.OpenDcsApplicationException;
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
    private final ExecutorService appExecutor;

    /**
     * ExecutorService for the tasks of this application.
     * 
     */
    protected final ExecutorService taskExecutor;

    private Future<?> loopFuture;
    private long shutdownWait;
    private AtomicBoolean run = new AtomicBoolean(true);

    protected StandaloneApp(AppBuilder<?,?> builder)
    {
        this.db = builder.db;
        this.shutdownWait = builder.shutdownWait;
        this.appExecutor = builder.appExecutor != null ? builder.appExecutor : Executors.newFixedThreadPool(2);
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

    /**
     * Start applicated on the even appExecutor
     */
    public void start()
    {
        loopFuture = CompletableFuture.runAsync(this::init, appExecutor)
                                      .thenRun(() -> {
                                        while (run.get())
                                        {
                                            try
                                            {
                                                loop();
                                                Thread.sleep(200);                                                
                                            }
                                            catch (InterruptedException ex)
                                            {
                                                Thread.currentThread().interrupt();
                                                break;
                                            }
                                        }
                                      });
        
    }
    
    /**
     * Terminate the application
     * @return if the termination was clean
     * @throws TimeoutException 
     * @throws ExecutionException 
     * @throws InterruptedException 
     */
    public boolean stop()
    {
        run.set(false);
        try {
            loopFuture.get(shutdownWait, TimeUnit.MILLISECONDS);
            return true;
        }
        catch (InterruptedException | ExecutionException | TimeoutException ex)
        {
            log.atError()
               .setCause(ex)
               .log("Application loop did not terminate in 30 seconds. Terminating Forcibly.");
            loopFuture.cancel(true);
            return false;
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
        private ExecutorService appExecutor;
        private ExecutorService taskExecutor;
        private long shutdownWait = 30000;

        protected abstract B self();

        public B withDatabase(OpenDcsDatabase db)
        {
            this.db = db;
            return self();
        }

        public B withAppExecutor(ExecutorService appExecutor)
        {
            this.appExecutor = appExecutor;
            return self();
        }

        public B withShutdownWait(long amount, TimeUnit unit)
        {
            this.shutdownWait = unit.toMillis(amount);
            return self();
        }

        public B withTaskExecutor(ExecutorService taskExecutor)
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
