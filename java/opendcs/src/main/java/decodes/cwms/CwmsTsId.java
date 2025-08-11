/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.cwms;

import java.util.Date;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import opendcs.opentsdb.Interval;
import opendcs.opentsdb.OffsetErrorAction;
import ilex.util.TextUtil;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.sql.DbKey;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.TimeSeriesIdentifier;

/**
 * Implements TimeSeriesIdentifier for CWMS database.
 * Uniquely identifies a CWMS Time Series.
 */
public class CwmsTsId implements TimeSeriesIdentifier
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private DbKey tsCode = Constants.undefinedId;
	private String siteName = null;
	private DataType dataType = null;
	private String paramType = null;
	private String interval = null;
	private String duration = null;
	private String version = null;
	private String description = null;
	private boolean versionFlag = false;
	private Integer utcOffset = null;
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
	private OffsetErrorAction offsetErrorAction = OffsetErrorAction.IGNORE;

	/** The interval Object from the database */
	private Interval intervalOb = null;
	private Interval durationOb = null;

	private Site site = null;

	private String baseLoc = null, subLoc = null, baseParam = null, subParam = null,
		baseVersion = null, subVersion = null;

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
			String description, boolean versionFlag, Integer utcOffset,
			String storageUnits)
	{
		this.tsCode = tsCode;
		setUniqueString(path);
		this.dataType = dataType;
		this.description = description;
		this.versionFlag = versionFlag;
		this.utcOffset = utcOffset;
		this.storageUnits = storageUnits;
	}

	@Override
	public TimeSeriesIdentifier copyNoKey()
	{
		CwmsTsId ret = new CwmsTsId(Constants.undefinedId,
			getUniqueString(), dataType, description, versionFlag,
			utcOffset, storageUnits);
		ret.setSite(site);
		ret.setSiteDisplayName(siteDisplayName);

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

		if (parts.length > 0)
			setLocation(parts[0]);
		if (parts.length > 1)
			setParam(parts[1]);
		if (parts.length > 2)
			paramType = parts[2];
		if (parts.length > 3)
			setInterval(parts[3]);
		if (parts.length > 4)
			setDuration(parts[4]);
		if (parts.length > 5)
			setVersion(parts[5]);
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
		else if (part.equalsIgnoreCase("param") || part.equalsIgnoreCase("datatype"))
			setParam(value);
		else if (part.equalsIgnoreCase("paramtype") || part.equalsIgnoreCase("statcode"))
			paramType = value;
		else if (part.equalsIgnoreCase("interval"))
			setInterval(value);
		else if (part.equalsIgnoreCase("duration"))
			setDuration(value);
		else if (part.equalsIgnoreCase("version"))
			setVersion(value);
		else
			log.warn("CwmsTsId setPart {}: invalid part", part);
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
		else if (part.equalsIgnoreCase("param") || part.equalsIgnoreCase("datatype"))
			return dataType == null ? "" : dataType.getCode();
		else if (part.equalsIgnoreCase("paramtype") || part.equalsIgnoreCase("statcode"))
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

	public Interval getDurationOb()
	{
		return durationOb;
	}

	public void setDisplayName(String nm)
	{
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

	public void setDuration(String dur)
	{
		duration = dur;
		durationOb = IntervalCodes.getInterval(dur);
	}


	private void setLocation(String v)
	{
		siteName = v;

		int hyphen = v.indexOf('-');
		if (hyphen <= 0)
		{
			baseLoc = v;
			subLoc = null;
		}
		else
		{
			baseLoc = v.substring(0, hyphen);
			subLoc = v.length() > hyphen+1 ? v.substring(hyphen+1) : null;
		}
		if (site == null) {
			site = new Site();
		}
		site.clearNames();
		SiteName sn = new SiteName(site, Constants.snt_CWMS, v);
		site.addName(sn);


	}

	private void setParam(String v)
	{
		if (dataType != null && dataType.getCode().equals(v))
			return;
		dataType = DataType.getDataType(Constants.datatype_CWMS, v);

		int hyphen = v.indexOf('-');
		if (hyphen <= 0)
		{
			baseParam = v;
			subParam = null;
		}
		else
		{
			baseParam = v.substring(0, hyphen);
			subParam = v.length() > hyphen+1 ? v.substring(hyphen+1) : null;
		}
	}

	public boolean getVersionFlag()
	{
		return versionFlag;
	}

	public void setVersionFlag(boolean versionFlag)
	{
		this.versionFlag = versionFlag;
	}

	public Integer getUtcOffset()
	{
		return utcOffset;
	}

	public void setUtcOffset(Integer utcOffset)
	{
		this.utcOffset = utcOffset;
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
		setLocation(siteName);
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
		int ret = TextUtil.strCompareIgnoreCase(getUniqueString(),
			rhs.getUniqueString());
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

		if (baseLoc.length() < 1)
			throw new BadTimeSeriesException("Empty base location");

		String param = dataType == null ? "" : dataType.getCode();
		if (param == null || param.length() == 0)
			throw new BadTimeSeriesException("No param specified");

		idx = param.indexOf('-');
		String baseParam = idx < 0 ? param : param.substring(0, idx);

		if (baseParam.length() < 1)
			throw new BadTimeSeriesException("Base parameter empty while using sub parameter.");

		if (paramType == null || paramType.length() == 0)
			throw new BadTimeSeriesException("No ParamType specified");

		if (interval == null || interval.length() == 0)
			throw new BadTimeSeriesException("No interval specified");

		if (duration == null || duration.length() == 0)
			throw new BadTimeSeriesException("No duration specified");

		if (version == null || version.length() == 0)
			throw new BadTimeSeriesException("No version specified");
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

	public void setVersion(String v)
	{
		version = v;
		int hyphen = v.indexOf('-');
		if (hyphen <= 0)
		{
			baseVersion = v;
			subVersion = null;
		}
		else
		{
			baseVersion = v.substring(0, hyphen);
			subVersion = v.length() > hyphen+1 ? v.substring(hyphen+1) : null;
		}
	}

	public String getBaseLoc()
	{
		return baseLoc;
	}

	public String getSubLoc()
	{
		return subLoc;
	}

	public String getBaseParam()
	{
		return baseParam;
	}

	public String getSubParam()
	{
		return subParam;
	}

	public String getBaseVersion()
	{
		return baseVersion;
	}

	public String getSubVersion()
	{
		return subVersion;
	}

	@Override
	public String toString() { return this.getUniqueString(); }

	@Override
	public int hashCode() { return toString().hashCode(); }
}
