package org.opendcs.tsdb;

import java.util.Date;

import decodes.sql.DbKey;
import decodes.tsdb.TimeSeriesIdentifier;

/**
 * Baseline TaskList record to cover the use case that is required by
 * all implementations.
 * Implementations should implement this interface and add additional functions 
 * as required to meet their needs.
 *
 */
public interface TaskListEntry
{
    /**
     * Arbitrary key used for delete operations.
     * @return
     */
    public long getRecordNum();
    /**
     * Which application this element is for.
     * @return
     */
    public DbKey getLoadingApp();
    /**
     * Which timeseries this entry is for
     * @return
     */
    public TimeSeriesIdentifier getTimeSeriesIdentifier();
    /**
     * When this entries was saved to the database
     * @return
     */
    public Date getLoadedTime();
    /**
     * The sample time of the date.
     * @return
     */
    public Date getSampleTime();
    /**
     * Time this entry was returned to the list after a failed computation.
     * @return
     */
    public Date getFailTime();
    /**
     * Whether the delete flag is set. Some algorithms operate on deleted data in different ways.
     * @return
     */
    public boolean isDeleted();

    /**
     * Was the value in the database null.
     * @return
     */
    public boolean valueWasNull();
}
