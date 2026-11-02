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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import decodes.tsdb.TsGroupMemberType;

/**
 * The tsdb_group_member_other rows, grouped by the part of the time series identifier
 * they constrain. A group matches a time series when the series' part is one of the
 * values recorded for it.
 *
 * Parts are held by name rather than by {@link TsGroupMemberType} because the column is
 * an open domain: HDB stores "ObjectType" in it, and any part of the database's own time
 * series identifier format may appear.
 */
public record GroupAttributes(Map<String, Set<String>> byPart)
{
    public static final GroupAttributes EMPTY = new GroupAttributes(Map.of());

    public GroupAttributes
    {
        Map<String, Set<String>> copy = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (byPart != null)
        {
            byPart.forEach((part, values) ->
            {
                Set<String> cleaned = immutableValues(values);
                if (part != null && !part.isBlank() && !cleaned.isEmpty())
                {
                    copy.put(part, cleaned);
                }
            });
        }
        byPart = Collections.unmodifiableMap(copy);
    }

    public static GroupAttributes of(String part, String... values)
    {
        return values == null ? EMPTY : of(part, Arrays.asList(values));
    }

    public static GroupAttributes of(String part, Collection<String> values)
    {
        Set<String> cleaned = immutableValues(values);
        return part == null || part.isBlank() || cleaned.isEmpty()
                ? EMPTY
                : new GroupAttributes(Map.of(part, cleaned));
    }

    /** @return the values recorded for a part, case-insensitively. Never null, may be empty. */
    public Set<String> values(String part)
    {
        return part == null ? Set.of() : byPart.getOrDefault(part, Set.of());
    }

    public Set<String> values(TsGroupMemberType part)
    {
        return values(part.toString());
    }

    /** @return the parts this group constrains, e.g. "Interval", "Version". */
    public Set<String> parts()
    {
        return byPart.keySet();
    }

    public boolean isEmpty()
    {
        return byPart.isEmpty();
    }

    /** @return true when {@code value} is one of the values recorded for {@code part}. */
    public boolean matches(String part, String value)
    {
        return value != null && values(part).contains(value);
    }

    private static Set<String> immutableValues(Collection<String> values)
    {
        if (values == null)
        {
            return Set.of();
        }
        Set<String> ret = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        values.stream().filter(v -> v != null && !v.isBlank()).forEach(ret::add);
        return ret.isEmpty() ? Set.of() : Collections.unmodifiableSet(ret);
    }
}
