package opendcs.dao;

import decodes.tsdb.DbIoException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;


import opendcs.dai.TaskListDAI;

import org.opendcs.tsdb.FailedTaskListEntry;
import org.opendcs.tsdb.TaskListEntry;

public abstract class TaskListDao extends DaoBase implements TaskListDAI
{
    // Oracle was providing things in the wrong timestamp using current_timestamp.
    // TODO: needs to be checked against Postgres
    final String curTimeSqlCommand;
    final String intervalSqlCommand;
    public TaskListDao(DatabaseConnectionOwner dco)
    {
        this(dco, "tasklist");
    }

    public TaskListDao(DatabaseConnectionOwner dco, String module)
    {
        this(dco, module, null);
    }

    public TaskListDao(DatabaseConnectionOwner dco, String module, Connection con)
    {
        super(dco, module, con);
        curTimeSqlCommand = dco.isOracle() ? "sysdate" : "current_timestamp" ;
        intervalSqlCommand = dco.isOracle() ? " ?/24 )" : " ? * INTERVAL '1' hour )" ; // Oracle date math
    }
    
    /**
     * Remove entries by record_num in a batch
     * @param entries
     * @throws DbIoException
     */
    @Override
    public void deleteEntries(Collection<? extends TaskListEntry> entries) throws DbIoException
    {
        try
        {
            doModifyBatch("delete from CP_COMP_TASKLIST where RECORD_NUM = ?", e ->
                new Object[] {e.getRecordNum()}, entries, 250);
        }
        catch (SQLException ex)
        {
            throw new DbIoException("Unable to delete collection of records.", ex);
        }
    }

    @Override
    public void updateFailedEntries(Collection<FailedTaskListEntry> entries, int maxRetries) throws DbIoException
    {
        final String deleteFailedAfterMaxRetries = "delete from CP_COMP_TASKLIST "
                                                 + "where RECORD_NUM = ? "
                                                 + "and ((" + curTimeSqlCommand + " - DATE_TIME_LOADED) > " // curTimeName
                                                 + intervalSqlCommand;
        final String updateFailedRetry = "update CP_COMP_TASKLIST set FAIL_TIME = " + curTimeSqlCommand + " where RECORD_NUM = ? and "
                                       + "( (" + curTimeSqlCommand + " - DATE_TIME_LOADED) <= " + intervalSqlCommand;
        if (maxRetries > 0)
        {
            try
            {
                doModifyBatch(deleteFailedAfterMaxRetries, e ->
                    {
                        return new Object[]{e.getRecordNum(), maxRetries};
                    },
                    entries,
                    250
                );
                doModifyBatch(updateFailedRetry, e ->
                    {
                        return new Object[]{e.getRecordNum(), maxRetries};
                    },
                    entries, 250
                );
                }
                catch (SQLException ex)
                {
                    throw new DbIoException("Unable to delete/update failed entries.");
                }
        }
        else
        {
            updateFailedEntries(entries);
        }
    }

    @Override
    public void updateFailedEntries(Collection<FailedTaskListEntry> entries) throws DbIoException
    {
        final String updateFailTime = "UPDATE CP_COMP_TASKLIST "
                                    + " SET FAIL_TIME = " + curTimeSqlCommand
        + " where record_num = ?";
        try
        {
            doModifyBatch(updateFailTime, e ->
                {
                    return new Object[]{e.getRecordNum()};
                },
                entries,
                250
            );
        }
        catch (SQLException ex)
        {
            throw new DbIoException("Unable to delete/update failed entries.");
        }
    }
}
