package org.opendcs.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;

import javax.sql.DataSource;

import org.opendcs.authentication.AuthSourceService;
import org.opendcs.authentication.impl.NoOpAuthSourceProvider;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.settings.api.OpenDcsSettings;
import org.opendcs.spi.authentication.AuthSource;
import org.opendcs.spi.database.DatabaseProvider;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.DatabaseException;
import decodes.util.DecodesSettings;
import decodes.util.PropertiesOwner;
import ilex.util.AuthException;
import opendcs.util.functional.ThrowingFunction;

public class DatabaseService
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
        return getDatabaseFor(settings, provider -> provider.createDatabase(appName, settings, credentials));
    }

    /**
     * Handles the actual logic of looping through the providers
     * @param settings
     * @param createFunc function to apply once a valid provided is found.
     * @return
     * @throws DatabaseException
     */
    private static OpenDcsDatabase getDatabaseFor(DecodesSettings settings, ThrowingFunction<DatabaseProvider, OpenDcsDatabase, Exception> createFunc) throws DatabaseException
    {
        loader.reload();
        final Iterator<DatabaseProvider> providers = loader.iterator();
        ArrayList<String> attemptedProviders = new ArrayList<>();
        while (providers.hasNext())
        {
            final DatabaseProvider provider = providers.next();
            attemptedProviders.add(provider.getClass().getName());
            if (provider.canCreate(settings))
            {
                try
                {
                    return createFunc.accept(provider);
                }
                catch (Exception ex)
                {
                    throw new DatabaseException("Unable to create database instance.", ex);
                }
            }
        }

        throw new DatabaseException(String.format("""
                No provider found for database type  %s. Available types are %s.
                """, settings.editDatabaseType, String.join(",", attemptedProviders)));
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
        return getDatabaseFor(settings, provider -> provider.createDatabase(dataSource, settings));
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
        return loadSettingsFromProperties(dataSource, settings, props);
    }

    /**
     * NOTE: This will mutate the provided settings instance.
     * @param <T> Type of Setttings
     * @param dataSource
     * @param settings settings instance on which to load data
     * @return the passed in settings object
     */
    public static <T extends PropertiesOwner & OpenDcsSettings> T loadSettingsFromProperties(DataSource dataSource, T settings) throws DatabaseException
    {
        return loadSettingsFromProperties(dataSource, settings, new Properties());
    }

    /**
     * NOTE: This will mutate the provided settings instance.
     * @param <T> Type of Setttings
     * @param dataSource
     * @param settings settings instance on which to load data
     * @param props previously loaded properties that may be required
     * @return the passed in settings object
     */
    public static <T extends PropertiesOwner & OpenDcsSettings> T loadSettingsFromProperties(DataSource dataSource, T settings, Properties existingProps) throws DatabaseException
    {
        Properties props = new Properties();
        props.putAll(existingProps);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("select prop_name,prop_value from tsdb_property");
             ResultSet rs = stmt.executeQuery())
        {
            while (rs.next())
            {
                final String name = rs.getString("prop_name");
                final String value = rs.getString("prop_value");
                if (value != null)
                {
                    props.put(name, value);
                }
            }
            settings.loadFromProperties(props);
            return settings;
        }
        catch (SQLException ex)
        {
            throw new DatabaseException("Error retrieving settings data.", ex);
        }
    }
}
