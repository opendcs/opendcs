package org.opendcs.database.model.mappers.dbenum;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.utils.sql.GenericColumns;

import decodes.db.EnumValue;

public class EnumValueMapper extends PrefixRowMapper<EnumValue>
{

    private EnumValueMapper(String prefix)
    {
        super(prefix);
    }

    public static final EnumValueMapper withPrefix(String prefix)
    {
        return new EnumValueMapper(prefix);
    }

    @Override
    public EnumValue map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        final String enumValue = rs.getString(prefix + "enumvalue");
        final String description = rs.getString(prefix + GenericColumns.DESCRIPTION);
        final String execClass = rs.getString(prefix + "execclass");
        final String editClass = rs.getString(prefix + "editclass");
        final Integer sortNumber = rs.getInt(prefix + "sortnumber");

        var tmp = new EnumValue(null, enumValue, description, execClass, editClass);
        tmp.setSortNumber(sortNumber);
        return tmp;
    }

}
