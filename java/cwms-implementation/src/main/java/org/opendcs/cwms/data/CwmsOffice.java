
/*
 *  Copyright 2025 OpenDCS Consortium and its Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.cwms.data;

import java.util.Objects;

import org.opendcs.data.Organization;
import org.opendcs.database.DatabaseKey;

import decodes.sql.DbKey;

public record CwmsOffice(DbKey id, String name, DbKey reportsToId, CwmsOffice reportsTo, String longName, String eroc, String type) implements Organization
{

    public CwmsOffice(DbKey id, String name, CwmsOffice reportsTo, String longName, String eroc, String type)
    {
        this(id, name, reportsTo != null ? reportsTo.id : null, reportsTo, longName, eroc, type);
    }

    @Override
    public DatabaseKey getId()
    {
        return this.id;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public String getDisplayName()
    {
        return this.longName;
    }

    @Override
    public CwmsOffice getReportsToOffice()
    {
        return this != this.reportsTo ? this.reportsTo : null;
    }

    // For hashCode and equals we only care if the first level of reporting office matches.
    // thus we use the stored reportsToId field and ignore the reportsTo instance for
    // these elements.

    private boolean basicsEqual(CwmsOffice office)
    {

        return office != null &&
               this.eroc.equals(office.eroc) &&
               this.id.equals(office.id) &&
               this.type.equals(office.type) &&
               this.name.equals(office.name) &&
               this.longName.equals(office.longName) &&
               Objects.equals(reportsToId, office.reportsToId);
    }

    @Override
    public boolean equals(Object rhs)
    {
        if (this == rhs)
        {
            return true;
        }
        else if (rhs instanceof CwmsOffice office)
        {
            return basicsEqual(office);
        }
        else
        {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        // we'll always have the reportsToId value, if any, but may not have the actual instance
        // so only hash on the id.
        return Objects.hash(this.id, this.name, this.type, this.longName, this.reportsToId);
    }
}
