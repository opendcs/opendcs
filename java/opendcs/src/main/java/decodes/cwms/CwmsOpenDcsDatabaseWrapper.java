package decodes.cwms;

import java.util.Map;

import javax.sql.DataSource;

import org.opendcs.database.AbstractJdbiOpenDcsDatabaseWrapper;
import org.opendcs.settings.api.OpenDcsSettings;

import decodes.db.Database;
import decodes.tsdb.TimeSeriesDb;

public class CwmsOpenDcsDatabaseWrapper extends AbstractJdbiOpenDcsDatabaseWrapper
{

    public CwmsOpenDcsDatabaseWrapper(Map<Class<? extends OpenDcsSettings>, OpenDcsSettings> settings,
            Database decodesDb, TimeSeriesDb timeSeriesDb, DataSource dataSource)
    {
        super(settings, decodesDb, timeSeriesDb, dataSource);
    }

    @Override
    protected void initialSetup()
    {
        /* do nothing at this time. */
    }
}
