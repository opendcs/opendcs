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

import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.IdentityProvider;
import org.opendcs.spi.authentication.IdentityProviderProvider;
import org.opendcs.spi.authentication.UnsupportedProviderException;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.sql.DbKey;

/**
 * Map identity provider columns
 */
public final class IdentityProviderMapper extends PrefixRowMapper<IdentityProvider>
{
    public static final String IDENTITY_PROVIDER_ID = "identity_provider_id";
    public static final String TYPE = "type";

    private IdentityProviderMapper(String prefix)
    {
        super(prefix);
    }

    @Override
    public IdentityProvider map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> columnMapperForKey = ctx.findColumnMapperFor(DbKey.class)
                                                    .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        DbKey id = columnMapperForKey.map(rs, prefix+GenericColumns.ID, ctx);
        String name = rs.getString(prefix+GenericColumns.NAME);
        String type = rs.getString(prefix+TYPE);
        ColumnMapper<ZonedDateTime> zdtMapper = ctx.findColumnMapperFor(ZonedDateTime.class)
                                                   .orElseThrow(() -> new SQLException(SqlErrorMessages.ZDT_MAPPER_NOT_FOUND));
        ZonedDateTime updatedAt = zdtMapper.map(rs, prefix+GenericColumns.UPDATED_AT, ctx);
        ColumnMapper<Map<String, Object>> columnMapperForConfig =
                ctx.findColumnMapperFor(new GenericType<Map<String, Object>>() {})
                   .orElseThrow(() -> new SQLException(SqlErrorMessages.CONFIG_MAPPER_NOT_FOUND));

        Map<String, Object> config = columnMapperForConfig.map(rs, prefix+GenericColumns.CONFIG, ctx);
        IdentityProviderProvider idpp;
        try
        {
            idpp = IdentityProviderProvider.createFor(type);
        }
        catch (UnsupportedProviderException ex)
        {
            throw new SQLException("Provider config stored in the database is not supported by this version of OpenDCS", ex);
        }
        return idpp.create(id, name, updatedAt, config);
    }

    public static IdentityProviderMapper withPrefix(String prefix)
    {
        return new IdentityProviderMapper(prefix);
    }
}
