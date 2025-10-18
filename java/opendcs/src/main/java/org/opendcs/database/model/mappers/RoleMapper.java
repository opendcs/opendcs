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
package org.opendcs.database.model.mappers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.Role;

import decodes.sql.DbKey;

public class RoleMapper extends PrefixRowMapper<Role>
{
    private RoleMapper(String prefix)
    {
        super(prefix);
    }

    @Override
    public Role map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        DbKey key = DbKey.createDbKey(rs, prefix+"id");
        String name = rs.getString(prefix+"name");
        String description = rs.getString(prefix+"description");
        ColumnMapper<ZonedDateTime> columnMapperForZDT = ctx.findColumnMapperFor(ZonedDateTime.class)
                                                            .get();
        ZonedDateTime updatedAt = columnMapperForZDT.map(rs, prefix+"updated_at", ctx);
        return new Role(key, name, description, updatedAt);
    }

    public static RoleMapper withPrefix(String prefix)
    {
        return new RoleMapper(prefix);
    }
}