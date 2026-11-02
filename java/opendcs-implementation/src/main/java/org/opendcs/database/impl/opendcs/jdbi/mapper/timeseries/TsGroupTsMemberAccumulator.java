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
 * Builds the time series members of every group in a group's hierarchy. Members come
 * back with one row per name of the series' site, so the first row for a member builds
 * the TimeSeriesIdentifier and the rows after it only add to it.
 */
public final class TsGroupTsMemberAccumulator
        implements ResultSetAccumulator<Map<TsGroupTsMemberAccumulator.MemberKey, TimeSeriesIdentifier>>
{
    private final TsGroupMappers mappers;
    private final List<MemberKey> missingTimeSeries = new ArrayList<>();

    public TsGroupTsMemberAccumulator(TsGroupMappers mappers)
    {
        this.mappers = mappers;
    }

    /**
     * @return members that are in tsdb_group_member_ts but have no row in the
     * time_series_identifier view, and so could not be built.
     */
    public List<MemberKey> getMissingTimeSeries()
    {
        return Collections.unmodifiableList(missingTimeSeries);
    }

    @Override
    public Map<MemberKey, TimeSeriesIdentifier> apply(Map<MemberKey, TimeSeriesIdentifier> previous,
            ResultSet rs, StatementContext ctx) throws SQLException
    {
        var key = new MemberKey(DbKey.createDbKey(rs, "group_id"), DbKey.createDbKey(rs, "ts_id"));
        var tsId = previous.get(key);

        if (tsId == null)
        {
            if (rs.getInt("ts_exists") == 0)
            {
                missingTimeSeries.add(key);
                return previous;
            }

            tsId = mappers.tsiMapper().map(rs, ctx);

            var dataType = mappers.dtMapper().map(rs, ctx);
            if (dataType != null)
            {
                tsId.setDataType(dataType);
            }

            // The site of a series is required, but check anyway: the mapper below
            // assumes it has a row to read.
            var siteId = DbKey.createDbKey(rs, mappers.siteMapper().column(OpenDcsSiteMapper.Columns.ID));
            if (!DbKey.isNull(siteId))
            {
                tsId.setSite(mappers.siteMapper().map(rs, ctx));
            }

            previous.put(key, tsId);
        }

        var siteName = mappers.siteNameMapper().map(rs, ctx);
        if (siteName != null && tsId.getSite() != null)
        {
            tsId.getSite().addName(siteName);
        }

        return previous;
    }

    /**
     * A time series as a member of one particular group. The same series is, of course,
     * allowed to be a member of any number of groups.
     */
    public record MemberKey(DbKey groupId, DbKey tsId)
    {
    }
}
