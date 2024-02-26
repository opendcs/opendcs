package opendcs.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.opendcs.tsdb.TaskListEntry;

import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;

public abstract class TaskListDao extends DaoBase
{
    public TaskListDao(DatabaseConnectionOwner dco)
    {
        this(dco, "tasklist");
    }

    public TaskListDao(DatabaseConnectionOwner dco, String module)
    {
        super(dco, module);
    }

    public TaskListDao(DatabaseConnectionOwner tsdb, String module, Connection con)
    {
        super(tsdb, module, con);
    }
    
    /**
     * Retrieve a set of entries encoded into appropraite TaskListEntry objects
     *     
     * @param appId Current Design: Loading application that we want data for.
     *              Future Design: Marker for parallel instances to retrieve arbitrary data? (more investigation is required.)
     * @param amount How much data to retrieve to process this batch.
     * @param includeFailed Whether data with fail_time set should be retrieved.
     * @return A list of TaskListEntries that can be further processed.
     * @throws DbIoException If anything goes wrong during the retrieval.
     */
    public abstract List<TaskListEntry> getEntriesFor(DbKey appId, int amount, boolean includeFailed) throws DbIoException;
    /**
     * Remove entries
     * @param entries
     * @throws DbIoException
     */
    public void deleteEntries(Collection<TaskListEntry> entries) throws DbIoException
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
}
