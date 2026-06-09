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
import java.util.EnumSet;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.Role;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.sql.DbKey;

public final class RoleMapper extends PrefixRowMapper<Role,org.opendcs.database.model.mappers.RoleMapper.Columns>
{
    public static final String ROLE_ID = "role_id";

    private RoleMapper(String prefix)
    {
        super(prefix, EnumSet.allOf(Columns.class));
    }

    @Override
    public Role map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> columnMapperForKey = ctx.findColumnMapperFor(DbKey.class)
                                                    .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        DbKey key = columnMapperForKey.map(rs, column(Columns.ID), ctx);
        String name = rs.getString(column(Columns.NAME));
        String description = rs.getString(column(Columns.DESCRIPTION));
        ColumnMapper<ZonedDateTime> columnMapperForZDT = ctx.findColumnMapperFor(ZonedDateTime.class)
                                                            .orElseThrow(() -> new SQLException(SqlErrorMessages.ZDT_MAPPER_NOT_FOUND));
        ZonedDateTime updatedAt = columnMapperForZDT.map(rs, column(Columns.UPDATED_AT), ctx);
        return new Role(key, name, description, updatedAt);
    }

    public static RoleMapper withPrefix(String prefix)
    {
        return new RoleMapper(prefix);
    }

    public static enum Columns implements TableColumnDefinition {
        ID(GenericColumns.ID),
        NAME(GenericColumns.NAME),
        DESCRIPTION(GenericColumns.DESCRIPTION),
        UPDATED_AT(GenericColumns.UPDATED_AT)
        ;

        private final String column;

        private Columns(String column) {
            this.column = column;
        }

        private Columns(GenericColumns other) {
            this.column = other.column();
        }

        @Override
        public String column() {
            return column;
        }
    }
}
