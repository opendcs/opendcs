package org.opendcs.database.model.mappers.engineeringunit;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;

import decodes.db.EngineeringUnit;

public final class EngineeringUnitMapper extends PrefixRowMapper<EngineeringUnit>
{

    private EngineeringUnitMapper(String prefix)
    {
        super(prefix);
    }

    public static EngineeringUnitMapper withPrefix(String prefix)
    {
        return new EngineeringUnitMapper(prefix);
    }

    @Override
    public EngineeringUnit map(ResultSet rs, StatementContext ctx) throws SQLException
    {   
        final String abbr = rs.getString(prefix + "unitabbr");
        final String name = rs.getString(prefix + "name");
        final String family = rs.getString(prefix + "family");
        final String measures = rs.getString(prefix + "measures");
        return new EngineeringUnit(abbr, name, family, measures);
    }
    
}
