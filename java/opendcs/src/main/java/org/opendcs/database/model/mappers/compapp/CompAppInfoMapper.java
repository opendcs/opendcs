package org.opendcs.database.model.mappers.compapp;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;

import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;

public final class CompAppInfoMapper extends PrefixRowMapper<CompAppInfo, org.opendcs.database.model.mappers.compapp.CompAppInfoMapper.Columns>
{
    private CompAppInfoMapper(String prefix)
    {
        super(prefix, Columns.class);
    }

    @Override
    public CompAppInfo map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> dbKeyMapper = ctx.findColumnMapperFor(DbKey.class)
                                             .orElseThrow(() -> new SQLException("No mapper registered for DbKey class."));
        final DbKey id = dbKeyMapper.map(rs, column(Columns.APP_ID), ctx);
        final String name = rs.getString(column(Columns.APP_NAME));
        final Boolean manualEditApp = rs.getBoolean(column(Columns.MANUAL_EDIT_APP));
        final String comment = rs.getString(column(Columns.COMMENT));

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
    

    public static enum Columns implements TableColumnDefinition
    {
        APP_ID("loading_application_id"),
        APP_NAME("loading_application_name"),
        MANUAL_EDIT_APP("manual_edit_app"),
        COMMENT("cmmt")
        ;

        private final String column;

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
