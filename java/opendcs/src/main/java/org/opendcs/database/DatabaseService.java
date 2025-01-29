package org.opendcs.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Properties;
import java.util.ServiceLoader;

import org.opendcs.authentication.AuthSourceService;
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
            final AuthSource auth = AuthSourceService.getFromString(settings.DbAuthFile);
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
    public static OpenDcsDatabase getDatabaseFor(javax.sql.DataSource dataSource) throws DatabaseException
    {
        /**
         * extract properties; requires simple table
         */
        DecodesSettings settings = decodesSettingsFromJdbc(dataSource);

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
    private static DecodesSettings decodesSettingsFromJdbc(javax.sql.DataSource dataSource) throws DatabaseException
    {
        DecodesSettings settings = new DecodesSettings();
        // TODO: Need a special case for the XML database.
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("select name,value from database_properties");
             ResultSet rs = stmt.executeQuery())
        {
            //
            while (rs.next())
            {
                final String name = rs.getString("name");
                final String value = rs.getString("value");
                //TODO: map database name/values to appropriate field using reflection.
            } 
        }
        catch (SQLException ex)
        {
            throw new DatabaseException("Error retrieving settings data.", ex);
        }

        return settings;
    }
}
