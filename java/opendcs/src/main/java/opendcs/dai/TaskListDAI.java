package opendcs.dai;

import java.util.Collection;
import java.util.List;

import org.opendcs.tsdb.FailedTaskListEntry;
import org.opendcs.tsdb.TaskListEntry;

import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;

public interface TaskListDAI extends DaiBase
{
    /**
     * Retrieve a set of entries encoded into appropriate TaskListEntry objects
     *
     * @param appId Current Design: Loading application that we want data for.
     *              Future Design: Marker for parallel instances to retrieve arbitrary data? (more investigation is required.)
     * @param amount How much data to retrieve to process this batch.
     * @param includeFailed Whether data with fail_time set should be retrieved.
     * @return A list of TaskListEntries that can be further processed.
     * @throws DbIoException If anything goes wrong during the retrieval.
     */
    public List<? extends TaskListEntry> getEntriesFor(DbKey appId, int amount, boolean includeFailed) throws DbIoException;
    /**
     * Remove entries
     * @param entries
     * @throws DbIoException
     */
    public void deleteEntries(Collection<? extends TaskListEntry> entries) throws DbIoException;

    /**
     * Update TaskList entries for computations that failed during processing. Update to given number of times.
     * @param entries The task list entries
     * @param retryFailed whether or not we will actually update the fail time
     * @param maxRetries how many times we allow reprocessing to happen.
     * @throws DbIoException
     */
    public void updateFailedEntries(Collection<FailedTaskListEntry> entries, int maxRetries) throws DbIoException;

    /**
     * Update the fail_time, ignoring how many times it's already been updated
     * @param entries
     * @throws DbIoException
     */
    public void updateFailedEntries(Collection<FailedTaskListEntry> entries) throws DbIoException;
}
