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
package decodes.rledit;

import javax.swing.UIManager;

import org.opendcs.database.DatabaseService;
import org.opendcs.database.api.OpenDcsDatabase;

import java.util.*;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.gui.WindowUtility;
import lrgs.gui.DecodesInterface;

import ilex.util.*;
import decodes.db.*;
import decodes.util.*;

/**
Main class for the reference list editor.
*/
public class RefListEditor 
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    private static ResourceBundle genericLabels = null;
    private static ResourceBundle labels = null;
    boolean packFrame = false;
    final private OpenDcsDatabase database;
    private RefListFrame frame;
    /** Construct the application. */
    public RefListEditor(OpenDcsDatabase database) throws DecodesException
    {
        this.database = database;
        frame = new RefListFrame(database, (v) -> handleExit());
        //Validate frames that have preset sizes
        //Pack frames that have useful preferred size info, e.g. from their layout
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
    private void handleExit() 
    {
        frame.dispose(); // Close the frame gracefully
        // Perform other cleanup tasks if needed before shutting down the app
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
     * @return resource bundle containing eledit-Editor labels for the selected
     * language.
     */
    public static ResourceBundle getLabels()
    {
        if (labels == null)
        {
            DecodesSettings settings = DecodesSettings.instance();
            labels = LoadResourceBundle.getLabelDescriptions(
                "decodes/resources/rledit", settings.language);
        }
        return labels;
    }

    /**
      Command line arguments.
      Only standard non-network-app arguments are required.
    */
    static CmdLineArgs cmdLineArgs = new CmdLineArgs(false, "rledit.log");

    /**
      Main method.
      @param args command line arguments.
    */
    public static void main(String[] args) 
        throws Exception
    {
        try 
        {
            UIManager.setLookAndFeel(new javax.swing.plaf.metal.MetalLookAndFeel());
        }
        catch(Exception ex) 
        {
            log.atError().setCause(ex).log("Unable to set look and feel to Metal.");
        }

        // Parse command line arguments.
        cmdLineArgs.parseArgs(args);
        genericLabels = getGenericLabels();
        labels = getLabels();
        log.info("RefListEditor Starting ({}) =====================", DecodesVersion.startupTag());

        DecodesSettings settings = DecodesSettings.instance();
        DecodesInterface.setGUI(true);
        OpenDcsDatabase database = DatabaseService.getDatabaseFor("RefListEditor", settings);
	    Database db = database.getLegacyDatabase(Database.class).get();
        db.initializeForEditing();
        Database.setDb(db);
        new RefListEditor(database);
    }
}
