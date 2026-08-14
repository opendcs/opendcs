/*
 *  Copyright 2025-2026 OpenDCS Consortium and its Contributors
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

package org.opendcs.database.impl.cwms.dao;

import java.util.LinkedHashMap;
import java.util.List;

import org.jdbi.v3.core.Handle;
import org.opendcs.cwms.data.CwmsOfficeBuilder;
import org.opendcs.data.Organization;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dai.OrganizationDao;
import org.opendcs.database.impl.cwms.jdbi.mapper.CwmsOfficeMapper;
import org.opendcs.database.impl.cwms.jdbi.mapper.office.CwmsOfficeReducer;
import org.opendcs.database.impl.opendcs.jdbi.logging.DetailSqlLogger;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.opendcs.utils.sql.SqlQueries;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.stringtemplate.v4.ST;

@ServiceProvider(service = OrganizationDao.class, path ="dao/CWMS-Oracle")
public final class CwmsOrganizationDao implements OrganizationDao
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    private static final String SELECT = """
            select <primary>, <reports_to>
              from cwms_v_office p
             left outer join cwms_v_office rt on rt.office_code = p.report_to_office_code
                                             and rt.office_code != p.office_code
             order by p.office_id <collate> asc
            <if(limit_clause)><limit_clause><endif>

        """;

    private CwmsOfficeMapper officeMapper = CwmsOfficeMapper.withPrefix("p");
    private CwmsOfficeMapper reportsToMapper = CwmsOfficeMapper.withPrefix("rt");

    @Override
    public List<Organization> getAll(DataTransaction tx, int limit, int offset)
            throws OpenDcsDataException
    {
        ST template = new ST(SELECT);
        Handle handle = tx.connection(Handle.class).orElseThrow();
        var ctx = tx.getContext();
        var dbEngine = ctx.getDatabaseEngine();
        template.add("primary", officeMapper.columnsForSelect())
                .add("reports_to", reportsToMapper.columnsForSelect())
                .add(SqlQueries.COLLATE_CLAUSE, SqlQueries.collateClauseFor(dbEngine));
        try(var query = handle.createQuery(template.render()))
        {


            if (limit > 0)
            {
                query.define("limit_clause", " OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY");
                query.bind("limit", limit);
                query.bind("offset", offset);
            }
            else
            {
                query.define("limit_clause", "");
            }
            query.setSqlLogger(new DetailSqlLogger(log));
            return query.registerRowMapper(officeMapper)
                        .reduceResultSet(new LinkedHashMap<>(), new CwmsOfficeReducer(officeMapper, reportsToMapper))
                        .values()
                        .stream()
                        .map(CwmsOfficeBuilder::build)
                        .map(o -> (Organization)o)
                        .toList();
        }
    }
}
