package org.opendcs.lrgs.http.dto;

import java.time.ZonedDateTime;

public class DcpMsg
{
    private final DataSource dataSource;
    private final ZonedDateTime retrievedTime;
    private final String msg;

    public DcpMsg(DataSource source, ZonedDateTime retrieveTime, String msg)
    {
        this.dataSource = source;
        this.retrievedTime = retrieveTime;
        this.msg = msg;
    }

    public DataSource getDataSource()
    {
        return dataSource;
    }

    public ZonedDateTime getRetrievedTime()
    {
        return retrievedTime;
    }

    public String getData()
    {
        return msg;
    }
}
