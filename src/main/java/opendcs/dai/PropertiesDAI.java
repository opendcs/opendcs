package opendcs.dai;

import java.util.List;
import java.util.Properties;

import opendcs.dao.CachableHasProperties;
import opendcs.dao.DbObjectCache;

import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;

public interface PropertiesDAI
	extends DaiBase
{
	/**
	 * Write properties to the database
	 * @param tableName name of the properties table
	 * @param idColumn name of the id column in the table
	 * @param parentKey surrogate key of the parent element owning the properties
	 * @param props the properties object
	 */
	public void writeProperties(String tableName, String idColumn, DbKey parentKey, Properties props)
		throws DbIoException;

	/** Some properties tables have a secondary key (key1, key2, prop_name, prop_value) */
	public void writeProperties(String tableName, String idColumn, String id2Column,
		DbKey parentKey, int key2, Properties props)
		throws DbIoException;

	/**
	 * Read properties from the database
	 * @param tableName name of the properties table
	 * @param idColumn name of the id column in the table
	 * @param parentKey surrogate key of the parent element owning the properties
	 * @param props the properties object
	 */
	public void readProperties(String tableName, String idColumn, DbKey parentKey, Properties props)
		throws DbIoException;

	public void readProperties(String tableName, String idColumn, String id2Column,
		DbKey parentKey, int key2, Properties props)
		throws DbIoException;

	/**
	 * Delete properties from the database
	 * @param tableName name of the properties table
	 * @param idColumn name of the id column in the table
	 * @param parentKey surrogate key of the parent element owning the properties
	 * @throws DbIoException
	 */
	public void deleteProperties(String tableName, String idColumn, DbKey parentKey)
		throws DbIoException;

	public void deleteProperties(String tableName, String idColumn, String id2Column,
		DbKey parentKey, int key2)
		throws DbIoException;
	
	/** Silently close any resources opened */
	public void close();

	/** 
	 * Read entire properties table into cached objects in one go.
	 * @return number of properties added, in total.
	 */
	public int readPropertiesIntoCache(String tableName, DbObjectCache<?> cache)
		throws DbIoException;
	
	/**
	 * Read properties into a list of objects that have both and ID and properties
	 * @param tableName The properties table to read from
	 * @param list The list to put the properties into
	 * @param whereClause Usually either blank or an IN clause containing IDs.
	 * @return
	 */
	public int readPropertiesIntoList(String tableName, List<? extends CachableHasProperties> list,
		String whereClause)
		throws DbIoException;

}
