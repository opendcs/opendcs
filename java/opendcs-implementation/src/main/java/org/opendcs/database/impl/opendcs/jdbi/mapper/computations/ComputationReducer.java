package org.opendcs.database.impl.opendcs.jdbi.mapper.computations;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.opendcs.database.DatabaseKey;
import org.opendcs.database.model.mappers.properties.PropertiesMapper;

import decodes.sql.DbKey;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbComputation;

public class ComputationReducer implements LinkedHashMapRowReducer<DbKey, DbComputation>
 {
    private final ComputationMappers mappers;
    private final AlgorithmReducer algoReducer;
    private final Map<DatabaseKey, DbCompAlgorithm> algorithms = new LinkedHashMap<>();

    public ComputationReducer(ComputationMappers mappers)
    {
        this.mappers = mappers;
        this.algoReducer = new AlgorithmReducer(mappers.algoMappers());
        
    }

    @Override
    public void accumulate(Map<DbKey, DbComputation> container, RowView rowView)
    {
        try
        {
            var key = rowView.getColumn(mappers.comp().column(ComputationMapper.Columns.ID), DbKey.class);
            var comp = container.computeIfAbsent(key,
                                                 newKey -> rowView.getRow(DbComputation.class,
                                         
                                                    mappers.comp().getPrefix()));
            var algoId = rowView.getColumn(mappers.comp().column(ComputationMapper.Columns.ALGORITHM_ID),
                                           DbKey.class);
            if (mappers.algoMappers() != null)
            {
                algoReducer.accumulate(algorithms, rowView);
                var algo = algorithms.get(algoId);
                if (comp.getAlgorithm() != null)
                {
                    comp.setAlgorithm(algo);
                }
            }
            
            var compParm = rowView.getRow(DbCompParm.class, mappers.compParms().getPrefix());
            if (compParm != null && comp.getParm(compParm.getRoleName()) == null)
            {
                comp.addParm(compParm);
            }

            var prop = mappers.compProps() != null
                     ? rowView.getRow(PropertiesMapper.PAIR_STRING_STRING, mappers.compProps().getPrefix())
                     : null;
            if (prop != null && prop.first != null && prop.second != null)
            {
                comp.setProperty(prop.first, prop.second);
            }
            
        }
        catch (SQLException ex)
        {
            throw new UnableToExecuteStatementException("Unable to process result row.", ex, null);
        }
    }
    
}
