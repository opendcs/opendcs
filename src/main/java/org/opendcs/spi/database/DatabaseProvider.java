package org.opendcs.spi.database;

import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.util.DecodesSettings;

import java.util.Properties;

public interface DatabaseProvider
{
    public boolean canCreate(DecodesSettings settings);
    public Database createDatabase(String appName, DecodesSettings settings, Properties credentials) throws DatabaseException;
}
