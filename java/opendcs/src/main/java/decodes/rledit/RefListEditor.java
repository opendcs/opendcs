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

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.*;

import ilex.gui.WindowUtility;
import ilex.util.LoadResourceBundle;
import lrgs.gui.DecodesInterface;

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

    /** Construct the application. */
    public RefListEditor()
    {
        RefListFrame frame = new RefListFrame();
        //Validate frames that have preset sizes
        //Pack frames that have useful preferred size info, e.g. from their layout
        if (packFrame) {
            frame.pack();
        }
        else {
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

        // Construct the database and the interface specified by properties.
        Database db = new decodes.db.Database();

        Database.setDb(db);
        DatabaseIO dbio =
            DatabaseIO.makeDatabaseIO(settings.editDatabaseTypeCode,
            settings.editDatabaseLocation);
        db.setDbIo(dbio);
        db.enumList.read();
        db.dataTypeSet.read();
        db.engineeringUnitList.read();

        new RefListEditor();
    }
}
