package org.opendcs.database.model.mappers.datasource;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.GenericColumns;

import decodes.db.DataSource;
import decodes.sql.DbKey;

public final class DataSourceMapper extends PrefixRowMapper<DataSource,DataSourceMapper.Columns>
{
    private DataSourceMapper(String prefix)
    {
        super(prefix, Columns.class);
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
        final DbKey id = dbKeyMapper.map(rs, column(Columns.ID), ctx);
        final String name = rs.getString(column(Columns.NAME));
        final String dataSourceType = rs.getString(column(Columns.SOURCE_TYPE));
        final String dataSourceArg = rs.getString(column(Columns.SOURCE_ARGS));

        var ds = new DataSource(name, dataSourceType);
        ds.forceSetId(id);
        ds.setDataSourceArg(dataSourceArg);
        return ds;
    }

    public enum Columns implements TableColumnDefinition
    {
        ID(GenericColumns.ID),
        NAME(GenericColumns.NAME),
        SOURCE_TYPE("datasourcetype"),
        SOURCE_ARGS("datasourcearg")
        ;

        private final String column;

        Columns(String column)
        {
            this.column = column;
        }

        Columns(GenericColumns other)
        {
            this.column = other.column();
        }

        @Override
        public String column()
        {
            return column;
        }
    }
}
