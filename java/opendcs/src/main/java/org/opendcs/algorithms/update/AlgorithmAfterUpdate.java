package org.opendcs.algorithms.update;

import java.sql.SQLException;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.spi.database.AfterUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openide.util.lookup.ServiceProvider;

/** Applies declared algorithm and computation renames after schema migration. */
@ServiceProvider(service = AfterUpdate.class, position = 100)
public final class AlgorithmAfterUpdate implements AfterUpdate
{
    private static final Logger log = LoggerFactory.getLogger(AlgorithmAfterUpdate.class);

    @Override
    public String name()
    {
        return "algorithm-sync";
    }

    @Override
    public void run(DataTransaction transaction) throws SQLException, OpenDcsDataException
    {
        for (AlgorithmUpdater.Summary summary : AlgorithmUpdater.synchronize(transaction, false))
        {
            if (summary.hasChanges())
            {
                log.info("Algorithm update: {}", summary);
            }
        }
    }
}
