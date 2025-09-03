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
package decodes.hdb;

import opendcs.opentsdb.Interval;
import ilex.util.TextUtil;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.IntervalList;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.sql.DbKey;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.TimeSeriesIdentifier;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

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
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
	public static final String TABSEL_PART = "TableSelector";
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
				log.atWarn()
				   .setCause(ex)
				   .log("Attempt to set modelId to '{}': ModelId must be numeric.", value);
			}
		}
		else if (part.equalsIgnoreCase(MODELRUNID_PART))
		{
			try { modelRunId = Integer.parseInt(value); }
			catch(NumberFormatException ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("Attempt to set modelRunId to '{}': ModelRunId must be numeric.", value);
			}
		}
		else
		{
			log.warn("Unknown Time Series Identifier part '{}' -- ignored", part);
		}
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
			log.warn("Unknown Time Series Identifier part '{}' -- ignored", part);
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
		displayName = nm;
	}
	
	@Override
	public void checkValid() throws BadTimeSeriesException
	{
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
	
	@Override
	public boolean equals(Object rhs)
	{
		boolean ret = false;
		if (!(rhs instanceof HdbTsId))
			ret = false;
		if (this == rhs)
			ret = true;
		ret = this.compareTo((HdbTsId)rhs) == 0;
		return ret;
	}

	@Override
	public String getUniqueName()
	{
		return getUniqueString();
	}
	
	@Override
	public String toString() { return this.getUniqueString(); }
	
	@Override
	public int hashCode() { return toString().hashCode(); }

}
