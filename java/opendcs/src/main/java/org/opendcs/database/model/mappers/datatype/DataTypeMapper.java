package org.opendcs.database.model.mappers.datatype;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.GenericColumns;

import decodes.db.DataType;
import decodes.db.DatabaseException;
import decodes.sql.DbKey;

public final class DataTypeMapper extends PrefixRowMapper<DataType,DataTypeMapper.Columns>
{
    private DataTypeMapper(String prefix)
    {
        super(prefix, Columns.class);
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
        final DbKey id = dbKeyMapper.map(rs, column(Columns.ID), ctx);
        if (rs.wasNull() || DbKey.isNull(id))
        {
            return null;
        }
        String standard = rs.getString(column(Columns.STANDARD));
        String code = rs.getString(column(Columns.CODE));
        String displayname = rs.getString(column(Columns.DISPLAY_NAME));
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

    public enum Columns implements TableColumnDefinition
    {
        ID(GenericColumns.ID),
        STANDARD("standard"),
        CODE("code"),
        DISPLAY_NAME("display_name")
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
            return this.column;
        }
    }
}
