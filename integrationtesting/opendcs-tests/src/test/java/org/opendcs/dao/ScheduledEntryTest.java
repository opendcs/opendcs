package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

@DecodesConfigurationRequired({
        "shared/test-sites.xml",
        "shared/ROWI4.xml",
        "shared/presgrp-regtest.xml",
        "HydroJsonTest/HydroJSON-rs.xml",
        "SimpleDecodesTest/site-OKVI4.xml",
        "SimpleDecodesTest/OKVI4-decodes.xml"
})
class ScheduledEntryTest extends AppTestBase
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

    @Test
    void test_retrieve_entry_by_status_id() throws Exception
    {
        try (ScheduleEntryDAI dao = db.getDbIo().makeScheduleEntryDAO())
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

            ScheduleEntryStatus ses2 = new ScheduleEntryStatus(DbKey.NullKey);
            ses2.setScheduleEntryName(se.getName());
            ses2.setScheduleEntryId(se.getId());
            ses2.setHostname("localhost");
            ses2.setRunStart(new Date());
            ses2.setRunStatus("Running");
            dao.writeScheduleStatus(ses2);

            final ScheduleEntry seRet = dao.readScheduleEntry("test");
            assertNotNull(seRet, "Could not read back Scheduled Entry.");

            final ArrayList<ScheduleEntryStatus> sesRet = dao.readScheduleStatus(seRet);
            assertNotNull(sesRet, "No status list returned.");

            ScheduleEntry retrievedEntry = dao.readScheduleEntryByStatusId(ses2.getId());
            assertNotNull(retrievedEntry, "Could not retrieve entry by status id.");

            // loading application name is not retrieved as part of the entry record
            assertEquals(se.getName(), retrievedEntry.getName(), "Retrieved entry name does not match.");
            assertEquals(se.getStartTime(), retrievedEntry.getStartTime(), "Retrieved entry start time does not match.");
            assertEquals(se.getRunInterval(), retrievedEntry.getRunInterval(), "Retrieved entry run interval does not match.");
            assertEquals(se.getRoutingSpecName(), retrievedEntry.getRoutingSpecName(), "Retrieved entry routing spec name does not match.");
            assertEquals(se.isEnabled(), retrievedEntry.isEnabled(), "Retrieved entry enabled status does not match.");
        }
    }
}
