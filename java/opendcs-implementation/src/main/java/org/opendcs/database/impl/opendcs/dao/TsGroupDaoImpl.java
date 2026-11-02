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

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.Query;
import org.opendcs.annotations.api.InjectDao;
import org.opendcs.database.DatabaseQuerySettings;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.CompDependsNotifyDao;
import org.opendcs.database.dai.TimeSeriesIdentifierDao;
import org.opendcs.database.dai.TsGroupDao;
import org.opendcs.database.impl.opendcs.jdbi.mapper.timeseries.TsGroupMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.timeseries.TsGroupMapper.Columns;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.SqlKeywords;
import org.opendcs.utils.sql.SqlQueries;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;
import org.slf4j.Logger;

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

    static final String SELECT_QUERY = """
            select tg.group_id tg_group_id, tg.group_name tg_group_name,
                   tg.group_type tg_group_type, tg.group_description tg_group_description
              from tsdb_group tg
            <where>
             order by tg.group_name <collate> asc
            <limit>
            """;

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

    static final String SELECT_TS_MEMBERS = """
            select ts_id from tsdb_group_member_ts where group_id = :group_id
            """;

    static final String SELECT_SITE_MEMBERS = """
            select site_id from tsdb_group_member_site where group_id = :group_id
            """;

    static final String SELECT_DATA_TYPE_MEMBERS = """
            select data_type_id from tsdb_group_member_dt where group_id = :group_id
            """;

    static final String SELECT_OTHER_MEMBERS = """
            select member_type, member_value from tsdb_group_member_other where group_id = :group_id
            """;

    static final String SELECT_CHILD_GROUPS = """
            select child_group_id, include_group from tsdb_group_member_group
             where parent_group_id = :parent_group_id
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

    static final String SELECT_COMPS_USING_GROUP = """
            select computation_id from cp_computation where group_id = :group_id
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

    static final String SELECT_PARENT_GROUPS = """
            select distinct parent_group_id from tsdb_group_member_group where child_group_id = :group_id
            """;

    static final String DELETE_CHILD_REFERENCE = """
            delete from tsdb_group_member_group where child_group_id = :group_id
            """;

    @InjectDao
    TimeSeriesIdentifierDao timeSeriesDao;

    @InjectDao
    CompDependsNotifyDao compDependsDao;

    private final TsGroupMapper groupMapper = TsGroupMapper.withPrefix("tg");

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
        try (var query = handle.createQuery(SELECT_QUERY))
        {
            query.define(COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                 .define(WHERE_CLAUSE, filterByType ? "where upper(tg.group_type) = upper(:group_type)" : "")
                 .define(LIMIT_CLAUSE, addLimitOffset(limit, offset));
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

            return query.map(groupMapper).list();
        }
    }

    @Override
    public TsGroup loadMembers(DataTransaction tx, TsGroup group) throws OpenDcsDataException
    {
        Objects.requireNonNull(group, "The group to read the members of must be provided.");
        return readMembers(tx, group, new LinkedHashMap<>(), new ArrayDeque<>());
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
                                          : keyGen.getKey("tsdb_group", handle.getConnection());
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
        writeChildGroups(handle, dbEngine, group, groupId);

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

    private Optional<TsGroup> findGroupById(Handle handle, DatabaseEngine dbEngine, DbKey id)
    {
        try (var query = groupQuery(handle, dbEngine))
        {
            return query.define(WHERE_CLAUSE, "where tg.group_id = :group_id")
                        .bind(Columns.GROUP_ID.column(), id)
                        .map(groupMapper)
                        .findFirst();
        }
    }

    private Optional<TsGroup> findGroupByName(Handle handle, DatabaseEngine dbEngine, String name)
    {
        try (var query = groupQuery(handle, dbEngine))
        {
            return query.define(WHERE_CLAUSE, "where lower(tg.group_name) = lower(:group_name)")
                        .bind(Columns.GROUP_NAME.column(), name)
                        .map(groupMapper)
                        .findFirst();
        }
    }

    private static Query groupQuery(Handle handle, DatabaseEngine dbEngine)
    {
        return handle.createQuery(SELECT_QUERY)
                     .define(COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine))
                     .define(LIMIT_CLAUSE, "");
    }

    /**
     * Read each of the member tables for the given group and then recurse into any
     * sub-groups. The {@code loaded} map keeps track of everything read so far so
     * that a group appearing more than once is only read once, while
     * {@code lineOfAncestry} detects cycles.
     */
    private TsGroup readMembers(DataTransaction tx, TsGroup group, Map<DbKey,TsGroup> loaded,
                                Deque<DbKey> lineOfAncestry) throws OpenDcsDataException
    {
        var handle = getHandle(tx);
        var dbEngine = tx.getContext().getDatabaseEngine();

        loaded.put(group.getGroupId(), group);
        lineOfAncestry.push(group.getGroupId());
        group.clear();
        // TsGroup.clear() leaves the groups it intersects with alone.
        group.setIntersectedGroups(new ArrayList<>());

        final List<DbKey> tsIds;
        final List<DbKey> siteIds;
        final List<DbKey> dataTypeIds;
        final List<TsGroupMember> otherMembers;
        final List<ChildGroup> childGroups;
        try (var tsMembers = handle.createQuery(SELECT_TS_MEMBERS)
                                   .bind(Columns.GROUP_ID.column(), group.getGroupId());
             var siteMembers = handle.createQuery(SELECT_SITE_MEMBERS)
                                     .bind(Columns.GROUP_ID.column(), group.getGroupId());
             var dataTypeMembers = handle.createQuery(SELECT_DATA_TYPE_MEMBERS)
                                         .bind(Columns.GROUP_ID.column(), group.getGroupId());
             var others = handle.createQuery(SELECT_OTHER_MEMBERS)
                                .bind(Columns.GROUP_ID.column(), group.getGroupId());
             var children = handle.createQuery(SELECT_CHILD_GROUPS)
                                  .bind(Columns.GROUP_ID.column(), group.getGroupId()))
        {
            // Read everything into lists before recursing, otherwise the result
            // sets are closed out from under us.
            tsIds = tsMembers.mapTo(DbKey.class).list();
            siteIds = siteMembers.mapTo(DbKey.class).list();
            dataTypeIds = dataTypeMembers.mapTo(DbKey.class).list();
            otherMembers = others.map((rs, ctx) -> new TsGroupMember(rs.getString("member_type"),
                                                                     rs.getString("member_value"))).list();
            childGroups = children.map((rs, ctx) -> new ChildGroup(DbKey.createDbKey(rs, "child_group_id"),
                                                                   combineOp(rs.getString("include_group")))).list();
        }

        for (var tsId : tsIds)
        {
            var found = timeSeriesDao.findBy(tx, tsId);
            if (found.isFailure())
            {
                log.atWarn()
                   .setCause(found.failure())
                   .log("Unable to read ts member with ts_id={} of tsdb_group id={} -- ignored.",
                        tsId, group.getGroupId());
                continue;
            }
            var tsMember = found.success().orElse(null);
            if (tsMember == null)
            {
                log.warn("tsdb_group id={} contains invalid ts member with ts_id={} -- ignored.",
                         group.getGroupId(), tsId);
            }
            else
            {
                group.addTsMember(tsMember);
            }
        }
        siteIds.forEach(group::addSiteId);
        dataTypeIds.forEach(group::addDataTypeId);
        otherMembers.forEach(member -> group.addOtherMember(member.getMemberType(), member.getMemberValue()));

        for (var child : childGroups)
        {
            if (lineOfAncestry.contains(child.id()))
            {
                log.warn("Group (id={}) {} -- has sub group ID={} -- circular reference detected -- ignored.",
                         group.getGroupId(), group.getGroupName(), child.id());
                continue;
            }

            var childGroup = loaded.get(child.id());
            if (childGroup == null)
            {
                var shallow = findGroupById(handle, dbEngine, child.id());
                if (shallow.isPresent())
                {
                    childGroup = readMembers(tx, shallow.get(), loaded, lineOfAncestry);
                }
            }

            if (childGroup == null)
            {
                log.warn("Group (id={}) {} -- has sub group ID={} that doesn't exist in database -- ignored.",
                         group.getGroupId(), group.getGroupName(), child.id());
            }
            else
            {
                group.addSubGroup(childGroup, child.combine());
            }
        }

        lineOfAncestry.pop();
        return group;
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

    private void writeChildGroups(Handle handle, DatabaseEngine dbEngine, TsGroup group, DbKey groupId)
            throws OpenDcsDataException
    {
        try (var delete = handle.createUpdate(DELETE_CHILD_GROUPS);
             var insert = handle.prepareBatch(INSERT_CHILD_GROUPS))
        {
            delete.bind("parent_group_id", groupId).execute();
            addChildren(handle, dbEngine, insert, group, group.getIncludedSubGroups(), ADD);
            addChildren(handle, dbEngine, insert, group, group.getExcludedSubGroups(), SUBTRACT);
            addChildren(handle, dbEngine, insert, group, group.getIntersectedGroups(), INTERSECT);
            insert.execute();
        }
    }

    private void addChildren(Handle handle, DatabaseEngine dbEngine, PreparedBatch insert, TsGroup parent,
                             Collection<TsGroup> children, char combine) throws OpenDcsDataException
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
                                        + "' of group '" + parent.getGroupName() + "' isn't in the database."))
                              .getGroupId();
            }

            insert.bind("parent_group_id", parent.getGroupId())
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

    /** One row of the tsdb_group_member_group table. */
    private record ChildGroup(DbKey id, char combine)
    {
    }
}
