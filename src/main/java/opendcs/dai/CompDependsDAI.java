package opendcs.dai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import decodes.sql.DbKey;
import decodes.tsdb.CpCompDependsRecord;
import decodes.tsdb.CpDependsNotify;
import decodes.tsdb.DbComputation;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesIdentifier;
import opendcs.util.functional.ThrowingConsumer;

public interface CompDependsDAI
	extends DaiBase
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
	public CpDependsNotify getCpCompDependsNotify() throws DbIoException;

	public void clearScratchpad()  throws DbIoException;

	public List<CpCompDependsRecord> getAllCompDependsEntries() throws DbIoException;

	public void addRecords(Collection<CpCompDependsRecord> records) throws DbIoException;
	/**
	 * Adds all given records to the scratch pad table
	 * @param records
	 * @throws DbIoException
	 */
	public void addRecordsToScratchPad(Collection<CpCompDependsRecord> records) throws DbIoException;

	/**
	 * Move the records from the scratch pad to the active table.
	 *
	 * @throws DbIoException
	 */
	public void mergeScratchPadToActive() throws DbIoException;
	/**
	 * Closes any resources opened by the DAO
	 */
	public void close();

	/**
	 * Given that almost all operations on these tables should be run in a transaction.
	 * Return an new instance of CompDependsDAI that has a single connection with
	 * auto commit off.
	 * @param consumer
	 * @throws DbIoException
	 */
	public void transaction(ThrowingConsumer<CompDependsDAI,DbIoException> consumer) throws DbIoException;

	public void removeExistingFromScratch() throws DbIoException;

	public void fillActiveFromScratch() throws DbIoException;
}
