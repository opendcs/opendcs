package org.opendcs.database.model.mappers.dbenum;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.GenericColumns;

import decodes.db.EnumValue;

public final class EnumValueMapper extends PrefixRowMapper<EnumValue,EnumValueMapper.Columns>
{

    private EnumValueMapper(String prefix)
    {
        super(prefix, Columns.class);
    }

    public static EnumValueMapper withPrefix(String prefix)
    {
        return new EnumValueMapper(prefix);
    }

    @Override
    public EnumValue map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        final String enumValue = rs.getString(column(Columns.ENUM_VALUE));
        final String description = rs.getString(column(Columns.DESCRIPTION));
        final String execClass = rs.getString(column(Columns.EXEC_CLASS));
        final String editClass = rs.getString(column(Columns.EDIT_CLASS));
        final Integer sortNumber = rs.getInt(column(Columns.SORT_NUMBER));

        var tmp = new EnumValue(null, enumValue, description, execClass, editClass);
        tmp.setSortNumber(sortNumber);
        return tmp;
    }


    public enum Columns implements TableColumnDefinition
    {
        ENUMID("enumid"),
        ENUM_VALUE("enumvalue"),
        DESCRIPTION(GenericColumns.DESCRIPTION),
        EXEC_CLASS("execclass"),
        EDIT_CLASS("editclass"),
        SORT_NUMBER("sortnumber")
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
