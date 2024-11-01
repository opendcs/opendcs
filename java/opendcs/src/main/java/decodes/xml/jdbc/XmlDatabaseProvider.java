package decodes.xml.jdbc;

import java.util.Properties;

import javax.sql.DataSource;

import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.SimpleDataSource;
import org.opendcs.database.SimpleOpenDcsDatabaseWrapper;
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
        return new SimpleOpenDcsDatabaseWrapper(settings, db, null);
    }
}
