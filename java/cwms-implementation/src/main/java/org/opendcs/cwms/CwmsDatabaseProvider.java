package org.opendcs.cwms;

import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.DatabaseQuerySettings;
import org.opendcs.database.DatabaseService;
import org.opendcs.spi.database.DatabaseProvider;

import com.google.auto.service.AutoService;

import decodes.cwms.CwmsConnectionInfo;
import decodes.cwms.CwmsConnectionPool;
import decodes.cwms.CwmsDatabaseQuerySettings;
import decodes.cwms.CwmsOpenDcsDatabaseWrapper;
import decodes.cwms.CwmsSqlDatabaseIO;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.sql.SqlDatabaseIO;
import decodes.tsdb.BadConnectException;
import decodes.util.DecodesException;
import decodes.util.DecodesSettings;
import usace.cwms.db.dao.util.connection.ConnectionLoginInfoImpl;

@AutoService(DatabaseProvider.class)
public class CwmsDatabaseProvider implements DatabaseProvider
{

    @Override
    public boolean canCreate(DecodesSettings settings)
    {
        return "CWMS".equals(settings.editDatabaseType)
            || "CWMS-Oracle".equals(settings.editDatabaseType);
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
            var cwmsSettings = DatabaseService.loadSettingsFromProperties(dataSource, new CwmsSettings());
            var allSettings = Map.of(
                DecodesSettings.class, settings,
                DatabaseQuerySettings.class, new CwmsDatabaseQuerySettings(),
                CwmsSettings.class, cwmsSettings
            );
            Database decodesDb = new Database(true);
            Database.setDb(decodesDb); // the CwmsSqlDatabaseIO constructor calls into the Database instance to verify things.
            decodesDb.setDbIo(new CwmsSqlDatabaseIO(dataSource, settings));
            CwmsTimeSeriesDb tsdb = new CwmsTimeSeriesDb(null, dataSource, settings);
            var db = new CwmsOpenDcsDatabaseWrapper(allSettings, decodesDb, tsdb, dataSource);
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
