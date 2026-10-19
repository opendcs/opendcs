package org.opendcs.spi.database;

import java.sql.Connection;
import java.sql.SQLException;

/** Work to run after schema migrations on an existing database. */
public interface AfterUpdate
{
    /** A stable name used for logging and duplicate detection. */
    String name();

    /** Lower positions run first. */
    default int position() { return 100; }

    /** Use only this connection; the caller commits or rolls back the transaction. */
    void run(Connection connection) throws SQLException;
}
