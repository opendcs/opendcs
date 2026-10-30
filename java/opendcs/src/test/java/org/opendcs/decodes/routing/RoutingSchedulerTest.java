package org.opendcs.decodes.routing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.LoadingAppDao;
import org.opendcs.database.dai.PresentationGroupDao;
import org.opendcs.database.dai.RoutingSpecDao;
import org.opendcs.database.dai.ScheduleEntryDao;
import org.opendcs.database.dai.ScheduleEntryStatusDao;
import org.opendcs.util.threading.WrappingScheduledExecutorService;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.consumer.DataConsumer;
import decodes.consumer.DataConsumerException;
import decodes.consumer.NullFormatter;
import decodes.consumer.OutputFormatter;
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
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
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

    @Mock
    PresentationGroupDao presDao;

    MockConsumer consumer;

    @Mock
    DataTransaction tx;

    @SuppressWarnings("null")
    @BeforeEach
    void setup() throws Exception
    {

        when(db.getDao(ScheduleEntryDao.class))
            .thenReturn(Optional.of(scheduleDao));
        when(db.getDao(RoutingSpecDao.class))
            .thenReturn(Optional.of(specDao));
        when(db.getDao(ScheduleEntryStatusDao.class))
            .thenReturn(Optional.of(statusDao));
        when(db.getDao(LoadingAppDao.class))
            .thenReturn(Optional.of(appDao));
        when(db.getDao(PresentationGroupDao.class))
            .thenReturn(Optional.of(presDao));

        when(presDao.getByName(eq(tx), anyString())).thenReturn(Optional.empty());

        when(db.newTransaction()).thenReturn(tx);
    }

    @SuppressWarnings("null")
    @Test
    void test_scheduler() throws Exception
    {
        final var latch = new CountDownLatch(1);
        consumer = new MockConsumer()
        {
            @Override
            public void close()
            {
                latch.countDown();
                super.close();
            }
        };
        var appInfo = new CompAppInfo();
        appInfo.setAppName("test");

        var scheduler = new RoutingScheduler.RoutingSchedulerBuilder()
                .withDatabase(db)
                .withSchedulerAppName(appInfo.getAppName())
                .withTaskExecutor(new WrappingScheduledExecutorService(Executors.newScheduledThreadPool(2))
                {

                    @Override
                    public void wrappedExecute(Runnable command)
                    {
                        // Mocked Statics are ThreadLocal. so we need to make sure this runs on the correct thread.
                        try (MockedStatic<DataConsumer> dc = mockStatic(DataConsumer.class,
                                               withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
                             MockedStatic<OutputFormatter> of = mockStatic(OutputFormatter.class,
                                                withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS)
                             ))
                        {
                            log.info("I'm on thread {}/{}",
                                     Thread.currentThread().getName(),
                                     Thread.currentThread().threadId());
                            dc.when(() -> DataConsumer.makeDataConsumer(anyString())).thenReturn(consumer);
                            dc.when(() -> OutputFormatter.makeOutputFormatter(anyString(), any(), any(), any(), any()))
                              .thenReturn(new NullFormatter());
                            command.run();
                        }
                        catch (Throwable ex)
                        {
                            log.atError().setCause(ex).log("Failed to run task.");
                            throw ex;
                        }
                    }

                })
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
        latch.await();

        assertFalse(consumer.messages.isEmpty());
    }

    @SuppressWarnings("null")
    @Test
    @Timeout (value = 5, unit = TimeUnit.SECONDS)
    void test_scheduler_error() throws Exception
    {
        final AtomicReference<Throwable> thrownException = new AtomicReference<>(null);
        final CountDownLatch latch = new CountDownLatch(1);
        var appInfo = new CompAppInfo();
        appInfo.setAppName("test-error");

        consumer = new MockConsumer()
        {
            @Override
            public void startMessage(DecodedMessage msg) throws DataConsumerException
            {
                log.info("Error from thread {}/{}",
                                     Thread.currentThread().getName(), Thread.currentThread().threadId());
                var ex = new DataConsumerException("I failed");
                thrownException.set(ex);
                latch.countDown();
                throw ex;
            }
        };

        var scheduler = new RoutingScheduler.RoutingSchedulerBuilder()
                .withDatabase(db)
                .withSchedulerAppName(appInfo.getAppName())
                .withTaskExecutor(new WrappingScheduledExecutorService(Executors.newScheduledThreadPool(2))
                {
                    @Override
                    public void wrappedExecute(Runnable command)
                    {
                        try (MockedStatic<DataConsumer> dc = mockStatic(DataConsumer.class,
                                               withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
                             MockedStatic<OutputFormatter> of = mockStatic(OutputFormatter.class,
                                               withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS)
                             ))
                        {
                            log.info("I'm on thread {}/{}",
                                     Thread.currentThread().getName(), Thread.currentThread().threadId());
                            dc.when(() -> DataConsumer.makeDataConsumer(anyString())).thenReturn(consumer);
                            dc.when(() -> OutputFormatter.makeOutputFormatter(anyString(), any(), any(), any(), any()))
                              .thenReturn(new NullFormatter());
                            command.run();
                        }
                        catch (Throwable ex)
                        {
                            log.atError().setCause(ex).log("Failed to run task.");
                            throw ex;
                        }
                    }
                })
                .build();

        when(appDao.getByName(any(), eq("test-error")))
            .thenReturn(Optional.of(appInfo));

        var entry = new ScheduleEntry("test-sched-error");
        entry.setEnabled(true);
        entry.setRoutingSpecId(DbKey.createDbKey(5L));
        entry.setRunInterval(null);
        entry.setStartTime(null);
        when(scheduleDao.getAll(any(), anyInt(), anyInt(), eq(appInfo)))
            .thenReturn(List.of(entry));


        var spec = new RoutingSpec("test-spec-error");
        spec.dataSource = mock(DataSource.class);
        when(spec.dataSource.makeDelegate())
            .thenReturn(new MockSourceExec(null, null));


        when(specDao.getById(any(), eq(entry.getRoutingSpecId())))
            .thenReturn(Optional.of(spec));

        assertTrue(scheduler.start().isSuccess());
        latch.await();
        assertTrue(consumer.messages.isEmpty());
        scheduler.stop();
        assertNotNull(thrownException.get());
        var ex = thrownException.get();
        assertChainOfCauseHas(DataConsumerException.class, ex);
    }

    <T extends Exception> void assertChainOfCauseHas(Class<T> expected, Throwable actual)
    {
        Throwable current = actual;
        boolean found = expected.isInstance(current);

        while (!found && current.getCause() != null && current.getCause() != current)
        {
            current = current.getCause();
            found = expected.isInstance(current);
        }

        assertTrue(found,
            () -> String.format("Exception of type %s was not found in the chain of cause.", expected.getName()));

    }

    static class MockConsumer extends DataConsumer
    {
        private static final Logger log = OpenDcsLoggerFactory.getLogger();

        public List<DecodedMessage> messages = new ArrayList<>();

        @Override
        public void open(String consumerArg, Properties props) throws DataConsumerException
        {
            log.info("Starting consumer.");
            log.info("Consumer open is on thread {}/{}",
                     Thread.currentThread().getName(), Thread.currentThread().threadId());
        }

        @Override
        public void close()
        {
            /** not used. */
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
            /** not used */
        }

        @Override
        public void endMessage()
        {
            /** not used */
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
            /* not used */
        }

        @Override
        public void init(Properties routingSpecProps, String since, String until, Vector<NetworkList> networkLists)
                throws DataSourceException
        {
            /* not used */
        }

        @Override
        public void close()
        {
            /* not used */
        }

        @Override
        protected RawMessage getSourceRawMessage() throws DataSourceException
        {
            log.info("Raw Message from {}/{}",
                                     Thread.currentThread().getName(), Thread.currentThread().threadId());

            if (count > 0)
            {
                count--;
                return new RawMessage(("hello" + (count+1)).getBytes(StandardCharsets.UTF_8));
            }
            return null;
        }

    }
}
