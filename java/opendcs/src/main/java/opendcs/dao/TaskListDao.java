package opendcs.dao;

import decodes.tsdb.DbIoException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;


import opendcs.dai.TaskListDAI;
import org.opendcs.tsdb.TaskListEntry;

public abstract class TaskListDao extends DaoBase implements TaskListDAI
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
}
