/**
 * $I$
 * 
 * $Log$
 * Revision 1.1  2018/05/23 19:59:02  mmaloney
 * OpenTSDB Initial Release
 *
 */
package decodes.tsdb.groupedit;

import java.awt.*;

import javax.swing.*;

import java.awt.event.*;

import javax.swing.border.*;

import opendcs.dai.DataTypeDAI;
import opendcs.dai.IntervalDAI;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;

import java.util.Calendar;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.TimeZone;

import ilex.util.AsciiUtil;
import ilex.util.LoadResourceBundle;
import ilex.util.Logger;
import ilex.util.StringPair;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.EngineeringUnit;
import decodes.db.SiteName;
import decodes.db.Site;
import decodes.dbeditor.SiteSelectDialog;
import decodes.dbeditor.SiteSelectPanel;
import decodes.gui.*;
import decodes.hdb.HdbTimeSeriesDb;
import decodes.hdb.HdbTsId;
import decodes.sql.DbKey;
import decodes.tsdb.ConstraintException;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.IntervalIncrement;
import decodes.tsdb.MissingAction;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;
import decodes.tsdb.TsdbDatabaseVersion;
import decodes.tsdb.algo.RoleTypes;
import decodes.tsdb.compedit.CAPEdit;
import decodes.tsdb.groupedit.HdbDatatypeSelectDialog;
import decodes.tsdb.groupedit.LocSelectDialog;
import decodes.tsdb.groupedit.ParamSelectDialog;
import decodes.tsdb.groupedit.SelectionMode;
import decodes.tsdb.groupedit.VersionSelectDialog;
import decodes.util.DecodesSettings;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.CwmsTsId;

/**
 * For DCS Toolkit 5.0 we now treat input and output parameters the same way. So
 * we have once-again reintroduced this class.
 */
@SuppressWarnings("serial")
public class TsListNewDialog extends GuiDialog
{
	private JButton okButton = new JButton();
	private JTextField parmTypeField = new JTextField();
	private JTextField siteField = new JTextField();
	private JButton siteSelectButton = new JButton(
		ceResources.getString("CompParmDialog.SelectButton"));
	private JTextField dataTypeField = new JTextField();
	private JButton selectTsButton = new JButton("Time Series Lookup");
	private JComboBox intervalCombo = new JComboBox();
	private JComboBox tabselCombo = new JComboBox(new String[] {"R_", "M_"});
	private JTextField deltaTField = new JTextField();
	private JComboBox deltaTUnitsCombo = new JComboBox(
		new String []{"Seconds", "Minutes", "Hours", "Days", "Weeks", 
			"Months", "Years" });
	private JTextField unitsField = new JTextField();
	private JButton unitsButton = new JButton(
		ceResources.getString("CompParmDialog.SelectButton"));
	private JComboBox ifMissingCombo = 
		new JComboBox(MissingAction.values());

	// Model ID Used by HDB:
	private JTextField modelIdField = new JTextField();

	// MJM: Extra components needed for USACE CWMS:
	private JComboBox durationCombo = new JComboBox();
	private JComboBox paramTypeCombo = new JComboBox();
	private JTextField versionField = new JTextField();

	/** The object being edited */
	boolean okPressed = false;
	private SiteSelectDialog siteSelectDialog = null;
	private TimeSeriesDb theDb;
	private TsListPanel tsListPanel = null;
	
	private static ResourceBundle dlgResources =  LoadResourceBundle.getLabelDescriptions(
		"decodes/resources/groupedit", DecodesSettings.instance().language);
	private static ResourceBundle genResources =  LoadResourceBundle.getLabelDescriptions(
		"decodes/resources/generic", DecodesSettings.instance().language);
	private static ResourceBundle ceResources =  LoadResourceBundle.getLabelDescriptions(
		"decodes/resources/compedit", DecodesSettings.instance().language);

	private TimeSeriesIdentifier tsidCreated = null;

	public TsListNewDialog(TopFrame theFrame, TsListPanel tsListPanel, TimeSeriesDb theDb)
	{
		super(theFrame, dlgResources.getString("NewTsDialog.DlgTitle"), true);
		this.tsListPanel = tsListPanel;
		this.theDb = theDb;
		
		siteSelectDialog = new SiteSelectDialog(this);

		try
		{
			buildPanel();
			getRootPane().setDefaultButton(okButton);
			pack();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public void fillValues(TimeSeriesIdentifier tsid)
	{
		SiteName sn = tsid.getSite().getPreferredName();
		siteField.setText(sn != null ? sn.getNameValue() : "");
		if (siteSelectDialog != null)
			siteSelectDialog.getSiteSelectPanel().setSelection(sn);
		
		siteField.setEnabled(true);
		siteSelectButton.setEnabled(true);

		DataType dt = tsid.getDataType();
		dataTypeField.setText(dt != null ? dt.getCode() : "");

		String intvCode = tsid.getInterval();
		if (intvCode == null || intvCode.length() == 0)
			intervalCombo.setSelectedIndex(0);
		else
			intervalCombo.setSelectedItem(intvCode);
		
		if (theDb.isHdb())
		{
			tabselCombo.setSelectedItem(tsid.getTableSelector());
			HdbTsId htsid = (HdbTsId)tsid;
			modelIdField.setText("" + htsid.getModelId());
		}
		else // CWMS or OpenTSDB
		{
			CwmsTsId ctsid = (CwmsTsId)tsid;
			paramTypeCombo.setSelectedItem(ctsid.getParamType());
			durationCombo.setSelectedItem(ctsid.getDuration());
			versionField.setText(ctsid.getVersion());
		}
	}

	/** Initializes GUI components. */
	void buildPanel()
		throws Exception
	{
		JPanel northPanel = new JPanel(new FlowLayout());
		JPanel southButtonPanel = new JPanel();
		JPanel fieldEntryPanel = new JPanel(new GridBagLayout());
		JPanel outerPanel = new JPanel(new BorderLayout());
		outerPanel.add(northPanel, BorderLayout.NORTH);
		outerPanel.add(fieldEntryPanel, BorderLayout.CENTER);
		outerPanel.add(southButtonPanel, BorderLayout.SOUTH);

		// NORTH Panel 1 line string instruction
		northPanel.add(new JLabel(dlgResources.getString("NewTsDialog.top")));

		// South panel contains OK and Cancel buttons
		okButton.setText("   " + genResources.getString("OK") + "   ");
		okButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				okButtonPressed();
			}
		});
		JButton cancelButton = new JButton(genResources.getString("cancel"));
		cancelButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				cancelButtonPressed();
			}
		});
		FlowLayout southLayout = new FlowLayout();
		southButtonPanel.setLayout(southLayout);
		southLayout.setHgap(35);
		southLayout.setVgap(10);
		southButtonPanel.add(okButton, null);
		southButtonPanel.add(cancelButton, null);
		
		// Site or Location
		String label = ceResources.getString("CompParmDialog.Site");
		if (theDb.isCwms() || theDb.isOpenTSDB())
			label = "Location:";
		fieldEntryPanel.add(new JLabel(label),
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 15, 4, 2), 0, 0));
		fieldEntryPanel.add(siteField,
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(4, 0, 4, 10), 0, 0));
		fieldEntryPanel.add(siteSelectButton,
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(4, 5, 4, 15), 0, 0));
		
		// Data Type or Param
		label = ceResources.getString("CompParmDialog.DataType");
		if (theDb.isCwms() || theDb.isOpenTSDB())
			label = "Param:";
		fieldEntryPanel.add(new JLabel(label),
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 15, 4, 2), 0, 0));
		fieldEntryPanel.add(dataTypeField, 
			new GridBagConstraints(1, 1, 1, 1, 1.0, 1.0, 
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(4, 0, 4, 10), 0, 0));
		JButton paramSelectButton = new JButton("Select");
		paramSelectButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					paramSelectButtonPressed();
				}
			});
		fieldEntryPanel.add(paramSelectButton,
			new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(4, 5, 4, 15), 0, 0));
		

		// For CWMS, Param Type
		int Y = 2;
		if (theDb.isCwms() || theDb.isOpenTSDB())
		{
			fieldEntryPanel.add(new JLabel(
				theDb.isCwms() ? "Param Type:" : "Statistics Code:"),
				new GridBagConstraints(0, Y, 1, 1, 0.0, 0.0, 
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(4, 15, 4, 2), 0, 0));
			paramTypeCombo.setEditable(true);
			for(String pt : theDb.listParamTypes())
				paramTypeCombo.addItem(pt);
			fieldEntryPanel.add(paramTypeCombo, 
				new GridBagConstraints(1, Y, 1, 1, 1.0, 1.0, 
					GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
					new Insets(4, 0, 4, 10), 0, 0));
			Y++;
		}
		
		// Interval
		label = ceResources.getString("CompParmDialog.Interval");
		fieldEntryPanel.add(new JLabel(label),
			new GridBagConstraints(0, Y, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 15, 4, 2), 0, 0));
		fieldEntryPanel.add(intervalCombo, 
			new GridBagConstraints(1, Y, 1, 1, 1.0, 1.0, 
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(4, 0, 4, 10), 0, 0));
		Y++;

		// For CWMS, Param Type, Duration & Version
		if (theDb.isCwms() || theDb.isOpenTSDB())
		{
			fieldEntryPanel.add(new JLabel("Duration:"),
				new GridBagConstraints(0, Y, 1, 1, 0.0, 0.0, 
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(4, 15, 4, 2), 0, 0));
			durationCombo.setEditable(true);
			fieldEntryPanel.add(durationCombo, 
				new GridBagConstraints(1, Y, 1, 1, 1.0, 1.0, 
					GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
					new Insets(4, 0, 4, 10), 0, 0));
			Y++;
			
			fieldEntryPanel.add(new JLabel("Version:"),
				new GridBagConstraints(0, Y, 1, 1, 0.0, 0.0, 
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(4, 15, 4, 2), 0, 0));
			fieldEntryPanel.add(versionField, 
				new GridBagConstraints(1, Y, 1, 1, 1.0, 1.0, 
					GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
					new Insets(4, 0, 4, 10), 0, 0));
			JButton versionSelectButton = new JButton("Select");
			versionSelectButton.addActionListener(
				new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent e)
					{
						versionSelectButtonPressed();
					}
				});
			fieldEntryPanel.add(versionSelectButton,
				new GridBagConstraints(2, Y, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
					new Insets(4, 5, 4, 15), 0, 0));
			Y++;
			
			if (theDb.isOpenTSDB())
			{
				fieldEntryPanel.add(new JLabel("Storage Units:"),
					new GridBagConstraints(0, Y, 1, 1, 0.0, 0.0, 
						GridBagConstraints.EAST, GridBagConstraints.NONE,
						new Insets(4, 15, 4, 2), 0, 0));
				fieldEntryPanel.add(unitsField, 
					new GridBagConstraints(1, Y, 1, 1, 1.0, 1.0, 
						GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
						new Insets(4, 0, 4, 10), 0, 0));
				fieldEntryPanel.add(unitsButton,
					new GridBagConstraints(2, Y, 1, 1, 0.0, 0.0, 
						GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
						new Insets(4, 5, 4, 15), 0, 0));
				unitsButton.addActionListener(
					new java.awt.event.ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							unitsButtonPressed();
						}
					});
			}
		}
		else // HDB Table Selector
		{
        	label = theDb.getTableSelectorLabel() + ":";
	        fieldEntryPanel.add(
	        	new JLabel(label), 
				new GridBagConstraints(0, Y, 1, 1, 0.0, 0.0,
	            	GridBagConstraints.EAST, GridBagConstraints.NONE, 
					new Insets(4, 15, 4, 2), 0, 0));
	        fieldEntryPanel.add(tabselCombo,
				new GridBagConstraints(1, Y, 1, 1, 1.0, 1.0,
	            	GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
					new Insets(4, 0, 4, 10), 0, 0));
	        Y++;
		}
		// For HDB, Model ID
		if (theDb.isHdb())
		{
	        fieldEntryPanel.add(
	        	new JLabel(ceResources.getString("CompParmDialog.ModelIDLabel")), 
				new GridBagConstraints(0, Y, 1, 1, 0.0, 0.0,
	            	GridBagConstraints.EAST, GridBagConstraints.NONE, 
					new Insets(4, 15, 4, 2), 0, 0));
	        fieldEntryPanel.add(modelIdField,
				new GridBagConstraints(1, Y, 1, 1, 1.0, 1.0,
	            	GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
					new Insets(4, 0, 4, 10), 0, 0));
	        Y++;
		}

		siteSelectButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					siteSelectButtonPressed();
				}
			});
			
		getContentPane().add(outerPanel);
		
		
		IntervalDAI intervalDAO = theDb.makeIntervalDAO();
		try
		{
			for (String ic : intervalDAO.getValidIntervalCodes())
				intervalCombo.addItem(ic);
			for(String s : intervalDAO.getValidDurationCodes())
				durationCombo.addItem(s);
		}
		finally
		{
			intervalDAO.close();
		}

		//TODO If openTSDB, add field for them to type in a description.

	}

	protected void versionSelectButtonPressed()
	{
		Collection<String> versions = tsListPanel.getDistinctPart("Version");
		String[] versionArray = new String[versions.size()];
		versions.toArray(versionArray);
		String selection = (String)JOptionPane.showInputDialog(null, "Currently Used Versions:", 
			"Select Version", JOptionPane.PLAIN_MESSAGE, null, versionArray, versionField.getText());
		if (selection != null)
			versionField.setText(selection);
	}

	protected void paramSelectButtonPressed()
	{
		String label = theDb.isHdb() ? "DataType" : "Param";
		Collection<String> params = tsListPanel.getDistinctPart(label);
		String[] paramArray = new String[params.size()];
		params.toArray(paramArray);
		String selection = (String)JOptionPane.showInputDialog(null, "Currently Used " + label + "s:", 
			"Select " + label, JOptionPane.PLAIN_MESSAGE, null, paramArray, dataTypeField.getText());
		if (selection != null)
			dataTypeField.setText(selection);
	}

	/**
	 * Called when the OK button is pressed.
	 */
	void okButtonPressed()
	{
		okPressed = true;

		// Extract and validate the fields
		String siteLabel = theDb.isHdb() ? "Site" : "Location";
		DbKey siteId = Constants.undefinedId;
		Site site = null;
		String siteName = siteField.getText().trim();
		if (siteName.length() == 0)
		{
			showError(siteLabel + " cannot be blank!");
			return;
		}
		
		try
		{
			siteId = theDb.lookupSiteID(siteName);
			if (siteId == Constants.undefinedId)
			{
				showError(LoadResourceBundle.sprintf(
					ceResources.getString("CompParmDialog.InvalidSite"), siteName));
				return;
			}
			else
				site = theDb.getSiteById(siteId);
		}
		catch (DbIoException ex)
		{
			showError("Cannot look up " + siteLabel + " '" + siteName + "': " + ex);
			return;
		}
		catch (NoSuchObjectException ex)
		{
			showError("No such " + siteLabel + " '" + siteName + "': " + ex);
			return;
		}

		String dtLabel = theDb.isHdb() ? "DataType" : "Param";
		String dtcode = dataTypeField.getText().trim();
		if (dtcode.length() == 0)
		{
			showError(dtLabel + " cannot be blank!");
			return;
		}

		DataType dt = null;
		try
		{
			dt = theDb.lookupDataType(dtcode);
			if (dt == null)
			{
				showError("Invalid " + dtLabel + " '" + dtcode + "'");
				return;
			}
		}
		catch (DbIoException ex)
		{
			showError("Error looking up " + dtLabel + " '" + dtcode + "': " + ex);
			return;
		}
		catch (NoSuchObjectException e)
		{
			showError("No such " + dtLabel + " '" + dtcode + "'");
			return;
		}
			
		String interval = (String) intervalCombo.getSelectedItem();
		
		String tabSel = (String)(theDb.isHdb() ? tabselCombo.getSelectedItem() : null);
		
		String paramType = (String)(theDb.isHdb() ? null : paramTypeCombo.getSelectedItem());
		
		
		String duration = (String)(theDb.isHdb() ? null : durationCombo.getSelectedItem());
		String version = theDb.isCwms() || theDb.isOpenTSDB() ? versionField.getText().trim() : null;
		
		int modelId = -1;
		if (theDb.isHdb())
		{
			String modelIdStr = modelIdField.getText().trim();
			if (modelIdStr.length() > 0)
			{
				try
				{
					modelId = Integer.parseInt(modelIdStr);
				}
				catch (NumberFormatException ex)
				{
					showError(LoadResourceBundle.sprintf(
						ceResources.getString("CompParmDialog.OKError6"),
						modelIdStr));
					return;
				}
			}
		}
		
		TimeSeriesIdentifier tsid = theDb.makeEmptyTsId();
		tsid.setSiteName(siteName);
		tsid.setSite(site);
		tsid.setDataType(dt);
		tsid.setInterval(interval);
		if (theDb.isOpenTSDB())
		{
			String units = unitsField.getText().trim();
			if (units.length() == 0)
			{
				showError("Please select storage units for the new time series.");
				return;
			}
			tsid.setStorageUnits(units);
		}
		
		
		if (theDb.isCwms() || theDb.isOpenTSDB())
		{
			tsid.setPart("ParamType", paramType);
			tsid.setPart("Duration", duration);
			tsid.setPart("Version", version);
		}
		else // HDB
		{
			tsid.setTableSelector(tabSel);
			HdbTsId htsid = (HdbTsId)tsid;
			htsid.setModelId(modelId);
		}
	
		TimeSeriesDAI tsDAO = theDb.makeTimeSeriesDAO();
		try
		{
			tsDAO.createTimeSeries(tsid);
			tsidCreated = tsid;
		}
		catch(Exception ex)
		{
			showError("Cannot create time series '" + tsid.getUniqueString() + "': " + ex);
			tsidCreated = null;
			return;
		}
		finally
		{
			tsDAO.close();
		}
		
		closeDlg();
	}

	/** Closes the dialog. */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/**
	 * Called when the Cancel button is pressed.
	 */
	void cancelButtonPressed()
	{
		okPressed = false;
		closeDlg();
	}

	private void siteSelectButtonPressed()
	{
		launchDialog(siteSelectDialog);
		Site site = siteSelectDialog.getSelectedSite();
		if (site != null)
		{
			SiteName sn = site.getPreferredName();
			if (sn != null)
				siteField.setText(sn.getNameValue());
		}
	}

	public TimeSeriesIdentifier getTsidCreated()
	{
		return tsidCreated;
	}
	
	private void unitsButtonPressed()
	{
		EUSelectDialog dlg = new EUSelectDialog(this);
		launchDialog(dlg);
		EngineeringUnit eu = dlg.getSelection();
		if (eu != null)
			unitsField.setText(eu.abbr);
	}


}
