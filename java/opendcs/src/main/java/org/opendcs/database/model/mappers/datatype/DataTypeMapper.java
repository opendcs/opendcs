package org.opendcs.database.model.mappers.datatype;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.utils.sql.GenericColumns;

import decodes.db.DataType;
import decodes.db.DatabaseException;
import decodes.sql.DbKey;

public final class DataTypeMapper extends PrefixRowMapper<DataType>
{

    private DataTypeMapper(String prefix)
    {
        super(prefix);
    }

    public static DataTypeMapper withPrefix(String prefix)
    {
        return new DataTypeMapper(prefix);
    }

    @Override
    public DataType map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> dbKeyMapper = ctx.findColumnMapperFor(DbKey.class)
                                             .orElseThrow(() -> new SQLException("No mapper registered for DbKey class."));
        final DbKey id = dbKeyMapper.map(rs, prefix + GenericColumns.ID, ctx);
        String standard = rs.getString(prefix + "standard");
        String code = rs.getString(prefix + "code");
        String displayname = rs.getString(prefix + "display_name");
        DataType dt = new DataType(standard, code);
        try
        {
            dt.setId(id);
        }
        catch (DatabaseException ex)
        {
            throw new SQLException("Unable to set id field on newly created DataType", ex);
        }
        dt.setDisplayName(displayname);
        return dt;
    }
    
}
