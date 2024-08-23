package org.opendcs.spi.database;

import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.tsdb.TimeSeriesDb;
import decodes.util.DecodesSettings;
import ilex.util.Pair;

import java.util.Properties;

public interface DatabaseProvider
{
    public boolean canCreate(DecodesSettings settings);
    public Pair<Database,TimeSeriesDb> createDatabase(String appName, DecodesSettings settings, Properties credentials) throws DatabaseException;
}
