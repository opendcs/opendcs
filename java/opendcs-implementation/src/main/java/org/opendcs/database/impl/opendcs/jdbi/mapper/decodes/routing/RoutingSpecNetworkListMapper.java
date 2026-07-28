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
package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.routing;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;

import ilex.util.Pair;

public final class RoutingSpecNetworkListMapper extends PrefixRowMapper<Pair<Long,String>, RoutingSpecNetworkListMapper.Columns>
{    
    public static final GenericType<Pair<Long,String>> ROUTING_SPEC_LIST = new GenericType<>()
    { /* marker interface */
    };
    private RoutingSpecNetworkListMapper(String prefix)
    {
        super(prefix, "routingspecnetworklist", Columns.class);
    }

    @Override
    public Pair<Long, String> map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        var specId = rs.getLong(column(Columns.ROUTING_SPEC_ID));
        if (rs.wasNull())
        {
            return null;
        }
        var listName = rs.getString(column(Columns.NETWORK_LIST_NAME));
        return Pair.of(specId, listName);
    }

    public static RoutingSpecNetworkListMapper withPrefix(String prefix)
    {
        return new RoutingSpecNetworkListMapper(prefix);
    }

    public enum Columns implements TableColumnDefinition
    {
        ROUTING_SPEC_ID("routingspecid"),
        NETWORK_LIST_NAME("networklistname")
        ;

        private final String column;

        Columns(String column)
        {
            this.column = column;
        }

        @Override
        public String column()
        {
            return column;
        }
        
    }

    
}
