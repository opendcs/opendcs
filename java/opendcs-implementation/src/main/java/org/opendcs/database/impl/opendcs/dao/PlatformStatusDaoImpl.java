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
import org.opendcs.database.impl.opendcs.jdbi.logging.DetailSqlLogger;
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
        selectTemplate.add(WHERE_CLAUSE, "ps.platform_id =:platform_id")
                      .add("columns", statusMapper.columnsForSelect(PlatformStatusMapper.Columns.LAST_ROUTING_SPEC_NAME))
                      .add("prefix", "ps"); // TODO: update once schedule entry is merged in.
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
            merge.setSqlLogger(new DetailSqlLogger(log));
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
        return List.of();
    }

    @Override
    public List<PlatformStatus> getPlatformStatusForNetList(DataTransaction tx, DbKey netlistId, int limit, int offset)
            throws OpenDcsDataException 
    {
        if (DbKey.isNull(netlistId))
        {
            return List.of();
        }
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var selectTemplate = queries.getInstanceOf(SELECT);

        if (selectTemplate == null)
        {
            throw new OpenDcsDataException("Could not find template");
        }
        selectTemplate.add(WHERE_CLAUSE, """
                        ps.platform_id in (
                            select tm.platformid from
                                networklistentry nle
                            left outer join transportmedium tm on tm.mediumid = nle.transportid
                            where nle.networklistid = :netlistid)       
                        """)
                      .add(LIMIT_CLAUSE, addLimitOffset(limit, offset))
                      .add("columns", statusMapper.columnsForSelect(PlatformStatusMapper.Columns.LAST_ROUTING_SPEC_NAME))
                      .add("prefix", "ps"); // TODO: update once schedule entry is merged in.
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

            return select.bind("netlistid", netlistId)
                         .registerRowMapper(statusMapper)
                         .mapTo(PlatformStatus.class)
                         .list();
        }
    }
}
