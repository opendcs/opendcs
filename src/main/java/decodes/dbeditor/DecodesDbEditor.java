package decodes.dbeditor;

import java.awt.*;
import java.util.Properties;
import java.util.ResourceBundle;

import ilex.gui.WindowUtility;
import lrgs.gui.DecodesInterface;

import ilex.util.LoadResourceBundle;
import ilex.util.Logger;
import ilex.util.StderrLogger;
import ilex.cmdline.*;
import decodes.util.*;
import decodes.db.*;

/**
The MAIN class for the DECODES Database Editor.
*/
public class DecodesDbEditor
{
    boolean packFrame = false;
    static Frame theFrame = null;
    static Properties theProperties = new Properties();
    public static boolean turnOffPopUps = false;
    private static ResourceBundle genericLabels = null;
    private static ResourceBundle dbeditLabels = null;
    
    /**Construct the application*/
    public DecodesDbEditor()
    {
        genericLabels = getGenericLabels();
        dbeditLabels = getDbeditLabels();
        
        DbEditorFrame frame = new DbEditorFrame();
        theFrame = frame;
        decodes.gui.GuiApp.topFrame = frame;

        //Validate frames that have preset sizes
        //Pack frames that have useful preferred size info, e.g. from their
        //layout
        if (packFrame)
        {
            frame.pack();
        }
        else
        {
            frame.validate();
        }

        WindowUtility.center(frame).setVisible(true);
    }
    /**
     * @return resource bundle containing generic labels for the selected
     * language.
     */
    public static ResourceBundle getGenericLabels() 
    {
        if (genericLabels == null)
        {
            DecodesSettings settings = DecodesSettings.instance();
            genericLabels = LoadResourceBundle.getLabelDescriptions(
                "decodes/resources/generic", settings.language);
        }
        return genericLabels;
    }

    /**
     * @return resource bundle containing DB-Editor labels for the selected
     * language.
     */
    public static ResourceBundle getDbeditLabels()
    {
        if (dbeditLabels == null)
        {
            DecodesSettings settings = DecodesSettings.instance();
            dbeditLabels = LoadResourceBundle.getLabelDescriptions(
                "decodes/resources/dbedit", settings.language);
        }
        return dbeditLabels;
    }

    static CmdLineArgs cmdLineArgs = new CmdLineArgs(false, "dbedit.log");
    static StringToken dbLocArg = new StringToken("E", 
        "Explicit Database Location", "", TokenOptions.optSwitch, "");
    //To turn on or off the "Are you sure..." pop ups
    static StringToken turnOnOffPopUps = new StringToken("T", 
    "Turn on or off the 'Are you sure...' pop ups - (Values: On -or- Off)", 
    "", TokenOptions.optSwitch, "On");
    static
    {
        dbLocArg.setType("dirname");
        cmdLineArgs.addToken(dbLocArg);
        cmdLineArgs.addToken(turnOnOffPopUps);
    }

    /**Main method*/
    public static void main(String[] args)
        throws Exception
    {
        Logger.setLogger(new StderrLogger("DecodesDbEditor"));

        // Parse command line arguments.
        try { cmdLineArgs.parseArgs(args); }
        catch(IllegalArgumentException ex)
        {
            System.exit(1);
        }

        Logger.instance().log(Logger.E_INFORMATION,
            "DecodesDbEditor Starting (" + DecodesVersion.startupTag()
            + ") =====================");

        DecodesSettings settings = DecodesSettings.instance();
        
        DecodesInterface.setGUI(true);

        // Construct the database and the interface specified by properties.
        Database db = new decodes.db.Database();
        Database.setDb(db);
        DatabaseIO dbio;

        String dbloc = dbLocArg.getValue();
        if (dbloc.length() > 0)
        {
            dbio = DatabaseIO.makeDatabaseIO(settings, dbloc);
        }
        else
            dbio = DatabaseIO.makeDatabaseIO(settings);

        Platform.configSoftLink = true;

        // Standard Database Initialization for all Apps:
        System.out.print("Reading DB: "); System.out.flush();
        db.setDbIo(dbio);
        System.out.print("Enum, "); System.out.flush();
        db.enumList.read();
        System.out.print("DataType, "); System.out.flush();
        db.dataTypeSet.read();
        System.out.print("EU, "); System.out.flush();
        db.engineeringUnitList.read();

        // Initialization for DB editor - read all the collections:
        Site.explicitList = true;
        System.out.print("Site, "); System.out.flush();
        db.siteList.read();
        Site.explicitList = true;
        System.out.print("Equip, "); System.out.flush();
        db.equipmentModelList.read();
        System.out.print("Config, "); System.out.flush();
        db.platformConfigList.read();
        System.out.print("Plat, "); System.out.flush();
        db.platformList.read();
        db.platformConfigList.countPlatformsUsing();

        System.out.print("Equip, "); System.out.flush();
        db.equipmentModelList.read();
        //db.equationSpecList.read();
        System.out.print("Pres, "); System.out.flush();
        db.presentationGroupList.read();
        System.out.print("Routing, "); System.out.flush();
        db.routingSpecList.read();
        System.out.print("DataSource, "); System.out.flush();
        db.dataSourceList.read();
        System.out.print("Netlist, "); System.out.flush();
        db.networkListList.read();
        //db.eqTableList.read();
        System.out.println("Database initialized.");
        
        

//        fixObjectReferences(db);
//
        //This flag is used to turn on or off some of the pop ups - specially
        //the ones on the Decoding Scripts
        String onOffPopUps = turnOnOffPopUps.getValue();
        if (onOffPopUps.equalsIgnoreCase("OFF"))
            DecodesDbEditor.turnOffPopUps = true;
        
        DecodesInterface.maintainGoesPdt();
        
        new DecodesDbEditor();
    }

    /**
     * Returns the top-level frame. This is handy for modal dialogs.
     */
    public static Frame getTheFrame()
    {
        return theFrame;
    }

//    /**
//     * Establishes and/or consolidates object references depending on the
//     * type of database interface that was used.
//     * @deprecated this method currently does nothing.
//     */
//    private static void fixObjectReferences(Database db)
//    {
//        if (db.getDbIo().getDatabaseType().equalsIgnoreCase("SQL"))
//        {
//            // TODO - for SQL may need to use key relations to establish
//            // references
//            return;
//        }
//        else // XML database
//        {
//            // Platform objects contain a copy of configurations. Replace
//            // these with references to the configs stored in the config
//            // list.
//            // While doing this, update the platform counts in the configs.
///*
//            for(Iterator it = db.platformList.iterator(); it.hasNext(); )
//            {
//                // If config in platform has same name as a config in the
//                // list, replace the platform's copy with the one in the
//                // list. Else, add the platform's config to the list.
//                Platform p = (Platform)it.next();
//                PlatformConfig pc = p.platformConfig;
//                if (pc != null)
//                {
//                    PlatformConfig listpc =
//                        db.platformConfigList.get(pc.configName);
//                    if (listpc != null)
//                    {
////System.out.println("# Script from Platform's pc = " + pc.decodesScripts.size());
////System.out.println("# Script from PlatformConfig rec = " + listpc.decodesScripts.size());
//
//                        p.platformConfig = listpc;
//                        listpc.numPlatformsUsing++;
//                    }
//                    else
//                    {
//                        db.platformConfigList.add(pc);
//                        pc.numPlatformsUsing = 1;
//                    }
//                }
//            }
//*/
//        }
//
//
//    }
}
