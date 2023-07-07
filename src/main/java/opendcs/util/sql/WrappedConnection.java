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
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;

import org.opendcs.jmx.WrappedConnectionMBean;
import org.opendcs.jmx.connections.JMXTypes;

import ilex.util.Logger;
import opendcs.util.functional.ThrowingConsumer;

/**
 * Used for pooled connection system. Allows code
 * to otherwise write a normal try-with-resources
 * to close the connection but will correctly call
 * theDb.freeConnection on behalf of the user
 *
 * Otherwise it's just a passthrough to the realConnection
 */
public class WrappedConnection implements Connection, WrappedConnectionMBean{
    private static Logger log = Logger.instance();

    private Connection realConnection;
    private final ThrowingConsumer<WrappedConnection,SQLException> onClose;
    private boolean trace;
    private List<StackTraceElement> openTrace = null;
    private List<StackTraceElement> closeTrace = null;
    private ZonedDateTime start = ZonedDateTime.now();
    private ZonedDateTime end = null;
    private final Thread openingThread;


    public WrappedConnection(Connection realConnection){
        this(realConnection, (c) -> {},false);
    }

    public WrappedConnection(Connection realConnection, final ThrowingConsumer<WrappedConnection,SQLException> onClose)
    {
        this(realConnection,onClose,false);
    }

    public WrappedConnection(Connection realConnection, final ThrowingConsumer<WrappedConnection,SQLException> onClose, boolean trace )
    {
        Objects.requireNonNull(realConnection, "WrappedConnection cannot wrap a null connection");
        Objects.requireNonNull(onClose, "WrappedConnections requires a valid Consumer for the close operation");
        this.realConnection = realConnection;
        this.onClose = onClose;
        this.trace = trace;
        this.openingThread = Thread.currentThread();
        if(trace)
        {
            openTrace = new ArrayList<>();
            StackTraceElement ste[] = Thread.currentThread().getStackTrace();
            // start at 3, after this constructor and the getThread/Stack trace call
            for(int i = 3; i < ste.length; i++)
            {
                openTrace.add(ste[i]);
            }
        }

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
        if (trace)
        {
            closeTrace = new ArrayList<>();
            StackTraceElement ste[] = Thread.currentThread().getStackTrace();
            // start at 3, after this constructor and the getThread/Stack trace call
            for(int i = 3; i < ste.length; i++)
            {
                closeTrace.add(ste[i]);
            }
            // send notice
            end = ZonedDateTime.now();
        }


        onClose.accept(this);
    }

    @Override
    public void commit() throws SQLException {
        realConnection.commit();
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
        if(trace)
        {
            log.debug2("Dev Msg: Plain create statement from:");
            if(log.getMinLogPriority() == Logger.E_DEBUG3)
            {
                StackTraceElement stk[] = Thread.currentThread().getStackTrace();
                for(int n = 2; n < stk.length; n++)
                {
                        log.debug3("\t" + n + ": " + stk[n]);
                }
            }
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
        return realConnection.getNetworkTimeout();
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

    public CompositeData asCompositeData() throws OpenDataException
    {
        String[] itemNames = new String[1];
        Object[] values = new Object[1];
        itemNames[0] = "Lifetime";
        values[0] = Duration.between(start, end == null ? ZonedDateTime.now() : end)
                            .getSeconds();
        return new CompositeDataSupport(JMXTypes.CONNECTION_LIST_TYPE,itemNames,values);
    }


    public void dumpData()
    {
        boolean closed = false;
        try
        {
            closed =  this.isClosed();
        }
        catch(SQLException ex)
        {
            System.err.println("Unable to verify if connection open.");

        }
        printStackTraceOnExit(closed);
    }

    private void printStackTraceOnExit(boolean closed)
    {
        System.err.println(String.format("Connection(closed state -> %s) with life time of %d seconds remains from:",closed,Duration.between(start, ZonedDateTime.now()).getSeconds()));
        if( openTrace != null)
        {
            System.err.println(String.format("\tOpened from (in thread %s)",(openingThread != null) ? this.openingThread.getName(): "no thread?"));
            for(StackTraceElement ste: openTrace)
            {
                System.err.println("\t\t" + ste.toString());
            }
        }
        if( closeTrace != null)
        {
            System.err.println("\tClosed from");
            for(StackTraceElement ste: closeTrace)
            {
                System.err.println("\t\t" + ste.toString());
            }
        }
    }

    /**
     * Get the underlying wrapped connection.
     * Primarily for the close lambda to search the out list
     * @return the connection in whatever state it's in.
     */
    public final Connection getRealConnection()
    {
        return this.realConnection;
    }

	@Override
	public int getRealConnectionHashCode()
    {
		return realConnection.hashCode();
	}

	@Override
	public String getConnectionOpened()
    {
		return start.toString();
	}

	@Override
	public long getConnectionLifetimeSeconds()
    {
		return Duration.between(start,
                                end == null
                                    ? ZonedDateTime.now()
                                    : end)
                       .getSeconds();
	}

	@Override
	public String[] getOpenStackTrace()
    {
		return openTrace != null
                    ? openTrace.stream()
                               .map(ste->ste.toString())
                               .collect(Collectors.toList())
                               .toArray(new String[0])
                    : new String[]{"No Trace."};
	}

	@Override
	public boolean getTracingOn()
    {
		return trace;
	}

    @Override
    public String getThread() {
        return openingThread.getName();
    }
}
