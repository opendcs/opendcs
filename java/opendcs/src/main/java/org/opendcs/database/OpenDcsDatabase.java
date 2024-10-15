package org.opendcs.database;

import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.tsdb.TimeSeriesDb;
import opendcs.dai.DaiBase;

public interface OpenDcsDatabase
{
    /**
     * Get the Decodes Database
     * @return
     */
    Database getDecodesDatabase();

    /**
     * Get the timeseries Database
     * @return
     */
    TimeSeriesDb getTimeSeriesDb();
    
    /**
     * Retrieve DAO from the database
     * @param <T> DAO Type
     * @param dao Dao Class
     * @return A valid instance for this database
     */
    <T extends DaiBase> T getDao(Class<T> dao) throws DatabaseException;
}
