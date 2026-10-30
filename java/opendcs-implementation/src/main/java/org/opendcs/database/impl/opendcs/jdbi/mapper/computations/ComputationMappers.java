package org.opendcs.database.impl.opendcs.jdbi.mapper.computations;

import org.opendcs.database.model.mappers.compapp.CompAppInfoMapper;
import org.opendcs.database.model.mappers.properties.PropertiesMapper;

/**
 * Convencies wrapper to hold all the required mappers for a computation.
 * ComputationMappers
 * @param comp Mapper of the computation
 * @param compParms Mapper for the computation parameters
 * @param compProps Mapper for the computation properties.
 * @param algoMappers Mapper set for the algorithm
 */
public record ComputationMappers(ComputationMapper comp, ComputationTsParameterMapper compParms,
                                 PropertiesMapper compProps, AlgorithmMappers algoMappers)
{
    
}
