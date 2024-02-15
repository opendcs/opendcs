package decodes.launcher;

import java.awt.*;

import javax.swing.*;

import java.awt.event.*;
import java.util.Objects;
import java.util.ResourceBundle;
import java.io.*;

import ilex.gui.WindowUtility;
import ilex.util.LoadResourceBundle;
import ilex.util.Logger;
import decodes.gui.TopFrame;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesSettings;
import decodes.util.ResourceFactory;

@SuppressWarnings("serial")
public class DecodesSetupFrame
    extends TopFrame
{
    private static ResourceBundle labels = getLabels();
    private static ResourceBundle genericLabels = getGenericLabels();
    private JPanel jPanel3 = new JPanel();
    private FlowLayout flowLayout2 = new FlowLayout();
    private JPanel southButtonPanel = new JPanel();
    private JButton saveDecodesPropsButton = new JButton();
    private JButton abandonDecodesPropsButton = new JButton();
    private DecodesPropsPanel decodesPropsPanel;
    private LauncherFrame launcherFrame = null;
    private Profile profile = null;

    private static DecodesSetupFrame _lastInstance = null;
    public static DecodesSetupFrame lastInstance() { return _lastInstance; }

    public DecodesSetupFrame(LauncherFrame launcherFrame, Profile profile)
    {
        Objects.requireNonNull(profile, "A valid profile must be provided.");
        this.profile = profile;
        this.launcherFrame = launcherFrame;
        exitOnClose = false;
        try
        {
            jbInit();
            setTitle(labels.getString("DecodesPropsPanel.title"));
            // ? this.setSize(new Dimension(770, 600));//650, 600
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            addWindowListener(new WindowAdapter()
            {
                public void windowClosed(WindowEvent e)
                {
                    if (getExitOnClose())
                        System.exit(0);
                }
            });
            pack();
            populateDecodesPropsTab();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        trackChanges("DecodesSetupFrame");
        _lastInstance = this;
    }

    public void cleanupBeforeExit()
    {
    }

    public static void main(String[] args)
    {
        CmdLineArgs cmdLineArgs = new CmdLineArgs();
        cmdLineArgs.parseArgs(args);
        labels = getLabels();
        genericLabels = getGenericLabels();
        DecodesSetupFrame setupFrame = new DecodesSetupFrame(null, cmdLineArgs.getProfile());
        setupFrame.setExitOnClose(true);
        WindowUtility.center(setupFrame).setVisible(true);
    }

    public static ResourceBundle getLabels()
    {
        if (labels == null)
        {
            DecodesSettings settings = DecodesSettings.instance();
            // Return the main label descriptions for ToolKit Setup App
            labels = LoadResourceBundle.getLabelDescriptions(
                    "decodes/resources/launcherframe", settings.language);
            if (labels == null)
                System.err.println("Cannot get labels!!!");
        }
        return labels;
    }

    public static ResourceBundle getGenericLabels()
    {
        if (genericLabels == null)
        {
            DecodesSettings settings = DecodesSettings.instance();
            // Load the generic properties file - includes labels that are used
            // in multiple screens
            genericLabels = LoadResourceBundle.getLabelDescriptions(
                    "decodes/resources/generic", settings.language);
            if (genericLabels == null)
                System.err.println("Cannot get genericLabels!!!");
        }
        return genericLabels;
    }

    private void jbInit() throws Exception
    {
        this.getContentPane().setLayout(new BorderLayout());

        jPanel3.setLayout(flowLayout2);
        flowLayout2.setHgap(40);

        saveDecodesPropsButton.setText(
            labels.getString("DecodesPropsPanel.saveChanges"));
        saveDecodesPropsButton.addActionListener(
            new java.awt.event.ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    saveChangesPressed();
                }
            });
        abandonDecodesPropsButton.setText(
            labels.getString("DecodesPropsPanel.abandonChanges"));
        abandonDecodesPropsButton.addActionListener(
            new java.awt.event.ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    abandonChangesPressed();
                }
            });
        southButtonPanel.add(saveDecodesPropsButton, null);
        southButtonPanel.add(abandonDecodesPropsButton, null);
        for(JButton jb : ResourceFactory.instance().additionalSetupButtons())
            southButtonPanel.add(jb, null);
        this.getContentPane().add(southButtonPanel, BorderLayout.SOUTH);

        decodesPropsPanel = new DecodesPropsPanel(this, labels, genericLabels);
        this.getContentPane().add(decodesPropsPanel, BorderLayout.CENTER);
    }

    private void saveChangesPressed()
    {
        DecodesSettings settings = decodesPropsPanel.saveToSettings();

        try
        {
            settings.saveToProfile(profile);
        }
        catch (IOException ex)
        {
            Logger.instance().failure(
                "Cannot save DECODES Properties File '" + profile.getFile().getAbsolutePath() + "': "
                + ex);
        }
        if (launcherFrame != null)
        {
            launcherFrame.setupSaved();
        }
    }

    private void abandonChangesPressed()
    {
        populateDecodesPropsTab();
    }

    void populateDecodesPropsTab()
    {
        try
        {
            DecodesSettings settings = DecodesSettings.fromProfile(this.profile);
            decodesPropsPanel.loadFromSettings(settings);
        }
        catch (IOException ex)
        {
            Logger.instance().failure(
                "Cannot open DECODES Properties File '" + profile.getFile().getAbsolutePath() + "': " + ex);
        }
    }
}
