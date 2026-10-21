package org.opendcs.database.impl.opendcs.jdbi.mapper.computations;

import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowViewMapper;
import org.jdbi.v3.core.result.RowView;

import decodes.tsdb.DbCompAlgorithm;

public class AlgorithmReducer implements RowViewMapper<DbCompAlgorithm>
{

    @Override
    public DbCompAlgorithm map(RowView rowView) throws SQLException
    {
        return null;
    }
    
}
