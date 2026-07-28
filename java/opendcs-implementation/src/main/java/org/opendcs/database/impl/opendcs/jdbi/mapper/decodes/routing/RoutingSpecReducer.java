package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.routing;

import java.sql.SQLException;
import java.util.Map;

import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.opendcs.database.model.mappers.properties.PropertiesMapper;

import decodes.db.RoutingSpec;
import ilex.util.Pair;

public final class RoutingSpecReducer implements LinkedHashMapRowReducer<Long, RoutingSpec>
{
    public static final GenericType<Pair<String,String>> ROUTING_SPEC_PROPERTIES = new GenericType<>()
    { /* marker class */
    };

    private final RoutingSpecMapper routingSpecMapper;
    private final RoutingSpecNetworkListMapper networkListMapper;
    private final PropertiesMapper routingSpecPropertiesMapper;

    public RoutingSpecReducer(RoutingSpecMapper routingSpecMapper, RoutingSpecNetworkListMapper networkListMapper, 
                              PropertiesMapper routingSpecPropertiesMapper)
    {
        this.routingSpecMapper = routingSpecMapper;
        this.networkListMapper = networkListMapper;
        this.routingSpecPropertiesMapper = routingSpecPropertiesMapper;
    }


    @Override
    public void accumulate(Map<Long, RoutingSpec> container, RowView rowView)
    {
        try
        {
            var specId = rowView.getColumn(routingSpecMapper.column(RoutingSpecMapper.Columns.ID), Long.class);
            var spec = container.computeIfAbsent(specId, id -> rowView.getRow(RoutingSpec.class));

            var list = rowView.getRow(RoutingSpecNetworkListMapper.ROUTING_SPEC_LIST);
            if (list != null)
            {
                spec.addNetworkListName(list.second);
            }

            var prop = rowView.getRow(ROUTING_SPEC_PROPERTIES);
            if (prop != null && prop.first != null && prop.second != null)
            {
                spec.setProperty(prop.first, prop.second);
            }
        }
        catch (SQLException ex)
        {
            throw new UnableToExecuteStatementException("Unable to process row", ex, null);
        }
    }   
}
