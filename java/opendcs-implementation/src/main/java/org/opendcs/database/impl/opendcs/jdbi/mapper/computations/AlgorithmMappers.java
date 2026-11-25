package org.opendcs.database.impl.opendcs.jdbi.mapper.computations;

import org.opendcs.database.model.mappers.properties.PropertiesMapper;

public record AlgorithmMappers(AlgorithmMapper algoMapper, AlgorithmTsParameterMapper parmMapper,
                            AlgorithmScriptMapper scriptMapper, PropertiesMapper propertiesMapper)
{    
}
