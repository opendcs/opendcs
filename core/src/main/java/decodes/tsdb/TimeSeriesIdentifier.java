/**
 * $Id$
 * 
 * This is open-source software written by Sutron Corporation under
 * contract to the federal government. You are free to copy and use this
 * source code for your own purposes, except that no part of the information
 * contained in this file may be claimed to be proprietary.
 * 
 * $Log$
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.11  2013/03/21 18:27:39  mmaloney
 * DbKey Implementation
 *
 * Revision 1.10  2012/08/01 16:39:51  mmaloney
 * dev
 *
 * Revision 1.9  2012/07/23 15:21:47  mmaloney
 * Refactor group evaluation for HDB.
 *
 * Revision 1.8  2012/07/05 18:27:04  mmaloney
 * tsKey is stored as a long.
 *
 * Revision 1.7  2012/06/18 15:15:39  mmaloney
 * Moved TS ID cache to base class.
 *
 * Revision 1.6  2012/06/13 14:40:38  mmaloney
 * Added get/set tabsel methods
 *
 * Revision 1.5  2012/05/15 14:09:11  mmaloney
 * Added checkValid method.
 *
 * Revision 1.4  2011/01/27 23:32:01  gchen
 * *** empty log message ***
 *
 * Revision 1.3  2010/12/21 19:20:52  mmaloney
 * group computations
 *
 * Revision 1.2  2010/11/28 21:05:24  mmaloney
 * Refactoring for CCP Time-Series Groups
 *
 * Revision 1.1  2010/10/22 18:01:24  mmaloney
 * CCP Refactoring
 *
 */

package decodes.tsdb;

import opendcs.dao.CachableDbObject;
import decodes.db.DataType;
import decodes.db.Site;
import decodes.sql.DbKey;

/**
 * Encapsulates a unique identifier to a time-series in the database.
 * Implementations exist for HDB, and CWMS.
 */
public interface TimeSeriesIdentifier
	extends Comparable<TimeSeriesIdentifier>, CachableDbObject
{
	/**
	 * Makes a copy of this time series identifier but without a key.
	 * This is usually done before transforming one tsid into another.
	 * @return the copy with surrogate key set to Constants.undefinedId
	 */
	public TimeSeriesIdentifier copyNoKey();
	
	/**
	 * @return unique string which identifies this time series, which
	 * may be in different formats depending on the underlying database.
	 */
	public String getUniqueString();
	
	/**
	 * Sets the constituent fields from the passed unique string.
	 * @param uniqueId the unique string identifying a time series.
	 * @throws BadTimeSeriesException if the format is invalid for the
	 * underlying database.
	 */
	public void setUniqueString(String uniqueId)
		throws BadTimeSeriesException;
	
	/**
	 * @return unique surrogate key for this time-series, if one exists,
	 * or Constants.undefinedId if not.
	 */
	public DbKey getKey();
	
	/**
	 * Sets the unique surrogate key for this time-series.
	 * This method should only be called from internal database interfaces.
	 * @param key the surrogate key.
	 */
	public void setKey(DbKey key);

	/**
	 * If a site object has been associated, this method will return it.
	 * Note: Not all DBs prepopulate the entire site objects. If you only
	 * need the mnemonic site name or a display site name, call those methods
	 * below.
	 * @return the Site object associated with this time series, or null
	 * if none.
	 */
	public Site getSite();

	/**
	 * Sets the site associated with this time-series.
	 * @param site the site
	 */
	public void setSite(Site site);
	
	/**
	 * @return the mnemonic site name for this time series.
	 */
	public String getSiteName();
	
	/**
	 * Sets the mnemonic site name for this time series.
	 */
	public void setSiteName(String siteName);
	
	/**
	 * @return a more descriptive site name for displays
	 */
	public String getSiteDisplayName();
	

	/**
	 * @return the DataType object associated with this time-series, or 
	 * null if none.
	 */
	public DataType getDataType();
	
	/**
	 * Sets the DataType object associated with this time-series.
	 * @param dt the data type object.
	 */
	public void setDataType(DataType dt);
	
	/**
	 * Time Series Identifiers may have multiple parts, depending on the
	 * underlying database. This method sets one of those parts.
	 * See TimeSeriesDb.getTsIdParts() to return a list of valid parts for
	 * a particular database.
	 * @param part a valid part name, see TimeSeriesDb.getTsIdParts()
	 * @param value The value for this part.
	 * @throws BadTimeSeriesException if settings are invalid in the
	 *   underlying database.
	 */
	public void setPart(String part, String value);
	
	/**
	 * Time Series Identifiers may have multiple parts, depending on the
	 * underlying database. This method sets one of those parts.
	 * See TimeSeriesDb.getTsIdParts() to return a list of valid parts for
	 * a particular database.
	 * @param part the name of the part to return
	 * @return the value for the specified part.
	 */
	public String getPart(String part);
	
	/**
	 * @return a string representation of this identifier, suitable for 
	 * display on a GUI.
	 */
	public String getDisplayName();
	
	/**
	 * Sets the display name
	 */
	public void setDisplayName(String nm);
	
	/**
	 * @return the EU abbreviation for the storage units for this time series.
	 */
	public String getStorageUnits();
	
	/**
	 * Sets the units abbreviation in which values of this TS are stored.
	 * @param unitsAbbr the valid abbreviation for the units.
	 */
	public void setStorageUnits(String unitsAbbr);
	
	/**
	 * @return a description of this time-series. May be multi-lines.
	 */
	public String getDescription();
	
	/**
	 * Sets the full description field for the time-series.
	 * Note: up to the first period or newline is considered the 'brief
	 * description'.
	 * @param desc The full description field.
	 */
	public void setDescription(String desc);
	
	/**
	 * @return a brief description of the time-series, suitable for display
	 * in a on-line field, or column in a GUI.
	 */
	public String getBriefDescription();
	
	/**
	 * @return the interval string for this time series. This will be one
	 * of the valid codes returned by TimeSeriesDb.getValidIntervalCodes().
	 */
	public String getInterval();
	
	/**
	 * Sets the interval string for this time series.
	 * @param intv a valid interval code for the underlying database.
	 */
	public void setInterval(String intv);
	
	/**
	 * @return the surrogate key data type ID or Constants.undefinedId if
	 * no data type is associated with this tsid.
	 */
	public DbKey getDataTypeId();
	
	/**
	 * Each database has rules for the format and length of the parts
	 * of a time-series ID. This method performs validation and returns
	 * silently if the ID stored here is valid in the underlying database.
	 * @throws BadTimeSeriesException if the ID is not valid, with a message
	 * explaining why.
	 */
	public void checkValid()
		throws BadTimeSeriesException;
	
	/**
	 * Each database has a concept of a table-selector. For HDB this 
	 * determines real vs. modeled data.
	 */
	public String getTableSelector();
	
	public void setTableSelector(String tabsel);
	
	/** @return the msec time that this TSID was read from the database. */
	public long getReadTime();

	/**
	 * Sets the msec time that this TSID was read from the database.
	 * @param readTime msec time value
	 */
	public void setReadTime(long readTime);

	/**
	 * Return true if this TSID matches the passed fully-qualified parm.
	 * @param parm the DbCompParm to check
	 * @return true if this TSID matches the passed fully-qualified parm
	 */
	public boolean matchesParm(DbCompParm parm);
	
	/**
	 * @return an array of strings that describe the parts of a TS ID in
	 * the underlying database.
	 */
	public String[] getParts();
	
	/** Must implement equals()! */
	public boolean equals(Object rhs);
}
