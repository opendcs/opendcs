package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.NetworkListDao;
import org.opendcs.database.dai.PlatformDao;
import org.opendcs.database.dai.PlatformStatusDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import decodes.db.PlatformStatus;
import decodes.db.Site;

@DecodesConfigurationRequired({
        "shared/test-sites.xml",
        "shared/ROWI4.xml",
        "shared/presgrp-regtest.xml",
        "HydroJsonTest/HydroJSON-rs.xml",
        "SimpleDecodesTest/site-OKVI4.xml",
        "SimpleDecodesTest/OKVI4-decodes.xml"
})
@EnableIfTsDb
class PlatformStatusDaoTestIT extends AppTestBase
{
    private static final String MEDIUM_ID = "CE344292";

    @ConfiguredField
    OpenDcsDatabase db;
   

    @Test
    void test_basic_operations() throws Exception
    {
        var statusDao = db.getDao(PlatformStatusDao.class).orElseThrow();
        var platformDao = db.getDao(PlatformDao.class).orElseThrow();

        try (var tx = db.newTransaction())
        {
            // TODO: change back to site and actually implement the PlatformDao by site
            // logic.
            var platform = platformDao.getByMediumId(tx, "goes-self-timed", MEDIUM_ID)
                                      .orElseGet(() -> fail("Could not retrieve Platform."));


            var status = new PlatformStatus(platform.getId());
            status.setAnnotation("Initial status");
            status.setLastContactTime(new Date(128, 7, 28, 9, 45, 0));
            var statusOut = statusDao.updatePlatformStatus(tx, status);
            assertEquals(status, statusOut);

            var statusOutById = statusDao.getByPlatformId(tx, platform.getId())
                                         .orElseGet(() -> fail("Could not retrieve status"));
            assertEquals(statusOut, statusOutById);

            statusDao.deletePlatformStatus(tx, platform.getId());
            assertTrue(statusDao.getByPlatformId(tx, platform.getId()).isEmpty());
        }
    }

    @Test
    void test_by_network_list() throws Exception
    {
        var statusDao = db.getDao(PlatformStatusDao.class).orElseThrow();
        var platformDao = db.getDao(PlatformDao.class).orElseThrow();
        var networkListDao = db.getDao(NetworkListDao.class).orElseThrow();

        
        try (var tx = db.newTransaction())
        {
            var platform = platformDao.getByMediumId(tx, "goes-self-timed", MEDIUM_ID)
                                      .orElseGet(() -> fail("Could not retrieve Platform."));

            var list = new NetworkList("test-list");
            list.addEntry(new NetworkListEntry(null, MEDIUM_ID));
            list.transportMediumType = "goes";
            list.siteNameTypePref = "cwms";

            var listOut = networkListDao.save(tx, list);
            
            var statuses = statusDao.getPlatformStatusForNetList(tx, listOut.getId(), -1, -1);
            assertTrue(statuses.isEmpty());

            var status = new PlatformStatus(platform.getId());
            status.setAnnotation("From list");
            status.setLastContactTime(new Date(126, 7, 12, 35, 0, 0));
            statusDao.updatePlatformStatus(tx, status);

            statuses = statusDao.getPlatformStatusForNetList(tx, listOut.getId(), -1, -1);
            assertEquals(1, statuses.size());

            assertEquals(status, statuses.getFirst());
        }
    }
}
