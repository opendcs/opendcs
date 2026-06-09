package org.opendcs.database.sql;

/**
 * Used primarily with Column enums to provide a standard interface to return
 * information about a column. At this time we are only dealing with the column name.
 * Future work will likely start including additional informaiton, such as types.
 */
public interface TableColumnDefinition
{
    /**
     * Returns the column name.
     * @return
     */
    String column();
}
