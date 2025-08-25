package org.opendcs.database.api.dao;

import java.util.Properties;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseKey;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;

public interface PropertiesDAO extends OpenDcsDao
{
    /**
    * Write properties to the database
    * @param tx Active transaction to use for saving
    * @param tableName name of the properties table
    * @param idColumn name of the id column in the table
    * @param parentKey surrogate key of the parent element owning the properties
    * @param props the set of properties to be saved
    */
   void writeProperties(DataTransaction tx, String tableName, String idColumn, DatabaseKey parentKey, Properties props) throws OpenDcsDataException;

   /**
    * Write properties to the database
    * @param tx Active transaction to use for saving
    * @param tableName name of the properties table
    * @param idColumn name of the id column in the table
    * @param id2Column name of secondary integer column
    * @param parentKey surrogate key of the parent element owning the properties
    * @param props the set of properties to be saved
    */
   void writeProperties(DataTransaction tx, String tableName, String idColumn, String id2Column, DatabaseKey parentKey, int key2, Properties props) throws OpenDcsDataException;

   /**
    * Read properties from the database
    * @param tx Active transaction to use for reading
    * @param tableName name of the properties table
    * @param idColumn name of the id column in the table
    * @param parentKey surrogate key of the parent element owning the properties
    * @return the set of properties read
    */
   Properties readProperties(DataTransaction tx, String tableName, String idColumn, DatabaseKey parentKey) throws OpenDcsDataException;

   /**
    * Read properties from the database
    * @param tx Active transaction to use for reading
    * @param tableName name of the properties table
    * @param idColumn name of the id column in the table
    * @param parentKey surrogate key of the parent element owning the properties
    * @param id2Column name of secondary integer column
    * @param key2 secondary key, primarily used for sorting.
    * @return the set of properties read
    */
   Properties readProperties(DataTransaction tx, String tableName, String idColumn, String id2Column, DatabaseKey parentKey, int key2) throws OpenDcsDataException;

   /**
    * Delete properties from the database
    * @param tx Active transaction to use for deleting
    * @param tableName name of the properties table
    * @param idColumn name of the id column in the table
    * @param parentKey surrogate key of the parent element owning the properties
    * @throws OpenDcsDataException
    */
   void deleteProperties(DataTransaction tx, String tableName, String idColumn, DatabaseKey parentKey) throws OpenDcsDataException;

   /**
    * Delete properties from the database
    * @param tx Active transaction to use to retrieve or process data
    * @param tableName name of the properties table
    * @param idColumn name of the id column in the table
    * @param parentKey surrogate key of the parent element owning the properties
    * @param id2Column name of secondary integer column
    * @param key2 secondary key, primarily used for sorting.
    * @throws OpenDcsDataException
    */
   void deleteProperties(DataTransaction tx, String tableName, String idColumn, String id2Column, DatabaseKey parentKey, int key2) throws OpenDcsDataException;

}
