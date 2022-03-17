/*
 * $Id$
 * 
 * $Log$
 * Revision 1.2  2014/06/02 00:23:18  mmaloney
 * Add getLastModified method.
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 */
package opendcs.dai;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.ConstraintException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.LockBusyException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TsdbCompLock;

/**
 * Defines public interface for reading/writing loading application and locks.
 * @author mmaloney - Mike Maloney, Cove Software, LLC
 */
public interface LoadingAppDAI
	extends DaiBase
{
	/**
	 * Return a list of computations assigned to a given application.
	 * @param appId application ID to filter by, -1 to get all computations
	 * @param enabledOnly call with true to only return computations that are enabled.
	 * @return a list of computations assigned to a given application.
	 * @throws DbIoException on Database IO error.
	 */
	public List<String> listComputationsByApplicationId( DbKey appId, boolean enabledOnly )
		throws DbIoException;
	
	/**
	 * List computation applications defined in this database.
	 * Option to list only those that are used by at least one computation.
	 * @param usedOnly true if you only want to retrieve processes that are
	 * actually assigned to a computation.
	 * @return list of computations.
	 */
	public ArrayList<CompAppInfo> listComputationApps(boolean usedOnly)
		throws DbIoException;
	
	/**
	 * List computation applications defined in this database.
	 * That is, list only the ones in the inlist 
	 * @param usedOnly true if you only want to retrieve processes that are
	 * actually assigned to a computation.
	 * @return list of computations.
	 */
	public ArrayList<CompAppInfo> ComputationAppsIn(String inList)
		throws DbIoException;

	/**
	 * Retrieves a computation app's info, given it's ID.
	 * @param id the application ID.
	 * @return computation app info.
	 */
	public CompAppInfo getComputationApp(DbKey id)
		throws DbIoException, NoSuchObjectException;

	/**
	 * Retrieves a computation app's info, given it's unique name.
	 * @param name the unique application name.
	 * @return computation app info.
	 */
	public CompAppInfo getComputationApp(String name)
		throws DbIoException, NoSuchObjectException;

	/**
	 * Retrieves a computation app's surrogate key, given it's unique name.
	 * @param name the unique application name.
	 * @return surrogate key.
	 */
	public DbKey lookupAppId(String name)
		throws DbIoException, NoSuchObjectException;

	/**
	 * Writes an computation app to the database.
	 * @throws DbIoException on Database IO error.
	 */
	public void writeComputationApp(CompAppInfo app)
		throws DbIoException;
	
	/**
	 * Deletes a computation app.
	 * Caller is responsible to check to make sure it's OK to do this.
	 */
	public void deleteComputationApp(CompAppInfo app)
		throws DbIoException, ConstraintException;

	/**
	 * Called by a comp proc to obtain the lock for 
	 * the specified application ID. 
	 * @param appId the application ID returned by connect()
	 * @param pid the system-unique process ID.
	 * @param host the host name where this process is running.
	 * @return the lock object.
	 * @throws LockBusyException if lock is unavailable or has been released by 
	 * another process.
	 */
	public TsdbCompLock obtainCompProcLock( CompAppInfo appInfo, int pid, String host )
		throws LockBusyException, DbIoException;
	
	/**
	 * Release the computation processor semaphore lock for the specified
	 * application.
	 * @param lock the lock to release
	 * @throws LockBusyException if lock cannot be released.
	 */
	public void releaseCompProcLock( TsdbCompLock lock )
		throws DbIoException;
	
	/**
	 * Called periodically by the running application
	 * to make sure this lock is valid in the database.
	 * Also updates the 'heartbeat' value in the database with the value found
	 * in the passed lock-object. (Application must update this manually.)
	 * 
	 * @param lock the lock object.
	 * @throws DbIoException if error reading lock info from db.
	 * @throws LockBusyException if the lock has been deleted.
	 */
	public void checkCompProcLock( TsdbCompLock lock)
		throws LockBusyException, DbIoException;
	
	/**
	 * @return an array of all lock objects currently defined in the
	 * database.
	 * @throws DbIoException on Database IO error.
	 */
	public List<TsdbCompLock> getAllCompProcLocks( )
		throws DbIoException;

	/**
	 * @return true if this DAO supports loading app locks.
	 */
	public boolean supportsLocks();

	/**
	 * Closes any resources opened by the DAO
	 */
	public void close();
	
	/**
	 * @return the database last modify time for the app or null if
	 * no LastModified property is defined.
	 * @param appId The application key
	 */
	public Date getLastModified(DbKey appId);
}
