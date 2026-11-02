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
import static org.opendcs.utils.sql.SqlQueries.WHERE_CLAUSE;
import static org.opendcs.utils.sql.SqlQueries.addLimitOffset;
import static org.opendcs.utils.sql.SqlQueries.fromDual;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.PreparedBatch;
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
import org.opendcs.database.impl.opendcs.jdbi.mapper.timeseries.TsGroupMapper.Columns;
import org.opendcs.database.impl.opendcs.jdbi.mapper.timeseries.TsGroupTsMemberAccumulator;
import org.opendcs.database.impl.opendcs.jdbi.mapper.timeseries.TsGroupTsMemberAccumulator.MemberKey;
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
import decodes.tsdb.TsGroup;
import decodes.tsdb.TsGroupMember;

/**
 * Reads and writes the TSDB_GROUP table and its various member tables.
 *
 * Which groups make up a group is worked out by the database: every read of a group's
 * members starts with the recursive association in the {@code closure} query, and each
 * of the member tables is then read for the whole hierarchy in one go.
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

    /** How a sub-group is combined with its parent, see TsGroup.addSubGroup. */
    private static final char ADD = 'A';
    private static final char SUBTRACT = 'S';
    private static final char INTERSECT = 'I';

    private static final String CLOSURE = "closure";
    private static final String HEADERS = "headers";
    private static final String GROUPS = "groups";
    private static final String ID_MEMBERS = "members";
    private static final String OTHER_MEMBERS = "otherMembers";
    private static final String TS_MEMBERS = "tsMembers";

    private static final String GROUP_COLUMNS_KEY = "group_columns";
    private static final String SITE_COLUMNS_KEY = "site_columns";
    private static final String SITE_NAME_COLUMNS_KEY = "site_name_columns";
    private static final String TABLE_KEY = "table";
    private static final String COLUMN_KEY = "column";

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

    static final String INSERT_OTHER_MEMBERS = """
            insert into tsdb_group_member_other(group_id, member_type, member_value)
            values (:group_id, :member_type, :member_value)
            """;

    static final String INSERT_CHILD_GROUPS = """
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

    static final String DELETE_OTHER_MEMBERS = """
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

    private static final String GROUP_TABLE = "tsdb_group";

    @InjectDao
    CompDependsNotifyDao compDependsDao;

    private final STGroup queries = StringTemplateSqlLocator.findStringTemplateGroup(TsGroupDaoImpl.class);

    private final TsGroupMappers mappers = TsGroupMappers.withPrefixes();

    @Override
    public Optional<TsGroup> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException
    {
        if (DbKey.isNull(id))
        {
            return Optional.empty();
        }

        var group = findGroupById(getHandle(tx), tx.getContext().getDatabaseEngine(), id);
        if (group.isEmpty())
        {
            return Optional.empty();
        }
        return Optional.of(loadMembers(tx, group.get()));
    }

    @Override
    public Optional<TsGroup> getByName(DataTransaction tx, String name) throws OpenDcsDataException
    {
        if (name == null || name.isBlank())
        {
            return Optional.empty();
        }

        var group = findGroupByName(getHandle(tx), tx.getContext().getDatabaseEngine(), name);
        if (group.isEmpty())
        {
            return Optional.empty();
        }
        return Optional.of(loadMembers(tx, group.get()));
    }

    @Override
    public List<TsGroup> getAll(DataTransaction tx, String groupType, int limit, int offset) throws OpenDcsDataException
    {
        var handle = getHandle(tx);
        var dbEngine = tx.getContext().getDatabaseEngine();
        final var filterByType = groupType != null && !groupType.isBlank();
        try (var query = headers(handle, dbEngine,
                                 filterByType ? "where upper(tg.group_type) = upper(:group_type)" : "",
                                 limit, offset))
        {
            if (filterByType)
            {
                query.bind(Columns.GROUP_TYPE.column(), groupType);
            }
            if (limit >= 0)
            {
                query.bind(SqlKeywords.LIMIT, limit);
            }
            if (offset >= 0)
            {
                query.bind(SqlKeywords.OFFSET, offset);
            }

            return query.map(mappers.groupMapper()).list();
        }
    }

    @Override
    public TsGroup loadMembers(DataTransaction tx, TsGroup group) throws OpenDcsDataException
    {
        Objects.requireNonNull(group, "The group to read the members of must be provided.");
        if (DbKey.isNull(group.getGroupId()))
        {
            throw new OpenDcsDataException("Only a group that is in the database has members to read.");
        }

        var handle = getHandle(tx);
        var dbEngine = tx.getContext().getDatabaseEngine();
        var groupId = group.getGroupId();

        group.clear();
        // TsGroup.clear() leaves the groups it intersects with alone.
        group.setIntersectedGroups(new ArrayList<>());

        final var groups = new LinkedHashMap<DbKey, TsGroup>();
        final var edges = new ArrayList<GroupEdge>();
        readGroupTree(handle, dbEngine, groupId, groups, edges);
        // The caller gets back the instance it handed in, not the one that was just read.
        groups.put(groupId, group);

        readTimeSeriesMembers(handle, dbEngine, groupId, groups);
        readIdMembers(handle, dbEngine, groupId, "tsdb_group_member_site", "site_id",
                      groups, TsGroup::addSiteId);
        readIdMembers(handle, dbEngine, groupId, "tsdb_group_member_dt", "data_type_id",
                      groups, TsGroup::addDataTypeId);
        readOtherMembers(handle, dbEngine, groupId, groups);

        for (var edge : withoutCycles(groupId, edges))
        {
            groups.get(edge.parentId()).addSubGroup(groups.get(edge.childId()), edge.combine());
        }

        return group;
    }

    @Override
    public TsGroup save(DataTransaction tx, TsGroup group) throws OpenDcsDataException
    {
        Objects.requireNonNull(group, "Cannot save a null TsGroup");
        if (group.getGroupName() == null || group.getGroupName().isBlank())
        {
            throw new OpenDcsDataException("Cannot save a TsGroup without a name.");
        }
        if (group.getGroupType() == null || group.getGroupType().isBlank())
        {
            throw new OpenDcsDataException("Cannot save the TsGroup '" + group.getGroupName() + "' without a type.");
        }

        var handle = getHandle(tx);
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();
        var keyGen = ctx.getGenerator(KeyGenerator.class)
                        .orElseThrow(() -> new OpenDcsDataException("No key generator configured."));

        final DbKey groupId;
        try (var merge = handle.createUpdate(MERGE_GROUP)
                               .define(SqlQueries.DUAL, fromDual(dbEngine)))
        {
            DbKey id = group.getGroupId();
            if (DbKey.isNull(id))
            {
                // If a group with this name already exists, assume the missing ID was in error and reuse it.
                var existing = findGroupByName(handle, dbEngine, group.getGroupName());
                id = existing.isPresent() ? existing.get().getGroupId()
                                          : keyGen.getKey(GROUP_TABLE, handle.getConnection());
            }
            groupId = id;

            merge.bind(Columns.GROUP_ID.column(), groupId)
                 .bind(Columns.GROUP_NAME.column(), group.getGroupName())
                 .bind(Columns.GROUP_TYPE.column(), group.getGroupType())
                 .bind(Columns.GROUP_DESCRIPTION.column(), group.getDescription())
                 .execute();
        }
        catch (DatabaseException ex)
        {
            throw new OpenDcsDataException("Unable to generate key to save ts group '"
                                           + group.getGroupName() + "'", ex);
        }

        writeTsMembers(handle, groupId, group);
        writeIdMembers(handle, DELETE_SITE_MEMBERS, INSERT_SITE_MEMBERS, "site_id",
                       groupId, group.getSiteIdList());
        writeIdMembers(handle, DELETE_DATA_TYPE_MEMBERS, INSERT_DATA_TYPE_MEMBERS, "data_type_id",
                       groupId, group.getDataTypeIdList());
        writeOtherMembers(handle, groupId, group.getOtherMembers());
        writeChildGroups(handle, dbEngine, groupId, group);

        saveNotify(tx, groupId, CpDependsNotify.GRP_MODIFIED);

        return getById(tx, groupId)
                .orElseThrow(() -> new OpenDcsDataException("Unable to retrieve TsGroup we just saved."));
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
             var deleteOther = handle.createUpdate(DELETE_OTHER_MEMBERS);
             var deleteAssociations = handle.createUpdate(DELETE_GROUP_ASSOCIATIONS);
             var deleteGroup = handle.createUpdate(DELETE_GROUP))
        {
            deleteTs.bind(Columns.GROUP_ID.column(), id).execute();
            deleteSite.bind(Columns.GROUP_ID.column(), id).execute();
            deleteDataType.bind(Columns.GROUP_ID.column(), id).execute();
            deleteOther.bind(Columns.GROUP_ID.column(), id).execute();
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

    /**
     * The recursive part of each of the read queries, which walks the sub-group
     * associations down from the group being read.
     */
    private String closure(DatabaseEngine dbEngine)
    {
        return template(CLOSURE)
                .add(SqlQueries.RECURSIVE_CTE_CLAUSE, SqlQueries.recursiveCteFor(dbEngine))
                .render();
    }

    private Optional<TsGroup> findGroupById(Handle handle, DatabaseEngine dbEngine, DbKey id)
    {
        try (var query = headers(handle, dbEngine, "where tg.group_id = :group_id", -1, -1))
        {
            return query.bind(Columns.GROUP_ID.column(), id)
                        .map(mappers.groupMapper())
                        .findFirst();
        }
    }

    private Optional<TsGroup> findGroupByName(Handle handle, DatabaseEngine dbEngine, String name)
    {
        try (var query = headers(handle, dbEngine, "where lower(tg.group_name) = lower(:group_name)", -1, -1))
        {
            return query.bind(Columns.GROUP_NAME.column(), name)
                        .map(mappers.groupMapper())
                        .findFirst();
        }
    }

    private Query headers(Handle handle, DatabaseEngine dbEngine, String whereClause, int limit, int offset)
    {
        var select = template(HEADERS)
                        .add(WHERE_CLAUSE, whereClause)
                        .add(COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                        .add(LIMIT_CLAUSE, addLimitOffset(limit, offset))
                        .add(GROUP_COLUMNS_KEY, mappers.groupMapper().columnsForSelect());

        return handle.createQuery(select.render());
    }

    /**
     * Read the group being asked for and every group below it, along with the
     * associations that tie them together.
     */
    private void readGroupTree(Handle handle, DatabaseEngine dbEngine, DbKey groupId,
                               Map<DbKey, TsGroup> groups, List<GroupEdge> edges)
    {
        var select = template(GROUPS)
                        .add(CLOSURE, closure(dbEngine))
                        .add(COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                        .add(GROUP_COLUMNS_KEY, mappers.groupMapper().columnsForSelect());

        try (var query = handle.createQuery(select.render()))
        {
            var rows = query.bind(Columns.GROUP_ID.column(), groupId)
                            .map((rs, ctx) -> new GroupRow(mappers.groupMapper().map(rs, ctx),
                                                           DbKey.createDbKey(rs, "parent_group_id"),
                                                           combineOp(rs.getString("include_group"))))
                            .list();
            for (var row : rows)
            {
                // A group that is a sub-group of more than one group has a row for each.
                groups.putIfAbsent(row.group().getGroupId(), row.group());
                if (!row.group().getGroupId().equals(row.parentId()))
                {
                    edges.add(new GroupEdge(row.parentId(), row.group().getGroupId(), row.combine()));
                }
            }
        }
    }

    /**
     * Read the time series members of every group in {@code groups}. Their sites and
     * data types come along in the same query.
     */
    private void readTimeSeriesMembers(Handle handle, DatabaseEngine dbEngine, DbKey groupId,
                                       Map<DbKey, TsGroup> groups)
    {
        var select = template(TS_MEMBERS)
                        .add(CLOSURE, closure(dbEngine))
                        .add(COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                        .add(SITE_COLUMNS_KEY, mappers.siteMapper().columnsForSelect())
                        .add(SITE_NAME_COLUMNS_KEY, mappers.siteNameMapper().columnsForSelect());

        var accumulator = new TsGroupTsMemberAccumulator(mappers);
        final Map<MemberKey, TimeSeriesIdentifier> members;
        try (var query = handle.createQuery(select.render()))
        {
            members = query.bind(Columns.GROUP_ID.column(), groupId)
                           .reduceResultSet(new LinkedHashMap<MemberKey, TimeSeriesIdentifier>(), accumulator);
        }

        members.forEach((member, tsId) ->
        {
            var group = groups.get(member.groupId());
            if (group != null)
            {
                group.addTsMember(tsId);
            }
        });

        accumulator.getMissingTimeSeries().forEach(member ->
        {
            var group = groups.get(member.groupId());
            log.warn("Group (id={}) {} -- has time series member ID={} that is not in the "
                     + "time series identifier view -- ignored.",
                     member.groupId(), group != null ? group.getGroupName() : "?", member.tsId());
        });
    }

    private void readIdMembers(Handle handle, DatabaseEngine dbEngine, DbKey groupId, String table, String column,
                               Map<DbKey, TsGroup> groups, BiConsumer<TsGroup, DbKey> adder)
    {
        var select = template(ID_MEMBERS)
                        .add(CLOSURE, closure(dbEngine))
                        .add(TABLE_KEY, table)
                        .add(COLUMN_KEY, column);

        try (var query = handle.createQuery(select.render()))
        {
            query.bind(Columns.GROUP_ID.column(), groupId)
                 .map((rs, ctx) -> new MemberRow(DbKey.createDbKey(rs, "group_id"),
                                                 DbKey.createDbKey(rs, "member_id")))
                 .forEach(row -> Optional.ofNullable(groups.get(row.groupId()))
                                         .ifPresent(group -> adder.accept(group, row.memberId())));
        }
    }

    private void readOtherMembers(Handle handle, DatabaseEngine dbEngine, DbKey groupId, Map<DbKey, TsGroup> groups)
    {
        var select = template(OTHER_MEMBERS).add(CLOSURE, closure(dbEngine));

        try (var query = handle.createQuery(select.render()))
        {
            query.bind(Columns.GROUP_ID.column(), groupId)
                 .map((rs, ctx) -> new OtherMemberRow(DbKey.createDbKey(rs, "group_id"),
                                                      rs.getString("member_type"),
                                                      rs.getString("member_value")))
                 .forEach(row -> Optional.ofNullable(groups.get(row.groupId()))
                                         .ifPresent(group -> group.addOtherMember(row.type(), row.value())));
        }
    }

    /**
     * Nothing in the schema stops a group from being a sub-group of one of its own
     * sub-groups, and everything that uses a group hierarchy walks it recursively. Walk
     * the associations here instead, dropping any that would re-enter a group already on
     * the way to it.
     */
    private static List<GroupEdge> withoutCycles(DbKey groupId, List<GroupEdge> edges)
    {
        var byParent = new LinkedHashMap<DbKey, List<GroupEdge>>();
        edges.forEach(edge -> byParent.computeIfAbsent(edge.parentId(), id -> new ArrayList<>()).add(edge));

        var acyclic = new ArrayList<GroupEdge>();
        removeCycles(groupId, byParent, new ArrayDeque<>(), acyclic);
        return acyclic;
    }

    private static void removeCycles(DbKey parentId, Map<DbKey, List<GroupEdge>> byParent,
                                     Deque<DbKey> lineOfAncestry, List<GroupEdge> acyclic)
    {
        lineOfAncestry.push(parentId);
        for (var edge : byParent.getOrDefault(parentId, List.of()))
        {
            if (lineOfAncestry.contains(edge.childId()))
            {
                log.warn("Group (id={}) -- has sub group ID={} -- circular reference detected -- ignored.",
                         parentId, edge.childId());
                continue;
            }
            acyclic.add(edge);
            removeCycles(edge.childId(), byParent, lineOfAncestry, acyclic);
        }
        lineOfAncestry.pop();
    }

    private void writeTsMembers(Handle handle, DbKey groupId, TsGroup group) throws OpenDcsDataException
    {
        final var tsIds = new ArrayList<DbKey>();
        for (TimeSeriesIdentifier tsId : group.getTsMemberList())
        {
            if (tsId == null || DbKey.isNull(tsId.getKey()))
            {
                throw new OpenDcsDataException("Group '" + group.getGroupName()
                                               + "' has a time series member that isn't in the database.");
            }
            tsIds.add(tsId.getKey());
        }
        writeIdMembers(handle, DELETE_TS_MEMBERS, INSERT_TS_MEMBERS, "ts_id", groupId, tsIds);
    }

    /**
     * Members are always replaced wholesale, which is simpler and no slower than
     * working out what changed.
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

    private static void writeOtherMembers(Handle handle, DbKey groupId, Collection<TsGroupMember> members)
    {
        try (var delete = handle.createUpdate(DELETE_OTHER_MEMBERS);
             var insert = handle.prepareBatch(INSERT_OTHER_MEMBERS))
        {
            delete.bind(Columns.GROUP_ID.column(), groupId).execute();
            for (var member : members)
            {
                insert.bind(Columns.GROUP_ID.column(), groupId)
                      .bind("member_type", member.getMemberType())
                      .bind("member_value", member.getMemberValue())
                      .add();
            }
            insert.execute();
        }
    }

    private void writeChildGroups(Handle handle, DatabaseEngine dbEngine, DbKey groupId, TsGroup group)
            throws OpenDcsDataException
    {
        try (var delete = handle.createUpdate(DELETE_CHILD_GROUPS);
             var insert = handle.prepareBatch(INSERT_CHILD_GROUPS))
        {
            delete.bind("parent_group_id", groupId).execute();
            addChildren(handle, dbEngine, insert, groupId, group.getGroupName(),
                        group.getIncludedSubGroups(), ADD);
            addChildren(handle, dbEngine, insert, groupId, group.getGroupName(),
                        group.getExcludedSubGroups(), SUBTRACT);
            addChildren(handle, dbEngine, insert, groupId, group.getGroupName(),
                        group.getIntersectedGroups(), INTERSECT);
            insert.execute();
        }
    }

    private void addChildren(Handle handle, DatabaseEngine dbEngine, PreparedBatch insert, DbKey parentGroupId,
                             String parentName, Collection<TsGroup> children, char combine)
            throws OpenDcsDataException
    {
        for (var child : children)
        {
            if (child == null)
            {
                continue;
            }
            var childId = child.getGroupId();
            if (DbKey.isNull(childId))
            {
                childId = findGroupByName(handle, dbEngine, child.getGroupName())
                              .orElseThrow(() -> new OpenDcsDataException("Sub-group '" + child.getGroupName()
                                        + "' of group '" + parentName + "' isn't in the database."))
                              .getGroupId();
            }

            insert.bind("parent_group_id", parentGroupId)
                  .bind("child_group_id", childId)
                  .bind("include_group", String.valueOf(combine))
                  .add();
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

    /** The include_group column is a character, but may be unset in older databases. */
    private static char combineOp(String value)
    {
        return value == null || value.isBlank() ? ADD : Character.toUpperCase(value.charAt(0));
    }

    /** One row of the group hierarchy: a group and the association that pulled it in. */
    private record GroupRow(TsGroup group, DbKey parentId, char combine)
    {
    }

    /** One sub-group association between two groups of the hierarchy being read. */
    private record GroupEdge(DbKey parentId, DbKey childId, char combine)
    {
    }

    /** One row of the site or data type member tables. */
    private record MemberRow(DbKey groupId, DbKey memberId)
    {
    }

    /** One row of the tsdb_group_member_other table. */
    private record OtherMemberRow(DbKey groupId, String type, String value)
    {
    }
}
