package opendcs.opentsdb;

import java.util.Properties;

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
        javax.sql.DataSource dataSource = new SimpleDataSource(settings.editDatabaseLocation, credentials);
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

        OpenTsdb tsdb = new OpenTsdb(appName, dataSource, settings);

        return new OpenDcsDatabase() {
            Database decodesDb = db;
            TimeSeriesDb tsDb = tsdb;

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
        };
    }
}
