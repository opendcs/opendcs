package org.opendcs.utils.sql;

public final class SqlErrorMessages
{
    public static final String ZDT_MAPPER_NOT_FOUND = "No Mapper for ZonedDateTime was found.";
    public static final String DBKEY_MAPPER_NOT_FOUND = "No Mapper for DbKey was found.";
    public static final String CONFIG_MAPPER_NOT_FOUND = "No Mapper for JSON Config was found.";

    public static final String NO_JDBI_HANDLE = "No Jdbi Handle available.";
    
    private SqlErrorMessages()
    {
        // only holds constants or static helpers
    }
}
