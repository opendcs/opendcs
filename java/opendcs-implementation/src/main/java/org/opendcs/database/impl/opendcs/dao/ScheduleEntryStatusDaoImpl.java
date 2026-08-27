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
package org.opendcs.database.impl.opendcs.dao;

import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.stringtemplate4.StringTemplateSqlLocator;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.ScheduleEntryStatusDao;
import org.opendcs.database.impl.opendcs.jdbi.logging.DetailSqlLogger;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.schedule.ScheduleEntryMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.schedule.ScheduleEntryStatusMapper;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.SqlKeywords;
import org.opendcs.utils.sql.SqlQueries;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.stringtemplate.v4.STGroup;

import decodes.db.DatabaseException;
import decodes.db.ScheduleEntryStatus;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;

@ServiceProvider(service = ScheduleEntryStatusDao.class)
public class ScheduleEntryStatusDaoImpl implements ScheduleEntryStatusDao
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    private static final String SELECT = "select";
    private static final String MERGE = "merge";
    private static final String DELETE = "delete";
    private static final String UNSET_PLATFORM_STATUS = "unsetPlatformStatus";

    private final ScheduleEntryStatusMapper statusMapper = ScheduleEntryStatusMapper.withPrefix("ses");

    private final STGroup queries;

    public ScheduleEntryStatusDaoImpl()
    {
        STGroup.verbose = true;
        queries = StringTemplateSqlLocator.findStringTemplateGroup(ScheduleEntryStatusDaoImpl.class);
    }

    @Override
    public Optional<ScheduleEntryStatus> getLastStatusFor(DataTransaction tx, DbKey scheduleEntryId) throws OpenDcsDataException
    {
        return get(tx, """
                where ses.schedule_entry_id = :schedule_entry_id
            and ses.last_modified =
                (select max(last_modified)
                   from schedule_entry_status
                  where schedule_entry_id = :schedule_entry_id
                )
                """, "schedule_entry_id", scheduleEntryId);
    }

    private Optional<ScheduleEntryStatus> get(DataTransaction tx, String whereClause, String whereBindKey, Object whereBind)
        throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));

        var selectTemplate = queries.getInstanceOf(SELECT);
        if (selectTemplate == null)
        {
            throw new OpenDcsDataException("No Instance of Select query is available.");
        }
        selectTemplate.add("where", whereClause)
                      .add("prefix", statusMapper.getPrefix())
                      .add("columns", statusMapper.columnsForSelect(ScheduleEntryStatusMapper.Columns.SCHEDULE_ENTRY_NAME));
        try (var select = handle.createQuery(selectTemplate.render()))
        {
            select.setSqlLogger(new DetailSqlLogger(log));
            select.bind(whereBindKey, whereBind);
            return select.registerRowMapper(statusMapper)
                         .mapTo(ScheduleEntryStatus.class)
                         .findFirst();
        }
    }

    private Optional<ScheduleEntryStatus> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        return get(tx, "where schedule_entry_status_id = :schedule_entry_status_id",
                   "schedule_entry_status_id", id);
    }

    @Override
    public ScheduleEntryStatus updateStatus(DataTransaction tx, ScheduleEntryStatus status) throws OpenDcsDataException
    {
        Objects.requireNonNull(status, "A valid ScheduleEntry instance must be provided.");
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();
        var keyGen = ctx.getGenerator(KeyGenerator.class)
                .orElseThrow(() -> new OpenDcsDataException("No key generator configured."));
        var mergeTemplate = queries.getInstanceOf(MERGE)
                                   .add("dual", dbEngine == DatabaseEngine.ORACLE ? "from dual" : "");

        try (var merge = handle.createUpdate(mergeTemplate.render()))
        {
            merge.setSqlLogger(new DetailSqlLogger(log));
            DbKey id = status.getId();
            var existing = getById(tx, id);
            if (existing.isPresent())
            {
                // If there's an existing app with this name, we'll just assume the provided id, if any, was in error
                id = existing.get().getId();
                log.trace("""
                    Using ID from existing ScheduleENtry, id={}, that was found. Provided ID was {}.
                    """,
                    id, status.getId());
            }
            final var bindKey = !DbKey.isNull(id) ? id : keyGen.getKey("schedule_entry_status", handle.getConnection());
            merge.bind(ScheduleEntryStatusMapper.Columns.ID.column(), bindKey)
                 .bind(ScheduleEntryStatusMapper.Columns.SCHEDULE_ENTRY_ID.column(), status.getScheduleEntryId())
                 .bindByType(ScheduleEntryStatusMapper.Columns.RUN_START_TIME.column(), status.getRunStart(), Date.class)
                 .bindByType(ScheduleEntryStatusMapper.Columns.LAST_MESSAGE_TIME.column(), status.getLastMessageTime(), Date.class)
                 .bindByType(ScheduleEntryStatusMapper.Columns.RUN_COMPLETE_TIME.column(), status.getRunStop(), Date.class)
                 .bindByType(ScheduleEntryMapper.Columns.LAST_MODIFIED.column(), new Date(), Date.class)
                 .bind(ScheduleEntryStatusMapper.Columns.HOSTNAME.column(), status.getHostname())
                 .bind(ScheduleEntryStatusMapper.Columns.RUN_STATUS.column(), status.getRunStatus())
                 .bind(ScheduleEntryStatusMapper.Columns.NUM_MESSAGES.column(), status.getNumMessages())
                 .bind(ScheduleEntryStatusMapper.Columns.NUM_DECODE_ERRORS.column(), status.getNumDecodesErrors())
                 .bind(ScheduleEntryStatusMapper.Columns.NUM_PLATFORMS.column(), status.getNumPlatforms())
                 .bind(ScheduleEntryStatusMapper.Columns.LAST_SOURCE.column(), status.getLastSource())
                 .bind(ScheduleEntryStatusMapper.Columns.LAST_CONSUMER.column(), status.getLastConsumer())
                 .execute();

            return getById(tx, bindKey)
                    .orElseThrow(
                        () -> new OpenDcsDataException("Unable to retrieve scheduled entry status that was just saved."));

        }
        catch (DatabaseException ex)
        {
            throw new OpenDcsDataException("Unable to generate new key for status entry.", ex);
        }
    }

    @Override
    public void deleteStatusEntriesBefore(DataTransaction tx, DbKey appId, ZonedDateTime cutoff) throws OpenDcsDataException
    {
        String where = """
            last_schedule_entry_status_id in
            (select
                schedule_entry_status_id
            from schedule_entry_status
            where loading_application_id = :appId and run_start_time < :cutoff
            )
                """;
        var cutoffDate = new Date(cutoff.toInstant().toEpochMilli());
        deleteQuery(tx, UNSET_PLATFORM_STATUS, where, "appId", appId, "cutoff", cutoffDate);
        deleteQuery(tx, DELETE, where, "appId", appId, "cutoff", cutoffDate);
    }

    /**
     * Handle dealing with the two different delete where clause without excessive duplication.
     * @param tx
     * @param template
     * @param where
     * @param bindSet
     * @throws OpenDcsDataException
     */
    private void deleteQuery(DataTransaction tx, String template, String where, Object... bindSet) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));

        var queryTemplate = queries.getInstanceOf(template);
        if (queryTemplate == null)
        {
            throw new OpenDcsDataException("No Instance of Select query is available.");
        }
        if (bindSet.length % 2 != 0)
        {
            throw new OpenDcsDataException(
                "Length of bind arguments not divisible by 2. Arguments must be bind, value pairs");
        }
        queryTemplate.add("where", where);

        try (var query = handle.createUpdate(queryTemplate.render()))
        {
            for (int i = 0; i < bindSet.length; i = i + 2)
            {
                query.bindByType((String)bindSet[i], bindSet[i+1], bindSet[i+1].getClass());
            }
            query.execute();
        }
    }

    @Override
    public void deleteStatusEntriesFor(DataTransaction tx, DbKey scheduleEntryId) throws OpenDcsDataException
    {
         String where = """
            last_schedule_entry_status_id in
            (select
                schedule_entry_status_id
            from schedule_entry_status
            where schedule_entry_id = :schedule_entry_id
            )
                """;
        deleteQuery(tx, UNSET_PLATFORM_STATUS, where, ScheduleEntryStatusMapper.Columns.SCHEDULE_ENTRY_ID, scheduleEntryId);
        deleteQuery(tx, DELETE, where, ScheduleEntryStatusMapper.Columns.SCHEDULE_ENTRY_ID, scheduleEntryId);
    }

    @Override
    public List<ScheduleEntryStatus> getStatusFor(DataTransaction tx, DbKey scheduleEntryId, int limit, int offset)
        throws OpenDcsDataException
    {

        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));

        var selectTemplate = queries.getInstanceOf(SELECT);
        if (selectTemplate == null)
        {
            throw new OpenDcsDataException("No Instance of Select query is available.");
        }
        selectTemplate.add(SqlQueries.WHERE_CLAUSE, "where schedule_entry_id = :schedule_entry_id")
                      .add(SqlQueries.LIMIT_CLAUSE, addLimitOffset(limit, offset))
                      .add("prefix", statusMapper.getPrefix())
                      .add("columns", statusMapper.columnsForSelect(ScheduleEntryStatusMapper.Columns.SCHEDULE_ENTRY_NAME));
        try (var select = handle.createQuery(selectTemplate.render()))
        {
            if (limit >= 0)
            {
                select.bind(SqlKeywords.LIMIT, limit);
            }
            if (offset >= 0)
            {
                select.bind(SqlKeywords.OFFSET, offset);
            }

            return select.bind(ScheduleEntryStatusMapper.Columns.SCHEDULE_ENTRY_ID.column(), scheduleEntryId)
                         .registerRowMapper(statusMapper)
                         .mapTo(ScheduleEntryStatus.class)
                         .list();
        }
    }
}
