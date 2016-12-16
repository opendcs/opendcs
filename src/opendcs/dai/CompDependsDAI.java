package opendcs.dai;

import java.util.ArrayList;

import decodes.sql.DbKey;
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
	 * @param comp The computation in question
	 * @return list of TSIDs with CP_COMP_DEPENDS records indicating that they
	 * are triggers for the computation
	 * @throws DbIoException
	 */
	public ArrayList<TimeSeriesIdentifier> getTriggersFor(DbComputation comp)
		throws DbIoException;

	/**
	 * Closes any resources opened by the DAO
	 */
	public void close();

}
