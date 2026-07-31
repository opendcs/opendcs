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

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.networklist.NetworkListMapper;
import org.opendcs.database.model.mappers.datasource.DataSourceMapper;

import decodes.db.DataSource;
import decodes.db.NetworkList;
import decodes.db.RoutingSpec;
import decodes.sql.DbKey;
import ilex.util.Pair;

public final class RoutingSpecReducer implements LinkedHashMapRowReducer<Long, RoutingSpec>
{
    public static final GenericType<Pair<String,String>> ROUTING_SPEC_PROPERTIES = new GenericType<>()
    { /* marker class */
    };

    private final RoutingSpecMappers mappers;
    private final LinkedHashMap<Long, NetworkList> networkLists = new LinkedHashMap<>();
    private final LinkedHashMap<DbKey, DataSource> datasSources = new LinkedHashMap<>();

    public RoutingSpecReducer(RoutingSpecMappers mappers)
    {
        this.mappers = mappers;
    }


    @Override
    public void accumulate(Map<Long, RoutingSpec> container, RowView rowView)
    {
        try
        {
            var specId = rowView.getColumn(mappers.specMapper().column(RoutingSpecMapper.Columns.ID), Long.class);
            var spec = container.computeIfAbsent(specId, id -> rowView.getRow(RoutingSpec.class));

            if (mappers.specPropertiesMapper() != null)
            {
                var prop = rowView.getRow(ROUTING_SPEC_PROPERTIES);
                if (prop != null && prop.first != null && prop.second != null)
                {
                    spec.setProperty(prop.first, prop.second);
                }
            }

            if (mappers.dataSourceAccumulator() != null)
            {
                mappers.dataSourceAccumulator().accumulate(datasSources, rowView);
                var dsId = rowView.getColumn(
                    mappers.dataSourceAccumulator()
                           .primaryMapper
                           .column(DataSourceMapper.Columns.ID), DbKey.class);
                if (spec.dataSource == null && dsId != null)
                {
                    spec.dataSource = datasSources.get(dsId);
                }
            }

            if (mappers.listReducer() != null)
            {
                mappers.listReducer().accumulate(networkLists, rowView);
                var specList = rowView.getRow(RoutingSpecNetworkListMapper.ROUTING_SPEC_LIST);

                /**
                 * Special case NetworkList that "always exists" but is not present in the
                 * tables... which explain why there's no normalization and foreign keys on
                 * those tables. Future work should have a "NetworkList" in the database
                 * for <all>, and either have no entries, or automatically add entries
                 * to the list. With the latter prefered as it would reduce special cases
                 * in the downstream components.
                 */
                if (specList != null &&
                    "<all>".equalsIgnoreCase(specList.second) &&
                    !spec.networkLists.contains(NetworkList.dummy_all))
                {
                    spec.networkListNames.add("<all>");
                    spec.networkLists.add(NetworkList.dummy_all);
                }
                var listId = rowView.getColumn(mappers.listMapper().column(NetworkListMapper.Columns.ID), Long.class);
                if (listId != null)
                {
                    var list = this.networkLists.get(listId);
                    if (list != null && !spec.networkLists.contains(list))
                    {
                        spec.networkLists.add(list);
                        spec.networkListNames.add(list.name);
                    }
                }
            }
        }
        catch (SQLException ex)
        {
            throw new UnableToExecuteStatementException("Unable to process row", ex, null);
        }
    }
}
