package org.opendcs.decodes.routing;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDataRuntimeException;
import org.opendcs.database.apps.StandaloneApp;
import org.opendcs.database.apps.exceptions.OpenDcsApplicationException;
import org.opendcs.database.dai.LoadingAppDao;
import org.opendcs.database.dai.RoutingSpecDao;
import org.opendcs.database.dai.ScheduleEntryDao;
import org.opendcs.database.dai.ScheduleEntryStatusDao;
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

public class RoutingScheduler extends StandaloneApp
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


    private void enableEntry(ScheduleEntry entry)
    {
        if (DbKey.isNull(entry.getRoutingSpecId()))
        {
            log.warn("Unable to start entry '{}', routing spec is not defined.", entry.getName());
        }
        var task = activeTasks.computeIfAbsent(entry.getName(), (name) ->
        {
            if (entry.getRunInterval() == null)
            {
                log.info("Starting {}", entry.getName());
                return taskExecutor.submit(() ->
                {
                    log.info("Running");
                    try (var tx = db.newTransaction())
                    {
                        log.info("have tx");
                        var specId = entry.getRoutingSpecId();
                        var spec = specDao.getById(tx, specId)
                                        .orElseThrow(() -> new OpenDcsDataException("No routing spec with id " + specId));
                        log.info("have spec");
                        var consumer = DataConsumer.makeDataConsumer(name);
                        log.info("made consumer");
                        consumer.open(spec.consumerArg, spec.getProperties());
                        log.info("Starting message stream.");
                        spec.dataSource
                            .makeDelegate()
                            .stream()
                            .peek(m -> log.info("looking at ", m))
                            .filter(m -> m.isSuccess())
                            .map(m -> m.success())
                            .map(m ->
                                {
                                    try
                                    {
                                        return new DecodedMessage((RawMessage)m, false);
                                    }
                                    catch (UnknownPlatformException ex)
                                    {
                                        return (DecodedMessage)null;
                                    }
                                })
                                .filter(m -> m != null)
                            .forEach(m -> log.info("got {}", m));
                    }
                    catch (InvalidDatabaseException | OpenDcsDataException | DataConsumerException ex)
                    {
                        log.atError().setCause(ex).log("not great");
                        throw new RuntimeException("Could not retrieve and run routing spec.", ex);
                    }
                });
            }
            else
            {
                return null;
            }
        });

        if (task != null)
        {
            /* something? */
        }
    }


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
