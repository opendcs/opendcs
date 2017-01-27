/*
*  $Id$
*/
package decodes.db;

import java.util.Vector;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Date;

import decodes.sql.DbKey;

import ilex.util.Logger;
import ilex.util.TextUtil;

/**
 * PlatformList is the collection of all known Platform objects.
 * It provides methods to search for platforms given a specific transport
 * medium type and ID.
 */
public class PlatformList extends DatabaseObject
{
	/** This stores all the known Platform objects. */
	private Vector<Platform> platformVec = new Vector<Platform>();
	
	/** This is a hashmap by transport medium of current platforms (i.e. no expiration) */
	private HashMap<String, Platform> currentPlatformMap = new HashMap<String, Platform>();

	/** Cross reference of Platform objects to SQL IDs. */
	private IdRecordList pidList = new IdRecordList("Platform");
	
	private boolean listWasRead = false;

	/** No-arg constructor.  */
	public PlatformList()
	{
	}

	/** @return this DatabaseObject's type, "PlatformList".  */
	public String getObjectType() {
		return "PlatformList";
	}

	/**
	  Adds a Platform object to the collection.
	  The SQL ID (if used) should be set prior to calling this method.
	  @param plat the platform to add.
	*/
	public synchronized void add(Platform plat)
	{
		boolean inVector = false;
		for(Platform tp : platformVec)
			if (plat == tp)
			{
				inVector = true;
				break;
			}

		if (!inVector && plat.idIsSet())
		{
			Platform oldplat = (Platform)pidList.get(plat.getId());
			if (oldplat != null)
				platformVec.remove(oldplat);
		}

		if (!inVector)
			platformVec.add(plat);

		pidList.add(plat);  // Adds or replaces in the ID list.
		
		if (plat.expiration == null)
		{
			for(Iterator<TransportMedium> tmit = plat.getTransportMedia(); tmit.hasNext(); )
			{
				TransportMedium tm = tmit.next();
				currentPlatformMap.put(tm.getTmKey(), plat);
			}
		}
	}
	
	private void refillCurrentPlatformMap()
	{
		currentPlatformMap.clear();
		int tms = 0;
		for(Platform plat : platformVec)
		{
			if (plat.expiration == null)
			{
				for(Iterator<TransportMedium> tmit = plat.getTransportMedia(); tmit.hasNext(); )
				{
					TransportMedium tm = tmit.next();
					String key = tm.getTmKey();
					currentPlatformMap.put(key, plat);
if (tm.getMediumId().equalsIgnoreCase("CE5E7ABA"))
	Logger.instance().info("Added tm map for " + key);
					tms++;
				}
			}
		}
Logger.instance().debug3("" + tms + " TMs processed, map size=" + currentPlatformMap.size());
	}

	
	/** 
	  Return a Platform object, given a SQL ID. Returns null if no match. 
	  @param id the ID to search for
	  @return a Platform object, given a SQL ID. Returns null if no match. 
	*/
	public Platform getById(DbKey id)
	{
		return (Platform)pidList.get(id);
	}

	/**
	  Returns the platform with indicated medium type and ID, that's
	  currently in affect (i.e. not expired).
	  See getPlatform(type, ID, Date ts) - general contract
	  @param mediumType transport medium type
	  @param mediumId transport medium ID
	  @return Platform or null if no match.
	*/
	public Platform getPlatform(String mediumType, String mediumId)
		throws DatabaseException
	{
		return getPlatform(mediumType, mediumId, new Date());
	}

	/**
	  Returns the platform with the passed medium type and ID.
	  Among all the Platforms with this TM, this finds the correct one
	  that was in effect for the specified time-stamp.  If no Platform
	  with the indicated medium type and ID are found, or if no matching
	  Platform was in effect at the indicated time-stamp, then this
	  returns null.
	  <p>
	  Contract: This method will return a complete, up-to-date platform,
	  or null. There is no need for the caller to check the isComplete
	  methods or expiration date.

	  @param mediumType transport medium type
	  @param mediumId transport medium ID
	  @param ts the time stamp
	  @return Platform or null if no match.

	  @throws DatabaseException if a read to the persistent database was
	   necessary and an error was encountered.
	*/
	public synchronized Platform getPlatform(String mediumType, String mediumId,
		Date ts)
		throws DatabaseException
	{
		Platform p = findPlatform(mediumType, mediumId, ts);
		if (getDatabase() == null || getDatabase().getDbIo() == null)
		{
			Logger.instance().debug3("getPlatform: No db or dbio.");
			return null;
		}
		boolean cs = getDatabase().getDbIo().commitAfterSelectStatus();
		if (p == null)
		{
			Logger.instance().debug3("getPlatform: No platform matching '" + mediumType + ":" + mediumId);
			DatabaseIO dbio = getDatabase().getDbIo();
			DbKey platId = getDatabase().getDbIo().lookupPlatformId(
				mediumType, mediumId, ts);
			if (platId == null || platId == Constants.undefinedId)
				return null;
			p = new Platform(platId);
			add(p);
		}
		
		if (!p.isComplete())
		{
			getDatabase().getDbIo().setCommitAfterSelect(false);
			p.read();
			getDatabase().getDbIo().setCommitAfterSelect(cs);
		}
		else
		{
			getDatabase().getDbIo().setCommitAfterSelect(false);
			Date dbLMT = getDatabase().getDbIo().getPlatformLMT(p);
			getDatabase().getDbIo().setCommitAfterSelect(cs);
			if (dbLMT == null)
			{
				// Platform has been deleted from database!
				String msg = "Platform '" + p.makeFileName() 
					+ "' has been removed from the database.";
				Logger.instance().log(Logger.E_WARNING, msg);
				removePlatform(p);
				return null;
			}
			
			// MJM Note: p.lastModifyTime is set from the <LastModifyTime> element
			// in an XML file.
			// But in XML, dbLMT will be the actual file time stamp from the OS.
			// The OS timestamp will always be slightly later.
			// Thus only reload if there is at least a 2 second difference.
			long deltaMsec = p.lastModifyTime.getTime() - dbLMT.getTime();
			if (deltaMsec < -2000)
			{
				String msg = "Platform '" + p.makeFileName() 
					+ "' has been modified in the database -- will reload";
				Logger.instance().log(Logger.E_DEBUG2, msg);
				// Object has been modified in the database.
				getDatabase().getDbIo().setCommitAfterSelect(false);
				p.clear();	// Clear out old definition but keep the ID.
				p.read();	// Reload with the new def.
				getDatabase().getDbIo().setCommitAfterSelect(cs);
			}
		}

		return p;
	}

	/**
	  Finds and returns the platform currently in the collection.
	  The returned platform may not be completely read or up-to-date.
	  @param mediumType transport medium type
	  @param mediumId transport medium ID
	  @param ts the time stamp, or null to return only 'current' platforms.
	  @return Platform or null if no match.
	*/
	public Platform findPlatform(String mediumType, String mediumId, Date ts)
	{
		// MJM 20151130 Added the HashMap for fast retrieval of 'current' platforms
		// (i.e. platforms with no expiration). This is needed by dcpmon.
		String tmKey = TransportMedium.makeTmKey(mediumType, mediumId);
		if (ts == null || System.currentTimeMillis() - ts.getTime() < 3600000L)
			return currentPlatformMap.get(tmKey);
			
		// Find the platform with earliest exp time that's after ts.
		// While iterating, find the current version.

		// The key we will search for:

		Platform current = null;
		Platform best = null;
		for(Iterator<Platform> it = platformVec.iterator(); it.hasNext(); )
		{
			Platform p = it.next();
			if (p.hasTmKey(tmKey))
			{
				// Matches the key! Check expiration date
				if (p.expiration == null)
					current = p;
				else if (p.expiration.compareTo(ts) >= 0) // exp time >= ts
				{
					if (best == null)
						best = p;
					else if (p.expiration.compareTo(best.expiration) < 0)
						best = p;
				}
			}
		}
		if (best != null)
			return best;
		else
			return current;  // Will be null if none found.
	}

	/**
	  Returns a Platform with matching site and designator.
	  Call with null designator to match A.) null or blank designator, or
	  B.) first platform at site.
	  @param site the site to search for
	  @param designator the designator
	  @return Platform or null if no match
	*/
	public Platform findPlatform(Site site, String designator)
	{
		if (site == null)
			return null;
		if (designator != null && designator.trim().length() == 0)
			designator = null;

		Platform bestSiteMatch = null;
		for(Iterator<Platform> it = iterator(); it.hasNext(); )
		{
			Platform p = it.next();
			if (p.getSite() == null)
				continue;

			boolean siteMatch = false;
			if (p.getSite() == site)
				siteMatch = true;

			// check for site name matches (might be a copy of the site).
			for(Iterator<SiteName> sit = site.getNames(); sit.hasNext(); )
			{
				SiteName sn1 = sit.next();
				SiteName sn2 = p.getSite().getName(sn1.getNameType());
				if (sn1.equals(sn2))
				{
					siteMatch = true;
					break;
				}
			}

			// Check for match of designator and site.
			// Return immediately if current (exp == null)
			if (siteMatch)
			{
//				if (bestSiteMatch == null)
//					bestSiteMatch = p;

				String pdes = p.getPlatformDesignator();
				if (designator == null)
				{
					if (pdes == null || pdes.trim().length() == 0)
					{
						if (p.expiration == null)
							return p;
						else
							bestSiteMatch = p;
					}
				}
				else if (TextUtil.strEqualIgnoreCase(pdes, designator) )
				{
					if (p.expiration == null)
						return p;
					else
						bestSiteMatch = p;
				}
			}
		}
		return bestSiteMatch;
	}
	
	public Platform readPlatformFromDb(SiteName sn, 
		String designator, boolean useDesignator)
	{
		DatabaseIO dbio = myDatabase.getDbIo();
		try
		{
			DbKey pid = dbio.lookupCurrentPlatformId(sn, designator, useDesignator);
			if (pid == null)
				return null;
			
			Platform plat = new Platform(pid);
			plat.read();
			return plat;
		}
		catch(DatabaseException ex)
		{
			Logger.instance().warning("Cannot get platform for site name '"
				+ sn.getDisplayName() + "': " + ex);
			return null;
		}
	}

	
	/*
		Get next platform designator  using this template

					<siteName>-<deviceId>-<sequence number >

		Finds the largest sequence number for the <siteName>-<deviceId>
		pair, increments it by one and returns the "next" designator.

		@param siteName the site to search for
		@param deviceId the device id to search for
		@return next Platform designator

	*/
	public String getDesignator(String siteName, String deviceId ) {
		String designator;
		String key = deviceId+"-";
		int maxseq = 0;
		int seq = 0;
		for(Iterator<Platform> it = platformVec.iterator(); it.hasNext(); ) 
		{
			Platform p = it.next();
			Site s = p.getSite();
			if ( s != null ) {
				if ( s.getDisplayName().equals(siteName) ) {
					String pdes = p.getPlatformDesignator();
					if ( pdes != null ) {
						if ( pdes.length() > key.length() ) {
							if ( pdes.substring(0,key.length() ).equals(key) ) {
								String[] component = pdes.split("-");
								seq = Integer.parseInt(component[1]);
								if ( seq > maxseq ) 
									maxseq = seq;
							}
						}
					}
				}
			} 
		}
		maxseq++;
		designator=key+maxseq;
		return designator;
	}

	/**
	 * @return all platforms with a matching site.
	 */
	public Vector<Platform> getPlatforms(Site site)
	{
		Vector<Platform> ret = new Vector<Platform>();
		for(Platform p : platformVec)
		{
			if (p.getSite() == null)
				continue;

			boolean siteMatch = false;
			if (p.getSite() == site)
				siteMatch = true;

			// check for site name matches (might be a copy of the site).
			for(Iterator<SiteName> sit = site.getNames(); sit.hasNext(); )
			{
				SiteName sn1 = sit.next();
				SiteName sn2 = p.getSite().getName(sn1.getNameType());
				if (sn1.equals(sn2))
				{
					siteMatch = true;
					break;
				}
			}

			if (siteMatch)
				ret.add(p);
		}
		return ret;
	}

	/**
	 * Search for a platform using its 'filename', which is 
	 * <p>
	 *    sitename [ -designator ]
	 * @param fn the filename
	 * @return platform if match found, null if not.
	 */
	public Platform getByFileName(String fn)
	{
		for(Iterator<Platform> it = iterator(); it.hasNext(); )
		{
			Platform p = it.next();
			if (fn.equals(p.makeFileName()))
				return p;
		}
		return null;
	}

	/**
	 * Given a site name value, find and return the first matching
	 * platform.
	 * @param snv a site name value to look for
	 * @return matching platform or null if none found.
	 */
	public Platform getBySiteNameValue(String snv)
	{
		for(Iterator<Platform> pit = iterator(); pit.hasNext(); )
		{
			Platform p = pit.next();
			if (p.getSite() != null)
			{
				for(Iterator<SiteName> sit = p.getSite().getNames(); sit.hasNext(); )
				{
					SiteName sn = sit.next();
					if (sn.getNameValue().equalsIgnoreCase(snv))
						return p;
				}
			}
		}
		return null;
	}

	/** @return an Iterator for the list of Platforms.  */
	public Iterator<Platform> iterator()
	{
		return platformVec.iterator();
	}

	/** @return the list of Platforms as a Vector.  */
	public Vector<Platform> getPlatformVector()
	{
		if (!listWasRead)
		{
			try { read(); }
			catch(DatabaseException ex)
			{
				Logger.instance().failure("Cannot read platform list: " + ex);
			}
		}
		return platformVec;
	}
	
	/** @return the number of Platforms in the list.  */
	public int size()
	{
		return platformVec.size();
	}

	/**
	  Removes a platform from the collection.
	  @param p the platform to remove
	*/
	public void removePlatform(Platform p)
	{
		for(Iterator<Platform> it = platformVec.iterator(); it.hasNext(); )
		{
 			Platform plat = it.next();
			if (p == plat) // must be same object to remove!!
			{
				it.remove();
				break;
			}
		}
		pidList.remove(p);
	}

	/**
	  Count number of platforms using the specified PlatformConfig.
	  @param pc the PlatformConfig
	  @return number of platforms
	*/
	public int countUsing(PlatformConfig pc)
	{
		int n = 0;
		String name = pc.configName;

		for(Iterator<Platform> it = platformVec.iterator(); it.hasNext(); )
		{
			Platform p = it.next();
			if (name.equalsIgnoreCase(p.getConfigName()))
				n++;
		}
		return n;
	}

	/**
	  This overrides the DatabaseObject's method.
	*/
	public void prepareForExec()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		throw new InvalidDatabaseException("Not implemented");
	}

	/**
	  This overrides the DatabaseObject's method.
	  @return false
	*/
	public boolean isPrepared()
	{
		return false;
	}

	/**
	* This overrides the DatabaseObject's method.
	*/
	public void validate()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		throw new InvalidDatabaseException("Not implemented");
	}

	/**
	* Read the entire list of Platforms from the database.
	*/
	public void read()
		throws DatabaseException
	{
		if (!myDatabase.siteList.wasRead())
			myDatabase.siteList.read();
		myDatabase.getDbIo().readPlatformList(this);
		listWasRead = true;
		refillCurrentPlatformMap();
//System.out.println("Read Platform List: " + size() + " entries.");
	}

	/**
	* Write the list of Platforms to the database.
	*/
	public void write()
		throws DatabaseException
	{
		myDatabase.getDbIo().writePlatformList(this);
		
	}

	/**
	 * @return last time that any platform in the list was modified.
	 */
	public Date getLastModified()
	{
		return myDatabase.getDbIo().getPlatformListLMT();
	}

	public void clear()
	{
		platformVec.clear();
		pidList.clear();
		currentPlatformMap.clear();
	}
}

