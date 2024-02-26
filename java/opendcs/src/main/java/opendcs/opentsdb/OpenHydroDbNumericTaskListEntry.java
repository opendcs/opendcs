package opendcs.opentsdb;

import java.util.Date;
import java.util.Objects;

import org.opendcs.tsdb.TaskListEntry;

import decodes.sql.DbKey;
import decodes.tsdb.TimeSeriesIdentifier;

public class OpenHydroDbNumericTaskListEntry implements TaskListEntry
{
    private final long recordNum;
    private final DbKey loadingApp;
    private final TimeSeriesIdentifier tsId;
    private final Date loadedTime;
    private final Date sampleTime;
    private final Date failTime;
    private final TsDataSource source;
    private final double value;
    private final int flags;
    private final boolean deleted;

    public OpenHydroDbNumericTaskListEntry(long recordNum, DbKey loadingApp, TimeSeriesIdentifier tsId, Date loadedTime,
                                           Date sampleTime, Date failTime, TsDataSource source, double value, int flags, boolean deleted)
    {
        this.recordNum = recordNum;
        this.loadingApp = Objects.requireNonNull(loadingApp, "A loading application is required.");
        this.tsId = Objects.requireNonNull(tsId, "A timeseries identifier is required.");
        this.loadedTime = Objects.requireNonNull(loadedTime, "Loaded time is required.");
        this.sampleTime = Objects.requireNonNull(sampleTime, "A sample time is required");
        this.failTime = failTime;
        this.source = source; //TODO: NOT NULL
        this.value = value; // TODO: can this be null? should for missing
        this.flags = flags;
        this.deleted = deleted;
    }


    @Override
    public long getRecordNum()
    {
        return this.recordNum;
    }

    @Override
    public DbKey getLoadingApp()
    {
        return this.loadingApp;
    }

    @Override
    public TimeSeriesIdentifier getTimeSeriesIdentifier()
    {
        return this.tsId;
    }

    @Override
    public Date getLoadedTime()
    {
        return this.loadedTime;
    }

    @Override
    public Date getSampleTime()
    {
        return this.sampleTime;
    }

    @Override
    public Date getFailTime()
    {
        return this.failTime;
    }

    @Override
    public boolean isDeleted()
    {
        return this.deleted;
    }
    
    public int getFlags()
    {
        return this.flags;
    }

    public TsDataSource getSource()
    {
        return this.source;
    }
}
