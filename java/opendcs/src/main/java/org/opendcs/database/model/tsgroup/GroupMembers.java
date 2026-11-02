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
import java.util.Set;

import decodes.sql.DbKey;
import decodes.tsdb.TimeSeriesIdentifier;

/**
 * The explicitly named members of a group, resolved to identifiers.
 *
 * This is the one part of a group that costs real work to read, and most callers want
 * neither it nor its sites and data types, so it is a separate read from the
 * {@link GroupDefinition} that names them.
 */
public record GroupMembers(List<TimeSeriesIdentifier> timeSeries, List<DbKey> unresolved)
{
    public static final GroupMembers EMPTY = new GroupMembers(List.of(), List.of());

    public GroupMembers
    {
        timeSeries = timeSeries == null ? List.of()
                                        : List.copyOf(timeSeries.stream().filter(Objects::nonNull).toList());
        unresolved = unique(unresolved);
    }

    public int size()
    {
        return timeSeries.size();
    }

    /**
     * @return true when some member is recorded in tsdb_group_member_ts but has no row in
     *         the time series identifier view, and so could not be resolved.
     */
    public boolean hasUnresolved()
    {
        return !unresolved.isEmpty();
    }

    private static List<DbKey> unique(List<DbKey> keys)
    {
        if (keys == null)
        {
            return List.of();
        }
        Set<DbKey> unique = new LinkedHashSet<>();
        keys.stream().filter(Objects::nonNull).forEach(unique::add);
        return List.copyOf(unique);
    }
}
