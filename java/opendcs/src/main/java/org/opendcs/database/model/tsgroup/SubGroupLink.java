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

import java.util.Objects;

import decodes.sql.DbKey;

/**
 * One row of tsdb_group_member_group: a parent group pulling a child group in with a
 * given operator. The child is held as a reference, so a definition never contains the
 * subtree it points at, and a group that contains itself cannot be represented.
 */
public record SubGroupLink(TsGroupRef group, GroupCombine combine)
{
    public SubGroupLink
    {
        Objects.requireNonNull(group, "A sub-group link has to say which group it links to.");
        Objects.requireNonNull(combine, "A sub-group link has to say how the group is combined.");
    }

    public DbKey id()
    {
        return group.id();
    }
}
