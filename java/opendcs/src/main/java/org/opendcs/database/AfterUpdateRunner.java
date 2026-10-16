package org.opendcs.database;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.spi.database.AfterUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openide.util.Lookup;

/** Discovers and executes after-updaters, each in its own database transaction. */
public final class AfterUpdateRunner
{
    private static final Logger log = LoggerFactory.getLogger(AfterUpdateRunner.class);

    private AfterUpdateRunner()
    {
    }

    public static void run(OpenDcsDatabase database) throws SQLException, OpenDcsDataException
    {
        run(database, List.copyOf(Lookup.getDefault().lookupAll(AfterUpdate.class)));
    }

    /** Package visible for testing transaction isolation. */
    static void run(OpenDcsDatabase database, List<AfterUpdate> updaters) throws SQLException, OpenDcsDataException
    {
        Set<String> names = new HashSet<>();
        for (AfterUpdate updater : updaters)
        {
            if (!names.add(updater.name()))
            {
                throw new IllegalArgumentException("Duplicate after-updater name: " + updater.name());
            }
        }
        for (AfterUpdate updater : updaters)
        {
            log.info("Running after-update task '{}'", updater.name());
            try (DataTransaction transaction = database.newTransaction())
            {
                try
                {
                    updater.run(transaction);
                    transaction.commit();
                }
                catch (SQLException | OpenDcsDataException | RuntimeException ex)
                {
                    try
                    {
                        transaction.rollback();
                    }
                    catch (OpenDcsDataException rollbackFailure)
                    {
                        ex.addSuppressed(rollbackFailure);
                    }
                    throw ex;
                }
            }
        }
    }
}
