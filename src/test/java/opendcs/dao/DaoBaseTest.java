package opendcs.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fixtures.NonPoolingConnectionOwner;
import fixtures.TestConnectionOwner;

public class DaoBaseTest
{
    TestConnectionOwner dbOwner = new NonPoolingConnectionOwner();

    @BeforeAll
    public static void setup() throws Exception
    {
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
        
    }

    @BeforeEach
    public void create_db() throws Exception
    {

        Connection conn = DriverManager.getConnection("jdbc:derby:memory:db;create=true");
        assertNotNull(conn,"Failed to acquire Derby database.");
        dbOwner.setConnection(conn);
        try( DaoBase dao = new DaoBase(dbOwner,"test"); )
        {
            for( String statement: IOUtils.resourceToString("/opendcs/dao/testdb.sql",StandardCharsets.UTF_8).trim().split(";"))
            {
                if (!statement.trim().isEmpty())
                {
                    System.out.println("Running: ");
                    System.out.println("=============");
                    System.out.println(statement);
                    System.out.println("=====end=====");
                    dao.doModify(statement, new Object[0]); // have to force with one we're using
                }

            }
        }
    }

    @AfterEach
    public void drop_db() throws Exception
    {
        try
        {
            DriverManager.getConnection("jdbc:derby:memory:db;drop=true");
        }
        catch(SQLException err)
        {
            if (err.getSQLState().equals("08006") )
            {
                dbOwner.setConnection(null);
            }
            else
            {
                throw err;
            }
        }

    }

    /**
     * NOTE: this test intentionally mixes some styles to show
     * different ways things can be done.
     */

    @Test
    public void test_do_query() throws Exception
    {
        try( DaoBase dao = new DaoBase(dbOwner,"test"); )
        {
            List<DaoBaseTest.SimpleComp> result = dao.getResults("select id,name from cp_computation", rs -> { return new SimpleComp(rs);});
            assertEquals(2, result.size(), "not all results returned");


            SimpleComp single = dao.getSingleResult("select id,name from cp_computation where lower(name)=lower(?)", DaoBaseTest::fromRS, "AddComp");
            assertNotNull(single, "unable to retrieve a single result");

            final List<String> comp_depends = new ArrayList<>();

            dao.doQuery("select ts.name as ts_name,comp.name as comp_name from cp_comp_depends cdp " +
                        "join cp_computation comp on comp.id = cdp.computation_id " +
                        "join timeseries ts on ts.id = cdp.timeseries_id " +
                        "where lower(comp.name) = lower(?)",(rs) -> {
                            comp_depends.add(String.format("%s->%s",rs.getString("ts_name"),rs.getString("comp_name")));
                        },"AddComp");
            assertEquals(2,comp_depends.size(),"not all computations in table received");
            assertEquals("To Add->AddComp",comp_depends.get(0));
            assertEquals("To Add 2->AddComp",comp_depends.get(1));
        }
    }

    @Test
    public void test_get_multiple_results() throws Exception
    {
        try( DaoBase dao = new DaoBase(dbOwner,"test"); )
        {
            List<DaoBaseTest.SimpleComp> result = dao.getResults("select id,name from cp_computation", rs -> { return new SimpleComp(rs);});
            assertEquals(2, result.size(), "not all results returned");
        }
    }

    public static SimpleComp fromRS(ResultSet rs) throws SQLException
    {
        return new SimpleComp(rs);
    }

    public static class SimpleComp
    {
        private int id;
        private String name;

        public SimpleComp(ResultSet rs) throws SQLException
        {
            id = rs.getInt("id");
            name = rs.getString("name");
        }



        public int getId()
        {
            return id;
        }

        public String getName()
        {
            return name;
        }

    }
}
