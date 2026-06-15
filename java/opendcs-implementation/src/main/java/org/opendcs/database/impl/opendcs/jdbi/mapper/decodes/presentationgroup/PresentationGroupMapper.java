package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.presentationgroup;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.db.PresentationGroup;
import decodes.sql.DbKey;

@SuppressWarnings("java:S2143") // Not the time to change this.
public class PresentationGroupMapper extends PrefixRowMapper<PresentationGroup, PresentationGroupMapper.Columns>
{
    protected PresentationGroupMapper(String prefix)
    {
        super(prefix, Columns.class);
    }

    @Override
    public PresentationGroup map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> columnMapperForKey = ctx.findColumnMapperFor(DbKey.class)
                                                    .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        ColumnMapper<Date> columnMapperForDate = ctx.findColumnMapperFor(Date.class)
                                                    .orElseThrow(() -> new SQLException(SqlErrorMessages.TIME_MAPPER_NOT_FOUND));
        final var pg = new PresentationGroup();
        pg.forceSetId(columnMapperForKey.map(rs, column(Columns.ID), ctx));
        pg.groupName = rs.getString(column(Columns.NAME));
        pg.lastModifyTime = columnMapperForDate.map(rs, column(Columns.LAST_MODIFY_TIME), ctx);
        pg.isProduction = rs.getBoolean(column(Columns.IS_PRODUCTION));
        pg.inheritsFrom = rs.getString(column(Columns.INHERITS_FROM));

        return pg;
    }

    public static PresentationGroupMapper withPrefix(String prefix)
    {
        return new PresentationGroupMapper(prefix);
    }

    public enum Columns implements TableColumnDefinition
    {
        ID(GenericColumns.ID),
        NAME(GenericColumns.NAME),
        LAST_MODIFY_TIME("lastmodifytime"),
        IS_PRODUCTION("isproduction"),
        INHERITS_FROM("inheritsfrom")
        ;

        private final String column;

        Columns(String column)
        {
            this.column = column;
        }

        Columns(GenericColumns other)
        {
            this.column = other.column();
        }

        @Override
        public String column()
        {
            return column;
        }
    }
}
