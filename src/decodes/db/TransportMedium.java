/*
*  $Id$
*  
*  Open Source Software
*  
*  $Log$
*  Revision 1.3  2014/08/29 18:24:35  mmaloney
*  6.1 Schema Mods
*
*  Revision 1.2  2014/08/22 17:23:05  mmaloney
*  6.1 Schema Mods and Initial DCP Monitor Implementation
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.5  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*/
package decodes.db;

import ilex.util.Logger;
import ilex.util.TextUtil;

/**
Holds all attributes for a Transport Medium
*/
public class TransportMedium extends DatabaseObject
{
	/**
	* The type of this TransportMedium.  This should match one of
	* the TransportMediumType enum values.
	*/
	private String mediumType;

	/**
	* This identifies this particular TransportMedium among all the
	* TransportMedia of this type.  An example of a GOES ID is
	* "CE458E08".  If the medium type is GOES-Random or GOES-Self-Timed,
	* then case is not significant, and this data member is stored in
	* all-uppercase.
	*/
	private String mediumId;

	/**
	* The channel number -- this is used by GOES-type TransportMediums.
	* If this is not used, it will have a value of -1.
	*/
	public int channelNum;

	/**
	* The time of the first transmission, as the second of the day.
	* If this is not used for a particular TransportMedium, it will
	* have a value of -1.
	*/
	public int assignedTime;

	/**
	* Window(?), in number of seconds
	* If this is not used for a particular TransportMedium, it will
	* have a value of -1.
	*/
	public int transmitWindow;

	/**
	* Interval between transmissions, in seconds.
	* If this is not used for a particular TransportMedium, it will
	* have a value of -1.
	*/
	public int transmitInterval;


	/**
	* This is a reference to the Platform to which this TransportMedium
	* applies.  This is never null.
	*/
	public Platform platform;

	/**
	  The name of the script to use to decode data from this TM.
	  This is a 'weak' link to a DECODES script. It is not evaluated until
	  necessary.
	*/
	public String scriptName;

	/**
	  This is the 'hard' reference to the DecodesScript used to decode the
	  data associated with this TransportMedium. It is not evaluated until
	  necessary to decode data.
	*/
	private DecodesScript decodesScript;

	/**
	* A reference to the EquipmentModel associated with this
	* TransportMedium.
	*/
	public EquipmentModel equipmentModel;

	/** Not implemented yet (?)  */
	public PlatformConfig performanceMeasurements;

	/** preamble is a GOES parameter, either S=Short or L=Long. */
	private char preamble;

	/**
	  Value added to GOES message time prior to computing sample times.
	  Defaults to 0. This value accomodates those platforms that take
	  sensor samples very close to the transmission times.
	  Example: Platform samples HG every 30 min., transmits at 00:30:00.
	  The most recent sample in the message is 00:00:00. Set the adjustment
	  to the maximum transmission time.
	*/
	private int timeAdjustment;

	/**
	  Time zone used for decoding data from this transport medium.
	*/
	private String timeZone;
	
	private String loggerType = null;
	private int baud = 0;
	private int stopBits = 0;
	private char parity = 'U'; // unknown
	private int dataBits = 0;
	private boolean doLogin = false;
	private String username = null;
	private String password = null;



	/// Key used for storing this TM in a hash table.
	private String tmKey;

	/**
	* Construct with just a reference to the Platform.
	*/
	public TransportMedium(Platform platform)
	{
		this.platform = platform;
		mediumType = "";
		mediumId = "";
		channelNum = Constants.undefinedIntKey;
		assignedTime = Constants.undefinedIntKey;
		transmitWindow = Constants.undefinedIntKey;
		transmitInterval = Constants.undefinedIntKey;
		scriptName = null;

		this.platform = platform;
		decodesScript = null;
		equipmentModel = null;
		performanceMeasurements = null;
		preamble = Constants.preambleShort;
		timeAdjustment = 0;
		timeZone = null;
	}

	/**
	  Construct with a reference to a Platform, a type and an ID.
	  This is the constructor used when parsing the PlatformList file in
	  the XML database, when the TransportXref element is encountered;
	  e.g.
	  <pre>&lt;TransportXref MediumType="GOES-Self-Timed" MediumId="CE3DB448"/&gt;</pre>
	  @param platform the Platform that owns this TM.
	  @param type the TransportMedium type
	  @param id the TransportMedium unique ID
	*/
	public TransportMedium(Platform platform, String type, String id)
	{
		this(platform);
		mediumType = type;
		setMediumId(id);
	}

	/**
	* This overrides the DatabaseObject's method.  This returns
	* "TransportMedium".
		@return "TransportMedium";
	*/
	public String getObjectType() {
		return "TransportMedium";
	}

	/**
	* @return a string appropriate for use as a unique file name.
	*/
	public String makeFileName()
	{
		StringBuffer sb;

		if (mediumType.equalsIgnoreCase(Constants.medium_GoesST)
		 || mediumType.equalsIgnoreCase(Constants.medium_GoesRD))
			sb = new StringBuffer("goes-");
		else if (mediumType.equalsIgnoreCase("Data-Logger"))
			sb = new StringBuffer("edl-");
		else if (mediumType.equalsIgnoreCase("Modem"))
			sb = new StringBuffer("modem-");
		else
			sb = new StringBuffer("utm-"); // utm = Unknown Transport Medium

		sb.append(mediumId);
		for(int i=0; i<sb.length(); i++)
			if (Character.isWhitespace(sb.charAt(i)))
				sb.setCharAt(i, '-');

		return sb.toString();
	}

	/** 
	  Set the DecodesScript for this TranportMedium.  
	  @param ds the DecodesScript
	*/
	public void setDecodesScript(DecodesScript ds)
	{
		decodesScript = ds;
	}

	/**
	* @return the DecodesScript for this TranportMedium.
	*/
	public DecodesScript getDecodesScript() 
	{
		return decodesScript;
	}

	/** @return this TransportMedium's ID string.  */
	public String getMediumId() 
	{
		return mediumId;
	}

	/** @return the medium type for this TM. */
	public String getMediumType()
	{
		return mediumType;
	}

	/** @return this GOES transport medium's preamble code S=short or L=long. */
	public char getPreamble() { return preamble; }

	/**
	  Sets this GOES transport medium's preamble code.
	  Passed value must be Constants.preambleShort or Constants.preambleLong.
	  @param p the code
	*/
	public void setPreamble(char p) { preamble = p; }


	/** @return this GOES transport medium's time adjustment. */
	public int getTimeAdjustment() { return timeAdjustment; }

	/** 
	  Sets this GOES transport medium's time adjustment. 
	  @param ta the time adjustment
	*/
	public void setTimeAdjustment(int ta) { timeAdjustment = ta; }

	/** 
	  Set this object's ID string.  Note that if the medium type
	  is GOES-Random or GOES-Self-Timed [Question:  what about
	  if the medium type is simply 'GOES'?] then this is converted
	  to uppercase before it is stored.
	  @param id the ID
	*/
	public void setMediumId(String id)
	{
		if (mediumType.toLowerCase().startsWith("goes"))
			id = id.toUpperCase();
		mediumId = id;
		tmKey = makeTmKey(mediumType, mediumId);
	}

	/** 
	  Sets the medium type 
	  @param typ the type
	*/
	public void setMediumType(String typ)
	{
		mediumType = typ;
		if (mediumId != null)
			setMediumId(mediumId);
	}
	
	/** @return true if this is a GOES self-timed or random transport medium. */
	public boolean isGoes()
	{
		return mediumType != null 
			&& mediumType.toLowerCase().startsWith("goes");
	}

	/** @return the timezone or null if none is assigned.
	*/
	public String getTimeZone() { return timeZone; }

	/** 
	  Sets the timezone 
	  @param tz string representation of the timezone
	*/
	public void setTimeZone(String tz) { timeZone = tz; }

	/**
	  Returns a string containing the medium type and ID.
	  Used by the PlatformList as a partial key to efficiently search for 
	  platforms matching a given type & id.
	  @return a string containing the medium type and ID.
	*/
	public String getTmKey()
	{
		return tmKey;
	}

	/**
	  Build a string containing type and ID for use as a key.
	  @return the key as a String
	*/
	static String makeTmKey(String typ, String id)
	{
		if (typ.toLowerCase().startsWith("goes"))
			return "goes:" + id.trim();
		else
			return typ + ":" + id.trim();
	}

	/**
	* Overrides the DatabaseObject method.
	*/
	public void prepareForExec()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		if (decodesScript == null && platform != null)
		{
			PlatformConfig pc = platform.getConfig();
			if (pc != null)
				decodesScript = pc.getScript(scriptName);
		}
	
	}

	/**
	* Overrides the DatabaseObject method.
	  @return true if prepared
	*/
	public boolean isPrepared()
	{
		if (decodesScript != null)
			return decodesScript.isPrepared();
		else
			return false;
	}

	/**
	* Overrides the DatabaseObject method.
	* At present, this does nothing.
	*/
	public void validate()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		// IO handled by Platform
	}

	/**
	* Overrides the DatabaseObject method; this does nothing, since
	* the I/O of this object is handled by the Platform to which it
	* belongs.
	*/
	public void read()
		throws DatabaseException
	{
		// IO handled by Platform
	}

	/**
	* Overrides the DatabaseObject method; this does nothing, since
	* the I/O of this object is handled by the Platform to which it
	* belongs.
	*/
	public void write()
		throws DatabaseException
	{
	}

	
	/**
	* Compares two TransportMedium objects.
	  @param obj the other TransportMedium
	*/
	public boolean equals(Object obj)
	{
		if (obj == null || !(obj instanceof TransportMedium))
			return false;
		TransportMedium tm = (TransportMedium)obj;
		if (!mediumType.equalsIgnoreCase(tm.mediumType)
		 || !mediumId.equals(tm.mediumId)
		 || !TextUtil.strEqualIgnoreCase(scriptName, tm.scriptName))
		{
			Logger.instance().debug3("TM differs by type, id, or script name");
			return false;
		}
		if (channelNum != tm.channelNum
		 || assignedTime != tm.assignedTime
		 || transmitWindow != tm.transmitWindow
		 || transmitInterval != tm.transmitInterval
		 || preamble != tm.preamble
		 || timeAdjustment != tm.timeAdjustment)
		{
			Logger.instance().debug3("TM differs by chan, goes window, or timeadj");
			return false;
		}
		
		if (equipmentModel == null)
		{
			if (tm.equipmentModel != null)
			{
				Logger.instance().debug3("TM differs by EqModel(a)");
				return false;
			}
		}
		else
		{
			if (tm.equipmentModel == null)
			{
				Logger.instance().debug3("TM differs by EqModel(b)");
				return false;
			}
			else if (!equipmentModel.equals(tm.equipmentModel))
			{
				Logger.instance().debug3("TM differs by EqModel(c)");
				return false;
			}
		}

		if (!TextUtil.strEqualIgnoreCase(timeZone, tm.timeZone))
		{
			Logger.instance().debug3("TM differs by TZ");
			return false;
		}
		
		if (!TextUtil.strEqual(loggerType, tm.loggerType))
			return false;
		if (baud != tm.baud
		 || stopBits != tm.stopBits
		 || parity != tm.parity
		 || dataBits != tm.dataBits
		 || doLogin != tm.doLogin
		 || !TextUtil.strEqual(username, tm.username)
		 || !TextUtil.strEqual(password, tm.password))
			return false;
		
		return true;
	}

	/**
	  return a deep copy of this transportmedium.
	*/
	public TransportMedium copy()
	{
		TransportMedium ntm = new TransportMedium(platform,
				getMediumType(), getMediumId());
		ntm.channelNum = this.channelNum;
		ntm.assignedTime = this.assignedTime;
		ntm.transmitWindow = this.transmitWindow;
		ntm.transmitInterval = this.transmitInterval;
		ntm.scriptName = this.scriptName;
		ntm.equipmentModel = this.equipmentModel;
		ntm.performanceMeasurements = this.performanceMeasurements;
		ntm.preamble = this.preamble;
		ntm.timeAdjustment = this.timeAdjustment;
		ntm.timeZone = this.timeZone;
		ntm.loggerType = this.loggerType;
		ntm.baud = this.baud;
		ntm.stopBits = this.stopBits;
		ntm.parity = this.parity;
		ntm.dataBits = this.dataBits;
		ntm.doLogin = this.doLogin;
		ntm.username = this.username;
		ntm.password = this.password;
		
		return ntm;
	}

	public String getLoggerType()
	{
		return loggerType;
	}

	public void setLoggerType(String loggerType)
	{
		this.loggerType = loggerType;
	}

	public int getBaud()
	{
		return baud;
	}

	public void setBaud(int baud)
	{
		this.baud = baud;
	}

	public int getStopBits()
	{
		return stopBits;
	}

	public void setStopBits(int stopBits)
	{
		this.stopBits = stopBits;
	}

	public char getParity()
	{
		return parity;
	}

	public void setParity(char parity)
	{
		this.parity = parity;
	}

	public int getDataBits()
	{
		return dataBits;
	}

	public void setDataBits(int dataBits)
	{
		this.dataBits = dataBits;
	}

	public boolean isDoLogin()
	{
		return doLogin;
	}

	public void setDoLogin(boolean doLogin)
	{
		this.doLogin = doLogin;
	}

	public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username;
	}

	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}
	
	public int hashCode()
	{
		return mediumType.hashCode() + mediumId.hashCode();
	}
}

