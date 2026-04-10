package org.opendcs.database.dai;

import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;

import decodes.db.DataSource;
import decodes.sql.DbKey;

public interface DataSourceDao extends OpenDcsDao
{
    /**
     * Retrieve a DataSource by DbKey.
     * 
     * Unlike other DAOs, at this time, the DataSource will not have been "prepared for exec."
     * 
     * @param tx active transaction to use
     * @param id Identifier of the DataSource desired
     * @return DataSource, including all group members if any
     * @throws OpenDcsDataException
     */
    Optional<DataSource> getDataSource(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * Retrieve a DataSource by name.
     * 
     * Unlike other DAOs, at this time, the DataSource will not have been "prepared for exec."
     * 
     * @param tx active transaction to use
     * @param name Name of the DataSource desired
     * @return DataSource, including all group members if any
     * @throws OpenDcsDataException
     */
    Optional<DataSource> getDataSource(DataTransaction tx, String name) throws OpenDcsDataException;

    /**
     * Save or update a DataSource within the database
     * @param tx active transaction to use
     * @param dataSource data source to save or update, do not set the Id if new.
     * @return a new DataSource instance. With new ID if the data source doesn't already exist.
     * @throws OpenDcsDataException
     */
    DataSource save(DataTransaction tx, DataSource dataSource) throws OpenDcsDataException;

    /**
     * Removes a data source from the database.
     * @param tx active transaction to use
     * @param id
     * @throws OpenDcsDataException
     */
    void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * Retrieve all DataSources, with pagination if desired.
     * 
     * Sources are always sorted by DataSource name with group members, if any, sorted by the sequence number.
     * 
     * @param tx active transaction to use
     * @param limit how many, -1 for all
     * @param offset where to start, -1 for not used.
     * @return List of data sources.
     * @throws OpenDcsDataException
     */
    List<DataSource> getDataSources(DataTransaction tx, int limit, int offset) throws OpenDcsDataException;

}
