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

import decodes.db.Constants;
import decodes.sql.DbKey;

/**
 * Everything that is known about a group without reading any of its members, which is
 * all a sub-group slot, a group picker or a group list view ever asks for.
 */
public record TsGroupRef(DbKey id, String name, String type, String description)
{
    public TsGroupRef
    {
        Objects.requireNonNull(id, "Use Constants.undefinedId, or TsGroupRef.unsaved(), for a group "
                                   + "that isn't in the database yet.");
        name = name == null ? "" : name;
        type = type == null ? "" : type;
        description = description == null ? "" : description;
    }

    public static TsGroupRef unsaved(String name, String type)
    {
        return new TsGroupRef(Constants.undefinedId, name, type, "");
    }

    public boolean isSaved()
    {
        return !DbKey.isNull(id);
    }

    /**
     * Group names are unique in the database, and a group that hasn't been saved yet -
     * one read from an XML file, for instance - has nothing but a name, so the name is
     * what identifies a reference once both groups are in the database.
     */
    public boolean isSameGroup(TsGroupRef other)
    {
        if (other == null)
        {
            return false;
        }
        if (isSaved() && other.isSaved())
        {
            return id.equals(other.id);
        }
        return !name.isBlank() && name.equalsIgnoreCase(other.name);
    }
}
