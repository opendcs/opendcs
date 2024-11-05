/*
*  $Id$
*  
*  $Log$
*  Revision 1.5  2016/04/15 19:31:22  mmaloney
*  LRGS Server List sorted by access time, most-recent-first.
*
*  Revision 1.4  2016/02/23 19:55:18  mmaloney
*  Refactor to allow a no-decode version of the browser.
*
*  Revision 1.3  2015/07/17 13:10:30  mmaloney
*  *** empty log message ***
*
*  Revision 1.2  2014/10/08 17:24:49  mmaloney
*  Added combo box for decoder format.
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.13  2013/03/28 19:19:32  mmaloney
*  User temp files are now placed under DCSTOOL_USERDIR which may be different
*  from DCSTOOL_HOME on linux/unix multi-user installations.
*
*  Revision 1.12  2013/03/28 17:29:09  mmaloney
*  Refactoring for user-customizable decodes properties.
*
*  Revision 1.11  2013/02/28 16:44:26  mmaloney
*  New SearchCriteriaEditPanel implementation.
*
*/
package lrgs.gui;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import org.opendcs.gui.GuiConstants;
import org.opendcs.gui.PasswordWithShow;

import javax.swing.border.BevelBorder;

import decodes.db.DatabaseException;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesSettings;
import decodes.util.ResourceFactory;

import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Properties;
import java.util.Enumeration;

import ilex.gui.*;
import ilex.cmdline.*;
import ilex.util.LoadResourceBundle;
import ilex.util.Logger;
import ilex.util.StderrLogger;
import ilex.util.EnvExpander;
import lrgs.common.*;
import lrgs.ldds.*;
import lrgs.rtstat.RtStatFrame;

/**
The MessageBrowser allows the user to display DCP messages on the screen
and save them to a file. It uses DDS (not CORBA) to pull DCP messages from
the remote system.
*/
@SuppressWarnings("serial")
public class MessageBrowser extends MenuFrame
    implements DcpMsgOutputMonitor, SearchCritEditorParent
{
    private static ResourceBundle labels = null;
    private static ResourceBundle genericLabels = null;

    public static final int DEFAULT_PORT_NUM = LddsParams.DefaultPort;
    public static final int StartHeight = 620;
    public static final int StartWidth = 950;//900;

    private static final String TITLE = "DCP Message Browser";
    private String hostName, userName;
    private int portNum;
    private SearchCriteria searchcrit;
    private SearchCriteriaEditorIF scedit;

    private JComboBox hostField;
    private JTextField portField = new JTextField(15), userField = new JTextField(15);
    private PasswordWithShow passwordField = new PasswordWithShow(GuiConstants.DEFAULT_PASSWORD_WITH);
    private JButton connectButton;
    private JTextField scfileField;
    private JButton scSelectButton, scEditButton;
    private JTextField prefixField, suffixField;
    private JTextField beforeDataField, afterDataField;
    private JCheckBox wrapCheck;

    private JTextArea messageArea;
    private JScrollPane messagePane;
    private MessageAreaOutputStream msgDisplayStream;
    private DcpMsgOutputThread msgOutputThread;
    private boolean displayPaused;
    private JButton nextMessageButton, saveToFileButton, clearButton;
    private JButton displayAllButton;
    private JLabel statusBar;

    private Properties connectionList;

    private static JFileChooser filechooser;
    private static final String[] scExtensions = { "sc" };

    private LddsClient client;
    private boolean firstAfterConnect;
    private boolean wrapLines;

    private boolean showRaw, doDecode;
    private String lastSearchCrit;
    private boolean displayingAll;

    private static String[] showChoices;
    private JComboBox showCombo;
    private JComboBox outCombo;
    private static String[] outFmts;
    private static boolean canDecode = true;


    /**
      Construct new MessageBrowser frame.
      @param hostName DDS server host to connect to
      @param portNum DDS port number
      @userName user name to use in connection
    */
    public MessageBrowser(String hostName, int portNum, String userName)
    {

        super(TITLE);

        try
        {
            ResourceFactory.instance().initDbResources();
        }
        catch (DatabaseException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try { cl.loadClass("lrgs.ddsrecv.DdsRecvConnectCfg"); }
        catch(ClassNotFoundException ex)
        {
            canDecode = false;
            showRaw = true;
            doDecode = false;
        }


        getMyLabelDescriptions();        
        setTitle(labels.getString("MessageBrowser.frameTitle"));
        filechooser = new JFileChooser();

        if (canDecode)
        {
            showChoices = new String[3];
            showChoices[0] = labels.getString("MessageBrowser.rawCombo");
            showChoices[1] = labels.getString("MessageBrowser.rawDecodedCombo");
            showChoices[2] = labels.getString("MessageBrowser.decodedCombo");
        }

        this.hostName = hostName != null ? hostName : "";
        this.portNum = portNum == 0 ? DEFAULT_PORT_NUM : portNum;
        this.userName = userName != null ? userName : "";
        scedit = null;
        msgOutputThread = null;
        displayingAll = false;

        searchcrit = new SearchCriteria();

        initProperties();
        MessageOutput.initProperties();
        buildMenuBar();
        initConnectionList();

        Container contpane = getContentPane();
        contpane.setLayout(new BorderLayout());

        statusBar = new JLabel(
                labels.getString("MessageBrowser.initializingLabel"));
        statusBar.setBorder(new BevelBorder(BevelBorder.LOWERED));
        contpane.add(statusBar, BorderLayout.SOUTH);

        // West contains Server, Search Criteria and Output Format Groups:
        JPanel west = new JPanel(new GridBagLayout());
        contpane.add(west, BorderLayout.WEST);

        // NorthWest contains Server group
        JPanel northwest = new JPanel(new GridBagLayout());
        northwest.setBorder(new TitledBorder(
                labels.getString("MessageBrowser.serverTitle")));

        northwest.add(new JLabel(
                labels.getString("MessageBrowser.hostName")),
            new GridBagConstraints(0, 0, 1, 1, 0.2, 1.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(2, 5, 2, 5), 0, 0)); 

        hostField = new JComboBox();
        hostField.setEditable(true);
        hostField.addActionListener(
            new ActionListener()
            {
                public void actionPerformed(ActionEvent ae)
                {
                    RtStatFrame.setFieldsFromHostSelection(hostField, connectionList, portField, 
                        userField, passwordField);
                }
            });
        RtStatFrame.loadConnectionsField(hostField, connectionList, "");
        northwest.add(hostField,
            new GridBagConstraints(1, 0, 1, 1, 0.8, 1.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(2, 5, 2, 5), 0, 0)); 

        northwest.add(new JLabel(labels.getString("MessageBrowser.port")),
            new GridBagConstraints(0, 1, 1, 1, 0.2, 1.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(2, 5, 2, 5), 0, 0)); 
        northwest.add(portField,
            new GridBagConstraints(1, 1, 1, 1, 0.8, 1.0,
                GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
                new Insets(2, 5, 2, 5), 0, 0)); 
        portField.setText(GuiApp.getProperty("MessageBrowser.Port"));

        Dimension d = portField.getPreferredSize();
        hostField.setPreferredSize(d);

        northwest.add(new JLabel(
                labels.getString("MessageBrowser.userName")),
            new GridBagConstraints(0, 2, 1, 1, 0.2, 1.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(2, 5, 2, 5), 0, 0)); 
        northwest.add(userField,
            new GridBagConstraints(1, 2, 1, 1, 0.8, 1.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(2, 5, 2, 5), 0, 0)); 
        if(this.userName.length()>0)
            userField.setText(this.userName);
        else
        userField.setText(GuiApp.getProperty("MessageBrowser.User"));

        northwest.add(new JLabel(
                labels.getString("MessageBrowser.password")),
            new GridBagConstraints(0, 3, 1, 1, 0.2, 1.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(2, 5, 2, 5), 0, 0)); 
        //northwest.add(passwordField = new JTextField(15),
        northwest.add(passwordField,
            new GridBagConstraints(1, 3, 1, 1, 0.8, 1.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(2, 5, 2, 5), 0, 0)); 

        // Connect button across bottom:
        connectButton =
            new SingleClickButton(labels.getString("MessageBrowser.connect"))
            {
                public void buttonPressed(AWTEvent event)
                {
                    connectButtonPress();
                }
            };
        northwest.add(connectButton, 
            new GridBagConstraints(0, 4, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(2, 5, 2, 5), 0, 0)); 

        // Finally, add northwest to the west panel.
        west.add(northwest, 
            new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0,
                GridBagConstraints.NORTH, GridBagConstraints.BOTH,
                new Insets(2, 5, 2, 5), 0, 0)); 


        // Midwest contains Search Criteria Group.
        JPanel midwest = new JPanel(new BorderLayout());
        midwest.setBorder(new TitledBorder(
                labels.getString("MessageBrowser.searchCriteria")));
        west.add(midwest, 
            new GridBagConstraints(0, 1, 1, 1, 0.0, 0.2,
                GridBagConstraints.NORTH, GridBagConstraints.BOTH,
                new Insets(2, 5, 2, 5), 0, 0)); 
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        p.add(new JLabel(labels.getString("MessageBrowser.fileName")));
        p.add(scfileField = new JTextField(15));
        scfileField.setText(
            GuiApp.getProperty("MessageBrowser.DefaultSearchCrit"));

        midwest.add(p, BorderLayout.NORTH);
        p = new JPanel(new FlowLayout(FlowLayout.CENTER));
        scSelectButton =
            new SingleClickButton(genericLabels.getString("select"))
            {
                public void buttonPressed(AWTEvent event)
                {
                    scSelectButtonPress();
                }
            };
        p.add(scSelectButton);
        scEditButton =
            new SingleClickButton(
                    "  " + genericLabels.getString("edit") + "  ")
            {
                public void buttonPressed(AWTEvent event)
                {
                    scEditButtonPress();
                }
            };
        p.add(scEditButton);
        midwest.add(p, BorderLayout.CENTER);
        p = new JPanel(new FlowLayout(FlowLayout.CENTER));
        midwest.add(p, BorderLayout.SOUTH);



        // Center contains big messge display area with buttons on bottom.
        JPanel center = new JPanel(new BorderLayout());
        contpane.add(center, BorderLayout.CENTER);
        messageArea = new JTextArea();
        msgDisplayStream = new MessageAreaOutputStream();

        messagePane = new JScrollPane(messageArea);
        messagePane.setBorder(new BevelBorder(BevelBorder.LOWERED));
        messageArea.setLineWrap(wrapLines);
        messageArea.setEditable(false);
        Font oldfont = messageArea.getFont();
        messageArea.setFont(new Font("Monospaced", Font.PLAIN, oldfont.getSize()));

        center.add(messagePane, BorderLayout.CENTER);
        p = new JPanel(new GridBagLayout());
        nextMessageButton = new JButton(
                labels.getString("MessageBrowser.displayNext"));
        nextMessageButton.addActionListener(
            new ActionListener()
            {
                public void actionPerformed(ActionEvent av)
                {
                    nextMessageButtonPress();
                }
            });
        p.add(nextMessageButton,
            new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(2, 0, 2, 5), 0, 0)); 

        displayAllButton = new JButton(
                labels.getString("MessageBrowser.displayAll"));
        displayAllButton.addActionListener(
            new ActionListener()
            {
                public void actionPerformed(ActionEvent av)
                {
                    displayAllButtonPress();
                }
            });
        p.add(displayAllButton,
            new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(2, 0, 2, 5), 0, 0)); 


        clearButton = new JButton("    " + 
                genericLabels.getString("clear")+ "    ");
        clearButton.addActionListener(
            new ActionListener()
            {
                public void actionPerformed(ActionEvent av)
                {
                    clearButtonPress();
                }
            });
        p.add(clearButton,
            new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(2, 5, 2, 5), 0, 0)); 

        saveToFileButton =
            new SingleClickButton(
                    labels.getString("MessageBrowser.saveToFile"))
            {
                public void buttonPressed(AWTEvent event)
                {
                    saveToFileButtonPress();
                }
            };


        p.add(saveToFileButton,
            new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(2, 5, 2, 0), 0, 0)); 

        center.add(p, BorderLayout.SOUTH);

        // Southwest contains Output Format group.
        JPanel southwest = new JPanel(new GridBagLayout());
        southwest.setBorder(new TitledBorder(
                labels.getString("MessageBrowser.displayFormat")));

        southwest.add(new JLabel(
                labels.getString("MessageBrowser.beforeMsg")),
            new GridBagConstraints(0, 0, 1, 1, 0.2, 1.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(2, 5, 2, 5), 0, 0)); 

        southwest.add(prefixField = new JTextField(16),
            new GridBagConstraints(1, 0, 1, 1, 0.8, 1.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(2, 5, 2, 5), 0, 0)); 
        prefixField.setText(GuiApp.getProperty("MessageBrowser.Prefix"));
        southwest.add(new JLabel(labels.getString("MessageBrowser.afterMsg")),
            new GridBagConstraints(0, 1, 1, 1, 0.2, 1.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(2, 5, 2, 5), 0, 0)); 
        southwest.add(suffixField = new JTextField(16),
            new GridBagConstraints(1, 1, 1, 1, 0.8, 1.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(2, 5, 2, 5), 0, 0)); 
        suffixField.setText(GuiApp.getProperty("MessageBrowser.Suffix"));


        if (canDecode)
        {
            southwest.add(new JLabel(labels.getString("MessageBrowser.show")),
                new GridBagConstraints(0, 2, 1, 1, 0.2, 1.0,
                    GridBagConstraints.EAST, GridBagConstraints.NONE,
                    new Insets(2, 5, 2, 5), 0, 0)); 
            showCombo = new JComboBox(showChoices);
            showCombo.setSelectedItem(
                GuiApp.getProperty("MessageBrowser.Show", "Raw"));
            southwest.add(showCombo,
                new GridBagConstraints(1, 2, 1, 1, 1.0, 1.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(2, 5, 2, 5), 0, 0)); 

            showCombo.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(java.awt.event.ActionEvent e)
                    {
                        int i = showCombo.getSelectedIndex();
                        showRaw = (i == 0 || i == 1);
                        doDecode = (i == 1 || i == 2);
                        GuiApp.setProperty("MessageBrowser.Show", showChoices[i]);
                    }
                });

            //=========
            southwest.add(new JLabel(labels.getString("MessageBrowser.outFormat")),
                new GridBagConstraints(0, 3, 1, 1, 0.2, 1.0,
                    GridBagConstraints.EAST, GridBagConstraints.NONE,
                    new Insets(2, 5, 2, 5), 0, 0));
            outFmts = DecodesInterface.getOutputFormats();
            if (outFmts == null)
            {
                String[] thisFmt = new String[1];
                thisFmt[0] = "human-readable"; 
                outCombo = new JComboBox(thisFmt);
            } else {

                outCombo = new JComboBox(outFmts);
            }
            outCombo.setSelectedItem(
                GuiApp.getProperty("MessageBrowser.OutputFormat", "human-readable"));
            southwest.add(outCombo,
                new GridBagConstraints(1, 3, 1, 1, 1.0, 1.0,
                    GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(2, 5, 2, 5), 0, 0));

            outCombo.addActionListener(
                new ActionListener()
                {
                    public void actionPerformed(java.awt.event.ActionEvent e)
                    {
                        int i = outCombo.getSelectedIndex();
                        GuiApp.setProperty("MessageBrowser.OutputFormat", outFmts[i]);
                    }
                });
            //=========

            southwest.add(new JLabel(
                    labels.getString("MessageBrowser.beforeData")),
                new GridBagConstraints(0, 4, 1, 1, 0.2, 1.0,
                    GridBagConstraints.EAST, GridBagConstraints.NONE,
                    new Insets(2, 5, 2, 5), 0, 0)); 
            southwest.add(beforeDataField = new JTextField(16),
                new GridBagConstraints(1, 4, 1, 1, 0.8, 1.0,
                    GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                    new Insets(2, 5, 2, 5), 0, 0)); 

            southwest.add(new JLabel(labels.getString("MessageBrowser.afterData")),
                new GridBagConstraints(0, 5, 1, 1, 0.2, 1.0,
                    GridBagConstraints.EAST, GridBagConstraints.NONE,
                    new Insets(2, 5, 2, 5), 0, 0)); 
            southwest.add(afterDataField = new JTextField(16),
                new GridBagConstraints(1, 5, 1, 1, 0.8, 1.0,
                    GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                    new Insets(2, 5, 2, 5), 0, 0)); 
        }

        wrapCheck = new JCheckBox(
                labels.getString("MessageBrowser.wraplonglines"),
            GuiApp.getBooleanProperty("MessageBrowser.WrapLongLines", false));
        southwest.add(wrapCheck,
            new GridBagConstraints(0, (canDecode ? 6 : 2), 2, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(2, 6, 2, 5), 0, 0)); 
        wrapLines = wrapCheck.isSelected();
        messageArea.setLineWrap(wrapLines);
        wrapCheck.addItemListener(
            new ItemListener()
            {
                public void itemStateChanged(ItemEvent ev)
                {
                    wrapLines = ev.getStateChange() == ItemEvent.SELECTED;
                    messageArea.setLineWrap(wrapLines);
                    GuiApp.setProperty("MessageBrowser.WrapLongLines", 
                        wrapLines ? "true" : "false");
                }
            });

        west.add(southwest, 
            new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0,
                GridBagConstraints.NORTH, GridBagConstraints.BOTH,
                new Insets(2, 5, 2, 5), 0, 0)); 

        String dir = System.getProperty("user.dir");
        filechooser.setCurrentDirectory(new File(dir));
        filechooser.addChoosableFileFilter(new ExtensionFileFilter(
            scExtensions, "Search Criteria"));

        if (canDecode)
        {
            beforeDataField.setText(
                GuiApp.getProperty("MessageBrowser.BeforeData", "----\\n"));
            afterDataField.setText(
                GuiApp.getProperty("MessageBrowser.AfterData", ""));

            int i = showCombo.getSelectedIndex();
            showRaw = (i == 0 || i == 1);
            doDecode = (i == 1 || i == 2);
        }
    }

    /**
     * Queries the GuiApp singleton for all the properties used by this
     * display. This makes sure they're in the property set and initializes
     * default values.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void initProperties()
    {
        GuiApp.getProperty("MessageBrowser.Prefix", "\\n");
        GuiApp.getProperty("MessageBrowser.Suffix", "\\n");
        String u = System.getProperty("user.name");
        GuiApp.getProperty("MessageBrowser.User", u != null ? u : "");
        GuiApp.getProperty("MessageBrowser.Port",
            "" + LddsParams.DefaultPort);
        GuiApp.getProperty("MessageBrowser.Timeout", "60");
        GuiApp.getProperty("MessageBrowser.DefaultSearchCrit", 
            "$DCSTOOL_USERDIR/MessageBrowser.sc");
        String nm = "MessageBrowser.WrapLongLines";
        GuiApp.getProperty(nm, "true");
        EditPropsAction.registerEditor(nm, 
            new JComboBox(new String[] { "true", "false" }));
        nm = "MessageBrowser.showMsgMetaData";
        GuiApp.getProperty(nm, "false");
        EditPropsAction.registerEditor(nm, 
            new JComboBox(new String[] { "true", "false" }));

        // Try to initialize DECODES:
        String dpf = GuiApp.getProperty("MessageBrowser.DecodesPropFile",
            "$DECODES_INSTALL_DIR/decodes.properties");
        dpf = EnvExpander.expand(dpf, System.getProperties());

        try
        {
            DecodesInterface.initDecodes(dpf);
            if (canDecode)
            {
                DecodesInterface.initializeForDecoding();
            }
        }
        catch(decodes.util.DecodesException ex)
        {
            Logger.instance().log(Logger.E_FAILURE,
                "Error initializing DECODES (Decoding functions disabled): "
                + ex);
            return;
        }
        catch(NoClassDefFoundError ex)
        {
            Logger.instance().log(Logger.E_FAILURE,
                "Cannot find DECODES classes. "
                + "Check CLASSPATH and software installation: " + ex);
            return;
        }

        if (canDecode)
        {
            nm = "MessageBrowser.PresentationGroup";
            String pgs[] = DecodesInterface.getPresentationGroups();
            GuiApp.getProperty(nm, "empty-presentation");
            if (pgs != null && pgs.length > 0)
                EditPropsAction.registerEditor(nm, new JComboBox(pgs));

            nm = "MessageBrowser.OutputFormat";
            String fmts[] = DecodesInterface.getOutputFormats();
            GuiApp.getProperty(nm, "human-readable");
            if (fmts != null && fmts.length > 0)
                EditPropsAction.registerEditor(nm, new JComboBox(fmts));

            GuiApp.getProperty("MessageBrowser.TimeZone", "UTC");

            nm = "MessageBrowser.EnableEquations";
            GuiApp.getProperty(nm, "true");
            EditPropsAction.registerEditor(nm, 
                new JComboBox(new String[] { "true", "false" }));

            nm = "MessageBrowser.Show";
            GuiApp.getProperty(nm, "Raw");
            EditPropsAction.registerEditor(nm, new JComboBox(showChoices));

            GuiApp.getProperty("MessageBrowser.BeforeData", "----\\n");

            GuiApp.getProperty("MessageBrowser.AfterData", "");
        }
    }

    /**
      Opens the file specified by the MessageBrowser.ConnectionsFile
      property and load the connection combo box.
    */
    private void initConnectionList()
    {
        connectionList = new Properties();
        String fn = LddsClient.getLddsConnectionsFile();
        fn = EnvExpander.expand(fn, System.getProperties());
        File file = new File(fn);
        try
        {
            FileInputStream fis = new FileInputStream(file);
            connectionList.load(fis);
            fis.close();
        }
        catch(IOException ioe)
        {
            System.out.println("No previously recorded connections");
        }
    }

    /** Sets the screen size. */
    public void setSize(Dimension d)
    {
        GuiApp.setProperty("MessageBrowser.height", ""+d.height);
        GuiApp.setProperty("MessageBrowser.width", ""+d.width);
    }

    /** Called when screen moved. Saves location in properties. */
    public void movedTo(Point p)
    {
        GuiApp.setProperty("MessageBrowser.x", ""+p.x);
        GuiApp.setProperty("MessageBrowser.y", ""+p.y);
    }

    /** Starts the GUI at the specified location. */
    public void startup(int x, int y)
    {
        int width = GuiApp.getIntProperty("MessageBrowser.width", StartWidth);
        int height = GuiApp.getIntProperty("MessageBrowser.height", StartHeight);
        x = GuiApp.getIntProperty("MessageBrowser.x", x);
        y = GuiApp.getIntProperty("MessageBrowser.y", y);
        launch(x, y, width, height);
        statusBar.setText(labels.getString("MessageBrowser.notConnectedLabel"));
    }

    /**
      @return array of 1 action, which starts the properties edit dialog.
    */
    protected AbstractAction[] getFileMenuActions()
    {
        AbstractAction[] ret = new AbstractAction[1];

        ret[0] = new EditPropsAction(this, "Message Browser ",
            new String[] { "General", "MessageBrowser", "MessageOutput" });

        return ret;
    }

    /** @return null -- don't need an Edit menu. */
    protected AbstractAction[] getEditMenuActions()
    {
        return null;
    }

    /**
    @return true - we do want a Help menu.
    */
    protected boolean isHelpMenuEnabled()
    {
        return true;
    }

    /** @return the name for the help page "MessageBrowser.html" */
    protected String getHelpFileName() { return "MessageBrowser.html"; }

    /**
     * Performs clean up before TopLevel exits.
     * Override this method in your subclass. It is called when the user selects
     * 'Exit' from the file menu. Do any cleanup and resource releasing necessary.
     * The default implementation here does nothing.
     */
    public void cleanupBeforeExit()
    {
        if (displayingAll)
        {
            stopDisplayAll();
        }
        if (client != null)
        {
            try
            {
                client.sendGoodbye();
                client.disconnect();
                client = null;
            }
            catch(Exception e) {}
        }
    }

    /**
      Called when connect button pressed.
      Uses current info from the hostname, port, user, and password fields
      to open the DDS connection and send the inital "Hello" message.
    */
    public void connectButtonPress()
    {
        statusBar.setText(labels.getString("MessageBrowser.connectingLabel"));
        if (client != null)
        {
            client.disconnect();
        }
        client = null;
        if (msgOutputThread != null)
        {
            msgOutputThread.cleanupAndDie();
        }
        msgOutputThread = null;
        nextMessageButton.setEnabled(true);

        int port;
        try
        {
            port = Integer.parseInt(portField.getText());
        }
        catch(NumberFormatException nfe)
        {
            port = LddsParams.DefaultPort;
        }

        String errmsg = null;
        hostName = (String)hostField.getSelectedItem();
        String pw = new String(passwordField.getPassword());

        try
        {
            client = new LddsClient(hostName, port);
            client.connect();

            if (pw.length() > 0)
            {
                client.sendAuthHello(userField.getText(), pw);
            }
            else
            {
                client.sendHello(userField.getText());
            }

            msgOutputThread = new DcpMsgOutputThread(this, client,
                msgDisplayStream, 5, "", "");
            displayPaused = true;
            msgOutputThread.start();
            statusBar.setText(LoadResourceBundle.sprintf(
                    labels.getString("MessageBrowser.connectedToLabel"),
                    hostName, port));
        }
        catch(UnknownHostException uhe)
        {
            errmsg = LoadResourceBundle.sprintf(
                    labels.getString("MessageBrowser.unknownHostErr"),
                    hostName) + uhe;
        }
        catch(IOException ioe)
        {
            errmsg = labels.getString("MessageBrowser.ioConnectErr") + ioe;
        }
        catch(ProtocolError pe)
        {
            errmsg = pe.toString();
        }
        catch(ServerError se)
        {
            errmsg = se.toString();
        }
        if (errmsg != null)
        {
            showError(errmsg);
            client = null;
            statusBar.setText(
                    labels.getString("MessageBrowser.notConnectedLabel"));
        }
        else
        {
            statusBar.setText(LoadResourceBundle.sprintf(
                    labels.getString("MessageBrowser.connectedToAsUserLabel"),
                    hostName, port, userField.getText()));

            firstAfterConnect = true;
            RtStatFrame.updateConnectionList(hostName, portField.getText(),
                userField.getText(), connectionList, pw);
            RtStatFrame.loadConnectionsField(hostField, connectionList, hostName);
        }
    }

    /**
      Called when Search Criteria Select button is pressed. Opens a
      FileChooser dialog.
    */
    public void scSelectButtonPress()
    {
        if (filechooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
        {
            scfileField.setText(filechooser.getSelectedFile().getPath());
        }
    }

    /**
      Called when Search Criteria Edit button is pressed. 
      If an SC Edit dialog is already active, just switch to it. Otherwise
      start one on the currently selected criteria.
    */
    public void scEditButtonPress()
    {
        if (scedit != null)
        {
            scedit.toFront();
            return;
        }

        String s = scfileField.getText();
        s = EnvExpander.expand(s, System.getProperties());

        decodes.util.ResourceFactory rf = 
            decodes.util.ResourceFactory.instance();
        try
        {
            if (s == null || s.length() == 0)
            {
                scedit = rf.getSearchCriteriaEditor(null);
            }
            else
            {
                scedit = rf.getSearchCriteriaEditor(new File(s));
            }
        }
        catch(IOException ioe)
        {
            showError(LoadResourceBundle.sprintf(
                    labels.getString("MessageBrowser.cannotEditErr"),
                    s) + ioe);
            return;
        }
        scedit.setParent((SearchCritEditorParent)this);
        scedit.startup(x+40, y+40);
        scedit.setAutoSave(true);
        lastSearchCrit = "";
    }

    /**
     * Checks to make sure search criteria is up to date and whether
     * or not it needs to be sent to the server.
     * If a search criteria editor window is up, check the contents
     * of the GUI window, rather than the file on disk.
     * After calling this function the local variable 'searchcrit' will
     * be guaranteed to be up-to-date.
     * @return true if search criteria needs to be sent to the server.
     */
    private boolean checkSearchCriteria()
    {
        if (lastSearchCrit == null)
        {
            lastSearchCrit = "";
        }
        String curSCName = scfileField.getText().trim();
        curSCName = EnvExpander.expand(curSCName, System.getProperties());

        // Determine if search criteria needs to be sent.
        boolean needToSendSC = false;

        // If a search-criteria-editor window is up, send the crit if
        // anything has been changed in it.
        if (scedit != null)
        {
            if (firstAfterConnect)
            {
                // They are different!
                searchcrit = scedit.getCurrenCriteria();
                needToSendSC = true;
            }
            // Else no changes have been made in the editor.
            else
            {
                SearchCriteria fromEditor = scedit.getCurrenCriteria();
                needToSendSC = !fromEditor.equals(searchcrit);
                searchcrit = fromEditor;
            }
        }
        // Else no editor active, just go by the filename specified.
        else if (curSCName.length() > 0
              && (firstAfterConnect || !curSCName.equals(lastSearchCrit)))
        {
            File f = new File(curSCName);
            try
            {
                searchcrit = new SearchCriteria(f);
            }
            catch(Exception ioe)
            {
                if (ioe instanceof FileNotFoundException 
                 && curSCName.endsWith("MessageBrowser.sc"))
                {
                    try 
                    {
                        f.createNewFile(); 
                        try 
                        {
                            searchcrit = new SearchCriteria(f);
                        }
                        catch(Exception ex) {}
                    }
                    catch(IOException ex2)
                    {
                        showError(LoadResourceBundle.sprintf(
                                labels.getString(
                                "MessageBrowser.cannotReadCreateErr"),
                                f.getPath()) + ex2.toString());
                        return false;
                    }
                }
                else
                {
                    showError(LoadResourceBundle.sprintf(
                            labels.getString("MessageBrowser.cannotOpenErr"),
                            curSCName) + ioe.toString());
                    return false;
                }
            }
            needToSendSC = true;
            lastSearchCrit = curSCName;
        }

        return needToSendSC;
    }

    /**
      Called when 'Clear' button pressed at bottom of screen.
      Clears the messgae area.
    */
    public void clearButtonPress()
    {
        messageArea.setText("");
    }

    /**
      Called when 'Next Message' button pressed at bottom of screen.
      If searchcrit needs to be sent, send it.
      Tell the background msgOutputThread to start & retrieve 1 message.
    */
    public void nextMessageButtonPress()
    {
        if (client == null)
        {
            connectButtonPress();
            if (client == null) // unsuccessful connect?
            {
                return;
            }
        }

        if (checkSearchCriteria())
        {
            if (!sendSearchCriteria(searchcrit))
            {
                return;
            }
        }
        msgOutputThread.prefix = prefixField.getText();
        msgOutputThread.suffix = suffixField.getText();
        msgOutputThread.timeout = 
            GuiApp.getIntProperty("MessageBrowser.Timeout", 5);
        msgOutputThread.showRaw = showRaw;
        msgOutputThread.doDecode = doDecode;
        msgOutputThread.beforeData = canDecode ? beforeDataField.getText() : null;
        msgOutputThread.afterData = canDecode ? afterDataField.getText() : null;
        msgOutputThread.showMetaData = 
            GuiApp.getBooleanProperty("MessageBrowser.showMsgMetaData", false);

        displayPaused = false;
        nextMessageButton.setEnabled(false);
    }

    /**
      Called if the 'send network lists' checkbox is checked right after
      the search criiteria is sent.
      Finds the network lists referenced in the search criteria and sends
      them to the server.
    */
    public void sendNetworkLists(SearchCriteria searchcrit)
    {
        if (searchcrit.NetlistFiles == null 
         || searchcrit.NetlistFiles.size() == 0)
        {
            Logger.instance().debug3("No lists to send.");
            return;  // no lists to send.
        }

        for(String s : searchcrit.NetlistFiles)
        {
            if (s.equalsIgnoreCase("<all>")
             || s.equalsIgnoreCase("<production>"))
            {
                continue;
            }

            File f = NetlistFinder.find(s);
            if (f != null)
            {
                try
                {
                    client.sendNetList(f, s);
                }
                catch(Exception e)
                {
                    System.err.println("Error sending network list "
                        + s + ": " + e);
                }
            }
            else
            {
                Logger.instance().warning("Cannot find netlist '" + s + "'");
            }
        }
    }

    /**
      Sends the search criteria to the DDS server.
      @return true if success and we should continue with message display.
    */
    public boolean sendSearchCriteria(SearchCriteria searchcrit)
    {
        firstAfterConnect = false;

        Logger.instance().debug3("Sending network lists.");
        sendNetworkLists(searchcrit);

        String errmsg = null;
        try
        {
            client.sendSearchCrit(searchcrit);
        }
        catch(IOException ioe)
        {
            // Probably means socket error.
            errmsg = labels.getString("MessageBrowser.ioSendingSCErr") + ioe;
            firstAfterConnect = true;
            client = null;   // Force reconnect
        }
        catch(ProtocolError pe)
        {
            errmsg = pe.toString();
            client = null;   // Force reconnect
        }
        catch(ServerError se)
        {
            errmsg = se.toString();
        }
        if (errmsg != null)
        {
            showError(errmsg);
            return false;
        }

        return true;
    }


    /**
      From the SearchCritEditorParent interface, informs us that the user
      has closed the search criiteria editor.
    */
    public void closingSearchCritEditor()
    {
        scedit = null;
    }

    /**
      Called when Save To File button is pressed.
      Starts a new MessageOutput dialog with the current connection &
      search crit parameters.
    */
    public void saveToFileButtonPress()
    {
        // Get port number
        int port;
        try
        {
            port = Integer.parseInt(portField.getText());
        }
        catch(NumberFormatException nfe)
        {
            port = LddsParams.DefaultPort;
        }

        // Make sure current 'searchcrit' object is up-to-date and make
        // a copy for the output thread to use.
        checkSearchCriteria();
        SearchCriteria outputcrit = new SearchCriteria(searchcrit);

        MessageOutput output = new MessageOutput(
            (String)hostField.getSelectedItem(), port, userField.getText(),
            outputcrit, prefixField.getText(), suffixField.getText(),
            true, doDecode,
            (canDecode ? beforeDataField.getText() : null),
            (canDecode ? afterDataField.getText() : null), showRaw);
        //String s = passwordField.getText().trim();
        String s = new String(passwordField.getPassword());
        if (s.length() > 0)
        {
            output.setPassword(s);
        }

        output.startup(x+40, y+40);
    }

    /**
      DcpMsgOutputMonitor methods telling us a message was just displayed.
      Make sure that the text area is scrolled to the bottom.
    */
    public void dcpMsgOutputStatus(DcpMsg msg)
    {
        messageArea.setCaretPosition(messageArea.getText().length());
        if (!displayingAll)
        {
            displayPaused = true;
            nextMessageButton.setEnabled(true);
        }
    }

    /**
      DcpMsgOutputMonitor method: Normally we allow one message to display
      then we return false, causing the output thread to pause after each
      message is displayed.
    */
    public boolean dcpMsgOutputIsPaused()
    {
        return displayPaused;
    }

    /** 
    Called when output thread encounters an error. Display it in a dialog.
    @param msg the error message
    */
    public void dcpMsgOutputError(String msg)
    {
        showError(msg);
        displayPaused = true;
        nextMessageButton.setEnabled(true);
        // Close connection so that reconnect is forced next time.
        client.disconnect();
        statusBar.setText(
                labels.getString("MessageBrowser.notConnectedLabel"));
        client = null;
        if (displayingAll)
        {
            stopDisplayAll();
        }
    }

    /**
    Called from the output thread when the until time is reached.
    Terminates the 'display all' function.
    */
    public void dcpMsgOutputDone()
    {
        JOptionPane.showMessageDialog(this,
            labels.getString("MessageBrowser.untilTimeReachedInfo"));
        displayPaused = true;
        nextMessageButton.setEnabled(true);
        if (displayingAll)
            stopDisplayAll();
    }

    /**
    Called from the output thread when the DDS server times out.
    Display error message.
    */
    public void dcpMsgTimeout()
    {
        showError(labels.getString("MessageBrowser.timeoutErr"));
        displayPaused = true;
        nextMessageButton.setEnabled(true);
        if (displayingAll)
            stopDisplayAll();
    }



    static CmdLineArgs cmdLineArgs = new CmdLineArgs(true, "util.log");
    static StringToken searchcrit_arg= new StringToken(
        "f", "Search Crit File", "", TokenOptions.optSwitch, "");
    static IntegerToken port_arg = new IntegerToken(
        "p", "Port Number", "", TokenOptions.optSwitch, DEFAULT_PORT_NUM);
    static StringToken user_arg = new StringToken(
        "u", "User Name", "", TokenOptions.optSwitch, "");
    static
    {
        cmdLineArgs.addToken(searchcrit_arg);
        cmdLineArgs.addToken(port_arg);
        cmdLineArgs.addToken(user_arg);
    }

    /**
      Main method for stand-alone operation.
      Usage:
      <ul>
        <li>-h hostname (required)</li>
        <li>-p port (optional default=16003)</li>
        <li>-u username (required)</li>
        <li>-P password (optional)</li>
        <li>-f search criteria (optional default = get all messages)</li>
        <li>-l log file name (optional)</li>
      </ul>
    */
    public static void main(String args[])
        throws Exception
    {
        Logger.setLogger(new StderrLogger("MessageBrowser"));
        DecodesInterface.setGUI(true);

        // Parse command line args & get argument values:
        cmdLineArgs.parseArgs(args);

        getMyLabelDescriptions();

        Logger.instance().debug1("MessageBrowser Starting.");

        GuiApp.setAppName(LrgsApp.ShortID);
        GeneralProperties.init();
        MessageBrowser me = new MessageBrowser(
            cmdLineArgs.getHostName(), port_arg.getValue(), user_arg.getValue());

        DecodesInterface.maintainGoesPdt();

        GuiApp.setTopFrame(me);
        me.startup(100, 100);
    }

    public static void getMyLabelDescriptions()
    {
        DecodesSettings settings = DecodesSettings.instance();
        //Load the generic properties file - includes labels that are used
        //in multiple screens
        genericLabels = LoadResourceBundle.getLabelDescriptions(
                "decodes/resources/generic",
                settings.language);
        //Return the main label descriptions for Message Browser App
        labels = LoadResourceBundle.getLabelDescriptions(
                "decodes/resources/msgaccess",
                settings.language);
    }

    public static ResourceBundle getLabels() 
    {
        if (labels == null)
        {
            getMyLabelDescriptions();
        }
        return labels;
    }

    public static ResourceBundle getGenericLabels() 
    {
        if (genericLabels == null)
        {
            getMyLabelDescriptions();
        }
        return genericLabels;
    }

    // Inner class to handle outputs to the JTextArea on the screen.
    class MessageAreaOutputStream extends OutputStream
    {
        public synchronized void write(int b)
        {
            b &= 0xff;
            char c = (char)b;
            messageArea.append(String.valueOf(c));
        }

        public synchronized void write(byte[] data, int offset, int length)
        {
            messageArea.append( new String(data, offset, length) );
        }
    }


    /**
      Called when display all button is pressed.
    */
    public void displayAllButtonPress()
    {
        if (!displayingAll)
        {
            startDisplayAll();
        }
        else
        {
            stopDisplayAll();
        }
    }

    public void startDisplayAll()
    {
        if (client == null)
        {
            connectButtonPress();
            if (client == null) // unsuccessful connect?
            {
                return;
            }
        }

        client.enableMultiMessageMode(true);

        if (checkSearchCriteria())
        {
            SearchCriteria toSend = new SearchCriteria(searchcrit);
            String s = toSend.getLrgsUntil();
            if (s == null || s.trim().length() == 0)
            {
                toSend.setLrgsUntil("now");
            }
            if (!sendSearchCriteria(toSend))
            {
                return;
            }
        }

        msgOutputThread.prefix = prefixField.getText();
        msgOutputThread.suffix = suffixField.getText();
        msgOutputThread.timeout = 
            GuiApp.getIntProperty("MessageBrowser.Timeout", 30);
        msgOutputThread.doDecode = doDecode;
        msgOutputThread.showRaw = showRaw;
        msgOutputThread.beforeData = canDecode ? beforeDataField.getText() : null;
        msgOutputThread.afterData = canDecode ? afterDataField.getText() : null;
        msgOutputThread.showMetaData = 
            GuiApp.getBooleanProperty("MessageBrowser.showMsgMetaData", false);

        displayPaused = false;

        displayAllButton.setText(
                labels.getString("MessageBrowser.stopButton"));
        displayingAll = true;
        nextMessageButton.setEnabled(false);
    }

    public void stopDisplayAll()
    {
        displayAllButton.setText(
                labels.getString("MessageBrowser.displayAll"));
        displayingAll = false;
        nextMessageButton.setEnabled(true);
        client.enableMultiMessageMode(false);
    }
}
