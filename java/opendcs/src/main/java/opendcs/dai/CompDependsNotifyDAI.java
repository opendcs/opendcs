package opendcs.dai;

import java.util.List;

import decodes.tsdb.CpDependsNotify;
import decodes.tsdb.DbIoException;

/**
 * Interface to handle centralized processing of Comp Depends Notification records
 */
public interface CompDependsNotifyDAI extends DaiBase
{
    /**
     * Retrieve all current notification records.
     * @return A valid list, which maybe empty.
     * @throws DbIoException
     */
    List<CpDependsNotify> getAllNotifyRecords() throws DbIoException;

    /**
     * Retrieve the next notification record.
     * @return a Single record, or null if none exist.
     * @throws DbIoException
     */
    CpDependsNotify getNextRecord() throws DbIoException;

    /**
     * Delete a notification record from the table.
     * 
     * Implementations should check for null and return immediately.
     * @param record The record to delete.
     * @throws DbIoException
     */
    void deleteNotifyRecord(CpDependsNotify record) throws DbIoException;

    /**
     * Save a new notification record to the database
     * @param record Only EventType and Key are required to be set. Other values set in the record are ignored.
     * @throws DbIoException
     */
    void saveRecord(CpDependsNotify record) throws DbIoException;
}
