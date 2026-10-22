package org.opendcs.decodes.routing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Vector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.LoadingAppDao;
import org.opendcs.database.dai.RoutingSpecDao;
import org.opendcs.database.dai.ScheduleEntryDao;
import org.opendcs.database.dai.ScheduleEntryStatusDao;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.consumer.DataConsumer;
import decodes.consumer.DataConsumerException;
import decodes.datasource.DataSourceException;
import decodes.datasource.DataSourceExec;
import decodes.datasource.RawMessage;
import decodes.db.DataSource;
import decodes.db.Database;
import decodes.db.InvalidDatabaseException;
import decodes.db.NetworkList;
import decodes.db.RoutingSpec;
import decodes.db.ScheduleEntry;
import decodes.decoder.DecodedMessage;
import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;

@ExtendWith(MockitoExtension.class)
@MockitoSettings (
    strictness = Strictness.WARN
)
class RoutingSchedulerTest
{
    @Mock
    OpenDcsDatabase db;

    @Mock
    RoutingSpecDao specDao;
    @Mock
    ScheduleEntryDao scheduleDao;
    @Mock
    ScheduleEntryStatusDao statusDao;
    @Mock
    LoadingAppDao appDao;

    MockConsumer consumer;

    @SuppressWarnings("null")
    @BeforeEach
    void setup() throws Exception
    {
        consumer = new MockConsumer();
        when(db.getDao(ScheduleEntryDao.class))
            .thenReturn(Optional.of(scheduleDao));
        when(db.getDao(RoutingSpecDao.class))
            .thenReturn(Optional.of(specDao));
        when(db.getDao(ScheduleEntryStatusDao.class))
            .thenReturn(Optional.of(statusDao));
        when(db.getDao(LoadingAppDao.class))
            .thenReturn(Optional.of(appDao));

        when(db.newTransaction()).thenReturn(mock(DataTransaction.class));

        var dc = mockStatic(DataConsumer.class);
        dc.when(() -> DataConsumer.makeDataConsumer(Mockito.anyString())).thenReturn(consumer);
    }


    @SuppressWarnings("null")
    @Test 
    void test_scheduler() throws Exception
    {
        var appInfo = new CompAppInfo();
        appInfo.setAppName("test");

        var scheduler = new RoutingScheduler.RoutingSchedulerBuilder()
                .withDatabase(db)
                .withSchedulerAppName(appInfo.getAppName())
                .build();
        
        when(appDao.getByName(any(), eq("test")))
            .thenReturn(Optional.of(appInfo));

        var entry = new ScheduleEntry("test-sched");
        entry.setEnabled(true);
        entry.setRoutingSpecId(DbKey.createDbKey(5L));
        entry.setRunInterval(null);
        entry.setStartTime(null);
        when(scheduleDao.getAll(any(), anyInt(), anyInt(), eq(appInfo)))
            .thenReturn(List.of(entry));

        
        var spec = new RoutingSpec("test-spec");
        spec.dataSource = mock(DataSource.class);
        when(spec.dataSource.makeDelegate())
            .thenReturn(new MockSourceExec(null, null));
        
        
        when(specDao.getById(any(), eq(entry.getRoutingSpecId())))
            .thenReturn(Optional.of(spec));

        assertTrue(scheduler.start().isSuccess());


        Thread.sleep(50000);
        assertFalse(consumer.messages.isEmpty());
        scheduler.stop();
    }

    static class MockConsumer extends DataConsumer
    {
        private static final Logger log = OpenDcsLoggerFactory.getLogger();

        public List<DecodedMessage> messages = new ArrayList<>();

        @Override
        public void open(String consumerArg, Properties props) throws DataConsumerException
        {
            log.info("Starting consumer.");            
        }

        @Override
        public void close()
        {
        
        }

        @Override
        public void startMessage(DecodedMessage msg) throws DataConsumerException
        {
            log.info("Got message: {}", msg);
            messages.add(msg);   
        }

        @Override
        public void println(String line)
        {
        }

        @Override
        public void endMessage()
        {
         
        }

    }

    static class MockSourceExec extends DataSourceExec
    {
        int count = 3;

        protected MockSourceExec(DataSource dataSource, Database decodesDatabase)
        {
            super(dataSource, decodesDatabase);
        }

        @Override
        public void processDataSource() throws InvalidDatabaseException 
        {
            
        }

        @Override
        public void init(Properties routingSpecProps, String since, String until, Vector<NetworkList> networkLists)
                throws DataSourceException
        {
        }

        @Override
        public void close()
        {
        }

        @Override
        protected RawMessage getSourceRawMessage() throws DataSourceException
        {
            if (count > 0)
            {
                count--;
                return new RawMessage(("hello" + (count+1)).getBytes(StandardCharsets.UTF_8));
            }
            return null;
        }

    }
}
