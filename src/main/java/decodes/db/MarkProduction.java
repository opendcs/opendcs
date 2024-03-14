/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.2  2013/03/28 17:29:09  mmaloney
*  Refactoring for user-customizable decodes properties.
*
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.6  2006/04/14 12:36:07  mmaloney
*  DecodesSettings now uses ilex.util.PropertiesUtil to load & save. This will
*  make it easier to add new properties. DatabaseType has been renamed with a
*  'Code' suffix.
*
*  Revision 1.5  2004/08/26 13:29:24  mjmaloney
*  Added javadocs
*
*  Revision 1.4  2003/11/15 19:51:04  mjmaloney
*  Remove obsolete PlatformList.noHash ref.
*
*  Revision 1.3  2003/08/01 19:17:23  mjmaloney
*  CmdLineArgs now takes default log file in constructor.
*
*  Revision 1.2  2002/10/06 14:23:57  mjmaloney
*  SQL Development.
*
*  Revision 1.1  2001/11/23 21:18:22  mike
*  dev
*
*/
package decodes.db;

import java.util.Iterator;
import java.util.Properties;
import java.io.FileInputStream;

import ilex.util.StderrLogger;
import ilex.util.Logger;
import ilex.cmdline.*;

import decodes.util.*;

/**
 * Marks the isProduction flag for all records in the database
 */
public class MarkProduction
{
	/** Default constructor. */
	MarkProduction()
	{
	}


	static CmdLineArgs cmdLineArgs = new CmdLineArgs(false, "util.log");
	static StringToken tfArg = new StringToken("", "true/false", "", 
		TokenOptions.optArgument, "");
	static
	{
		cmdLineArgs.addToken(tfArg);
	}

	/**
	  Main method.
	  @param args the arguments
	*/
	public static void main(String args[])
		throws Exception
	{
		boolean isProduction = true;

		Logger.setLogger(new StderrLogger("DecodesDbEditor"));

		// Parse command line arguments.
		cmdLineArgs.parseArgs(args);

		Logger.instance().log(Logger.E_INFORMATION,
			"MarkProduction Starting =====================================");
		if (tfArg.getValue().equalsIgnoreCase("false"))
			isProduction = false;

		DecodesSettings settings = DecodesSettings.instance();

		// Read the edit database into memory.
		Database db = new decodes.db.Database();
		Database.setDb(db);
		DatabaseIO editDbio = DatabaseIO.makeDatabaseIO(settings);
		db.setDbIo(editDbio);
		db.read();

		// Set the production flags according to the passed argument.
		for(Iterator it = db.platformList.iterator(); it.hasNext(); )
		{
			Platform p = (Platform)it.next();
			p.isProduction = isProduction;
			p.write();
		}
		//for(Iterator it = db.eqTableList.iterator(); it.hasNext(); )
		//{
		//	EqTable p = (EqTable)it.next();
		//	p.isProduction = isProduction;
		//}
		//for(Iterator it = db.equationSpecList.iterator(); it.hasNext(); )
		//{
		//	EquationSpec p = (EquationSpec)it.next();
		//	p.isProduction = isProduction;
		//}
		for(Iterator it = db.presentationGroupList.iterator(); it.hasNext(); )
		{
			PresentationGroup p = (PresentationGroup)it.next();
			p.isProduction = isProduction;
			p.write();
		}
		for(Iterator it = db.routingSpecList.iterator(); it.hasNext(); )
		{
			RoutingSpec p = (RoutingSpec)it.next();
			p.isProduction = isProduction;
			p.write();
		}

		// Re-write the database.
		//db.write();
	}
}
