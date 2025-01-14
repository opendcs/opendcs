package decodes.rledit;

import javax.swing.UIManager;

import org.opendcs.database.DatabaseService;
import org.opendcs.database.api.OpenDcsDatabase;

import java.util.*;

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
    private static ResourceBundle genericLabels = null;
    private static ResourceBundle labels = null;
    boolean packFrame = false;
    final private OpenDcsDatabase database;

    /** Construct the application. */
    public RefListEditor(OpenDcsDatabase database)
    {
        this.database = database;
        RefListFrame frame = new RefListFrame(database);
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
        try {
//            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.setLookAndFeel(new javax.swing.plaf.metal.MetalLookAndFeel());
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        Logger.setLogger(new StderrLogger("DecodesDbEditor"));

        // Parse command line arguments.
        cmdLineArgs.parseArgs(args);
        genericLabels = getGenericLabels();
        labels = getLabels();
        Logger.instance().log(Logger.E_INFORMATION,
            "RefListEditor Starting (" + DecodesVersion.startupTag()
            + ") =====================");

        DecodesSettings settings = DecodesSettings.instance();
        DecodesInterface.setGUI(true);
        OpenDcsDatabase database = DatabaseService.getDatabaseFor("RefListEditor", settings);
        database.getLegacyDatabase(Database.class).get().initializeForEditing();
        new RefListEditor(database);
    }
}
