package decodes.launcher;

import static org.slf4j.helpers.Util.getCallingClass;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

import ilex.util.AsciiUtil;
import ilex.util.AuthException;
import ilex.util.EnvExpander;
import ilex.util.LoadResourceBundle;
import ilex.util.PropertiesUtil;
import ilex.util.UserAuthFile;
import ilex.gui.LoginDialog;
import decodes.gui.PropertiesEditPanel;
import decodes.gui.TopFrame;
import decodes.util.DecodesSettings;

import org.opendcs.authentication.AuthSourceService;
import org.opendcs.spi.authentication.AuthSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DecodesPropsPanel extends JPanel
{
    private final Logger log = LoggerFactory.getLogger(getCallingClass());
    private final ResourceBundle labels;
    private final ResourceBundle genericLabels;
    String editDbTypes[] = { "XML", "SQL", "NWIS", "CWMS", "OPENTSDB", "HDB"};
    JComboBox<String> editDbTypeCombo = new JComboBox<>(editDbTypes);
    JTextField editDbLocationField = new JTextField(50);
    private PropertiesEditPanel propsEditPanel = null;
    private Properties origProps = new Properties();
    final Profile profile;

    JButton dbPasswordButton;

    String[] usedfields = new String[]{
        "sitenametypepreference",
        "edittimezone",
        "jdbcdriverclass",
        "sqlkeygenerator",
        "sqldateformat",
        "sqltimezone",
        "editdatabaselocation",
        "editdatabasetype",
        "databaselocation",
        "databasetype",
        "datatypestdpreference"
        };
    ArrayList<String> usedFieldsArray;
    ArrayList<JLabel> newLabels= new ArrayList<JLabel>();
    ArrayList<JTextField> newFields = new ArrayList<JTextField>();
    ArrayList<String> newKeys = new ArrayList<String>();

    private TopFrame parent;

    public DecodesPropsPanel(TopFrame parent, ResourceBundle labels, ResourceBundle genericLabels, Profile profile)
    {
        this.profile = profile;
        this.parent = parent;
        this.labels = labels;
        this.genericLabels = genericLabels;

        dbPasswordButton = new JButton(
            labels.getString("DecodesPropsPanel.dbPassword"));
        this.parent = parent;
        usedFieldsArray = new ArrayList<String>();
        for(int pos = 0;pos<usedfields.length;pos++)
        {
            usedFieldsArray.add(pos,usedfields[pos]);
        }
        try
        {
            jbInit();
        }
        catch(Exception ex)
        {
            log.error("Unable to initialize Properties Panel.", ex);
        }
    }

    private void jbInit() throws Exception
    {
        setLayout(new BorderLayout());

        // North Panel contains params for connecting to the database
        JPanel editDbPanel = new JPanel(new GridBagLayout());
        this.add(editDbPanel, BorderLayout.NORTH);
        editDbPanel.setBorder(new TitledBorder(
            labels.getString("DecodesPropsPanel.editableDatabase")));
        editDbTypeCombo.addActionListener(e -> editDbTypeComboSelected());
        editDbPanel.add(
            new JLabel(labels.getString("DecodesPropsPanel.type")),
                new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(4, 10, 4, 2), 0, 0));
        editDbPanel.add(editDbTypeCombo,
            new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(4, 0, 4, 0), 0, 0));
        editDbPanel.add(dbPasswordButton,
            new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(4, 20, 4, 20), 0, 0));
        editDbPanel.add(new JLabel(labels.getString("DecodesPropsPanel.location")),
            new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE,
                new Insets(4, 10, 4, 2), 0, 0));
        editDbPanel.add(editDbLocationField,
            new GridBagConstraints(1, 1, 2, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                new Insets(4, 0, 4, 20), 0, 0));

        dbPasswordButton.addActionListener(e -> dbPasswordButtonPressed());

        propsEditPanel = new PropertiesEditPanel(origProps, false);
        propsEditPanel.setBorder(new TitledBorder(
            labels.getString("DecodesPropsPanel.preferences")));
        this.add(propsEditPanel, BorderLayout.CENTER);

        editDbTypeCombo.addActionListener(e -> editDbTypeChanged());
    }

    protected void editDbTypeChanged()
    {
        String dbType = (String)editDbTypeCombo.getSelectedItem();
        if (dbType.equalsIgnoreCase("CWMS"))
        {
            propsEditPanel.setProperty("dbClassName", "decodes.cwms.CwmsTimeSeriesDb");
            propsEditPanel.setProperty("jdbcDriverClass", "oracle.jdbc.driver.OracleDriver");
            propsEditPanel.setProperty("sqlKeyGenerator", "decodes.sql.OracleSequenceKeyGenerator");
        }
        else if (dbType.equalsIgnoreCase("HDB"))
        {
            propsEditPanel.setProperty("dbClassName", "decodes.hdb.HdbTimeSeriesDb");
            propsEditPanel.setProperty("jdbcDriverClass", "oracle.jdbc.driver.OracleDriver");
            propsEditPanel.setProperty("sqlKeyGenerator", "decodes.sql.OracleSequenceKeyGenerator");
        }
    }

    public boolean isValidated()
    {
        return true;
    }

    void editDbTypeComboSelected()
    {
        String x = (String)editDbTypeCombo.getSelectedItem();
        dbPasswordButton.setEnabled(!x.equalsIgnoreCase("XML"));
    }

    /**
     * Fill in this GUI panel with values from the passed settings.
     * @param settings
     */
    public void loadFromSettings(DecodesSettings settings)
    {
        origProps.clear();
        settings.saveToProps(origProps);

        // Since we show database type & location at the North panel, remove
        // them from the properties list.
        PropertiesUtil.rmIgnoreCase(origProps, "editDatabaseType");
        PropertiesUtil.rmIgnoreCase(origProps, "editDatabaseLocation");
        propsEditPanel.setProperties(origProps);
        propsEditPanel.setPropertiesOwner(settings);

        int typ = settings.editDatabaseTypeCode;
        editDbTypeCombo.setSelectedIndex(
            typ == DecodesSettings.DB_XML ? 0 :
            typ == DecodesSettings.DB_SQL ? 1 :
            typ == DecodesSettings.DB_NWIS ? 2 :
            typ == DecodesSettings.DB_CWMS ? 3 :
            typ == DecodesSettings.DB_OPENTSDB ? 4 :
            typ == DecodesSettings.DB_HDB ? 5 : 0);
        editDbLocationField.setText(settings.editDatabaseLocation);
    }

    DecodesSettings saveToSettings()
    {
        DecodesSettings settings = new DecodesSettings();
        propsEditPanel.saveChanges(); // this saves back to 'origProps'
        settings.loadFromProperties(origProps);

        int idx = editDbTypeCombo.getSelectedIndex();
        settings.editDatabaseTypeCode =
            idx == 0 ? DecodesSettings.DB_XML :
            idx == 1 ? DecodesSettings.DB_SQL :
            idx == 2 ? DecodesSettings.DB_NWIS :
            idx == 3 ? DecodesSettings.DB_CWMS :
            idx == 4 ? DecodesSettings.DB_OPENTSDB :
            idx == 5 ? DecodesSettings.DB_HDB : DecodesSettings.DB_NONE;
        settings.editDatabaseLocation = editDbLocationField.getText();
        return settings;
    }

    private void dbPasswordButtonPressed()
    {
        try
        {
            DecodesSettings settingsPanel = new DecodesSettings();
            DecodesSettings settingsProfile = DecodesSettings.fromProfile(profile);
            settingsPanel.loadFromProperties(origProps); // instead of the profile use the current pro
            if (!settingsPanel.DbAuthFile.equalsIgnoreCase(settingsProfile.DbAuthFile))
            {
                throw new InvalidStateException("nomatch");
            }
            else if (settingsProfile.DbAuthFile == null)
            {
                throw new InvalidStateException("noauth");
            }
            try
            {
                AuthSource as = AuthSourceService.getFromString(settingsProfile.DbAuthFile);
                if (!as.canWrite())
                {
                    throw new InvalidStateException("nowrite");
                }
            }
            catch (AuthException ex)
            {
                if (!ex.getLocalizedMessage().contains("Unable to read"))
                {
                    throw ex; // most likely the file doesn't exist yet.
                }
            }
            LoginDialog dlg = new LoginDialog(parent,
                labels.getString("DecodesPropsPanel.loginUserInfoTitle"));
            parent.launchDialog(dlg);
            if (dlg.isOK())
            {

                final String afn = EnvExpander.expand(settingsProfile.DbAuthFile);
                final UserAuthFile uaf = new UserAuthFile(afn);
                try
                {
                    log.info("Writing encrypted daemon password to '{}'", afn);
                    uaf.write(dlg.getUserName(), new String(dlg.getPassword()));
                }
                catch(Exception ex)
                {
                    parent.showError(
                        LoadResourceBundle.sprintf(
                        labels.getString("DecodesPropsPanel.cannotSavePassErr"),
                        afn) + ex);
                }
            }
        }
        catch (AuthException ex)
        {
            String msg = "There was an error processing the authentication settings." + ex.getLocalizedMessage();
            log.error("Unable to process DbAuthFile property", ex);
            JOptionPane.showMessageDialog(this, AsciiUtil.wrapString(msg, 60), "Error!", JOptionPane.ERROR_MESSAGE);
        }
        catch (IOException ex)
        {
            String msg = "The current settings have not been saved to disk. Please save the properties to disk first and then set the password.";
            JOptionPane.showMessageDialog(this, AsciiUtil.wrapString(msg, 60), "Error!", JOptionPane.ERROR_MESSAGE);
        }
        catch (InvalidStateException ex)
        {
            final String exMsg = ex.getLocalizedMessage();
            String msg;
            if (exMsg.equals("nomatch"))
            {
                msg = "The active settings and on-disk properties do not match. Please save the properties to disk first and then set the password.";
            }
            else if(exMsg.equalsIgnoreCase("noauth"))
            {
                msg = "This profile does not require authentication.";
            }
            else if (ex.getLocalizedMessage().equals("nowrite"))
            {
                msg = "The configured auth settings do not support editing.";
            }
            else
            {
                msg = "Error processing information. See log";
                log.error("Unable to process auth file conditions", ex);
            }
            JOptionPane.showMessageDialog(this, AsciiUtil.wrapString(msg, 60), "Error!", JOptionPane.ERROR_MESSAGE);
        }
    }
}