/*
 * $Id$
 * 
 * $Log$
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 */
package opendcs.dai;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import decodes.sql.DbKey;
import decodes.tsdb.CompFilter;
import decodes.tsdb.ConstraintException;
import decodes.tsdb.DbComputation;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.compedit.ComputationInList;

/**
 * Defines public interface for reading/writing site (i.e. location) objects.
 * @author mmaloney - Mike Maloney, Cove Software, LLC
 */
public interface ComputationDAI
	extends DaiBase
{
	/**
	 * Returns the computation with given ID. The computation is filled
	 * with all of its property, parameter, and algorithm links.
	 * @param compId the ID of the computation
	 * @return computation with given name.
	 * @throws DbIoException on Database IO error.
	 * @throws NoSuchObjectException if named computation doesn't exist.
	 */
	DbComputation getComputationById(DbKey compId)
		throws DbIoException, NoSuchObjectException;

	/**
	 * Returns the computation with given name. The computation is filled
	 * with all of its property, parameter, and algorithm links.
	 * @param name the name of the computation
	 * @return computation with given name.
	 * @throws DbIoException on Database IO error.
	 * @throws NoSuchObjectException if named computation doesn't exist.
	 */
	DbComputation getComputationByName(String name)
		throws DbIoException, NoSuchObjectException;
	
	/**
	 * This queries computations and does the filtering by app ID and
	 * algorithm ID only. The application further filters by parameters.
	 * @param filter the computation filter containing app and algorithm IDs
	 * @return List of computations
	 */
	ArrayList<DbComputation> listCompsForGUI(CompFilter filter)
		throws DbIoException;

	/**
	 * This queries computations and does the filtering by app ID and
	 * algorithm ID only. The application further filters by parameters.
	 * @param filter the computation filter containing app and algorithm IDs
	 * @return List of computations
	 */
	List<DbComputation> listComps(Predicate<DbComputation> filter)
			throws DbIoException;
	
	/**
	 * Writes a computation to the database.
	 * Note: Does not write the subordinate algorithm record.
	 * @throws DbIoException on Database IO error.
	 */
	void writeComputation( DbComputation comp )
		throws DbIoException;
	
	/**
	 * Given a computation name, return the unique ID.
	 * @param name the name
	 * @return the id
	 * @throws NoSuchObjectException if no match found
	 */
	DbKey getComputationId(String name)
		throws DbIoException, NoSuchObjectException;

	/**
	 * Deletes a computation with the specified computation ID.
	 * Does nothing if no such computation exists.
	 * @param id the computation ID
	 * @throws DbIoException on Database IO error.
	 * @throws ConstraintException if this comp cannot be deleted because
	 *  other records in the DB (like data) depend on it.
	 */
	void deleteComputation(DbKey id)
		throws DbIoException, ConstraintException;

	/**
	 * Closes any resources opened by the DAO
	 */
	void close();

}
