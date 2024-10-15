package org.opendcs.spi.database;

import decodes.db.DatabaseException;
import decodes.util.DecodesSettings;

import java.util.Properties;

import org.opendcs.database.OpenDcsDatabase;

public interface DatabaseProvider
{
    /**
     * If this provider can create a database from the given settings.
     * @param settings
     * @return
     */
    boolean canCreate(DecodesSettings settings);

    /**
     * Create an instance of OpenDcsDatabase
     * @param appName name of connecting application. can be null.
     * @param settings DecodesSettings object
     * @param credentials Properties with appropriate credentials for the given database.
     * @return
     * @throws DatabaseException
     */
    OpenDcsDatabase createDatabase(String appName, DecodesSettings settings, Properties credentials) throws DatabaseException;

    /**
     * Create an instance of OpenDcsDatabase
     * @param settings DecodesSettigns object
     * @param credentials Properties with appropriate credentials for the given database.
     * @return
     * @throws DatabaseException
     */
    default OpenDcsDatabase createDatabase(DecodesSettings settings, Properties credentials) throws DatabaseException
    {
        return createDatabase(null, settings, credentials);
    }

    /**
     * Create an instance of OpenDcsDatabase with existing connection setup.
     * DecodesSettings will be filled in from the datasource.
     * @param dataSource any valid javax.sql.DataSource
     * @param DecodesSettings
     * @return
     * @throws DatabaseException
     */
    OpenDcsDatabase createDatabase(javax.sql.DataSource dataSource, DecodesSettings settings) throws DatabaseException;
}
