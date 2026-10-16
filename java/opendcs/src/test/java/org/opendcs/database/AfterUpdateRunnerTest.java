package org.opendcs.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.UUID;

import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.spi.database.AfterUpdate;

class AfterUpdateRunnerTest
{
    @Test
    void discoversAlgorithmAfterUpdater()
    {
        assertTrue(ServiceLoader.load(AfterUpdate.class).stream()
                .anyMatch(provider -> provider.type().getName().equals(
                        "org.opendcs.algorithms.update.AlgorithmAfterUpdate")));
    }

    @Test
    void runsInOrderAndRollsBackOnlyTheFailingUpdater() throws Exception
    {
        String url = "jdbc:derby:memory:after_update_" + UUID.randomUUID() + ";create=true";
        Jdbi jdbi = Jdbi.create(url);
        jdbi.useHandle(handle -> handle.execute("CREATE TABLE UPDATE_MARKER (NAME VARCHAR(20))"));
        List<String> ran = new ArrayList<>();
        AfterUpdate first = updater("first", 10, tx ->
        {
            ran.add("first");
            insert(tx, "first");
        });
        AfterUpdate second = updater("second", 20, tx ->
        {
            ran.add("second");
            try (Connection connection = tx.connection(Connection.class).orElseThrow())
            {
                assertFalse(connection.getAutoCommit());
                insert(tx, "second");
            }
            throw new SQLException("abort this updater");
        });

        assertThrows(SQLException.class, () -> AfterUpdateRunner.run(jdbi, List.of(second, first)));
        assertEquals(List.of("first", "second"), ran);
        assertEquals(List.of("first"), jdbi.withHandle(handle ->
                handle.createQuery("SELECT NAME FROM UPDATE_MARKER").mapTo(String.class).list()));
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

    private static AfterUpdate updater(String name, int order, Action action)
    {
        return new AfterUpdate()
        {
            @Override
            public String name()
            {
                return name;
            }

            @Override
            public int order()
            {
                return order;
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
