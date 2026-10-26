package org.opendcs.database.impl.opendcs.jdbi.mapper.computations;

import java.sql.SQLException;
import java.util.Map;

import org.jdbi.v3.core.mapper.RowViewMapper;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowReducer;
import org.jdbi.v3.core.result.RowView;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.opendcs.data.computations.algorithms.AlgorithmScriptPart;
import org.opendcs.database.DatabaseKey;
import org.opendcs.database.model.mappers.properties.PropertiesMapper;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.sql.DbKey;
import decodes.tsdb.DbAlgoParm;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbCompAlgorithmScript;

public class AlgorithmReducer implements LinkedHashMapRowReducer<DatabaseKey, DbCompAlgorithm>
{

    private final AlgorithmMapper algoMapper;
    private final AlgorithmTsParameterMapper parmMapper;
    private final AlgorithmScriptMapper scriptMapper;
    private final PropertiesMapper propertiesMapper;

    public AlgorithmReducer(AlgorithmMappers mappers)
    {
        this.algoMapper = mappers.algoMapper();
        this.parmMapper = mappers.parmMapper();
        this.scriptMapper = mappers.scriptMapper();
        this.propertiesMapper = mappers.propertiesMapper();
    }

    @Override
    public void accumulate(Map<DatabaseKey, DbCompAlgorithm> container, RowView rowView)
    {
        try
        {
            var key = rowView.getColumn(algoMapper.column(AlgorithmMapper.Columns.ID), DbKey.class);

            var algo = container.computeIfAbsent(key, newKey -> rowView.getRow(DbCompAlgorithm.class, algoMapper.getPrefix()));

            var parm = rowView.getRow(DbAlgoParm.class, parmMapper.getPrefix());
            if (parm != null)
            {
                algo.addParm(parm);
            }

            var scriptPart = scriptMapper != null 
                           ? rowView.getRow(AlgorithmScriptPart.class, scriptMapper.getPrefix())
                           : null;

            if (scriptPart != null)
            {
                var script = algo.getScript(scriptPart.type());
                if (script == null)
                {
                    // NOTE: this assumes the rows were sorted by script_type, block_num ascending and are thus consistent.
                    algo.putScript(new DbCompAlgorithmScript(algo, scriptPart.type()));
                    script = algo.getScript(scriptPart.type());
                }
                script.addToText(scriptPart.scriptData());
            }

            var prop = propertiesMapper != null
                     ? rowView.getRow(PropertiesMapper.PAIR_STRING_STRING, propertiesMapper.getPrefix())
                     : null;
            if (prop != null && prop.first != null && prop.second != null)
            {
                algo.setProperty(prop.first, prop.second);
            }

        }
        catch (SQLException ex)
        {
            throw new UnableToExecuteStatementException("Unable to process result row.", ex, null);
        }
    }
    
}
