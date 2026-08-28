package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.PlatformDao;
import org.opendcs.database.dai.PlatformStatusDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

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
            var platform = platformDao.getByMediumId(tx, "goes-self-timed", "CE344292")
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
}
