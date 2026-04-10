package org.opendcs.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        public DatabaseEngine getDatabase()
        {
            throw new UnsupportedOperationException("Unimplemented method 'getDatabase'");
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
}
