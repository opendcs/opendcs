package org.opendcs.database.model.mappers.properties;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.utils.sql.GenericColumns;

import ilex.util.Pair;

public class PropertiesMapper extends PrefixRowMapper<Pair<String,String>>
{
    private PropertiesMapper(String prefix)
    {
        super(prefix);
    }


    public static PropertiesMapper withPrefix(String prefix)
    {
        return new PropertiesMapper(prefix);
    }

    @Override
    public Pair<String, String> map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        return Pair.of(rs.getString(prefix+GenericColumns.NAME),
                       rs.getString(prefix+"prop_value"));
    }

}
