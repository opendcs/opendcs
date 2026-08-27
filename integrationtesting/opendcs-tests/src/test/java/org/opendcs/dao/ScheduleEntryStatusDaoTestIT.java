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

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.ScheduleEntryDao;
import org.opendcs.database.dai.ScheduleEntryStatusDao;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;

import decodes.db.ScheduleEntry;
import decodes.db.ScheduleEntryStatus;
import decodes.sql.DbKey;

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
            status.setRunStart(new Date());
            status.setRunStatus("starting");
            status.setNumMessages(10);
            status.setNumDecodesErrors(0);
            status.setScheduleEntryId(seOut.getId());

            var statusOut = statusDao.updateStatus(tx, status);
            assertFalse(DbKey.isNull(statusOut.getId()));

        }
    }
}
