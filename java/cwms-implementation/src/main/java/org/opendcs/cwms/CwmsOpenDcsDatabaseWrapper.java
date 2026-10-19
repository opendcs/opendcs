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
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.cwms.CwmsLocationLevelDAO;
import decodes.db.Database;
import decodes.tsdb.TimeSeriesDb;

public class CwmsOpenDcsDatabaseWrapper extends AbstractJdbiOpenDcsDatabaseWrapper
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

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
    // This method serves as a modification of the transaction returned to the user
    // it is not responsible for closing the transaction it created. the call is.
    @SuppressWarnings("resource")
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
        catch (OpenDcsDataRuntimeException ex)
        {
            // just log the error. If this happens it's likely from some rule that CWMS Has implemented.
            // Or the methods have gone away.
            log.atWarn()
               .setCause(ex)
               .log("Unable to disable CWMS Location Cache. " +
                    " You may need odd results if created and deleting locations.");
        }

        return tx;
    }
}
