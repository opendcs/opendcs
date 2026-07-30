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
package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.networklist;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.db.NetworkList;
import decodes.sql.DbKey;

@SuppressWarnings("java:S2143")
public final class NetworkListMapper extends PrefixRowMapper<NetworkList, NetworkListMapper.Columns>
{
    protected NetworkListMapper(String prefix)
    {
        super(prefix, "networklist", Columns.class);
    }
    
    @Override
    public NetworkList map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> columnMapperForKey = ctx.findColumnMapperFor(DbKey.class)
                                                    .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        ColumnMapper<Date> dateMapper = ctx.findColumnMapperFor(Date.class)
                                .orElseThrow(() -> new SQLException(SqlErrorMessages.TIME_MAPPER_NOT_FOUND));
        var list = new NetworkList();
        list.forceSetId(columnMapperForKey.map(rs, column(Columns.ID), ctx));
        list.name = rs.getString(column(Columns.NAME));
        list.transportMediumType = rs.getString(column(Columns.TRANSPORT_MEDIUM_TYPE));
        list.siteNameTypePref = rs.getString(column(Columns.SITENAME_TYPE_PREFERENCE));
        list.lastModifyTime = dateMapper.map(rs, column(Columns.LAST_MODIFY_TIME), ctx);

        return list;
    }

    public static NetworkListMapper withPrefix(String prefix)
    {
        return new NetworkListMapper(prefix);
    }
    
    public enum Columns implements TableColumnDefinition
    {
        ID(GenericColumns.ID),
        NAME(GenericColumns.NAME),
        TRANSPORT_MEDIUM_TYPE("transportmediumtype"),
        SITENAME_TYPE_PREFERENCE("sitenametypepreference"),
        LAST_MODIFY_TIME("lastmodifytime")
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
            return this.column;
        }
    }
}
