package decodes.cwms;

import java.util.Properties;

import javax.sql.DataSource;

import org.opendcs.spi.database.DatabaseProvider;

import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.tsdb.BadConnectException;
import decodes.tsdb.TimeSeriesDb;
import decodes.util.DecodesSettings;
import ilex.util.Pair;
import usace.cwms.db.dao.util.connection.ConnectionLoginInfoImpl;

public class CwmsDatabaseProvider implements DatabaseProvider
{

    @Override
    public boolean canCreate(DecodesSettings settings)
    {
        return "CWMS".equals(settings.editDatabaseType);
    }

    @Override
    public Pair<Database,TimeSeriesDb> createDatabase(String appName, DecodesSettings settings, Properties credentials)
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
            Database db = new Database(true);
            DataSource ds = CwmsConnectionPool.getPoolFor(conInfo);
            Database.setDb(db); // the CwmsSqlDatabaseIO constructor calls into the Database instance to verify things.
            db.setDbIo(new CwmsSqlDatabaseIO(ds, settings));
            db.read();
            CwmsTimeSeriesDb tsdb = new CwmsTimeSeriesDb(appName, ds, settings);
            
            return Pair.of(db,tsdb);
        }
        catch (BadConnectException ex)
        {
            throw new DatabaseException("Unable to connect to CWMS DB", ex);
        }
    }
    
}
