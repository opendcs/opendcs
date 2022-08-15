package opendcs.opentsdb.hydrojson;

import ilex.util.Logger;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import opendcs.dai.IntervalDAI;
import opendcs.dao.DaoBase;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.sql.KeyGenerator;
import decodes.sql.OracleSequenceKeyGenerator;
import decodes.sql.SequenceKeyGenerator;
import decodes.sql.SqlDatabaseIO;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesDb;

/**
 * This class is constructed for each request and is used to access the TSDB.
 * @author mmaloney
 *
 */
public class TsdbInterface
	implements AutoCloseable
{
	static final String module = "TsdbInterface";
	private TimeSeriesDb theDb = null;
	private SqlDatabaseIO sqlDbIo = null;
	static boolean isOracle = false;
	static boolean isCwms = false;
	static boolean isHdb = false;
	static boolean isOpenTsdb = false;
	static boolean isTypeDetermined = false;
	private DataSource dataSource = null;
	static long lastDecodesCacheFill = 0L;
	static final long fillDecodesEveryMS = 3600000L; // refill decodes cache every hour
	
	/** Provides reason for last error return. */
	private String reason = null;
	
	public TsdbInterface() 
		throws DbIoException
	{
		Connection con = null;
		KeyGenerator kg = null;

		try
		{
			Context initialCtx = new InitialContext();
			Context envCtx = (Context)initialCtx.lookup("java:comp/env");
			dataSource = (DataSource)envCtx.lookup("jdbc/opentsdb");
			con = dataSource.getConnection();
			isOracle = con.getMetaData().getDatabaseProductName().toLowerCase().contains("oracle");
			kg = isOracle ? new OracleSequenceKeyGenerator() : new SequenceKeyGenerator();
		}
		catch(SQLException ex)
		{
			String msg = "Cannot connect to database for jdbc/opentsdb: " + ex;
			System.err.println(msg);
			ex.printStackTrace(System.err);
			throw new DbIoException(msg);
		}
		catch (NamingException ex)
		{
			String msg = "Cannot lookup envCtx java:comp/env, and then jdbc/opentsdb: " + ex;
			System.err.println(msg);
			ex.printStackTrace(System.err);
			throw new DbIoException(msg);
		}
		
		String tsdbClass = 
				isHdb ? "decodes.hdb.HdbTimeSeriesDb" :
				isCwms ? "decodes.cwms.CwmsTimeSeriesDb" : 
				"opendcs.opentsdb.OpenTsdb";
		Logger.instance().info("tsdbClass=" + tsdbClass);
		if (!isTypeDetermined)
		{
			Logger.instance().info(module + " determining database type");
			isTypeDetermined = true;

			Statement statement = null;
			try
			{
				statement = con.createStatement();
				// hdb_damtype table only exists in HDB.
				ResultSet rs = statement.executeQuery("select * from hdb_damtype");
				isHdb = true;
				try { rs.close(); } catch(Exception ex) {}
			}
			catch (SQLException ex)
			{
				isHdb = false;
			}
			finally
			{
				if (statement != null)
					try { statement.close(); } catch(Exception ex) {}
				statement = null;
			}
			
			if (!isHdb)
			{
				try
				{
					statement = con.createStatement();
					// hdb_damtype table only exists in HDB.
					String q = "select distinct parameter_id from cwms_v_parameter";
					ResultSet rs = statement.executeQuery(q);
					isCwms = true;
					try { rs.close(); } catch(Exception ex) {}
				}
				catch (SQLException ex)
				{
					isCwms = false;
				}
				finally
				{
					try { statement.close(); } catch(Exception ex) {}
				}
			}
			isOpenTsdb = tsdbClass == "opendcs.opentsdb.OpenTsdb";
			Logger.instance().info("isHdb=" + isHdb + ", isCwms=" + isCwms 
				+ ", isOpenTsdb=" + isOpenTsdb + ", isOracle=" + isOracle);
		}
		
		try
		{
			setTheDb((TimeSeriesDb)Class.forName(tsdbClass).newInstance());
			getTheDb().setConnection(con);
			getTheDb().keyGenerator = kg;
		}
		catch (Exception ex)
		{
			String msg = "Cannot Instantiate '" + tsdbClass + "': " + ex;
			System.err.println(msg);
			ex.printStackTrace(System.err);
			if (con != null)
				try { con.close(); } catch(Exception ex2) {}
			throw new DbIoException(msg);
		}
		finally
		{
		}
		
		// Does DECODES Database Exist yet?
		if (Database.getDb() == null)
		{
			Logger.instance().info(module + " Making new DECODES Database.");
			Database.setDb(new Database());
			String appDbIoClass = 
				isHdb ? "decodes.hdb.HdbSqlDatabaseIO" :
				isCwms ? "decodes.cwms.CwmsSqlDatabaseIO" : "opendcs.opentsdb.OpenTsdbSqlDbIO";
			Logger.instance().info("appDbIoClass=" + appDbIoClass);

			try
			{
				setSqlDbIo((SqlDatabaseIO)Class.forName(appDbIoClass).newInstance());
				getSqlDbIo().setKeyGenerator(kg);
				SqlDatabaseIO.readVersionInfo(getSqlDbIo(), con);
				Database.getDb().setDbIo(getSqlDbIo());
			}
			catch (Exception ex)
			{
				String msg = "Cannot Instantiate '" + appDbIoClass + "': " + ex;
				System.err.println(msg);
				ex.printStackTrace(System.err);
				if (con != null)
					try { con.close(); } catch(Exception ex2) {}
				Database.setDb(null);
				throw new DbIoException(msg);
			}
		}
		else // Database already exists.
			setSqlDbIo((SqlDatabaseIO)Database.getDb().getDbIo());

		// Let SqlDatabaseIO use the data source for connections as needed. 
		getSqlDbIo().setPoolingDataSource(dataSource);

		if (System.currentTimeMillis() - lastDecodesCacheFill > fillDecodesEveryMS)
			fillDecodesCache();
	}
	
	public void close()
	{
		if (getTheDb() != null)
		{
			getTheDb().closeConnection();
			setTheDb(null);
		}
		if (Database.getDb() != null
		 && Database.getDb().getDbIo() != null)
		{
			Database.getDb().getDbIo().close();
			
		}
	}
	
	/**
	 * Fill or refill the DECODES cached objects.
	 * @throws DbIoException
	 */
	public void fillDecodesCache()
		throws DbIoException
	{
		Logger.instance().info("TsdbInterface.fillDecodesCache()");
		Database db = Database.getDb();
		SqlDatabaseIO sqlDbIo = (SqlDatabaseIO)db.getDbIo();
if (sqlDbIo == null) System.out.println("ERROR SqlDbIo is NULL!!!");
		IntervalDAI intervalDAO = null;
		try
		{
			System.out.println("Enums");
			db.enumList.read();

			System.out.println("DataTypes");
			db.dataTypeSet.read();
			
			System.out.println("engineeringUnitList");
			db.engineeringUnitList.read();
			
			System.out.println("Intervals");
			intervalDAO = sqlDbIo.makeIntervalDAO();
			intervalDAO.loadAllIntervals();
			
			lastDecodesCacheFill = System.currentTimeMillis();
		}
		catch (Exception ex)
		{
			String msg = "Error filling DECODES cache: " + ex;
			System.err.println(msg);
			ex.printStackTrace(System.err);
			throw new DbIoException(msg);
		}
		finally
		{
			if (intervalDAO != null)
				intervalDAO.close();
		}
System.out.println("TsdbInterface.fillDecodesCache() finished.");
	}
	
	public boolean isUserValid(String username, String password)
	{
		Connection poolCon = getTheDb().getConnection();
		Connection userCon = null;

		try (DaoBase daoBase = new DaoBase(getTheDb(), "TsdbInterface"))
		{
			// The only way to verify that user/pw is valid is to attempt to establish a connection:
			DatabaseMetaData metaData = poolCon.getMetaData();
			String url = metaData.getURL();
			userCon = DriverManager.getConnection(url, username, password);
			// Above with throw SQLException if user/pw is not valid.
			
			// Now verify that user has appropriate privilege. This only works on Postgress currently:
			String q = "select pm.roleid, pr.rolname from pg_auth_members pm, pg_roles pr "
				+ "where pm.member = (select oid from pg_roles where rolname = '" + username + "') "
				+ "and pm.roleid = pr.oid";
			ResultSet rs = daoBase.doQuery(q);
			while(rs.next())
			{
				int roleid = rs.getInt(1);
				String role = rs.getString(2);
				System.out.println("User '" + username + "' has role " + roleid + "=" + role);
				if (role.equalsIgnoreCase("OTSDB_ADMIN") || role.equalsIgnoreCase("OTSDB_MGR"))
					return true;
			}
			reason = "User " + username + " does not have OTSDB_ADMIN or OTSDB_MGR privilege "
					+ "- Not Authorized.";
			System.out.println("isUserValid(" + username + ") failed: " + reason);
			return false;

		}
		catch (Exception e)
		{
System.out.println("isUserValid - Authentication failed: " + e);
			reason = e.getMessage();
			return false;
		}
		finally
		{
			if (userCon != null)
				try { userCon.close(); } catch(Exception ex) {}
		}
	}

	public String getReason()
	{
		return reason;
	}

	TimeSeriesDb getTheDb()
	{
		return theDb;
	}

	void setTheDb(TimeSeriesDb theDb)
	{
		this.theDb = theDb;
	}

	SqlDatabaseIO getSqlDbIo()
	{
		return sqlDbIo;
	}

	void setSqlDbIo(SqlDatabaseIO sqlDbIo)
	{
		this.sqlDbIo = sqlDbIo;
	}

}
