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
package org.opendcs.database.model.mappers.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Map;

import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.UserBuilder;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.SqlErrorMessages;
import org.opendcs.utils.sql.GenericColumns;

import decodes.sql.DbKey;

public final class UserBuilderMapper extends PrefixRowMapper<UserBuilder,UserBuilderMapper.Columns>
{
    /**
     * Used for queries referencing the user id as a foreign key
     */
    public static final String USER_ID = "user_id";

    private UserBuilderMapper(String prefix)
    {
        super(prefix, Columns.class);
    }


    public static UserBuilderMapper withPrefix(String prefix)
    {
        return new UserBuilderMapper(prefix);
    }

    @Override
    public UserBuilder map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        UserBuilder builder = new UserBuilder();
        ColumnMapper<ZonedDateTime> columnMapperForZDT
                = ctx.findColumnMapperFor(ZonedDateTime.class)
                     .orElseThrow(() -> new SQLException(SqlErrorMessages.ZDT_MAPPER_NOT_FOUND));
        ColumnMapper<DbKey> columnMapperForKey = ctx.findColumnMapperFor(DbKey.class)
                                                    .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));

        builder.withId(columnMapperForKey.map(rs, column(Columns.ID), ctx))
               .withEmail(rs.getString(column(Columns.EMAIL)))
               .withUpdatedAt(columnMapperForZDT.map(rs, column(Columns.UPDATED_AT), ctx))
               .withCreatedAt(columnMapperForZDT.map(rs, column(Columns.CREATED_AT), ctx))
                ;
        ColumnMapper<Map<String, Object>> columnMapperForConfig =
                ctx.findColumnMapperFor(new GenericType<Map<String, Object>>() {})
                   .orElseThrow(() -> new SQLException(SqlErrorMessages.CONFIG_MAPPER_NOT_FOUND));

        Map<String, Object> preferences = columnMapperForConfig.map(rs, column(Columns.PREFERENCES), ctx);
        return builder.withPreferences(preferences);
    }

    public enum Columns implements TableColumnDefinition
    {
        ID(GenericColumns.ID),
        EMAIL(GenericColumns.EMAIL),
        UPDATED_AT(GenericColumns.UPDATED_AT),
        CREATED_AT(GenericColumns.CREATED_AT),
        PREFERENCES(GenericColumns.PREFERENCES)
        ;

        private final String column;

        Columns(String column)
        {
            this.column = column;
        }

        Columns(GenericColumns other)
        {
            this.column = other.column();
        }

        @Override
        public String column()
        {
            return column;
        }
    }
}
