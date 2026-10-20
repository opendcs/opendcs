package org.opendcs.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import decodes.util.DecodesSettings;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;
import org.opendcs.algorithms.update.AlgorithmAfterUpdate;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.settings.api.OpenDcsSettings;
import org.opendcs.spi.database.AfterUpdate;
import org.openide.util.Lookup;

class AfterUpdateRunnerTest
{
    @Test
    void discoversAlgorithmAfterUpdater()
    {
        assertTrue(Lookup.getDefault().lookupAll(AfterUpdate.class).stream()
                .anyMatch(updater -> updater instanceof AlgorithmAfterUpdate));
    }

    @Test
    void commitsCompletedUpdaterAndRollsBackFailingUpdater() throws Exception
    {
        String url = "jdbc:derby:memory:after_update_" + UUID.randomUUID() + ";create=true";
        Jdbi jdbi = Jdbi.create(url);
        jdbi.useHandle(handle -> handle.execute("CREATE TABLE UPDATE_MARKER (NAME VARCHAR(20))"));
        DecodesSettings settings = new DecodesSettings();
        AtomicInteger transactionCount = new AtomicInteger();
        OpenDcsDatabase database = database(jdbi, settings, transactionCount);
        List<String> ran = new ArrayList<>();
        AfterUpdate first = updater("first", tx ->
        {
            ran.add("first");
            assertSame(settings, tx.getContext().getSettings(DecodesSettings.class).orElseThrow());
            insert(tx, "first");
        });
        AfterUpdate second = updater("second", tx ->
        {
            ran.add("second");
            try (Connection connection = tx.connection(Connection.class).orElseThrow())
            {
                assertFalse(connection.getAutoCommit());
                insert(tx, "second");
            }
            throw new SQLException("abort this updater");
        });

        OpenDcsDataException failure = assertThrows(OpenDcsDataException.class,
                () -> AfterUpdateRunner.run(database, List.of(first, second)));
        assertTrue(failure.getCause() instanceof SQLException);
        assertEquals(2, transactionCount.get());
        assertEquals(List.of("first", "second"), ran);
        assertEquals(List.of("first"), jdbi.withHandle(handle ->
                handle.createQuery("SELECT NAME FROM UPDATE_MARKER").mapTo(String.class).list()));
    }

    private static OpenDcsDatabase database(Jdbi jdbi, DecodesSettings settings, AtomicInteger transactionCount)
    {
        return new OpenDcsDatabase()
        {
            @Override
            public DataTransaction newTransaction()
            {
                transactionCount.incrementAndGet();
                return new JdbiTransaction(jdbi.open(), new TransactionContextImpl(null,
                        Map.of(DecodesSettings.class, settings), DatabaseEngine.GENERIC_SQL));
            }

            @Override
            public DatabaseEngine getDatabaseEngine()
            {
                return DatabaseEngine.GENERIC_SQL;
            }

            @Override
            public <T extends OpenDcsSettings> Optional<T> getSettings(Class<T> settingsClass)
            {
                return settingsClass == DecodesSettings.class
                        ? Optional.of(settingsClass.cast(settings)) : Optional.empty();
            }

            @Override
            public <T extends OpenDcsDao> Optional<T> getDao(Class<T> daoClass)
            {
                return Optional.empty();
            }

            @Override
            public <T> Optional<T> getLegacyDatabase(Class<T> databaseClass)
            {
                return Optional.empty();
            }
        };
    }

    private static void insert(DataTransaction tx, String name) throws Exception
    {
        try (var connection = tx.connection(Connection.class).orElseThrow();
             var statement = connection.prepareStatement("INSERT INTO UPDATE_MARKER VALUES (?)"))
        {
            statement.setString(1, name);
            statement.executeUpdate();
        }
    }

    private static AfterUpdate updater(String name, Action action)
    {
        return new AfterUpdate()
        {
            @Override
            public String name()
            {
                return name;
            }

            @Override
            public void run(DataTransaction transaction) throws SQLException
            {
                try
                {
                    action.run(transaction);
                }
                catch (SQLException ex)
                {
                    throw ex;
                }
                catch (Exception ex)
                {
                    throw new SQLException(ex);
                }
            }
        };
    }

    @FunctionalInterface
    private interface Action
    {
        void run(DataTransaction transaction) throws Exception;
    }
}
