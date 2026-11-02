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
package org.opendcs.database.impl.opendcs.jdbi.mapper.timeseries;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jdbi.v3.core.result.ResultSetAccumulator;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.sites.OpenDcsSiteMapper;

import decodes.sql.DbKey;
import decodes.tsdb.TimeSeriesIdentifier;

/**
 * Builds a group's time series members from the joined rows of the tsMembers query. A
 * member comes back with one row per name of its site, so the first row for a member
 * builds the TimeSeriesIdentifier and the rows after it only add to it.
 */
public final class TsGroupTsMemberAccumulator implements ResultSetAccumulator<Map<DbKey, TimeSeriesIdentifier>>
{
    private final TsGroupMappers mappers;
    private final List<DbKey> unresolved = new ArrayList<>();

    public TsGroupTsMemberAccumulator(TsGroupMappers mappers)
    {
        this.mappers = mappers;
    }

    /**
     * @return members that are in tsdb_group_member_ts but have no row in the
     * time_series_identifier view, and so could not be built.
     */
    public List<DbKey> getUnresolved()
    {
        return Collections.unmodifiableList(unresolved);
    }

    @Override
    public Map<DbKey, TimeSeriesIdentifier> apply(Map<DbKey, TimeSeriesIdentifier> previous,
            ResultSet rs, StatementContext ctx) throws SQLException
    {
        var tsId = DbKey.createDbKey(rs, "ts_id");
        var resolved = previous.get(tsId);

        if (resolved == null)
        {
            if (rs.getInt("ts_exists") == 0)
            {
                unresolved.add(tsId);
                return previous;
            }

            resolved = mappers.tsiMapper().map(rs, ctx);

            var dataType = mappers.dtMapper().map(rs, ctx);
            if (dataType != null)
            {
                resolved.setDataType(dataType);
            }

            // A series has to have a site, but check anyway: the mapper below assumes it
            // has a row to read.
            var siteId = DbKey.createDbKey(rs, mappers.siteMapper().column(OpenDcsSiteMapper.Columns.ID));
            if (!DbKey.isNull(siteId))
            {
                resolved.setSite(mappers.siteMapper().map(rs, ctx));
            }

            previous.put(tsId, resolved);
        }

        var siteName = mappers.siteNameMapper().map(rs, ctx);
        if (siteName != null && resolved.getSite() != null)
        {
            resolved.getSite().addName(siteName);
        }

        return previous;
    }
}
