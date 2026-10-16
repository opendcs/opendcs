package org.opendcs.spi.database;

import java.sql.SQLException;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;

/** Work to run after schema migrations on an existing database. */
public interface AfterUpdate
{
    /** A stable name for logging and future command line selection. */
    String name();

    /** Lower values run first. Names break ties so discovery order does not matter. */
    default int order()
    {
        return 0;
    }

    /** Use only this transaction for database work; the caller commits or rolls it back. */
    void run(DataTransaction transaction) throws SQLException, OpenDcsDataException;
}
