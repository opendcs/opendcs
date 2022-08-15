package opendcs.opentsdb.hydrojson.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;

import decodes.tsdb.DbIoException;
import opendcs.dao.DaoBase;
import opendcs.dao.DatabaseConnectionOwner;

public class TsDAO
	extends DaoBase
{
	public static String module = "TsDAO";

	public TsDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, module);
	}
	
	public Properties getTsdbProperties()
		throws DbIoException
	{
		String q = "select prop_name, prop_value from tsdb_property";
		ResultSet rs = doQuery(q);
		try
		{
			Properties ret = new Properties();
			while (rs.next())
			{
				ret.setProperty(rs.getString(1), rs.getString(2));
			}
			return ret;
		}
		catch(SQLException ex)
		{
			throw new DbIoException(module + ".getTsdbProperties() error in query '" + q + "': " + ex);
		}
	}
	
	/**
	 * Note call with null value to delete the property
	 * @param name
	 * @param value
	 */
	public void setTsdbProperties(Properties props)
		throws DbIoException
	{
		String q = "";
		try
		{
			for (Enumeration<?>  pen = props.propertyNames(); pen.hasMoreElements(); )
			{
				String name = (String)pen.nextElement();
				String value = props.getProperty(name);
				if (value == null || value.trim().length() == 0)
				{
					q = "delete from tsdb_property where lower(prop_name) = " + sqlString(name.toLowerCase());
					this.doModify(q);
				}
				else
				{
					q = "select prop_value from tsdb_property where lower(prop_name) = " 
						+ sqlString(name.toLowerCase());
					ResultSet rs = doQuery(q);
					if (!rs.next())
						q = "insert into tsdb_property(prop_name, prop_value) values ("
							+ sqlString(name) + ", " + sqlString(value) + ")";
					else
						q = "update tsdb_property set prop_value = " + sqlString(value)
							+ " where lower(prop_name) = " + sqlString(name.toLowerCase());
					doModify(q);
				}
			}
		}
		catch(SQLException ex)
		{
			throw new DbIoException(module + ".setTsdbProperty() error in query '" + q + "': " + ex);
		}
	}
	
	

}
