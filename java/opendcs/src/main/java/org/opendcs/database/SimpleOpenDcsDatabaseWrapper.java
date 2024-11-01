package org.opendcs.database;

import java.util.Objects;
import java.util.Optional;

import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDatabase;

import decodes.db.Database;
import decodes.tsdb.TimeSeriesDb;
import decodes.util.DecodesSettings;

public class SimpleOpenDcsDatabaseWrapper implements OpenDcsDatabase
{
    private final DecodesSettings settings;
    private final Database decodesDb;
    private final TimeSeriesDb timeSeriesDb;


    public SimpleOpenDcsDatabaseWrapper(DecodesSettings settings, Database decodesDb, TimeSeriesDb timeSeriesDb)
    {
        this.settings = settings;
        this.decodesDb = decodesDb;
        this.timeSeriesDb = timeSeriesDb;
    }

    @SuppressWarnings("unchecked") // class is checked before casting
    @Override
    public <T> Optional<T> getLegacyDatabase(Class<T> legacyDatabaseType)
    {
        if (Database.class.equals(legacyDatabaseType))
        {
            return Optional.of((T)decodesDb);
        }
        else if (TimeSeriesDb.class.isAssignableFrom(legacyDatabaseType))
        {
            // The XML database does not currently have timeseries.
            return Optional.ofNullable((T)timeSeriesDb);
        }
        else
        {
            return Optional.empty();
        }
    }

    @Override
    public <T extends OpenDcsDao> Optional<T> getDao(Class<T> dao)
    {
        //TODO: likely implementation, lookup dynamically for functions that the return the given DAO in the implementation
        // caching that knowledge as appropriate.
        return Optional.empty();
    }
    
}
