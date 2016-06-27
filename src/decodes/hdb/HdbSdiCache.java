/**
 * $Id$
 * 
 * Open Source Software
 * 
 * $Log$
 * Revision 1.2  2015/12/05 16:19:55  mmaloney
 * Replaced ArrayList with HashMap. A Typical HDB will have thousands of SDIs.
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.2  2013/03/21 18:27:40  mmaloney
 * DbKey Implementation
 *
 */
package decodes.hdb;

import java.util.HashMap;

import decodes.sql.DbKey;

public class HdbSdiCache
{
	/** Maps SDI to object with site id and datatype id */
	private HashMap<DbKey, HdbSiteDatatype> sdiMap = new HashMap<DbKey, HdbSiteDatatype>();
	
	public void clear()
	{
		sdiMap.clear();
	}
	
	public void add(HdbSiteDatatype sdi)
	{
		sdiMap.put(sdi.getSdi(), sdi);
	}
	
	/**
	 * @return the HdbSiteDatatype with the passed sdi or null if none in cache.
	 */
	public HdbSiteDatatype get(DbKey sdi)
	{
		return sdiMap.get(sdi);
	}
	
	/**
	 * @return HdbSiteDatatype with passed siteId and datatypeId or null if none in cache.
	 */
	public HdbSiteDatatype get(DbKey siteId, DbKey datatypeId)
	{
		for(HdbSiteDatatype hsd : sdiMap.values())
			if (hsd.getSiteId().equals(siteId) && hsd.getDatatypeId().equals(datatypeId))
				return hsd;
		return null;
	}
	
	public int size() { return sdiMap.size(); }
}
