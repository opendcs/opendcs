package org.opendcs.database.model.mappers.datasource;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.result.RowView;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.GenericColumns;

import decodes.db.DataSource;
import decodes.sql.DbKey;

public final class DataSourceMapper extends PrefixRowMapper<DataSource,DataSourceMapper.Columns>
{
    private DataSourceMapper(String prefix)
    {
        super(prefix, "datasource", Columns.class);
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

    @Override
    public DataSource mapView(RowView view)
    {
        try
        {
            final var id = view.getColumn(column(Columns.ID), DbKey.class);
            if (id == null)
            {
                return null;
            }
            final String name = view.getColumn(column(Columns.NAME), String.class);
            final String dataSourceType = view.getColumn(column(Columns.SOURCE_TYPE), String.class);
            final String dataSourceArg = view.getColumn(column(Columns.SOURCE_ARGS), String.class);
            final var ds = new DataSource(name, dataSourceType);
            ds.forceSetId(id);
            ds.setDataSourceArg(dataSourceArg);
            return ds;
        }
        catch (SQLException ex)
        {
            throw new UnableToExecuteStatementException("unable to retrieve column value.", ex, null);
        }
    }

    public enum Columns implements TableColumnDefinition
    {
        ID(GenericColumns.ID),
        NAME(GenericColumns.NAME),
        SOURCE_TYPE("datasourcetype"),
        SOURCE_ARGS("datasourcearg"),
        SEQUENCE_NUMBER("sequencenum")
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
