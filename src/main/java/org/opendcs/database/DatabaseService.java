package org.opendcs.database;

import java.util.Iterator;
import java.util.Properties;
import java.util.ServiceLoader;

import org.opendcs.authentication.AuthSourceService;
import org.opendcs.spi.authentication.AuthSource;
import org.opendcs.spi.database.DatabaseProvider;

import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.util.DecodesSettings;
import ilex.util.AuthException;

public class DatabaseService
{
    private static ServiceLoader<DatabaseProvider> loader = ServiceLoader.load(DatabaseProvider.class);

    public static Database getDatabaseFor(String appName, DecodesSettings settings) throws DatabaseException
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

    public static Database getDatabaseFor(String appName, DecodesSettings settings, Properties credentials) throws DatabaseException
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
}
