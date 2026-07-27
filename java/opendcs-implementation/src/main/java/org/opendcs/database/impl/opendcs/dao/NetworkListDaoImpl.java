package org.opendcs.database.impl.opendcs.dao;


import static org.opendcs.utils.sql.SqlQueries.COLLATE_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.LEFT_OUTER;
import static org.opendcs.utils.sql.SqlQueries.WHERE_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;
import static org.opendcs.utils.sql.SqlQueries.collateClauseFor;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.SqlLogger;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.stringtemplate4.StringTemplateSqlLocator;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.NetworkListDao;
import org.opendcs.database.impl.opendcs.jdbi.logging.DetailSqlLogger;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.networklist.NetworkListEntryMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.networklist.NetworkListMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.networklist.NetworkListReducer;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.SqlKeywords;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;
import org.slf4j.Logger;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import decodes.db.DatabaseException;
import decodes.db.NetworkList;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;

@ServiceProviders({
    @ServiceProvider(service = NetworkListDao.class, path = "dao/OpenDCS-Postgres"),
    @ServiceProvider(service = NetworkListDao.class, path = "dao/OpenDCS-Oracle"),
    @ServiceProvider(service = NetworkListDao.class, path = "dao/OPENTSDB"),
    @ServiceProvider(service = NetworkListDao.class, path = "default")
})
public class NetworkListDaoImpl implements NetworkListDao
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    private static final String SELECT = "select";
    private static final String MERGE = "networkListMerge";
    private static final String DELETE_NETWORKLIST = "deleteNetworkList";
    private static final String DELETE_NETWORKLIST_ENTRY = "deleteNetworkListEntry";
    private static final String INSERT_NETWORKLIST_ENTRY = "insertNetworkListEntry";

    private final STGroup queries;

    private final NetworkListMapper listMapper = NetworkListMapper.withPrefix("nl");
    private final NetworkListEntryMapper listEntryMapper = NetworkListEntryMapper.withPrefix("nle");

    public NetworkListDaoImpl()
    {
        queries = StringTemplateSqlLocator.findStringTemplateGroup(NetworkListDaoImpl.class);
    }

    @Override
    public Optional<NetworkList> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        if (DbKey.isNull(id))
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
        selectTemplate.add(WHERE_CLAUSE, "where id = :id");
        try (var select = handle.createQuery(setDefines(selectTemplate, dbEngine, listMapper, listEntryMapper)))
        {
            registerMappers(select, listMapper, listEntryMapper);
            return select.bind(NetworkListMapper.Columns.ID.column(), id)
                         .reduceRows(new NetworkListReducer(listMapper, listEntryMapper))
                         .findFirst();
        }
    }

    @Override
    public Optional<NetworkList> getByName(DataTransaction tx, String name) throws OpenDcsDataException
    {
        if (name == null || name.isBlank())
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
        selectTemplate.add(WHERE_CLAUSE, "where upper(name) = upper(:name)");
        try (var select = handle.createQuery(setDefines(selectTemplate, dbEngine, listMapper, listEntryMapper)))
        {
            registerMappers(select, listMapper, listEntryMapper);
            return select.bind(NetworkListMapper.Columns.NAME.column(), name)
                         .reduceRows(new NetworkListReducer(listMapper, listEntryMapper))
                         .findFirst();
        }
    }

    @Override
    public NetworkList save(DataTransaction tx, NetworkList networkList) throws OpenDcsDataException
    {
        Objects.requireNonNull(networkList, "Cannot save null network list");
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();
        var keyGen = ctx.getGenerator(KeyGenerator.class)
                .orElseThrow(() -> new OpenDcsDataException("No key generator configured."));
        var mergeTemplate = queries.getInstanceOf(MERGE)
                                   .add("dual", dbEngine == DatabaseEngine.ORACLE ? "from dual" : "");
        var insertNetworkListEntry = queries.getInstanceOf(INSERT_NETWORKLIST_ENTRY).render();
        try (var merge = handle.createUpdate(mergeTemplate.render());
             var insertListEntry = handle.prepareBatch(insertNetworkListEntry))
        {
            DbKey id = networkList.getId();
            var existing = getByName(tx, networkList.name);
            if (existing.isPresent())
            {
                // If there's an existing app with this name, we'll just assume the provided id, if any, was in error
                id = existing.get().getId();
                log.trace("""
                    Using ID from existing NetworkList, id={}, that was found. Provided ID was {}.
                    """,
                    id, networkList.getId());
            }
            final var bindKey = !DbKey.isNull(id) ? id : keyGen.getKey("platform", handle.getConnection());
            merge.bind(NetworkListMapper.Columns.ID.column(), bindKey)
                 .bind(NetworkListMapper.Columns.NAME.column(), networkList.name)
                 .bind(NetworkListMapper.Columns.TRANSPORT_MEDIUM_TYPE.column(), networkList.transportMediumType)
                 .bind(NetworkListMapper.Columns.SITENAME_TYPE_PREFERENCE.column(), networkList.siteNameTypePref)
                 .bindByType(NetworkListMapper.Columns.LAST_MODIFY_TIME.column(),
                       ZonedDateTime.now(ZoneId.of("UTC")).toInstant().toEpochMilli(), Date.class)
                 .execute();
            deleteEntries(handle, bindKey);

            if (!networkList.networkListEntries.isEmpty())
            {
                for(var entry: networkList.networkListEntries.values())
                {
                    insertListEntry.bind(NetworkListEntryMapper.Columns.NETWORKLIST_ID.column(), bindKey)
                                   .bind(NetworkListEntryMapper.Columns.TRANSPORT_ID.column(), entry.transportId)
                                   .bind(NetworkListEntryMapper.Columns.PLATFORM_NAME.column(), entry.getPlatformName())
                                   .bind(NetworkListEntryMapper.Columns.DESCRIPTION.column(), entry.getDescription())
                                   .add();
                }
                insertListEntry.execute();
            }

            return getById(tx, bindKey).orElseThrow(() -> new OpenDcsDataException("Unable to retrieve network list we just saved."));
        }
        catch (DatabaseException ex)
        {
            throw new OpenDcsDataException("Unable to create new key for networklist", ex);
        }
    }

    @Override
    public void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        if (DbKey.isNull(id))
        {
            return;
        }
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
        var deleteTemplate = queries.getInstanceOf(DELETE_NETWORKLIST);

        if (deleteTemplate == null)
        {
            throw new OpenDcsDataException("Could not find template");
        }
        deleteEntries(handle, id);
        try (var deleteList = handle.createUpdate(deleteTemplate.render()))
        {
            deleteList.bind(NetworkListMapper.Columns.ID.column(), id).execute();
        }
    }

    private void deleteEntries(Handle handle, DbKey id) throws OpenDcsDataException
    {
        var deleteEntryTemplate = queries.getInstanceOf(DELETE_NETWORKLIST_ENTRY);
        if (deleteEntryTemplate == null)
        {
            throw new OpenDcsDataException("Could not find template");
        }
        try (var deleteEntry = handle.createUpdate(deleteEntryTemplate.render()))
        {
            deleteEntry.bind(NetworkListEntryMapper.Columns.NETWORKLIST_ID.column(), id).execute();
        }
    }

    @Override
    public List<NetworkList> getAll(DataTransaction tx, int limit, int offset, String mediumType, boolean includeEntries)
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
        String mediumSearch = null;
        if ("goes".equalsIgnoreCase(mediumType))
        {
            mediumSearch = " upper(transportmediumtype) like upper(:mediumType || '%') ";
        }
        else if (mediumType != null)
        {
            mediumSearch = " upper(transportmediumtype) = upper(:mediumType)";
        }
        
        if (mediumSearch != null)
        {
            selectTemplate.add("medium_filter", mediumSearch);
        }

        var limitClause = addLimitOffset(limit, offset);
        if (limitClause != null)
        {
            selectTemplate.add("limit", limitClause);
        }

        try (var select = handle.createQuery(setDefines(selectTemplate, dbEngine, listMapper, includeEntries ? listEntryMapper : null)))
        {
            if (includeEntries)
            {
                registerMappers(select, listMapper, listEntryMapper);
            }
            else
            {
                registerMappers(select, listMapper);
            }

            if (mediumSearch != null)
            {
                select.bind("mediumType", mediumType);
            }

            if (limitClause != null)
            {
                if (limit >= 0)
                {
                    select.bind(SqlKeywords.LIMIT, limit);
                }
                if (offset >= 0)
                {
                    select.bind(SqlKeywords.OFFSET, offset);
                }
            }
            return select.reduceRows(new NetworkListReducer(listMapper, includeEntries ? listEntryMapper : null))
                         .toList();
        }
    }

    private static Query registerMappers(Query query, PrefixRowMapper<?,?>... mappers)
    {
        for (var mapper: mappers)
        {
            query.registerRowMapper(mapper);
        }
        return query;
    }

    private static String setDefines(ST select, DatabaseEngine dbEngine, NetworkListMapper listMapper, NetworkListEntryMapper listEntryMapper)
    {
        select.add("list_columns", listMapper.columnsForSelect());
        if (listEntryMapper != null)
        {
            select.add("entry_columns", listEntryMapper.columnsForSelect());
            select.add("entry_join", listEntryMapper.joinStatement(LEFT_OUTER, 
                                                                         NetworkListEntryMapper.Columns.NETWORKLIST_ID,
                                                                         "nl",
                                                                         NetworkListMapper.Columns.ID.column()));
        }
        return select.add(COLLATE_CLAUSE, collateClauseFor(dbEngine)).render();
    }
}
