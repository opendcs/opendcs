package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.routing;

import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.networklist.NetworkListEntryMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.networklist.NetworkListMapper;
import org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.networklist.NetworkListReducer;
import org.opendcs.database.model.mappers.properties.PropertiesMapper;

public record RoutingSpecMappers(RoutingSpecMapper specMapper, RoutingSpecNetworkListMapper specListMapper,
                                 PropertiesMapper specPropertiesMapper, NetworkListMapper listMapper,
                                 NetworkListEntryMapper listEntryMapper, NetworkListReducer listReducer)
{
    public RoutingSpecMappers(RoutingSpecMapper specMapper, RoutingSpecNetworkListMapper specListMapper,
                                 PropertiesMapper specPropertiesMapper, NetworkListMapper listMapper,
                                 NetworkListEntryMapper listEntryMapper)
    {
        this(specMapper, specListMapper, specPropertiesMapper,
             listMapper, listEntryMapper,
             listMapper != null ? new NetworkListReducer(listMapper, listEntryMapper) : null);
    }
}
