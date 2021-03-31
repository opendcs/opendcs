package opendcs.dai;

import java.util.ArrayList;
import java.util.Collection;

import decodes.sql.DbKey;
import decodes.tsdb.CpDependsNotify;
import decodes.tsdb.DbComputation;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesIdentifier;

public interface CompDependsDAI
{
	/**
	 * Removes any CompDepends records that reference the passed TSID
	 * @param tsid The Time Series Identifier
	 * @throws DbIoException on database error
	 */
	public void removeTsDependencies(TimeSeriesIdentifier tsid)
		throws DbIoException;

	/**
	 * Write the CP_COMP_DEPENDS records for the computation.
	 * @param comp the computation
	 * @throws DbIoException
	 */
	public void writeCompDepends( DbComputation comp )
		throws DbIoException;

	/**
	 * Remove any dependencies referencing the passed time series surrogate key
	 * @param timeSeriesKey the surrogate key
	 * @throws DbIoException
	 */
	public void deleteCompDependsForTsKey(DbKey timeSeriesKey)
		throws DbIoException;

	/**
	 * Remove any dependencies referencing the passed computation surrogate key.
	 * @param compId the surrogate key
	 * @throws DbIoException
	 */
	public void deleteCompDependsForCompId(DbKey compId)
		throws DbIoException;

	/**
	 * Find all TSIDs that can serve as triggers for the passed computation.
	 * @param compID The computation in question
	 * @return list of TSIDs with CP_COMP_DEPENDS records indicating that they
	 * are triggers for the computation
	 * @throws DbIoException
	 */
	public ArrayList<TimeSeriesIdentifier> getTriggersFor(DbKey compID)
		throws DbIoException;
	
	/**
	 * Find all computations that would be triggered by the passed set of TSIDs. If appId
	 * is not a NullKey, then also filter the computation list by the app ID.
	 * @param tsids The list of time series IDs
	 * @param appId Optional appId to filter list by
	 * @return list of dependent computations
	 * @throws DbIoException
	 */
	public ArrayList<DbKey> getCompIdsFor(Collection<TimeSeriesIdentifier> tsids, DbKey appId)
		throws DbIoException;
	
	/**
	 * Get next CP_COMP_DEPENDS_NOTIFY record and remove it from the table.
	 * @return next CP_COMP_DEPENDS_NOTIFY record or null if none.
	 */
	public CpDependsNotify getCpCompDependsNotify()
		throws DbIoException;


	/**
	 * Closes any resources opened by the DAO
	 */
	public void close();

}
