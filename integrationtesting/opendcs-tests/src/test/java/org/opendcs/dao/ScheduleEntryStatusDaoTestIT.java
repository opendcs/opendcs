/*
* Where Applicable, Copyright 2026 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.ScheduleEntryDao;
import org.opendcs.database.dai.ScheduleEntryStatusDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.db.ScheduleEntry;
import decodes.db.ScheduleEntryStatus;
import decodes.sql.DbKey;

@DecodesConfigurationRequired({
        "shared/test-sites.xml",
        "shared/ROWI4.xml",
        "shared/presgrp-regtest.xml",
        "HydroJsonTest/HydroJSON-rs.xml",
        "SimpleDecodesTest/site-OKVI4.xml",
        "SimpleDecodesTest/OKVI4-decodes.xml"
})
@EnableIfTsDb
class ScheduleEntryStatusDaoTestIT extends AppTestBase
{
    @ConfiguredField
    OpenDcsDatabase db;

    @Test
    void test_basic_operations() throws Exception
    {
        var entryDao = db.getDao(ScheduleEntryDao.class).orElseThrow();
        var statusDao = db.getDao(ScheduleEntryStatusDao.class).orElseThrow();
        try (var tx = db.newTransaction())
        {
            var se = new ScheduleEntry("test-status");
            se.setEnabled(false);
            se.setLoadingAppName("RoutingScheduler");
            se.setRoutingSpecName("OKVI4-input");
            se.setStartTime(new Date());
            se.setRunInterval("1h");

            var seOut = entryDao.save(tx, se);


            var status = new ScheduleEntryStatus(DbKey.NullKey); // constructor ID is for the status, not the schedule entry
            status.setHostname("the tests");
            status.setRunStart(new Date(26, 1, 1, 0, 0, 0));
            status.setLastMessageTime(new Date(26, 1, 1, 0, 0, 0));
            status.setRunStatus("starting");
            status.setNumMessages(10);
            status.setNumDecodesErrors(0);
            status.setScheduleEntryId(seOut.getId());

            var statusOut = statusDao.updateStatus(tx, status);
            assertFalse(DbKey.isNull(statusOut.getId()));


            statusOut.setLastMessageTime(new Date(26, 1, 1, 2, 0, 0));
            statusOut.setRunStop(new Date(26, 1, 1, 2, 0, 0));
            statusOut.setRunStatus("done");

            var statusOut2 = statusDao.updateStatus(tx, statusOut);

            assertEquals("done", statusOut2.getRunStatus());

            var lastStatus = statusDao.getLastStatusFor(tx, seOut.getId());
            assertEquals(statusOut2, lastStatus.orElseGet(() -> fail("Status Entry was not retrieved.")));

            var status2 = new ScheduleEntryStatus(DbKey.NullKey);
            status2.setHostname("the tests");
            status2.setRunStart(new Date(26, 1, 1, 3, 0, 0));
            status2.setLastMessageTime(new Date(26, 1, 3, 0, 0, 0));
            status2.setRunStatus("starting");
            status2.setNumMessages(20);
            status2.setNumDecodesErrors(0);
            status2.setScheduleEntryId(seOut.getId());
            var status2Out = statusDao.updateStatus(tx, status2);
            assertFalse(DbKey.isNull(status2Out.getId()));

            var statuses = statusDao.getStatusFor(tx, seOut.getId(), -1, -1);

            var first = statusDao.getStatusFor(tx, seOut.getId(), 1, 0);
            var last = statusDao.getStatusFor(tx, seOut.getId(), 1, 1);

            assertEquals(2, statuses.size());
            assertEquals(statuses.getFirst(), first.getFirst());
            assertEquals(statuses.getLast(), last.getLast());

            var byStatusId = entryDao.getByStatusId(tx, statusOut2.getId()).orElseGet(() -> fail("could not get status id"));
            assertEquals(seOut, byStatusId);

            statusDao.deleteStatusEntriesFor(tx, seOut.getId());
            assertTrue(statusDao.getStatusFor(tx, seOut.getId(), -1, -1).isEmpty());

            statusDao.updateStatus(tx, status2);

            assertFalse(statusDao.getStatusFor(tx, seOut.getId(), -1, -1).isEmpty());

            statusDao.deleteStatusEntriesBefore(tx, seOut.getLoadingAppId(), ZonedDateTime.now(ZoneId.of("UTC")));
            assertTrue(statusDao.getStatusFor(tx, seOut.getId(), -1, -1).isEmpty());
        }

    }
}
