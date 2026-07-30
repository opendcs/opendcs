package org.opendcs.database.sql;

public interface TableDefinition<E extends Enum<E> & TableColumnDefinition>
{
    String getTableName();
    String getTablePrefix();
    Enum<E> getIdColumn();
}
