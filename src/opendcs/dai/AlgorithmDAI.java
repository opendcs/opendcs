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

import decodes.sql.DbKey;
import decodes.tsdb.ConstraintException;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;


/**
 * Defines public interface for reading/writing algorithm objects.
 * @author mmaloney - Mike Maloney, Cove Software, LLC
 */
public interface AlgorithmDAI
{
	
	/**
	 * Given an algorithm name, return the unique ID.
	 * @param name the name
	 * @return the id
	 */
	public DbKey getAlgorithmId(String name)
		throws DbIoException, NoSuchObjectException;
	
	/**
	 * Return an algorithm with its subordinate meta-data.
	 * @param name the unique name of the algorithm
	 * @return an algorithm with its subordinate meta-data.
	 * @throws NoSuchObjectException if named algorithm doesn't exist.
	 * @throws DbIoException on Database IO error.
	 */
	public DbCompAlgorithm getAlgorithm(String name)
		throws DbIoException, NoSuchObjectException;

	/**
	 * Return an algorithm with its subordinate meta-data.
	 * @param id the algorithm unique ID.
	 * @return an algorithm with its subordinate meta-data.
	 * @throws NoSuchObjectException if named algorithm doesn't exist.
	 * @throws DbIoException on Database IO error.
	 */
	public DbCompAlgorithm getAlgorithmById(DbKey id)
		throws DbIoException, NoSuchObjectException;
	
	/**
	 * Return complete list of algorithms.
	 * @return
	 * @throws DbIoException
	 */
	public ArrayList<DbCompAlgorithm> listAlgorithms()
		throws DbIoException;
	
	/**
	 * Writes an algorithm to the database.
	 * @throws DbIoException on Database IO error.
	 */
	public void writeAlgorithm( DbCompAlgorithm algo )
		throws DbIoException;

	/**
	 * Deletes an algorithm from the database. 
	 * Fails silently if the named algorithm doesn't exist.
	 * @param id the algorithm ID
	 * @throws DbIoException on Database IO error.
	 * @throws ConstraintException if any computations currently depend on
	 * this algorithm.
	 */
	public void deleteAlgorithm(DbKey id)
		throws DbIoException, ConstraintException;

	
	/**
	 * Closes any resources opened by the DAO
	 */
	public void close();
}
