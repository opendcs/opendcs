/**
 * $Id$
 * 
 * $Log$
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.23  2013/05/28 13:14:04  mmaloney
 * ILEXPROJECTS-147 bug fix. Must also check interval when comparing to parm.
 *
 * Revision 1.22  2013/03/21 18:27:40  mmaloney
 * DbKey Implementation
 *
 * Revision 1.21  2012/08/23 19:04:34  mmaloney
 * Implement methods to write to the tasklist table from Java.
 *
 * Revision 1.20  2012/08/15 16:07:47  mmaloney
 * Consistency in calling setDisplayName for HdbTsId objects.
 *
 * Revision 1.19  2012/08/15 15:53:18  mmaloney
 * Consistency in calling setDisplayName for HdbTsId objects.
 *
 * Revision 1.18  2012/08/14 18:15:56  mmaloney
 * display name
 *
 * Revision 1.17  2012/08/14 18:06:58  mmaloney
 * Allow display name to be set in unique string.
 *
 * Revision 1.16  2012/08/13 17:49:22  mmaloney
 * dev
 *
 * Revision 1.15  2012/08/13 17:43:39  mmaloney
 * dev
 *
 * Revision 1.14  2012/08/01 17:01:27  mmaloney
 * debug.
 *
 * Revision 1.13  2012/08/01 14:24:34  mmaloney
 * Implement equals(Object) to allow storage in HashSet.
 *
 * Revision 1.12  2012/07/30 21:14:50  mmaloney
 * In HDB, after setting a TSID's datatype, have to invalidate SDI to force a new lookup.
 *
 * Revision 1.11  2012/07/30 21:10:39  mmaloney
 * In HDB, after setting a TSID's datatype, have to invalidate SDI to force a new lookup.
 *
 * Revision 1.10  2012/07/27 12:51:43  mmaloney
 * Null ptr in copyNoKey
 *
 * Revision 1.9  2012/07/23 15:21:31  mmaloney
 * Refactor group evaluation for HDB.
 *
 * Revision 1.8  2012/07/05 18:24:57  mmaloney
 * tsKey is stored as a long.
 *
 * Revision 1.7  2012/06/18 15:15:14  mmaloney
 * Moved TS ID cache to base class.
 *
 * Revision 1.6  2012/06/13 16:27:16  mmaloney
 * remove debugs
 *
 * Revision 1.5  2012/06/13 14:38:30  mmaloney
 * dev
 *
 * Revision 1.4  2012/06/12 18:39:11  mmaloney
 * dev
 *
 * Revision 1.3  2012/06/12 18:31:33  mmaloney
 * dev
 *
 * Revision 1.2  2012/06/12 18:27:40  mmaloney
 * dev
 *
 * Revision 1.1  2012/06/07 19:01:23  mmaloney
 * Implement HdbTsId
 *
 * Revision 1.5  2012/05/31 19:52:43  mmaloney
 * Implemented CP_TS_ID table reads.
 *
 * 
 * This is open-source software written by Sutron Corporation under
 * contract to the federal government. Anyone is free to copy and use this
 * source code for any purpos, except that no part of the information
 * contained in this file may be claimed to be proprietary.
 *
 * Except for specific contractual terms between Sutron and the federal 
 * government, this source code is provided completely without warranty.

 */
package decodes.hdb;

import opendcs.opentsdb.Interval;
import ilex.util.Logger;
import ilex.util.TextUtil;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.IntervalList;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.sql.DbKey;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;

/**
 * This class holds a time-series identifier for HDB.
 * 
 * In HDB, a time-series is identified with a string with the following form:
 *    Site.DataType.Interval.TableSelector[.ModelId[.ModelRunId]]
 * For real data, there are only 4 parts.
 * For modeled data, the ModelId must be specified, and (if this is
 * an out specifying output, such as outputts) the ModelRunId can
 * also be specified.
 * 
 */
public class HdbTsId implements TimeSeriesIdentifier
{
	public int getModelId()
	{
		return modelId;
	}

	public void setModelId(int modelId)
	{
		this.modelId = modelId;
	}

	public int getModelRunId()
	{
		return modelRunId;
	}

	public void setModelRunId(int modelRunId)
	{
		this.modelRunId = modelRunId;
	}

	/** The ts_id from the CP_TS_ID table */
	private DbKey tsId = Constants.undefinedId;
	
	/** The site_datatype_id from CP_TS_ID. This is different from ts_id. */
	private DbKey sdi = Constants.undefinedId;
	
	/** interval from CP_TS_ID */
	private String interval = null;
	
	/** Either "R_" or "M_" (real vs. modeled). From CP_TS_ID.table_selector */
	private String tabsel = null;
	
	/** From CP_TS_ID.model_id. This is different from model_run_id. */
	int modelId = Constants.undefinedIntKey;
	
	/** output programs like outputts need to completely specify model-run-id
	 * Note that this is not stored in CP_TS_ID because it's not used by the CP.
	 */
	int modelRunId = Constants.undefinedIntKey;
	
	/** For cases where we populate from a string, hold site-name separately */
	private String siteName;
	
	// External links derived from the above info:
	private Site site = null;
	private DataType dataType = null;
	private String displayName = null;
	private String unitsAbbr = null;
	private String desc = null;
	
	// Constant strings for the parts of a time series identifier
	public static final String SITE_PART = "Site";
	public static final String DATATYPE_PART = "DataType";
	public static final String INTERVAL_PART = "Interval";
	public static final String TABSEL_PART = "Real/Modeled";
	public static final String MODELID_PART = "ModelId";
	public static final String MODELRUNID_PART = "ModelRunId"; // only used for output programs

	/** This determines the columns and labels in the GUI: */
	public static final String tsIdParts[] = { SITE_PART, DATATYPE_PART, 
		INTERVAL_PART, TABSEL_PART, MODELID_PART };
	
	/** used for caching */
	private long readTime = 0L;


	public HdbTsId()
	{
	}

	public HdbTsId(String uniqueId)
	{
		setUniqueString(uniqueId);
	}
	
	public String[] getParts()
	{
		return HdbTsId.tsIdParts;
	}
	
	@Override
	public String getUniqueString()
	{
		String ret = this.getPart(SITE_PART) + "." + getPart(DATATYPE_PART)
			+ "." + interval + "." + tabsel;
		if (tabsel != null && tabsel.equalsIgnoreCase("M_") && modelId != Constants.undefinedIntKey)
			ret = ret + "." + modelId;
		if (tabsel != null && tabsel.equalsIgnoreCase("M_") && modelRunId != Constants.undefinedIntKey)
			ret = ret + "." + modelRunId;
		return ret;
	}

	@Override
	public void setUniqueString(String uniqueId)
	{
		int paren = uniqueId.indexOf('(');
		if (paren > 0)
		{
			String displayName = uniqueId.substring(paren+1);
			uniqueId = uniqueId.substring(0,  paren);
			int endParen = displayName.indexOf(')');
			if (endParen > 0)
				displayName = displayName.substring(0,  endParen);
			setDisplayName(displayName);
			
		}
		String parts[] = uniqueId.split("\\.");
		if (parts.length >= 1)
			setPart(SITE_PART, parts[0]);
		if (parts.length >= 2)
			setPart(DATATYPE_PART, parts[1]);
		if (parts.length >= 3)
			setPart(INTERVAL_PART, parts[2]);
		if (parts.length >= 4)
			setPart(TABSEL_PART, parts[3]);
		if (parts.length >= 5)
			setPart(MODELID_PART, parts[4]);
		if (parts.length >= 6)
			setPart(MODELRUNID_PART, parts[5]);
	}

	@Override
	public DbKey getKey()
	{
		return tsId;
	}

	@Override
	public Site getSite()
	{
		return site;
	}

	@Override
	public void setSite(Site site)
	{
		this.site = site;
		if (siteName == null && site != null)
		{
			SiteName sn = site.getPreferredName();
			siteName = sn.getNameValue();
		}
	}

	@Override
	public DataType getDataType()
	{
		return dataType;
	}

	@Override
	public void setDataType(DataType dt)
	{
		this.dataType = dt;
	}

	@Override
	public void setPart(String part, String value)
	{
		if (part.equalsIgnoreCase(SITE_PART))
			setSiteName(value);
		else if (part.equalsIgnoreCase(DATATYPE_PART))
		{
			if (dataType != null && dataType.getCode().equals(value))
				return;
			dataType = DataType.getDataType(Constants.datatype_HDB, value);
		}
		else if (part.equalsIgnoreCase(INTERVAL_PART))
		{
			interval = value;
		}
		else if (part.equalsIgnoreCase(TABSEL_PART)
			|| part.equalsIgnoreCase("table_selector")) // legacy
		{
			tabsel = value;
		}
		else if (part.equalsIgnoreCase(MODELID_PART))
		{
			try { modelId = Integer.parseInt(value); }
			catch(NumberFormatException ex)
			{
				Logger.instance().warning("Attempt to set modelId to '" + value 
					+ "': ModelId must be numeric.");
			}
		}
		else if (part.equalsIgnoreCase(MODELRUNID_PART))
		{
			try { modelRunId = Integer.parseInt(value); }
			catch(NumberFormatException ex)
			{
				Logger.instance().warning("Attempt to set modelRunId to '" + value 
					+ "': ModelRunId must be numeric.");
			}
		}
		else
			Logger.instance().warning("Unknown Time Series Identifier part '"
				+ part + "' -- ignored");
	}

	@Override
	public String getPart(String part)
	{
		if (part.equalsIgnoreCase(SITE_PART))
		{
			if (siteName != null)
				return siteName;
			else if (site != null)
			{
				SiteName sn = site.getPreferredName();
				return sn.getNameValue();
			}
			else
				return "";
		}
		else if (part.equalsIgnoreCase(DATATYPE_PART))
		{
			if (dataType != null)
				return dataType.getCode();
			else
				return "";
		}
		else if (part.equalsIgnoreCase(INTERVAL_PART))
			return interval;
		else if (part.equalsIgnoreCase(TABSEL_PART) 
			|| part.equalsIgnoreCase("table_selector")) // legacy
			return tabsel;
		else if (part.equalsIgnoreCase(MODELID_PART))
			return "" + modelId;
		else if (part.equalsIgnoreCase(MODELRUNID_PART))
			return "" + modelRunId;
		else
		{
			Logger.instance().warning("Unknown Time Series Identifier part '"
				+ part + "' -- ignored");
			return "";
		}
	}

	@Override
	public String getDisplayName()
	{
		return displayName != null ? displayName
			: "" + getDataTypeId() + " at " + getSiteName();
	}

	@Override
	public String getStorageUnits()
	{
		return unitsAbbr;
	}

	@Override
	public void setStorageUnits(String unitsAbbr)
	{
		this.unitsAbbr = unitsAbbr;
	}

	@Override
	public String getDescription()
	{
		return desc;
	}

	@Override
	public void setDescription(String desc)
	{
		this.desc = desc;
	}

	@Override
	public String getBriefDescription()
	{
		if (desc == null)
			return "";
		int i=0;
		for(; i<desc.length(); i++)
		{
			char c = desc.charAt(i);
			if (c == '.' || c == '\n' || c == '\r')
				break;
		}
		return desc.substring(0, i);
	}

	@Override
	public String getInterval()
	{
		return interval;
	}

	@Override
	public void setInterval(String intv)
	{
		this.interval = intv;
	}

	@Override
	public TimeSeriesIdentifier copyNoKey()
	{
		HdbTsId copy = new HdbTsId();
		copy.site = this.site;
		copy.dataType = this.dataType;
		copy.sdi = this.sdi;
		copy.interval = this.interval;
		copy.tabsel = this.tabsel;
		copy.modelId = this.modelId;
		copy.modelRunId = this.modelRunId;
		copy.displayName = this.displayName;
		copy.unitsAbbr = this.unitsAbbr;
		copy.desc = this.desc;
		copy.siteName = this.siteName;
		return copy;
	}

	@Override
	public void setKey(DbKey key)
	{
		tsId = key;
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

	@Override
	public String getSiteDisplayName()
	{
		return getSiteName();
	}

	@Override
	public DbKey getDataTypeId()
	{
		if (dataType != null)
			return dataType.getId();
		return Constants.undefinedId;
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
	public void setDisplayName(String nm)
	{
//Logger.instance().info("Set display name to '" + nm + "'");
		displayName = nm;
	}
	
	@Override
	public void checkValid() throws BadTimeSeriesException
	{
//Logger.instance().info("HdbTsId.checkValid");
		if (site == null)
			throw new BadTimeSeriesException("Site unassigned");
		if (dataType == null)
			throw new BadTimeSeriesException("Datatype unassigned");
		if (interval == null)
			throw new BadTimeSeriesException("Interval unassigned");
		boolean ok = false;

		for(Interval intv : IntervalList.instance().getList())
			if (interval.equalsIgnoreCase(intv.getName()))
			{
				ok = true;
				break;
			}
		if (!ok)
			throw new BadTimeSeriesException("Invalid HDB interval '" + interval + "'");
		
		if (tabsel == null)
			throw new BadTimeSeriesException("Table Selector unassigned");
		ok = false;
		if (!tabsel.equalsIgnoreCase("R_") && !tabsel.equalsIgnoreCase("M_"))
			throw new BadTimeSeriesException("Invalid HDB table-selector '" + tabsel + "'");
	}

	public DbKey getSdi()
	{
		return sdi;
	}

	public void setSdi(DbKey sdi)
	{
		this.sdi = sdi;
	}

	@Override
	public String getTableSelector()
	{
		return tabsel;
	}

	@Override
	public void setTableSelector(String tabsel)
	{
		this.tabsel = tabsel;
	}

	@Override
	public long getReadTime()
	{
		return readTime;
	}

	@Override
	public void setReadTime(long readTime)
	{
		this.readTime = readTime;
	}

	@Override
	public boolean matchesParm(DbCompParm parm)
	{
		if (!parm.getSiteDataTypeId().equals(sdi))
			return false;
		DataType pdt = parm.getDataType();
		if (pdt == null || !pdt.equals(dataType))
			return false;
		
		// MJM ILEXPROJECTS-147 Bug Fix - must also check interval in HDB.
		String pint = parm.getInterval();
		if (pint == null || !pint.equalsIgnoreCase(interval))
			return false;
		
		String pts = parm.getTableSelector();
		if (TextUtil.strCompareIgnoreCase(pts, tabsel) != 0)
			return false;
		if (tabsel != null && tabsel.equalsIgnoreCase("M_")
		 && parm.getModelId() != modelId)
			return false;
		
		return true;
	}
	
	public boolean equals(Object rhs)
	{
		boolean ret = false;
		if (!(rhs instanceof HdbTsId))
			ret = false;
		if (this == rhs)
			ret = true;
		ret = this.compareTo((HdbTsId)rhs) == 0;
Logger.instance().info("HdbTsId.equals: this='" + getUniqueString() + "' rhs='" 
+ ((HdbTsId)rhs).getUniqueString() + ", returning " + ret);
		return ret;
	}

	@Override
	public String getUniqueName()
	{
		return getUniqueString();
	}
}
