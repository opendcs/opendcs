package org.opendcs.dao.opendcs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.PlatformDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

@EnableIfTsDb({"OpenDCS-Postgres", "OpenDCS-Oracle"})
@DecodesConfigurationRequired({
    "shared/test-sites.xml",
    "SimpleDecodesTest/site-OKVI4.xml",
    "SimpleDecodesTest/OKVI4-decodes.xml"

})
class OpenDcsPlatformDaoTestIT extends AppTestBase
{

    @ConfiguredField
    OpenDcsDatabase db;

    @Test
    void test_can_read_existing_platform() throws Exception
    {
        var dao = db.getDao(PlatformDao.class).orElseThrow();

        try (var tx = db.newTransaction())
        {
            var platform = dao.getByMediumId(tx, "goes-self-timed", "CE344292")
                              .orElseGet(() -> fail("Could not retrieve platform."));

            assertFalse(platform.transportMedia.isEmpty());
            assertNotNull(platform.getSite());
            assertFalse(platform.getSite().getNameArray().isEmpty());
            assertEquals("I'm here", platform.getProperty("SystemCheck"));

            dao.delete(tx, platform.getId());

            var shouldFail = dao.getById(tx, platform.getId());
            assertFalse(shouldFail.isPresent());

            tx.rollback();
        }
        
    }

    @Test
    void test_get_all_partial_data() throws Exception
    {
        var dao = db.getDao(PlatformDao.class).orElseThrow();

        try (var tx = db.newTransaction())
        {
            var platforms = dao.getAll(tx, -1, -1, false);

            assertFalse(platforms.isEmpty());
            assertNull(platforms.get(0).getSite());
        }
    }
}
