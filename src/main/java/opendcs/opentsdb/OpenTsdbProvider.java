package opendcs.opentsdb;

import java.util.Properties;

import javax.sql.DataSource;

import org.opendcs.database.OpenDcsDatabase;
import org.opendcs.database.SimpleDataSource;
import org.opendcs.spi.database.DatabaseProvider;

import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.tsdb.TimeSeriesDb;
import decodes.util.DecodesException;
import decodes.util.DecodesSettings;
import ilex.util.Pair;
import opendcs.dai.DaiBase;

public class OpenTsdbProvider implements DatabaseProvider
{
    private String appName = null;

    @Override
    public boolean canCreate(DecodesSettings settings)
    {
        return settings.editDatabaseTypeCode == DecodesSettings.DB_OPENTSDB;
    }

    @Override
    public OpenDcsDatabase createDatabase(String appName, DecodesSettings settings, Properties credentials) throws DatabaseException
    {
        if (credentials.getProperty("user") == null && credentials.getProperty("username") != null)
        {
            // our AuthSource provides "username", the Postgres JDBC driver expects "user"
            credentials.setProperty("user", credentials.getProperty("username"));
        }
        this.appName = appName;
        javax.sql.DataSource dataSource = new SimpleDataSource(settings.editDatabaseLocation, credentials);
        return createDatabase(dataSource, settings);
    }


    private Database getDecodesDatabase(javax.sql.DataSource dataSource, DecodesSettings settings) throws DatabaseException
    {
        Database db = new Database(true);            
        db.setDbIo(new OpenTsdbSqlDbIO(dataSource, settings));
        Database.setDb(db);
        try
        {
            db.init(settings);
        }
        catch(DecodesException ex)
        {
            throw new DatabaseException("Unable to initialize decodes.", ex);
        }
        return db;
    }

    @Override
    public OpenDcsDatabase createDatabase(DataSource dataSource, DecodesSettings settings) throws DatabaseException
    {
        Database decodesDb = getDecodesDatabase(dataSource, settings);
        OpenTsdb tsDb = new OpenTsdb(appName, dataSource, settings);
        return new OpenDcsDatabaseImpl(decodesDb, tsDb);
    }

    public static class OpenDcsDatabaseImpl implements OpenDcsDatabase
    {
        final Database decodesDb;
        final TimeSeriesDb tsDb;

        private OpenDcsDatabaseImpl(Database decodesDb, TimeSeriesDb tsDb)
        {
            this.decodesDb = decodesDb;
            this.tsDb = tsDb;
        }

        @Override
        public Database getDecodesDatabase()
        {
            return decodesDb;
        }

        @Override
        public TimeSeriesDb getTimeSeriesDb()
        {
            return tsDb;
        }

        @Override
        public <T extends DaiBase> T getDao(Class<T> dao) throws DatabaseException
        {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getDao'");
        }
    }
}
