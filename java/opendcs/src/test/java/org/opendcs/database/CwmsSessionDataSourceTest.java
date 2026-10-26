package org.opendcs.database;

import static org.junit.jupiter.api.Assertions.*;

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opendcs.spi.database.AfterUpdate;

import decodes.launcher.Profile;
import usace.cwms.db.dao.util.connection.ConnectionLoginInfo;

class CwmsSessionDataSourceTest
{
    @TempDir
    Path tempDir;

    @Test
    void cwmsUpdatesGetSessionInitializationButSchemaMigrationsDoNot() throws Exception
    {
        Profile profile = profile("CWMS");
        DataSource migrationSource = ManageDatabaseApp.getDataSourceFromProfileAndUserInfo(
                profile, null, "test", "test");
        DataSource updateSource = ManageDatabaseApp.getDataSourceForAfterUpdates(
                profile, null, "test", "test");
        assertEquals(SimpleDataSource.class, migrationSource.getClass());
        assertTrue(updateSource instanceof CwmsSessionDataSource);
    }

    @Test
    void nonCwmsUpdatesKeepThePlainJdbcConnection() throws Exception
    {
        DataSource source = ManageDatabaseApp.getDataSourceForAfterUpdates(
                profile("OPENTSDB"), null, "test", "test");
        assertEquals(SimpleDataSource.class, source.getClass());
        try (Connection connection = source.getConnection())
        {
            assertTrue(connection.isValid(1));
        }
    }

    @Test
    void initializesEveryConnectionWithItsOwnCredentialsAndConfiguredOffice() throws Exception
    {
        RecordingSource source = new RecordingSource();
        try (Connection connection = source.getConnection())
        {
            assertSame(connection, source.lastConnection);
            assertTrue(connection.getAutoCommit());
        }
        try (Connection connection = source.getConnection("other", "other"))
        {
            assertSame(connection, source.lastConnection);
        }
        assertEquals(2, source.logins.size());
        assertEquals("test", source.logins.get(0).getUser());
        assertEquals("other", source.logins.get(1).getUser());
        assertEquals("other", source.logins.get(1).getPassword());
        assertEquals("NWO", source.logins.get(0).getUserOfficeId());
        assertEquals("NWO", source.logins.get(1).getUserOfficeId());
    }

    @Test
    void failedSessionInitializationClosesTheConnectionAndPreservesTheError() throws Exception
    {
        RecordingSource source = new RecordingSource();
        source.failure = new SQLException("Cannot establish office context");
        assertSame(source.failure, assertThrows(SQLException.class, source::getConnection));
        assertTrue(source.lastConnection.isClosed());
    }

    @Test
    void fatalInitializationFailureStillClosesTheConnection() throws Exception
    {
        RecordingSource source = new RecordingSource();
        source.fatalFailure = new AssertionError("Initialization failed");
        assertSame(source.fatalFailure, assertThrows(AssertionError.class, source::getConnection));
        assertTrue(source.lastConnection.isClosed());
    }

    @Test
    void afterUpdateRunnerInitializesTheSessionBeforeStartingEachUpdaterTransaction() throws Exception
    {
        RecordingSource source = new RecordingSource();
        AfterUpdate first = updater("first", source);
        AfterUpdate second = updater("second", source);
        AfterUpdateRunner.run(source, Arrays.asList(first, second));
        assertEquals(2, source.logins.size());
        assertTrue(source.lastConnection.isClosed());
    }

    private static AfterUpdate updater(String name, RecordingSource source)
    {
        return new AfterUpdate()
        {
            public String name() { return name; }

            public void run(Connection connection) throws SQLException
            {
                assertSame(source.lastConnection, connection);
                assertFalse(connection.getAutoCommit());
                assertFalse(source.logins.isEmpty());
            }
        };
    }

    private Profile profile(String databaseType) throws Exception
    {
        Properties properties = new Properties();
        properties.setProperty("editDatabaseType", databaseType);
        properties.setProperty("editDatabaseLocation", url());
        properties.setProperty("CwmsOfficeId", "NWO");
        Path file = tempDir.resolve(databaseType + ".properties");
        try (Writer writer = Files.newBufferedWriter(file))
        {
            properties.store(writer, "Test profile");
        }
        return Profile.getProfile(file.toFile());
    }

    private static String url()
    {
        return "jdbc:derby:memory:cwms_session_" + UUID.randomUUID() + ";create=true";
    }

    private static class RecordingSource extends CwmsSessionDataSource
    {
        final List<ConnectionLoginInfo> logins = new ArrayList<>();
        Connection lastConnection;
        SQLException failure;
        AssertionError fatalFailure;

        RecordingSource()
        {
            super(url(), "test", "test", "NWO");
        }

        @Override
        protected void initializeSession(Connection connection, ConnectionLoginInfo loginInfo) throws SQLException
        {
            assertTrue(connection.getAutoCommit());
            lastConnection = connection;
            logins.add(loginInfo);
            if (failure != null) throw failure;
            if (fatalFailure != null) throw fatalFailure;
        }
    }
}
