/*
 * $Id$
 * 
 * $Log$
 */
package opendcs.dao;

import ilex.util.HasProperties;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import opendcs.dai.PropertiesDAI;

import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;

/**
 * Data Access Object for writing/reading Properties objects to/from a SQL database.
 * There are several properties tables with the same three-column structure:
 * (id, prop_name, prop_value).
 * For backward compatibility we do not name the columns (some DECODES tables had
 * columns named 'name' and 'value').
 * The name of the id column varies. For this reason it must be passed to the read/write methods.
 * 
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public class PropertiesSqlDao 
	extends DaoBase 
	implements PropertiesDAI
{
	public PropertiesSqlDao(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, "PropertiesSqlDao");
	}
	
	@Override
	public void readProperties(String tableName, String idColumn, 
		DbKey parentKey, Properties props)
			throws DbIoException
	{
		String q = "select * from " + tableName + " where " + idColumn + " = " + parentKey;
		
		ResultSet rs = doQuery(q);
		try
		{
			while (rs != null && rs.next())
			{
				String name = rs.getString(2);
				String value = rs.getString(3);
				if (value == null)
					value = "";
				props.setProperty(name, value);
			}
		}
		catch (SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}
	
	@Override
	public void readProperties(String tableName, String idColumn,
		String id2Column, DbKey parentKey, int key2, Properties props)
		throws DbIoException
	{
		String q = "select * from " + tableName + " where " + idColumn + " = " + parentKey
			+ " and " + id2Column + " = " + key2;
		
		ResultSet rs = doQuery(q);
		try
		{
			while (rs != null && rs.next())
			{
				String name = rs.getString(3);
				String value = rs.getString(4);
				if (value == null)
					value = "";
				props.setProperty(name, value);
			}
		}
		catch (SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}

	@Override
	public void writeProperties(String tableName, String idColumn,
		DbKey parentKey, Properties props) throws DbIoException
	{
		deleteProperties(tableName, idColumn, parentKey);
		for(Object kob : props.keySet())
		{
			String key = (String)kob;
			String q = "insert into " + tableName + " values(" + parentKey
				+ ", " + sqlString(key) + ", " + sqlString(props.getProperty(key)) + ")"; 
			doModify(q);
		}
	}

	@Override
	public void writeProperties(String tableName, String idColumn,
		String id2Column, DbKey parentKey, int key2, Properties props)
		throws DbIoException
	{
		deleteProperties(tableName, idColumn, id2Column, parentKey, key2);
		for(Object kob : props.keySet())
		{
			String propName = (String)kob;
			String q = "insert into " + tableName + " values(" + parentKey
				+ ", " + key2
				+ ", " + sqlString(propName) + ", " + sqlString(props.getProperty(propName)) + ")"; 
			doModify(q);
		}
	}


	@Override
	public void deleteProperties(String tableName, String idColumn,
		DbKey parentKey) throws DbIoException
	{
		String q = "delete from " + tableName + " where " + idColumn + " = " + parentKey;
		doModify(q);
	}


	@Override
	public void deleteProperties(String tableName, String idColumn,
		String id2Column, DbKey parentKey, int key2) 
			throws DbIoException
	{
		String q = "delete from " + tableName + " where " + idColumn + " = " + parentKey
			+ " and " + id2Column + " = " + key2;
		doModify(q);
	}

	@Override
	public int readPropertiesIntoCache(String tableName, DbObjectCache<?> cache)
		throws DbIoException
	{
		String q = "select * from " + tableName;
		ResultSet rs = doQuery(q);
		int n = 0;
		try
		{
			while(rs != null && rs.next())
			{
				Object ob = cache.getByKey(DbKey.createDbKey(rs, 1));
				if (ob == null)
					continue;
				if (!(ob instanceof HasProperties))
					throw new DbIoException(
						"Cannot read properties because cached object is not HasProperties");

				String name = rs.getString(2);
				String value = rs.getString(3);
				if (value == null)
					value = "";
				
				HasProperties hp = (HasProperties)ob;
				hp.setProperty(name, value);
				n++;
			}
			return n;
		}
		catch (SQLException e)
		{
			throw new DbIoException("Error reading properties for table " 
				+ tableName + ": " + e.getMessage());
		}
	}

	@Override
	public int readPropertiesIntoList(String tableName, List<? extends CachableHasProperties> list,
		String whereClause)
		throws DbIoException
	{
		String q = "select * from " + tableName;
		if (whereClause != null)
			q = q + " " + whereClause;
		ResultSet rs = doQuery(q);
		int n = 0;
		try
		{
		  nextProp:
			while(rs != null && rs.next())
			{
				DbKey key = DbKey.createDbKey(rs, 1);
				for(CachableHasProperties chp : list)
					if (chp.getKey().equals(key))
					{
						String name = rs.getString(2);
						String value = rs.getString(3);
						if (value == null)
							value = "";

						chp.setProperty(name, value);
						n++;
						continue nextProp;
					}
				warning("Table '" + tableName + "' has property with key=" + key 
					+ " and no matching object in list.");
			}
			return n;
		}
		catch (SQLException e)
		{
			throw new DbIoException("Error reading properties for table " 
				+ tableName + ": " + e.getMessage());
		}
	}

}
