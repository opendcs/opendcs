package org.opendcs.database.impl.opendcs.dao.lrgs;

import static org.opendcs.utils.sql.SqlQueries.COLLATE_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.LIMIT_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.OFFSET_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.WHERE_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;
import static org.opendcs.utils.sql.SqlQueries.collateClauseFor;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.stringtemplate4.StringTemplateSqlLocator;
import org.opendcs.data.lrgs.LrgsDataInterfaceType;
import org.opendcs.database.DatabaseKey;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dao.lrgs.LrgsDataInterfaceTypeDao;
import org.opendcs.database.impl.opendcs.jdbi.logging.DetailSqlLogger;
import org.opendcs.database.impl.opendcs.jdbi.mapper.lrgs.LrgsDataInterfaceTypeMapper;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.stringtemplate.v4.STGroup;

import decodes.sql.DbKey;

@ServiceProvider (service = LrgsDataInterfaceTypeDao.class)
public class LrgsDataInterfaceTypeDaoImpl implements LrgsDataInterfaceTypeDao
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    private final STGroup queries;
    private final LrgsDataInterfaceTypeMapper typeMapper = LrgsDataInterfaceTypeMapper.withPrefix("ldit");

    public static final String SELECT = "select";
    public static final String MERGE = "merge";
    public static final String DELETE = "delete";

    public LrgsDataInterfaceTypeDaoImpl()
    {
        queries = StringTemplateSqlLocator.findStringTemplateGroup(getClass());
    }

    @Override
    public Optional<LrgsDataInterfaceType> getById(DataTransaction tx, DatabaseKey id) throws OpenDcsDataException
    {
        return getBy(tx, "id = :id", "id", id);
    }

    @Override
    public Optional<LrgsDataInterfaceType> getByName(DataTransaction tx, String name) throws OpenDcsDataException
    {
        return getBy(tx, "name = :name", "name", name);
    }

    private Optional<LrgsDataInterfaceType> getBy(DataTransaction tx, String whereClause, String whereKey, Object whereVal) throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();

        var selectTemplate = queries.getInstanceOf(SELECT);

        if (selectTemplate == null)
        {
            throw new OpenDcsDataException("Could not find template");
        }
        selectTemplate.add(WHERE_CLAUSE, whereClause);
        selectTemplate.add("columns", typeMapper.columnsForSelect());
        selectTemplate.add("prefix", typeMapper.getPrefix());
        selectTemplate.add(COLLATE_CLAUSE, collateClauseFor(dbEngine));
        try (var select = handle.createQuery(selectTemplate.render()))
        {
            return select.registerRowMapper(typeMapper)
                         .bind(whereKey, whereVal)
                         .mapTo(LrgsDataInterfaceType.class)
                         .findFirst();
        }
    }

    @Override
    public LrgsDataInterfaceType save(DataTransaction tx, LrgsDataInterfaceType entry) throws OpenDcsDataException
    {
        Objects.requireNonNull(entry, "Cannot save null LrgsDataInterfaceType");
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();
        var mergeTemplate = queries.getInstanceOf(MERGE)
                                   .add("dual", dbEngine == DatabaseEngine.ORACLE ? "from dual" : "");
        try (var merge = handle.createUpdate(mergeTemplate.render()))
        {
            DbKey id = (DbKey)entry.id();
            if (DbKey.isNull(id)) // if we have an id, we'll assume it's correct.
            {
                var existing = getByName(tx, entry.name());
                if (existing.isPresent())
                {
                    // If there's an existing app with this name, we'll just assume the provided id, if any, was in error
                    id = (DbKey)existing.get().id();
                    log.trace("""
                        Using ID from existing LrgsDataInterfaceType, id={}, that was found. Provided ID was {}.
                        """,
                        id, entry.id());
                }
                else
                {
                    id = DbKey.NullKey;
                }
            }
            final var bindKey = id;


            merge.bind(GenericColumns.ID.column(), bindKey) // this should have more type safety
                                                                   // however, at present we only use
                                                                   // DbKey
                 .bind(GenericColumns.NAME.column(), entry.name())
                 .bind(LrgsDataInterfaceTypeMapper.Columns.SERVICE_PROVIDER.column(), entry.serviceProvider())
                 .execute();

            return getByName(tx, entry.name())
                .orElseThrow(() -> new OpenDcsDataException("Unable to retrieve LrgsDataInterfaceType we just saved."));

        }
    }

    @Override
    public void delete(DataTransaction tx, DatabaseKey id) throws OpenDcsDataException
    {
        if (id == null)
        {
            return;
        }
        
        var deleteTemplate = queries.getInstanceOf(DELETE);
        if (deleteTemplate == null)
        {
            throw new OpenDcsDataException("Unable to get query template");
        }
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        try(var delete = handle.createUpdate(deleteTemplate.render()))
        {
            delete.bind(GenericColumns.ID.column(), id).execute();
        }

    }

    /**    (non-Javadoc)
     * 
     * @see org.opendcs.database.dao.BasicDao#getAll(org.opendcs.database.api.DataTransaction, int, int, boolean)
     * @param expand For this implementation expand is ignored as the data set is small.
     */
    @Override
    public List<LrgsDataInterfaceType> getAll(DataTransaction tx, int limit, int offset, boolean expand)
            throws OpenDcsDataException
    {
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();

        var selectTemplate = queries.getInstanceOf(SELECT);

        if (selectTemplate == null)
        {
            throw new OpenDcsDataException("Could not find template");
        }
        selectTemplate.add("columns", typeMapper.columnsForSelect());
        selectTemplate.add("prefix", typeMapper.getPrefix());
        selectTemplate.add(COLLATE_CLAUSE, collateClauseFor(dbEngine));
        selectTemplate.add(LIMIT_CLAUSE, addLimitOffset(limit, offset));

        try (var select = handle.createQuery(selectTemplate.render()))
        {

            if (limit > -1)
            {
                select.bind(LIMIT_CLAUSE, limit);                
            }

            if (offset > -1)
            {
                select.bind(OFFSET_CLAUSE, offset);
            }

            return select.registerRowMapper(typeMapper)
                         .mapTo(LrgsDataInterfaceType.class)
                         .list();
        }
    }

    @Override
    public Optional<LrgsDataInterfaceType> getByServiceProvider(DataTransaction tx, String serviceProvider)
            throws OpenDcsDataException
    {
        return getBy(tx, "service_provider = :sp", "sp", serviceProvider);
    }
    
}
