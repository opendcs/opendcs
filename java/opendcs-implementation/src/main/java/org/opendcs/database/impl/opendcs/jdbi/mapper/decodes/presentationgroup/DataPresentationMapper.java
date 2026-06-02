package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.presentationgroup;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.impl.opendcs.jdbi.column.numeric.NullableDouble;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.model.mappers.datatype.DataTypeMapper;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.db.DataPresentation;
import decodes.sql.DbKey;

public class DataPresentationMapper extends PrefixRowMapper<DataPresentation>
{
    private static final ColumnMapper<Double> DOUBLE_MAPPER = new NullableDouble();    

    private final DataTypeMapper dataTypeMapper;

    protected DataPresentationMapper(String prefix, String dataTypePrefix)
    {
        super(prefix);
        dataTypeMapper = DataTypeMapper.withPrefix(dataTypePrefix);
    }

    @Override
    public DataPresentation map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> columnMapperForKey = ctx.findColumnMapperFor(DbKey.class)
                                                    .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        var id = columnMapperForKey.map(rs, prefix + GenericColumns.ID, ctx);
        if (DbKey.isNull(id))
        {
            return null;
        }
        final var presentation = new DataPresentation();
        presentation.forceSetId(id);
        presentation.setUnitsAbbr(rs.getString(prefix + "unitabbr"));
        presentation.setMaxDecimals(rs.getInt(prefix + "maxdecimals"));

        presentation.setMaxValue(DOUBLE_MAPPER.map(rs, prefix + "max_value", ctx));
        presentation.setMinValue(DOUBLE_MAPPER.map(rs, prefix + "min_value", ctx));
        
        // equipmentid is on the table, but not in the DataPresentation Object

        presentation.setDataType(dataTypeMapper.map(rs, ctx));
        presentation.prepareForExec();
        presentation.setTimeLastRead();
        
        return presentation;
    }
    
    /**
     * Create an instance of DataPresentation mapper appropriate to the current query.
     * @param prefix prefix for the datapresentation values.
     * @param dataTypePrefix prefix for the associated data type
     * @return
     */
    public static DataPresentationMapper withPrefix(String prefix, String dataTypePrefix)
    {
        return new DataPresentationMapper(prefix, dataTypePrefix);
    }
}
