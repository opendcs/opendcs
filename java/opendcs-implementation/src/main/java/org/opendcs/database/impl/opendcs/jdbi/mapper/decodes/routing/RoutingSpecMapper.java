package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.routing;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.db.RoutingSpec;
import decodes.sql.DbKey;

public final class RoutingSpecMapper extends PrefixRowMapper<RoutingSpec, RoutingSpecMapper.Columns>
{
    private RoutingSpecMapper(String prefix)
    {
        super(prefix, "routingspec", Columns.class);
    }

    
    @Override
    public RoutingSpec map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> columnMapperForKey = ctx.findColumnMapperFor(DbKey.class)
                                                    .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        ColumnMapper<Date> dateMapper = ctx.findColumnMapperFor(Date.class)
                                .orElseThrow(() -> new SQLException(SqlErrorMessages.TIME_MAPPER_NOT_FOUND));

        var ret = new RoutingSpec();
        ret.forceSetId(columnMapperForKey.map(rs, column(Columns.ID), ctx));
        ret.setName(rs.getString(column(Columns.NAME)));
        ret.enableEquations = rs.getBoolean(column(Columns.ENABLE_EQUATIONS));
        ret.usePerformanceMeasurements = rs.getBoolean(column(Columns.USE_PERFORMANCE_MEASUREMENTS));
        ret.outputFormat = rs.getString(column(Columns.OUTPUT_FORMAT));
        ret.outputTimeZoneAbbr = rs.getString(column(Columns.OUTPUT_TIME_ZONE));
        ret.presentationGroupName = rs.getString(column(Columns.PRESENTATION_GROUP_NAME));
        ret.sinceTime = rs.getString(column(Columns.SINCE_TIME));
        ret.untilTime = rs.getString(column(Columns.UNTIL_TIME));
        ret.consumerType = rs.getString(column(Columns.CONSUMER_TYPE));
        ret.consumerType = rs.getString(column(Columns.CONSUMER_ARGS));
        ret.lastModifyTime = dateMapper.map(rs, column(Columns.LAST_MODIFY_TIME), ctx);
        ret.isProduction = rs.getBoolean(column(Columns.LAST_MODIFY_TIME));

        return ret;
    }

    public static RoutingSpecMapper withPrefix(String prefix)
    {
        return new RoutingSpecMapper(prefix);
    }

    public enum Columns implements TableColumnDefinition
    {
        ID(GenericColumns.ID),
        NAME(GenericColumns.NAME),
        ENABLE_EQUATIONS("enableequations"),
        USE_PERFORMANCE_MEASUREMENTS("userperformancemeasurements"),
        OUTPUT_FORMAT("outputformat"),
        OUTPUT_TIME_ZONE("outputtimezone"),
        PRESENTATION_GROUP_NAME("presentationgroupname"),
        SINCE_TIME("sincetime"),
        UNTIL_TIME("untiltime"),
        CONSUMER_TYPE("consumertype"),
        CONSUMER_ARGS("consumerarg"),
        LAST_MODIFY_TIME("lastmodifytime"),
        IS_PRODUCTION("isproduction")
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
