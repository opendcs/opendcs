/*
* Where Applicable, Copyright 2026 OpenDCS Consortium and/or its contributors
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
package org.opendcs.database.impl.opendcs.jdbi.mapper.timeseries;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.tsgroup.TsGroupRef;
import org.opendcs.database.model.mappers.OpenDcsTableMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.sql.DbKey;

/**
 * Maps the TSDB_GROUP table itself. Members live in their own tables and are read by the
 * TsGroupDao implementation.
 */
public final class TsGroupRefMapper extends OpenDcsTableMapper<TsGroupRef,TsGroupRefMapper.Columns>
{
    private static final TsGroupRefMapper DEFAULT_INSTANCE = new TsGroupRefMapper("");

    private TsGroupRefMapper(String prefix)
    {
        super(prefix, "tsdb_group", Columns.class);
    }

    public static TsGroupRefMapper withNoPrefix()
    {
        return withPrefix(null);
    }

    public static TsGroupRefMapper withPrefix(String prefix)
    {
        if (prefix == null || prefix.isBlank())
        {
            return DEFAULT_INSTANCE;
        }
        return new TsGroupRefMapper(prefix);
    }

    @Override
    public TsGroupRef map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> dbKeyMapper = ctx.findColumnMapperFor(DbKey.class)
                                             .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        final DbKey id = dbKeyMapper.map(rs, column(Columns.GROUP_ID), ctx);
        if (rs.wasNull() || DbKey.isNull(id))
        {
            return null;
        }

        return new TsGroupRef(id,
                              rs.getString(column(Columns.GROUP_NAME)),
                              rs.getString(column(Columns.GROUP_TYPE)),
                              rs.getString(column(Columns.GROUP_DESCRIPTION)));
    }

    public enum Columns implements TableColumnDefinition
    {
        GROUP_ID("group_id"),
        GROUP_NAME("group_name"),
        GROUP_TYPE("group_type"),
        GROUP_DESCRIPTION("group_description")
        ;

        private final String column;

        Columns(String column)
        {
            this.column = column;
        }

        @Override
        public String column()
        {
            return this.column;
        }
    }
}
