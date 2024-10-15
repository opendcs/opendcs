package org.opendcs.spi.database;

import decodes.db.DatabaseException;
import decodes.util.DecodesSettings;

import java.util.Properties;

import org.opendcs.database.OpenDcsDatabase;

public interface DatabaseProvider
{
    public boolean canCreate(DecodesSettings settings);
    public OpenDcsDatabase createDatabase(String appName, DecodesSettings settings, Properties credentials) throws DatabaseException;
}
