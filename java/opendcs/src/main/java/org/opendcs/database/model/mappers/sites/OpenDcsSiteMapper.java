package org.opendcs.database.model.mappers.sites;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.db.Site;
import decodes.sql.DbKey;

public class OpenDcsSiteMapper extends PrefixRowMapper<Site>
{


    
    protected OpenDcsSiteMapper(String prefix)
    {
        super(prefix);
    }

    public static OpenDcsSiteMapper withPrefix(String prefix)
    {
        return new OpenDcsSiteMapper(prefix);
    }

    @Override
    public Site map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> columnMapperForKey = ctx.findColumnMapperFor(DbKey.class)
                                                    .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        var ret = new Site();

        var id = columnMapperForKey.map(rs, prefix + "id", ctx);
        ret.forceSetId(id);
        
        ret.setPublicName(rs.getString(prefix + "public_name"));        
        ret.setDescription(rs.getString(prefix + "description"));

        return ret;
    }
    
}
