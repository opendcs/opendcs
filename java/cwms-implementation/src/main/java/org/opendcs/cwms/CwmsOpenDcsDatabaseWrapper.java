package org.opendcs.cwms;

import java.util.Map;

import javax.sql.DataSource;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.SqlStatements;
import org.opendcs.database.AbstractJdbiOpenDcsDatabaseWrapper;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDataRuntimeException;
import org.opendcs.database.dai.SiteReferenceMetaData;
import org.opendcs.database.impl.cwms.jdbi.mapper.exception.CwmsExceptionMapper;
import org.opendcs.settings.api.OpenDcsSettings;

import decodes.cwms.CwmsLocationLevelDAO;
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
        final var wrapper = new DaoWrapper<>(() -> new CwmsLocationLevelDAO(this.getLegacyDatabase(TimeSeriesDb.class)
                                                                                .orElseThrow(() -> new OpenDcsDataRuntimeException("No Timeseries database available during initial setup."))));
        mapDao(SiteReferenceMetaData.class, wrapper);
        mapDao(CwmsLocationLevelDAO.class, wrapper);
        this.jdbi.define("numeric_date", false);
        this.jdbi.getConfig(SqlStatements.class)
                 .addExceptionHandler(new CwmsExceptionMapper());
    }

    @Override
    public DataTransaction newTransaction() throws OpenDcsDataException
    {
        var tx = super.newTransaction();
        var handle = tx.connection(Handle.class)
                       .orElseThrow(() -> new OpenDcsDataException("CWMS Impl isn't using Jdbi?"));
        try(var disableCaching = handle.createCall("""
                    begin
                     cwms_cache.disable(cwms_loc.g_location_id_cache);
                     cwms_cache.disable(cwms_loc.g_location_code_cache);
                    end;
                    """))
        {
            disableCaching.invoke();
        }

        return tx;
    }
}
