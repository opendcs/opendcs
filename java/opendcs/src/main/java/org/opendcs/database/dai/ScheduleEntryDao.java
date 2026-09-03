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
package org.opendcs.database.dai;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;

import decodes.db.ScheduleEntry;
import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;

public interface ScheduleEntryDao extends OpenDcsDao
{
    Optional<ScheduleEntry> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException;
    Optional<ScheduleEntry> getByStatusId(DataTransaction tx, DbKey statusId) throws OpenDcsDataException;
    Optional<ScheduleEntry> getByName(DataTransaction tx, String name) throws OpenDcsDataException;

    ScheduleEntry save(DataTransaction tx, ScheduleEntry entry) throws OpenDcsDataException;

    void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    default List<ScheduleEntry> getAll(DataTransaction tx, int limit, int offset) throws OpenDcsDataException
    {
        return getAll(tx, limit, offset, null);
    }

    List<ScheduleEntry> getAll(DataTransaction tx, int limit, int offset, CompAppInfo forApp) throws OpenDcsDataException;

    Optional<ScheduleEntry> ifStatusUpdatedSince(DataTransaction tx, DbKey entryId, ZonedDateTime previous) throws OpenDcsDataException;
   
}
