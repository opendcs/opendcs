package opendcs.opentsdb;

import static org.slf4j.helpers.Util.getCallingClass;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;

import org.opendcs.tsdb.TaskListEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import opendcs.dao.DatabaseConnectionOwner;
import opendcs.dao.TaskListDao;

public class OpenHydroDbTaskListDao extends TaskListDao
{
    private static final Logger log = LoggerFactory.getLogger(getCallingClass());

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
    public List<TaskListEntry> getEntriesFor(DbKey appId, int amount) throws DbIoException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getEntriesFor'");
    }

    @Override
    public void deleteEntries(Collection<TaskListEntry> entries) throws DbIoException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteEntries'");
    }
}
