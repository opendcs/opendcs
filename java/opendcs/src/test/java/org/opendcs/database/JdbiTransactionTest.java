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
import org.opendcs.database.api.OpenDcsDataException;
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
            tx.commit();
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
     * close() commits by default (matching Jdbi's commit-and-close happy path), so a DAO/resource
     * that does DELETE-then-INSERT in a single transaction must explicitly roll back if the INSERT
     * (or anything after it) throws before commit — relying on close() to undo it is no longer safe.
     */
    @Test
    void test_explicit_rollback_on_error_undoes_delete() throws Exception
    {
        // Arrange: create table and seed one row in auto-commit mode
        try (DataTransaction tx = new JdbiTransaction(jdbi.open(), dummyContext))
        {
            var h = tx.connection(Handle.class).get();
            h.createUpdate("create table delete_test(id integer, name varchar(200))").execute();
            tx.commit();
        }
        try (DataTransaction tx = new JdbiTransaction(jdbi.open().begin(), dummyContext))
        {
            var h = tx.connection(Handle.class).get();
            h.createUpdate("insert into delete_test(id,name) values (1, 'existing')").execute();
            tx.commit();
        }

        // Act: simulate the delete-then-failed-insert pattern, explicitly rolling back on failure,
        // the same shape as the REST resources' try/catch-and-rollback error path.
        assertThrows(Exception.class, this::deleteThenFailInsert);

        // Assert: the original row must still exist — DELETE was explicitly rolled back
        try (DataTransaction tx = new JdbiTransaction(jdbi.open().begin(), dummyContext))
        {
            var h = tx.connection(Handle.class).get();
            var count = h.createQuery("select count(*) from delete_test where id = 1")
                         .map(r -> r.getColumn(1, Integer.class))
                         .one();
            assertEquals(1, count, "DELETE must be rolled back when the caller explicitly rolls back on error");
        }
    }

    /**
     * DELETE succeeds, then the INSERT fails before commit is reached; the catch block rolls
     * back explicitly and rethrows, mirroring PresentationGroupDaoImpl.updateDataPresentations
     * and the REST resources' error path.
     */
    private void deleteThenFailInsert() throws Exception
    {
        try (DataTransaction tx = new JdbiTransaction(jdbi.open().begin(), dummyContext))
        {
            try
            {
                var h = tx.connection(Handle.class).get();
                h.createUpdate("delete from delete_test where id = 1").execute();
                h.createUpdate("insert into delete_test(id,name) values (999, null)").execute();
                throw new RuntimeException("Simulated failure during insert batch");
            }
            catch (RuntimeException ex)
            {
                tx.rollback();
                throw ex;
            }
        }
    }

    /**
     * When jdbiHandle.commit() fails, Jdbi's own LocalTransactionHandler already rolls back
     * internally before throwing TransactionException — our commit() just needs to catch and
     * propagate that failure as OpenDcsDataException rather than re-attempting a redundant
     * rollback (or worse, letting it escape unwrapped). A deferred unique constraint lets both
     * inserts succeed and only fail once checked at commit time, so this exercises a real
     * commit-time failure without also breaking the connection.
     */
    @Test
    void test_commit_failure_triggers_rollback_attempt() throws Exception
    {
        try (DataTransaction setup = new JdbiTransaction(jdbi.open(), dummyContext))
        {
            var setupHandle = setup.connection(Handle.class).get();
            // A deferred unique constraint lets both inserts succeed now and fail only when
            // checked at commit time, so the connection itself stays perfectly healthy —
            // isolating the "commit fails, rollback must still be attempted" behavior without
            // also breaking the connection rollback() would need to use.
            setupHandle.createUpdate("create table commit_fail_test(id integer, "
                    + "constraint commit_fail_uq unique(id) deferrable initially deferred)").execute();
            setup.commit();
        }

        JdbiTransaction tx = new JdbiTransaction(jdbi.open().begin(), dummyContext);
        var h = tx.connection(Handle.class).get();
        h.createUpdate("insert into commit_fail_test(id) values (1)").execute();
        h.createUpdate("insert into commit_fail_test(id) values (1)").execute();

        OpenDcsDataException thrown = assertThrows(OpenDcsDataException.class, tx::commit);
        assertTrue(thrown.getMessage().contains("commit"),
                "must report the commit failure, not the follow-up rollback outcome");

        try (DataTransaction check = new JdbiTransaction(jdbi.open().begin(), dummyContext))
        {
            var checkHandle = check.connection(Handle.class).get();
            var count = checkHandle.createQuery("select count(*) from commit_fail_test")
                    .map(r -> r.getColumn(1, Integer.class))
                    .one();
            assertEquals(0, count,
                    "a failed commit must leave the transaction's inserts rolled back, not partially committed");
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
            tx.commit();
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
