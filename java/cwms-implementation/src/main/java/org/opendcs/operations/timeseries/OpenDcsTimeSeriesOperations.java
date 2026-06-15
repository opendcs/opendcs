package org.opendcs.operations.timeseries;

import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.openide.util.lookup.ServiceProvider;

import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesIdentifier;

@ServiceProvider(service = TimeSeriesOperations.class)
public class OpenDcsTimeSeriesOperations implements TimeSeriesOperations
{

    @Override
    public TimeSeriesIdentifier transformUniqueString(TimeSeriesIdentifier tsidRet, DbCompParm parm)
    {
        throw new UnsupportedOperationException("Not yet implemented method 'transformTsidByCompParm'");
    }

    @Override
    public Optional<TimeSeriesIdentifier> transformTsidByCompParm(DataTransaction tx, TimeSeriesIdentifier tsId,
            DbCompParm parm, boolean createTS, boolean fillInParm, String timeSeriesDisplayName)
            throws OpenDcsDataException, NoSuchObjectException, BadTimeSeriesException
    {
       throw new UnsupportedOperationException("Not yet implemented method 'transformTsidByCompParm'");
    }

    @Override
    public Optional<TimeSeriesIdentifier> expandSDI(DataTransaction tx, DbCompParm parm) throws OpenDcsDataException
    {
        throw new UnsupportedOperationException("Not yet implemented method 'transformTsidByCompParm'");
    }
    

    @Override
    public TimeSeriesIdentifier modifyTSID(TimeSeriesIdentifier tsIdIn)
    {
        return null;
    }
}
