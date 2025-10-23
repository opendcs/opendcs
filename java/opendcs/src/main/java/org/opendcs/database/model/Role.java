/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
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
package org.opendcs.database.model;

import java.time.ZonedDateTime;

import decodes.sql.DbKey;

/**
 * Defined Access Role.
 */
public class Role
{
    public final DbKey id;
    public final String name;
    public final String description;
    public final ZonedDateTime updatedAt;


    public Role(DbKey id, String name, String description, ZonedDateTime updatedAt)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.updatedAt = updatedAt;
    }
}
