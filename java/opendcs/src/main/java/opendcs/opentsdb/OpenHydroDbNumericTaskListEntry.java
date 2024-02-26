package opendcs.opentsdb;

import java.util.Date;

import org.opendcs.tsdb.TaskListEntry;

import decodes.sql.DbKey;
import decodes.tsdb.TimeSeriesIdentifier;

public class OpenHydroDbNumericTaskListEntry implements TaskListEntry
{
    long recordNum;
    DbKey loadingApp;
    TimeSeriesIdentifier tsid;
    Date loadedTime;
    Date sampleTime;
    Date failTime;
    DbKey source;
    double value;
    int flags;

    @Override
    public long getRecordNum()
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRecordNum'");
    }

    @Override
    public DbKey getLoadingApp()
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getLoadingApp'");
    }

    @Override
    public TimeSeriesIdentifier getTimeSeriesIdentifier()
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTimeSeriesIdentifier'");
    }

    @Override
    public Date getLoadedTime()
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getLoadedTime'");
    }

    @Override
    public Date getSampleTime()
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSampleTime'");
    }

    @Override
    public Date getFailTime()
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getFailTime'");
    }

    @Override
    public boolean isDeleted()
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isDeleted'");
    }
    
    public int flags()
    {
        return -1;
    }
}
