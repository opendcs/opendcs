/**
 * $Id$
 * 
 * Open Source Software
 * 
 * $Log$
 * Revision 1.2  2013/03/21 18:27:40  mmaloney
 * DbKey Implementation
 *
 */
package decodes.hdb;

import java.util.ArrayList;
import java.util.Iterator;

import decodes.sql.DbKey;

public class HdbSdiCache
{
	private ArrayList<HdbSiteDatatype> cache = new ArrayList<HdbSiteDatatype>();
	
	public void clear()
	{
		cache.clear();
	}
	
	public void add(HdbSiteDatatype sdi)
	{
		for(Iterator<HdbSiteDatatype> sdiit = cache.iterator(); sdiit.hasNext();)
			if (sdiit.next().getSdi() == sdi.getSdi())
			{
				sdiit.remove();
				break;
			}
		cache.add(sdi);
	}
	
	/**
	 * @return the HdbSiteDatatype with the passed sdi or null if none in cache.
	 */
	public HdbSiteDatatype get(DbKey sdi)
	{
		for(HdbSiteDatatype hsd : cache)
			if (hsd.getSdi().equals(sdi))
				return hsd;
		return null;
	}
	
	/**
	 * @return HdbSiteDatatype with passed siteId and datatypeId or null if none in cache.
	 */
	public HdbSiteDatatype get(DbKey siteId, DbKey datatypeId)
	{
		for(HdbSiteDatatype hsd : cache)
			if (hsd.getSiteId().equals(siteId) && hsd.getDatatypeId().equals(datatypeId))
				return hsd;
		return null;
	}
}
