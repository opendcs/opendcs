package org.opendcs.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.Generator;
import org.opendcs.database.api.TransactionContext;
import org.opendcs.settings.api.OpenDcsSettings;

class JdbiTransactionTest
{
    Jdbi jdbi;
    final TransactionContext dummyContext = new TransactionContext()
    {

        @Override
        public <T extends Generator> Optional<T> getGenerator(Class<T> generatorClass)
        {
            throw new UnsupportedOperationException("Unimplemented method 'getGenerator'");
        }

        @Override
        public <T extends OpenDcsSettings> Optional<T> getSettings(Class<T> settingsClass)
        {
            throw new UnsupportedOperationException("Unimplemented method 'getSettings'");
        }

        @Override
        public DatabaseEngine getDatabaseEngine()
        {
            throw new UnsupportedOperationException("Unimplemented method 'getDatabaseEngine'");
        }
    };

    @BeforeEach
    void create_db()
    {
        jdbi = Jdbi.create("jdbc:derby:memory:db;create=true");
    }

    @AfterEach
    void drop_db() throws Exception
    {
        try
        {
            DriverManager.getConnection("jdbc:derby:memory:db;drop=true");
        }
        catch(SQLException err)
        {
            if (!"08006".equals(err.getSQLState()))
            {
                throw err;
            }
        }
    }

    @Test
    void test_jdbi_transaction_returns_empty_on_not_supported() throws Exception
    {
        try (DataTransaction tx = new JdbiTransaction(jdbi.open(), dummyContext))
        {
            // Is this somewhat silly, using an InputStream as a "connection" type... yes.
            // However, with DataTransaction we are attempting to "future proof" a bit and
            // provide the flexibility for different sources of data they likely don't
            // follow the javax.sql.Connection semantics.
            assertFalse(tx.connection(InputStream.class).isPresent());
        }
    }

    @Test
    void test_jdbi_transaction_operations() throws Exception
    {
        try (DataTransaction tx = new JdbiTransaction(jdbi.open(), dummyContext))
        {
            var h = tx.connection(Handle.class).get();
            h.createUpdate("create table simple_table(id integer, name varchar(200))").execute();
        }

        try (DataTransaction tx = new JdbiTransaction(jdbi.open().begin(), dummyContext))
        {
            var h = tx.connection(Handle.class).get();
            h.createUpdate("insert into simple_table(id,name) values (:id, :name)")
             .bind("id", 1)
             .bind("name", "test")
             .execute();
            tx.commit();
        }

        try (DataTransaction tx = new JdbiTransaction(jdbi.open().begin(), dummyContext))
        {
            var h = tx.connection(Handle.class).get();
            h.createUpdate("delete from simple_table where id =:id")
             .bind("id", 1)
             .execute();
            tx.rollback();
        }

        try (DataTransaction tx = new JdbiTransaction(jdbi.open().begin(), dummyContext))
        {
            var h = tx.connection(Handle.class).get();
            var result = h.createQuery("select id from simple_table")
                          .map(r -> r.getColumn(1, Integer.class))
                          .findOne();
            assertTrue(result.isPresent());
            assertEquals(1, result.get());
        }
    }

    /**
     * A DAO that does DELETE-then-INSERT in a single transaction should roll back the DELETE
     * if the INSERT (or anything after it) throws before commit. Closing the transaction
     * without committing must NOT permanently persist the DELETE.
     */
    @Test
    void test_close_without_commit_rolls_back_delete() throws Exception
    {
        // Arrange: create table and seed one row in auto-commit mode
        try (DataTransaction tx = new JdbiTransaction(jdbi.open(), dummyContext))
        {
            var h = tx.connection(Handle.class).get();
            h.createUpdate("create table delete_test(id integer, name varchar(200))").execute();
        }
        try (DataTransaction tx = new JdbiTransaction(jdbi.open().begin(), dummyContext))
        {
            var h = tx.connection(Handle.class).get();
            h.createUpdate("insert into delete_test(id,name) values (1, 'existing')").execute();
            tx.commit();
        }

        // Act: simulate the delete-then-failed-insert pattern without commit
        assertThrows(Exception.class, () ->
        {
            try (DataTransaction tx = new JdbiTransaction(jdbi.open().begin(), dummyContext))
            {
                var h = tx.connection(Handle.class).get();
                // Step 1: DELETE (succeeds) — mirrors PresentationGroupDaoImpl.updateDataPresentations
                h.createUpdate("delete from delete_test where id = 1").execute();
                // Step 2: INSERT throws — mirrors a bad-data / constraint failure during re-insert
                h.createUpdate("insert into delete_test(id,name) values (999, null)").execute(); // name is varchar, null OK
                // Simulate a failure that would occur before tx.commit() is called
                throw new RuntimeException("Simulated failure during insert batch");
                // close() is called by try-with-resources here — MUST rollback, not commit
            }
        });

        // Assert: the original row must still exist — DELETE was rolled back with the transaction
        try (DataTransaction tx = new JdbiTransaction(jdbi.open().begin(), dummyContext))
        {
            var h = tx.connection(Handle.class).get();
            var count = h.createQuery("select count(*) from delete_test where id = 1")
                         .map(r -> r.getColumn(1, Integer.class))
                         .one();
            assertEquals(1, count, "DELETE must be rolled back when transaction is closed without commit");
        }
    }

    /**
     * Companion test: confirms that close() after a successful commit() does NOT roll back
     * the already-committed work (commit-then-close is the correct success path).
     */
    @Test
    void test_explicit_commit_then_close_persists_changes() throws Exception
    {
        // Arrange
        try (DataTransaction tx = new JdbiTransaction(jdbi.open(), dummyContext))
        {
            var h = tx.connection(Handle.class).get();
            h.createUpdate("create table commit_test(id integer, name varchar(200))").execute();
        }

        // Act: insert with explicit commit, then close
        try (DataTransaction tx = new JdbiTransaction(jdbi.open().begin(), dummyContext))
        {
            var h = tx.connection(Handle.class).get();
            h.createUpdate("insert into commit_test(id,name) values (42, 'saved')").execute();
            tx.commit(); // explicit commit — close() should NOT roll this back
        }

        // Assert: data is present
        try (DataTransaction tx = new JdbiTransaction(jdbi.open().begin(), dummyContext))
        {
            var h = tx.connection(Handle.class).get();
            var result = h.createQuery("select name from commit_test where id = 42")
                          .map(r -> r.getColumn(1, String.class))
                          .findOne();
            assertTrue(result.isPresent(), "Committed data must survive close()");
            assertEquals("saved", result.get());
        }
    }
}
