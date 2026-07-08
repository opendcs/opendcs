package org.opendcs.regression_tests.routing;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import decodes.datasource.RawMessage;
import decodes.db.DataSource;
import decodes.db.Database;
import decodes.db.RoutingSpec;
import decodes.routing.RoutingScheduler;
import decodes.routing.RoutingSpecThread;
import decodes.tsdb.CompAppInfo;
import lrgs.common.DcpMsg;

@EnableIfTsDb({"OpenDCS-Postgres", "OpenDCS-Oracle", "CWMS-Oracle"})
@DecodesConfigurationRequired({
    "shared/test-sites.xml",
    "SimpleDecodesTest/site-OKVI4.xml"
})
@TestInstance(Lifecycle.PER_CLASS)
class RoutingSpecThreadTestsIT extends AppTestBase
{
    private static final String REGRESSION_SCHEDULER = "regression-scheduler";

    LrgsTestInstance lrgs;
    RoutingScheduler scheduler;

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

        var decodesDb = db.getLegacyDatabase(Database.class).orElseThrow();
        var consumers = decodesDb.enumList.getEnum("DataConsumer");
        consumers.addValue("memory", "in memory consumer for testing",
                        "org.opendcs.regression_tests.routing.SingletonMemoryConsumer", "");
        consumers.write();
        decodesDb.enumList.write();

        scheduler = new RoutingScheduler();
        scheduler.setNoExitAfterRunApp(true);

        var msg = """
4804F5C804011203139G31-5HN060W0000177:HG
31#30+3.95500e+00+3.95700e+00+3.95700e+00+3.95700e+00+3.95700e+00+3.95700e+00:HG
196#180+3.94900e+00:HG 206#180+3.96100e+00:VB
31#60+1.18576e+01+1.18620e+01+1.18509e+01:ZL$
            """.getBytes();

        var dcpMsg = new DcpMsg(msg, msg.length, 0);
        lrgs.getArchive().archiveMsg(dcpMsg, lrgs.getLrgsInputs().get(0));
    }

    @Test
    void test_lrgs_source() throws Exception
    {
        var spec = new RoutingSpec("regression-spec-lrgs");
        spec.dataSource = new DataSource("regression-lrgs", "lrgs");
        spec.dataSource.setDataSourceArg("hostname=localhost, username=anonymous, port=" + lrgs.getDdsPort());
        spec.consumerType = "memory";
        spec.outputFormat = "null";

        var decodesDb = db.getLegacyDatabase(Database.class).orElseThrow();
        decodesDb.dataSourceList.add(spec.dataSource);
        decodesDb.dataSourceList.write();
        decodesDb.routingSpecList.add(spec);
        decodesDb.routingSpecList.write();

        spec.prepareForExec();
        spec.dataSource.prepareForExec();
        var specThread = RoutingSpecThread.makeInstance(spec);
        specThread.run();

        var messages = SingletonMemoryConsumer.messagesFor("4804F5C8");
        assertFalse(messages.isEmpty());
    }
}
