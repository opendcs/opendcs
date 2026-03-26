package org.opendcs.utils.sql;

import org.opendcs.database.api.DatabaseEngine;

public final class SqlQueries
{
    public static final String LIMIT_CLAUSE = "limit";
    public static final String WHERE_CLAUSE = "where";
    public static final String COLLATE_CLAUSE = "collate";

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

    /**
     * Retrieve the appropriate COLLATE CLAUSE for a given Database Type.
     * This returns the approprate COLLATE scheme for "ASCII" collation based
     * on the binary value of each character (byte).
     * @param dbEngine
     * @return
     */
    public static String collateClauseFor(DatabaseEngine dbEngine)
    {
        return dbEngine == DatabaseEngine.POSTGRES ? "COLLATE \"C\"" : "COLLATE BINARY";
    }
}
