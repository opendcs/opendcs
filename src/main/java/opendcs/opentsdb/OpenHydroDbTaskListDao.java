package opendcs.opentsdb;

import static org.slf4j.helpers.Util.getCallingClass;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.opendcs.tsdb.BadTaskListEntry;
import org.opendcs.tsdb.TaskListEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesIdentifier;
import opendcs.dao.DatabaseConnectionOwner;
import opendcs.dao.TaskListDao;

public class OpenHydroDbTaskListDao extends TaskListDao
{
    private static final Logger log = LoggerFactory.getLogger(getCallingClass());
    final String getTaskListStmtQuery = 
            "select a.RECORD_NUM, a.TS_ID, a.num_value, a.txt_value, a.sample_time, a.LOADING_APPLICATION_ID, "
            + "a.DELETE_FLAG, a.flags, a.date_time_loaded, a.fail_time "
            + "from CP_COMP_TASKLIST a "
            + "where a.LOADING_APPLICATION_ID = ?";
    

    public OpenHydroDbTaskListDao(DatabaseConnectionOwner dco)
    {
        this(dco, "OpenHydroDbTaskList");
    }

    public OpenHydroDbTaskListDao(DatabaseConnectionOwner dco, String module)
    {
        super(dco, module);
    }

    public OpenHydroDbTaskListDao(DatabaseConnectionOwner dco, String module, Connection c)
    {
        super(dco, module, c);
    }

    @Override
    public List<? extends TaskListEntry> getEntriesFor(DbKey appId, int amount, boolean includeFailed) throws DbIoException
    {
        String withFailClause = getTaskListStmtQuery + getFailTimeClause(includeFailed);
        final String withSortAndLimit = withFailClause + 
            (db.isOracle() ? " and ROWNUM < ? order by a.ts_id, a.sample_time"
                           : " order by a.ts_id, a.sample_time limit ?"
            );
        try
        {
            return getResultsIgnoringNull(withSortAndLimit, rs -> rs2tasklistEntry(rs), appId, amount);
        }
        catch (SQLException ex)
        {
            if (log.isTraceEnabled())
            {
                log.trace("failed query was '{}'", withSortAndLimit);
            }
            throw new DbIoException("Unable to retrieve task list entries. with query ", ex);
        }
    }

    private TaskListEntry rs2tasklistEntry(ResultSet rs) throws SQLException
    {
        TaskListEntry entry = null;
        long recordNum = rs.getLong("record_num");
        DbKey tsKey = DbKey.createDbKey(rs, "ts_id");
        boolean deleteFlag = rs.getString("delete_flag").equals("TRUE") ? true : false;
        Date sampleTime = new Date(rs.getLong("sample_time"));
        Date dateTimeLoad = new Date(rs.getLong("date_time_loaded"));
        Date failTime = null;
        DbKey loadingApp = DbKey.createDbKey(rs, "loading_application_id");
        long ft = rs.getLong("fail_time");
        if(!rs.wasNull())
        {
            failTime = new Date(ft);
        }
        int flags = rs.getInt("flags");
        /**
         * NOTE: this isn't practical to due in a join yet. The OpenHydroDb needs a view for the ts_spec table
         * (Or alternatively the query just gets really messy looking which is probably fine.)
         */
        try(OpenTimeSeriesDAO tsDao = (OpenTimeSeriesDAO)this.db.makeTimeSeriesDAO())
        {
            final TimeSeriesIdentifier tsId = tsDao.getTimeSeriesIdentifier(tsKey);

            double val = rs.getDouble("num_value");
            if (!rs.wasNull())
            {
                entry = new OpenHydroDbNumericTaskListEntry(recordNum, loadingApp, tsId, dateTimeLoad,
                                                            sampleTime, failTime, null, val, flags, deleteFlag);
            }
            else
            {
                String txtVal = rs.getString("txt_value");
                if(!rs.wasNull())
                {
                    log.warn("A text value was present in the text list. This is not yet supported.");
                    entry = new BadTaskListEntry(recordNum, loadingApp, tsId, dateTimeLoad, sampleTime, failTime);
                }
                else
                {
                    // we'll assume this was numeric
                    entry = new OpenHydroDbNumericTaskListEntry(recordNum, loadingApp, tsId, dateTimeLoad,
                                                            sampleTime, failTime, null, null, flags, deleteFlag);
                }
            }
        }
        catch (DbIoException ex)
        {
            // how to we mark as bad (perhaps a generic BadTaskListEntry?)
            throw new SQLException("Unable to retrieve time series identifier for a tasklist entry.", ex);
        }
        catch (NoSuchObjectException ex)
        {
            log.atWarn()
               .setCause(ex)
               .log("No time series exists for ts id {}", ex);
            entry = new BadTaskListEntry(recordNum, loadingApp, null, dateTimeLoad, sampleTime, failTime);
        }
        return entry;
    }

    private String getFailTimeClause(boolean includeFailed)
    {
        String retVal = " and a.FAIL_TIME is null";
        if (includeFailed)
        {
            retVal = db.isOracle() ? " and (a.FAIL_TIME is null OR (current_timestamp -  a.FAIL_TIME) >= 3600000)"
                                : " and (a.FAIL_TIME is null OR (extract(epoch from now())*100) - a.FAIL_TIME >= 3600000)" ;
        }
        return retVal;
    }
}
