package decodes.cwms;

import java.util.Properties;

import javax.sql.DataSource;

import org.opendcs.spi.database.DatabaseProvider;

import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.tsdb.BadConnectException;
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
    public Database createDatabase(String appName, DecodesSettings settings, Properties credentials)
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
            DataSource ds = CwmsConnectionPool.getPoolFor(conInfo);
            CwmsTimeSeriesDb tsdb = new CwmsTimeSeriesDb(appName, ds, settings);
            return tsdb;
        }
        catch (BadConnectException ex)
        {
            throw new DatabaseException("Unable to connect to CWMS DB", ex);
        }
    }
    
}
