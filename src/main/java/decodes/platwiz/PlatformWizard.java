package decodes.platwiz;

import javax.swing.JOptionPane;
import javax.swing.JDialog;
import java.util.ResourceBundle;

import ilex.gui.WindowUtility;
import ilex.util.LoadResourceBundle;
import ilex.util.Logger;
import ilex.util.AsciiUtil;

import decodes.db.Database;
import decodes.db.DatabaseIO;
import decodes.db.Platform;
import decodes.db.Site;
import decodes.db.DatabaseException;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesSettings;
import decodes.util.DecodesVersion;

/**
Main class for Platform Wizard
*/
public class PlatformWizard 
{
    private static ResourceBundle genericLabels = null;
    private static ResourceBundle platwizLabels = null;
    
    boolean packFrame = false;

    /** The singleton instance */
    private static PlatformWizard _instance;

    /** The main GUI frame */
    private Frame1 frame;

    /** The Platform object we are constructing. */
    private Platform platform;

    /** True if data was saved. */
    boolean saved;

    /** @returns singleton instance */
    public static PlatformWizard instance()
    {
        genericLabels = getGenericLabels();
        platwizLabels = getPlatwizLabels();
        if (_instance == null)
            _instance = new PlatformWizard();
        return _instance;
    }

    public Frame1 getPlatwizFrame() { return frame; }

    /** Constructs the application */
    public PlatformWizard() 
    {
        frame = new Frame1();
        saved = false;
        //Validate frames that have preset sizes
        //Pack frames that have useful preferred size info, e.g. from their layout
        if (packFrame) {
            frame.pack();
        }
        else {
            frame.validate();
        }
        platform = null;
    }

    public static void resetInstance()
    {
        _instance = null;
        genericLabels = null;
        platwizLabels = null;
    }
    
    /** Called from main to make the frame visible */
    public void show()
    {
        WindowUtility.center(frame).setVisible(true);
    }

    /** The command line arguments */
    static CmdLineArgs cmdLineArgs = new CmdLineArgs(false, "platwiz.log");

    /** 
      Shows a modal error message dialog, centered on the window. 
      @param msg  The message to display.
    */
    public void showError(String msg)
    {
        Logger.instance().warning(msg);
        JOptionPane.showMessageDialog(frame,
            AsciiUtil.wrapString(msg, 60), "Error!", JOptionPane.ERROR_MESSAGE);
    }

    /**
      Launches a model dialog at a reasonable position on the screen.
      @param dlg the dialog
    */
    public void launchDialog(JDialog dlg)
    {
        dlg.validate();
        dlg.setLocationRelativeTo(frame);
        dlg.setVisible(true);
    }

    /**
      @returns the DCP address specified on the StartPanel, or null if none.
    */
    public String getDcpAddress()
    {
        return frame.startPanel.getDcpAddress();
    }

    /**
      @returns numeric GOES ST Channel or -1 if none specified.
    */
    public int getSTChannel()
    {
        return frame.startPanel.getSTChannel();
    }

    /**
      @returns numeric GOES RD Channel or -1 if none specified.
    */
    public int getRDChannel()
    {
        return frame.startPanel.getRDChannel();
    }

    /**
      @returns the USGS Site ID specified on the StartPanel, or null if none.
    */
    public String getUsgsSiteId()
    {
        return frame.startPanel.getUsgsSiteId();
    }

    /** Returns true if GOES self-timed messages should be processed. */
    public boolean processGoesST()
    {
        return frame.startPanel.processGoesST();
    }

    /** Returns true if GOES Random messages should be processed. */
    public boolean processGoesRD()
    {
        return frame.startPanel.processGoesRD();
    }

    /** Returns true if EDL files should be processed. */
    public boolean processEDL()
    {
        return frame.startPanel.processEDL();
    }

    /** 
      Sets the platform object we are editing.
      @param p  The Platform object.
    */
    public void setPlatform(Platform p)
    {
        this.platform = p;
    }

    /** @return the platform object we are editing. */
    public Platform getPlatform() { return platform; }

    /** 
      @return the GUI frame. 
    */
    public Frame1 getFrame() { return frame; }

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
     * @return resource bundle containing PlaWiz-Editor labels for the selected
     * language.
     */
    public static ResourceBundle getPlatwizLabels()
    {
        if (platwizLabels == null)
        {
            DecodesSettings settings = DecodesSettings.instance();
            platwizLabels = LoadResourceBundle.getLabelDescriptions(
                "decodes/resources/platwiz", settings.language);
        }
        return platwizLabels;
    }
    
    /**
      Main method.
      @param args command line arguments.
    */
    public static void main(String[] args) 
    {
        // Parse command line arguments.
        cmdLineArgs.parseArgs(args);

        Logger.instance().info(
            "PlatformWizard Starting (" + DecodesVersion.startupTag()
            + ") =====================");

        // Initialize settings from properties file
        DecodesSettings settings = DecodesSettings.instance();

        // Construct the database and the interface specified by properties.
        try
        {
            Database db = new decodes.db.Database();
            Database.setDb(db);
            DatabaseIO dbio = DatabaseIO.makeDatabaseIO(settings);

            Platform.configSoftLink = false;
            System.out.print("Reading DB: "); System.out.flush();
            db.setDbIo(dbio);
            System.out.print("Enum, "); System.out.flush();
            db.enumList.read();
            System.out.print("DataType, "); System.out.flush();
            db.dataTypeSet.read();
            System.out.print("EU, "); System.out.flush();
            db.engineeringUnitList.read();
            Site.explicitList = true;
            System.out.print("Site, "); System.out.flush();
            db.siteList.read();
            System.out.print("Equip, "); System.out.flush();
            db.equipmentModelList.read();

            System.out.print("Config, "); System.out.flush();
            db.platformConfigList.read();
            System.out.print("Plat, "); System.out.flush();
            db.platformList.read();
            db.platformConfigList.countPlatformsUsing();

            // Note: need data sources for loading sample message.
            System.out.print("DataSource, "); System.out.flush();
            db.dataSourceList.read();

            System.out.println("DONE.");

//            db.presentationGroupList.read();
//            db.routingSpecList.read();
//            db.networkListList.read();
        }
        catch(DatabaseException ex)
        {
            Logger.instance().fatal("Cannot initialize DECODES database: "+ex);
            System.exit(1);
        }

        // Initialization for DB editor - read all the collections:
        Site.explicitList = true;

        instance().show();
    }
}
