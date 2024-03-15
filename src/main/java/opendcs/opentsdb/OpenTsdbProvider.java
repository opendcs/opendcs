package opendcs.opentsdb;

import java.util.Properties;

import org.opendcs.database.SimpleDataSource;
import org.opendcs.spi.database.DatabaseProvider;

import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.util.DecodesSettings;

public class OpenTsdbProvider implements DatabaseProvider
{

    @Override
    public boolean canCreate(DecodesSettings settings)
    {
        return settings.editDatabaseTypeCode == DecodesSettings.DB_OPENTSDB;
    }

    @Override
    public Database createDatabase(String appName, DecodesSettings settings, Properties credentials) throws DatabaseException
    {
        if (credentials.getProperty("user") == null && credentials.getProperty("username") != null)
        {
            // our AuthSource provides "username", the Postgres JDBC driver expects "user"
            credentials.setProperty("user", credentials.getProperty("username"));
        }
        javax.sql.DataSource dataSource = new SimpleDataSource(settings.editDatabaseLocation, credentials);
        OpenTsdb tsdb = new OpenTsdb(appName, dataSource, settings);
        return tsdb;
    }
}
