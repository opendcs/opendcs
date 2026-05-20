package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.presentationgroup;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;

import decodes.db.RoundingRule;

public class RoundingRuleMapper extends PrefixRowMapper<RoundingRule>
{

    protected RoundingRuleMapper(String prefix)
    {
        super(prefix);
    }

    @Override
    public RoundingRule map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        var roundingRule = new RoundingRule();
        
        
        var upperLimit = rs.getDouble(prefix + "upperlimit");
        var signifigantDigits = rs.getInt(prefix + "sigdigits");

        roundingRule.setUpperLimit(upperLimit);
        roundingRule.sigDigits = signifigantDigits;

        return roundingRule;
    }
    

    public static RoundingRuleMapper withPrefix(String prefix)
    {
        return new RoundingRuleMapper(prefix);
    }
}
