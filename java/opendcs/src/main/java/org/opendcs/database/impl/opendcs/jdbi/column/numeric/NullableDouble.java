package org.opendcs.database.impl.opendcs.jdbi.column.numeric;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;

import decodes.db.Constants;

/**
 * Utility to handle cases were null is allowed for a double in the database
 * but a primitive type it used in a returned class.
 */
public class NullableDouble implements ColumnMapper<Double>
{
    public static final double DEFAULT_NULL_REPLACEMENT = Constants.undefinedDouble;

    private final double defaultValue;

    public NullableDouble()
    {
        this.defaultValue = DEFAULT_NULL_REPLACEMENT;
    }

    public NullableDouble(double defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    @Override
    public Double map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException
    {
        double tmp = rs.getDouble(columnNumber);
        if (rs.wasNull())
        {
            tmp = defaultValue;
        }
        return tmp;
    }
}
