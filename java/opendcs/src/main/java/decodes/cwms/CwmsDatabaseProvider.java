package decodes.cwms;

import java.util.Properties;

import javax.sql.DataSource;

import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.SimpleOpenDcsDatabaseWrapper;
import org.opendcs.spi.database.DatabaseProvider;

import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.sql.SqlDatabaseIO;
import decodes.tsdb.BadConnectException;
import decodes.util.DecodesException;
import decodes.util.DecodesSettings;
import usace.cwms.db.dao.util.connection.ConnectionLoginInfoImpl;

public class CwmsDatabaseProvider implements DatabaseProvider
{

    @Override
    public boolean canCreate(DecodesSettings settings)
    {
        return "CWMS".equals(settings.editDatabaseType);
    }

    @Override
    public OpenDcsDatabase createDatabase(String appName, DecodesSettings settings, Properties credentials)
            throws DatabaseException
    {
        try
        {
            CwmsConnectionInfo conInfo = new CwmsConnectionInfo();
            ConnectionLoginInfoImpl info = new ConnectionLoginInfoImpl(settings.editDatabaseLocation, 
                                                                credentials.getProperty("user", credentials.getProperty(("username"))),
                                                                credentials.getProperty("password"),
                                                                settings.CwmsOfficeId);
            conInfo.setLoginInfo(info);
          
            DataSource dataSource = CwmsConnectionPool.getPoolFor(conInfo);
            return createDatabase(dataSource, settings);
        }
        catch (BadConnectException ex)
        {
            throw new DatabaseException("Unable to connect to CWMS DB", ex);
        }
    }

    @Override
    public OpenDcsDatabase createDatabase(DataSource dataSource, DecodesSettings settings) throws DatabaseException
    {
        try
        {
            Database decodesDb = new Database(true);
            Database.setDb(decodesDb); // the CwmsSqlDatabaseIO constructor calls into the Database instance to verify things.
            decodesDb.setDbIo(new CwmsSqlDatabaseIO(dataSource, settings));
            CwmsTimeSeriesDb tsdb = new CwmsTimeSeriesDb(null, dataSource, settings);
            var db = new SimpleOpenDcsDatabaseWrapper(settings, decodesDb, tsdb, dataSource);
            ((SqlDatabaseIO)decodesDb.getDbIo()).setDcsDatabase(db);
            decodesDb.init(settings);
            tsdb.setDcsDatabase(db);
            return db;
        }
        catch (DecodesException ex)
        {
            throw new DatabaseException("Unable to perform minimal decodes initialization.", ex);
        }
    }
}
