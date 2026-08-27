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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.ScheduleEntryDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;

import decodes.db.ScheduleEntry;
import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;

/**
 * NOTE: substantially similar to {@see ScheduleEntryTest}, however this is targetted at the
 * new DAO.
 * ScheduleEntryDaoTest
 */
@DecodesConfigurationRequired({
        "shared/test-sites.xml",
        "shared/ROWI4.xml",
        "shared/presgrp-regtest.xml",
        "HydroJsonTest/HydroJSON-rs.xml",
        "SimpleDecodesTest/site-OKVI4.xml",
        "SimpleDecodesTest/OKVI4-decodes.xml"
})
class ScheduleEntryDaoTestIT extends AppTestBase
{
    @ConfiguredField
    OpenDcsDatabase db;

    @Test
    void test_basic_operations() throws Exception
    {
        var dao = db.getDao(ScheduleEntryDao.class).orElseThrow();
        try (var tx = db.newTransaction())
        {
            var se = new ScheduleEntry("test");
            se.setEnabled(false);
            se.setLoadingAppName("RoutingScheduler");
            se.setRoutingSpecName("OKVI4-input");
            se.setStartTime(new Date());
            se.setRunInterval("1h");

            var seOut = dao.save(tx, se);

            assertEquals(se.getName(), seOut.getName());
            assertFalse(seOut.isEnabled());
            assertNotNull(seOut.getRoutingSpecId());
            assertEquals(se.getRoutingSpecName(), seOut.getRoutingSpecName());
            assertFalse(DbKey.isNull(seOut.getLoadingAppId()));
            assertEquals(se.getLoadingAppName(), seOut.getLoadingAppName());
            assertEquals(se.getRunInterval(), seOut.getRunInterval());
            assertEquals(se.getStartTime(), seOut.getStartTime());
            assertEquals(se.getTimezone(), seOut.getTimezone());

            seOut.setEnabled(true);
            var seOut2 = dao.save(tx, seOut); // this will also exercise the routing spec and loading by id paths.
            assertEquals(se.getName(), seOut2.getName());
            assertTrue(seOut2.isEnabled());
            assertNotNull(seOut2.getRoutingSpecId());
            assertEquals(se.getRoutingSpecName(), seOut2.getRoutingSpecName());
            assertFalse(DbKey.isNull(seOut2.getLoadingAppId()));
            assertEquals(se.getLoadingAppName(), seOut2.getLoadingAppName());
            assertEquals(se.getRunInterval(), seOut2.getRunInterval());
            assertEquals(se.getStartTime(), seOut2.getStartTime());
            assertEquals(se.getTimezone(), seOut.getTimezone());


            var id = seOut.getId();
            dao.delete(tx, id);
            dao.getById(tx, id)
               .ifPresent(entry -> fail("Entry was not deleted."));            

            se.setLoadingAppName(null);
            se.setLoadingAppId(null);
            var seOut3 = dao.save(tx, se);
            assertTrue(DbKey.isNull(seOut3.getLoadingAppId()));
            var name = seOut3.getLoadingAppName();
            assertTrue(name == null || name.isBlank());

            id = seOut3.getId();
            var lm = seOut.getLastModified();
            var epoch = lm.getTime() - 500000;
            var checkDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epoch),ZoneId.of("UTC"));
            dao.ifStatusUpdatedSince(tx, id, checkDate)
               .orElseGet(() -> fail("New ScheduleEntry instance was not returned."));


            se.setRoutingSpecId(null);
            se.setRoutingSpecName(null);

            assertThrows(OpenDcsDataException.class, () -> dao.save(tx, se));

        }
    }

    @Test
    void test_pagination() throws Exception
    {
        var dao = db.getDao(ScheduleEntryDao.class).orElseThrow();
        final int COUNT = 100;
        
        try (var tx = db.newTransaction())
        {
            var se = new ScheduleEntry("Random not RoutingSchedule Entry");
            se.setEnabled(false);
            se.setLoadingAppName("utility");
            se.setRoutingSpecName("OKVI4-input");
            se.setStartTime(new Date());
            se.setRunInterval("1h");

            dao.save(tx, se);


            ScheduleEntry seOut = null;
            for (int i = 0; i < COUNT; i++)
            {
                se = new ScheduleEntry(String.format("AAA-TestEntry-%03d",i));
                se.setEnabled(false);
                se.setLoadingAppName("RoutingScheduler");
                se.setRoutingSpecName("OKVI4-input");
                se.setStartTime(new Date());
                se.setRunInterval(String.format("%dh", i));

                seOut = dao.save(tx, se);
            }
            var all = dao.getAll(tx, -1, -1);

            var first10 = dao.getAll(tx, 10, 0);
            var second10 = dao.getAll(tx, 10, 10);

            assertTrue(all.size() >= COUNT);
            assertEquals("AAA-TestEntry-050", all.get(50).getName());

            assertEquals(all.get(0), first10.getFirst());
            assertEquals(all.get(9), first10.getLast());
            assertEquals(all.get(10), second10.getFirst());
            assertEquals(all.get(19), second10.getLast());

            assertNotNull(seOut);
            var app = new CompAppInfo(seOut.getLoadingAppId());
            var forApp = dao.getAll(tx, -1, -1, app);
            assertFalse(forApp.isEmpty());
            final var expectedAppId = seOut.getLoadingAppId();
            assertFalse(forApp.stream().anyMatch(e -> e.getLoadingAppId() != expectedAppId));
            tx.rollback();
        }
    }
}
