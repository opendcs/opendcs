package opendcs.util.sql;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Executor;

import ilex.util.Logger;
import opendcs.dao.DatabaseConnectionOwner;

/**
 * Used for pooled connection system. Allows code
 * to otherwise write a normal try-with-resources
 * to close the connection but will correctly call
 * theDb.freeConnection on behalf of the user
 * 
 * Otherwise it's just a passthrough to the realConnection
 */
public class WrappedConnection implements Connection{
    private static Logger log = Logger.instance();

    private Connection realConnection;
    private final DatabaseConnectionOwner theDb;

    public WrappedConnection(Connection realConnection, final DatabaseConnectionOwner theDb)
    {
        Objects.requireNonNull(realConnection, "WrappedConnection cannot wrap a null connection");
        Objects.requireNonNull(theDb, "WrappedConnections requires a valid TimeSeries DB instance");
        this.realConnection = realConnection;
        this.theDb = theDb;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return realConnection.isWrapperFor(iface);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return realConnection.unwrap(iface);
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        realConnection.abort(executor);
    }

    @Override
    public void clearWarnings() throws SQLException {
        realConnection.clearWarnings();
    }

    @Override
    public void close() throws SQLException {
        theDb.freeConnection(realConnection);
    }

    @Override
    public void commit() throws SQLException {
        realConnection.close();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return realConnection.createArrayOf(typeName, elements);
    }

    @Override
    public Blob createBlob() throws SQLException {
        return realConnection.createBlob();
    }

    @Override
    public Clob createClob() throws SQLException {
        return realConnection.createClob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        return realConnection.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return realConnection.createSQLXML();
    }

    @Override
    public Statement createStatement() throws SQLException {
        logPlainCreate();
        return realConnection.createStatement();
    }

    private void logPlainCreate() {
        log.debug2("Dev Msg: Plain create statement from:");
        StackTraceElement stk[] = Thread.getAllStackTraces().get(Thread.currentThread());
			for(int n = 2; n < stk.length; n++) 
			{
				String s = stk[n].toString().toLowerCase();
				if (s.contains("dao") || s.contains("io.")) 
					log.debug2("\t" + n + ": " + stk[n]);
			}
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        logPlainCreate();
        return realConnection.createStatement(resultSetType, resultSetConcurrency);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        logPlainCreate();
        return realConnection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return realConnection.createStruct(typeName, attributes);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return realConnection.getAutoCommit();
    }

    @Override
    public String getCatalog() throws SQLException {
        return realConnection.getCatalog();
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return realConnection.getClientInfo();
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return realConnection.getClientInfo(name);
    }

    @Override
    public int getHoldability() throws SQLException {
        return realConnection.getHoldability();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return realConnection.getMetaData();
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getSchema() throws SQLException {
        return realConnection.getSchema();
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return realConnection.getTransactionIsolation();
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return realConnection.getTypeMap();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return realConnection.getWarnings();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return realConnection.isClosed();
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return realConnection.isReadOnly();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return realConnection.isValid(timeout);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return realConnection.nativeSQL(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        return realConnection.prepareCall(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return realConnection.prepareCall(sql,resultSetType,resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
        return realConnection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return realConnection.prepareStatement(sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return realConnection.prepareStatement(sql, autoGeneratedKeys);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return realConnection.prepareStatement(sql, columnIndexes);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return realConnection.prepareStatement(sql,columnNames);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException {
        return realConnection.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
        return realConnection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        realConnection.releaseSavepoint(savepoint);
    }

    @Override
    public void rollback() throws SQLException {
        realConnection.rollback();
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        realConnection.rollback(savepoint);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        realConnection.setAutoCommit(autoCommit);
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        realConnection.setCatalog(catalog);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        realConnection.setClientInfo(properties);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        realConnection.setClientInfo(name, value);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        realConnection.setHoldability(holdability);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        realConnection.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        realConnection.setReadOnly(readOnly);   
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return realConnection.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return realConnection.setSavepoint(name);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        realConnection.setSchema(schema);
        
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        realConnection.setTransactionIsolation(level);
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        realConnection.setTypeMap(map);
    }
    
}
