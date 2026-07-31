package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.DataSourceDao;
import org.opendcs.database.dai.RoutingSpecDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.db.NetworkList;
import decodes.db.RoutingSpec;

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
            // for this test we don't actually care which data source
            final var dataSource = dataSourceDao.getDataSources(tx, 1, 0).getFirst();
                                                  
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
}
