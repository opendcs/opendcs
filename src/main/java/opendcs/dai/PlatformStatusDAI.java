package opendcs.dai;

import java.util.ArrayList;

import decodes.db.PlatformStatus;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;

public interface PlatformStatusDAI extends AutoCloseable
{
	/**
	 * Read the status record for a given platform
	 * @param platformId the platform ID
	 * @return the PlatformStatus record, or null if none currently stored
	 * @throws DbIoException on any I/O error
	 */
	public PlatformStatus readPlatformStatus(DbKey platformId)
		throws DbIoException;
	
	/**
	 * Write a platform status record.
	 * @param platformStatus the record to write
	 * @throws DbIoException on any I/O error
	 */
	public void writePlatformStatus(PlatformStatus platformStatus)
		throws DbIoException;
	
	/**
	 * Used by GUIs and report generators to list all platform status records.
	 * @return an array list with all current platform status records
	 * @throws DbIoException on a non-recoverable I/O error
	 */
	public ArrayList<PlatformStatus> listPlatformStatus()
		throws DbIoException;
	
	/**
	 * Deletes the status record for this platform, if one exists.
	 * @param platformId the platform ID
	 * @throws DbIoException on any database error
	 */
	public void deletePlatformStatus(DbKey platformId)
		throws DbIoException;
	
	/** Release any resources allocated */
	public void close();

}
