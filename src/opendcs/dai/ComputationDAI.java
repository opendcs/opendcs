/*
 * $Id$
 * 
 * $Log$
 */
package opendcs.dai;

import java.util.ArrayList;

import decodes.db.Site;
import decodes.db.SiteList;
import decodes.db.SiteName;
import decodes.sql.DbKey;
import decodes.tsdb.CompFilter;
import decodes.tsdb.ConstraintException;
import decodes.tsdb.DbComputation;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;

/**
 * Defines public interface for reading/writing site (i.e. location) objects.
 * @author mmaloney - Mike Maloney, Cove Software, LLC
 */
public interface ComputationDAI
{
	/**
	 * Returns the computation with given ID. The computation is filled
	 * with all of its property, parameter, and algorithm links.
	 * @param compId the ID of the computation
	 * @return computation with given name.
	 * @throws DbIoException on Database IO error.
	 * @throws NoSuchObjectException if named computation doesn't exist.
	 */
	public DbComputation getComputationById(DbKey compId)
		throws DbIoException, NoSuchObjectException;

	/**
	 * Returns the computation with given name. The computation is filled
	 * with all of its property, parameter, and algorithm links.
	 * @param name the name of the computation
	 * @return computation with given name.
	 * @throws DbIoException on Database IO error.
	 * @throws NoSuchObjectException if named computation doesn't exist.
	 */
	public DbComputation getComputationByName(String name)
		throws DbIoException, NoSuchObjectException;
	
	/**
	 * This queries computations and does the filtering by app ID and
	 * algorithm ID only. The application further filters by parameters.
	 * @param filter the computation filter containing app and algorithm IDs
	 * @return List of computations
	 */
	public ArrayList<DbComputation> listCompsForGUI(CompFilter filter)
		throws DbIoException;
	
	/**
	 * Writes a computation to the database.
	 * Note: Does not write the subordinate algorithm record.
	 * @throws DbIoException on Database IO error.
	 */
	public void writeComputation( DbComputation comp )
		throws DbIoException;
	
	/**
	 * Given a computation name, return the unique ID.
	 * @param name the name
	 * @return the id
	 * @throws NoSuchObjectException if no match found
	 */
	public DbKey getComputationId(String name)
		throws DbIoException, NoSuchObjectException;

	/**
	 * Deletes a computation with the specified computation ID.
	 * Does nothing if no such computation exists.
	 * @param id the computation ID
	 * @throws DbIoException on Database IO error.
	 * @throws ConstraintException if this comp cannot be deleted because
	 *  other records in the DB (like data) depend on it.
	 */
	public void deleteComputation(DbKey id)
		throws DbIoException, ConstraintException;

	/**
	 * Closes any resources opened by the DAO
	 */
	public void close();
}
