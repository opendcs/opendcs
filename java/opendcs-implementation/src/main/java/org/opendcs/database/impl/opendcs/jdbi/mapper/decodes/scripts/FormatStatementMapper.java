package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.scripts;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;

import decodes.db.FormatStatement;

public class FormatStatementMapper extends PrefixRowMapper<FormatStatement, org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.scripts.FormatStatementMapper.Columns>
{

    protected FormatStatementMapper(String prefix)
    {
        super(prefix, Columns.class);
    }

    @Override
    public FormatStatement map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        final int num = rs.getInt(column(Columns.SEQUENCE_NUMBER));
        if (rs.wasNull())
        {
            return null;
        }
        final String label = rs.getString(column(Columns.LABEL));
        final String format = rs.getString(column(Columns.FORMAT));

        var fs = new FormatStatement(null, num);
        fs.label = label;
        fs.format = format;
        return fs;
    }

    public static FormatStatementMapper withPrefix(String prefix)
    {
        return new FormatStatementMapper(prefix);
    }

    public enum Columns implements TableColumnDefinition
    {
        SEQUENCE_NUMBER("sequencenum"),
        LABEL("label"),
        FORMAT("format")
        ;

        private final String column;

        Columns(String column)
        {
            this.column = column;
        }

        @Override
        public String column()
        {
            return column;
        }
        
    }
}
