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
package org.opendcs.database.dai;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.model.tsgroup.GroupDefinition;
import org.opendcs.database.model.tsgroup.GroupMembers;
import org.opendcs.database.model.tsgroup.TsGroupRef;

import decodes.sql.DbKey;

/**
 * Reads and writes Time Series Groups (the TSDB_GROUP table and its member tables).
 *
 * The methods are split by how much of a group they read, because most of what OpenDCS
 * does with a group needs a small part of it: a picker or a sub-group slot needs a
 * {@link TsGroupRef}, an editor or an export needs the {@link GroupDefinition}, and only
 * something that has to know which series a group works out to needs
 * {@link GroupMembers} or the hierarchy.
 */
public interface TsGroupDao extends OpenDcsDao
{
    /**
     * Read one group's id, name, type and description, which costs one query and reads no
     * members.
     *
     * @param tx
     * @param id
     * @return
     * @throws OpenDcsDataException
     */
    Optional<TsGroupRef> ref(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * As {@link #ref}, found by name. The name is matched ignoring case.
     *
     * @param tx
     * @param name
     * @return
     * @throws OpenDcsDataException
     */
    Optional<TsGroupRef> refByName(DataTransaction tx, String name) throws OpenDcsDataException;

    /**
     * List groups, optionally of one type, without reading any of their members.
     *
     * @param tx
     * @param groupType the type of group to list, or null for all of them
     * @param limit -1 for no limit
     * @param offset -1 for no offset
     * @return
     * @throws OpenDcsDataException
     */
    List<TsGroupRef> refs(DataTransaction tx, String groupType, int limit, int offset) throws OpenDcsDataException;

    /**
     * Read a group's members by key, its criteria and the groups it pulls in.
     *
     * Sub-groups are returned as references, so this reads exactly one level and never
     * walks the hierarchy. Nothing here resolves a time series: see {@link #members}.
     *
     * @param tx
     * @param id
     * @return
     * @throws OpenDcsDataException
     */
    Optional<GroupDefinition> definition(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * As {@link #definition}, found by name. The name is matched ignoring case.
     *
     * @param tx
     * @param name
     * @return
     * @throws OpenDcsDataException
     */
    Optional<GroupDefinition> definitionByName(DataTransaction tx, String name) throws OpenDcsDataException;

    /**
     * Read the definitions of a list of groups, which costs one query per part of a group
     * no matter how many groups are asked for.
     *
     * @param tx
     * @param groupType the type of group to list, or null for all of them
     * @param limit -1 for no limit
     * @param offset -1 for no offset
     * @return
     * @throws OpenDcsDataException
     */
    List<GroupDefinition> definitions(DataTransaction tx, String groupType, int limit, int offset)
            throws OpenDcsDataException;

    /**
     * Resolve a group's explicitly named members, with their sites and data types. This is
     * the expensive part of a group, and the part that most callers don't want.
     *
     * Members that aren't in the database, or that the database can't describe as a time
     * series identifier, are reported by
     * {@link GroupMembers#hasUnresolved()} rather than dropped in silence.
     *
     * @param tx
     * @param id
     * @return an empty result for a group with no explicit members, or that isn't there.
     * @throws OpenDcsDataException
     */
    GroupMembers members(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * Which groups would be pulled in by using this one, including the group itself. The
     * hierarchy is walked by the database, and a group that is its own sub-group does not
     * loop.
     *
     * @param tx
     * @param id
     * @return an empty set for a group that isn't there.
     * @throws OpenDcsDataException
     */
    Set<DbKey> descendants(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * Which groups would be affected by changing this one, including the group itself.
     *
     * @param tx
     * @param id
     * @return an empty set for a group that isn't there.
     * @throws OpenDcsDataException
     */
    Set<DbKey> ancestors(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * Save or update a group and all of its members, replacing whatever was there.
     *
     * Sub-groups have to be in the database already; one that is only known by name is
     * looked up, and an unknown name is an error.
     *
     * @param tx
     * @param definition
     * @return the group as it now reads back from the database, key included.
     * @throws OpenDcsDataException
     */
    GroupDefinition save(DataTransaction tx, GroupDefinition definition) throws OpenDcsDataException;

    /**
     * Delete a group. Any computation that used it is disabled and no longer refers to it,
     * and any group that used it as a sub-group loses that link. See
     * {@link #removeDependenciesFor}.
     *
     * @param tx
     * @param id
     * @throws OpenDcsDataException
     */
    void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * @param tx
     * @param id
     * @return how many computations use this group.
     * @throws OpenDcsDataException
     */
    int countCompsUsing(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * Detach everything that depends on a group: computations that use it directly, which
     * are disabled in the process, and the sub-group links of any group that uses it.
     *
     * Notification records are written for each affected computation and group so that the
     * computation dependency updater can reconcile them.
     *
     * @param tx
     * @param deletedGroupId
     * @throws OpenDcsDataException
     */
    void removeDependenciesFor(DataTransaction tx, DbKey deletedGroupId) throws OpenDcsDataException;

    /**
     * Convenience for callers that don't care which type of group they list.
     *
     * @param tx
     * @return
     * @throws OpenDcsDataException
     */
    default List<TsGroupRef> refs(DataTransaction tx) throws OpenDcsDataException
    {
        return refs(tx, null, -1, -1);
    }

    /**
     * Read the definitions of a group and of every group it pulls in, however deep, which
     * is what working out a group's membership needs. The hierarchy is walked by the
     * database, so this costs one query per part of a group rather than one per group.
     *
     * @param tx
     * @param id the group at the top of the hierarchy
     * @return the definitions, by key, including the group asked for. Empty when the group
     *         isn't in the database.
     * @throws OpenDcsDataException
     */
    Map<DbKey, GroupDefinition> definitionsInHierarchy(DataTransaction tx, DbKey id) throws OpenDcsDataException;
}
