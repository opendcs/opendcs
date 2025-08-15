/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.dbeditor;

import java.awt.*;
import java.util.Properties;
import java.util.ResourceBundle;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.gui.WindowUtility;
import lrgs.gui.DecodesInterface;

import ilex.util.LoadResourceBundle;
import ilex.cmdline.*;
import decodes.util.*;
import decodes.db.*;

/**
The MAIN class for the DECODES Database Editor.
*/
public class DecodesDbEditor
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
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


        // Parse command line arguments.
        try { cmdLineArgs.parseArgs(args); }
        catch(IllegalArgumentException ex)
        {
            log.atError().setCause(ex).log("Unable to process command line arguements.");
            System.exit(1);
        }

        log.info("DecodesDbEditor Starting ({}) =====================", DecodesVersion.startupTag());

        DecodesSettings settings = DecodesSettings.instance();

        DecodesInterface.setGUI(true);

        // Construct the database and the interface specified by properties.
        Database db = new decodes.db.Database();
        Database.setDb(db);
        DatabaseIO dbio;

        String dbloc = dbLocArg.getValue();
        if (dbloc.length() > 0)
        {
            dbio = DatabaseIO.makeDatabaseIO(settings.DB_XML, dbloc);
        }
        else
            dbio = DatabaseIO.makeDatabaseIO(settings.editDatabaseTypeCode,
                settings.editDatabaseLocation);

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
        System.out.println("Database initialized.");

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
}