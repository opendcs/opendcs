package org.opendcs.dao.opendcs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import opendcs.opentsdb.OffsetErrorAction;
import opendcs.opentsdb.OpenDcsDbSettings;

@EnableIfTsDb({"OpenDCS-Postgres", "OpenDCS-Oracle"})
class OpenDcsDbSettingsTestIT extends AppTestBase
{
    @ConfiguredField
    OpenDcsDatabase db;


    @Test
    void test_can_retrieve_settings()
    {
        var openDcsDbSettings = db.getSettings(OpenDcsDbSettings.class)
                                  .orElseGet(() -> fail("Required settings not available"));
        assertTrue(openDcsDbSettings.allowDstOffsetVariation);
        assertEquals(OffsetErrorAction.ROUND, openDcsDbSettings.offsetErrorActionEnum);
    }
}
