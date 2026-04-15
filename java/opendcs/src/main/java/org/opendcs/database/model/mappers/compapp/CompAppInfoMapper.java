package org.opendcs.database.model.mappers.compapp;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;

import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;

public final class CompAppInfoMapper extends PrefixRowMapper<CompAppInfo>
{
    private CompAppInfoMapper(String prefix)
    {
        super(prefix);
    }

    @Override
    public CompAppInfo map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> dbKeyMapper = ctx.findColumnMapperFor(DbKey.class)
                                             .orElseThrow(() -> new SQLException("No mapper registered for DbKey class."));
        final DbKey id = dbKeyMapper.map(rs, prefix + "loading_application_id", ctx);
        final String name = rs.getString(prefix + "loading_application_name");
        final Boolean manualEditApp = rs.getBoolean(prefix + "manual_edit_app");
        final String comment = rs.getString(prefix + "CMMNT");

        CompAppInfo appInfo = new CompAppInfo(id);
        appInfo.setAppName(name);
        appInfo.setManualEditApp(manualEditApp);
        appInfo.setComment(comment);

        return appInfo;
    }

    public static CompAppInfoMapper withPrefix(String prefix)
    {
        return new CompAppInfoMapper(prefix);
    }
    
}
