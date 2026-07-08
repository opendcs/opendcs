package org.opendcs.regression_tests.routing;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.LoadingAppDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;
import org.opendcs.fixtures.lrgs.LrgsTestInstance;
import org.python.modules.thread.thread;

import decodes.db.DataSource;
import decodes.db.Database;
import decodes.db.RoutingSpec;
import decodes.routing.RoutingScheduler;
import decodes.routing.RoutingSpecThread;
import decodes.tsdb.CompAppInfo;

@EnableIfTsDb({"OpenDCS-Postgres", "OpenDCS-Oracle", "CWMS-Oracle"})
@DecodesConfigurationRequired({
    "shared/test-sites.xml",
    "SimpleDecodesTest/site-OKVI4.xml"
})
@TestInstance(Lifecycle.PER_CLASS)
class SchedulerTestsIT extends AppTestBase
{
    private static final String REGRESSION_SCHEDULER = "regression-scheduler";

    LrgsTestInstance lrgs;
    RoutingScheduler scheduler;
    //Thread schedulerThread;
  
    @ConfiguredField
    OpenDcsDatabase db;

    
    
    @BeforeAll
    void setup() throws Exception
    {
        assertDoesNotThrow(() ->
        {
            var lrgsHome = Files.createTempDirectory("lrgshome").toFile();
            lrgsHome.mkdirs();
            lrgs = new LrgsTestInstance(lrgsHome);
        });

        var loadingDao = db.getDao(LoadingAppDao.class).orElseThrow();
        try (var tx = db.newTransaction())
        {
            var appInfo = new CompAppInfo();
            appInfo.setAppName(REGRESSION_SCHEDULER);
            loadingDao.save(tx, appInfo);
        }

        scheduler = new RoutingScheduler();
        scheduler.setNoExitAfterRunApp(true);
        final String[] args = {"-P", this.configuration.getPropertiesFile().getAbsolutePath(), "-a", REGRESSION_SCHEDULER};
        // schedulerThread = new Thread(() ->
        // {
        //     try
        //     {
        //         scheduler.execute(args);
        //     }
        //     catch (Exception ex)
        //     {
        //         throw new RuntimeException(ex);
        //     }
        // }, REGRESSION_SCHEDULER); 
        // schedulerThread.setDaemon(true);
        // schedulerThread.start();
    }

    @AfterAll
    void teardown()
    {
        //schedulerThread.interrupt();
    }


    @Test
    void test_lrgs_soure() throws Exception
    {
        var spec = new RoutingSpec("regression-spec-lrgs");
        spec.dataSource = new DataSource("regression-lrgs", "lrgs");
        spec.dataSource.setDataSourceArg("hostname=localhost, port=" + lrgs.getDdsPort());
        spec.consumerType = "null";

        var decodesDb = db.getLegacyDatabase(Database.class).orElseThrow();
        decodesDb.dataSourceList.add(spec.dataSource);
        decodesDb.dataSourceList.write();
        decodesDb.routingSpecList.add(spec);
        decodesDb.routingSpecList.write();

        spec.prepareForExec();
        var specThread = RoutingSpecThread.makeInstance(spec);
        specThread.run();

    }
}
