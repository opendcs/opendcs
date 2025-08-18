package org.opendcs.tsdb;

import java.util.Date;

import decodes.sql.DbKey;
import decodes.tsdb.TimeSeriesIdentifier;

public class BadTaskListEntry implements TaskListEntry
{
    private final long recordNum;
    private final DbKey loadingApp;
    private final TimeSeriesIdentifier tsId;
    private final Date loadedTime;
    private final Date sampleTime;
    private final Date failTime;

    public BadTaskListEntry(long recordNum, DbKey loadingApp, TimeSeriesIdentifier tsId, Date loadedTime,
                            Date sampleTime, Date failTime)
    {
        this.recordNum = recordNum;
        this.loadingApp = loadingApp;
        this.tsId = tsId;
        this.loadedTime = loadedTime;
        this.sampleTime = sampleTime;
        this.failTime = failTime;

    }

    public BadTaskListEntry(final TaskListEntry other)
    {
        this.recordNum = other.getRecordNum();
        this.loadingApp = other.getLoadingApp();
        this.tsId = other.getTimeSeriesIdentifier();
        this.loadedTime = other.getLoadedTime();
        this.sampleTime = other.getSampleTime();
        this.failTime = other.getFailTime();
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
        return false;
    }

    @Override
    public boolean valueWasNull()
    {
        return false;
    }
}
