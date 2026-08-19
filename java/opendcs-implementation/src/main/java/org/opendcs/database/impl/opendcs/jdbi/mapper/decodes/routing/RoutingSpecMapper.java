/*
* Where Applicable, Copyright 2026 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
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

@SuppressWarnings("java:S2143") // to be fixed at a later date
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
        ret.consumerArg = rs.getString(column(Columns.CONSUMER_ARGS));
        ret.lastModifyTime = dateMapper.map(rs, column(Columns.LAST_MODIFY_TIME), ctx);
        ret.isProduction = rs.getBoolean(column(Columns.IS_PRODUCTION));

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
        DATA_SOURCE_ID("datasourceid"),
        ENABLE_EQUATIONS("enableequations"),
        USE_PERFORMANCE_MEASUREMENTS("useperformancemeasurements"),
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
