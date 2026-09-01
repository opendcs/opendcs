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
import static org.opendcs.utils.sql.SqlQueries.collateClauseFor;

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
import org.opendcs.database.dai.ScheduleEntryDao;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.schedule.ScheduleEntryMapper;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.SqlKeywords;
import org.opendcs.utils.sql.SqlQueries;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.stringtemplate.v4.STGroup;

import decodes.db.DatabaseException;
import decodes.db.ScheduleEntry;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;
import decodes.tsdb.CompAppInfo;

@ServiceProvider(service = ScheduleEntryDao.class)
public class ScheduleEntryDaoImpl implements ScheduleEntryDao
{
    private static final String LOADINGAPP_KEY = "loadingapp";

    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    private static final String SELECT = "select";
    private static final String MERGE = "merge";
    private static final String DELETE = "delete";

    private final STGroup queries;

    private final ScheduleEntryMapper entryMapper = ScheduleEntryMapper.withPrefix("se");

    public ScheduleEntryDaoImpl()
    {
        queries = StringTemplateSqlLocator.findStringTemplateGroup(ScheduleEntryDaoImpl.class);
    }

    @Override
    public Optional<ScheduleEntry> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        if (DbKey.isNull(id))
        {
            return Optional.empty();
        }

        return getBy(tx, " where schedule_entry_id = :id ", "id", id);
    }

    private Optional<ScheduleEntry> getBy(DataTransaction tx, String whereClause, String whereBindKey,
                                          Object whereBind, Object... additionalBinds) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));

        var selectTemplate = queries.getInstanceOf(SELECT);
        if (selectTemplate == null)
        {
            throw new OpenDcsDataException("No Instance of Select query is available.");
        }
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();
        selectTemplate.add("where", whereClause)
                      .add("columns",
                           entryMapper.columnsForSelect(ScheduleEntryMapper.Columns.LOADING_APPLICATION_NAME,
                                                        ScheduleEntryMapper.Columns.ROUTINSPEC_NAME
                      ))
                      .add(SqlQueries.COLLATE_CLAUSE, collateClauseFor(dbEngine));
        try (var select = handle.createQuery(selectTemplate.render()))
        {
            select.bind(whereBindKey, whereBind);
            if (additionalBinds.length % 2 != 0)
            {
                throw new OpenDcsDataException(
                    "Length of Additional arguments not divisible by 2. Arguments must be bind, value pairs");
            }
            for (int i = 0; i < additionalBinds.length; i = i + 2)
            {
                select.bindByType((String)additionalBinds[i], additionalBinds[i+1], additionalBinds[i+1].getClass());
            }

            return select.registerRowMapper(entryMapper)
                         .mapTo(ScheduleEntry.class)
                         .findFirst();
        }
    }

    @Override
    public Optional<ScheduleEntry> getByStatusId(DataTransaction tx, DbKey statusId)
        throws OpenDcsDataException
    {
        if (DbKey.isNull(statusId))
        {
            return Optional.empty();
        }

        return getBy(tx, """
                where schedule_entry_id =
                    (select
                        distinct schedule_entry_id
                       from schedule_entry_status
                      where schedule_entry_status_id = :statusId
                    )
                """,
         "statusId", statusId);
    }

    @Override
    public Optional<ScheduleEntry> getByName(DataTransaction tx, String name) throws OpenDcsDataException
    {
        if (name == null || name.isBlank())
        {
            return Optional.empty();
        }
        return getBy(tx, "where upper(se.name) = upper(:name)", "name", name);
    }

    @Override
    @SuppressWarnings("java:S138") // mostly whitespace, should be able to improve if we can find a good way 
                                   // to deal with better automating the binding (beyond just making
                                   // our datatypes fit the Jdbi design... though that's definitely an option.)
    public ScheduleEntry save(DataTransaction tx, ScheduleEntry entry) throws OpenDcsDataException
    {
        Objects.requireNonNull(entry, "A valid ScheduleEntry instance must be provided.");
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();
        var keyGen = ctx.getGenerator(KeyGenerator.class)
                .orElseThrow(() -> new OpenDcsDataException("No key generator configured."));
        var mergeTemplate = queries.getInstanceOf(MERGE)
                                   .add("dual", dbEngine == DatabaseEngine.ORACLE ? "from dual" : "");
        Object routingBind = entry.getRoutingSpecName();
        if (!DbKey.isNull(entry.getRoutingSpecId()))
        {
            mergeTemplate.add("routingspec", ":routingspecValue");
            routingBind = entry.getRoutingSpecId();
        }
        else if (routingBind != null && !((String)routingBind).isBlank())
        {
            mergeTemplate.add("routingspec", """
                    (select distinct id
                       from routingspec
                      where upper(name) = upper(:routingspecValue)
                    )
                """);
        }
        else
        {
            throw new OpenDcsDataException("Cannot save schedule entry without valid routing spec reference (name or id)");
        }

        Object loadingAppBind = entry.getLoadingAppName();
        if (!DbKey.isNull(entry.getLoadingAppId()))
        {
            loadingAppBind = entry.getLoadingAppId();
            mergeTemplate.add(LOADINGAPP_KEY, ":loadingappValue");
        }
        else if (loadingAppBind != null && !((String)loadingAppBind).isBlank())
        {
            mergeTemplate.add(LOADINGAPP_KEY, """
                    (select distinct loading_application_id
                       from hdb_loading_application app
                       where upper(app.loading_application_name) = upper(:loadingappValue) )
                    """);
        }
        else
        {

            mergeTemplate.add(LOADINGAPP_KEY, ":loadingappValue");
        }


        try (var merge = handle.createUpdate(mergeTemplate.render()))
        {
            DbKey id = entry.getId();
            var existing = getByName(tx, entry.getName());
            if (existing.isPresent())
            {
                // If there's an existing app with this name, we'll just assume the provided id, if any, was in error
                id = existing.get().getId();
                log.trace("""
                    Using ID from existing ScheduleEntry, id={}, that was found. Provided ID was {}.
                    """,
                    id, entry.getId());
            }
            final var bindKey = !DbKey.isNull(id) ? id : keyGen.getKey("schedule_entry", handle.getConnection());
            if (loadingAppBind instanceof String || !DbKey.isNull((DbKey)loadingAppBind))
            {
                merge.bind("loadingappValue", loadingAppBind);
            }
            else
            {
                merge.bind("loadingappValue", DbKey.NullKey);
            }

            merge.bind(ScheduleEntryMapper.Columns.ID.column(), bindKey)
                 .bind(ScheduleEntryMapper.Columns.NAME.column(), entry.getName())
                 .bind("routingspecValue", routingBind)
                 .bind(ScheduleEntryMapper.Columns.TIME_ZONE.column(), entry.getTimezone())
                 .bind(ScheduleEntryMapper.Columns.RUN_INTERVAL.column(), entry.getRunInterval())
                 .bind(ScheduleEntryMapper.Columns.ENABLED.column(), entry.isEnabled())
                 .bindByType(ScheduleEntryMapper.Columns.START_TIME.column(),
                             entry.getStartTime(),
                             Date.class)
                 .bindByType(ScheduleEntryMapper.Columns.LAST_MODIFIED.column(), new Date(), Date.class)
                 .execute();

            return getById(tx, bindKey)
                    .orElseThrow(
                        () -> new OpenDcsDataException("Unable to retrieve scheduled entry that was just saved."));

        }
        catch (DatabaseException ex)
        {
            throw new OpenDcsDataException("Unable to generate new key for entry.", ex);
        }
    }

    @Override
    public void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var deleteTemplate = queries.getInstanceOf(DELETE);
        try (var delete = handle.createUpdate(deleteTemplate.render()))
        {
            delete.bind(ScheduleEntryMapper.Columns.ID.column(), id).execute();
        }
    }


    @Override
    public Optional<ScheduleEntry> ifStatusUpdatedSince(DataTransaction tx, DbKey id, ZonedDateTime previous)
        throws OpenDcsDataException
    {
        var date = new Date(previous.toInstant().toEpochMilli());
        return getBy(tx, """
                where se.schedule_entry_id = :id
                and se.last_modified >= :checkDate
                """, "id", id, "checkDate", date);
    }

    @Override
    public List<ScheduleEntry> getAll(DataTransaction tx, int limit, int offset, CompAppInfo app)
            throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));

        var selectTemplate = queries.getInstanceOf(SELECT);
        if (selectTemplate == null)
        {
            throw new OpenDcsDataException("No Instance of Select query is available.");
        }
        selectTemplate.add(SqlKeywords.LIMIT, addLimitOffset(limit, offset))
                      .add("columns",
                           entryMapper.columnsForSelect(ScheduleEntryMapper.Columns.LOADING_APPLICATION_NAME,
                                                        ScheduleEntryMapper.Columns.ROUTINSPEC_NAME
                      ));
        var byApp = app != null && !DbKey.isNull(app.getAppId());
        if (byApp)
        {
            selectTemplate.add(SqlQueries.WHERE_CLAUSE, """
                    where se.loading_application_id = :id
                    """);
        }
        try (var select = handle.createQuery(selectTemplate.render()))
        {
            if (limit > 0)
            {
                select.bind(SqlKeywords.LIMIT, limit);
            }
            if (offset >= 0)
            {
                select.bind(SqlKeywords.OFFSET, offset);
            }
            if (byApp)
            {
                select.bind("id", app.getAppId());
            }

            return select.registerRowMapper(entryMapper)
                         .mapTo(ScheduleEntry.class)
                         .list();
        }
    }
}
