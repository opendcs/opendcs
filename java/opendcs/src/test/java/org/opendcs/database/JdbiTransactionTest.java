package org.opendcs.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.SqlStatements;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.Generator;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDataRuntimeException;
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
        // Idea is to move this to the Wrapper Implementations, default to this
        // allow/exepect implementations to refine. Deriving things like
        // constraint errors from Runtime exceptoin and passing them on.
        // handlers are attempted in reverse order of operation: https://jdbi.org/#_exception_handling
        // Does need to exist here due to not using the wrapper.
        // I suggest we create at least a default Handler per target database engine
        // that handles those elements with obvious error codes (like the actual ORA- PG- errors that are
        // for say, foreign key constraints, etc)
        jdbi.getConfig(SqlStatements.class).addExceptionHandler((ex, ctx) ->
        {
            throw new OpenDcsDataRuntimeException("Error during query operation", ex);
        });
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
            h.createUpdate("create table simple_table(id integer primary key, name varchar(200))").execute();
            h.createUpdate("create table child_table(parent_id integer references simple_table(id), description varchar(100))").execute();
        }

        // simple update
        try (DataTransaction tx = new JdbiTransaction(jdbi.open().begin(), dummyContext))
        {
            var h = tx.connection(Handle.class).get();
            h.createUpdate("insert into simple_table(id,name) values (:id, :name)")
             .bind("id", 1)
             .bind("name", "test")
             .execute();
        }

        // rollback
        try (DataTransaction tx = new JdbiTransaction(jdbi.open().begin(), dummyContext))
        {
            var h = tx.connection(Handle.class).get();
            h.createUpdate("delete from simple_table where id =:id")
             .bind("id", 1)
             .execute();
            tx.rollback();
        }

        // verify data still present
        try (DataTransaction tx = new JdbiTransaction(jdbi.open().begin(), dummyContext))
        {
            var h = tx.connection(Handle.class).get();
            var result = h.createQuery("select id from simple_table")
                          .map(r -> r.getColumn(1, Integer.class))
                          .findOne();
            assertTrue(result.isPresent());
            assertEquals(1, result.get());
        }

        // force a constraint error and verify we get the suppress transaction exception
        // Currently not failing correctly. org.jdbi.v3.core.statement.UnableToExecuteStatementException is
        // getting thrown before we call commit. This is definitely expected behavior (and I suspect it's 
        // implementation dependent, this test is h2). So it would appear we need to expand on our 
        // mechanism of error handling. Perhaps something around https://jdbi.org/#_exception_handling
        var dataException = assertThrows(OpenDcsDataRuntimeException.class, () ->
        {
            try (var tx = new JdbiTransaction(jdbi.open().begin(), dummyContext))
            {
                var h = tx.connection(Handle.class).get();

                try(var good = h.createUpdate("insert into child_table(parent_id, description) values(:id, :text)")
                                .bind("id", 1)
                                .bind("text", "hello");
                    var bad = h.createUpdate("insert into child_table(parent_id, description) values(:id, :text)")
                               .bind("id", 2)
                               .bind("text", "i will fail"))
                 {
                    good.execute();
                    bad.execute();
                 }
            }
        });
        assertNotNull(dataException.getCause());

        // do the same thing again, but using the new wrapper.
        var dataException2 = assertThrows(OpenDcsDataException.class, () ->
        {
            try (var tx = new JdbiTransaction(jdbi.open().begin(), dummyContext))
            {
                var h = tx.connection(Handle.class).get();

                tx.wrapErrors(() -> 
                {
                    try(var good = h.createUpdate("insert into child_table(parent_id, description) values(:id, :text)")
                                    .bind("id", 1)
                                    .bind("text", "hello");
                        var bad = h.createUpdate("insert into child_table(parent_id, description) values(:id, :text)")
                                .bind("id", 2)
                                .bind("text", "i will fail"))
                    {
                        good.execute();
                        bad.execute();
                    }
                 });
            }
        });
        assertNotNull(dataException2.getCause());
    }
}
