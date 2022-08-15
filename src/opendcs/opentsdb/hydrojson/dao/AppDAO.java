package opendcs.opentsdb.hydrojson.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import opendcs.dao.DaoBase;
import opendcs.dao.DatabaseConnectionOwner;
import opendcs.opentsdb.hydrojson.ErrorCodes;
import opendcs.opentsdb.hydrojson.beans.AppRef;
import opendcs.opentsdb.hydrojson.beans.LoadingApp;
import opendcs.opentsdb.hydrojson.errorhandling.WebAppException;

public class AppDAO
	extends DaoBase
{
	public static String module = "AppDAO";

	public AppDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, module);
	}
	
	public ArrayList<AppRef> getAppRefs()
			throws DbIoException
	{
		ArrayList<AppRef> ret = new ArrayList<AppRef>();
		SimpleDateFormat lastModifiedSdf = new SimpleDateFormat("yyyyMMddHHmmss");
		lastModifiedSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		String q = "select ap.LOADING_APPLICATION_ID, ap.LOADING_APPLICATION_NAME, ap.CMMNT"
				+ " from HDB_LOADING_APPLICATION ap"
				+ " order by ap.LOADING_APPLICATION_ID";
		
		ResultSet rs = doQuery(q);
		try
		{
			while (rs.next())
			{
				AppRef ref = new AppRef();
				ref.setAppId(rs.getLong(1));
				ref.setAppName(rs.getString(2));
				ref.setComment(rs.getString(3));
				ret.add(ref);
			}
			
			q = "select LOADING_APPLICATION_ID, PROP_NAME, PROP_VALUE"
				+ " from REF_LOADING_APPLICATION_PROP";
			rs = doQuery(q);
			while (rs.next())
			{
				long id = rs.getLong(1);
				String name = rs.getString(2);
				String value = rs.getString(3);
				for(AppRef ref : ret)
					if (ref.getAppId() == id)
					{
						if (name.equalsIgnoreCase("apptype"))
							ref.setAppType(value);
						else if (name.equalsIgnoreCase("lastmodified"))
						{
							try 
							{
								ref.setLastModified(lastModifiedSdf.parse(value));
							}
							catch(ParseException ex) {}
						}
						break;
					}
			}
			
			return ret;
		}
		catch(SQLException ex)
		{
			throw new DbIoException(module + ".getAppRefs() error in query '" + q + "': " + ex);
		}
	}

	public LoadingApp getApp(long id)
		throws DbIoException, WebAppException
	{
		SimpleDateFormat lastModifiedSdf = new SimpleDateFormat("yyyyMMddHHmmss");
		lastModifiedSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		String q = "select LOADING_APPLICATION_NAME, CMMNT, MANUAL_EDIT_APP"
				+ " from HDB_LOADING_APPLICATION"
				+ " where LOADING_APPLICATION_ID = " + id;
		ResultSet rs = doQuery(q);
		
		try
		{
			if (!rs.next())
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, 
					"No HDB_LOADING_APPLICATION with id=" + id);
			
			LoadingApp ret = new LoadingApp();
			ret.setAppId(id);
			ret.setAppName(rs.getString(1));
			ret.setComment(rs.getString(2));
			ret.setManualEditingApp(TextUtil.str2boolean(rs.getString(3)));

			q = "select PROP_NAME, PROP_VALUE"
				+ " from REF_LOADING_APPLICATION_PROP"
				+ " where LOADING_APPLICATION_ID = " + id;
			rs = doQuery(q);
			while (rs.next())
			{
				String name = rs.getString(1);
				String value = rs.getString(2);
				if (name.equalsIgnoreCase("apptype"))
					ret.setAppType(value);
				else if (name.equalsIgnoreCase("lastmodified"))
				{
					try 
					{
						ret.setLastModified(lastModifiedSdf.parse(value));
					}
					catch(ParseException ex) {}
				}
				else
					ret.getProperties().setProperty(name, value);
			}
			return ret;
		}
		catch(SQLException ex)
		{
			throw new DbIoException(module + ".getApp error in query '" + q + "': " + ex);
		}
	}

	public void writeApp(LoadingApp app)
		throws DbIoException, WebAppException
	{
		// Check to make sure name is unique throw WebAppException if conflict
		// I.e. check for list with same name but different key.
		String q = "select LOADING_APPLICATION_ID from HDB_LOADING_APPLICATION "
			+ "where lower(LOADING_APPLICATION_NAME) = " 
			+ sqlString(app.getAppName().toLowerCase());
		if (app.getAppId() != DbKey.NullKey.getValue())
			q = q + " and LOADING_APPLICATION_ID != " + app.getAppId();
System.out.println(module + " Check for dup name: " + q);
		ResultSet rs = doQuery(q);
		try
		{
			if (rs.next())
				throw new WebAppException(ErrorCodes.NOT_ALLOWED, 
					"Cannot write App with name '" + app.getAppName() 
					+ "' because another App with id=" + rs.getLong(1) 
					+ " also has that name.");

			app.setLastModified(new Date());
			if (app.getAppId() == DbKey.NullKey.getValue())
			{
				app.setAppId(getKey("HDB_LOADING_APPLICATION").getValue());
	
				q = "insert into HDB_LOADING_APPLICATION(LOADING_APPLICATION_ID,"
					+ " LOADING_APPLICATION_NAME, MANUAL_EDIT_APP, CMMNT) values ("
					+ app.getAppId() + ", "
					+ sqlString(app.getAppName()) + ", "
					+ sqlString(app.isManualEditingApp() ? "Y": "N") + ", "
					+ sqlString(app.getComment())
					+ ")";
				doModify(q);
				addProps(app);
			}
			else
			{
				q = "update HDB_LOADING_APPLICATION set"
					+ " LOADING_APPLICATION_NAME = " + sqlString(app.getAppName())
					+ ", MANUAL_EDIT_APP = " + sqlString(app.isManualEditingApp() ? "Y": "N")
					+ ", CMMNT = " + sqlString(app.getComment())
					+ " where LOADING_APPLICATION_ID = " + app.getAppId();
				doModify(q);
				
				q = "delete from REF_LOADING_APPLICATION_PROP where LOADING_APPLICATION_ID = " + app.getAppId();
				doModify(q);
				addProps(app);
			}
		}
		catch (SQLException e)
		{
			throw new DbIoException("writeApp Error in query '" + q + "'");
		}

	}			

	private void addProps(LoadingApp app)
		throws DbIoException
	{
		Properties props = new Properties();
		PropertiesUtil.copyProps(props, app.getProperties());
		
		if (app.getAppType() != null)
			props.setProperty("appType", app.getAppType());

		SimpleDateFormat lastModifiedSdf = new SimpleDateFormat("yyyyMMddHHmmss");
		lastModifiedSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		props.setProperty("LastModified", lastModifiedSdf.format(app.getLastModified()));
		
		for(Object k : props.keySet())
		{
			String pn = (String)k;
			String pv = props.getProperty(pn);
			String q = "insert into REF_LOADING_APPLICATION_PROP(LOADING_APPLICATION_ID, PROP_NAME, PROP_VALUE)"
				+ " values (" + app.getAppId() + ", " + sqlString(pn) + ", " + sqlString(pv) + ")";
			doModify(q);
		}
	}

	public void deleteApp(long appId)
		throws DbIoException, WebAppException
	{
		String q = "select count(*) from CP_COMPUTATION where LOADING_APPLICATION_ID = " + appId;
		ResultSet rs = doQuery(q);
		try
		{
			if (rs.next())
			{
				long n = rs.getLong(1);
				if (!rs.wasNull() && n > 0)
					throw new WebAppException(ErrorCodes.NOT_ALLOWED, "Cannot delete loading appId=" + appId
						+ " because there are " + n + " computations assigned to this app.");
			}
			q = "select PID, HOSTNAME, HEARTBEAT from CP_COMP_PROC_LOCK where LOADING_APPLICATION_ID = " 
				+ appId;
			rs = doQuery(q);
			if (rs.next())
			{
				long heartbeat = rs.getLong(3);
				if (System.currentTimeMillis() - heartbeat < 20000L)
					throw new WebAppException(ErrorCodes.NOT_ALLOWED, "Cannot delete loading appId=" + appId
						+ " because it is currently running on host '" + rs.getString(2)
						+ "' with PID=" + rs.getInt(1));

			}
			q = "select count(*) from SCHEDULE_ENTRY where LOADING_APPLICATION_ID = " + appId;
			rs = doQuery(q);
			if (rs.next())
			{
				long n = rs.getLong(1);
				if (!rs.wasNull() && n > 0)
					throw new WebAppException(ErrorCodes.NOT_ALLOWED, "Cannot delete loading appId=" + appId
						+ " because there are " + n + " schedule entries assigned to this app.");
			}

			if (db.isOpenTSDB())
			{			
				q = "select count(*) from TSDB_DATA_SOURCE where LOADING_APPLICATION_ID = " + appId;
				rs = doQuery(q);
				if (rs.next())
				{
					int n = rs.getInt(1);
					if (n > 0)
						throw new WebAppException(ErrorCodes.NOT_ALLOWED, "Cannot delete loading appId=" + appId
							+ " because there are " + n + " TSDB_DATA_SOURCE records assigned to this app.");
				}
			}
			
			q = "delete from CP_COMP_TASKLIST where LOADING_APPLICATION_ID = " + appId;
			doModify(q);
			
			q = "delete from DACQ_EVENT where LOADING_APPLICATION_ID = " + appId;
			doModify(q);

			q = "delete from REF_LOADING_APPLICATION_PROP where LOADING_APPLICATION_ID = " + appId;
			doModify(q);

			q = "delete from HDB_LOADING_APPLICATION where LOADING_APPLICATION_ID = " + appId;
			doModify(q);
		}
		catch(SQLException ex)
		{
			String msg = module + ".deleteApp: error in query '" + q + "': " + ex;
			System.err.println(msg);
			ex.printStackTrace();
			throw new DbIoException(msg);
		}
	}

}
