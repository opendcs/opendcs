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

import static org.opendcs.utils.sql.SqlQueries.LIMIT_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.WHERE_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.stringtemplate4.StringTemplateSqlLocator;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.PlatformStatusDao;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms.PlatformStatusMapper;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.SqlKeywords;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.stringtemplate.v4.STGroup;

import decodes.db.PlatformStatus;
import decodes.sql.DbKey;

@ServiceProvider(service = PlatformStatusDao.class)
public class PlatformStatusDaoImpl implements PlatformStatusDao
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    private static final String SELECT = "select";
    private static final String DELETE = "delete";
    private static final String MERGE = "merge";

    private final STGroup queries;

    private final PlatformStatusMapper statusMapper = PlatformStatusMapper.withPrefix("ps");

    public PlatformStatusDaoImpl()
    {
        queries = StringTemplateSqlLocator.findStringTemplateGroup(PlatformStatusDaoImpl.class);
    }

    @Override
    public Optional<PlatformStatus> getByPlatformId(DataTransaction tx, DbKey platformId) throws OpenDcsDataException
    {
        if (DbKey.isNull(platformId))
        {
            return Optional.empty();
        }
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var selectTemplate = queries.getInstanceOf(SELECT);

        if (selectTemplate == null)
        {
            throw new OpenDcsDataException("Could not find template");
        }
        selectTemplate.add(WHERE_CLAUSE, "where ps.platform_id = :platform_id")
                      .add("columns", statusMapper.columnsForSelect(PlatformStatusMapper.Columns.LAST_ROUTING_SPEC_NAME,
                                                                          PlatformStatusMapper.Columns.PLATFORM_DESIGNATOR))
                      .add("prefix", statusMapper.getPrefix());
        try (var select = handle.createQuery(selectTemplate.render()))
        {
            return select.bind(PlatformStatusMapper.Columns.PLATFORM_ID.column(), platformId)
                         .registerRowMapper(statusMapper)
                         .mapTo(PlatformStatus.class)
                         .findOne();
        }
    }

    @Override
    public PlatformStatus updatePlatformStatus(DataTransaction tx, PlatformStatus platformStatus) throws OpenDcsDataException
    {
        if (DbKey.isNull(platformStatus.getPlatformId()))
        {
            throw new OpenDcsDataException("Provided Platform status does not have a valid platform Id set");
        }
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();
        var mergeTemplate = queries.getInstanceOf(MERGE)
                                   .add("dual", dbEngine == DatabaseEngine.ORACLE ? "from dual" : "");
        
        try (var merge = handle.createUpdate(mergeTemplate.render()))
        {
            merge.bind(PlatformStatusMapper.Columns.PLATFORM_ID.column(), platformStatus.getPlatformId())
                 .bind(PlatformStatusMapper.Columns.LAST_SCHEDULE_ENTRY_STATUS_ID.column(),
                       platformStatus.getLastScheduleEntryStatusId())
                 .bind(PlatformStatusMapper.Columns.LAST_FAILURE_CODES.column(), platformStatus.getLastFailureCodes())
                 .bind(PlatformStatusMapper.Columns.ANNOTATION.column(), platformStatus.getAnnotation())
                 .bindByType(PlatformStatusMapper.Columns.LAST_CONTACT_TIME.column(),
                             platformStatus.getLastContactTime(),
                             Date.class)
                 .bindByType(PlatformStatusMapper.Columns.LAST_ERROR_TIME.column(),
                             platformStatus.getLastErrorTime(),
                             Date.class)
                 .bindByType(PlatformStatusMapper.Columns.LAST_MESSAGE_TIME.column(),
                             platformStatus.getLastMessageTime(),
                             Date.class)
                 .execute();
  
            return getByPlatformId(tx, platformStatus.getPlatformId())
                    .orElseThrow(() -> new OpenDcsDataException(("Unable to retrieve platform status we just created.")));
        }
    }

    @Override
    public void deletePlatformStatus(DataTransaction tx, DbKey platformId) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var deleteTemplate = queries.getInstanceOf(DELETE);
        try (var delete = handle.createUpdate(deleteTemplate.render()))
        {
            delete.bind(PlatformStatusMapper.Columns.PLATFORM_ID.column(), platformId).execute();
        }
    }

    @Override
    public List<PlatformStatus> getAll(DataTransaction tx, int limit, int offset) throws OpenDcsDataException
    {
        return getAll(tx, limit, offset, "");
    }

    private List<PlatformStatus> getAll(DataTransaction tx, int limit, int offset, String whereClause, Object... additionalBinds) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var selectTemplate = queries.getInstanceOf(SELECT);

        if (selectTemplate == null)
        {
            throw new OpenDcsDataException("Could not find template");
        }
        selectTemplate.add(WHERE_CLAUSE, whereClause)
                      .add(LIMIT_CLAUSE, addLimitOffset(limit, offset))
                      .add("columns", statusMapper.columnsForSelect(PlatformStatusMapper.Columns.LAST_ROUTING_SPEC_NAME,
                                                                          PlatformStatusMapper.Columns.PLATFORM_DESIGNATOR))
                      .add("prefix", statusMapper.getPrefix());
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

            if (additionalBinds.length % 2 != 0)
            {
                throw new OpenDcsDataException(
                    "Length of Additional arguments not divisible by 2. Arguments must be bind, value pairs");
            }
            for (int i = 0; i < additionalBinds.length; i = i + 2)
            {
                select.bindByType((String)additionalBinds[i], additionalBinds[i+1], additionalBinds[i+1].getClass());
            }
            return select.registerRowMapper(statusMapper)
                         .mapTo(PlatformStatus.class)
                         .list();
        }
    }


    @Override
    public List<PlatformStatus> getPlatformStatusForNetList(DataTransaction tx, DbKey netlistId, int limit, int offset)
            throws OpenDcsDataException 
    {
        if (DbKey.isNull(netlistId))
        {
            return List.of();
        }
        final String where = """
                        where ps.platform_id in (
                            select tm.platformid from
                                networklistentry nle
                            left outer join transportmedium tm on tm.mediumid = nle.transportid
                            where nle.networklistid = :netlistid)       
                        """;

        return getAll(tx, limit, offset, where, "netlistid", netlistId);
    }
}
