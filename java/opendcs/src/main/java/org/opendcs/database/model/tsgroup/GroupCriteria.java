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
package org.opendcs.database.model.tsgroup;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import decodes.sql.DbKey;

/**
 * What makes a time series a member of a group, as the database stores it: the
 * tsdb_group_member_ts, _site, _dt and _other tables.
 *
 * Explicit time series members are held by key. Resolving those keys to
 * TimeSeriesIdentifier objects is a separate read, because for most groups most of the
 * time nobody needs them.
 */
public record GroupCriteria(List<DbKey> timeSeriesKeys, List<DbKey> siteIds, List<DbKey> dataTypeIds,
                            GroupAttributes attributes)
{
    public static final GroupCriteria EMPTY = new GroupCriteria(List.of(), List.of(), List.of(),
                                                                GroupAttributes.EMPTY);

    public GroupCriteria
    {
        timeSeriesKeys = unique(timeSeriesKeys);
        siteIds = unique(siteIds);
        dataTypeIds = unique(dataTypeIds);
        attributes = attributes == null ? GroupAttributes.EMPTY : attributes;
    }

    /**
     * Both engines insist that a time series match at least one thing before it is a
     * member, so a group with no sites, no data types and no attributes has no members at
     * all beyond the ones named outright.
     *
     * @return true when nothing but an explicitly named time series can belong to this group.
     */
    public boolean matchesNothing()
    {
        return siteIds.isEmpty() && dataTypeIds.isEmpty() && attributes.isEmpty();
    }

    /** @return true when this group has no members of any kind. */
    public boolean isEmpty()
    {
        return matchesNothing() && timeSeriesKeys.isEmpty();
    }

    /**
     * The member tables have no primary key that would stop the same member being written
     * twice, and no meaning for it either, so duplicates are dropped here, keeping the
     * order they came in. An unsaved key is kept, rather than dropped, so that saving the
     * group can say which member isn't in the database.
     */
    private static List<DbKey> unique(List<DbKey> keys)
    {
        if (keys == null)
        {
            return List.of();
        }
        var unique = new LinkedHashSet<DbKey>();
        keys.stream().filter(Objects::nonNull).forEach(unique::add);
        return List.copyOf(unique);
    }
}
