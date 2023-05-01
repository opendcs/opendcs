/*
*  $Id$
*/
package decodes.dbeditor;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.ResourceBundle;

import ilex.util.Logger;
import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.Site;
import decodes.db.SiteFactory;
import decodes.db.SiteName;
import decodes.gui.EnumComboBox;
import decodes.gui.GuiDialog;
import decodes.util.DecodesSettings;


/**
Dialog for entering a new site name.
*/
public class SiteNameEntryDialog extends GuiDialog
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

    private JPanel mainPanel = new JPanel();
    private BorderLayout mainBorderLayout = new BorderLayout();
    private JPanel southPanel = new JPanel();
    private FlowLayout southFlowLayout = new FlowLayout();
    private JButton okButton = new JButton();
    private JButton cancelButton = new JButton();
    private JPanel centerPanel = new JPanel();
    private GridBagLayout centerGridBagLayout = new GridBagLayout();
    private JLabel nameTypeLabel = new JLabel();
	private EnumComboBox nameTypeCombo = new EnumComboBox("SiteNameType",
		DecodesSettings.instance().siteNameTypePreference);
    private JLabel nameValueLabel = new JLabel();
    private JTextField nameValueField = new JTextField();
    private JLabel usgsDbnoLabel = new JLabel();
    private JComboBox usgsDbnoCombo;
	private JLabel agencyCodeLabel = new JLabel();
    private JComboBox agencyCodeCombo;

	/** Used to validate name &amp; make a Site object after user enters name. */
	public static SiteFactory siteFactory = new SiteFactory();

	/** After OK pressed, this is the newly-created site. */
	private Site theSite;

	/** The site name if one is pre-selected. */
	private SiteName theSiteName;

	/** List of valid USGS DBNOs in the connected database. */
	private static String dbnos[] = { "01", "02", "03", "04", "05" };

	/** List of valid agencies in the connected database. */
	private static String agencies[] = { "USGS", "USBR", "USACE", "NIFC", 
		"NOAA", "NOS", "NDBC", "TVA", "NWS", "NFS", "NPS" };

	boolean okPressed = false;

	/** No args constructor. */
    public SiteNameEntryDialog()
	{
        super(getDbEditFrame(), 
			dbeditLabels.getString("SiteNameEntryDialog.title"), true);
    	usgsDbnoCombo = new JComboBox(dbnos);
		agencyCodeCombo = new JComboBox(agencies);
		theSite = null;
		theSiteName = null;
        try {
            jbInit();
			getRootPane().setDefaultButton(okButton);
            pack();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
		enableControls();
    }

	/** Initialize the GUI components. */
    void jbInit() 
		throws Exception 
	{
        mainPanel.setLayout(mainBorderLayout);
    	mainPanel.setPreferredSize(new Dimension(300, 220));
        southPanel.setLayout(southFlowLayout);
        okButton.setPreferredSize(new Dimension(100, 27));
        okButton.setText(
			genericLabels.getString("OK"));
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                okButton_actionPerformed(e);
            }
        });
        cancelButton.setPreferredSize(new Dimension(100, 27));
        cancelButton.setText(
			genericLabels.getString("cancel"));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancelButton_actionPerformed(e);
            }
        });
        southFlowLayout.setHgap(35);
        southFlowLayout.setVgap(10);
        centerPanel.setLayout(centerGridBagLayout);
        nameTypeLabel.setText(
			dbeditLabels.getString("SiteNameEntryDialog.siteNameType"));
        nameValueLabel.setText(
			genericLabels.getString("identifier") + ":");
        usgsDbnoLabel.setText("USGS DBNO:");
		agencyCodeLabel.setText(
			dbeditLabels.getString("SiteNameEntryDialog.agencyCode"));
        getContentPane().add(mainPanel);
        mainPanel.add(southPanel, BorderLayout.SOUTH);
        southPanel.add(okButton, null);
        southPanel.add(cancelButton, null);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        centerPanel.add(nameTypeLabel, 
			new GridBagConstraints(0, 0, 1, 1, 0.3, 0.5,
            	GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, 
				new Insets(10, 10, 5, 3), 0, 0));
        centerPanel.add(nameTypeCombo, 
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.5,
            	GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, 
				new Insets(10, 1, 5, 20), 0, 0));
        centerPanel.add(nameValueLabel, 
			new GridBagConstraints(0, 1, 1, 1, 0.3, 0.0,
            	GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(5, 10, 5, 3), 0, 0));
        centerPanel.add(nameValueField, 
			new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
            	GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(5, 1, 5, 20), 0, 0));
        centerPanel.add(usgsDbnoLabel, 
			new GridBagConstraints(0, 2, 1, 1, 0.3, 0.0,
            	GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(5, 10, 5, 3), 0, 0));
        centerPanel.add(usgsDbnoCombo, 
			new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
            	GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(5, 1, 5, 30), 0, 0));
        centerPanel.add(agencyCodeLabel, 
			new GridBagConstraints(0, 3, 1, 1, 0.3, 0.5,
            	GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, 
				new Insets(5, 10, 5, 3), 0, 0));
        centerPanel.add(agencyCodeCombo, 
			new GridBagConstraints(1, 3, 1, 1, 1.0, 0.5,
            	GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, 
				new Insets(5, 1, 5, 30), 0, 0));

        nameTypeCombo.addActionListener(
			new java.awt.event.ActionListener() 
			{
            	public void actionPerformed(ActionEvent e) 
				{
                	nameTypeCombo_actionPerformed();
            	}
        	});
		nameTypeCombo.setEditable(false);
    	usgsDbnoCombo.setEditable(false);
    }

	/** 
	  Called when OK button is pressed. 
	  @param e ignored
	*/
    void okButton_actionPerformed(ActionEvent e)
	{
		okPressed = true;
		String nameType = nameTypeCombo.getSelection();
		String nameValue = nameValueField.getText().trim();
		if (nameValue.length() == 0)
		{
			showError(
				dbeditLabels.getString("SiteNameEntryDialog.emptyNameErr"));
			theSite = null;
			return;
		}
		// 2012/07/05 CWMS Location Names can contain spaces
		else if (!nameType.equalsIgnoreCase(Constants.snt_CWMS) && nameValue.indexOf(' ') > 0)
		{
			Logger.instance().debug3("SiteNameType='" + nameType + "', value='" + nameValue + "'");
			int r = JOptionPane.showConfirmDialog(this,
				dbeditLabels.getString("SiteNameEntryDialog.nameFormatErr"),
				dbeditLabels.getString("SiteNameEntryDialog.nameFormatErrTitle"),
				JOptionPane.YES_NO_OPTION);
			if (r == JOptionPane.NO_OPTION)
			{
				theSite = null;
				return;
			}
			StringBuilder sb = new StringBuilder(nameValue);
			for(int i=0; i<sb.length(); i++)
				if (sb.charAt(i) == ' ')
					sb.setCharAt(i, '_');
			nameValue = sb.toString();
			nameValueField.setText(nameValue);
		}
			
		String dbno = null;
		String agencyCode = null;
		if (nameType.equalsIgnoreCase("USGS"))
		{
			dbno = (String)usgsDbnoCombo.getSelectedItem();
			if (dbno.length() == 1)
				dbno = "0" + dbno;
			agencyCode = (String)agencyCodeCombo.getSelectedItem();
			if (agencyCode.length() == 0)
				agencyCode = "USGS";
		}

		try
		{
			SiteName sn = new SiteName(theSite, nameType, nameValue);
			sn.setUsgsDbno(dbno);
			sn.setAgencyCode(agencyCode);

			if (theSite == null)
				theSite = siteFactory.makeNewSite(sn, Database.getDb());
			else // either adding or modifying name in exiisting site.
			{
				siteFactory.validateName(sn, Database.getDb());
				if (theSiteName == null)
					theSite.addName(sn);
				else
				{
					theSiteName.setNameValue(nameValue);
					theSiteName.setUsgsDbno(dbno);
					theSiteName.setAgencyCode(agencyCode);
				}
			}
			closeDlg();
		}
		catch(Exception ex)
		{
			showError(ex.getMessage());
			theSite = null;
		}
    }

	/** Closes the dialog. */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/** 
	  Called when Cancel button is pressed. 
	  @param e ignored
	*/
    void cancelButton_actionPerformed(ActionEvent e)
	{
		okPressed = false;
		theSite = null;
		closeDlg();
    }

	/** 
	 * Sets the site to which a name will be added (null to create new site.)
	 * @param site the site.
	 */
	public void setSite(Site site)
	{
		theSite = site;

		// Now assume we're adding new name. Set combo to unused type.
		for(int i=0; i<nameTypeCombo.getItemCount(); i++)
		{
			String nt = (String)nameTypeCombo.getItemAt(i);
			if (theSite.getName(nt) == null)
			{
				nameTypeCombo.setSelectedIndex(i);
				nameTypeCombo_actionPerformed();
				break;
			}
		}
	}

	/** @return the Newly created site, or Null if cancel pressed. */
	public Site getSite()
	{
		return theSite;
	}

	/**
	 * Sets initial selection for site name.
	 * @param siteName the site name, which must be already associated with 
	 * the previously set site.
	 */
	public void setSiteName(SiteName siteName)
	{
		String nt = siteName.getNameType();
		String nv = siteName.getNameValue();
		nameTypeCombo.setSelection(nt);
		nameTypeCombo.setEnabled(false);
		if (nt.equalsIgnoreCase("USGS"))
		{
			String dbno = siteName.getUsgsDbno();
			if (dbno != null)
				usgsDbnoCombo.setSelectedItem(dbno);
			else
				usgsDbnoCombo.setSelectedIndex(0);

			String agency = siteName.getAgencyCode();
			if (agency != null)
				agencyCodeCombo.setSelectedItem(agency);
			else
				agencyCodeCombo.setSelectedIndex(0);

			usgsDbnoCombo.setEnabled(true);
			usgsDbnoLabel.setEnabled(true);
			agencyCodeCombo.setEnabled(true);
			agencyCodeLabel.setEnabled(true);
		}
		else
		{
			usgsDbnoCombo.setEnabled(false);
			usgsDbnoLabel.setEnabled(false);
			agencyCodeCombo.setEnabled(false);
			agencyCodeLabel.setEnabled(false);
		}
    	nameValueField.setText(nv);
		theSiteName = siteName;
	}

	private void nameTypeCombo_actionPerformed()
	{
		enableControls();
	}

	private void enableControls()
	{
		String nameType = nameTypeCombo.getSelection();
		boolean isUsgs = nameType.equalsIgnoreCase("USGS");
		usgsDbnoCombo.setEnabled(isUsgs);
		usgsDbnoLabel.setEnabled(isUsgs);
		agencyCodeCombo.setEnabled(isUsgs);
		agencyCodeLabel.setEnabled(isUsgs);
	}

	/**
	 * Sets the list of known DBNOs to display in the site name dialog.
	 * @param _dbnos the list of dbno's
	 */
	public static void setDbnos(String _dbnos[])
	{
		dbnos = _dbnos;
	}

	/**
	 * Sets the list of known agencies to display in the site name dialog.
	 * @param _agencies the list of agencies
	 */
	public static void setAgencies(String _agencies[])
	{
		agencies = _agencies;
	}
}
