package decodes.xml.jdbc;

import java.util.Properties;

import org.opendcs.database.SimpleDataSource;
import org.opendcs.spi.database.DatabaseProvider;

import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.tsdb.TimeSeriesDb;
import decodes.util.DecodesSettings;
import decodes.xml.XmlDatabaseIO;
import ilex.util.Pair;

public class XmlDatabaseProvider implements DatabaseProvider
{
    @Override
    public boolean canCreate(DecodesSettings settings)
    {
        return settings.editDatabaseTypeCode == DecodesSettings.DB_XML;
    }

    @Override
    public Pair<Database,TimeSeriesDb> createDatabase(String appName, DecodesSettings settings, Properties credentials)
            throws DatabaseException 
    {
        Database db = new Database(true);

        javax.sql.DataSource dataSource = new SimpleDataSource(settings.editDatabaseLocation, credentials);
        XmlDatabaseIO dbIo = new XmlDatabaseIO(dataSource, settings);
        db.setDbIo(dbIo);
        db.read();
        return Pair.of(db, null);
    }
}
