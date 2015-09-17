package decodes.cwms.validation.dao;

import java.util.ArrayList;

import decodes.cwms.validation.Screening;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesIdentifier;

public interface ScreeningDAI
{
	/**
	 * Write a screening object to the CWMS database.
	 * This takes care of whether the screening object existed previously or
	 * if it is new.
	 * @param screening the Screening object to write.
	 * @throws DbIoException on any database error.
	 */
	public void writeScreening(Screening screening)
		throws DbIoException;
	
	/**
	 * Deletes a screening object from the database
	 * @param screening The screening to delete, only the DbKey screeningCode is used.
	 * @throws DbIoException on any database error.
	 */
	public void deleteScreening(Screening screening)
		throws DbIoException;
	
	/**
	 * Retrieves a screening object given its surrogate key.
	 * @param screeningCode the surrogate key
	 * @return the screening object
	 * @throws DbIoException
	 * @throws NoSuchObjectException if no such screening exists.
	 */
	public Screening getByKey(DbKey screeningCode)
		throws DbIoException, NoSuchObjectException;
	
	/**
	 * Return the screening associated with the passed time series identifier,
	 * or null if there is no association.
	 * @param tsid the time series identifier
	 * @return the screening, or null if none is associated with tsid.
	 * @throws DbIoException on any database error.
	 */
	public TsidScreeningAssignment getScreeningForTS(TimeSeriesIdentifier tsid)
		throws DbIoException;
	
	/**
	 * Clear any cached screenings. Used by GUI when Refresh is pressed.
	 */
	public void clearCache();
	
	/**
	 * Used by GUI to populate the list tab.
	 * @return list of all screening objects in the database.
	 */
	public ArrayList<Screening> getAllScreenings()
		throws DbIoException;
	
	/**
	 * Return an array of all screening assignments.
	 * @param activeOnly if true, then only return active assignments
	 * @return an array of all screening assignments
	 * @throws DbIoException on any SQL error
	 */
	public ArrayList<TsidScreeningAssignment> getTsidScreeningAssignments(boolean activeOnly)
		throws DbIoException;
	
	/**
	 * Assign the passed screening to the passed time series identifier
	 * @param screening the screening object
	 * @param tsid the time series identifier
	 * @param active if true then the screening is active
	 * @throws DbIoException on any SQL error
	 */
	public void assignScreening(Screening screening, TimeSeriesIdentifier tsid, boolean active)
		throws DbIoException;
	
	/**
	 * Unassign the passed screening from the passed time series identifier.
	 * If tsid is null, then unassign the screening from all time series identifiers.
	 * @param screening the screening
	 * @param tsid the time series identifier
	 * @throws DbIoException on any SQL error
	 */
	public void unassignScreening(Screening screening, TimeSeriesIdentifier tsid)
		throws DbIoException;
	
	/**
	 * Given a string ID, return the surrogate key for this screening, or NullKey if undefined.
	 * @param screeningId the string screening ID
	 * @return the key or DbKey.NullKey if undefined.
	 * @throws DbIoException on any database error
	 */
	public DbKey getKeyForId(String screeningId)
		throws DbIoException;
	
	/**
	 * Rename a screening
	 * @param oldId the old unique ID
	 * @param newId the new unique ID
	 * @throws DbIoException on any database error
	 */
	public void renameScreening(String oldId, String newId)
		throws DbIoException;
	
	/**
	 * Modify a screening's description. The old description is replaced with the passed
	 * value, which may contain newlines and or carriage returns.
	 * @param screeningId the unique screening id
	 * @param desc the new description
	 * @throws DbIoException on any database error
	 */
	public void updateScreeningDescription(String screeningId, String desc)
		throws DbIoException;

	/**
	 * Free any resource allocated in the DAO.
	 */
	public void close();
}
