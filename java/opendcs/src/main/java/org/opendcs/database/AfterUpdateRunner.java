package org.opendcs.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import javax.sql.DataSource;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Handle;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.spi.database.AfterUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Discovers and executes after-updaters, each in its own database transaction. */
public final class AfterUpdateRunner
{
    private static final Logger log = LoggerFactory.getLogger(AfterUpdateRunner.class);

    private AfterUpdateRunner()
    {
    }

    public static void run(DataSource dataSource) throws SQLException, OpenDcsDataException
    {
        run(Jdbi.create(dataSource));
    }

    public static void run(Jdbi jdbi) throws SQLException, OpenDcsDataException
    {
        List<AfterUpdate> updaters = new ArrayList<>();
        ServiceLoader.load(AfterUpdate.class).forEach(updaters::add);
        run(jdbi, updaters);
    }

    /** Package visible for testing order and transaction isolation. */
    static void run(Jdbi jdbi, List<AfterUpdate> updaters) throws SQLException, OpenDcsDataException
    {
        List<AfterUpdate> ordered = updaters.stream()
                .sorted(Comparator.comparingInt(AfterUpdate::order).thenComparing(AfterUpdate::name))
                .toList();
        Set<String> names = new HashSet<>();
        for (AfterUpdate updater : ordered)
        {
            if (!names.add(updater.name()))
            {
                throw new IllegalArgumentException("Duplicate after-updater name: " + updater.name());
            }
        }
        for (AfterUpdate updater : ordered)
        {
            log.info("Running after-update task '{}'", updater.name());
            try (Handle handle = jdbi.open())
            {
                DatabaseEngine engine = DatabaseEngine.from(
                        handle.getConnection().getMetaData().getDatabaseProductName());
                try (DataTransaction transaction = new JdbiTransaction(handle,
                        new TransactionContextImpl(null, Map.of(), engine)))
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
}
