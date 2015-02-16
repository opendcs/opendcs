/*
*  $Id$
*
*  $Log$
*  Revision 1.3  2014/12/11 20:21:24  mmaloney
*  Removed duplicate PropertySpec
*
*  Revision 1.2  2014/10/02 14:33:13  mmaloney
*  Conditional Season Processing
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.15  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*  Revision 1.14  2011/06/21 15:19:35  shweta
*  added diagmostic and timeout properties
*
*  Revision 1.13  2011/03/16 19:37:15  shweta
*  Added method to get diagnostic and timeout properties
*
*  Revision 1.12  2011/01/07 16:01:56  mmaloney
*  bugfixes
*
*  Revision 1.11  2009/10/20 15:47:12  mjmaloney
*  SFWMD debug
*
*  Revision 1.10  2009/10/12 18:51:33  mjmaloney
*  Import for SFWMD
*
*  Revision 1.9  2009/10/12 18:36:56  mjmaloney
*  Import for SFWMD
*
*  Revision 1.8  2009/10/12 18:20:42  mjmaloney
*  Import for SFWMD
*
*  Revision 1.7  2009/10/07 17:37:31  mjmaloney
*  added getTM iterator method.
*
*  Revision 1.6  2009/04/30 15:22:11  mjmaloney
*  Iridium updates
*
*  Revision 1.5  2008/11/20 18:49:20  mjmaloney
*  merge from usgs mods
*
*/

package decodes.db;

import java.util.*;

import ilex.util.*;
import decodes.decoder.FieldParseException;
import decodes.decoder.Season;
import decodes.sql.DbKey;
import decodes.util.DecodesSettings;
import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;

/**
 * This encapsulates information about a single DCP platform.
 * The RecordList to which this platform belongs is a data member
 * of this Database's PlatformList object.
 */
public class Platform
	extends IdDatabaseObject
	implements HasProperties, PropertiesOwner
{
	// _id is stored in the IdDatabaseObject superclass.

	/** For the database editor to associate platforms to configs by name only. */
	public static boolean configSoftLink = false;

	/**
	* This Platform's description.  This always contains the description,
	* regardless of whether this Platform's data is complete or not.
	* This might be null, if there is no description.
	*/
	public String description;

	/**
	* This Platform's agency.  This always contains the agency name,
	* regardless of whether this Platform's data is complete or not.
	*/
	public String agency;

	/**
	* Whether this data should be "published" to the installed database.
	* This only contains a valid value when this Platform's data has been
	* completely read from the database.
	*/
	public boolean isProduction;

	/**
	* This gives this Platform object's expiration date.  This will be null
	* if the Platform doesn't have an expiration date.
	*/
	public Date expiration;

	/**
	* The time that this record in the database was last modified.  This will
	* be null if that's not known.
	*/
	public Date lastModifyTime;

	/**
	* The name of the PlatformConfig associated with this Platform.
	*/
	private String configName;


	/**
	* The Site at which this Platform resides.  This can be null, if the
	* Site is unknown.
	*/
	public Site site;

	/**
	* The PlatformConfig associated with this Platform.
	* Note that this will be null in two cases:  1, there is no PlatformConfig
	* associated with this Platform, or 2, the config's name hasn't been
	* resolved into a reference yet.
	*/
	private PlatformConfig platformConfig;

	/** Holds the list of TransportMediums associated with this Platform. */
	public Vector<TransportMedium> transportMedia;

	/** Stores the PlatformSensors associated with this Platform. */
	public Vector<PlatformSensor> platformSensors;

	/**
	* This will be true when this Platform's data has been completely read
	* from the database.  This is set by Platform.read() method.
	*/
	private boolean isReadComplete;

	/**
	* A list of properties for this Platform.
	* This is never null.
	*/
	private Properties properties = new Properties();

	/**
	* The platform designator that, along with site ID, uniquely identifies
	* a platform in the database. By default, it is null, meaning undefined.
	*/
	private String platformDesignator;

	private PropertySpec[] platformPropSpecs =
	{
		new PropertySpec("debugLevel", PropertySpec.INT,
			"(default=0) Set to 1, 2, 3 for increasing levels of debug information" +
			" when this platform is decoded."),
		new PropertySpec("ignoreSeason", PropertySpec.DECODES_ENUM + Constants.enum_Season,
			"Set to have this platform ignored during a specified season."),
		new PropertySpec("processSeason", PropertySpec.DECODES_ENUM + Constants.enum_Season,
			"Set to have this platform only processed during a specified season."),
		new PropertySpec("pollPriority", PropertySpec.INT,
			"(default=3) For polled stations, this determines the order in which "
			+ "they will be polled (1 = highest priority = polled first)")
	};
	
	// Populated during prepareForExec:
	private Season ignoreSeason = null;
	private Season processSeason = null;
	
	/**
	* No-arg constructor.  This creates a new Platform which belongs to
	* the current databsae.
	*/
	public Platform()
	{
		super(); // sets _id to Constants.undefinedId;
		clear();
	}

	/**
	* Construct with just an ID.  This creates a new Platform which
	* belongs to the current database.
	  @param id the database ID
	*/
	public Platform(DbKey id)
	{
		this();
		try { this.setId(id); }
		catch(DatabaseException ex) {} // won't happen.
	}

	/** Clears the platform data back to its originally created empty state. */
	void clear()
	{
		agency = null;
		description = null;
		lastModifyTime = null;
		isProduction = false;
		expiration = null;
		site = null;
		platformConfig = null;
		platformSensors = new Vector<PlatformSensor>();
		transportMedia = new Vector<TransportMedium>();
		isReadComplete = false;
		configName = null;
		properties.clear();
		platformDesignator = null;
	}

	/**
	* Makes a name suitable for display or filename.
	* The name is built from the site name, using the preferred name type.
	  @return String suitable as a file name
	*/
	public String makeFileName()
	{
		String ret = (site == null) ? "unknownSite" : getSiteName(false);
		if (platformDesignator != null && platformDesignator.length() > 0)
			ret = ret + "-" + platformDesignator;
		return ret;
	}

	/** @return name with the type-prefix. */
	public String getSiteName()
	{
		return getSiteName(true);
	}

	/** @return the site object or null if none assigned. */
	public Site getSite() { return site; }

	/**
	  Sets the site association for this platform.
	  @param site the site
	*/
	public void setSite(Site site)
	{
		this.site = site;
	}

	/** 
	  Returns name with or without the name type prefix. 
	  @param prefixWithType true if you want type prefix in returned string.
	  @return name with or without the name type prefix. 
	*/
	public String getSiteName(boolean prefixWithType)
	{
		if (site == null)
			return "";
		SiteName sn = site.getPreferredName();
		if ( sn == null )
			return "";
		else if (!prefixWithType)
			return site.getPreferredName().getNameValue();
		else
			return site.getPreferredName().makeFileName();
	}

	/**
	  @return transport ID of the 'preferred' type as set in your
	  decodes properties file, or any ID if preferred not found, or
	  null if no IDs are present.
	*/
	public String getPreferredTransportId()
	{
		int n = transportMedia.size();
		for(int i=0; i<n; i++)
		{
			TransportMedium tm = transportMedia.elementAt(i);
			if (tm.getMediumType().equalsIgnoreCase(
				DecodesSettings.instance().transportMediumTypePreference))
				return tm.getMediumId();
		}
		if (n > 0)
		{
			TransportMedium tm = transportMedia.elementAt(0);
			return tm.getMediumId();
		}
		else return "unknown";
	}
  /**
    @return next sequence number for the specified type  of transport id --
		pattern is
				 <deviceid>-<sequence number>
    return -1 if no transport id of this type is found.  
  */
	/* SED - 05/15/2008 */
	
	public int  getNextTransportIdSequenceNo()          
	{
		int maxseq = -1;
		int seq = -1;
		int n = transportMedia.size();
		for(int i=0; i<n; i++)
		{
			TransportMedium tm = transportMedia.elementAt(i);
			if ( !tm.getMediumType().matches("*goes*") ) {
			  String mid = tm.getMediumId();
				String[] comp = mid.split("-");
				if ( comp.length == 3 ) {
					seq = Integer.parseInt(comp[2]);
					if ( seq > maxseq ) 
						maxseq = seq;
				}
			}
		}
		if ( maxseq != -1 )
			maxseq++;
		return(maxseq);
	}

	/**
	* Returns the transport medium with the specified type, if there
	* is one.  If not, this returns null.
	  @param type the transport medium type to retrieve.
	  @return the transport medium with the specified type, or null if none.
	*/
	public TransportMedium getTransportMedium(String type)
	{
		for(Iterator<TransportMedium> it = transportMedia.iterator();
			it.hasNext(); )
		{
			TransportMedium tm = it.next();
			if (tm.getMediumType().equalsIgnoreCase(type))
				return tm;
		}
		return null;
	}

	/**
	 * Convenience method to return GOES DCP address as a string, or null
	 * if there is no GOES transport medium.
	 */
	public String getDcpAddress()
	{
		TransportMedium tm = getTransportMedium(Constants.medium_Goes);
		if (tm == null)
			tm = getTransportMedium(Constants.medium_GoesST);
		if (tm == null)
			tm = getTransportMedium(Constants.medium_GoesRD);
		if (tm == null)
			tm = getTransportMedium(Constants.medium_IRIDIUM);
		if (tm == null)
			return null;
		return tm.getMediumId();
	}

	/**
	  Get the PlatformConfig associated with this Platform.
	  If configSoftLink is true, the config with a matching name is returned.
	  Else If a hard platformConfig association has been made, 
	  the config is returned.
	  Else null is returned.
	  @return the configuration.
	*/
	public PlatformConfig getConfig()
	{
		if (configSoftLink)
			return Database.getDb().platformConfigList.get(configName);

		return platformConfig;
	}

	/**
	* Set the PlatformConfig associated with this Platform.
	*/
	public void setConfig(PlatformConfig pc)
	{
		platformConfig = pc;
	}

	/**
	* Get the PlatformConfig's name.  If there is no PlatformConfig yet
	* associated with this Platform, this returns "unknown".
	  @return configuration's name
	*/
	public String getConfigName()
	{
		if (configName != null)
			return configName;
		if (platformConfig == null)
			return "unknown";
		return platformConfig.configName == null ? "none" :
			platformConfig.configName;
	}

	/**
	* Set the name which refers to the PlatformConfig associated with this
	* Platform.
	  @param n the config name
	*/
	public void setConfigName(String n)
	{
		configName = n;
	}

	/** Returns "Platform" */
	public String getObjectType() {
		return "Platform";
	}


	/**
	  Prepares this platform, and all its subordinate objects for execution
	  by a routing specification.
	*/
	public void prepareForExec()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		// Make sure we have the complete object from the run-time database.
		if (!isComplete())
		{
			try { read(); }
			catch(DatabaseException e)
			{
				if (e instanceof IncompleteDatabaseException)
					throw (IncompleteDatabaseException)e;
				else if (e instanceof InvalidDatabaseException)
					throw (InvalidDatabaseException)e;
				else
					throw new InvalidDatabaseException(e.toString());
			}
		}
		PlatformConfig pc = getConfig();
		if (pc == null)
			throw new InvalidDatabaseException(
				"Platform '" + makeFileName() + "' has no PlatformConfig");

		pc.prepareForExec();

		for(Iterator<TransportMedium> it = transportMedia.iterator(); 
			it.hasNext(); )
		{
			// Prepare the TM, which makes its link to the DecodesScript
			TransportMedium tm = it.next();
			tm.prepareForExec();
		}
		
		ignoreSeason = processSeason = null;
		DbEnum seasonEnum = Database.getDb().enumList.getEnum(Constants.enum_Season);
		if (seasonEnum != null)
		{
			String seasonAbbr = getProperty("ignoreSeason");
			if (seasonAbbr != null && seasonAbbr.trim().length() > 0)
			{
				EnumValue ev = seasonEnum.findEnumValue(seasonAbbr);
				if (ev == null)
					Logger.instance().warning("Platform " + getDisplayName()
						+ " Unknown 'ignoreSeason' property value '" + seasonAbbr + "'");
				else
				{
					try
					{
						ignoreSeason = new Season(ev);
					}
					catch (FieldParseException ex)
					{
						Logger.instance().warning("Platform " + getDisplayName()
							+ " ignoreSeason: " + ex);
						ignoreSeason = null;
					}
				}
			}
			if ((seasonAbbr = getProperty("processSeason")) != null && seasonAbbr.trim().length() > 0)
			{
				EnumValue ev = seasonEnum.findEnumValue(seasonAbbr);
				if (ev == null)
					Logger.instance().warning("Platform " + getDisplayName()
						+ " Unknown 'processSeason' property value '" + seasonAbbr + "'");
				else
				{
					try
					{
						processSeason = new Season(ev);
					}
					catch (FieldParseException ex)
					{
						Logger.instance().warning("Platform " + getDisplayName()
							+ " processSeason: " + ex);
						processSeason = null;
					}
				}
			}
		}
	}

	/** 
	  @return true if this platform has previously been prepared 
	  for execution. 
	*/
	public boolean isPrepared()
	{
		PlatformConfig pc = getConfig();
		return isComplete()
			&& pc != null
			&& pc.isPrepared();
	}

	/**
	* From DatabaseObject interface, this, currently, does nothing.
	*/
	public void validate()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
	}

	/**
	* Overrides the DatabaseObject's method; this reads this Platform
	* from the database. For SQL, this uses this object's _id member to
	* uniquely identify the record in the database.
	*/
	public void read()
		throws DatabaseException
	{
		myDatabase.getDbIo().readPlatform(this);
		isReadComplete = true;
	}

	/**
	* Overrides the DatabaseObject's method; this writes this Platform
	* to the database.  For SQL, this uses this object's _id member to
	* uniquely identify the record in the database.
	*/
	public void write()
		throws DatabaseException
	{
		if (site == null)
			throw new DatabaseException(
				"Cannot save platform without site assignment.");
		lastModifyTime = new Date();
		myDatabase.getDbIo().writePlatform(this);
	}

	/**
	* This makes a copy of the Platform without copying its ID value.
	* This is used, for example, when making a historical version.
	  @return copy of this platform but without ID set.
	*/
	public Platform noIdCopy()
	{
		Platform ret = copy();
		ret.clearId();

		return ret;
	}

	/**
	* Copies this Platform.  This is used (perhaps among other places) when
	* opening a Platform edit panel.  The user edits a copy of the Platform.
	* When the user closes the panel, the software compares the original
	* with the copy to determine if anything has changed.
	  @return deep copy of this platform
	*/
	public Platform copy()
	{
		Platform ret = new Platform(getId());
		ret.copyFrom(this);
		return ret;
	}

	/**
	 * Make this Platform a copy of the passed platform, except for Database
	 * and surrogate keys.
	 * @param rhs the platform we're copying from.
	 */
	public void copyFrom(Platform rhs)
	{
		this.description = rhs.description;
		this.agency = rhs.agency;
		this.isProduction = rhs.isProduction;
		this.lastModifyTime = rhs.lastModifyTime;
		this.expiration = rhs.expiration;
		this.site = rhs.site;
		this.platformConfig = rhs.platformConfig;
		this.configName = rhs.configName;
		this.platformDesignator = rhs.platformDesignator;
		this.properties = (Properties)rhs.properties.clone();

		this.transportMedia.clear();
		for(int i = 0; i < rhs.transportMedia.size(); i++)
		{
			TransportMedium tm = rhs.transportMedia.elementAt(i);
			TransportMedium ntm = tm.copy();
			tm.platform = this;
			this.transportMedia.add(ntm);
		}

		this.platformSensors.clear();
		for(int i = 0; i < rhs.platformSensors.size(); i++)
		{
			PlatformSensor ps = rhs.platformSensors.elementAt(i);
			PlatformSensor nps = ps.copy(this);
			this.platformSensors.add(nps);
		}
	}

	/**
	* Returns the PlatformSensor object corresponding to the passed sensor
	* number, or null if none.
	  @return PlatformSensor by number, or null if no-such-sensor.
	*/
	public PlatformSensor getPlatformSensor(int sensorNum)
	{
		int n = platformSensors.size();
		for(int i=0; i<n; i++)
		{
			PlatformSensor ps = platformSensors.elementAt(i);
			if (ps.sensorNumber == sensorNum)
				return ps;
		}
		return null;
	}

	/**
	 * An 'equals' method for use by import programs.
	 * - Does deep compare through site constituents.
	 * - Ignores differences in platform ID & isProduction.
	 * @param op the imported platform we are comparing to.
	 * @return
	 */
	public boolean importEquals(Platform op)
	{
		if (this == op)
			return true;

		Logger.instance().debug3("Comparing '" + makeFileName()
			+ "' to imported '" + op.makeFileName() + "'");
		if ((site == null && op.site != null)
		 || (site != null && op.site == null)
		 || (site != null && !site.importEquals(op.getSite())))
		{
			Logger.instance().debug3("Sites differ");
			return false;
		}

		if (!TextUtil.strEqual(description, op.description))
		{
			Logger.instance().debug3("descriptions differ");
			return false;
		}
		if (!TextUtil.strEqualIgnoreCase(this.getConfigName(), op.getConfigName()))
		{
//			Logger.instance().debug3("ConfigNames differ");
			return false;
		}

		if (!TextUtil.strEqualIgnoreCase(platformDesignator, 
			op.platformDesignator))
		{
			Logger.instance().debug3("Designators differ");
			return false;
		}

		if (!eqChk(agency, op.agency))
		{
			Logger.instance().debug3("agency differs");
			return false;
		}
		if (!eqChk(expiration, op.expiration))
		{
			Logger.instance().debug3("expiration differs");
			return false;
		}

		if (transportMedia.size() != op.transportMedia.size())
		{
			Logger.instance().debug3("tm number differs");
			return false;
		}

		for(TransportMedium tm : transportMedia)
		{
			TransportMedium optm = op.getTransportMedium(tm.getMediumType());
			if (optm == null)
			{
				Logger.instance().debug3("This has TM type " + optm.getMediumType()
					+ ", but imported does not.");
				return false;
			}
			if (!tm.equals(optm))
			{
				Logger.instance().debug3("TM type " + optm.getMediumType()
					+ "differs.");
				return false;
			}
		}

		int numthis = 0;
		for(int i = 0; i<platformSensors.size(); i++)
			if (!(platformSensors.elementAt(i)).isEmpty())
				numthis++;
		int numthat = 0;
		for(int i = 0; i<op.platformSensors.size(); i++)
			if (!(op.platformSensors.elementAt(i)).isEmpty())
				numthat++;

		if (numthis != numthat)
		{
			Logger.instance().debug3("Number of platform sensors differ. "
				+ "(this has " + numthis + ", that has " + numthat + ")");
			return false;
		}
		int n1=platformSensors.size();
		for(int i=0; i<n1; i++)
		{
			PlatformSensor thisps =(PlatformSensor)platformSensors.elementAt(i);
			PlatformSensor thatps = op.getPlatformSensor(thisps.sensorNumber);

			if (thisps.isEmpty())
			{
				if (thatps == null || thatps.isEmpty())
					continue;
				else
				{
					Logger.instance().debug3("Platform sensor " + thisps.sensorNumber + " differs(a)");
					return false;
				}
			}
			else
			{
				if (thatps == null || thatps.isEmpty())
				{
					Logger.instance().debug3("Platform sensor " + thisps.sensorNumber + " differs(b)");
					return false;
				}
				else if (!thisps.equals(thatps))
				{
					Logger.instance().debug3("Platform sensor " + thisps.sensorNumber + " differs(c)");
					return false;
				}
			}
		}

		// Check properties for equality.
		if (!PropertiesUtil.propertiesEqual(properties, op.properties))
		{
			Logger.instance().debug3("Platform Properties differ");
			return false;
		}

		if (platformConfig != null && !platformConfig.equals(
			op.platformConfig))
		{
			Logger.instance().debug3("Configs differ");
			return false;
		}
		return true;
	
	}
	
	/**
	* @return true if the passed platform contains the same data as this one.
	*/
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!(obj instanceof Platform))
			return false;

		Platform p = (Platform)obj;
		if (this.site != p.site
		 || this.platformConfig != p.platformConfig
		 || isProduction != p.isProduction
		 || site != p.site)
			return false;

		if (!TextUtil.strEqual(description, p.description))
			return false;
		if (!TextUtil.strEqualIgnoreCase(configName, p.configName))
			return false;

		if (!TextUtil.strEqualIgnoreCase(platformDesignator, 
			p.platformDesignator))
			return false;

		if (!eqChk(agency, p.agency)
		 || !eqChk(expiration, p.expiration)
		 || !eqChk(lastModifyTime, p.lastModifyTime))
			return false;

		if (transportMedia.size() != p.transportMedia.size())
			return false;

		int n=transportMedia.size();
		for(int i=0; i<n; i++)
			if (!transportMedia.elementAt(i).equals(
				p.transportMedia.elementAt(i)))
				return false;

		int numthis = 0;
		for(int i = 0; i<platformSensors.size(); i++)
			if (!(platformSensors.elementAt(i)).isEmpty())
				numthis++;
		int numthat = 0;
		for(int i = 0; i<p.platformSensors.size(); i++)
			if (!((PlatformSensor)p.platformSensors.elementAt(i)).isEmpty())
				numthat++;

		if (numthis != numthat)
			return false;
		int n1=platformSensors.size();
		for(int i=0; i<n1; i++)
		{
			PlatformSensor thisps = platformSensors.elementAt(i);
			PlatformSensor thatps = p.getPlatformSensor(thisps.sensorNumber);

			if (thisps.isEmpty())
			{
				if (thatps == null || thatps.isEmpty())
					continue;
				else
					return false;
			}
			else
			{
				if (thatps == null || thatps.isEmpty())
					return false;
				else if (!thisps.equals(thatps))
					return false;
			}
		}

		// Check properties for equality.
		if (this.properties.size() != p.properties.size())
			return false;
		for(Enumeration it = this.properties.propertyNames(); 
			it.hasMoreElements(); )
		{
			String nm = (String)it.nextElement();
			String v1 = this.properties.getProperty(nm);
			String v2 = p.properties.getProperty(nm);
			if (!TextUtil.strEqual(v1, v2))
				return false;
		}
		return true;
	}

	/**
	 * Adds this sensor to the vector. If it was already there (with same
	 * sensor number) it is replaced.
	  @param ps the sensor to add
	 */
	public void addPlatformSensor(PlatformSensor ps)
	{
		int n=platformSensors.size();
		int pos = n;
		for(int i=0; i<n; i++)
		{
			PlatformSensor t = platformSensors.elementAt(i);
			if (t.sensorNumber == ps.sensorNumber)
			{
				pos = i;
				platformSensors.remove(t);
				break;
			} else if ( ps.sensorNumber < t.sensorNumber ) {
				pos = i;
				break;
			}
		}
		platformSensors.add(pos, ps);
	}

	/**
	* @return an iterator over this Platform's
	*/
	public Iterator<PlatformSensor> getPlatformSensors()
	{
		return platformSensors.iterator();
	}

	/**
	* @return true if this object has been completely read from the
	* database, or false if not.
	*/
	public boolean isComplete() {
		return isReadComplete;
	}

	/**
	  Sets the isReadComplete flag.
	  Used in the PlatformWizard to force a complete platform to be re-read
	  from the database.
	  @param tf the new flag value
	*/
	public void setIsComplete(boolean tf)
	{
		isReadComplete = tf;
	}

	/**
	 * Sets the Platform Designator.
	 * @param des the designator.
	 */
	public void setPlatformDesignator(String des)
	{
		platformDesignator = des;
	}

	/**
	 * Gets the Platform Designator.
	 * @return the designator.
	 */
	public String getPlatformDesignator()
	{
		return platformDesignator;
	}

	/**
	 * @return the agency name.
	 */
	public String getAgency()
	{
		return agency;
	}
	
	/**
	 * Sets the agency name.
	 * @param agency the agency
	 */
	public void setAgency(String agency)
	{
		this.agency = agency;
	}
	
	/**
	 * @return the description.
	 */
	public String getDescription()
	{
		return description;
	}

	public String getBriefDescription()
	{
		if (description == null)
			return "";
		int i=0;
		for(; i<description.length() && i < 60; i++)
		{
			char c = description.charAt(i);
			if (c == '.' || c == '\n' || c == '\r')
				break;
		}
		return description.substring(0, i);
	}

	/**
	 * Sets the description.
	 * @param description the description
	 */
	public void setDescription(String description)
	{
		this.description = description;
	}
	
	public String getDisplayName()
	{
		return makeFileName();
	}
	
	public Iterator<TransportMedium> getTransportMedia()
	{
		return transportMedia.iterator();
	}

	@Override
	public PropertySpec[] getSupportedProps()
	{
		return platformPropSpecs;
	}

	@Override
	public boolean additionalPropsAllowed()
	{
		// Allow additional props not defined herein.
		return true;
	}
	
	public Properties getProperties()
	{
		return properties;
	}

	@Override
	public void setProperty(String name, String value)
	{
		PropertiesUtil.rmIgnoreCase(properties, name);
		properties.setProperty(name, value);
	}

	@Override
	public String getProperty(String name)
	{
		return PropertiesUtil.getIgnoreCase(properties, name);
	}

	@Override
	public Enumeration getPropertyNames()
	{
		return properties.keys();
	}

	@Override
	public void rmProperty(String name)
	{
		PropertiesUtil.rmIgnoreCase(properties, name);
	}
	
	/**
	 * If a debug level is assigned to this platform or site, return it.
	 * @return debug level (1...3) if a "debugLevel" properties is assigned
	 * to this site, or 0 if none.
	 */
	public int getDebugLevel()
	{
		String s = PropertiesUtil.getIgnoreCase(properties, "debugLevel");
		if (s != null)
		{
			try { return Integer.parseInt(s.trim()); }
			catch(Exception ex) {}
		}
		if (site != null && (s = site.getProperty("debugLevel")) != null)
		{
			try { return Integer.parseInt(s.trim()); }
			catch(Exception ex) {}
		}
		return 0;
	}

	public Season getIgnoreSeason()
	{
		return ignoreSeason;
	}

	public Season getProcessSeason()
	{
		return processSeason;
	}
}
