package org.opendcs.database.model.mappers.datasource;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.utils.sql.GenericColumns;

import decodes.db.DataSource;
import decodes.sql.DbKey;

public final class DataSourceMapper extends PrefixRowMapper<DataSource>
{

    private DataSourceMapper(String prefix)
    {
        super(prefix);
    }

    public static DataSourceMapper withPrefix(String prefix)
    {
        return new DataSourceMapper(prefix);
    }


    @Override
    public DataSource map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> dbKeyMapper = ctx.findColumnMapperFor(DbKey.class)
                                             .orElseThrow(() -> new SQLException("No mapper registered for DbKey class."));
        final DbKey id = dbKeyMapper.map(rs, prefix + GenericColumns.ID, ctx);
        final String name = rs.getString(prefix + GenericColumns.NAME);
        final String dataSourceType = rs.getString(prefix + "datasourcetype");
        final String dataSourceArg = rs.getString(prefix + "datasourcearg");

        var ds = new DataSource(name, dataSourceType);
        ds.forceSetId(id);
        ds.setDataSourceArg(dataSourceArg);
        return ds;
    }
    
}
