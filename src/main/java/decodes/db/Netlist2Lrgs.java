/*
*  $Id$
*/
package decodes.db;

import java.io.IOException;
import java.io.FileInputStream;
import java.util.Properties;
import javax.xml.parsers.ParserConfigurationException;

import ilex.util.Logger;
import ilex.util.StderrLogger;
import ilex.cmdline.*;

import decodes.util.*;


import org.xml.sax.SAXException;

/**
 * This is a utility to convert DECODES-style network lists into LRGS (or DRS)
 * formatted network lists.
 */
public class Netlist2Lrgs
{
	/** Default constructor. */
    public Netlist2Lrgs()
    {
    }

    static CmdLineArgs cmdLineArgs = new CmdLineArgs(false, "util.log");

	static StringToken nlArgs = new StringToken("", "Netlist-Names", "",
		TokenOptions.optArgument|TokenOptions.optMultiple
		|TokenOptions.optRequired, "");
	static StringToken dbLocArg = new StringToken("E",
		"Explicit Database Location", "", TokenOptions.optSwitch, "");
	static BooleanToken useEditArg = new BooleanToken("e", "Use Edit DB", "",
		TokenOptions.optSwitch, false);

	static
	{
		cmdLineArgs.addToken(nlArgs);
		cmdLineArgs.addToken(dbLocArg);
		cmdLineArgs.addToken(useEditArg);
	}

	/**
	 * Usage: [java] decodes.db.Netlist2Lrgs [network-list list...]
	  @param args the arguments
	 */
	public static void main(String args[])
		throws IOException, DecodesException,
		       SAXException, ParserConfigurationException
	{
		Logger.setLogger(new StderrLogger("Netlist2Lrgs"));

		// Parse command line arguments.
		cmdLineArgs.parseArgs(args);

		DecodesSettings settings = DecodesSettings.instance();

		// Construct the database and the interface specified by properties.
		Database db = new decodes.db.Database();
		Database.setDb(db);
		DatabaseIO dbio = null;
		String dbloc = dbLocArg.getValue();
		if (dbloc.length() > 0)
		{
			dbio = DatabaseIO.makeDatabaseIO(settings, dbloc);
		}
		else
		{
			dbio = DatabaseIO.makeDatabaseIO(settings);
		}
		db.setDbIo(dbio);

		Site.explicitList = false; // YES Sites automatically added to SiteList

		// Initialize standard collections:
		db.enumList.read();
//		db.timeZoneList.read();
//		db.dataTypeSet.read();
//		db.engineeringUnitList.read();
//		db.siteList.read();
//		db.platformList.read();
//		db.platformConfigList.read();
//		db.equipmentModelList.read();
//		db.equationSpecList.read();
//		db.eqTableList.read();

        db.networkListList.read();

		for(int i = 0; i < nlArgs.NumberOfValues(); i++)
		{
            String nlname = nlArgs.getValue();
            NetworkList nl = db.networkListList.find(nlname);
            if (nl == null)
            {
                Logger.instance().log(Logger.E_FATAL,
                    "No such network list " + nlname);
                break;
            }
            nl.read();
            nl.prepareForExec();
            if (nl.legacyNetworkList != null)
            	System.out.println(nl.legacyNetworkList.toFileString());
		}
	}
}
