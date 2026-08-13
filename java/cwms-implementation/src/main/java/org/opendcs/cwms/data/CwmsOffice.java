
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

import org.opendcs.data.Organization;
import org.opendcs.database.DatabaseKey;

import decodes.sql.DbKey;

public record CwmsOffice(DbKey id, String name, CwmsOffice reportsTo, String longName, String eroc, String type) implements Organization
{

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
}
