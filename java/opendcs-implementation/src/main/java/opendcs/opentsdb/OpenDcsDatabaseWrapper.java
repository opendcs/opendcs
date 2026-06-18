package opendcs.opentsdb;

import java.util.Map;

import javax.sql.DataSource;

import org.opendcs.database.AbstractJdbiOpenDcsDatabaseWrapper;
import org.opendcs.settings.api.OpenDcsSettings;
import org.opendcs.utils.sql.SqlQueries;

import decodes.db.Database;
import decodes.tsdb.TimeSeriesDb;

public class OpenDcsDatabaseWrapper extends AbstractJdbiOpenDcsDatabaseWrapper
{

    public OpenDcsDatabaseWrapper(Map<Class<? extends OpenDcsSettings>, OpenDcsSettings> settings, Database decodesDb,
            TimeSeriesDb timeSeriesDb, DataSource dataSource)
    {
        super(settings, decodesDb, timeSeriesDb, dataSource);
    }

    @Override
    protected void initialSetup()
    {
        jdbi.define(SqlQueries.DUAL, SqlQueries.dualFor(this.dbEngine));
    }
}
