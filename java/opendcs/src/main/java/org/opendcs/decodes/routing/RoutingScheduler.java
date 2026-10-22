package org.opendcs.decodes.routing;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDataRuntimeException;
import org.opendcs.database.apps.StandaloneApp;
import org.opendcs.database.apps.exceptions.OpenDcsApplicationException;
import org.opendcs.database.dai.LoadingAppDao;
import org.opendcs.database.dai.RoutingSpecDao;
import org.opendcs.database.dai.ScheduleEntryDao;
import org.opendcs.database.dai.ScheduleEntryStatusDao;

public class RoutingScheduler extends StandaloneApp
{
    private final RoutingSpecDao specDao;
    private final ScheduleEntryDao scheduleDao;
    private final ScheduleEntryStatusDao statusDao;
    private final LoadingAppDao appDao;
    private final String schedulerAppName;
    
    
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
        try (var tx = db.newTransaction())
        {
            var app = appDao.getByName(tx, schedulerAppName).orElseThrow();
            var schedules = scheduleDao.getAll(tx, -1, -1, app);
            schedules.forEach(entry ->
            {
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
    public void loop()
    {
        // check on things.
    }

    @Override
    public void terminate()
    {

    }


    public static class RoutingSchedulerBuilder extends StandaloneApp.AppBuilder<RoutingScheduler,RoutingSchedulerBuilder>
    {
        private String schedulerAppName;

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
