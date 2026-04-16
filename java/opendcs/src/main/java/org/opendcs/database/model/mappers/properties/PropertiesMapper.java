package org.opendcs.database.model.mappers.properties;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.utils.sql.GenericColumns;

import ilex.util.Pair;

public final class PropertiesMapper extends PrefixRowMapper<Pair<String,String>>
{
    private final String prop;

    public static final GenericType<Pair<String,String>> PAIR_STRING_STRING = new GenericType<>()
    {
        /* reference to allow JDBI to map Pair requests. */
    };

    private PropertiesMapper(String prefix, String prop)
    {
        super(prefix);
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
        return Pair.of(rs.getString(prefix+prop+GenericColumns.NAME),
                       rs.getString(prefix+"prop_value"));
    }

}
