/*
 *  Copyright 2026 OpenDCS Consortium and its Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.opendcs.database.impl.opendcs.jdbi.mapper.apps;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.sql.DbKey;
import decodes.tsdb.TsdbCompLock;

public class CompLockMapper extends PrefixRowMapper<TsdbCompLock, CompLockMapper.Columns>
{
    protected CompLockMapper(String prefix)
    {
        super(prefix, "cp_comp_proc_lock", Columns.class);
    }

    @Override
    public TsdbCompLock map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        var keyMapper = ctx.findColumnMapperFor(DbKey.class)
                           .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        var dateMapper = ctx.findColumnMapperFor(Date.class)
                            .orElseThrow(() -> new SQLException(SqlErrorMessages.TIME_MAPPER_NOT_FOUND));
        var appId = keyMapper.map(rs, column(Columns.APP_ID), ctx);
        if (DbKey.isNull(appId) || rs.wasNull())
        {
            return null;
        }

        var pid = rs.getInt(column(Columns.PID));
        var hostName = rs.getString(column(Columns.HOSTNAME));
        var heartBeat = dateMapper.map(rs, column(Columns.HEARTBEAT), ctx);
        var status = rs.getString(column(Columns.STATUS));
        var name = rs.getString(column(Columns.APP_NAME));
        var ret = new TsdbCompLock(appId, pid, hostName, heartBeat, status);
        ret.setAppName(name);
        return ret;
    }


    public static CompLockMapper withPrefix(String prefix)
    {
        return new CompLockMapper(prefix);
    }
    
    public enum Columns implements TableColumnDefinition
    {
        APP_ID("loading_application_id"),
        // NOTE: the name is pulled from the joined loading_application_name table
        // and not specifically on this time. Didn't make sense to pull in the whole
        // LoadingAppDao, or table, just to get the name column
        APP_NAME("loading_application_name"),
        PID("pid"),
        HOSTNAME("hostname"),
        HEARTBEAT("heartbeat"),
        STATUS("cur_status")
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
