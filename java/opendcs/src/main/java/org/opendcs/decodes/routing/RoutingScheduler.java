package org.opendcs.decodes.routing;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.opendcs.authentication.OpenDcsAuthException;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDataRuntimeException;
import org.opendcs.database.apps.StandaloneApp;
import org.opendcs.database.apps.exceptions.OpenDcsApplicationException;
import org.opendcs.database.dai.LoadingAppDao;
import org.opendcs.database.dai.RoutingSpecDao;
import org.opendcs.database.dai.ScheduleEntryDao;
import org.opendcs.database.dai.ScheduleEntryStatusDao;
import org.opendcs.util.Result;

public class RoutingScheduler extends StandaloneApp
{
    private final RoutingSpecDao specDao;
    private final ScheduleEntryDao scheduleDao;
    private final ScheduleEntryStatusDao statusDao;
    private final LoadingAppDao appDao;
    private final ScheduledExecutorService executor;
    private final String scheduleName;
    
    
    private RoutingScheduler(RoutingSchedulerBuilder builder)
    {
        super(builder);
        this.scheduleName = builder.scheduleName;
        this.executor = builder.executor;
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
            var app = appDao.getByName(tx, scheduleName).orElseThrow();
            var schedules = scheduleDao.getAll(tx, -1, -1, app);
            schedules.forEach(entry ->
            {
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


    public static class RoutingSchedulerBuilder extends StandaloneApp.AppBuilder<RoutingScheduler,RoutingSchedulerBuilder>
    {
        private ScheduledExecutorService executor = Executors.newScheduledThreadPool(4);
        private String scheduleName;

        @Override
        protected RoutingSchedulerBuilder self()
        {
            return this;
        }

        public RoutingSchedulerBuilder withExecutor(ScheduledExecutorService executor)
        {
            this.executor = executor;
            return self();
        }

        public RoutingSchedulerBuilder withSchedulerName(String scheduleName)
        {
            this.scheduleName = scheduleName;
            return self();
        }

        @Override
        public RoutingScheduler build() throws OpenDcsApplicationException
        {
            return new RoutingScheduler(this);
        }
    }
}
