/*
 * $Id$
 * 
 * $Log$
 * Revision 1.3  2016/06/07 21:54:00  mmaloney
 * Added HDB selection to Settings GUI.
 *
 * Revision 1.2  2014/05/27 12:56:36  mmaloney
 * cleanup
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.6  2013/03/28 18:43:10  mmaloney
 * border around scroll pane, not in it.
 *
 * Revision 1.5  2013/03/28 18:38:02  mmaloney
 * Alphabetize by key name.
 *
 * Revision 1.4  2013/03/28 17:29:09  mmaloney
 * Refactoring for user-customizable decodes properties.
 *
 * Revision 1.3  2013/02/10 19:21:34  mmaloney
 * Don't display unsupported apps.
 *
 * Revision 1.2  2012/05/30 20:49:31  mmaloney
 * null pointer bug.
 *
 * Revision 1.1  2012/05/21 21:20:56  mmaloney
 * Generic DECODES Properties Frame
 *
 * Revision 1.20  2011/01/05 13:24:55  mmaloney
 * dev
 *
 * Revision 1.19  2010/06/15 20:15:34  gchen
 * *** empty log message ***
 *
 * Revision 1.18  2010/06/15 19:50:44  gchen
 * 
 * This is open-source software written by Sutron Corporation under
 * contract to the federal government. Anyone is free to copy and use this
 * source code for any purpos, except that no part of the information
 * contained in this file may be claimed to be proprietary.
 *
 * Except for specific contractual terms between Sutron and the federal 
 * government, this source code is provided completely without warranty.
*/

package decodes.launcher;

import java.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

import java.awt.event.*;

import ilex.util.EnvExpander;
import ilex.util.LoadResourceBundle;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.UserAuthFile;
import ilex.gui.LoginDialog;
import decodes.gui.PropertiesEditPanel;
import decodes.gui.TopFrame;
import decodes.util.DecodesSettings;

public class DecodesPropsPanel extends JPanel
{
	private static ResourceBundle labels;
	private static ResourceBundle genericLabels;
	String editDbTypes[] = { "XML", "SQL", "NWIS", "CWMS", "OPENTSDB", "HDB"};
	JComboBox editDbTypeCombo = new JComboBox(editDbTypes);
	JTextField editDbLocationField = new JTextField(50);
	private PropertiesEditPanel propsEditPanel = null;
	private Properties origProps = new Properties();

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
	
	public DecodesPropsPanel(TopFrame parent, ResourceBundle labels, ResourceBundle genericLabels)
	{
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
		try {
			jbInit();
		}
		catch(Exception e) {
			e.printStackTrace();
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
		editDbTypeCombo.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				editDbTypeComboSelected();
			}
		});
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

		dbPasswordButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				dbPasswordButtonPressed();
			}
		});

		propsEditPanel = new PropertiesEditPanel(origProps, false);
		propsEditPanel.setBorder(new TitledBorder(
			labels.getString("DecodesPropsPanel.preferences")));
		this.add(propsEditPanel, BorderLayout.CENTER);
		
		editDbTypeCombo.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					editDbTypeChanged();
				}
			});
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

	public void saveToSettings(DecodesSettings settings)
	{
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
	}

	private void dbPasswordButtonPressed()
	{
		LoginDialog dlg = new LoginDialog(parent, 
				labels.getString("DecodesPropsPanel.loginUserInfoTitle"));
		parent.launchDialog(dlg);
		if (dlg.isOK())
		{
			String afn = propsEditPanel.getProperty("DbAuthFile");
			if (afn == null)
				afn = "$HOME/.decodes.auth";
			afn = EnvExpander.expand(afn);
			UserAuthFile uaf = new UserAuthFile(afn);
			try
			{
				Logger.instance().info("Writing encrypted daemon password to '" + afn + "'");
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
}