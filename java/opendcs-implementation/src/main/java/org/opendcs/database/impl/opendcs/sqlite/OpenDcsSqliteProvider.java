package org.opendcs.database.impl.opendcs.sqlite;

import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.opendcs.database.DatabaseQuerySettings;
import org.opendcs.database.DatabaseService;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.spi.database.DatabaseProvider;

import decodes.db.DatabaseException;
import decodes.util.DecodesSettings;
import opendcs.opentsdb.OpenDcsDatabaseWrapper;
import opendcs.opentsdb.OpenDcsDbSettings;

public class OpenDcsSqliteProvider implements DatabaseProvider
{
    public static final String NAME = "OpenDCS-SQLite";

    @Override
    public boolean canCreate(DecodesSettings settings)
    {
        return NAME.equalsIgnoreCase(settings.editDatabaseType);
    }

    @Override
    public OpenDcsDatabase createDatabase(DataSource dataSource, DecodesSettings settings) throws DatabaseException
    {
        var dbSettings = DatabaseService.loadSettingsFromProperties(dataSource, new OpenDcsDbSettings());

        var allSettings = Map.of(
            DecodesSettings.class, settings,
            DatabaseQuerySettings.class, DatabaseQuerySettings.DEFAULT_SETTINGS,
            OpenDcsDbSettings.class, dbSettings
        );
        var db = new OpenDcsDatabaseWrapper(allSettings, null, null, dataSource);
        return db;
    }

    @Override
    public OpenDcsDatabase createDatabase(String module, DecodesSettings settings, Properties properties) throws DatabaseException
    {
        return null;
    }
    
}
