package org.opendcs.dao.opendcs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.opendcs.cwms.CwmsSettings;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import opendcs.opentsdb.OffsetErrorAction;
import opendcs.opentsdb.OpenDcsDbSettings;


class OpenDcsSettingsTestIT extends AppTestBase
{
    @ConfiguredField
    OpenDcsDatabase db;


    @Test
    @EnableIfTsDb({"OpenDCS-Postgres", "OpenDCS-Oracle"})
    void test_can_retrieve_settings_odcs()
    {
        var openDcsDbSettings = db.getSettings(OpenDcsDbSettings.class)
                                  .orElseGet(() -> fail("Required settings not available"));
        assertTrue(openDcsDbSettings.allowDstOffsetVariation);
        assertEquals(OffsetErrorAction.ROUND, openDcsDbSettings.offsetErrorActionEnum);
    }

    @EnableIfTsDb({"CWMS-Oracle"})
    @Test
    void test_can_retrieve_settings_cwms()
    {
        var cwmsSettings = db.getSettings(CwmsSettings.class);
        assertTrue(cwmsSettings.isPresent());
    }
}
