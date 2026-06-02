package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.presentationgroup;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.db.PresentationGroup;
import decodes.sql.DbKey;

public class PresentationGroupMapper extends PrefixRowMapper<PresentationGroup>
{

    protected PresentationGroupMapper(String prefix)
    {
        super(prefix);    
    }

    @Override
    public PresentationGroup map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> columnMapperForKey = ctx.findColumnMapperFor(DbKey.class)
                                                    .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        ColumnMapper<Date> columnMapperForDate = ctx.findColumnMapperFor(Date.class)
                                                    .orElseThrow(() -> new SQLException(SqlErrorMessages.TIME_MAPPER_NOT_FOUND));
        final var pg = new PresentationGroup();
        pg.forceSetId(columnMapperForKey.map(rs, prefix + GenericColumns.ID, ctx));
        pg.groupName = rs.getString(prefix + GenericColumns.NAME);
        pg.lastModifyTime = columnMapperForDate.map(rs, prefix + "lastmodifytime", ctx);
        pg.isProduction = rs.getBoolean(prefix + "isproduction");
        pg.inheritsFrom = rs.getString(prefix+"inheritsfrom");

        return pg;
    }
    

    public static PresentationGroupMapper withPrefix(String prefix)
    {
        return new PresentationGroupMapper(prefix);
    }
}
