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

import static org.opendcs.utils.sql.SqlQueries.COLLATE_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.LIMIT_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.RECURSIVE_CTE_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.WHERE_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;
import static org.opendcs.utils.sql.SqlQueries.fromDual;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.stringtemplate4.StringTemplateSqlLocator;
import org.opendcs.annotations.api.InjectDao;
import org.opendcs.database.DatabaseQuerySettings;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.CompDependsNotifyDao;
import org.opendcs.database.dai.TsGroupDao;
import org.opendcs.database.impl.opendcs.jdbi.mapper.timeseries.TsGroupMappers;
import org.opendcs.database.impl.opendcs.jdbi.mapper.timeseries.TsGroupRefMapper.Columns;
import org.opendcs.database.impl.opendcs.jdbi.mapper.timeseries.TsGroupTsMemberAccumulator;
import org.opendcs.database.model.tsgroup.GroupCombine;
import org.opendcs.database.model.tsgroup.GroupCriteria;
import org.opendcs.database.model.tsgroup.GroupDefinition;
import org.opendcs.database.model.tsgroup.GroupMembers;
import org.opendcs.database.model.tsgroup.SubGroupLink;
import org.opendcs.database.model.tsgroup.TsGroupRef;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.SqlKeywords;
import org.opendcs.utils.sql.SqlQueries;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;
import org.slf4j.Logger;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;

import decodes.db.DatabaseException;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;
import decodes.tsdb.CpDependsNotify;
import decodes.tsdb.TimeSeriesIdentifier;

/**
 * Reads and writes the TSDB_GROUP table and its member tables.
 *
 * Every read starts by deciding, in SQL, which groups it applies to - one group, a page of
 * them, or a whole hierarchy - and then reads each member table for all of those groups at
 * once. Which means the number of statements a read costs does not depend on how big the
 * group is, or how deep.
 *
 * NOTE: this is intentionally doing no caching at this time. Current focus is simply "correctness."
 */
@ServiceProviders({
    @ServiceProvider(service = TsGroupDao.class, path = "dao/OpenDCS-Postgres"),
    @ServiceProvider(service = TsGroupDao.class, path = "dao/OpenDCS-Oracle"),
    @ServiceProvider(service = TsGroupDao.class, path = "dao/OPENTSDB")
})
public class TsGroupDaoImpl implements TsGroupDao
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    private static final String FILTERED_GROUPS = "filteredGroups";
    private static final String HIERARCHY_GROUPS = "hierarchyGroups";
    private static final String AFFECTED_GROUPS = "affectedGroups";
    private static final String IDS = "ids";
    private static final String HEADERS = "headers";
    private static final String ID_MEMBERS = "idMembers";
    private static final String ATTRIBUTES = "attributes";
    private static final String LINKS = "links";
    private static final String TS_MEMBERS = "tsMembers";

    private static final String GROUPS_KEY = "groups";
    private static final String REF_COLUMNS_KEY = "ref_columns";
    private static final String CHILD_COLUMNS_KEY = "child_columns";
    private static final String SITE_COLUMNS_KEY = "site_columns";
    private static final String SITE_NAME_COLUMNS_KEY = "site_name_columns";
    private static final String TABLE_KEY = "table";
    private static final String COLUMN_KEY = "column";

    private static final String GROUP_TABLE = "tsdb_group";
    private static final String TS_MEMBER_TABLE = "tsdb_group_member_ts";
    private static final String TS_MEMBER_COLUMN = "ts_id";
    private static final String SITE_MEMBER_TABLE = "tsdb_group_member_site";
    private static final String SITE_MEMBER_COLUMN = "site_id";
    private static final String DATA_TYPE_MEMBER_TABLE = "tsdb_group_member_dt";
    private static final String DATA_TYPE_MEMBER_COLUMN = "data_type_id";

    private static final String BY_ID = "where tg.group_id = :group_id";
    private static final String BY_NAME = "where lower(tg.group_name) = lower(:group_name)";
    private static final String BY_TYPE = "where upper(tg.group_type) = upper(:group_type)";
    private static final String EVERY_GROUP = "";

    static final String MERGE_GROUP = """
            merge into tsdb_group tg
            using (select :group_id group_id, :group_name group_name, :group_type group_type,
                          :group_description group_description <dual>) input
            on (tg.group_id = input.group_id)
            when matched then
                update set group_name = input.group_name, group_type = input.group_type,
                        group_description = input.group_description
            when not matched then
                insert(group_id, group_name, group_type, group_description)
                values(input.group_id, input.group_name, input.group_type, input.group_description)
            """;

    static final String INSERT_TS_MEMBERS = """
            insert into tsdb_group_member_ts(group_id, ts_id) values (:group_id, :ts_id)
            """;

    static final String INSERT_SITE_MEMBERS = """
            insert into tsdb_group_member_site(group_id, site_id) values (:group_id, :site_id)
            """;

    static final String INSERT_DATA_TYPE_MEMBERS = """
            insert into tsdb_group_member_dt(group_id, data_type_id) values (:group_id, :data_type_id)
            """;

    static final String INSERT_ATTRIBUTES = """
            insert into tsdb_group_member_other(group_id, member_type, member_value)
            values (:group_id, :member_type, :member_value)
            """;

    static final String INSERT_LINKS = """
            insert into tsdb_group_member_group(parent_group_id, child_group_id, include_group)
            values (:parent_group_id, :child_group_id, :include_group)
            """;

    static final String DELETE_TS_MEMBERS = """
            delete from tsdb_group_member_ts where group_id = :group_id
            """;

    static final String DELETE_SITE_MEMBERS = """
            delete from tsdb_group_member_site where group_id = :group_id
            """;

    static final String DELETE_DATA_TYPE_MEMBERS = """
            delete from tsdb_group_member_dt where group_id = :group_id
            """;

    static final String DELETE_ATTRIBUTES = """
            delete from tsdb_group_member_other where group_id = :group_id
            """;

    static final String DELETE_CHILD_GROUPS = """
            delete from tsdb_group_member_group where parent_group_id = :parent_group_id
            """;

    /** Removes a group from both sides of any association, used when the group itself goes away. */
    static final String DELETE_GROUP_ASSOCIATIONS = """
            delete from tsdb_group_member_group
             where parent_group_id = :group_id or child_group_id = :group_id
            """;

    static final String DELETE_GROUP = """
            delete from tsdb_group where group_id = :group_id
            """;

    static final String COUNT_COMPS_USING_GROUP = """
            select count(*) from cp_computation where group_id = :group_id
            """;

    /** Computations that can no longer run because their group is going away are disabled. */
    static final String DISABLE_COMPS_USING_GROUP = """
            update cp_computation
               set enabled = 'N', group_id = null, date_time_loaded = :date_time_loaded
             where group_id = :group_id
            """;

    static final String SELECT_COMPS_USING_GROUP = """
            select computation_id from cp_computation where group_id = :group_id
            """;

    static final String SELECT_PARENT_GROUPS = """
            select distinct parent_group_id from tsdb_group_member_group where child_group_id = :group_id
            """;

    static final String DELETE_CHILD_REFERENCE = """
            delete from tsdb_group_member_group where child_group_id = :group_id
            """;

    @InjectDao
    CompDependsNotifyDao compDependsDao;

    private final STGroup queries = StringTemplateSqlLocator.findStringTemplateGroup(TsGroupDaoImpl.class);

    private final TsGroupMappers mappers = TsGroupMappers.withPrefixes();

    @Override
    public Optional<TsGroupRef> ref(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        if (DbKey.isNull(id))
        {
            return Optional.empty();
        }
        return first(readRefs(getHandle(tx), tx.getContext().getDatabaseEngine(),
                              filteredGroups(dbEngineOf(tx), BY_ID, -1, -1), binds(Columns.GROUP_ID.column(), id)));
    }

    @Override
    public Optional<TsGroupRef> refByName(DataTransaction tx, String name) throws OpenDcsDataException
    {
        if (name == null || name.isBlank())
        {
            return Optional.empty();
        }
        return first(readRefs(getHandle(tx), tx.getContext().getDatabaseEngine(),
                              filteredGroups(dbEngineOf(tx), BY_NAME, -1, -1), binds(Columns.GROUP_NAME.column(), name)));
    }

    @Override
    public List<TsGroupRef> refs(DataTransaction tx, String groupType, int limit, int offset)
            throws OpenDcsDataException
    {
        var filterByType = hasText(groupType);
        return List.copyOf(readRefs(getHandle(tx), tx.getContext().getDatabaseEngine(),
                                    filteredGroups(dbEngineOf(tx), filterByType ? BY_TYPE : EVERY_GROUP, limit, offset),
                                    typeBinds(groupType, limit, offset)).values());
    }

    @Override
    public Optional<GroupDefinition> definition(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        if (DbKey.isNull(id))
        {
            return Optional.empty();
        }
        return Optional.ofNullable(readDefinitions(tx, filteredGroups(dbEngineOf(tx), BY_ID, -1, -1),
                                                  binds(Columns.GROUP_ID.column(), id)).get(id));
    }

    @Override
    public Optional<GroupDefinition> definitionByName(DataTransaction tx, String name) throws OpenDcsDataException
    {
        if (name == null || name.isBlank())
        {
            return Optional.empty();
        }
        var found = readDefinitions(tx, filteredGroups(dbEngineOf(tx), BY_NAME, -1, -1), binds(Columns.GROUP_NAME.column(), name));
        return found.values().stream().findFirst();
    }

    @Override
    public List<GroupDefinition> definitions(DataTransaction tx, String groupType, int limit, int offset)
            throws OpenDcsDataException
    {
        var filterByType = hasText(groupType);
        return List.copyOf(readDefinitions(tx, filteredGroups(dbEngineOf(tx), filterByType ? BY_TYPE : EVERY_GROUP, limit, offset),
                                           typeBinds(groupType, limit, offset)).values());
    }

    @Override
    public Map<DbKey, GroupDefinition> definitionsInHierarchy(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        if (DbKey.isNull(id))
        {
            return Map.of();
        }
        return readDefinitions(tx, hierarchyGroups(dbEngineOf(tx), HIERARCHY_GROUPS), binds(Columns.GROUP_ID.column(), id));
    }

    @Override
    public GroupMembers members(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        if (DbKey.isNull(id))
        {
            return GroupMembers.EMPTY;
        }

        var handle = getHandle(tx);
        var select = template(TS_MEMBERS)
                        .add(COLLATE_CLAUSE, SqlQueries.collateClauseFor(tx.getContext().getDatabaseEngine()))
                        .add(SITE_COLUMNS_KEY, mappers.siteMapper().columnsForSelect())
                        .add(SITE_NAME_COLUMNS_KEY, mappers.siteNameMapper().columnsForSelect());

        var accumulator = new TsGroupTsMemberAccumulator(mappers);
        final Map<DbKey, TimeSeriesIdentifier> found;
        try (var query = handle.createQuery(select.render()))
        {
            found = query.bind(Columns.GROUP_ID.column(), id)
                         .reduceResultSet(new LinkedHashMap<DbKey, TimeSeriesIdentifier>(), accumulator);
        }

        accumulator.getUnresolved().forEach(tsId ->
        {
            log.warn("Group (id={}) has time series member ID={} that has no time series identifier -- ignored.",
                     id, tsId);
        });

        return new GroupMembers(List.copyOf(found.values()), accumulator.getUnresolved());
    }

    @Override
    public Set<DbKey> descendants(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        return groupIds(tx, HIERARCHY_GROUPS, id);
    }

    @Override
    public Set<DbKey> ancestors(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        return groupIds(tx, AFFECTED_GROUPS, id);
    }

    @Override
    public GroupDefinition save(DataTransaction tx, GroupDefinition definition) throws OpenDcsDataException
    {
        Objects.requireNonNull(definition, "Cannot save a null group definition");
        var criteria = definition.criteria();
        requireSavedKeys(criteria.timeSeriesKeys(), "time series", definition.name());
        requireSavedKeys(criteria.siteIds(), "site", definition.name());
        requireSavedKeys(criteria.dataTypeIds(), "data type", definition.name());

        var handle = getHandle(tx);
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();
        var keyGen = ctx.getGenerator(KeyGenerator.class)
                        .orElseThrow(() -> new OpenDcsDataException("No key generator configured."));

        final DbKey groupId;
        try (var merge = handle.createUpdate(MERGE_GROUP)
                               .define(SqlQueries.DUAL, fromDual(dbEngine)))
        {
            DbKey id = definition.id();
            if (DbKey.isNull(id))
            {
                // If a group of this name is already here, assume the missing id was in error
                // and update that one instead of failing on the unique name.
                var existing = refByName(handle, dbEngine, definition.name());
                id = existing.isPresent() ? existing.get().id()
                                          : keyGen.getKey(GROUP_TABLE, handle.getConnection());
            }
            groupId = id;

            merge.bind(Columns.GROUP_ID.column(), groupId)
                 .bind(Columns.GROUP_NAME.column(), definition.name())
                 .bind(Columns.GROUP_TYPE.column(), definition.type())
                 .bind(Columns.GROUP_DESCRIPTION.column(), definition.description())
                 .execute();
        }
        catch (DatabaseException ex)
        {
            throw new OpenDcsDataException("Unable to generate a key to save the group '"
                                           + definition.name() + "'", ex);
        }

        writeIdMembers(handle, DELETE_TS_MEMBERS, INSERT_TS_MEMBERS, TS_MEMBER_COLUMN,
                       groupId, criteria.timeSeriesKeys());
        writeIdMembers(handle, DELETE_SITE_MEMBERS, INSERT_SITE_MEMBERS, SITE_MEMBER_COLUMN,
                       groupId, criteria.siteIds());
        writeIdMembers(handle, DELETE_DATA_TYPE_MEMBERS, INSERT_DATA_TYPE_MEMBERS, DATA_TYPE_MEMBER_COLUMN,
                       groupId, criteria.dataTypeIds());
        writeAttributes(handle, groupId, criteria);
        writeLinks(handle, dbEngine, groupId, definition);

        saveNotify(tx, groupId, CpDependsNotify.GRP_MODIFIED);

        return definition(tx, groupId)
                .orElseThrow(() -> new OpenDcsDataException("Unable to read back the group we just saved."));
    }

    @Override
    public void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        if (DbKey.isNull(id))
        {
            return;
        }

        removeDependenciesFor(tx, id);

        var handle = getHandle(tx);
        try (var deleteTs = handle.createUpdate(DELETE_TS_MEMBERS);
             var deleteSite = handle.createUpdate(DELETE_SITE_MEMBERS);
             var deleteDataType = handle.createUpdate(DELETE_DATA_TYPE_MEMBERS);
             var deleteAttributes = handle.createUpdate(DELETE_ATTRIBUTES);
             var deleteAssociations = handle.createUpdate(DELETE_GROUP_ASSOCIATIONS);
             var deleteGroup = handle.createUpdate(DELETE_GROUP))
        {
            deleteTs.bind(Columns.GROUP_ID.column(), id).execute();
            deleteSite.bind(Columns.GROUP_ID.column(), id).execute();
            deleteDataType.bind(Columns.GROUP_ID.column(), id).execute();
            deleteAttributes.bind(Columns.GROUP_ID.column(), id).execute();
            deleteAssociations.bind(Columns.GROUP_ID.column(), id).execute();
            deleteGroup.bind(Columns.GROUP_ID.column(), id).execute();
        }
    }

    @Override
    public int countCompsUsing(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        var handle = getHandle(tx);
        try (var count = handle.createQuery(COUNT_COMPS_USING_GROUP))
        {
            return count.bind(Columns.GROUP_ID.column(), id)
                        .mapTo(Integer.class)
                        .findOne()
                        .orElse(0);
        }
    }

    @Override
    public void removeDependenciesFor(DataTransaction tx, DbKey deletedGroupId) throws OpenDcsDataException
    {
        if (DbKey.isNull(deletedGroupId))
        {
            return;
        }

        var handle = getHandle(tx);
        var numericDate = tx.getContext().getSettings(DatabaseQuerySettings.class)
                            .map(DatabaseQuerySettings::numericDate)
                            .orElse(true);

        final List<DbKey> compIds;
        final List<DbKey> parentGroupIds;
        try (var selectComps = handle.createQuery(SELECT_COMPS_USING_GROUP)
                                     .bind(Columns.GROUP_ID.column(), deletedGroupId);
             var selectParents = handle.createQuery(SELECT_PARENT_GROUPS)
                                       .bind(Columns.GROUP_ID.column(), deletedGroupId))
        {
            compIds = selectComps.mapTo(DbKey.class).list();
            parentGroupIds = selectParents.mapTo(DbKey.class).list();
        }

        try (var disableComps = handle.createUpdate(DISABLE_COMPS_USING_GROUP)
                                      .define("numeric_date", numericDate);
             var deleteChildren = handle.createUpdate(DELETE_CHILD_REFERENCE))
        {
            disableComps.bind(Columns.GROUP_ID.column(), deletedGroupId)
                        .bindByType("date_time_loaded", new Date(), Date.class)
                        .execute();
            deleteChildren.bind(Columns.GROUP_ID.column(), deletedGroupId).execute();
        }

        // Let the computation dependency updater know that the affected
        // computations and parent groups need to be reconciled.
        compIds.forEach(compId -> saveNotify(tx, compId, CpDependsNotify.CMP_MODIFIED));
        parentGroupIds.forEach(groupId -> saveNotify(tx, groupId, CpDependsNotify.GRP_MODIFIED));
    }

    private Map<DbKey, GroupDefinition> readDefinitions(DataTransaction tx, String groups,
                                                        Map<String, Object> binds) throws OpenDcsDataException
    {
        var handle = getHandle(tx);
        var parts = new Parts(readRefs(handle, tx.getContext().getDatabaseEngine(), groups, binds));

        readIdMembers(handle, groups, binds, TS_MEMBER_TABLE, TS_MEMBER_COLUMN, parts::timeSeries);
        readIdMembers(handle, groups, binds, SITE_MEMBER_TABLE, SITE_MEMBER_COLUMN, parts::site);
        readIdMembers(handle, groups, binds, DATA_TYPE_MEMBER_TABLE, DATA_TYPE_MEMBER_COLUMN, parts::dataType);
        readAttributes(handle, groups, binds, parts);
        readLinks(handle, tx.getContext().getDatabaseEngine(), groups, binds, parts);

        return parts.build();
    }

    private Map<DbKey, TsGroupRef> readRefs(Handle handle, DatabaseEngine dbEngine, String groups,
                                            Map<String, Object> binds)
    {
        var select = template(HEADERS)
                        .add(GROUPS_KEY, groups)
                        .add(COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                        .add(REF_COLUMNS_KEY, mappers.refMapper().columnsForSelect());

        var refs = new LinkedHashMap<DbKey, TsGroupRef>();
        try (var query = handle.createQuery(select.render()))
        {
            bindAll(query, binds).map(mappers.refMapper())
                                 .forEach(ref -> Optional.ofNullable(ref).ifPresent(read -> refs.put(read.id(), read)));
        }
        return refs;
    }

    private void readIdMembers(Handle handle, String groups, Map<String, Object> binds, String table, String column,
                               BiConsumer<DbKey, DbKey> adder)
    {
        var select = template(ID_MEMBERS)
                        .add(GROUPS_KEY, groups)
                        .add(TABLE_KEY, table)
                        .add(COLUMN_KEY, column);

        try (var query = handle.createQuery(select.render()))
        {
            bindAll(query, binds)
                .map((rs, ctx) -> new MemberRow(DbKey.createDbKey(rs, "group_id"),
                                                DbKey.createDbKey(rs, "member_id")))
                .forEach(row -> adder.accept(row.groupId(), row.memberId()));
        }
    }

    private void readAttributes(Handle handle, String groups, Map<String, Object> binds, Parts parts)
    {
        var select = template(ATTRIBUTES).add(GROUPS_KEY, groups);

        try (var query = handle.createQuery(select.render()))
        {
            bindAll(query, binds)
                .map((rs, ctx) -> new AttributeRow(DbKey.createDbKey(rs, "group_id"),
                                                   rs.getString("member_type"),
                                                   rs.getString("member_value")))
                .forEach(row -> parts.attribute(row.groupId(), row.part(), row.value()));
        }
    }

    private void readLinks(Handle handle, DatabaseEngine dbEngine, String groups, Map<String, Object> binds,
                           Parts parts)
    {
        var select = template(LINKS)
                        .add(GROUPS_KEY, groups)
                        .add(CHILD_COLUMNS_KEY, mappers.childRefMapper().columnsForSelect());

        try (var query = handle.createQuery(select.render()))
        {
            bindAll(query, binds)
                .map((rs, ctx) -> new LinkRow(DbKey.createDbKey(rs, "group_id"),
                                              mappers.childRefMapper().map(rs, ctx),
                                              GroupCombine.fromValue(rs.getString("include_group"))))
                .forEach(row -> parts.link(row.groupId(), new SubGroupLink(row.child(), row.combine())));
        }
    }

    private Set<DbKey> groupIds(DataTransaction tx, String groupsTemplate, DbKey id) throws OpenDcsDataException
    {
        if (DbKey.isNull(id))
        {
            return Set.of();
        }

        var select = template(IDS).add(GROUPS_KEY, hierarchyGroups(dbEngineOf(tx), groupsTemplate));
        try (var query = getHandle(tx).createQuery(select.render()))
        {
            return new LinkedHashSet<>(query.bind(Columns.GROUP_ID.column(), id).mapTo(DbKey.class).list());
        }
    }

    /** @return the SQL that picks out one group by id, name, type, or a page of all of them. */
    /** @return the SQL picking out the groups a read applies to: one, a type, or all of them. */
    private String filteredGroups(DatabaseEngine dbEngine, String whereClause, int limit, int offset)
    {
        return template(FILTERED_GROUPS)
                .add(WHERE_CLAUSE, whereClause)
                .add(COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                .add(LIMIT_CLAUSE, addLimitOffset(limit, offset))
                .render();
    }

    /** @return the SQL that walks a hierarchy, down from a group or up to its ancestors. */
    private String hierarchyGroups(DatabaseEngine dbEngine, String templateName)
    {
        return template(templateName)
                .add(RECURSIVE_CTE_CLAUSE, SqlQueries.recursiveCteFor(dbEngine))
                .render();
    }

    private Optional<TsGroupRef> refByName(Handle handle, DatabaseEngine dbEngine, String name)
    {
        return first(readRefs(handle, dbEngine, filteredGroups(dbEngine, BY_NAME, -1, -1),
                              binds(Columns.GROUP_NAME.column(), name)));
    }

    private static Map<String, Object> typeBinds(String groupType, int limit, int offset)
    {
        var binds = new LinkedHashMap<String, Object>();
        if (hasText(groupType))
        {
            binds.put(Columns.GROUP_TYPE.column(), groupType);
        }
        if (limit >= 0)
        {
            binds.put(SqlKeywords.LIMIT, limit);
        }
        if (offset >= 0)
        {
            binds.put(SqlKeywords.OFFSET, offset);
        }
        return binds;
    }

    private static Map<String, Object> binds(String name, Object value)
    {
        var binds = new LinkedHashMap<String, Object>();
        binds.put(name, value);
        return binds;
    }

    private static Query bindAll(Query query, Map<String, Object> binds)
    {
        binds.forEach(query::bind);
        return query;
    }

    private static Optional<TsGroupRef> first(Map<DbKey, TsGroupRef> refs)
    {
        return refs.values().stream().findFirst();
    }

    private static DatabaseEngine dbEngineOf(DataTransaction tx)
    {
        return tx.getContext().getDatabaseEngine();
    }

    private static boolean hasText(String value)
    {
        return value != null && !value.isBlank();
    }

    /**
     * Members are always replaced wholesale, which is simpler and no slower than working
     * out what changed.
     */
    private static void writeIdMembers(Handle handle, String deleteSql, String insertSql, String memberColumn,
                                       DbKey groupId, Collection<DbKey> memberIds)
    {
        try (var delete = handle.createUpdate(deleteSql);
             var insert = handle.prepareBatch(insertSql))
        {
            delete.bind(Columns.GROUP_ID.column(), groupId).execute();
            for (var memberId : memberIds)
            {
                insert.bind(Columns.GROUP_ID.column(), groupId)
                      .bind(memberColumn, memberId)
                      .add();
            }
            insert.execute();
        }
    }

    private static void writeAttributes(Handle handle, DbKey groupId,
                                        GroupCriteria criteria)
    {
        try (var delete = handle.createUpdate(DELETE_ATTRIBUTES);
             var insert = handle.prepareBatch(INSERT_ATTRIBUTES))
        {
            delete.bind(Columns.GROUP_ID.column(), groupId).execute();
            criteria.attributes().byPart().forEach((part, values) -> values.forEach(value ->
            {
                insert.bind(Columns.GROUP_ID.column(), groupId)
                      .bind("member_type", part)
                      .bind("member_value", value)
                      .add();
            }));
            insert.execute();
        }
    }

    private void writeLinks(Handle handle, DatabaseEngine dbEngine, DbKey groupId, GroupDefinition definition)
            throws OpenDcsDataException
    {
        try (var delete = handle.createUpdate(DELETE_CHILD_GROUPS);
             var insert = handle.prepareBatch(INSERT_LINKS))
        {
            delete.bind("parent_group_id", groupId).execute();
            for (var link : definition.subGroups())
            {
                var childId = link.group().id();
                if (DbKey.isNull(childId))
                {
                    childId = refByName(handle, dbEngine, link.group().name())
                                  .orElseThrow(() -> new OpenDcsDataException("Sub-group '" + link.group().name()
                                            + "' of group '" + definition.name() + "' isn't in the database."))
                                  .id();
                }

                insert.bind("parent_group_id", groupId)
                      .bind("child_group_id", childId)
                      .bind("include_group", String.valueOf(link.combine().value()))
                      .add();
            }
            insert.execute();
        }
    }

    private static void requireSavedKeys(Collection<DbKey> keys, String what, String groupName)
            throws OpenDcsDataException
    {
        for (var key : keys)
        {
            if (DbKey.isNull(key))
            {
                throw new OpenDcsDataException("The group '" + groupName + "' has a " + what
                                               + " member that isn't in the database.");
            }
        }
    }

    private void saveNotify(DataTransaction tx, DbKey id, char eventType)
    {
        try
        {
            CpDependsNotify cdn = new CpDependsNotify();
            cdn.setKey(id);
            cdn.setEventType(eventType);
            compDependsDao.saveRecord(tx, cdn);
        }
        catch (OpenDcsDataException ex)
        {
            log.atWarn().setCause(ex).log("Unable to save {} notification record for group id={}",
                                          eventType, id);
        }
    }

    private ST template(String name)
    {
        var template = queries.getInstanceOf(name);
        return Objects.requireNonNull(template, () -> "No '" + name + "' query for " + getClass().getSimpleName());
    }

    private static Handle getHandle(DataTransaction tx) throws OpenDcsDataException
    {
        return tx.connection(Handle.class)
                 .orElseThrow(() -> new OpenDcsDataException(SqlErrorMessages.NO_JDBI_HANDLE));
    }

    /** One row of a group's key-only member tables. */
    private record MemberRow(DbKey groupId, DbKey memberId)
    {
    }

    /** One row of tsdb_group_member_other. */
    private record AttributeRow(DbKey groupId, String part, String value)
    {
    }

    /** One row of tsdb_group_member_group, with the child group read in. */
    private record LinkRow(DbKey groupId, TsGroupRef child, GroupCombine combine)
    {
    }

    /**
     * Holds one builder per group while the member tables are read a table at a time, then
     * builds them all.
     */
    private static final class Parts
    {
        private final Map<DbKey, GroupDefinition.Builder> builders = new LinkedHashMap<>();

        Parts(Map<DbKey, TsGroupRef> refs)
        {
            refs.values().forEach(ref -> builders.put(ref.id(), GroupDefinition.builder(ref)));
        }

        void timeSeries(DbKey groupId, DbKey tsId)
        {
            forEachBuilder(groupId, builder -> builder.timeSeries(tsId));
        }

        void site(DbKey groupId, DbKey siteId)
        {
            forEachBuilder(groupId, builder -> builder.sites(siteId));
        }

        void dataType(DbKey groupId, DbKey dataTypeId)
        {
            forEachBuilder(groupId, builder -> builder.dataTypes(dataTypeId));
        }

        void attribute(DbKey groupId, String part, String value)
        {
            forEachBuilder(groupId, builder -> builder.attribute(part, value));
        }

        void link(DbKey groupId, SubGroupLink link)
        {
            forEachBuilder(groupId, builder -> builder.combine(link.group(), link.combine()));
        }

        Map<DbKey, GroupDefinition> build()
        {
            var definitions = new LinkedHashMap<DbKey, GroupDefinition>();
            builders.forEach((id, builder) -> definitions.put(id, builder.build()));
            return definitions;
        }

        private void forEachBuilder(DbKey groupId, Consumer<GroupDefinition.Builder> add)
        {
            var builder = builders.get(groupId);
            if (builder == null)
            {
                // The member tables all have a foreign key back to tsdb_group, so this can
                // only be a group that went away while it was being read.
                log.warn("A member of group (id={}) has no group to read into -- ignored.", groupId);
                return;
            }
            add.accept(builder);
        }
    }
}
