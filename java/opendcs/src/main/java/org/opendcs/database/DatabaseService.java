package org.opendcs.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Properties;
import java.util.ServiceLoader;

import javax.sql.DataSource;

import org.opendcs.authentication.AuthSourceService;
import org.opendcs.authentication.impl.NoOpAuthSourceProvider;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.spi.authentication.AuthSource;
import org.opendcs.spi.database.DatabaseProvider;

import decodes.db.DatabaseException;
import decodes.util.DecodesSettings;
import ilex.util.AuthException;

public class DatabaseService
{
    private static ServiceLoader<DatabaseProvider> loader = ServiceLoader.load(DatabaseProvider.class);

    public static OpenDcsDatabase getDatabaseFor(String appName, DecodesSettings settings) throws DatabaseException
    {
        try
        {
            final String authSource = settings.DbAuthFile != null
                                    ? settings.DbAuthFile
                                    : (NoOpAuthSourceProvider.PROVIDER_NAME+":");
            final AuthSource auth = AuthSourceService.getFromString(authSource);
            final Properties credentials = auth.getCredentials();
            return getDatabaseFor(appName, settings, credentials);
        }
        catch (AuthException ex)
        {
            throw new DatabaseException("Unable to get credentials for database.", ex);
        }
    }

    public static OpenDcsDatabase getDatabaseFor(String appName, DecodesSettings settings, Properties credentials) throws DatabaseException
    {
        final Iterator<DatabaseProvider> providers = loader.iterator();
        while (providers.hasNext())
        {
            final DatabaseProvider provider = providers.next();
            if (provider.canCreate(settings))
            {
                try
                {
                    return provider.createDatabase(appName, settings, credentials);
                }
                catch (Exception ex)
                {
                    throw new DatabaseException("Unable to create database instance.", ex);
                }
            }
        }
        throw new DatabaseException("No provider found for database type  " + settings.editDatabaseType);
    }

    /**
     * Create database given a valid DataSource. Will load settings from the database.
     * @param dataSource any valid javax.sql.DataSource. However, a connection pool is expected as multiple connections 
     *                   may be needed
     * @return
     * @throws DatabaseException any error with class initialization or loading of properties
     */
    public static OpenDcsDatabase getDatabaseFor(DataSource dataSource) throws DatabaseException
    {
        DecodesSettings settings = decodesSettingsFromJdbc(dataSource, new Properties());
        return createDatabaseFromSettings(dataSource, settings);
    }

    /**
     * Create database given a valid DataSource. Will load settings from the database.
     * @param dataSource any valid javax.sql.DataSource. However, a connection pool is expected as multiple connections
     *                   may be needed
     * @return
     * @throws DatabaseException any error with class initialization or loading of properties
     */
    public static OpenDcsDatabase getDatabaseFor(DataSource dataSource, Properties properties) throws DatabaseException
    {
        DecodesSettings settings = decodesSettingsFromJdbc(dataSource, properties);
        return createDatabaseFromSettings(dataSource, settings);
    }

    private static OpenDcsDatabase createDatabaseFromSettings(DataSource dataSource, DecodesSettings settings) throws DatabaseException
    {
        final Iterator<DatabaseProvider> providers = loader.iterator();
        while (providers.hasNext())
        {
            final DatabaseProvider provider = providers.next();
            if (provider.canCreate(settings))
            {
                return provider.createDatabase(dataSource, settings);
            }
        }
        throw new DatabaseException("No provider for database type " + settings.editDatabaseType);
    }

    /**
     * Retrieve the decodes settings.
     *
     * @param dataSource
     * @return
     * @throws DatabaseException
     */
    private static DecodesSettings decodesSettingsFromJdbc(DataSource dataSource, Properties props) throws DatabaseException
    {
        DecodesSettings settings = new DecodesSettings();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("select prop_name,prop_value from tsdb_property");
             ResultSet rs = stmt.executeQuery())
        {
            while (rs.next())
            {
                final String name = rs.getString("prop_name");
                final String value = rs.getString("prop_value");
                props.put(name, value);
            }
            settings.loadFromProperties(props);
        }
        catch (SQLException ex)
        {
            throw new DatabaseException("Error retrieving settings data.", ex);
        }

        return settings;
    }
}
