package org.opendcs.database.dai;

import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;

import decodes.tsdb.CpDependsNotify;

/**
 * Interface to handle centralized processing of Comp Depends Notification records
 */
public interface CompDependsNotifyDao extends OpenDcsDao
{
    /**
     * Retrieve all current notification records.
     *
     * @param tx
     * @return A valid list, which maybe empty.
     * @throws OpenDcsDataException
     */
    List<CpDependsNotify> getAllNotifyRecords(DataTransaction tx) throws OpenDcsDataException;

    /**
     * Retrieve the next notification record.
     *
     * @param tx
     * @return a Single record, or null if none exist.
     * @throws OpenDcsDataException
     */
    Optional<CpDependsNotify> getNextRecord(DataTransaction tx) throws OpenDcsDataException;

    /**
     * Delete a notification record from the table.
     *
     * Implementations should check for null and return immediately.
     *
     * @param tx
     * @param record The record to delete.
     * @throws OpenDcsDataException
     */
    void deleteNotifyRecord(DataTransaction tx,CpDependsNotify notificationRecord) throws OpenDcsDataException;

    /**
     * Save a new notification record to the database
     *
     * @param tx
     * @param record Only EventType and Key are required to be set. Other values set in the record are ignored.
     * @throws OpenDcsDataException
     */
    void saveRecord(DataTransaction tx, CpDependsNotify notificationRecord) throws OpenDcsDataException;
}
