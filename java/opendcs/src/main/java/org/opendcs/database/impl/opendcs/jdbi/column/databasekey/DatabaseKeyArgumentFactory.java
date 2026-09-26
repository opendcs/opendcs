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
package org.opendcs.database.impl.opendcs.jdbi.column.databasekey;

import java.sql.Types;

import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;

import decodes.sql.DbKey;

/**
 * Map a DbKey instance to the appropriate column value.
 * The {@see decodes.sql.DbKey#NullKey} is set as null.
 * DatabaseKeyArgumentFactory
 */
public class DatabaseKeyArgumentFactory extends AbstractArgumentFactory<DbKey>
{

    public DatabaseKeyArgumentFactory()
    {
        super(Types.BIGINT);
    }

    @Override
    protected Argument build(DbKey value, ConfigRegistry config)
    {
        return (position, statement, ctx) ->
        {
            if (DbKey.isNull(value))
            {
                statement.setNull(position, Types.NUMERIC);
            }
            else
            {
                statement.setLong(position, value.getValue());
            }
        };
    }
}
