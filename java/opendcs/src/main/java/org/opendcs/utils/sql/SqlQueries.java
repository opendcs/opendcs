package org.opendcs.utils.sql;

public final class SqlQueries
{
    private SqlQueries()
    {
        /* static methods only */
    }

    /**
     * Helper function to add limit and offset fields to queries.
     * 
     * Developer NOTE: initial review showed that this is standard syntax
     * supported by current versions of each database. Should we discover 
     * that not to be the case we can either expand this to include a DatabaseEngine
     * parameter or pass the buck to the implementation somehow.
     * @param limit
     * @param offset
     * @return
     */
    public static String addLimitOffset(int limit, int offset)
    {
        return (offset != -1 ? " offset :offset rows" : "") +
               (limit != -1 ? " fetch next :limit rows only": "");
    }
}
