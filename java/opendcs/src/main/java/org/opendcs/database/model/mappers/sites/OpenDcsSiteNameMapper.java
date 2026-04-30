package org.opendcs.database.model.mappers.sites;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;

import decodes.db.SiteName;

public class OpenDcsSiteNameMapper extends PrefixRowMapper<SiteName>
{

    protected OpenDcsSiteNameMapper(String prefix)
    {
        super(prefix);
    }

    public static OpenDcsSiteNameMapper withPrefix(String prefix)
    {
        return new OpenDcsSiteNameMapper(prefix);
    }

    @Override
    public SiteName map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        var type = rs.getString(prefix + "nametype");
        var name = rs.getString(prefix + "sitename");
        if (type == null)
        {
            return null;
        }
        var ret = new SiteName(null, type, name);
        ret.setAgencyCode(rs.getString(prefix + "agency_cd"));
        ret.setUsgsDbno(rs.getString(prefix + "dbnum"));
        return ret;
    }
    
}
