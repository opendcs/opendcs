package org.opendcs.utils.sql;

import org.opendcs.database.sql.TableColumnDefinition;

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
