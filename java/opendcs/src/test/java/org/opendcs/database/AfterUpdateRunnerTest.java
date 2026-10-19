package org.opendcs.database;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.opendcs.algorithms.update.AlgorithmAfterUpdate;
import org.opendcs.spi.database.AfterUpdate;

class AfterUpdateRunnerTest
{
    @Test
    void discoversAlgorithmAfterUpdater()
    {
        List<AfterUpdate> updaters = new ArrayList<>();
        ServiceLoader.load(AfterUpdate.class).forEach(updaters::add);
        assertTrue(updaters.stream().anyMatch(updater -> updater instanceof AlgorithmAfterUpdate));
    }

    @Test
    void commitsCompletedUpdaterAndRollsBackFailingUpdaterInPositionOrder() throws Exception
    {
        SimpleDataSource source = source();
        List<String> ran = new ArrayList<>();
        AfterUpdate first = updater("first", 10, connection ->
        {
            assertFalse(connection.getAutoCommit());
            ran.add("first");
            insert(connection, "first");
        });
        AfterUpdate second = updater("second", 20, connection ->
        {
            ran.add("second");
            insert(connection, "second");
            throw new SQLException("abort this updater");
        });
        AfterUpdate third = updater("third", 30, connection -> ran.add("third"));
        assertThrows(SQLException.class,
                () -> AfterUpdateRunner.run(source, Arrays.asList(third, second, first)));
        assertEquals(Arrays.asList("first", "second"), ran);
        assertEquals(Collections.singletonList("first"), markers(source));
    }

    @Test
    void rejectsDuplicateNamesBeforeRunningAnyUpdater() throws Exception
    {
        SimpleDataSource source = source();
        AfterUpdate updater = updater("same", 10, connection -> insert(connection, "unexpected"));
        assertThrows(IllegalArgumentException.class,
                () -> AfterUpdateRunner.run(source, Arrays.asList(updater, updater)));
        assertTrue(markers(source).isEmpty());
    }

    @Test
    void rollsBackUncheckedFailures() throws Exception
    {
        SimpleDataSource source = source();
        AfterUpdate updater = updater("broken", 10, connection ->
        {
            insert(connection, "broken");
            throw new IllegalStateException("abort");
        });
        assertThrows(IllegalStateException.class,
                () -> AfterUpdateRunner.run(source, Collections.singletonList(updater)));
        assertTrue(markers(source).isEmpty());
    }

    private static SimpleDataSource source() throws SQLException
    {
        SimpleDataSource source = new SimpleDataSource(
                "jdbc:derby:memory:after_update_" + UUID.randomUUID() + ";create=true", "test", "test");
        try (Connection connection = source.getConnection();
             PreparedStatement statement = connection.prepareStatement("CREATE TABLE UPDATE_MARKER (NAME VARCHAR(20))"))
        {
            statement.executeUpdate();
        }
        return source;
    }

    private static List<String> markers(SimpleDataSource source) throws SQLException
    {
        List<String> result = new ArrayList<>();
        try (Connection connection = source.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT NAME FROM UPDATE_MARKER");
             ResultSet rows = statement.executeQuery())
        {
            while (rows.next()) result.add(rows.getString(1));
        }
        return result;
    }

    private static void insert(Connection connection, String name) throws SQLException
    {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO UPDATE_MARKER VALUES (?)"))
        {
            statement.setString(1, name);
            statement.executeUpdate();
        }
    }

    private static AfterUpdate updater(String name, int position, Action action)
    {
        return new AfterUpdate()
        {
            public String name() { return name; }
            public int position() { return position; }
            public void run(Connection connection) throws SQLException { action.run(connection); }
        };
    }

    @FunctionalInterface
    private interface Action
    {
        void run(Connection connection) throws SQLException;
    }
}
