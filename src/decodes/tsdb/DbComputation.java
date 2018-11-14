/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*  
*  $Log$
*  Revision 1.3  2018/05/01 17:39:26  mmaloney
*  sourceId is now a DbKey
*
*  Revision 1.2  2014/10/07 12:41:25  mmaloney
*  removed SeasonID
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.18  2013/08/18 19:48:45  mmaloney
*  Implement EffectiveStart/End relative properties
*
*  Revision 1.17  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*/
package decodes.tsdb;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Enumeration;
import java.util.Iterator;

import opendcs.dao.CachableHasProperties;

import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import decodes.db.Constants;
import decodes.sql.DbKey;
import decodes.tsdb.xml.CompXioTags;

/**
* This class holds the Meta-data record for a single computation.
* It also provides the methods for preparing a computation for
* execution (by instantiating and initializing it's algorithm
* object), and for applying the computation to a time series.
*/
public class DbComputation
	implements CompMetaData, CachableHasProperties
{
	/** Computation ID in database. */
	private DbKey computationId;
	
	/**
	 * Name of this computation as defined in the database.
	 * Note this should be a unique key also.
	 */
	private String name;
	
	/** Multi-line comment.  */
	private String comment;

	/** The loading application ID. */
	private DbKey appId;
	
	/** Last time this computation, or any of its constituents was changed. */
	private Date lastModified;
	
	/** True if this computation is enabled for execution. */
	private boolean enabled;

	/** Start of date range for which this computation is valid. */
	private Date validStart;

	/** End of date range for which this computation is valid. */
	private Date validEnd;

	/** Link to the algorithm's meta-data. */
	private DbCompAlgorithm algorithm;

	/** A list of this computation's parameters. */
	private ArrayList<DbCompParm> parmList;

	/**
	 * Properties from the meta-data CompProperty records.
	 */
	private Properties props;

	/**
	 * Instantiates and initializes the algorithm executive.
	 * This method is called once before the first time a computation
	 * is executed after it is loaded (or reloaded) from the database.
	 */
	private DbAlgorithmExecutive executive;

	/** Temporary storage for algorithmId during I/O from SQL database */
	DbKey algorithmId = Constants.undefinedId;

	/** Temporary storage for algorithm name during I/O from XML file */
	private String algorithmName;

	/** Temporary storage for application name during I/O from XML file */
	private String applicationName;

	/** Used during execution to correlate run id's between inputs & outputs. */
	private int modelRunId;
	
	/** Assigned when computation is initialized for exec. */
	private DbKey dataSourceId = DbKey.NullKey;

//	/** Indicates this is a transient generated computation for a group. */
//	private boolean _isTransient = false;

	/** 
	 * Temporary storage for the tasklist recnums that triggered this 
	 * execution of the computation.
	 */
	private HashSet<Integer> triggeringRecNums;
	
	/** New database attribute for db version 9 */
	private DbKey groupId = Constants.undefinedId;
	private TsGroup group = null;
	private String groupName = null;
	
	/** Temporary storage for HDB Convert2Group utility */
	public TimeSeriesIdentifier triggeringTsid = null;
	
	boolean isReloaded =false;
	
	/** For timed computations, compproc will use this transient field to track when to run. */
	private transient Date nextRunTime = null;

	/**
	 * Constructor. 
	 * @param id unique computation ID.
	 * @param name name of this computation.
	 */
	public DbComputation(DbKey id, String name)
	{
		computationId = id;
		this.name = name;
		algorithm = null;
		props = new Properties();
		comment = "";
		appId = Constants.undefinedId;
		lastModified = null;
		enabled = false;
		parmList = new ArrayList<DbCompParm>();
		executive = null;
		validStart = null;
		validEnd = null;
		algorithmId = Constants.undefinedId;
		algorithmName = null;
		modelRunId = Constants.undefinedIntKey;
		triggeringRecNums = new HashSet<Integer>();
		groupId = Constants.undefinedId;
		group = null;
		groupName = null;
	}

	/** Sets Computation ID. */
	public void setId(DbKey id) { computationId = id; }

	/** @return Computation ID in database. */
	public DbKey getId() { return computationId; }

	/** @return Computation name. */
	public String getName() { return name; }

	/** Sets the name */
	public void setName(String name) { this.name = name; }

	/** 
	 * Sets the Multi-line comment.
	 * @param x the comment
	 */
	public void setComment(String x)
	{
		comment = x;
	}

	/** @return the Multi-line comment. */
	public String getComment() { return comment; }

	/** @return the loading application ID. */
	public DbKey getAppId() { return appId; }

	/** 
	 * Sets the loading application ID. 
	 * @param x the app ID.
	 */
	public void setAppId(DbKey x)
	{
		appId = x;
	}
	
	/** 
	 * Set last modify time. 
	 * @param x the last modify time.
	 */
	public void setLastModified(Date x)
	{
		lastModified = x;
	}

	/** @return last modify time. */
	public Date getLastModified() { return lastModified; }
	
	/** @return True if this computation is enabled for execution. */
	public boolean isEnabled() { return enabled; }

	/** 
	 * Sets flag indicating whether this computation is enabled for execution.
	 * @param flag True if this computation is enabled for execution.
	 */
	public void setEnabled(boolean flag) { enabled = flag; }

	/** 
	 * Sets start of date range for which this computation is valid.
	 * If not null, this also clears any "EffectiveStart" property that was previously set.
	 * @param x the start of date range.
	 */
	public void setValidStart(Date x)
	{
		validStart = x;
		if (validStart != null)
			rmProperty("EffectiveStart");
	}

	/** @return start of date range for which this computation is valid. */
	public Date getValidStart() { return validStart; }

	/** 
	 * Sets end of date range for which this computation is valid. 
	 * If not null, this also clears any "EffectiveEnd" property that was previously set.
	 * @param x the end of date range.
	 */
	public void setValidEnd(Date x)
	{
		validEnd = x;
		if (validEnd != null)
			rmProperty("EffectiveEnd");
	}

	/** @return end of date range for which this computation is valid. */
	public Date getValidEnd() { return validEnd; }

	/**
	 * Sets link to the algorithm's meta-data. 
	 * @param algorithm the link.
	 */
	public void setAlgorithm(DbCompAlgorithm algorithm)
	{
		this.algorithm = algorithm;
		if (algorithm != null)
		{
			algorithmName = algorithm.getName();
			algorithmId = algorithm.getId();
		}
	}

	/** @return link to the algorithm's meta-data. */
	public DbCompAlgorithm getAlgorithm() { return algorithm; }

	/** @return algorithm ID link. */
	public DbKey getAlgorithmId()
	{
		if (algorithm != null)
			return algorithm.getId();
		else
			return algorithmId;
	}

	/** 
	 * Adds a property to this computation's meta-data.
	 * @param name the property name.
	 * @param value the property value.
	 */
	public void setProperty(String name, String value)
	{
		props.setProperty(name, value);
	}

	/**
	 * Search composite properties from algorithm with overrides in computation.
	 * @param name the property name.
	 * @return value of name property, or null if not defined.
	 */
	public String getProperty(String name)
	{
		String ret = PropertiesUtil.getIgnoreCase(props, name);
		if (ret == null && algorithm != null)
			ret = algorithm.getProperty(name);
		return ret;
	}

	/**
	 * @return enumeration of all names in the property set.
	 */
	public Enumeration getPropertyNames()
	{
		return props.propertyNames();
	}

	/**
	 * @return the properties as a set.
	 */
	public Properties getProperties() { return props; }

	/**
	 * Removes a property assignment.
	 * @param name the property name.
	 */
	public void rmProperty(String name)
	{
		PropertiesUtil.rmIgnoreCase(props, name);
	}

	/**
	 * Return a parameter by it's algorithm role.
	 * Note: role names are not case sensitive.
	 * @param role the role name.
	 * @return the DbCompParm playing that role in this computation, or null
	 *         if no match found.
	 */
	public DbCompParm getParm(String role)
	{
		for(DbCompParm ret : parmList)
		{
			if (role.equalsIgnoreCase(ret.getRoleName()))
				return ret;
		}
		return null;
	}

	public Iterator<DbCompParm> getParms()
	{
		return parmList.iterator();
	}
	
	public ArrayList<DbCompParm> getParmList()
	{
		return parmList;
	}

	/**
	 * Adds a parameter to this computation.
	 * If a parameter with the same role-name already exists, it is removed.
	 * @param parm the parameter
	 */
	public void addParm(DbCompParm parm)
	{
		rmParm(parm.getRoleName());
		parmList.add(parm);
	}

	/**
	 * If a parameter with role-name already exists, it is removed.
	 * @param role the parameter
	 */
	public void rmParm(String role)
	{
		DbCompParm old = getParm(role);
		if (old != null)
			parmList.remove(old);
	}

	/**
	 * For use by the editor.
	 */
	public void clearParms()
	{
		parmList.clear();
	}

	/**
	 * Delegates to the apply method of the DbAlgorithmExecutive.
	 * @param msg the data collection
	 * @param tsdb the database
	 * @throws DbCompException if apply fails.
	 * @throws DbIoException on IO error to database.
	 */
	public void apply( DataCollection msg, TimeSeriesDb tsdb )
		throws DbCompException, DbIoException
	{
		if (executive == null)
			throw new DbCompException("Computation '" + name 
				+ "' not initialized.");		
		executive.apply(msg);
	}

	/**
	 * Called when the comp-app periodically reloads a changed computation.
	 * Discarding the executive here will force it to be re-created and
	 * initialized with the new meta-data the next time it is executed.
	 */
	public void setUnPrepared()
	{
		executive = null;
	}
	
	/**
	 * Check last-modify time in DB & reload this computation if necessary.
	 * If 1st time since loaded, construct the DbAlgorithmExec object and 
	 * initialize it.
	 * @param tsdb the time series database.
	 * @throws DbCompException on any initialization failure.
	 * @throws DbIoException on database IO error.
	 * @throws NoSuchObjectException if this computation has been deleted
	 * from the database -- caller should remove it from its list.
	 */
	public void prepareForExec( TimeSeriesDb tsdb )
		throws DbCompException, DbIoException
	{
		if (algorithm == null)
			throw new DbCompException("Cannot prepare computation '" + name
				+ "': No algorithm assigned.");

		if (executive == null || this.isReloaded)
		{
			try
			{
				ClassLoader cl = Thread.currentThread().getContextClassLoader();
				String clsName = algorithm.getExecClass();
				Logger.instance().debug3("Instantiating new algo exec '" + clsName + "'");
				Class cls = cl.loadClass(clsName);
				executive = (DbAlgorithmExecutive)cls.newInstance();
				executive.init(this, tsdb);
				dataSourceId = tsdb.getDataSourceId(appId, this);
			}
			catch(DbCompException ex)
			{
				executive = null;
				String msg = "Cannot prepare computation '"	+ name + "' with algo exec class '"
					+ algorithm.getExecClass() + "': " + ex;
				Logger.instance().warning(msg);
				throw ex;
			}
			catch(Exception ex)
			{
				executive = null;
				String msg = "Cannot prepare computation '"	+ name + "' with algo exec class '"
					+ algorithm.getExecClass() + "': " + ex;
				System.err.println(msg);
				ex.printStackTrace();
				throw new DbCompException(msg);
			}
		}
	}

	/** @see decodes.tsdb.CompMetaData */
	public String getObjectType() { return CompXioTags.computation; }

	/** @see decodes.tsdb.CompMetaData */
	public String getObjectName() { return name; }

	/**
	 * Used during I/O from XML file for temporary storage of algorithm
	 * name, prior to full association with an algorithm object.
	 */
	public void setAlgorithmName(String nm)
	{
		algorithmName = nm;
	}

	/**
	 * Used during I/O from XML file for temporary storage of algorithm
	 * name, prior to full association with an algorithm object.
	 */
	public String getAlgorithmName()
	{
		if (algorithm != null)
			return algorithm.getName();
		return algorithmName; 
	}

	/**
	 * Used during I/O from XML file for temporary storage of algorithm
	 * name, prior to full association with an algorithm object.
	 */
	public void setApplicationName(String nm)
	{
		applicationName = nm;
	}

	/**
	 * Used during I/O from XML file for temporary storage of algorithm
	 * name, prior to full association with an algorithm object.
	 */
	public String getApplicationName() { return applicationName; }

	/**
	 * @return the model run ID being used in this execution of the computation.
	 */
	public int getModelRunId()
	{
		return modelRunId;
	}

	/**
	 * Sets the model run ID to be used in this execution of the computation.
	 * @param mri the model run id.
	 */
	public void setModelRunId(int mri)
	{
		modelRunId = mri;
	}

	/**
	 * For use by the editor, create a copy of this object, but with no ID.
	 */
	public DbComputation copyNoId()
	{
		DbComputation dc = new DbComputation(Constants.undefinedId, name);
		dc.comment = this.comment;
		dc.appId = this.appId;
		dc.enabled = this.enabled;
		dc.validStart = this.validStart;
		dc.validEnd = this.validEnd;
		dc.algorithm = this.algorithm;
		dc.executive = this.executive;
		dc.algorithmId = this.algorithmId;
		dc.algorithmName = this.algorithmName;
		dc.applicationName = this.applicationName;
		dc.groupId = this.groupId;
		dc.groupName = this.groupName;
		dc.group = this.group;
		
		PropertiesUtil.copyProps(dc.props, this.props);

		for(DbCompParm parm : parmList)
			dc.addParm(new DbCompParm(parm));

		return dc;
	}

	/**
	 * For use by the editor to detect changes, return true if the passed
	 * object is equal to this one.
	 */
	public boolean equalsNoId(DbComputation rhs)
	{
		if (!TextUtil.strEqual(name, rhs.name))
			return false;
		if (!TextUtil.strEqual(comment, rhs.comment))
			return false;
		if (this.appId != rhs.appId)
			return false;
		if (this.enabled != rhs.enabled)
			return false;
		if (this.algorithmId != rhs.algorithmId)
			return false;
		if (!TextUtil.strEqual(this.algorithmName, rhs.algorithmName))
			return false;
//Logger.instance().debug3("DbComputation.equalsNoId 1");
		if (!TextUtil.strEqual(this.applicationName, rhs.applicationName))
			return false;
		if (!PropertiesUtil.propertiesEqual(props, rhs.props))
			return false;
//Logger.instance().debug3("DbComputation.equalsNoId 2");

		// validStart & validEnd may be null:
		boolean eq = !(this.validStart==null ^ rhs.validStart==null) 
		   && (this.validStart==null || this.validStart.equals(rhs.validStart));
		if (!eq)
			return false;
		eq = !(this.validEnd==null ^ rhs.validEnd==null) 
		   && (this.validEnd==null || this.validEnd.equals(rhs.validEnd));

//Logger.instance().debug3("DbComputation.equalsNoId 3");
		if (parmList.size() != rhs.parmList.size())
			return false;
//Logger.instance().debug3("DbComputation.equalsNoId 3.1");
		for(int i=0; i<parmList.size(); i++)
		{
			DbCompParm p1 = parmList.get(i);
			DbCompParm p2 = rhs.getParm(p1.getRoleName());
			if (!p1.equals(p2))
				return false;
		}
//Logger.instance().debug3("DbComputation.equalsNoId 4");
		
		if (this.groupId != rhs.groupId)
			return false;
//Logger.instance().debug3("DbComputation.equalsNoId 5: this.groupName='" + this.groupName + "' rhs.groupName='" + rhs.groupName + "'");
		if (!TextUtil.strEqual(this.groupName, rhs.groupName))
			return false;
		
		return true;
	}

	public void setAlgorithmId(DbKey algorithmId)
	{
		this.algorithmId = algorithmId;		
	}

	public DbKey getDataSourceId() { return dataSourceId; }

	public void shutdown()
	{
		if (executive != null)
			executive.shutdown();
	}

	/**
	 * @return true if this computation has any group inputs.
	 */
	public boolean hasGroupInput()
	{
		return groupId != Constants.undefinedId;
	}
	
//	public boolean isTransient() { return _isTransient; }
//
//	public void setIsTransient(boolean tf ) { _isTransient = tf; }

	public DbAlgorithmExecutive getExecutive() { return executive; }

	public HashSet<Integer> getTriggeringRecNums() { return triggeringRecNums;}

	public DbKey getGroupId()
	{
		return groupId;
	}

	public void setGroupId(DbKey groupId)
	{
		this.groupId = groupId;
	}
	
	/**
	 * Sets the Time Series Group used to determine input & output param components.
	 * @param group the group
	 */
	public void setGroup(TsGroup group)
	{
		this.group = group;
		if (group != null)
		{
			groupName = group.getGroupName();
			groupId = group.getGroupId();
		}
		else
		{
			groupId = Constants.undefinedId;
			groupName = null;
		}
	}
	
	/** @return the time series group to use for inputs & outputs */
	public TsGroup getGroup() { return group; }
	
	/**
	 * @return the name of the group for this comp, or null if none.
	 */
	public String getGroupName()
	{
		if (group != null)
			return group.getGroupName();
		return groupName; 
	}

	/**
	 * Return the missing action associated with the named input parm.
	 * This is set from the parmname_MISSING property.
	 * @param parmName the input parameter rolename
	 * @return the MissingAction (default is FAIL if no property).
	 */
	public MissingAction getMissingAction(String parmName)
	{
		String s = getProperty(parmName + "_MISSING");
		return MissingAction.fromString(s);
	}
	
	/**
	 * Return the units abbreviation for the specified parameter, or
	 * null if none is defined.
	 * This is stored in the parmname_EU property.
	 * @param parmName the parameter role name
	 * @return the units or null if none defined.
	 */
	public String getUnitsAbbr(String parmName)
	{
		return getProperty(parmName + "_EU");
	}
	
	@Override
	public DbKey getKey()
	{
		return getId();
	}

	@Override
	public String getUniqueName()
	{
		return getName();
	}

	/**
	 * If this is a timed computation, compproc uses nextRunTime to track when to next
	 * run the computation. This is a transient field. (not stored in db)
	 * @return The next run time or null if this is not a timed computation.
	 */
	public Date getNextRunTime()
	{
		return nextRunTime;
	}

	/**
	 * CompProc uses this to track when to next run a timed (non-triggered) computation.
	 * @param nextRunTime
	 */
	public void setNextRunTime(Date nextRunTime)
	{
		this.nextRunTime = nextRunTime;
	}
}
