///*
//*  $Id$
//*
//*  $State$
//*
//*  $Log$
//*  Revision 1.2  2013/03/28 17:29:09  mmaloney
//*  Refactoring for user-customizable decodes properties.
//*
//*  Revision 1.1  2008/04/04 18:21:00  cvs
//*  Added legacy code to repository
//*
//*  Revision 1.10  2006/04/14 12:36:06  mmaloney
//*  DecodesSettings now uses ilex.util.PropertiesUtil to load & save. This will
//*  make it easier to add new properties. DatabaseType has been renamed with a
//*  'Code' suffix.
//*
//*  Revision 1.9  2006/03/28 15:37:15  mmaloney
//*  dev
//*
//*  Revision 1.8  2003/11/15 19:43:43  mjmaloney
//*  Removed obsolete PlatformList.noHash reference.
//*
//*  Revision 1.7  2003/08/01 19:17:23  mjmaloney
//*  CmdLineArgs now takes default log file in constructor.
//*
//*  Revision 1.6  2002/10/10 15:11:06  mjmaloney
//*  SQL Dev.
//*
//*  Revision 1.5  2001/12/02 22:02:34  mike
//*  dev
//*
//*  Revision 1.4  2001/11/30 19:21:15  mike
//*  dev
//*
//*  Revision 1.3  2001/11/28 16:56:24  mike
//*  Bug fixes & removing debug messages.
//*
//*  Revision 1.2  2001/11/21 21:19:06  mike
//*  Implemented working DbInstall
//*
//*  Revision 1.1  2001/11/21 20:26:49  mike
//*  Implemented Installer.
//*
//*
//*/
//package decodes.db;
//
//import java.util.Iterator;
//import java.util.Properties;
//import java.io.FileInputStream;
//import java.util.Vector;
//
//import ilex.util.StderrLogger;
//import ilex.util.Logger;
//
//import decodes.util.*;
//import decodes.xml.CreatePlatformXref;
//
//public class DbInstall
//{
//	DbInstall()
//	{
//	}
//
//
//	static CmdLineArgs cmdLineArgs = new CmdLineArgs(false, "util.log");
//
//	public static void main(String args[])
//		throws Exception
//	{
//		Logger.setLogger(new StderrLogger("DbInstall"));
//
//		// Parse command line arguments.
//		cmdLineArgs.parseArgs(args);
//
//		Logger.instance().log(Logger.E_INFORMATION,
//			"DbInstall Starting ==================================");
//
//		DecodesSettings settings = DecodesSettings.instance();
//
//		// Read the edit database into memory.
//		Database the_db = new decodes.db.Database();
//		Database.setDb(the_db);
//		DatabaseIO editDbio = 
//			DatabaseIO.makeDatabaseIO(settings.editDatabaseTypeCode,
//				settings.editDatabaseLocation);
//		the_db.setDbIo(editDbio);
//		the_db.read();
//
//		// Remove the non production Platforms
//		Vector toRemove = new Vector();
//		for(Iterator it = the_db.platformList.iterator(); it.hasNext(); )
//		{
//			Platform p = (Platform)it.next();
//			if (!p.isProduction)
//				toRemove.add(p);
//		}
//		for(Iterator it = toRemove.iterator(); it.hasNext(); )
//		{
//			Platform p = (Platform)it.next();
//			the_db.platformList.removePlatform(p);
//		}
//
//		// Remove the non production PresentationGroups
//		toRemove.clear();
//		for(Iterator it = the_db.presentationGroupList.iterator();it.hasNext();)
//		{
//			PresentationGroup p = (PresentationGroup)it.next();
//			if (!p.isProduction)
//				toRemove.add(p);
//		}
//		for(Iterator it = toRemove.iterator(); it.hasNext(); )
//		{
//			PresentationGroup p = (PresentationGroup)it.next();
//			the_db.presentationGroupList.remove(p);
//		}
//
//		// Remove the non production RoutingSpecs
//		toRemove.clear();
//		for(Iterator it = the_db.routingSpecList.iterator(); it.hasNext(); )
//		{
//			RoutingSpec p = (RoutingSpec)it.next();
//			if (!p.isProduction)
//				toRemove.add(p);
//		}
//		for(Iterator it = toRemove.iterator(); it.hasNext(); )
//		{
//			RoutingSpec p = (RoutingSpec)it.next();
//			the_db.routingSpecList.remove(p);
//		}
//
//
//
////		toRemove.clear();
////		for(Iterator it = the_db.eqTableList.iterator(); it.hasNext(); )
////		{
////			EqTable p = (EqTable)it.next();
////			if (!p.isProduction)
////				it.remove();
////		}
////		for(Iterator it = the_db.equationSpecList.iterator(); it.hasNext(); )
////		{
////			EquationSpec p = (EquationSpec)it.next();
////			if (!p.isProduction)
////				it.remove();
////		}
//
//		// Point the DBIO to the 'installed' database & write it back out.
//		DatabaseIO installDbio = DatabaseIO.makeDatabaseIO(
//			settings.databaseTypeCode, settings.databaseLocation);
//		the_db.setDbIo(installDbio);
//
//		// Write the low-level stuff always:
//		the_db.enumList.write();
//		the_db.dataTypeSet.write();
//		the_db.engineeringUnitList.write();
//
//		// Write out all Equipment Model records:
//		the_db.equipmentModelList.write();
//
//		// Write production platforms & subordinate configs & sites.
//		for(Iterator it = the_db.platformList.iterator(); it.hasNext(); )
//		{
//			Platform p = (Platform)it.next();
//			PlatformConfig pc = p.getConfig();
//			if (pc != null)
//				pc.write();
//			if (p.site != null)
//				p.site.write();
//			for(Iterator sit = p.platformSensors.iterator(); sit.hasNext(); )
//			{
//				PlatformSensor ps = (PlatformSensor)sit.next();
//				if (ps.site != null)
//					ps.site.write();
//			}
//			p.write();
//		}
//
//		the_db.dataSourceList.write();
//		the_db.networkListList.write();
//		the_db.presentationGroupList.write();
//		// the_db.eqationSpecList.write();
//
//		for(Iterator it = the_db.routingSpecList.iterator(); it.hasNext(); )
//		{
//			RoutingSpec p = (RoutingSpec)it.next();
//			p.write();
//		}
//
//		if (settings.databaseTypeCode == DecodesSettings.DB_XML)
//			CreatePlatformXref.createPlatformXref(settings.databaseLocation,
//				the_db);
//	}
//}
