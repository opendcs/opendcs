package opendcs.dai;

import java.util.ArrayList;
import java.util.Date;

import decodes.db.ScheduleEntry;
import decodes.db.ScheduleEntryStatus;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;

public interface ScheduleEntryDAI
{
	/**
	 * List the schedule entries for a given loading application.
	 * If app is null, then list all entries.
	 * @param app the loading application, or null to list all entries.
	 * @return list of schedule entries
	 * @throws DbIoException on database error.
	 */
	public ArrayList<ScheduleEntry> listScheduleEntries(CompAppInfo app)
		throws DbIoException;
	
	/**
	 * Check the passed schedule entry against the database using the
	 * last modify time. If necessary, reload from database and return
	 * true. If it's already up-to-date, return false.
	 * @param scheduleEntry the schedule entry to check
	 * @return true if entry was reloaded, false if up-to-date.
	 * @throws DbIoException on database error
	 * @throws NoSuchObjectException if entry has been deleted from database
	 */
	public boolean checkScheduleEntry(ScheduleEntry scheduleEntry)
		throws DbIoException, NoSuchObjectException;
	
	/**
	 * Read a single schedule entry by its name
	 * @param name
	 * @return ScheduleEntry or null if no match found.
	 * @throws DbIoException
	 * @throws NoSuchObjectException
	 */
	public ScheduleEntry readScheduleEntry(String name)
		throws DbIoException;

	/**
	 * Write the schedule entry to the database.
	 * @param scheduleEntry
	 * @throws DbIoException on database error
	 */
	public void writeScheduleEntry(ScheduleEntry scheduleEntry)
		throws DbIoException;
	
	/**
	 * Delete the schedule entry from the database.
	 * @param scheduleEntry
	 * @throws DbIoException on database error
	 */
	public void deleteScheduleEntry(ScheduleEntry scheduleEntry)
		throws DbIoException;
	
	/**
	 * Return all the statuses for a given schedule entry, sorted in
	 * chronological ascending order (oldest first). If scheduleEntry==null,
	 * then return all schedule status.
	 * @param scheduleEntry
	 * @return list of schedule statuses
	 * @throws DbIoException on database error
	 */
	public ArrayList<ScheduleEntryStatus> 
		readScheduleStatus(ScheduleEntry scheduleEntry)
		throws DbIoException;
	
	/**
	 * Write the passed schedule status to the database. This may be an
	 * update of a previously written status.
	 * @param seStatus the status
	 * @throws DbIoException on database error
	 */
	public void writeScheduleStatus(ScheduleEntryStatus seStatus)
		throws DbIoException;

	/**
	 * This method trims the status table by deleting entries before a
	 * given cutoff.
	 * @param cutoff entries with last modify time before this date/time are deleted
	 * @throws DbIoException 
	 */
	public void deleteScheduleStatusBefore(CompAppInfo appInfo, Date cutoff)
		throws DbIoException;
	
	/**
	 * Deletes any status entries for a given schedule entry. Usually done when
	 * a schedule entry is about to be deleted.
	 * @param scheduleEntry the schedule entry
	 * @throws DbIoException
	 */
	public void deleteScheduleStatusFor(ScheduleEntry scheduleEntry)
		throws DbIoException;
	
	/**
	 * Get the last schedule status for the passed schedule entry
	 * @param scheduleEntry the schedule entry
	 * @return most recent status or null if none found
	 * @throws DbIoException
	 */
	public ScheduleEntryStatus getLastScheduleStatusFor(ScheduleEntry scheduleEntry)
		throws DbIoException;

	/**
	 * Closes any resources opened by the DAO
	 */
	public void close();
}
