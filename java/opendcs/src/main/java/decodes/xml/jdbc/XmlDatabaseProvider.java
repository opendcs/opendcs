package decodes.xml.jdbc;

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
import decodes.xml.XmlDatabaseIO;
import opendcs.dai.DaiBase;

public class XmlDatabaseProvider implements DatabaseProvider
{
    @Override
    public boolean canCreate(DecodesSettings settings)
    {
        return settings.editDatabaseTypeCode == DecodesSettings.DB_XML;
    }

    @Override
    public OpenDcsDatabase createDatabase(String appName, DecodesSettings settings, Properties credentials)
            throws DatabaseException 
    {
        javax.sql.DataSource dataSource = new SimpleDataSource(settings.editDatabaseLocation, credentials);
        return createDatabase(dataSource, settings);
    }

    @Override
    public OpenDcsDatabase createDatabase(DataSource dataSource, DecodesSettings settings) throws DatabaseException
    {
        Database db = new Database(true);
        XmlDatabaseIO dbIo = new XmlDatabaseIO(dataSource, settings);
        db.setDbIo(dbIo);
        try
        {
            db.init(settings);
        }
        catch(DecodesException ex)
        {
            throw new DatabaseException("Unable to initialize decodes.", ex);
        }
        return new OpenDcsDatabase()
        {
            Database decodesDb = db;

            @Override
            public Database getDecodesDatabase()
            {
                return decodesDb;
            }

            @Override
            public TimeSeriesDb getTimeSeriesDb() {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("XML database doesn't support timeseries operations.");
            }

            @Override
            public <T extends DaiBase> T getDao(Class<T> dao) throws DatabaseException {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'getDao'");
            }
        };
    }
}
