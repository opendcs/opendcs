package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.DataSourceDao;
import org.opendcs.database.dai.RoutingSpecDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.db.DataSource;
import decodes.db.NetworkList;
import decodes.db.RoutingSpec;
import decodes.sql.DbKey;

@EnableIfTsDb
@DecodesConfigurationRequired({
    "shared/test-sites.xml",
    "SimpleDecodesTest/site-OKVI4.xml",
    "SimpleDecodesTest/OKVI4-decodes.xml"
})
class RoutingSpecDaoTestIT extends AppTestBase
{

    @ConfiguredField
    OpenDcsDatabase db;

    @Test
    void test_routing_spec_operations_existing() throws Exception
    {
        var dao = db.getDao(RoutingSpecDao.class).orElseThrow();

        try (var tx = db.newTransaction())
        {
            var spec = dao.getByName(tx, "OKVI4-input")
                          .orElseGet(() -> fail("could not find spec OKVI4-input"));

            assertTrue(spec.isPrepared());
            assertFalse(spec.networkLists.isEmpty());
            assertEquals("<all>", spec.networkLists.getFirst().name);


            var specById = dao.getById(tx, spec.getId())
                              .orElseGet(() -> fail("could not find spec by id " + spec.getId()));
            assertEquals(spec, specById);
        }
    }

    @Test
    void test_create_update_delete() throws Exception
    {
        var dao = db.getDao(RoutingSpecDao.class).orElseThrow();
        var dataSourceDao = db.getDao(DataSourceDao.class).orElseThrow();



        try (var tx = db.newTransaction())
        {
            final var dataSource = dataSourceDao.getDataSource(tx, "OKVI4")
                                                .orElseGet(() -> fail("no data source configured named OKVI4"));
            assertFalse(DbKey.isNull(dataSource.getId()));
            final var specIn = new RoutingSpec("Simple-Test-Spec");
            specIn.consumerType = "pipe";
            specIn.dataSource = dataSource;
            specIn.addNetworkListName("<all>");
            specIn.networkLists.add(NetworkList.dummy_all);
            specIn.sinceTime = "now - 1 day";
            specIn.untilTime = "now";
            specIn.outputFormat = "human-readable";
            specIn.outputTimeZoneAbbr = "UTC";
            specIn.presentationGroupName = "CWMS";
            specIn.usePerformanceMeasurements = true;

            final var specOut = dao.save(tx, specIn);

            assertEquals(specIn.getName(), specOut.getName());
            assertFalse(specIn.networkLists.isEmpty());
            assertEquals(specIn.untilTime, specOut.untilTime);

            specOut.untilTime = "now - 1 hour";
            final var specOut2 = dao.save(tx, specOut);
            assertEquals(specOut.untilTime, specOut2.untilTime);
            assertNotEquals(specIn.untilTime, specOut2.untilTime);

            dao.delete(tx, specOut.getId());

            dao.getById(tx, specOut.getId()).ifPresent((s) -> fail("Spec was not deleted"));
        }
    }


    @Test
    void test_pagination() throws Exception
    {
        var dao = db.getDao(RoutingSpecDao.class).orElseThrow();

        /**
         * This is test code and intentionally repeatable "randomness". Any security
         * reports indicating this is "cryptographically insecure" will be dutifully ignored.
         */
        final var random = new Random(15); // NOSONAR


        final var dsMap = new HashMap<DbKey, DataSource>();

        try (var tx = db.newTransaction())
        {
            var dataSources = createDataSources(db,tx );
            assumeFalse(dataSources.isEmpty());
            final int NUM_SPECS = 50;
            for (int i = 0; i < NUM_SPECS; i++)
            {
                var ds = dataSources.get(random.nextInt(dataSources.size()-1));
                var spec = createSpec(i, ds);
                var specOut = dao.save(tx, spec);

                dsMap.put(specOut.getId(), ds);
            }

            var allSpecOnly = dao.getAll(tx, -1, -1, false);
            assertFalse(allSpecOnly.isEmpty(), "No RoutingSpecs were retrieved");
            assertNull(allSpecOnly.get(0).dataSource, "Data Source was set when it shouldn't have been");
            assertTrue(allSpecOnly.get(0).networkLists.isEmpty(), "NetLists returned when they shouldn't have been.");

            var all = dao.getAll(tx, -1, -1, true);

            assertFalse(all.isEmpty());

            for (var spec: all)
            {
                if (dsMap.containsKey(spec.getId())) // there may be other specs we didn't create in the list.
                {
                    assertEquals(dsMap.get(spec.getId()), spec.dataSource);
                }
            }

            var first10 = dao.getAll(tx, 10, 0, true);
            var second10 = dao.getAll(tx, 10, 10, true);

            assertEquals(all.get(0), first10.get(0));

            assertEquals(all.get(10), second10.get(0));

            // No schedule entry Dao yet so just make sure the query appropriately returns
            // nothing for now since we haven't assign any of them to anything.
            var scheduled = dao.getAll(tx, -1, -1, true, "does not exist");
            assertTrue(scheduled.isEmpty());


        }
    }

    private RoutingSpec createSpec(int idx, DataSource ds) throws Exception
    {
        String name = String.format("000-spec-%03d", idx);
        var spec = new RoutingSpec(name);
        spec.consumerType = "pipe";
        spec.dataSource = ds;
        spec.addNetworkListName("<all>");
        spec.networkLists.add(NetworkList.dummy_all);
        spec.sinceTime = "now - 1 day";
        spec.untilTime = "now";
        spec.outputFormat = "human-readable";
        spec.outputTimeZoneAbbr = "UTC";
        spec.presentationGroupName = "CWMS";
        spec.usePerformanceMeasurements = true;
        return spec;
    }

    private List<DataSource> createDataSources(OpenDcsDatabase db, DataTransaction tx) throws Exception
    {
        ArrayList<DataSource> ret = new ArrayList<>();
        var dataSourceDao = db.getDao(DataSourceDao.class).orElseThrow();
        final var lrgs1In = new DataSource("DS-1", "lrgs");
        lrgs1In.setDataSourceArg("host=localhost, username=bob");

        final var lrgs2In = new DataSource("DS-2", "lrgs");
        lrgs2In.setDataSourceArg("host=remotehost, username=alice");

        final var groupIn = new DataSource("DS-GROUP", "roundrobingroup");

        final var lrgs1Out = dataSourceDao.save(tx, lrgs1In);
        final var lrgs2Out = dataSourceDao.save(tx, lrgs2In);

        groupIn.addGroupMember(0, lrgs1Out);
        groupIn.addGroupMember(1, lrgs2Out);
        final var groupOut = dataSourceDao.save(tx, groupIn);

        ret.add(groupOut);
        ret.add(lrgs1Out);
        ret.add(lrgs2Out);

        return ret;
    }
}
