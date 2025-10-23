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
import java.util.function.BiConsumer;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.result.RowView;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.IdentityProviderMapping;
import org.opendcs.database.model.Role;
import org.opendcs.database.model.User;
import org.opendcs.database.model.User.Builder;
import org.opendcs.database.model.mappers.PrefixRowMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import decodes.sql.DbKey;

public class UserBuilderMapper extends PrefixRowMapper<User.Builder>
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
    public User.Builder map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        User.Builder builder = new User.Builder();
        ColumnMapper<ZonedDateTime> columnMapperForZDT
                = ctx.findColumnMapperFor(ZonedDateTime.class)
                     .get();
        ColumnMapper<DbKey> columnMapperForKey = ctx.findColumnMapperFor(DbKey.class).get();

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

    public static class UserBuilderReducer implements BiConsumer<Map<Long, User.Builder>, RowView>
    {
        public static final UserBuilderReducer USER_BUILDER_REDUCER = new UserBuilderReducer();

        @Override
        public void accept(Map<Long, Builder> map, RowView rowView)
        {

            User.Builder ub = map.computeIfAbsent(rowView.getColumn("u_id", Long.class),
                    qid -> rowView.getRow(User.Builder.class)
            );
            Long roleId = rowView.getColumn("r_id", Long.class);
            if (roleId != null)
            {
                Role r = rowView.getRow(Role.class);
                ub.withRole(r);
            }
            String subject = rowView.getColumn("i_subject", String.class);
            if (subject != null)
            {
                IdentityProviderMapping idpM = rowView.getRow(IdentityProviderMapping.class);
                ub.withIdentityMapping(idpM);
            }

        }

    }
}