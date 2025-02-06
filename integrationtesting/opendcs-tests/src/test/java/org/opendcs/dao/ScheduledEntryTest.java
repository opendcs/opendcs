package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ComputationConfigurationRequired;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;

import decodes.db.Database;
import decodes.db.ScheduleEntry;
import decodes.db.ScheduleEntryStatus;
import decodes.sql.DbKey;
import opendcs.dai.ScheduleEntryDAI;

//@ComputationConfigurationRequired("shared/loading-apps.xml")
@DecodesConfigurationRequired({
        "shared/test-sites.xml",
        "shared/ROWI4.xml",
        "shared/presgrp-regtest.xml",
        "HydroJsonTest/HydroJSON-rs.xml",
        "SimpleDecodesTest/site-OKVI4.xml",
        "SimpleDecodesTest/OKVI4-decodes.xml"
})
public class ScheduledEntryTest extends AppTestBase
{

    @ConfiguredField
    private Database db;

    @Test
    void test_scheduled_entry_delete() throws Exception
    {
        try(ScheduleEntryDAI dao = db.getDbIo().makeScheduleEntryDAO())
        {
            ScheduleEntry se = new ScheduleEntry("test");
            se.setEnabled(false);
            se.setLoadingAppName("compproc-regtest");
            se.setRoutingSpecName("OKVI4-input");
            se.setStartTime(new Date());
            se.setRunInterval("1h");
            dao.writeScheduleEntry(se);

            ScheduleEntryStatus ses = new ScheduleEntryStatus(DbKey.NullKey);
            ses.setScheduleEntryName(se.getName());
            ses.setScheduleEntryId(se.getId());
            ses.setHostname("bleh");
            ses.setRunStart(new Date());
            ses.setRunStatus("Working");
            dao.writeScheduleStatus(ses);

            final ScheduleEntry seRet = dao.readScheduleEntry("test");
            assertNotNull(seRet, "Could not read back Scheduled Entry.");
            
            final ArrayList<ScheduleEntryStatus> sesRet = dao.readScheduleStatus(seRet);
            assertNotNull(sesRet, "No status list returned.");
            assertFalse(sesRet.isEmpty(), "No status returned.");


            dao.deleteScheduleEntry(seRet);
            final ArrayList<ScheduleEntryStatus> sesRet2 = dao.readScheduleStatus(seRet);
            assertTrue(sesRet2.isEmpty(), "Status entries were not deleted.");
        }
    }
}
