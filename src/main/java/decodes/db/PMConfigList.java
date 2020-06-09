/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.10  2004/08/25 19:31:15  mjmaloney
*  Added javadocs & deprecated unused code.
*
*  Revision 1.9  2002/07/15 22:13:53  chris
*  Fix a comment.
*
*  Revision 1.8  2001/04/21 20:19:23  mike
*  Added read & write methods to all DatabaseObjects
*
*  Revision 1.7  2001/04/13 16:46:38  mike
*  dev
*
*  Revision 1.6  2001/04/12 12:30:37  mike
*  dev
*
*  Revision 1.5  2001/04/02 00:42:33  mike
*  DatabaseObject is now an abstract base-class.
*
*  Revision 1.4  2001/03/23 20:09:25  mike
*  Collection classes are no longer monostate static collections.
*
*  Revision 1.3  2001/03/20 03:43:24  mike
*  Implement final parsers
*
*  Revision 1.2  2001/03/18 18:24:35  mike
*  Implemented PerformanceMeasurments objects & parsers.
*
*  Revision 1.1  2001/03/17 18:53:06  mike
*  Created PMConfig & PMConfigList (performance measurements decoders)
*
*/
package decodes.db;

import java.util.HashMap;
import java.util.Collection;
import java.util.Iterator;

// /**
// PMList is a collection all all known PMConfig objects.
// (A PMConfig is a wrapper for the PlatformConfig object that decodes
// header information into a series of "Performance Measurments").
// When creating a new PMConfig object, it is not
// necessary to explicitly add a new PMConfig to the set, because it is done
// automatically in the constructor.
// */
// public class PMConfigList extends DatabaseObject
// {
// 	private HashMap configs;
// 
// 	public PMConfigList()
// 	{
// 		configs = new HashMap();
// 	}
// 
// 	public String getObjectType() { return "PMConfigList"; }
// 
// 	/**
// 	  This method is called from the PMConfig constructor. Do not call it
// 	  explicitely.
// 	*/
// 	void add(PMConfig cfg)
// 	{
// 		configs.put(cfg.mediumType, cfg);
// 	}
// 
// 	public PMConfig getPMConfig(String mediumType)
// 	{
// 		return (PMConfig)configs.get(mediumType);
// 	}
// 
// 	public Collection values()
// 	{
// 		return configs.values();
// 	}
// 
// 	public int size()
// 	{
// 		return configs.size();
// 	}
// 
// 	/**
// 	  From DatabaseObject
// 	*/
// 	public void prepareForExec()
// 		throws IncompleteDatabaseException, InvalidDatabaseException
// 	{
// 		throw new InvalidDatabaseException("Not implemented");
// 	}
// 
// 	/**
// 	  From DatabaseObject
// 	*/
// 	public boolean isPrepared()
// 	{
// 		return false;
// 	}
// 
// 	/**
// 	  From DatabaseObject
// 	*/
// 	public void validate()
// 		throws IncompleteDatabaseException, InvalidDatabaseException
// 	{
// 		throw new InvalidDatabaseException("Not implemented");
// 	}
// 
// 	public void read()
// 		throws DatabaseException
// 	{
// 		myDatabase.getDbIo().readPMConfigList(this);
// 	}
// 
// 	public void write()
// 		throws DatabaseException
// 	{
// 		Iterator it = values().iterator();
// 		while(it.hasNext())
// 		{
// 			PMConfig ob = (PMConfig)it.next();
// 			ob.write();
// 		}
// 	}
// }
// 
