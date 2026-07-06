package org.opendcs.database.impl.cwms.jdbi.mapper;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.opendcs.database.api.OpenDcsDataRuntimeException;
import org.opendcs.database.model.mappers.sites.OpenDcsSiteMapper;

public class CwmsSiteMapper extends OpenDcsSiteMapper
{

    protected CwmsSiteMapper(String prefix)
    {
        super(prefix);
    }
    

    @Override
    /**
     * @param joinType type of join, without the word "join", can be null
     * @param idColumn
     * @param otherTable
     * @param otherIdColumn
     * @return
     */
    public String columnsForSelect()
    {
        final ArrayList<String> columnList = new ArrayList<>();
        columns.forEach(c ->
        {
            try
            {
                columnOverride(columnList, c);
            }
            catch (SQLException ex)
            {
                throw new OpenDcsDataRuntimeException("A very unlikely situtation has happened.", ex);
            }
        });

        return String.join(",", columnList);
    }

    /**
     * CWMS Doesn't have a modified time column so we replace it with the current timestamp
     * Additionally several of the columns have a different name or a fixed value so we override that
     * here as well.
     * 
     * NOTE: yes, this is a bit rediculous, but it seems easier than building an *entirely* different
     * SiteMapper + Columns implementation
     */
    private void columnOverride(List<String> columnList, OpenDcsSiteMapper.Columns c) throws SQLException
    {
        final String prefixNoUnderscore = prefix.substring(0, prefix.length() - 1);
        final String columnFormat = "%s.%s %s";
        if (c.equals(OpenDcsSiteMapper.Columns.MODIFY_TIME))
        {
            columnList.add(String.format("cast(%s as date) %s", "systimestamp", column(c)));
        }
        else if (c.equals(OpenDcsSiteMapper.Columns.ELEVATION_UNITS))
        {
            columnList.add(String.format("%s %s", "'m'", column(c)));
        }
        else if (c.equals(OpenDcsSiteMapper.Columns.ID))
        {
            columnList.add(String.format(columnFormat, prefixNoUnderscore, "location_code", column(c)));
        }
        else if (c.equals(OpenDcsSiteMapper.Columns.COUNTRY))
        {
            columnList.add(String.format(columnFormat, prefixNoUnderscore, "nation_id", column(c)));
        }
        else if (c.equals(OpenDcsSiteMapper.Columns.TIMEZONE))
        {
            columnList.add(String.format(columnFormat, prefixNoUnderscore, "time_zone_name", column(c)));
        }
        else if (c.equals(OpenDcsSiteMapper.Columns.REGION))
        {
            columnList.add(String.format(columnFormat, prefixNoUnderscore, "county_name", column(c)));
        }
        else if (c.equals(OpenDcsSiteMapper.Columns.STATE))
        {
            columnList.add(String.format(columnFormat, prefixNoUnderscore, "state_initial", column(c)));
        }
        else if (c.equals(OpenDcsSiteMapper.Columns.NEAREST_CITY))
        {
            columnList.add(String.format(columnFormat, prefixNoUnderscore, "nearest_city", column(c)));
        }
        else
        {
            columnList.add(String.format(columnFormat, prefixNoUnderscore, c.column(), column(c)));
        }
    }

    public static CwmsSiteMapper withPrefix(String prefix)
    {
        return new CwmsSiteMapper(prefix);
    }
}
