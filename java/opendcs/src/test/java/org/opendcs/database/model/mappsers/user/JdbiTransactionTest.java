package org.opendcs.database.model.mappsers.user;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.DriverManager;
import java.sql.SQLException;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendcs.database.JdbiTransaction;
import org.opendcs.database.api.DataTransaction;

public class JdbiTransactionTest
{
    Jdbi jdbi;

    @BeforeAll
    static void setup() throws Exception
    {
        Class<?> clazz = Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        var dbC = clazz.getConstructor();
        var obj = dbC.newInstance();
        assertNotNull(obj);
    }

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
    void test_jdbi_transaction_returns_empty_on_unknown() throws Exception
    {
        try (DataTransaction tx = new JdbiTransaction(jdbi.open()))
        {
            assertFalse(tx.connection(Integer.class).isPresent());
        }
    }

    @Test
    void test_jdbi_transaction_operations() throws Exception
    {
        try (DataTransaction tx = new JdbiTransaction(jdbi.open()))
        {
            var h = tx.connection(Handle.class).get();
            h.createUpdate("create table simple_table(id integer, name varchar(200))").execute();
        }

        try (DataTransaction tx = new JdbiTransaction(jdbi.open().begin()))
        {
            var h = tx.connection(Handle.class).get();
            h.createUpdate("insert into simple_table(id,name) values (:id, :name)")
             .bind("id", 1)
             .bind("name", "test")
             .execute();
        }

        try (DataTransaction tx = new JdbiTransaction(jdbi.open().begin()))
        {
            var h = tx.connection(Handle.class).get();
            h.createUpdate("delete from simple_table where id =:id")
             .bind("id", 1)
             .execute();
            tx.rollback();
        }

        try (DataTransaction tx = new JdbiTransaction(jdbi.open().begin()))
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
