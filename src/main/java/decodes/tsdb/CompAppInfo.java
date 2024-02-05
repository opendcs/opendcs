/* 
* $Id
*/
package decodes.tsdb;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

import opendcs.dai.LoadingAppDAI;
import opendcs.dao.CachableDbObject;
import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.util.HasProperties;
import ilex.util.PropertiesUtil;
import decodes.sql.DbKey;
import decodes.tsdb.xml.CompXioTags;
import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;
import decodes.db.Constants;
import decodes.db.DatabaseException;
import decodes.db.IdDatabaseObject;
import decodes.db.IncompleteDatabaseException;
import decodes.db.InvalidDatabaseException;

/**
This class is used only for listing computation apps. Each of these
entries holds the ID, name, comment, and number of computations for
a single computation application.
*/
public class CompAppInfo
	extends IdDatabaseObject
	implements CompMetaData, CachableDbObject, HasProperties, PropertiesOwner, Serializable
{
	/** The app ID */
	private DbKey appId;

	/** the app name */
	private String appName;

	/** comment */
	private String comment;

	/** number of computations currently assigned to this app */
	private int numComputations;

	/** True if this is a manual edit app (used by USBR) */
	private boolean manualEditApp;
	
	/** A set of properties for this application. */
	private Properties props;
	
	/** Time that this app was last modified in the database */
	private Date lastModified = null;
	
	private PropertySpec propSpecs[] =
	{
//		new PropertySpec("appType", PropertySpec.DECODES_ENUM + "ApplicationType", 
//			"Determines type of application"),
		new PropertySpec("allowedHosts", PropertySpec.STRING, 
			"comma-separated list of hostnames or ip addresses"),

		// Dummy properties for testing the GUI
		//TODO remove the following:
//		new PropertySpec("dummyInt", PropertySpec.INT, "dummy int prop"),
//		new PropertySpec("dummyNumber", PropertySpec.NUMBER, "dummy number prop"),
//		new PropertySpec("dummyFilename", PropertySpec.FILENAME, "dummy filename prop"),
//		new PropertySpec("dummyDirectory", PropertySpec.DIRECTORY, "dummy directory prop"),
//		new PropertySpec("dummyTZ", PropertySpec.TIMEZONE, "dummy timezone prop"),
//		new PropertySpec("dummyBoolean", PropertySpec.BOOLEAN, "dummy boolean prop"),
//		new PropertySpec("dummyHost", PropertySpec.HOSTNAME, "dummy hostname prop")
	};

	public CompAppInfo(DbKey id)
	{
		this();
		appId = id;
	}

	public CompAppInfo()
	{
		appId = Constants.undefinedId;
		appName = null;
		comment = null;
		numComputations = 0;
		props = new Properties();
		manualEditApp = false;
	}

	/** @see decodes.tsdb.CompMetaData */
	public String getObjectType() { return CompXioTags.loadingApplication; }

	/** @see decodes.tsdb.CompMetaData */
	public String getObjectName() { return appName; }

	/** @return the app ID */
	public DbKey getAppId() { return appId; }

	/** 
	 * Sets the app ID 
	 * @param id the ID
	 */
	public void setAppId(DbKey id) { appId = id; }

	/** @return the app name */
	public String getAppName() { return appName; }

	/** 
	 * Sets the app Name 
	 * @param nm the Name
	 */
	public void setAppName(String nm) { appName = nm; }
	
	/**
	 * @return appType property value if one is defined, or blank string if not.
	 */
	public String getAppType()
	{
		String ret = getProperty("appType");
		return ret != null ? ret : "";
	}

	/** @return the comment */
	public String getComment() { return comment; }

	/** 
	 * Sets the comment
	 * @param cm the comment
	 */
	public void setComment(String cm) { comment = cm; }

	/** @return the number of computations that this app manages */
	public int getNumComputations() { return numComputations; }

	/** 
	 * Sets the number of computations that this app manages.
	 * @param n the number
	 */
	public void setNumComputations(int n) { numComputations = n; }

	/**
	 * Creates & returns a copy of this object, except for the database ID.
	 * @return a copy of this object, except for the database ID.
	 */
	public CompAppInfo copyNoId()
	{
		CompAppInfo newOb = new CompAppInfo();
		newOb.setAppName(appName);
		newOb.setComment(comment);
		newOb.setManualEditApp(manualEditApp);
		PropertiesUtil.copyProps(newOb.props, props);
		return newOb;
	}

	/**
	 * @return true if the passed object is equal to this except for db id.
	 */
	public boolean equalsNoId(CompAppInfo rhs)
	{
		if (!TextUtil.strEqual(rhs.getAppName(), appName))
			return false;
		String c1 = rhs.getComment();
		if (c1 != null && c1.trim().length() == 0)
			c1 = null;
		String c2 = comment;
		if (c2 != null && c2.trim().length() == 0)
			c2 = null;
		if (!TextUtil.strEqual(c1, c2))
			return false;

		if (manualEditApp != rhs.getManualEditApp())
			return false;

		return PropertiesUtil.propertiesEqual(props, rhs.props);
	}

	/** 
	 * Adds a property to this object's meta-data.
	 * @param name the property name.
	 * @param value the property value.
	 */
	public void setProperty(String name, String value)
	{
		props.setProperty(name, value);
	}

	/**
	 * Retrieve a property by name.
	 * @param name the property name.
	 * @return value of name property, or null if not defined.
	 */
	public String getProperty(String name)
	{
		return PropertiesUtil.getIgnoreCase(props, name);
	}

	/**
	 * @return enumeration of all names in the property set.
	 */
	public Enumeration getPropertyNames()
	{
		return props.propertyNames();
	}

	/**
	 * Removes a property assignment.
	 * @param name the property name.
	 */
	public void rmProperty(String name)
	{
		props.remove(name);
	}

	public Properties getProperties() { return props; }

	public void setProperties(Properties props) { this.props = props; }

	/**
	 * @return true if this is a manual edit application.
	 */
	public boolean getManualEditApp() { return manualEditApp; }

	/**
	 * Set flag indicating this is a manual edit application.
	 */
	public void setManualEditApp(boolean tf) { manualEditApp = tf; }

	@Override
	public DbKey getKey()
	{
		return getAppId();
	}

	@Override
	public String getUniqueName()
	{
		return getAppName();
	}

	@Override
	public void prepareForExec() throws IncompleteDatabaseException,
		InvalidDatabaseException
	{
	}

	@Override
	public boolean isPrepared()
	{
		return true;
	}

	@Override
	public void validate() throws IncompleteDatabaseException,
		InvalidDatabaseException
	{
	}

	@Override
	public void read() throws DatabaseException
	{
	}

	@Override
	public void write() throws DatabaseException
	{
		if (getDatabase() != null)
		{
			LoadingAppDAI loadingAppDAO = getDatabase().getDbIo().makeLoadingAppDAO();
			try
			{
				loadingAppDAO.writeComputationApp(this);
			}
			catch (DbIoException ex)
			{
				String msg = "Cannot write loading app '" + getAppName()
					+ "': " + ex;
				Logger.instance().warning(msg);
				throw new DatabaseException(msg, ex);
			}
			finally
			{
				loadingAppDAO.close();
			}
		}
	}

	@Override
	public PropertySpec[] getSupportedProps()
	{
		return propSpecs;
	}

	@Override
	public boolean additionalPropsAllowed()
	{
		return true;
	}

	public Date getLastModified()
	{
		return lastModified;
	}

	public void setLastModified(Date lastModified)
	{
		this.lastModified = lastModified;
	}
	
	/**
	 * Some processes are restricted by an 'allowedHosts' property. If no
	 * such property exists, or if it does and the local system's IP address
	 * or hostname is included in the list, then return true.
	 * If any error occurs while retrieving the local system's inet address
	 * or in comparing, then this method returns true.
	 * Otherwise return false.
	 * @return true if this process is allowed to run on the local system.
	 */
	public boolean canRunLocally()
	{
		String s = getProperty("allowedHosts");
		if (s == null)
			return true; // no restrictions
		try
		{
			InetAddress localHost = InetAddress.getLocalHost();
			if (localHost == null)
				return true;
			StringTokenizer st = new StringTokenizer(s, ", ");
			while(st.hasMoreTokens())
			{
				String h = st.nextToken();
				InetAddress ih = InetAddress.getByName(h);
				if (localHost.equals(ih))
					return true;
			}
			return false;
		}
		catch (Exception e)
		{
			Logger.instance().warning("canRunLocally() cannot check inet address: " + e);
			return true;
		}
	}
}
