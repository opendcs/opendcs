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

package org.opendcs.database.impl.cwms.jdbi.mapper.office;

import static org.opendcs.utils.ExceptionUtil.wrappedComputeIfAbsent;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.jdbi.v3.core.result.ResultSetAccumulator;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.cwms.data.CwmsOfficeBuilder;
import org.opendcs.database.impl.cwms.jdbi.mapper.CwmsOfficeMapper;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.sql.DbKey;

public class CwmsOfficeReducer implements ResultSetAccumulator<Map<DbKey,CwmsOfficeBuilder>>
{

    private final CwmsOfficeMapper primaryOfficeMapper;
    private final CwmsOfficeMapper reportToOfficeMapper;

    public CwmsOfficeReducer(CwmsOfficeMapper primaryMapper, CwmsOfficeMapper reportToMapper)
    {
        this.primaryOfficeMapper = primaryMapper;
        this.reportToOfficeMapper = reportToMapper;
    }

    @Override
    public Map<DbKey, CwmsOfficeBuilder> apply(Map<DbKey, CwmsOfficeBuilder> previous, ResultSet rs, StatementContext ctx)
            throws SQLException 
    {
        var keyMapper = ctx.findColumnMapperFor(DbKey.class)
                           .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        var id = keyMapper.map(rs, primaryOfficeMapper.column(CwmsOfficeMapper.Columns.OFFICE_CODE), ctx);

        var office = wrappedComputeIfAbsent(previous, id, ofcId -> primaryOfficeMapper.map(rs, ctx), SQLException.class);
        
        var reportToId = keyMapper.map(rs, primaryOfficeMapper.column(CwmsOfficeMapper.Columns.REPORTS_TO_OFFICE_CODE), ctx);
        if (reportToId != null)
        {
            var reportToOffice = wrappedComputeIfAbsent(previous, reportToId, reportToOfcId -> reportToOfficeMapper.map(rs, ctx), SQLException.class);
            office.withReportsTo(reportToOffice);
        }

        return previous;    
    }
    
}
