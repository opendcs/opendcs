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
import java.util.Map;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.impl.opendcs.BuiltInIdentityProvider;
import org.opendcs.database.model.IdentityProvider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import decodes.sql.DbKey;

/**
 * Map identity provider columns
 */
public class IdentityProviderMapper extends PrefixRowMapper<IdentityProvider>
{
    private final ObjectMapper om = new ObjectMapper();

    private IdentityProviderMapper(String prefix)
    {
        super(prefix);
    }

    @Override
    public IdentityProvider map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        DbKey id = DbKey.createDbKey(rs, prefix+"id");
        String name = rs.getString(prefix+"name");
        String type = rs.getString(prefix+"type");
        ColumnMapper<ZonedDateTime> zdtMapper = ctx.findColumnMapperFor(ZonedDateTime.class).orElseThrow(() -> new SQLException("No mapper registered for ZonedDateTime."));
        ZonedDateTime updatedAt = zdtMapper.map(rs, prefix+"updated_at", ctx);
        try
        {
            String jsonData = rs.getString(prefix+"config");
            Map<String, Object> config = om.readValue(jsonData, new TypeReference<Map<String, Object>>() {});
            // TODO: other providers
            return new BuiltInIdentityProvider(id, name, updatedAt, config);
        }
        catch (JsonProcessingException | SQLException ex)
        {
            throw new SQLException("Unable to parse provided json", ex);
        }

    }

    public static IdentityProviderMapper withPrefix(String prefix)
    {
        return new IdentityProviderMapper(prefix);
    }
}
