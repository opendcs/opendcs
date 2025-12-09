package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
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
            ses.setRunStatus("Starting");
            dao.writeScheduleStatus(ses);

            ScheduleEntryStatus ses2 = new ScheduleEntryStatus(DbKey.NullKey);
            ses2.setScheduleEntryName(se.getName());
            ses2.setScheduleEntryId(se.getId());
            ses2.setHostname("saved again quickly");
            ses2.setRunStart(new Date(ses.getRunStart().getTime() + 5000 /* add 5 seconds */));
            ses2.setRunStatus("Working");
            dao.writeScheduleStatus(ses2);

            ScheduleEntryStatus ses3 = new ScheduleEntryStatus(DbKey.NullKey);
            ses3.setScheduleEntryName(se.getName());
            ses3.setScheduleEntryId(se.getId());
            ses3.setHostname("saved again quickly");
            ses3.setRunStart(new Date(ses.getRunStart().getTime() + 10000 /* add 10 seconds */));
            ses3.setRunStop(new Date());
            ses3.setRunStatus("Done");
            dao.writeScheduleStatus(ses3);

            final ScheduleEntryStatus singleRet = dao.getLastScheduleStatusFor(se);
            assertNotNull(singleRet, "Could not read last status.");


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
