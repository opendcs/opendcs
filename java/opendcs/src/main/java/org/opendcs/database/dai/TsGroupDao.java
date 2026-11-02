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
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;

import decodes.sql.DbKey;
import decodes.tsdb.TsGroup;

/**
 * Interface for reading and writing Time Series Groups
 * (the TSDB_GROUP table and its various member tables).
 */
public interface TsGroupDao extends OpenDcsDao
{
    /**
     * Retrieve a group, and all of its members, by its surrogate key.
     *
     * Sub-groups are retrieved as well, recursively. Groups that are members of
     * themselves (directly or anywhere in their hierarchy) are ignored, as are
     * members that no longer exist in the database.
     *
     * @param tx
     * @param id
     * @return
     * @throws OpenDcsDataException
     */
    Optional<TsGroup> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * Retrieve a group, and all of its members, by its name.
     * Name comparison is case-insensitive.
     *
     * @param tx
     * @param name
     * @return
     * @throws OpenDcsDataException
     */
    Optional<TsGroup> getByName(DataTransaction tx, String name) throws OpenDcsDataException;

    /**
     * Retrieve all groups, optionally limited to a single group type.
     *
     * This is a shallow read of just the group's id, name, type and description,
     * suitable for populating a list. Use {@link #loadMembers} or
     * {@link #getById} to retrieve the members of a group.
     *
     * @param tx
     * @param groupType the type of groups to list, or null for all groups
     * @param limit
     * @param offset
     * @return
     * @throws OpenDcsDataException
     */
    List<TsGroup> getAll(DataTransaction tx, String groupType, int limit, int offset) throws OpenDcsDataException;

    default List<TsGroup> getAll(DataTransaction tx, int limit, int offset) throws OpenDcsDataException
    {
        return getAll(tx, null, limit, offset);
    }

    default List<TsGroup> getAll(DataTransaction tx, String groupType) throws OpenDcsDataException
    {
        return getAll(tx, groupType, -1, -1);
    }

    /**
     * Fill in the members of a group that was retrieved without them, e.g. one
     * from {@link #getAll}. Any previously loaded members of the group are discarded.
     *
     * @param tx
     * @param group the group to load the members of. It must have been retrieved from the database.
     * @return the same instance that was provided, now with its members filled in.
     * @throws OpenDcsDataException
     */
    TsGroup loadMembers(DataTransaction tx, TsGroup group) throws OpenDcsDataException;

    /**
     * Save or update a group and all of its members.
     *
     * Time series and sub-group members must already exist in the database;
     * members that are only known by name are looked up. Use their respective
     * DAOs to save them first if they don't exist yet.
     *
     * @param tx
     * @param group
     * @return the group as it now exists in the database.
     * @throws OpenDcsDataException
     */
    TsGroup save(DataTransaction tx, TsGroup group) throws OpenDcsDataException;

    /**
     * Delete a group. Any computations that used it are disabled and have their
     * reference to the group removed, and any group that used it as a sub-group
     * has that association removed. See {@link #removeDependenciesFor}.
     *
     * @param tx
     * @param id
     * @throws OpenDcsDataException
     */
    void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * @param tx
     * @param id
     * @return the number of computations that use the given group.
     * @throws OpenDcsDataException
     */
    int countCompsUsing(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * Remove anything that depends on the given group, namely computations that
     * use it directly, which are disabled in the process, and the sub-group
     * associations of any group that uses it.
     *
     * Notification records are written for each affected computation and group so
     * that the computation dependency updater can reconcile them.
     *
     * @param tx
     * @param deletedGroupId
     * @throws OpenDcsDataException
     */
    void removeDependenciesFor(DataTransaction tx, DbKey deletedGroupId) throws OpenDcsDataException;
}
