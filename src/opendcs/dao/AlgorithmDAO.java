/*
 * $Id$
 * 
 * $Log$
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * This software was written by Cove Software, LLC ("COVE") under contract 
 * to the United States Government. 
 * 
 * No warranty is provided or implied other than specific contractual terms
 * between COVE and the U.S. Government
 * 
 * Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
 */
package opendcs.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;

import opendcs.dai.AlgorithmDAI;

import decodes.sql.DbKey;
import decodes.tsdb.ConstraintException;
import decodes.tsdb.DbAlgoParm;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;


/**
 * Data Access Object for writing/reading Algorithm objects to/from a SQL database
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public class AlgorithmDAO 
	extends DaoBase 
	implements AlgorithmDAI
{
	private PropertiesSqlDao propertiesSqlDao = null;
	public AlgorithmDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, "AlgorithmDao");
		propertiesSqlDao = new PropertiesSqlDao(tsdb);
	}
	
	@Override
	public DbKey getAlgorithmId(String name)
		throws DbIoException, NoSuchObjectException
	{
		String q = "select ALGORITHM_ID from CP_ALGORITHM "
			+ "where ALGORITHM_NAME = '" + name + "'";
		try
		{
			ResultSet rs = doQuery(q);
			if (rs.next())
				return DbKey.createDbKey(rs, 1);
			throw new NoSuchObjectException("No algorithm for name '" + name 
				+ "'");
		}
		catch(SQLException ex)
		{
			String msg = "Error getting algorithm ID for name '" + name + "': "
				+ ex;
			failure(msg);
			throw new DbIoException(msg);
		}
	}

	public DbCompAlgorithm getAlgorithmById(DbKey id)
		throws DbIoException, NoSuchObjectException
	{
		String q = "select * from CP_ALGORITHM where ALGORITHM_ID = " + id;
		try
		{
			ResultSet rs = doQuery(q);
			if (rs.next())
			{
				//int id = rs.getInt(1);
				String nm = rs.getString(2);
				String cls = rs.getString(3);
				String cmmt = rs.getString(4);
				DbCompAlgorithm algo = new DbCompAlgorithm(id, nm, cls, cmmt);

				fillAlgorithmSubordinates(algo);
				return algo;
			}
			else
				throw new NoSuchObjectException("No algorithm with ID=" + id);
		}
		catch(SQLException ex)
		{
			String msg = "Error reading algorithm with ID=" + id + ": " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}


	/**
	 * Return an algorithm with its subordinate meta-data.
	 * @param name the unique name of the algorithm
	 * @return an algorithm with its subordinate meta-data.
	 * @throws NoSuchObjectException if named algorithm doesn't exist.
	 * @throws DbIoException on Database IO error.
	 */
	public DbCompAlgorithm getAlgorithm(String name)
		throws DbIoException, NoSuchObjectException
	{
		return getAlgorithmById(getAlgorithmId(name));
	}
	
	private void fillAlgorithmSubordinates(DbCompAlgorithm algo)
		throws SQLException, DbIoException
	{
		String q = "select * from CP_ALGO_TS_PARM where ALGORITHM_ID = "
			+ algo.getId();
		ResultSet rs = doQuery(q);
		while(rs.next())
		{
			String role = rs.getString(2);
			String type = rs.getString(3);
			algo.addParm(new DbAlgoParm(role, type));
		}
		
		propertiesSqlDao.readProperties("CP_ALGO_PROPERTY", "ALGORITHM_ID", 
			algo.getId(), algo.getProperties());
	}

	@Override
	public void writeAlgorithm( DbCompAlgorithm algo )
		throws DbIoException
	{
		DbKey id = algo.getId();
		boolean isNew = id.isNull();
		if (isNew)
		{
			// Could be import from XML to overwrite existing algorithm.
			try
			{
				id = getAlgorithmId(algo.getName());
				isNew = id.isNull();
				algo.setId(id);
			}
			catch(NoSuchObjectException ex) { /* ignore */ }
		}

		String q;
		try 
		{
			if (isNew)
			{
				id = getKey("CP_ALGORITHM");
				q = "INSERT INTO CP_ALGORITHM(algorithm_id, algorithm_name, "
					+ "exec_class, cmmnt) VALUES("
					+ id
					+ ", " + sqlString(algo.getName())
					+ ", " + sqlString(algo.getExecClass())
					+ ", " + sqlString(algo.getComment())
					+ ")";
				
				algo.setId(id);
			}
			else // update
				q = "UPDATE CP_ALGORITHM "
			  	+ "SET ALGORITHM_NAME = " + sqlString(algo.getName()) 
			  	+ ", EXEC_CLASS = " + sqlString(algo.getExecClass())
			  	+ ", CMMNT = " + sqlString(algo.getComment())
				+ " WHERE ALGORITHM_ID = " + id;

			doModify(q);
			if (!isNew)
			{
				// Delete & re-add parameters
				q = "DELETE FROM CP_ALGO_TS_PARM WHERE ALGORITHM_ID = " + id;
				doModify(q);
			}
			for(Iterator<DbAlgoParm> it = algo.getParms(); it.hasNext(); )
			{
				DbAlgoParm dap = it.next();
				q = "INSERT INTO CP_ALGO_TS_PARM VALUES ("
					+ id + ", "
					+ sqlString(dap.getRoleName()) + ", " 
					+ sqlString(dap.getParmType()) + ")"; 
				doModify(q);
			}
			
			propertiesSqlDao.writeProperties("CP_ALGO_PROPERTY", "ALGORITHM_ID", 
				id, algo.getProperties());
			
			q = "UPDATE CP_COMPUTATION "
			    + "SET DATE_TIME_LOADED = " + db.sqlDate(new Date())
			    + " WHERE ALGORITHM_ID = " + id;
			doModify(q);
		}
		catch(DbIoException ex)
		{
			warning(ex.getMessage());
			throw ex;
		}
	}
	
	@Override
	public void deleteAlgorithm(DbKey id)
		throws DbIoException, ConstraintException
	{
		try
		{
			String q = "select count(*) from CP_COMPUTATION "
				+ "where ALGORITHM_ID = " + id;
			ResultSet rs = doQuery(q);
			if (rs.next())
			{
				int n = rs.getInt(1);
				if (n > 0)
					throw new ConstraintException(
						"Cannot delete algorithm with ID=" + id
						+ " because " + n + " computations rely on it.");
			}
			q = "delete from CP_ALGO_TS_PARM where ALGORITHM_ID = " + id;
			doModify(q);
			propertiesSqlDao.deleteProperties("CP_ALGO_PROPERTY", "ALGORITHM_ID", id);
			q = "delete from CP_ALGORITHM where ALGORITHM_ID = " + id;
			doModify(q);
		}
		catch(SQLException ex)
		{
			String msg = "Error deleting algorithm with ID=" + id + ": " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}
	
	@Override
	public void close()
	{
		super.close();
		propertiesSqlDao.close();
	}

}
