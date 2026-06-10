package org.opendcs.database.model.mappers.sites;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;

import decodes.db.SiteName;

public class OpenDcsSiteNameMapper extends PrefixRowMapper<SiteName,OpenDcsSiteNameMapper.Columns>
{

    protected OpenDcsSiteNameMapper(String prefix)
    {
        super(prefix, Columns.class);
    }

    public static OpenDcsSiteNameMapper withPrefix(String prefix)
    {
        return new OpenDcsSiteNameMapper(prefix);
    }

    @Override
    public SiteName map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        var type = rs.getString(column(Columns.NAME_TYPE));
        if (type == null)
        {
            return null;
        }
        var name = rs.getString(column(Columns.SITE_NAME));
        var ret = new SiteName(null, type, name);
        ret.setAgencyCode(rs.getString(column(Columns.AGENCY_CODE)));
        ret.setUsgsDbno(rs.getString(column(Columns.USGS_DBNO)));
        return ret;
    }

    public enum Columns implements TableColumnDefinition
    {
        NAME_TYPE("nametype"),
        SITE_NAME("sitename"),
        AGENCY_CODE("agency_cd"),
        USGS_DBNO("dbnum")
        ;

        private final String column;

        Columns(String column)
        {
            this.column = column;
        }

        @Override
        public String column()
        {
            return this.column;
        }
    }
}
