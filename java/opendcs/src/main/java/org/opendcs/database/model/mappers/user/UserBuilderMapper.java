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

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.UserBuilder;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.utils.sql.SqlErrorMessages;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import decodes.sql.DbKey;

public final class UserBuilderMapper extends PrefixRowMapper<UserBuilder>
{
    private final ObjectMapper om = new ObjectMapper();

    private UserBuilderMapper(String prefix)
    {
        super(prefix);
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

        builder.withId(columnMapperForKey.map(rs, prefix+"id", ctx))
               .withEmail(rs.getString(prefix+"email"))
               .withUpdatedAt(columnMapperForZDT.map(rs, prefix+"updated_at", ctx))
               .withCreatedAt(columnMapperForZDT.map(rs, prefix+"created_at", ctx))
                ;
        String jsonData = rs.getString(prefix+"preferences");
        Map<String, Object> preferences;
        try
        {
            preferences = om.readValue(jsonData, new TypeReference<Map<String, Object>>() {});
            return builder.withPreferences(preferences);
        }
        catch (JsonProcessingException ex)
        {
            throw new SQLException("Unable to parse provided json preferences", ex);
        }
    }
}