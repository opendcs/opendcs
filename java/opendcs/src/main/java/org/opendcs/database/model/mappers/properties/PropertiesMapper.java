package org.opendcs.database.model.mappers.properties;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.GenericColumns;

import ilex.util.Pair;

public final class PropertiesMapper extends PrefixRowMapper<Pair<String,String>,PropertiesMapper.Columns>
{
    private final String prop;

    public static final GenericType<Pair<String,String>> PAIR_STRING_STRING = new GenericType<>()
    {
        /* reference to allow JDBI to map Pair requests. */
    };

    private PropertiesMapper(String prefix, String prop)
    {
        super(prefix, Columns.class);
        this.prop = addUnderscoreIfMissing(prop);
    }


    public static PropertiesMapper withPrefix(String prefix)
    {
        return new PropertiesMapper(prefix, "");
    }

    /**
     * Only equipmentproperty doesn't use the column name "prop_name" and is just "name"
     * @param prefix
     * @param prefixPropNameColumn
     * @return
     */
    public static PropertiesMapper withPrefix(String prefix, boolean prefixPropNameColumn)
    {
        return new PropertiesMapper(prefix, prefixPropNameColumn ? "prop" : "");
    }

    @Override
    public Pair<String, String> map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        return Pair.of(rs.getString(column(Columns.NAME)),
                       rs.getString(column(Columns.VALUE)));
    }

    /**
     * Override default behavior to insert prop_ or not.
     */
    @Override
    protected String column(Columns column) throws SQLException
    {
        if (Columns.NAME == column)
        {
            return prefix + prop + column.column();
        }
        else
        {
            return super.column(column);
        }
    }

    public enum Columns implements TableColumnDefinition
    {
        NAME(GenericColumns.NAME),
        VALUE("prop_value")
        ;

        private final String column;

        Columns(GenericColumns other)
        {
            this.column = other.column();
        }

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
