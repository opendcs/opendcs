package org.opendcs.database.impl.opendcs.dao.lrgs;

import static org.opendcs.utils.sql.SqlQueries.COLLATE_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.LEFT_OUTER;
import static org.opendcs.utils.sql.SqlQueries.LIMIT_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.OFFSET_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.WHERE_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;
import static org.opendcs.utils.sql.SqlQueries.collateClauseFor;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.jackson2.Jackson2Plugin;
import org.jdbi.v3.stringtemplate4.StringTemplateSqlLocator;
import org.opendcs.data.lrgs.LrgsDataInterface;
import org.opendcs.database.DatabaseKey;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dao.lrgs.LrgsDataInterfaceDao;
import org.opendcs.database.impl.opendcs.jdbi.column.json.ConfigArgumentFactory;
import org.opendcs.database.impl.opendcs.jdbi.column.json.ConfigColumnMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.lrgs.LrgsDataInterfaceMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.lrgs.LrgsDataInterfaceTypeMapper;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.stringtemplate.v4.STGroup;

import decodes.sql.DbKey;

@ServiceProvider (service = LrgsDataInterfaceDao.class)
public class LrgsDataInterfaceDaoImpl implements LrgsDataInterfaceDao
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    private final STGroup queries;
    private final LrgsDataInterfaceMapper mapper = LrgsDataInterfaceMapper.withPrefix("ldi");
    private final LrgsDataInterfaceTypeMapper typeMapper = LrgsDataInterfaceTypeMapper.withPrefix("ldit");

    public static final String SELECT = "select";
    public static final String MERGE = "merge";
    public static final String DELETE = "delete";

    public LrgsDataInterfaceDaoImpl()
    {
        queries = StringTemplateSqlLocator.findStringTemplateGroup(getClass());
    }

    @Override
    public Optional<LrgsDataInterface> getById(DataTransaction tx, DatabaseKey id) throws OpenDcsDataException
    {
        return getBy(tx, "ldi.id = :id", "id", id);
    }

    @Override
    public Optional<LrgsDataInterface> getByName(DataTransaction tx, String name) throws OpenDcsDataException
    {
        return getBy(tx, "ldi.name = :name", "name", name);
    }

    private Optional<LrgsDataInterface> getBy(DataTransaction tx, String whereClause, String whereKey, Object whereVal) throws OpenDcsDataException
    {
        var handle = getHandle(tx);
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();

        var selectTemplate = queries.getInstanceOf(SELECT);

        if (selectTemplate == null)
        {
            throw new OpenDcsDataException("Could not find template");
        }

        selectTemplate.add(WHERE_CLAUSE, whereClause);
        selectTemplate.add("columns", mapper.columnsForSelect(LrgsDataInterfaceMapper.Columns.CONFIG));
        selectTemplate.add("type_columns", typeMapper.columnsForSelect());
        selectTemplate.add("type_join",
                           typeMapper.joinStatement(LEFT_OUTER, LrgsDataInterfaceTypeMapper.Columns.ID,
                                                    "ldi", "id"));
        selectTemplate.add("prefix", mapper.getPrefix());
        selectTemplate.add(COLLATE_CLAUSE, collateClauseFor(dbEngine));
        try (var select = handle.createQuery(selectTemplate.render()))
        {
            return select.registerRowMapper(typeMapper)
                         .registerRowMapper(mapper)
                         .bind(whereKey, whereVal)
                         .mapTo(LrgsDataInterface.class)
                         .findFirst();
        }
    }

    @Override
    public LrgsDataInterface save(DataTransaction tx, LrgsDataInterface entry) throws OpenDcsDataException
    {
        Objects.requireNonNull(entry, "Cannot save null LrgsDataInterfaceType");
        var handle = getHandle(tx);
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
                        Using ID from existing LrgsDataInterface, id={}, that was found. Provided ID was {}.
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
                 .bindByType(LrgsDataInterfaceMapper.Columns.TYPE_ID.column(), entry.type() != null ? entry.type().id() : null, DbKey.class)
                 .bind(LrgsDataInterfaceMapper.Columns.CONFIG.column(), entry.config())
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
    public List<LrgsDataInterface> getAll(DataTransaction tx, int limit, int offset, boolean expand)
            throws OpenDcsDataException
    {
        var handle = getHandle(tx);
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();

        var selectTemplate = queries.getInstanceOf(SELECT);

        if (selectTemplate == null)
        {
            throw new OpenDcsDataException("Could not find template");
        }
        selectTemplate.add("columns", mapper.columnsForSelect(LrgsDataInterfaceMapper.Columns.CONFIG));
        selectTemplate.add("type_columns", typeMapper.columnsForSelect());
        selectTemplate.add("type_join",
                           typeMapper.joinStatement(LEFT_OUTER, LrgsDataInterfaceTypeMapper.Columns.ID,
                                                    "ldi", "id"));
        selectTemplate.add("prefix", mapper.getPrefix());
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
                         .registerRowMapper(mapper)
                         .mapTo(LrgsDataInterface.class)
                         .list();
        }
    }   

    /**
     * Helper function. Will be able to just call tx.getConnection(Handle.class)
     * in the future
     * @param tx
     * @return
     */
    // Use of this suppress is temporary. Handle should be part of DataTransaction
    // which would handle the close operation.
    @SuppressWarnings("resource")
    private Handle getHandle(DataTransaction tx) throws OpenDcsDataException
    {
        var h = tx.connection(Handle.class)
                            .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        h.getJdbi()
         .installPlugin(new Jackson2Plugin());
        return h.registerArgument(new ConfigArgumentFactory())
                .registerColumnMapper(new ConfigColumnMapper());
    }
}
