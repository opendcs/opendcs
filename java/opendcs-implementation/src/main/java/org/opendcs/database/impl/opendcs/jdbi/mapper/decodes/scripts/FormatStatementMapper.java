package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.scripts;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;

import decodes.db.FormatStatement;

public class FormatStatementMapper extends PrefixRowMapper<FormatStatement>
{

    protected FormatStatementMapper(String prefix)
    {
        super(prefix);
    }

    @Override
    public FormatStatement map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        final int num = rs.getInt(prefix + "sequencenum");
        if (rs.wasNull())
        {
            return null;
        }
        final String label = rs.getString(prefix + "label");
        final String format = rs.getString(prefix + "format");

        var fs = new FormatStatement(null, num);
        fs.label = label;
        fs.format = format;
        return fs;
    }

    public static FormatStatementMapper withPrefix(String prefix)
    {
        return new FormatStatementMapper(prefix);
    }
}
