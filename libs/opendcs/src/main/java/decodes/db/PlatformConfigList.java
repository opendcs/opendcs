/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.6  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*  Revision 1.5  2009/10/20 15:36:21  mjmaloney
*  SFWMD debug
*
*  Revision 1.4  2008/12/14 00:51:08  mjmaloney
*  platform count fix
*
*  Revision 1.3  2008/11/20 18:49:20  mjmaloney
*  merge from usgs mods
*
*  Revision 1.2  2008/11/11 02:01:53  mjmaloney
*  Bug fixes.
*
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.15  2008/02/29 18:10:33  mmaloney
*  dev
*
*  Revision 1.14  2008/02/11 14:48:02  mmaloney
*  dev
*
*  Revision 1.13  2008/02/07 17:31:10  satin
*  Added method
*
*    public String freeSequenceNumber(String prefix)
*
*      Returns the next "free" sequence number for a
*      configuration that begins with "prefix",e.g.
*      given the configuration prefix SU8200D-USGSMT,
*      it searches the list of configurations with that
*      prefix, finds the maximum sequence number, and
*      returns it incremented by 1.
*
*  Revision 1.12  2004/08/27 12:23:08  mjmaloney
*  Added javadocs
*
*  Revision 1.11  2002/09/21 23:32:43  mjmaloney
*  SQL dev.
*
*  Revision 1.10  2002/07/15 22:15:39  chris
*  Added javadoc comments -- cosmetic changes only.
*
*  Revision 1.9  2002/03/14 21:07:44  mike
*  Bug fixes.
*
*  Revision 1.8  2001/11/24 18:29:10  mike
*  First working DbImport!
*
*  Revision 1.7  2001/11/23 21:18:22  mike
*  dev
*
*  Revision 1.6  2001/11/17 22:14:33  mike
*  dev
*
*  Revision 1.5  2001/05/03 02:14:39  mike
*  dev
*
*  Revision 1.4  2001/04/21 20:19:23  mike
*  Added read & write methods to all DatabaseObjects
*
*  Revision 1.3  2001/04/12 12:30:33  mike
*  dev
*
*  Revision 1.2  2001/04/02 00:42:33  mike
*  DatabaseObject is now an abstract base-class.
*
*  Revision 1.1  2001/03/23 22:07:04  mike
*  Implemented PlatformConfigList & added it to Database class.
*
*/
package decodes.db;

//import java.util.HashMap;
import ilex.util.TextUtil;

import java.util.Vector;
import java.util.Collection;
import java.util.Iterator;

import decodes.sql.DbKey;

/**
 * PlatformConfigList is a collection of all known PlatformConfig objects.
 * This class provides methods for storing and retrieving these.  Every
 * PlatformConfig within this Database must have a unique name, and this
 * class enforces that.
 */
public class PlatformConfigList
	extends DatabaseObject
{
	private Vector<PlatformConfig> configVec;
	private IdRecordList configIdList;

	/** Default constructor. */
	public PlatformConfigList()
	{
		configVec = new Vector<PlatformConfig>();
		configIdList = new IdRecordList("PlatformConfig");
	}

	/** @return "PlatformConfigList". */
	public String getObjectType() { return "PlatformConfigList"; }

  /**
   * Adds a config object to the collection.
   * If there already is a PlatformConfig with the same name, it is
   * replaced.
	  @param cfg the PlatformConfig to add
   */
    public synchronized void add(PlatformConfig cfg)
	{
		PlatformConfig oldCfg = doGet(cfg.configName);
		if (oldCfg != null)
			remove(oldCfg);

		configVec.add(cfg);
		configIdList.add(cfg);
	}

  	/**
   	  Get a PlatformConfig by name.  Note that though case is preserved in
   	  the PlatformConfig's name, this method is not case-sensitive.
	  @param name the name to search for
   	*/
    public synchronized PlatformConfig get(String name)
    {
    	return doGet(name);
    }
    
    // Non-synchronized to be called within other synchronized methods.
    private PlatformConfig doGet(String name)
	{
		for(PlatformConfig pc : configVec)
			if (pc.configName.equalsIgnoreCase(name))
				return pc;
		return null;
	}

	/**
	  Get PlatformConfig by database ID.
	  @param id the database ID.
	  @return PlatformConfig or null if no match.
	*/
	public synchronized PlatformConfig getById(DbKey id)
	{
		return (PlatformConfig)configIdList.get(id);
	}

  	/**
   	  Remove a PlatformConfig from the list.
	  @param ob the PlatformConfig to remove
   	*/
	public synchronized void remove(PlatformConfig ob)
	{
		configVec.remove(ob);
		configIdList.remove(ob);
	}
	
	public synchronized void removeByName(String cfgName)
	{
		for(Iterator<PlatformConfig> pcit = configVec.iterator();
			pcit.hasNext(); )
		{
			PlatformConfig pc = pcit.next();
			if (TextUtil.strEqualIgnoreCase(pc.configName, cfgName))
				pcit.remove();
		}
	}

  	/**
   	  Get the list as a Collection.
	  @return Collection containing PlatformConfig objects
   	*/
	public Collection<PlatformConfig> values()
	{
		return configVec;
	}

  	/**
   	  Get an iterator on the list of PlatformConfig objects.
	  @return iterator into PlatformConfig objects
   	*/
//	public Iterator iterator()
//	{
//		return configVec.iterator();
//	}

  	/**
   	* @return the total number of PlatformConfigs.
   	*/
	public int size()
	{
		return configVec.size();
	}

	/* Counts the number of platforms in this database using this config. */
	public synchronized void countPlatformsUsing()
	{
		for(PlatformConfig pc : configVec)
		{
			pc.numPlatformsUsing = Database.getDb().platformList.countUsing(pc);
		}
	}

	/**
	  From DatabaseObject
	*/
	public void prepareForExec()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		throw new InvalidDatabaseException("Not implemented");
	}

	/**
	  From DatabaseObject
	  @return false
	*/
	public boolean isPrepared()
	{
		return false;
	}

	/**
	  From DatabaseObject
	*/
	public void validate()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		throw new InvalidDatabaseException("Not implemented");
	}

  	/**
   	* Reads this PlatformConfigList, and its kids.
   	*/
	public void read()
		throws DatabaseException
	{
		myDatabase.getDbIo().readConfigList(this);
	}

  	/**
   	* Writes this PlatformConfigList out to the database.
   	*/
	public void write()
		throws DatabaseException
	{
		for(Iterator it = configVec.iterator(); it.hasNext(); )
		{
			PlatformConfig ob = (PlatformConfig)it.next();
			ob.write();
		}
	}

	public void clear()
	{
		configVec.clear();
		configIdList.clear();
	}
}

