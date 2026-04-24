package org.opendcs.lrgs.http.dto;

public class DataSource
{
    private final String name;
    private final String type;

    public DataSource(String name, String type)
    {
        this.name = name;
        this.type = type;
    }

    public String getName()
    {
        return name;
    }

    public String getType()
    {
        return type;
    }
}
