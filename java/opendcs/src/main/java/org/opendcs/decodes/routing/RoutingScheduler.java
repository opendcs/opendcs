package org.opendcs.decodes.routing;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDataRuntimeException;
import org.opendcs.database.apps.StandaloneApp;
import org.opendcs.database.apps.exceptions.OpenDcsApplicationException;
import org.opendcs.database.dai.LoadingAppDao;
import org.opendcs.database.dai.RoutingSpecDao;
import org.opendcs.database.dai.ScheduleEntryDao;
import org.opendcs.database.dai.ScheduleEntryStatusDao;
import org.opendcs.util.Result;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.consumer.DataConsumer;
import decodes.consumer.DataConsumerException;
import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.db.InvalidDatabaseException;
import decodes.db.ScheduleEntry;
import decodes.decoder.DecodedMessage;
import decodes.sql.DbKey;

/**
 * Handles scheduled and real-time schedule Routing Specs.
 *
 * Does not currently handle cli-oneshot routing specs. We are currently focused on the primary logic.
 *
 * RoutingScheduler
 */
public final class RoutingScheduler extends StandaloneApp
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    private final RoutingSpecDao specDao;
    private final ScheduleEntryDao scheduleDao;
    private final ScheduleEntryStatusDao statusDao;
    private final LoadingAppDao appDao;
    private final String schedulerAppName;
    private final Map<String, Future<?>> activeTasks = new HashMap<>();

    private RoutingScheduler(RoutingSchedulerBuilder builder)
    {
        super(builder);
        this.schedulerAppName = builder.schedulerAppName;
        this.specDao = db.getDao(RoutingSpecDao.class).orElseThrow(); // TODO: well defined exceptions
        this.scheduleDao = db.getDao(ScheduleEntryDao.class).orElseThrow();
        this.statusDao = db.getDao(ScheduleEntryStatusDao.class).orElseThrow();
        this.appDao = db.getDao(LoadingAppDao.class).orElseThrow();
    }

    @Override
    public void init()
    {
        log.info("Starting scheduler for application name '{}'", schedulerAppName);
    }

    /**    (non-Javadoc)
     * Process the scheduled on the defined interval/wait for this application.
     * @see org.opendcs.database.apps.StandaloneApp#loop()
     */
    @Override
    public void loop()
    {
        try (var tx = db.newTransaction())
        {
            var app = appDao.getByName(tx, schedulerAppName).orElseThrow();
            var schedules = scheduleDao.getAll(tx, -1, -1, app);
            schedules.forEach(entry ->
            {
                if (entry.isEnabled())
                {
                    enableEntry(entry);
                }

                // setup initial entries.
                // what's the logic here?
            });
        }
        catch (OpenDcsDataException ex)
        {
            throw new OpenDcsDataRuntimeException(ex);
        }
    }

    @Override
    public void terminate()
    {
        activeTasks.forEach((k,v) ->
        {
            v.cancel(true);
        });
    }

    /**
     * Given a specific scheduled entry, determine how or if it should be scheduled or terminated.
     * @param entry
     */
    private void enableEntry(ScheduleEntry entry)
    {
        if (DbKey.isNull(entry.getRoutingSpecId()))
        {
            log.warn("Unable to start entry '{}', routing spec is not defined.", entry.getName());
        }
        var task = activeTasks.computeIfAbsent(entry.getName(), name ->
        {
            log.info("Starting {}", name);
            if (entry.getRunInterval() == null) // continuous, just submit
            {
                try
                {
                    return taskExecutor.submit(Router.createRouter(db, entry.getRoutingSpecId()));
                }
                catch (OpenDcsDataRuntimeException ex)
                {
                    log.atError().setCause(ex).log("Unable to create Router for entry.");
                    return null;
                }
            }
            else
            {
                return null;
            }
        });

        if (task != null && task.isDone())
        {
            try
            {
                task.get();
            }
            catch (CancellationException | InterruptedException | ExecutionException ex)
            {
                if (ex instanceof InterruptedException)
                {
                    Thread.currentThread().interrupt();
                }
                log.atError().setCause(ex).log("Abnormal termination of schedule {}", entry.getName());
            }
        }
    }

    /**
     * Construct a new routing scheduler with sensible defaults or changed settings.
     * RoutingSchedulerBuilder
     */
    public static class RoutingSchedulerBuilder extends StandaloneApp.AppBuilder<RoutingScheduler,RoutingSchedulerBuilder>
    {
        private String schedulerAppName;

        public RoutingSchedulerBuilder()
        {
            withLoopWait(60, TimeUnit.SECONDS);
        }

        @Override
        protected RoutingSchedulerBuilder self()
        {
            return this;
        }

        public RoutingSchedulerBuilder withSchedulerAppName(String scheduleName)
        {
            this.schedulerAppName = scheduleName;
            return self();
        }

        @Override
        public RoutingScheduler build() throws OpenDcsApplicationException
        {
            return new RoutingScheduler(this);
        }
    }
}
