package opendcs.dao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import decodes.tsdb.TimeSeriesDb;
import fixtures.PoolingConnectionOwner;
import opendcs.opentsdb.OpenTsdb;
import opendcs.util.sql.WrappedConnection;
import org.apache.derby.jdbc.EmbeddedDataSource;
/**
 * This is full of mocks as it's specifically testing the logical flow of
 * concepts not a particular implementation.
 */
public class DaoBaseLambaHandlersPoolTest {

    @Test
    public void test_DAO_without_manual_connection_uses_but_does_not_close_non_pooled_tsdb() throws Exception
    {
        Connection mockConn = Mockito.mock(Connection.class);
        boolean closed[] = new boolean[1];
        closed[0] = false;
        doAnswer(a -> {
            closed[0] = true;
            return null;
        }).when(mockConn).close();

        TimeSeriesDb db = Mockito.mock(TimeSeriesDb.class);
        when(db.getConnection()).thenReturn(mockConn);
        doNothing().when(db).freeConnection(mockConn);

        boolean result[] = new boolean[1];
        result[0] = false;
        try(DaoBase dao = new DaoBase(db,"Test");)
        {
            dao.withConnection(conn -> {
                result[0] = true;
            });
        }
        assertFalse(closed[0],"Dao closed connection when it shouldn't have.");
        assertTrue(result[0],"Consumer was not run.");
    }

    @Test
    public void test_DAO_with_manual_connection_doesnt_close_it() throws Exception
    {
        Connection mockConn = Mockito.mock(Connection.class);
        boolean closed[] = new boolean[1];
        closed[0] = false;
        doAnswer(a -> {
            closed[0] = true;
            return null;
        }).when(mockConn).close();

        TimeSeriesDb db = Mockito.mock(TimeSeriesDb.class);
        when(db.getConnection()).thenReturn(mockConn);
        doAnswer( a -> {
            Connection c = a.getArgument(0);
            c.close();
            return null;
        }).when(db).freeConnection(any(Connection.class));

        boolean result[] = new boolean[1];
        result[0] = false;
        try(DaoBase dao = new DaoBase(db,"Test",mockConn);)
        {
            dao.withConnection(conn -> {
                result[0] = true;
            });

            dao.withConnection(conn->{
                result[0] = true;
            });
        }
        assertFalse(closed[0],"Dao closed connection when it shouldn't have.");
        assertTrue(result[0],"Consumer was not run.");
    }

    @Test
    public void test_with_real_pool_gives_new_connection_each_query() throws Exception
    {
        EmbeddedDataSource pool = spy(new EmbeddedDataSource());
        pool.setDatabaseName("memory:newconn");
        pool.setCreateDatabase("create");
        DatabaseConnectionOwner dbOwner = new PoolingConnectionOwner(pool);
        try(DaoBase dao = new DaoBase(dbOwner,"test");)
        {
            dao.doModify("create table test_table(id varchar(255) primary key,value varchar(255))", new Object[0]);
            dao.doModify("insert into test_table(id,value) values('a','1')",new Object[0]);
            String ret = dao.getSingleResult("select value from test_table where id=?",rs->rs.getString(1),"a");
            assertEquals("1",ret,"could not retrieve stored value");
            verify(pool,times(3)).getConnection();
        }
    }

    @Test
    public void test_with_real_pool_uses_provided_connection_each_query() throws Exception
    {
        EmbeddedDataSource pool = spy(new EmbeddedDataSource());
        pool.setDatabaseName("memory:reuseconn");
        pool.setCreateDatabase("create");
        DatabaseConnectionOwner dbOwner = new PoolingConnectionOwner(pool);
        try(Connection conn = pool.getConnection();
            DaoBase dao = new DaoBase(dbOwner,"test",conn);
            DaoBase dao2 = new DaoBase(dbOwner,"test2");
        )
        {
            String getQuery = "select value from test_table where id=?";
            dao2.setManualConnection(conn);
            dao.doModify("create table test_table(id varchar(255) primary key,value varchar(255))", new Object[0]);
            dao.doModify("insert into test_table(id,value) values('a','1')",new Object[0]);
            String ret = dao.getSingleResult(getQuery,rs->rs.getString(1),"a");
            assertEquals("1",ret,"could not retrieve stored value");
            
            ResultSet rs = dao.doQuery("select value from test_table where id='" + "a" + "'");
            assertTrue(rs.next(),"Could not get row from old method.");
            
            assertEquals("1",rs.getString(1),"could not retrieve stored value with old method");

            assertEquals("1",dao2.getSingleResult(getQuery,rs2->rs2.getString(1),"a"));

            verify(pool,times(1)).getConnection();
        }
    }
}
