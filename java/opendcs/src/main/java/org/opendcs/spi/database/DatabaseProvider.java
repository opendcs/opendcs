package org.opendcs.spi.database;

import decodes.db.DatabaseException;
import decodes.util.DecodesSettings;

import java.sql.SQLException;
import java.util.Properties;

import org.opendcs.database.api.OpenDcsDatabase;

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
     * @param settings DecodesSettings object
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
     * @param settings DecodesSettings object
     * @returnOpenDcsDatabase
     * @throws DatabaseException
     */
    OpenDcsDatabase createDatabase(javax.sql.DataSource dataSource, DecodesSettings settings) throws DatabaseException;

    /**
     * Load the tsdb_property table to a properties object.
     * @param dataSource
     * @return
     * @throws DatabaseException
     */
    default Properties loadPropertiesTable(javax.sql.DataSource dataSource) throws DatabaseException
    {
        Properties props = new Properties();
        try (var c = dataSource.getConnection();
             var getProps = c.prepareStatement("select prop_name, prop_value from tsdb_property");
             var rs = getProps.executeQuery())
        {
            while(rs.next())
            {
                var key = rs.getString("prop_name");
                var value = rs.getString("prop_value");
                props.put(key, value);
            }
        }
        catch (SQLException ex)
        {
            throw new DatabaseException("Unable to load properties.", ex);
        }
        return props;
    }
}
