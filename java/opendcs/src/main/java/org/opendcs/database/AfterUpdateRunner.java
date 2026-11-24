package org.opendcs.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import javax.sql.DataSource;

import org.opendcs.spi.database.AfterUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Discovers and executes after-updaters, each in its own database transaction. */
public final class AfterUpdateRunner
{
    private static final Logger log = LoggerFactory.getLogger(AfterUpdateRunner.class);

    private AfterUpdateRunner() { }

    public static void run(DataSource source) throws SQLException
    {
        List<AfterUpdate> updaters = new ArrayList<>();
        ServiceLoader.load(AfterUpdate.class).forEach(updaters::add);
        run(source, updaters);
    }

    /** Package visible for testing transaction isolation and ordering. */
    static void run(DataSource source, List<AfterUpdate> updaters) throws SQLException
    {
        Set<String> names = new HashSet<>();
        for (AfterUpdate updater : updaters)
        {
            if (!names.add(updater.name()))
            {
                throw new IllegalArgumentException("Duplicate after-updater name: " + updater.name());
            }
        }
        List<AfterUpdate> ordered = new ArrayList<>(updaters);
        ordered.sort(Comparator.comparingInt(AfterUpdate::position).thenComparing(AfterUpdate::name));
        for (AfterUpdate updater : ordered)
        {
            log.info("Running after-update task '{}'", updater.name());
            try (Connection connection = source.getConnection())
            {
                connection.setAutoCommit(false);
                try
                {
                    updater.run(connection);
                    connection.commit();
                }
                catch (SQLException | RuntimeException | Error ex)
                {
                    try
                    {
                        connection.rollback();
                    }
                    catch (SQLException rollbackFailure)
                    {
                        ex.addSuppressed(rollbackFailure);
                    }
                    throw ex;
                }
            }
        }
    }
}
