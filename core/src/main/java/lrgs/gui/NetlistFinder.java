/*
* $Id$
*
* $Log$
* Revision 1.5  2010/08/19 14:39:54  mmaloney
* Before searching for a DECODES network list, must read the list from the DB.
*
* Revision 1.4  2009/08/12 19:44:17  mjmaloney
* usgs merge
*
* Revision 1.3  2009/01/22 00:32:17  mjmaloney
* DB Caching improvements to make msgaccess start quicker.
* Remove the need to cache the entire database.
*
* Revision 1.2  2008/05/29 22:37:09  cvs
* dev
*
* Revision 1.2  2004/07/07 20:51:14  mike
* dev
*
* Revision 1.1  2004/07/06 19:16:26  mike
* dev
*
*
*/
package lrgs.gui;

import java.io.File;
import java.io.IOException;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.InvalidDatabaseException;

/**
Contains static methods for finding network lists.
*/
public class NetlistFinder
{
	/**
	* Finds a network list.
	* @returns File object representing the readable network list or null
	* if not found.
	*/
	public static File find(String name)
	{
		// The <all> and <production> lists are handled at a low-level inside
		// LddsClient, so there won't be any corresponding file.
		if (name.equalsIgnoreCase("<all>")
		 || name.equalsIgnoreCase("<production>"))
			return null;
		
		Logger.instance().debug3("Looking for network list '" + name + "'");
		File f = new File(name);
		Logger.instance().debug3("   Checking '" + f.getPath() + "'");
		if (f.canRead())
			return f;
		f = new File(EnvExpander.expand("$DECODES_INSTALL_DIR/netlist/"+name));
		Logger.instance().debug3("   Checking '" + f.getPath() + "'");
		if (f.canRead())
			return f;

		Database db = Database.getDb();
		try { db.networkListList.read(); }
		catch(DatabaseException ex)
		{
			Logger.instance().warning("Cannot load network lists from DECODES database: " + ex);
		}
		decodes.db.NetworkList nl = db.networkListList.find(name);
		if (nl != null)
		{
			
			Logger.instance().debug3("   Found DECODES netlist by that name.");
			String tmpdirname = EnvExpander.expand("$DCSTOOL_USERDIR/tmp");
			try
			{
				nl.prepareForExec();
				lrgs.common.NetworkList lln = nl.legacyNetworkList;
				File tmpdir = new File(tmpdirname);
				if (!tmpdir.isDirectory())
					tmpdir.mkdirs();
				f = new File(tmpdir, name);
				Logger.instance().debug3("   Creating '" + f.getPath() + "'");
				lln.saveFile(f);
				return f;
			}
			catch(InvalidDatabaseException ex)
			{
				Logger.instance().warning(
					"Cannot construct legacy netlist file for '" + name + "': "
					+ ex);
				return null;
			}
			catch(IOException ex)
			{
				Logger.instance().warning(
					"Cannot write legacy netlist file '" + name + "' in dir '" 
					+ tmpdirname + "': " + ex);
				return null;
			}
		}
		
		return null;
	}
}
