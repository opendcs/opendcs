package org.opendcs.database.impl.opendcs.dao;

import static org.opendcs.utils.sql.SqlQueries.WHERE_CLAUSE;

import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.stringtemplate4.StringTemplateSqlLocator;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.PlatformStatusDao;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms.PlatformMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms.PlatformReducer;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.platforms.PlatformStatusMapper;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.openide.util.lookup.ServiceProvider;
import org.stringtemplate.v4.STGroup;

import decodes.db.PlatformStatus;
import decodes.sql.DbKey;

@ServiceProvider(service = PlatformStatusDao.class)
public class PlatformStatusDaoImpl implements PlatformStatusDao
{
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
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();

        var selectTemplate = queries.getInstanceOf(SELECT);

        if (selectTemplate == null)
        {
            throw new OpenDcsDataException("Could not find template");
        }
        selectTemplate.add(WHERE_CLAUSE, "ps.platform_id =:platform_id")
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
        return null;
    }

    @Override
    public void deletePlatformStatus(DataTransaction tx, DbKey platformId) throws OpenDcsDataException
    {
        
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
        return List.of();
    }
}
