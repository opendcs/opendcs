/*
 * $Id$
 * 
 * $Log$
 * Revision 1.6  2016/11/19 15:55:51  mmaloney
 * Join PARM records with parent records to bring in implicit VPD filter in CWMS.
 *
 * Revision 1.5  2016/03/24 19:21:07  mmaloney
 * Support for algo scripts.
 *
 * Revision 1.4  2016/01/27 22:12:10  mmaloney
 * When reading the complete list, get the params and props in one go rather than
 * individually.
 *
 * Revision 1.3  2015/12/30 20:39:23  mmaloney
 * Bugfix: listAlgorithms was using the same Statement in nested queries, causing SQL exception.
 *
 * Revision 1.2  2014/07/03 12:53:41  mmaloney
 * debug improvements.
 *
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

import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.util.Base64;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import opendcs.dai.AlgorithmDAI;
import decodes.sql.DbKey;
import decodes.tsdb.ConstraintException;
import decodes.tsdb.DbAlgoParm;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbCompAlgorithmScript;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.ScriptType;
import decodes.tsdb.TsdbDatabaseVersion;
import decodes.tsdb.compedit.AlgorithmInList;


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
	public Connection getConnection()
	{
		// Overriding getConnection allows lazy connection setting and
		// ensures subordinate DAOs will use same conn as this object.
		if (myCon == null)
		{
			super.getConnection();
			propertiesSqlDao.setManualConnection(myCon);
		}
		return myCon;
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
	
	@Override
	public ArrayList<DbCompAlgorithm> listAlgorithms()
		throws DbIoException
	{
		ArrayList<DbCompAlgorithm> ret = new ArrayList<DbCompAlgorithm>();
		String q = "select * from CP_ALGORITHM";
		try
		{
			ResultSet rs = doQuery(q);
			while (rs.next())
			{
				DbKey id = DbKey.createDbKey(rs, 1);
				String nm = rs.getString(2);
				String cls = rs.getString(3);
				String cmmt = rs.getString(4);
				DbCompAlgorithm algo = new DbCompAlgorithm(id, nm, cls, cmmt);

				ret.add(algo);
			}
	
			q = "select a.* from CP_ALGO_TS_PARM a, CP_ALGORITHM b "
				+ "where a.ALGORITHM_ID = b.ALGORITHM_ID";
			rs = doQuery(q);
			while(rs.next())
			{
				DbKey algoId = DbKey.createDbKey(rs, 1);
				String role = rs.getString(2);
				String type = rs.getString(3);
				for(DbCompAlgorithm algo : ret)
					if (algo.getId().equals(algoId))
					{
						algo.addParm(new DbAlgoParm(role, type));
						break;
					}
			}
			propertiesSqlDao.readPropertiesIntoList("CP_ALGO_PROPERTY", ret, null);
			
			if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_13)
			{
				// Join with CP_ALGORITHM for CWMS to implicitly filter by db_office_code.
				q = "select a.ALGORITHM_ID, a.SCRIPT_TYPE, a.SCRIPT_DATA "
					+ "from CP_ALGO_SCRIPT a, CP_ALGORITHM b "
					+ "where a.ALGORITHM_ID = b.ALGORITHM_ID "
					+ "order by ALGORITHM_ID, SCRIPT_TYPE, BLOCK_NUM";
				rs = doQuery(q);
				DbCompAlgorithm lastAlgo = null;
				while(rs.next())
				{
					DbKey algoId = DbKey.createDbKey(rs, 1);
					DbCompAlgorithm algo = null;
					for(DbCompAlgorithm dca : ret)
						if (dca.getId().equals(algoId))
						{
							algo = dca;
							break;
						}
					if (algo == null)
					{
						// Shouldn't happen because of the join
						continue;
					}
					if (algo != lastAlgo)
					{
						algo.clearScripts();
						lastAlgo = algo;
					}
					ScriptType scriptType = ScriptType.fromDbChar(rs.getString(2).charAt(0));
					String scriptData = rs.getString(3);
					if (scriptData != null)
						scriptData = new String(Base64.decodeBase64(scriptData.getBytes()));
					DbCompAlgorithmScript script = algo.getScript(scriptType);
					if (script == null)
					{
						script = new DbCompAlgorithmScript(algo, scriptType);
						algo.putScript(script);
					}
					script.addToText(scriptData);
				}
			}
			
			return ret;
		}
		catch(SQLException ex)
		{
			String msg = "Error reading algorithm list: " + ex;
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
		
		if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_13)
		{
			algo.clearScripts();
			q = "select SCRIPT_TYPE, SCRIPT_DATA from CP_ALGO_SCRIPT "
				+ "where ALGORITHM_ID = " + algo.getId()
				+ " order by SCRIPT_TYPE, BLOCK_NUM";
			rs = doQuery(q);
			while(rs.next())
			{
				ScriptType scriptType = ScriptType.fromDbChar(rs.getString(1).charAt(0));
				String b64 = rs.getString(2);
				String scriptData = new String(Base64.decodeBase64(b64.getBytes()));
Logger.instance().debug1("DAO fill subord: b64='" + b64 + "' scriptData='" + scriptData + "'");
				DbCompAlgorithmScript script = algo.getScript(scriptType);
				if (script == null)
				{
					script = new DbCompAlgorithmScript(algo, scriptType);
					algo.putScript(script);
				}
				script.addToText(scriptData);
			}
		}
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
			
			if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_13)
			{
				q = "DELETE FROM CP_ALGO_SCRIPT WHERE ALGORITHM_ID = " + algo.getId();
				doModify(q);
Logger.instance().debug1("AlgorithmDAO.writeAlgo algorithm has " + algo.getScripts().size() + " scripts.");
				for(DbCompAlgorithmScript script : algo.getScripts())
				{
					String text = script.getText();
					if (text == null || text.length() == 0)
						continue;
					// Have to convert to Base64 to preserve quotes, newlines, etc.
					String b64 = new String(Base64.encodeBase64(text.getBytes()));
Logger.instance().debug1("AlgorithmDAO.writeAlgo script " + script.getScriptType() + " text '"
	+ text + "' b64=" + b64);
					int blockNum = 1;
					while(b64 != null)
					{
						String block = b64.length() < 4000 ? b64 : b64.substring(0, 4000);
						b64 = (block == b64) ? null : b64.substring(4000);
						q = "INSERT INTO CP_ALGO_SCRIPT VALUES("
							+ algo.getId() + ", " 
							+ "'" + script.getScriptType().getDbChar() + "', "
							+ (blockNum++) + ", "
							+ sqlString(block)
							+ ")";
						doModify(q);
					}
				}
			}
			
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
			
			q = "delete from CP_ALGO_SCRIPT where ALGORITHM_ID = " + id;
			doModify(q);

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

	@Override
	public ArrayList<String> listAlgorithmNames() throws DbIoException
	{
		String q = "select ALGORITHM_NAME from CP_ALGORITHM";
		try
		{
			ResultSet rs = doQuery(q);
			ArrayList<String> ret = new ArrayList<String>();
			while(rs.next())
				ret.add(rs.getString(1));
			return ret;
		}
		catch(SQLException ex)
		{
			String msg = "Error listing algorithms: " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}

	@Override
	public ArrayList<AlgorithmInList> listAlgorithmsForGui()
		throws DbIoException
	{
		String q = "select algorithm_id, algorithm_name, "
			+ "exec_class, cmmnt "
			+ "from cp_algorithm";
		try
		{
			ResultSet rs = doQuery(q);
			ArrayList<AlgorithmInList> ret = new ArrayList<AlgorithmInList>();
			while(rs != null && rs.next())
				ret.add(new AlgorithmInList(DbKey.createDbKey(rs, 1), rs.getString(2),
					rs.getString(3), 0, TextUtil.getFirstLine(rs.getString(4))));
			
			q = "select a.algorithm_id, count(1) as CompsUsingAlgo "
				+ "from cp_algorithm a, cp_computation b "
				+ "where a.algorithm_id = b.algorithm_id "
				+ "group by a.algorithm_id";
			rs = doQuery(q);
			while(rs != null && rs.next())
			{
				DbKey algoId = DbKey.createDbKey(rs, 1);
				int numCompsUsing = rs.getInt(2);
				for(AlgorithmInList ail : ret)
				{
					if (ail.getAlgorithmId().equals(algoId))
					{
						ail.setNumCompsUsing(numCompsUsing);
						break;
					}
				}
			}
		
			return ret;
		}
		catch(SQLException ex)
		{
			String msg = "Error listing algorithms for GUI: " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}


}
