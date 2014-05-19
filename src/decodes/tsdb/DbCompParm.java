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
*/
package decodes.tsdb;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import ilex.util.Logger;
import ilex.util.TextUtil;
import decodes.db.Constants;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.db.DataType;
import decodes.sql.DbKey;

/**
* This data-structure class holds info for a single computation 
* parameter.
*/
public class DbCompParm
{
	/** Algorithm role name. */
	private String algoRoleName;
	 
	/**
	 * String interval code.
	 * @see IntervalCodes
	 */
	private String interval;
	
	/** Table selector (used by concrete database implementation).  */
	private String tableSelector;
	
	/**
	 * Signed number of seconds. Offset for retrieving this parameter
	 * relative to zero-based time.
	 */
	private int deltaT;
	
	/** 
	 * Parameter type from the DbAlgoParm record associated with this object. 
	 */
	private String algoParmType;

	/**
	 * Model ID (only used for modeled data)
	 */
	private int modelId;

	/** For variable-site output parameters */
	private DbKey dataTypeId;

	private String deltaTUnits = null;
	
	/** Transient storage of units within the GUI */
	private String unitsAbbr = null;
	
	private DbKey sdi = DbKey.NullKey;
	
	private Site site = new Site();
	
	private DataType dataType = null;
	
	/** OPENDCS6.0 allows Site association in DbCompParm directly */
	private DbKey siteId = Constants.undefinedId;
	
	
	public String getUnitsAbbr()
	{
		return unitsAbbr;
	}

	public void setUnitsAbbr(String unitsAbbr)
	{
		this.unitsAbbr = unitsAbbr;
	}

	/**
	 * Constructor.
	 *
	 * @param sdi the site datatype ID (for HDB) or ddid (for NWIS)
	 * @param interval the interval code
	 * @param tsel Table selector
	 * @param deltaT offset for reading/writing this param from zero-based time.
	 * @see IntervalCodes
	 * @see decodes.hdb.HdbTableSelector
	 */
	public DbCompParm(String algoRoleName, DbKey sdi, String interval, 
		String tsel, int deltaT)
	{
		this.algoRoleName = algoRoleName;
		this.interval = interval;
		tableSelector = tsel;
		if (tableSelector == null)
			tableSelector = "";
		this.deltaT = deltaT;
		this.algoParmType = null;
		this.modelId = Constants.undefinedIntKey;
		this.dataTypeId = Constants.undefinedId;
		this.sdi = sdi;
	}

	/** Copy constructor */
	public DbCompParm(DbCompParm rhs)
	{
		this.algoRoleName = rhs.algoRoleName;
		this.interval = rhs.interval;
		this.tableSelector = rhs.tableSelector;
		this.deltaT = rhs.deltaT;
		this.algoParmType = rhs.algoParmType;
		this.modelId = rhs.modelId;
		this.dataTypeId = rhs.dataTypeId;
		this.deltaTUnits = rhs.deltaTUnits;
		this.sdi = rhs.sdi;
		this.dataType = rhs.dataType;
		this.site.copyFrom(rhs.site);
		this.site.forceSetId(rhs.site.getId());
	}

	/**
	 * @return the role name assigned in the algo parm record.
	 */
	public String getRoleName( )
	{
		return algoRoleName;
	}

	/**
	 * Sets the role name assigned in the algo parm record.
	 * Wparam roleName the role Name
	 */
	public void setRoleName(String roleName)
	{
		this.algoRoleName = roleName;
	}

	/**
	 * Sets the parameter type code defined from the algo parm record.
	 * @param algoParmType the parameter type
	 */
	public void setAlgoParmType(String algoParmType)
	{
		this.algoParmType = algoParmType;
	}

	/**
	 * Return the parameter type code defined from the algo parm record.
	 */
	public String getAlgoParmType( )
	{
		return algoParmType;
	}

	/**
	 * @return the site data-type ID.
	 */
	public DbKey getSiteDataTypeId() 
	{
//		return siteDatatype.getSDI();
		return sdi;
	}
	
	/**
	 * Sets the site data-type ID.
	 * @param id the site data-type ID.
	 */
	public void setSiteDataTypeId(DbKey id)
	{
//		siteDatatype.setSDI(id);
		this.sdi = id;
	}

	/**
	 * @return the interval code.
	 */
	public String getInterval()
	{
		return interval;
	}

	/**
	 * Sets the interval code.
	 * @param x the interval code.
	 */
	public void setInterval(String x)
	{
		interval = x;
	}

	/**
	 * @return the Table selector.
	 */
	public String getTableSelector()
	{
		return tableSelector;
	}

	/**
	 * Sets the Table selector.
	 * @param x the Table selector.
	 */
	public void setTableSelector(String x)
	{
		tableSelector = x == null ? "" : x;
	}

	/**
	 * @return the deltaT.
	 */
	public int getDeltaT() { return deltaT; }

	/**
	 * Sets the site delta-T value.
	 * @param x the delta-T value.
	 */
	public void setDeltaT(int x)
	{
		deltaT = x;
	}

	/** @return the modelId (Constants.undefinedId for real data) */
	public int getModelId() { return modelId; }

	/**
	 * Sets the model ID (use Constants.undefinedId for real data)
	 * @param id the model ID
	 */
	public void setModelId(int id) { modelId = id; }

	public void setSite(Site site)
	{
		if (site != null)
			this.site = site;
		else
			clearSite();
	}
	
	public void clearSite()
	{
		site = new Site();
	}
	
	/**
	 * Associates a site name with this param.
	 * @param siteName the site name
	 */
	public void addSiteName(SiteName siteName)
	{
//		siteDatatype.addSiteName(siteName);
		site.addName(siteName);
	}

	/**
	 * @return the SiteName objects associated with this parm.
	 */
	public ArrayList<SiteName> getSiteNames()
	{
//		return siteDatatype.getSiteNames();
		return site.getNameArray();
	}

	/**
	 * Get the site name with the preferred standard.
	 * If no site name with preferred standard is present, return the
	 * first assigned in the list.
	 * If list is empty, return null.
	 * @return the site name with the preferred standard.
	 */
	public SiteName getSiteName()
	{
//		return siteDatatype.getSiteName();
		return site.getPreferredName();
	}

	/**
	 * Get the site name with the specified standard.
	 * @return the site name with the specified standard or null if none.
	 */
	public SiteName getSiteName(String nameType)
	{
		if (nameType == null)
			return site.getPreferredName();
		return site.getName(nameType);
		
//		if (nameType == null)
//			return getSiteName();
//
//		for(SiteName sn : getSiteNames())
//			if (sn.getNameType().equalsIgnoreCase(nameType))
//				return sn;
//		return null;
	}

	/**
	 * Associates a data type with this param.
	 * @param dataType the data type
	 */
	public void setDataType(DataType dataType)
	{
		this.dataType = dataType;
//		siteDatatype.setDataType(dataType);
		dataTypeId = dataType == null ? Constants.undefinedId : dataType.getId();
	}

	/**
	 * Get the data type with the preferred standard.
	 * If no data type with preferred standard is present, return the
	 * first assigned data type in the list.
	 * If list is empty, return null.
	 * @return the data type with the preferred standard.
	 */
	public DataType getDataType()
	{
		return dataType;
//		return siteDatatype.getDataType();
	}

//	public SiteDatatype getSiteDatatype() { return siteDatatype; }

	public String toString()
	{
		return algoRoleName + "(" + algoParmType + "):"
			+ getSiteDataTypeId() + ":" + interval + ":" + tableSelector
			+ ":deltaT=" + deltaT + "(" + deltaTUnits + ")";
	}

	public boolean equals(DbCompParm rhs)
	{
		if (!TextUtil.strEqual(this.algoRoleName, rhs.algoRoleName))
			return false;
//		if (this.siteDatatype.getSDI() != rhs.siteDatatype.getSDI())
//			return false;
		if (!this.sdi.equals(rhs.sdi))
			return false;
		if (!TextUtil.strEqual(this.interval, rhs.interval))
			return false;
		if (!TextUtil.strEqual(this.tableSelector, rhs.tableSelector)
		 || this.deltaT != rhs.deltaT
		 || !TextUtil.strEqual(this.algoParmType, rhs.algoParmType)
		 || this.modelId != rhs.modelId)
			return false;
		String thisUnits = this.deltaTUnits != null && this.deltaTUnits.equalsIgnoreCase("seconds")
			? null : this.deltaTUnits;
		String rhsUnits = rhs.deltaTUnits != null && rhs.deltaTUnits.equalsIgnoreCase("seconds")
			? null : rhs.deltaTUnits;
		
		if (!TextUtil.strEqual(thisUnits, rhsUnits))
			return false;
		return true;
	}

	public boolean isInput()
	{
		return algoParmType != null && algoParmType.length() > 0
			&& Character.toLowerCase(algoParmType.charAt(0)) == 'i';
	}

	public boolean isOutput()
	{
		return algoParmType != null && algoParmType.length() > 0
			&& Character.toLowerCase(algoParmType.charAt(0)) == 'o';
	}

	public DbKey getDataTypeId() { return dataTypeId; }

	public void setDataTypeId(DbKey dtid) { dataTypeId = dtid; }

	/**
	 * In CWMS, table-selector is ParamType.Duration.Version
     * @return the paramType
     */
    public String getParamType()
    {
    	if (tableSelector == null)
    		return "";
    	int idx = tableSelector.indexOf('.');
    	if (idx == -1)
    		return tableSelector;
    	return tableSelector.substring(0, idx);
    }
    
	/**
	 * In CWMS, table-selector is ParamType.Duration.Version
     * @return the duration
     */
    public String getDuration()
    {
    	if (tableSelector == null)
    		return "";
    	int idx = tableSelector.indexOf('.');
    	if (idx == -1)
    		return tableSelector;
    	int lidx = tableSelector.lastIndexOf('.');
    	if (lidx == -1)
    		return tableSelector.substring(idx+1);
    	return tableSelector.substring(idx+1, lidx);
    }

   
	/**
	 * In CWMS, table-selector is ParamType.Duration.Version
     * @return the version
     */
   public String getVersion()
    {
    	if (tableSelector == null)
    		return null;
    	int lidx = tableSelector.lastIndexOf('.');
    	if (lidx == -1)
    		return "";
    	return tableSelector.substring(lidx+1);
    }

	public String getDeltaTUnits()
	{
		return deltaTUnits;
	}

	public void setDeltaTUnits(String deltaTUnits)
	{
		this.deltaTUnits = deltaTUnits;
	}
	
	public DbKey getSiteId()
	{
		if (site != null)
			return site.getId();
		else return siteId;
	}
	
	public void setSiteId(DbKey siteId)
	{
		this.siteId = siteId;
		if (site != null)
			site.forceSetId(siteId);
	}
	
	/**
	 * ADD the deltaT to the base time to compute param time.
	 * @param baseTime
	 * @return
	 */
	public Date baseTimeToParamTime(Date baseTime, Calendar aggCal)
	{
		if (deltaTUnits == null || deltaTUnits.trim().length() == 0)
			return new Date(baseTime.getTime() + deltaT*1000L);
	
		aggCal.setTime(baseTime);
		int incr = deltaTUnits2CalConst();
		if (incr == -1)
		{
			Logger.instance().warning("Param '" + getRoleName()
				+ "' has invalid deltaTUnits '" + deltaTUnits + "' -- deltaT ignored.");
			return baseTime;
		}
		aggCal.add(incr, deltaT);
		return aggCal.getTime();
	}
	
	private int deltaTUnits2CalConst()
	{
		return 
			TextUtil.startsWithIgnoreCase(deltaTUnits, "second") ? Calendar.SECOND :
			TextUtil.startsWithIgnoreCase(deltaTUnits, "minute") ? Calendar.MINUTE :
			TextUtil.startsWithIgnoreCase(deltaTUnits, "hour") ? Calendar.HOUR :
			TextUtil.startsWithIgnoreCase(deltaTUnits, "day") ? Calendar.DAY_OF_MONTH :
			TextUtil.startsWithIgnoreCase(deltaTUnits, "week") ? Calendar.WEEK_OF_YEAR :
			TextUtil.startsWithIgnoreCase(deltaTUnits, "month") ? Calendar.MONTH :
			TextUtil.startsWithIgnoreCase(deltaTUnits, "year") ? Calendar.YEAR :
			-1;
	}
	
	/**
	 * SUBTRACT the deltaT from the param time to compute base time.
	 * @param baseTime
	 * @return
	 */
	public Date paramTimeToBaseTime(Date baseTime, Calendar aggCal)
	{
		if (deltaTUnits == null || deltaTUnits.trim().length() == 0)
			return new Date(baseTime.getTime() - deltaT*1000L);

		aggCal.setTime(baseTime);
		int incr = deltaTUnits2CalConst();
		if (incr == -1)
		{
			Logger.instance().warning("Param '" + getRoleName()
				+ "' has invalid deltaTUnits '" + deltaTUnits + "' -- deltaT ignored.");
			return baseTime;
		}
		aggCal.add(incr, deltaT * -1);
		return aggCal.getTime();
	}

}
