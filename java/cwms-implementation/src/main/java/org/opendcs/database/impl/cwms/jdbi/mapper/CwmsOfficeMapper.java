/*
 *  Copyright 2025 OpenDCS Consortium and its Contributors
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

package org.opendcs.database.impl.cwms.jdbi.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.cwms.data.CwmsOfficeBuilder;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.sql.DbKey;

public class CwmsOfficeMapper extends PrefixRowMapper<CwmsOfficeBuilder, CwmsOfficeMapper.Columns>
{
    protected CwmsOfficeMapper(String prefix)
    {
        super(prefix, "cwms_v_office", Columns.class);
    }

    @Override
    public CwmsOfficeBuilder map(ResultSet rs, StatementContext ctx) throws SQLException 
    {
        var keyMapper = ctx.findColumnMapperFor(DbKey.class)
                           .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        var id = keyMapper.map(rs, column(Columns.OFFICE_CODE), ctx);
        if (DbKey.isNull(id) || rs.wasNull())
        {
            return null;
        }

        return new CwmsOfficeBuilder()
            .withId(id)
            .withName(rs.getString(column(Columns.OFFICE_ID)))
            .withLongName(rs.getString(column(Columns.LONG_NAME)))
            .withEroc(rs.getString(column(Columns.EROC)))
            .withType(rs.getString(column(Columns.TYPE)))
            .withReportsToId(keyMapper.map(rs, column(Columns.REPORTS_TO_OFFICE_CODE), ctx))
            ;
    }

    public static CwmsOfficeMapper withPrefix(String prefix)
    {
        return new CwmsOfficeMapper(prefix);
    }

    public enum Columns implements TableColumnDefinition
    {
        OFFICE_CODE("office_code"),
        OFFICE_ID("office_id"),
        TYPE("office_type"),
        EROC("eroc"),
        LONG_NAME("long_name"),
        REPORTS_TO_OFFICE_CODE("report_to_office_code")
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
