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
package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.schedule;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.db.ScheduleEntry;
import decodes.sql.DbKey;

public final class ScheduleEntryMapper extends PrefixRowMapper<ScheduleEntry, ScheduleEntryMapper.Columns>
{
    private ScheduleEntryMapper(String prefix)
    {
        super(prefix, "schedule_entry", Columns.class);
    }

    
    @Override
    public ScheduleEntry map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> keyMapper = ctx.findColumnMapperFor(DbKey.class)
                                                    .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        ColumnMapper<Date> dateMapper = ctx.findColumnMapperFor(Date.class)
                                .orElseThrow(() -> new SQLException(SqlErrorMessages.TIME_MAPPER_NOT_FOUND));
        var id = keyMapper.map(rs, column(Columns.ID), ctx);
        if (id == null || rs.wasNull())
        {
            return null;
        }
        var ret = new ScheduleEntry(id);
        ret.setName(rs.getString(column(Columns.NAME)));
        ret.setLoadingAppId(keyMapper.map(rs, column(Columns.LOADING_APPLICATION_ID), ctx));
        ret.setLoadingAppName(rs.getString(column(Columns.LOADING_APPLICATION_NAME)));
        ret.setRoutingSpecId(keyMapper.map(rs, column(Columns.ROUTINGSPEC_ID), ctx));
        ret.setRoutingSpecName(rs.getString(column(Columns.ROUTINSPEC_NAME)));
        ret.setTimezone(rs.getString(column(Columns.TIME_ZONE)));
        ret.setStartTime(dateMapper.map(rs, column(Columns.START_TIME), ctx));
        ret.setRunInterval(rs.getString(column(Columns.RUN_INTERVAL)));
        ret.setEnabled(rs.getBoolean(column(Columns.ENABLED)));
        ret.setLastModified(dateMapper.map(rs, column(Columns.LAST_MODIFIED), ctx));
        return ret;
    }
  
    public static ScheduleEntryMapper withPrefix(String prefix)
    {
        return new ScheduleEntryMapper(prefix);
    }  

    public enum Columns implements TableColumnDefinition
    {
        ID("schedule_entry_id"),
        NAME(GenericColumns.NAME),
        LOADING_APPLICATION_ID("loading_application_id"),
        LOADING_APPLICATION_NAME("loading_application_name"),
        ROUTINGSPEC_ID("routingspec_id"),
        ROUTINGSPEC_NAME("routingspec_name"),
        START_TIME("start_time"),
        TIME_ZONE("timezone"),
        RUN_INTERVAL("run_interval"),
        ENABLED("enabled"),
        LAST_MODIFIED("last_modified")
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
            return this.column;
        }
        
    }

}
