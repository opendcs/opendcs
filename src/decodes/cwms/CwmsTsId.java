/**
 * $Id$
 * 
 * Open Source Software
 * 
 * $Log$
 * Revision 1.18  2013/03/21 18:27:40  mmaloney
 * DbKey Implementation
 *
 * Revision 1.17  2012/09/11 12:48:02  mmaloney
 * dev
 *
 * Revision 1.16  2012/08/01 14:24:10  mmaloney
 * Implement equals(Object) to allow storage in HashSet.
 *
 * Revision 1.15  2012/07/23 15:20:44  mmaloney
 * Refactor group evaluation for HDB.
 *
 * Revision 1.14  2012/07/05 18:24:02  mmaloney
 * CWMS location names may contain spaces and multiple hyphens.
 * ts key is stored as a long.
 *
 * Revision 1.13  2012/06/18 13:20:43  mmaloney
 * minRecNum in cp_comp_tasklist must be defined as long
 *
 * Revision 1.12  2012/06/13 14:38:50  mmaloney
 * Added get/set tabsel methods
 *
 * Revision 1.11  2012/05/15 14:08:11  mmaloney
 * Added checkValid method.
 *
 * Revision 1.10  2011/03/22 14:13:25  mmaloney
 * Added caching for DbComputations and CWMS Time Series Identifiers.
 *
 * Revision 1.9  2011/02/02 20:42:28  mmaloney
 * bug fixes
 *
 */
package decodes.cwms;

import java.util.Date;

import opendcs.opentsdb.Interval;
import opendcs.opentsdb.OffsetErrorAction;
import ilex.util.Logger;
import ilex.util.TextUtil;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.Site;
import decodes.sql.DbKey;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.TimeSeriesIdentifier;

/**
 * Implements TimeSeriesIdentifier for CWMS database.
 * Uniquely identifies a CWMS Time Series.
 */
public class CwmsTsId 
	implements TimeSeriesIdentifier
{
	private DbKey tsCode = Constants.undefinedId;
	private String siteName = null; 
	private DataType dataType = null;
	private String paramType = null;
	private String interval = null;
	private String duration = null;
	private String version = null;
	private String description = null;
	private boolean versionFlag = false;
	private int intervalUtcOffset = -1;
	private String storageUnits = null;
	private String siteDisplayName = null;
	public String displayName = null;
	private boolean active = true;
	
	/** Time this record was last modified in the database */
	private Date lastModified = null;
	
	/** OpenTSDB needs a table number to find the data */
	private int storageTable = -1;
	
	/** OpenTSDB allows numeric or string samples. */
	private char storageType = 'N'; // N=Numeric, S=String
	
	/** In OpenTSDB this allows time offset to vary by an hour over DST changes */
	private boolean allowDstOffsetVariation = false;

	/** In OpenTSDB, this determines what to do when an UTC Offset Error is detected. */
	private OffsetErrorAction offsetErrorAction = null;
	
	/** The interval Object from the database */
	private Interval intervalOb = null;
	
	private Site site = null;

	/** used for caching */
	private long readTime = 0L;
	
	/** The parts of a CWMS Time Series Identifier */
	public static final String tsIdParts[] = { "Location", "Param", "ParamType", "Interval", 
		"Duration", "Version" };

	
	public CwmsTsId()
	{
	}

	/**
	 * When reading from DB, this constructor is used.
	 * @param tsCode the time-series code
	 * @param path the 6-part CWMS path identifier
	 * @param description the description
	 * @param versionFlag true if this TS is versioned
	 * @param intervalUtcOffset the intervalUtcOffset
	 * @param storageUnits the storage units for this TS
	 */
	public CwmsTsId(DbKey tsCode, String path, DataType dataType, 
			String description, boolean versionFlag, int intervalUtcOffset,
			String storageUnits)
		{
			this.tsCode = tsCode;
			setUniqueString(path);
			this.dataType = dataType;
			this.description = description;
			this.versionFlag = versionFlag;
			this.intervalUtcOffset = intervalUtcOffset;
			this.storageUnits = storageUnits;
		}
	
	@Override
	public TimeSeriesIdentifier copyNoKey()
	{
		CwmsTsId ret = new CwmsTsId(Constants.undefinedId,
			getUniqueString(), dataType, description, versionFlag,
			intervalUtcOffset, storageUnits);
		ret.setSite(site);
		ret.setSiteDisplayName(siteDisplayName);
//Logger.instance().debug3("CwmsTsId.copyNoKey new path=" + ret.getUniqueString());
		
		return ret;
	}

	/* (non-Javadoc)
	 * @see decodes.tsdb.TimeSeriesIdentifier#getUniqueString()
	 */
	@Override
	public String getUniqueString()
	{
		String param = dataType == null ? "" : dataType.getCode();
		return siteName + "." + param + "." + paramType
			+ "." + interval + "." + duration + "." + version;
	}

	@Override
	public void setUniqueString(String uniqueId)
	{
		String parts[] = uniqueId.split("\\.");
//Logger.instance().debug3("CwmsTsId.setUniqueString str='" + uniqueId+ "': #parts=" + parts.length);
//for(int i=0; i<parts.length; i++)Logger.instance().debug3("part[" + i + "]='" + parts[i] + "'");
		if (parts.length > 0)
			siteName = parts[0];
		if (parts.length > 1)
			setParam(parts[1]);
		if (parts.length > 2)
			paramType = parts[2];
		if (parts.length > 3)
			setInterval(parts[3]);
		if (parts.length > 4)
			duration = parts[4];
		if (parts.length > 5)
			version = parts[5];
	}

	/** The key in CWMS is the ts_code */
	@Override
	public void setKey(DbKey key)
	{
		tsCode = key;
	}

	/* (non-Javadoc)
	 * @see decodes.tsdb.TimeSeriesIdentifier#getKey()
	 */
	@Override
	public DbKey getKey()
	{
		return tsCode;
	}

	/* (non-Javadoc)
	 * @see decodes.tsdb.TimeSeriesIdentifier#getSite()
	 */
	@Override
	public Site getSite()
	{
		return site;
	}

	/* (non-Javadoc)
	 * @see decodes.tsdb.TimeSeriesIdentifier#setSite(decodes.db.Site)
	 */
	@Override
	public void setSite(Site site)
	{
		this.site = site;
	}

	/* (non-Javadoc)
	 * @see decodes.tsdb.TimeSeriesIdentifier#getDataType()
	 */
	@Override
	public DataType getDataType()
	{
		return dataType;
	}

	/* (non-Javadoc)
	 * @see decodes.tsdb.TimeSeriesIdentifier#setDataType(decodes.db.DataType)
	 */
	@Override
	public void setDataType(DataType dataType)
	{
		this.dataType = dataType;
	}

	/* (non-Javadoc)
	 * @see decodes.tsdb.TimeSeriesIdentifier#setPart(java.lang.String, java.lang.String)
	 */
	@Override
	public void setPart(String part, String value)
	{
		if (part.equalsIgnoreCase("location")
		 || part.equalsIgnoreCase("site"))
			setLocation(value);
		else if (part.equalsIgnoreCase("param")
		 || part.equalsIgnoreCase("datatype"))
			setParam(value);
		else if (part.equalsIgnoreCase("paramtype"))
			paramType = value;
		else if (part.equalsIgnoreCase("interval"))
			setInterval(value);
		else if (part.equalsIgnoreCase("duration"))
			duration = value;
		else if (part.equalsIgnoreCase("version"))
			version = value;
		else
			Logger.instance().warning("CwmsTsId setPart " + part 
				+ ": invalid part");
	}

	/* (non-Javadoc)
	 * @see decodes.tsdb.TimeSeriesIdentifier#getPart(java.lang.String)
	 */
	@Override
	public String getPart(String part)
	{
		if (part.equalsIgnoreCase("location")
			 || part.equalsIgnoreCase("site"))
			return siteName;
		else if (part.equalsIgnoreCase("param")
			 || part.equalsIgnoreCase("datatype"))
			return dataType == null ? "" : dataType.getCode();
		else if (part.equalsIgnoreCase("paramtype"))
			return paramType;
		else if (part.equalsIgnoreCase("interval"))
			return interval;
		else if (part.equalsIgnoreCase("duration"))
			return duration;
		else if (part.equalsIgnoreCase("version"))
			return version;
		else
			return null;
	}

	/* (non-Javadoc)
	 * @see decodes.tsdb.TimeSeriesIdentifier#getDisplayName()
	 */
	@Override
	public String getDisplayName()
	{
		return displayName == null ? getUniqueString() : displayName;
	}
	
	/* (non-Javadoc)
	 * @see decodes.tsdb.TimeSeriesIdentifier#getStorageUnits()
	 */
	@Override
	public String getStorageUnits()
	{
		return storageUnits;
	}

	/* (non-Javadoc)
	 * @see decodes.tsdb.TimeSeriesIdentifier#getDescription()
	 */
	@Override
	public String getDescription()
	{
		return description;
	}

	/* (non-Javadoc)
	 * @see decodes.tsdb.TimeSeriesIdentifier#getBriefDescription()
	 */
	@Override
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

	/* (non-Javadoc)
	 * @see decodes.tsdb.TimeSeriesIdentifier#getInterval()
	 */
	@Override
	public String getInterval()
	{
		return interval;
	}
	
	public Interval getIntervalOb()
	{
		return intervalOb;
	}

	public void setDisplayName(String nm)
	{
Logger.instance().debug3("TS " + getUniqueString() + ", setting displayName='" + nm + "'");
		this.displayName = nm;
	}

	@Override
	public void setStorageUnits(String unitsAbbr)
	{
		this.storageUnits = unitsAbbr;
	}

	@Override
	public void setDescription(String desc)
	{
		this.description = desc;
	}

	@Override
	public void setInterval(String intv)
	{
		interval = intv;
		intervalOb = IntervalCodes.getInterval(intv);
	}
	
	private void setLocation(String loc)
	{
		siteName = loc;
	}
	
	private void setParam(String param)
	{
		if (dataType != null && dataType.getCode().equals(param))
			return;
		dataType = DataType.getDataType(Constants.datatype_CWMS, param);
	}

	public boolean getVersionFlag()
	{
		return versionFlag;
	}

	public void setVersionFlag(boolean versionFlag)
	{
		this.versionFlag = versionFlag;
	}

	public int getIntervalUtcOffset()
	{
		return intervalUtcOffset;
	}

	public void setIntervalUtcOffset(int intervalUtcOffset)
	{
		this.intervalUtcOffset = intervalUtcOffset;
	}
	
	public DbKey getDataTypeId()
	{
		return dataType == null ? Constants.undefinedId : dataType.getId();
	}

	@Override
	public String getSiteName()
	{
		return siteName;
	}

	@Override
	public void setSiteName(String siteName)
	{
		this.siteName = siteName;
	}


	public void setSiteDisplayName(String siteDisplayName)
	{
		this.siteDisplayName = siteDisplayName;
		
	}
	@Override
	public String getSiteDisplayName()
	{
		return siteDisplayName;
	}

	@Override
	public int compareTo(TimeSeriesIdentifier rhs)
	{
		int ret = TextUtil.strCompareIgnoreCase(getDisplayName(),
			rhs.getDisplayName());
		if (ret != 0)
			return ret;
		long diff = getKey().getValue() - rhs.getKey().getValue();
		return diff > 0 ? 1 : diff < 0 ? -1 : 0;
	}

	@Override
	public void checkValid()
		throws BadTimeSeriesException
	{
		if (siteName == null || siteName.length() == 0)
			throw new BadTimeSeriesException("No location specified");
		
		int idx = siteName.indexOf('-');
		String baseLoc = idx < 0 ? siteName : siteName.substring(0, idx);
		String subLoc = idx < 0 ? null : siteName.substring(idx+1);
		
		if (baseLoc.length() < 1)
			throw new BadTimeSeriesException("Empty base location");
		if (baseLoc.length() > 16)
			throw new BadTimeSeriesException("Base location longer than 16 chars.");
		if (subLoc != null && subLoc.length() > 32)
			throw new BadTimeSeriesException("Sub-location longer than 32 chars.");
		
		String param = dataType == null ? "" : dataType.getCode();
		if (param == null || param.length() == 0)
			throw new BadTimeSeriesException("No param specified");
		
		idx = param.indexOf('-');
		String baseParam = idx < 0 ? param : param.substring(0, idx);
		String subParam = idx < 0 ? null : param.substring(idx+1);
		if (baseParam.length() > 16)
			throw new BadTimeSeriesException("Base parameter longer than 16 chars.");
		if (subParam != null && subParam.length() > 32)
			throw new BadTimeSeriesException("Sub-parameter longer than 32 chars.");

		if (paramType == null || paramType.length() == 0)
			throw new BadTimeSeriesException("No ParamType specified");
		if (paramType.length() > 16)
			throw new BadTimeSeriesException("ParamType longer than 16 chars.");
		
		if (interval == null || interval.length() == 0)
			throw new BadTimeSeriesException("No interval specified");
		if (interval.length() > 16)
			throw new BadTimeSeriesException("interval longer than 16 chars.");
		
		if (duration == null || duration.length() == 0)
			throw new BadTimeSeriesException("No duration specified");
		if (duration.length() > 16)
			throw new BadTimeSeriesException("duration longer than 16 chars.");

		if (version == null || version.length() == 0)
			throw new BadTimeSeriesException("No version specified");
		if (version.length() > 32)
			throw new BadTimeSeriesException("version longer than 32 chars.");
	}

	@Override
	public String getTableSelector()
	{
		return getPart("paramtype") + "." +
				getPart("duration") + "." + getPart("version");
	}

	@Override
	public void setTableSelector(String tabsel)
	{
	}

	public long getReadTime()
	{
		return readTime;
	}

	public void setReadTime(long readTime)
	{
		this.readTime = readTime;
	}

	@Override
	public boolean matchesParm(DbCompParm parm)
	{
		return parm.getSiteDataTypeId().equals(this.tsCode);
	}

	@Override
	public String[] getParts()
	{
		return CwmsTsId.tsIdParts;
	}
	
	public boolean equals(Object rhs)
	{
		if (!(rhs instanceof CwmsTsId))
			return false;
		if (this == rhs)
			return true;
		return this.compareTo((CwmsTsId)rhs) == 0;
	}

	@Override
	public String getUniqueName()
	{
		return getUniqueString();
	}

	public OffsetErrorAction getOffsetErrorAction()
	{
		return offsetErrorAction;
	}

	public void setOffsetErrorAction(OffsetErrorAction offsetErrorAction)
	{
		this.offsetErrorAction = offsetErrorAction;
	}

	public boolean isActive()
	{
		return active;
	}

	public void setActive(boolean active)
	{
		this.active = active;
	}

	public Date getLastModified()
	{
		return lastModified;
	}

	public void setLastModified(Date lastModified)
	{
		this.lastModified = lastModified;
	}

	public int getStorageTable()
	{
		return storageTable;
	}

	public void setStorageTable(int storageTable)
	{
		this.storageTable = storageTable;
	}

	public char getStorageType()
	{
		return storageType;
	}

	public void setStorageType(char storageType)
	{
		this.storageType = storageType;
	}

	public boolean isAllowDstOffsetVariation()
	{
		return allowDstOffsetVariation;
	}

	public void setAllowDstOffsetVariation(boolean allowDstOffsetVariation)
	{
		this.allowDstOffsetVariation = allowDstOffsetVariation;
	}

	public String getDuration() { return duration; }
	
	public String getParamType() { return paramType; }
	
	public String getStatisticsCode() { return paramType; }
	
	public String getVersion() { return version; }
}
