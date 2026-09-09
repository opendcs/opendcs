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

import decodes.sql.DbKey;

public final class CwmsOfficeBuilder
{
    private DbKey id;
    private String name;
    private String eroc;
    private String type;
    private String longName;
    private CwmsOfficeBuilder reportsTo;
    private DbKey reportsToId;


    public CwmsOfficeBuilder withId(DbKey id)
    {
        this.id = id;
        return this;
    }

    public CwmsOfficeBuilder withName(String name)
    {
        this.name = name;
        return this;
    }

    public CwmsOfficeBuilder withEroc(String eroc)
    {
        this.eroc = eroc;
        return this;
    }

    public CwmsOfficeBuilder withType(String type)
    {
        this.type = type;
        return this;
    }

    public CwmsOfficeBuilder withLongName(String longName)
    {
        this.longName = longName;
        return this;
    }

    public CwmsOfficeBuilder withReportsTo(CwmsOfficeBuilder reportsTo)
    {
        this.reportsTo = reportsTo;
        return this;
    }

    public CwmsOfficeBuilder withReportsToId(DbKey reportsToId)
    {
        this.reportsToId = reportsToId;
        return this;
    }

    public CwmsOffice build()
    {
        
        return new CwmsOffice(id, name, reportsToId,
                             (reportsTo != null && reportsTo != this) ? reportsTo.build() : null, longName, eroc, type);
    }
}
