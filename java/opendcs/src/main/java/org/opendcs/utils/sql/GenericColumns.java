package org.opendcs.utils.sql;

import org.opendcs.database.sql.TableColumnDefinition;

/**
 * Enumeration of common column names. Individual Mappers
 * must duplicate these to their local enumeration, they should
 * do so by providing a constructor that takes a GenericColumns 
 * enum to handle the copy.
 */
public enum GenericColumns implements TableColumnDefinition
{
    CREATED_AT("created_at"),
    UPDATED_AT ("updated_at"),
    ID ("id"),
    NAME ("name"),
    CONFIG ("config"),
    PREFERENCES ("preferences"),
    EMAIL ("email"),
    SUBJECT ("subject"),
    DESCRIPTION ("description")

    ;

    private final String column;

    private GenericColumns(String column)
    {
        this.column = column;
    }

    @Override
    public String column()
    {
        return this.column;
    }
}
