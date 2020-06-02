/**
 * $Id: CompParmDialog.java,v 1.14 2020/02/14 15:16:27 mmaloney Exp $
 * 
 * $Log: CompParmDialog.java,v $
 * Revision 1.14  2020/02/14 15:16:27  mmaloney
 * Fixes to Comp Edit for OpenTSDB
 *
 * Revision 1.13  2019/06/10 19:30:01  mmaloney
 * Separate out OpenTSDB from CWMS for datatype selection.
 *
 * Revision 1.12  2019/02/25 20:02:55  mmaloney
 * HDB 660 Allow Computation Parameter Site and Datatype to be set independently in group comps.
 *
 * Revision 1.11  2019/01/28 14:44:23  mmaloney
 * Remove stdout debug.
 *
 * Revision 1.10  2018/04/09 14:54:30  mmaloney
 * For HDB allow input parms to not previously exist. Ask user to create.
 *
 * Revision 1.9  2018/01/23 14:55:54  mmaloney
 * HDB 483 Cnvt single comp to group, bug--wasn't removing SDI.
 *
 * Revision 1.8  2017/11/02 20:46:21  mmaloney
 * Improve error message and fix potential HDB null ptr bug.
 *
 * Revision 1.7  2017/05/31 21:32:13  mmaloney
 * GUI improvements for HDB
 *
 * Revision 1.6  2017/01/11 14:09:14  mmaloney
 * CompEdit CompParmDialog Lookup time Series should allow wildcards in the site name
 * for CWMS.
 *
 * Revision 1.5  2016/10/11 19:03:55  mmaloney
 * Final GUI Prototype
 *
 * Revision 1.4  2016/06/27 15:41:00  mmaloney
 * Improve wording for data type prompt.
 *
 * Revision 1.3  2015/04/14 18:19:51  mmaloney
 * Fixed GUI issues with selecting Sites.
 *
 * Revision 1.2  2014/05/22 12:27:24  mmaloney
 * CWMS fix: Wasn't displaying Location after creating new TS.
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.26  2013/07/24 19:06:21  mmaloney
 * Don't restrict the datatype lookup to non-group comps.
 *
 * Revision 1.25  2013/03/21 18:27:39  mmaloney
 * DbKey Implementation
 *
 * Revision 1.24  2012/10/04 12:26:30  mmaloney
 * Improve error message.
 *
 * Revision 1.23  2012/07/27 13:13:24  mmaloney
 * Improve error msg.
 *
 * Revision 1.22  2012/07/27 12:52:02  mmaloney
 * Modifications for group comps in HDB.
 *
 * Revision 1.21  2012/07/27 12:40:24  mmaloney
 * Modifications for group comps in HDB.
 *
 * Revision 1.20  2012/06/21 21:11:55  mmaloney
 * Null pointer fix: Check interval for null before using.
 *
 * Revision 1.19  2012/06/21 21:05:04  mmaloney
 * Null pointer fix: Check interval for null before using.
 *
 * Revision 1.18  2012/06/04 19:27:56  mmaloney
 * Fixed CompParmDialog - select Data Type function for HDB.
 *
 * Revision 1.17  2012/05/18 19:59:26  mmaloney
 * null pointer bug.
 *
 * Revision 1.16  2012/05/18 19:23:24  mmaloney
 * null pointer bug.
 *
 * Revision 1.15  2012/05/10 14:15:31  mmaloney
 * fixed null pointer in comp parm dialog.
 *
 * Revision 1.14  2012/02/16 20:47:34  mmaloney
 * Store units in the DbCompParm so they are available to
 * theDb.transformTsidByCompParm. This enables a new time
 * series to be created with the correct units.
 *
 * Revision 1.13  2011/03/30 19:30:17  mmaloney
 * dev
 *
 * Revision 1.12  2011/03/24 19:32:40  mmaloney
 * Issue warning in CWMS if aggregate output in DST TimeZone that doesn't start with ~.
 *
 * Revision 1.11  2011/01/28 18:51:46  mmaloney
 * Fixed automatic deltas.
 * Implemented code for deltaTUnits
 *
 * Revision 1.10  2011/01/27 19:26:16  mmaloney
 * null pointer bug fix. DataType is allowed to be null.
 *
 * Revision 1.9  2011/01/18 13:51:51  mmaloney
 * debug
 *
 * Revision 1.8  2011/01/01 21:28:53  mmaloney
 * CWMS Testing
 *
 * Revision 1.7  2010/12/21 19:20:35  mmaloney
 * group computations
 *
 * Revision 1.6  2010/12/09 17:36:38  mmaloney
 * Use EUSelectDialog.java
 *
 * Revision 1.5  2010/12/05 15:51:52  mmaloney
 * Comp Parm Edits for DCSTool 5.0
 *
 * Revision 1.4  2010/11/28 21:05:25  mmaloney
 * Refactoring for CCP Time-Series Groups
 *
 */
package decodes.tsdb.compedit;

import java.awt.*;

import javax.swing.*;

import java.awt.event.*;

import javax.swing.border.*;

import opendcs.dai.IntervalDAI;
import opendcs.dai.SiteDAI;

import java.util.Calendar;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.TimeZone;

import ilex.util.AsciiUtil;
import ilex.util.LoadResourceBundle;
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
import decodes.sql.DbKey;
import decodes.tsdb.ConstraintException;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.IntervalIncrement;
import decodes.tsdb.MissingAction;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;
import decodes.tsdb.TsdbDatabaseVersion;
import decodes.tsdb.algo.RoleTypes;
import decodes.tsdb.groupedit.HdbDatatypeSelectDialog;
import decodes.tsdb.groupedit.LocSelectDialog;
import decodes.tsdb.groupedit.ParamSelectDialog;
import decodes.tsdb.groupedit.SelectionMode;
import decodes.tsdb.groupedit.VersionSelectDialog;
import decodes.util.DecodesSettings;

/**
 * For DCS Toolkit 5.0 we now treat input and output parameters the same way. So
 * we have once-again reintroduced this class.
 */
@SuppressWarnings("serial")
public class CompParmDialog extends GuiDialog
{
	private ResourceBundle ceResources = CAPEdit.instance().compeditDescriptions;
	private JPanel outerPanel = new JPanel();
	private JTextField compNameField = new JTextField();
	private JTextField roleNameField = new JTextField();
	private JButton okButton = new JButton();
	private JButton cancelButton = new JButton();
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
	private DbCompParm theParm;
	private ComputationsEditPanel parent;
	private String compName;
	boolean okPressed = false;
	private SiteSelectDialog siteSelectDialog = null;
	private TimeSeriesDb theDb;
	

	public CompParmDialog(boolean isInput, SiteSelectPanel siteSelectPanel)
	{
		super(CAPEdit.instance().getFrame(),
			CAPEdit.instance().compeditDescriptions
				.getString("CompParmDialog.CompParm"), true);
		theDb = CAPEdit.instance().theDb;
		
		if (theDb.isCwms() || theDb.isOpenTSDB())
		{
		}
		else
		{
			siteSelectDialog = new SiteSelectDialog(this);
			siteSelectDialog.setSiteSelectPanel(siteSelectPanel);
		}

		try
		{
			buildPanel(isInput);
			ifMissingCombo.insertItemAt("", 0);
			getRootPane().setDefaultButton(okButton);
			pack();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public void setInfo(ComputationsEditPanel parent, int tabIdx,
			String compName, DbCompParm theParm)
	{
		this.parent = parent;
		this.compName = compName;
		this.theParm = theParm;
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

		// If group input, then user can leave selections blank
		if (parent.hasGroupInput())
		{
			if (((String)intervalCombo.getItemAt(0)).length() != 0)
			{
				tabselCombo.insertItemAt("", 0);
				intervalCombo.insertItemAt("", 0);
				durationCombo.insertItemAt("", 0);
				paramTypeCombo.insertItemAt("", 0);
			}
		}
		else if (((String)intervalCombo.getItemAt(0)).length() == 0)
		{
			tabselCombo.removeItemAt(0);
			intervalCombo.removeItemAt(0);
			durationCombo.removeItemAt(0);
			paramTypeCombo.removeItemAt(0);
		}
		fillValues();
		pack();
	}

	/** Fills the GUI components with values from the object being edited. */
	void fillValues()
	{
//System.out.println("fillValues loc='" + theParm.getLocSpec() 
//+ "', parm='" + theParm.getParamSpec() + "', + ver='" + theParm.getVersion() + "'");
		compNameField.setText(compName);
		roleNameField.setText(theParm.getRoleName());
		
		SiteName sn = theParm.getSiteName();
		siteField.setText(sn != null ? sn.getNameValue() : "");
		if (sn != null && !theDb.isCwms() && !theDb.isOpenTSDB())
			siteSelectDialog.getSiteSelectPanel().setSelection(sn);
		if (sn == null && (theDb.isCwms() || theDb.isOpenTSDB()))
			siteField.setText(theParm.getLocSpec());
		siteField.setEnabled(true);
		siteSelectButton.setEnabled(true);

		DataType dt = theParm.getDataType();
		dataTypeField.setText(dt != null ? dt.getCode() : "");
		if (dt == null && (theDb.isCwms() || theDb.isOpenTSDB()))
			dataTypeField.setText(theParm.getParamSpec());

		String intvCode = theParm.getInterval();
		if (intvCode == null || intvCode.length() == 0)
			intervalCombo.setSelectedIndex(0);
		else
			intervalCombo.setSelectedItem(intvCode);
		if (!theDb.isCwms() && !theDb.isOpenTSDB())
			tabselCombo.setSelectedItem(theParm.getTableSelector());
		else // CWMS or OpenTSDB
		{
			paramTypeCombo.setSelectedItem(theParm.getParamType());
			durationCombo.setSelectedItem(theParm.getDuration());
			versionField.setText(theParm.getVersion());
		}
		deltaTField.setText("" + theParm.getDeltaT());
		modelIdField.setText("" + theParm.getModelId());
		String pt = theParm.getAlgoParmType();
		if (pt != null)
		{
			int pti = RoleTypes.getIndex(pt);
			if (pti >= 0)
				parmTypeField.setText(RoleTypes.getRoleType(pti));
		}
		roleNameField.setEditable(false);
		
		String s = theParm.getDeltaTUnits();
		if (s != null)
			deltaTUnitsCombo.setSelectedItem(s);
		else
			deltaTUnitsCombo.setSelectedIndex(0);
		if (theDb.tsdbVersion < TsdbDatabaseVersion.VERSION_6)
			deltaTUnitsCombo.setVisible(false);
		
		s = parent.getHiddenProperty(theParm.getRoleName() + "_EU");
		unitsField.setText(s != null ? s : "");
		
		if (theParm.isInput())
		{
			String n = theParm.getRoleName() + "_MISSING";
			s = parent.getHiddenProperty(n);
			if (s == null || s.trim().length() == 0)
				ifMissingCombo.setSelectedIndex(0);
			else
			{
				for(int i = 0; i<ifMissingCombo.getItemCount(); i++)
					if (s.equalsIgnoreCase(ifMissingCombo.getItemAt(i).toString()))
					{
						ifMissingCombo.setSelectedIndex(i);
						break;
					}
			}
		}
	}

	/** Initializes GUI components. */
	void buildPanel(boolean isInput)
		throws Exception
	{
		JPanel northPanel = new JPanel();
		JPanel southButtonPanel = new JPanel();
		JPanel fieldEntryPanel = new JPanel();
		outerPanel.setLayout(new BorderLayout());
		outerPanel.add(northPanel, BorderLayout.NORTH);
		outerPanel.add(fieldEntryPanel, BorderLayout.CENTER);
		outerPanel.add(southButtonPanel, BorderLayout.SOUTH);

		// NORTH Panel contains name, role, and parameter type
		northPanel.setBorder(new TitledBorder(
			BorderFactory.createLineBorder(new Color(153, 153, 153), 2), ""));
		northPanel.setLayout(new GridBagLayout());
		compNameField.setEditable(false);
		northPanel.add(
			new JLabel(ceResources.getString("CompParmDialog.ComputationName")),
			new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(6, 20, 3, 2), 0, 0));
		northPanel.add(compNameField,
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(6, 0, 3, 20), 0, 0));
		northPanel.add(
			new JLabel(ceResources.getString("CompParmDialog.RoleName")),
				new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, 
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(3, 20, 3, 2), 0, 0));
		roleNameField.setEditable(false);
		roleNameField.setToolTipText(ceResources
			.getString("CompParmDialog.RoleNameToolTip"));
		northPanel.add(roleNameField,
			new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 3, 20), 0, 0));
		northPanel.add(
			new JLabel(ceResources.getString("CompParmDialog.ParmType")),
				new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, 
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(3, 20, 6, 2), 0, 0));
		parmTypeField.setEditable(false);
		northPanel.add(parmTypeField, 
			new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 6, 20), 0, 0));
		parmTypeField.setToolTipText(
			ceResources.getString("CompParmDialog.ParmTypeToolTip"));

		// South panel contains OK and Cancel buttons
		okButton.setText("   "
			+ CAPEdit.instance().genericDescriptions.getString("OK")
			+ "   ");
		okButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				okButtonPressed();
			}
		});
		cancelButton.setText(CAPEdit.instance().genericDescriptions
			.getString("cancel"));
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
		fieldEntryPanel.setLayout(new GridBagLayout());
		String label = ceResources.getString("CompParmDialog.Site");
		if (theDb.isCwms())
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
		
		// Time Series Lookup Button
		fieldEntryPanel.add(selectTsButton,
			new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(4, 5, 4, 15), 0, 0));
		
		
		// Data Type or Param
		label = ceResources.getString("CompParmDialog.DataType");
		if (theDb.isCwms() || theDb.isOpenTSDB())
			label = "Param:";
		fieldEntryPanel.add(new JLabel(label),
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 15, 4, 2), 0, 0));
		fieldEntryPanel.add(dataTypeField, 
			new GridBagConstraints(1, 2, 1, 1, 1.0, 1.0, 
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
			new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(4, 5, 4, 15), 0, 0));

		// For CWMS, Param Type
		int Y = 3;
		if (theDb.isCwms() || theDb.isOpenTSDB())
		{
			fieldEntryPanel.add(new JLabel("Param Type:"),
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
		// For HDB, Model Run ID
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

		// Delta T
		fieldEntryPanel.add(
			new JLabel(ceResources.getString("CompParmDialog.DeltaTLabel")),
			new GridBagConstraints(0, Y, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4, 15, 4, 2), 0, 0));
		fieldEntryPanel.add(deltaTField, 
			new GridBagConstraints(1, Y, 1, 1, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(4, 0, 4, 10), 0, 0));
		deltaTField.setToolTipText(
			ceResources.getString("CompParmDialog.DeltaTToolTip"));
		fieldEntryPanel.add(deltaTUnitsCombo,
			new GridBagConstraints(2, Y, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(4, 5, 4, 15), 0, 0));
		Y++;

		// Units
		fieldEntryPanel.add(
			new JLabel(ceResources.getString("CompParmDialog.UnitsLabel")),
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
		Y++;
		
		// If Missing
		if (isInput)
		{
			fieldEntryPanel.add(
				new JLabel(ceResources.getString("CompParmDialog.IfMissingLabel")),
				new GridBagConstraints(0, Y, 1, 1, 0.0, 0.0,
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(4, 15, 4, 2), 0, 0));
			fieldEntryPanel.add(ifMissingCombo, 
				new GridBagConstraints(1, Y, 1, 1, 1.0, 1.0,
					GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
					new Insets(4, 0, 4, 10), 0, 0));
			Y++;
		}
		
		// ============================= KEEP THIS
		siteSelectButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					siteSelectButtonPressed();
				}
			});
		siteField.setToolTipText(
			ceResources.getString("CompParmDialog.SiteFieldToolTip"));
		dataTypeField.setToolTipText("Type data type code or press Lookup for list.");
		selectTsButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					selectTsButtonPressed();
				}
			});
		unitsButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					unitsButtonPressed();
				}
			});
			
		getContentPane().add(outerPanel);
	}

	protected void versionSelectButtonPressed()
	{
		VersionSelectDialog versionSelectDialog = 
			new VersionSelectDialog(CAPEdit.instance().getFrame(), theDb,
				parent.hasGroupInput() ? SelectionMode.CompEditGroup : SelectionMode.CompEditNoGroup);
		versionSelectDialog.setCurrentValue(versionField.getText());
		launchDialog(versionSelectDialog);
		if (!versionSelectDialog.isCancelled())
		{
			StringPair result = versionSelectDialog.getResult();
			versionField.setText(result.second);
		}
	}

	protected void paramSelectButtonPressed()
	{
//TODO Can't I do something like the CWMS dialog here?
		if (theDb.isCwms() || theDb.isOpenTSDB())
		{
			ParamSelectDialog paramSelectDialog = 
				new ParamSelectDialog(CAPEdit.instance().getFrame(), theDb,
					parent.hasGroupInput() ? SelectionMode.CompEditGroup : SelectionMode.CompEditNoGroup);
			paramSelectDialog.setCurrentValue(dataTypeField.getText());
	
			launchDialog(paramSelectDialog);
			if (!paramSelectDialog.isCancelled())
			{
				StringPair result = paramSelectDialog.getResult();
				dataTypeField.setText(result.second);
			}
		}
		else if (theDb.isHdb())
		{
			HdbDatatypeSelectDialog dlg = new HdbDatatypeSelectDialog(parent.topFrame, 
				(HdbTimeSeriesDb)theDb);
			dlg.setCurrentValue(dataTypeField.getText());
			launchDialog(dlg);
			StringPair result = dlg.getResult();
			if (result != null)
				dataTypeField.setText(result.first);
		}
//		else if (theDb.isOpenTSDB())
//		{
////TODO get rid of this and use the CWMS code above.
//			String s = JOptionPane.showInputDialog(this, "Enter Data Type:");
//			if (s != null)
//				dataTypeField.setText(s);
//		}
	}

	/**
	 * Called when the OK button is pressed.
	 */
	void okButtonPressed()
	{
		okPressed = true;

		// Validate the fields
		try
		{
			DbKey siteId = Constants.undefinedId;
			Site site = null;
			String siteName = siteField.getText().trim();
			if (siteName.length() > 0
			 && !siteName.contains("*-") && !siteName.contains("-*"))
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
			else if (!parent.hasGroupInput())
			{
				// No group and no site specified -- error!
				showError(ceResources.getString("CompParmDialog.OKError3"));
				return;
			}

			String dtcode = dataTypeField.getText().trim();
			DataType dt = null;
			if (dtcode.length() > 0
			 && !dtcode.contains("*-") && !dtcode.contains("-*"))
			{
				dt = theDb.lookupDataType(dtcode);
				if (dt == null)
				{
					showError("Invalid data type '" + dtcode + "'");
					return;
				}
			}
			else if (!parent.hasGroupInput())
			{
				// No group and no datatype specified -- error!
				showError(ceResources.getString("CompParmDialog.MissingDataType"));
				return;
			}
			else
			{
				// no data type and there IS a group on this comp.
				if (siteId != Constants.undefinedId
				 && theDb.getTsdbVersion() < TsdbDatabaseVersion.VERSION_9)
				{
					// Pre version 9 there was no independent SITE_ID in cp_comp_ts_parm.
					showError("Cannot specify site without data type in pre version 9 db!");
					return;
				}
			}
			
			String interval = (String) intervalCombo.getSelectedItem();
			String tabSel = "";
			String paramType = (theDb.isCwms() || theDb.isOpenTSDB()) ? 
				(String)paramTypeCombo.getSelectedItem() : null;
			String duration = (theDb.isCwms() || theDb.isOpenTSDB()) ? 
				(String)durationCombo.getSelectedItem() : null;
			String version = (theDb.isCwms() || theDb.isOpenTSDB()) ? 
				versionField.getText().trim() : null;
			if (theDb.isCwms() || theDb.isOpenTSDB())
			{
				// If algorithm is an aggregate (duration not 0), 
				// AND timezone honors DST
				// AND interval >= 1Day and does not start with '~'
				// THEN display a warning.
				IntervalIncrement iinc = IntervalCodes.getIntervalCalIncr(interval);
				if (duration != null && duration.trim().length() > 0
				 && !duration.equalsIgnoreCase(IntervalCodes.int_cwms_zero)
				 && interval != null && !interval.startsWith("~")
				 && iinc != null && iinc.getCalConstant() <= Calendar.DAY_OF_MONTH)
				{
					String tzs = parent.getProperty("aggregateTimeZone");
					if (tzs == null)
						tzs = DecodesSettings.instance().aggregateTimeZone;
					TimeZone tz = TimeZone.getTimeZone(tzs);
					if (tz.useDaylightTime())
					{
						if (JOptionPane.showConfirmDialog(this, 
							AsciiUtil.wrapString(
								"This output is an aggregate in a timezone (" + tzs
								+ ") that supports daylight time. "
								+ "You should use interval '~" + interval + "' to avoid having CWMS "
								+ "reject your data when the calendar switches between daylight/standard "
								+ "time. Do you want to continue anyway?", 60))
							!= JOptionPane.YES_OPTION)
						{
							return;
						}
					}
				}
				
				tabSel = paramType + "." + duration + "." + version;
				if (parent.hasGroupInput())
					tabSel = tabSel + "." + siteName + "." + dtcode;
			}
			else
			{
				tabSel = (String) tabselCombo.getSelectedItem();
			}

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

			String deltaTStr = deltaTField.getText().trim();
			int deltaT = 0;
			if (deltaTStr.length() > 0)
			{
				try
				{
					deltaT = Integer.parseInt(deltaTStr);
				}
				catch (NumberFormatException ex)
				{
					showError(LoadResourceBundle.sprintf(
						ceResources.getString("CompParmDialog.OKError7"),
						deltaTStr));
					return;
				}
			}
			String deltaTUnits = (String)deltaTUnitsCombo.getSelectedItem();
			if (deltaTUnits.length() == 0)
				deltaTUnits = null;

			if (!parent.hasGroupInput())
			{
				try
				{
					theDb.validateParm(siteId, dtcode, interval, tabSel, -1);
				}
				catch (ConstraintException ex)
				{
					showError(ceResources.getString("CompParmDialog.OKError8")
							+ ex.getMessage());
					return;
				}
			}

//			theParm.setSiteId(siteId);
			theParm.setSite(site);
			theParm.setDataType(dt);
			theParm.setInterval(interval);
			DbKey dtid = dt == null ? Constants.undefinedId : dt.getId();
			theParm.setDataTypeId(dtid);
//System.out.println("Setting tabsel='" + tabSel + "'");
			theParm.setTableSelector(tabSel);
			theParm.setDeltaT(deltaT);
			theParm.setDeltaTUnits(deltaTUnits);
			theParm.setModelId(Constants.undefinedIntKey);
			theParm.setModelId(modelId);

			String unitsAbbr = unitsField.getText().trim();
			if (unitsAbbr.length() == 0)
				unitsAbbr = null;
			parent.setHiddenProperty(theParm.getRoleName()+"_EU", unitsAbbr);
			
			String s;
			if (theParm.isInput())
			{
				s = ifMissingCombo.getSelectedItem().toString();
				if (s.length() == 0)
					s = null;
				parent.setHiddenProperty(theParm.getRoleName()+"_MISSING", s);
			}

// The following code is suspect: it is changing the site that is in the cache!!
// Furthermore it is assuming that the siteName in the TextField is of the preferred type,
// which is not necessarily true.
// It must never do this!
// Is this done so that when the dialog is displayed again, the same name can be displayed,
// and not some alias for the same site??
//			if (siteId != Constants.undefinedId)
//			{
//				theParm.addSiteName(
//					new SiteName(null, DecodesSettings.instance().siteNameTypePreference,
//						siteName));
//			}
//			else
//			{
//				theParm.clearSite();
//			}

			if (parent.hasGroupInput())
			{
				// HDB Issue 483, must clear out a possible SDI that was there before.
				if(theDb.isHdb() && (site == null || dt == null))
					theParm.setSiteDataTypeId(Constants.undefinedId);
			}
			else
			{
				String nm = parent.getProperty(
					theParm.getRoleName() + "_tsname");

				try
				{
					TimeSeriesIdentifier existingTsid = theDb.transformTsidByCompParm(
						null,    // Original tsid (don't have one) 
						theParm, // DbCompParm
						false,   // DON'T Create if doesn't exist
						true,    // Do fill in parm
						nm);     // display name
					
					if (existingTsid == null)
						throw new NoSuchObjectException("");
				}
				catch (NoSuchObjectException ex)
				{
					String algoParmType = theParm.getAlgoParmType();
					boolean canCreate = theDb.isHdb()
						|| (algoParmType != null && algoParmType.toLowerCase().startsWith("o"));

					if (canCreate && JOptionPane.showConfirmDialog(this, 
						 ceResources.getString("CompParmDialog.DoesntExist"))
						 == JOptionPane.YES_OPTION)
					{
						try
						{
							// Note nm might be null -- this is OK
							if (unitsAbbr != null && unitsAbbr.length() > 0)
								theParm.setUnitsAbbr(unitsAbbr);
							TimeSeriesIdentifier newTsid = theDb.transformTsidByCompParm(
								null,    // Original tsid (don't have one) 
								theParm, // DbCompParm
								true,    // Create if doesn't exist
								true,    // Do modify parm
								nm);     // display name
//SiteName sn = theParm.getSiteName();
//Logger.instance().debug3("After TS creation, siteName=" + (sn==null ? "null" : sn.getNameValue()));
						}
						catch (Exception exi)
						{
							String msg = "Cannot create time-series: " + exi;
							System.err.println(msg);
							exi.printStackTrace(System.err);
							showError(msg);
							return;
						}
					}
					else
					// Params for inputs must pre-exist.
					{
						showError("No such time series. Input parameters for non-group computations "
							+ "must refer to time series that already exist. Create it first.");
						return;
					}
				}
			}
		}
		catch (Exception ex)
		{
			String msg = ceResources.getString("CompParmDialog.OKError9") + " "
				+ ex;
			System.err.println(msg);
			ex.printStackTrace(System.err);
			showError(msg);
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
		if (theDb.isCwms() || theDb.isOpenTSDB())
		{
			LocSelectDialog locSelectDialog = new LocSelectDialog(CAPEdit.instance().getFrame(), theDb,
				parent.hasGroupInput() ? SelectionMode.CompEditGroup : SelectionMode.CompEditNoGroup);

			locSelectDialog.setCurrentValue(siteField.getText());
			launchDialog(locSelectDialog);
			if (!locSelectDialog.isCancelled())
			{
				StringPair result = locSelectDialog.getResult();
				if (result != null)
					siteField.setText(result.second);
			}
		}
		else
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
	}

	private void selectTsButtonPressed()
	{
		String siteName = siteField.getText().trim();
		if (siteName.length() == 0)
		{
			showError("Enter a Site/Location name to lookup time series.");
			return;
		}
		DbKey siteId = Constants.undefinedId;
		
		// TODO 'dataTypes' is a misnomer what I'm building is a list of string arrays:
		// the first row is the header labels for the select table.
		// subsequent rows contain the residual fields for time series (minus site).
		ArrayList<String[]> dataTypes;
		SiteDAI siteDAO = theDb.makeSiteDAO();
		try
		{
			siteId = siteDAO.lookupSiteID(siteName);
			if (DbKey.isNull(siteId))
			{
				if ((theDb.isCwms() || theDb.isOpenTSDB()) && siteName.contains("*"))
				{
					TsGroup tmpGroup = new TsGroup();
					tmpGroup.setGroupName("tmp");
					tmpGroup.setTransient();
					tmpGroup.addOtherMember("location", siteName);
					ArrayList<TimeSeriesIdentifier> tslist = theDb.expandTsGroup(tmpGroup);
					if (tslist.size() == 0)
					{
						showError("No time series match site spec '" + siteName + "'");
						return;
					}
					dataTypes = new ArrayList<String[]>();
					
					String tsIdParts[] = theDb.getTsIdParts();
					String hdr[] = new String[tsIdParts.length-1];
					for(int idx=1; idx < tsIdParts.length; idx++)
						hdr[idx-1] = tsIdParts[idx];
					dataTypes.add(hdr);
					HashSet<String> inList = new HashSet<String>();
					for(TimeSeriesIdentifier tsid : tslist)
					{
						String tsidstr=tsid.getUniqueString();
						tsidstr = tsidstr.substring(tsidstr.indexOf('.'));
						if (inList.add(tsidstr))
						{
							String row[] = new String[hdr.length];
							for(int idx=0; idx < hdr.length; idx++)
								row[idx] = tsid.getPart(hdr[idx]);
							dataTypes.add(row);
						}
					}
				}
				else
				{
					showError(LoadResourceBundle.sprintf(ceResources
						.getString("CompParmDialog.LookupButtonError2"),
						siteName));
					return;
				}
			}
			else
				dataTypes = theDb.getDataTypesForSite(siteId);
		}
		catch (Exception ex)
		{
			showError(ceResources
				.getString("CompParmDialog.LookupButtonError3") + " " + ex);
			return;
		}
		finally
		{
			siteDAO.close();
		}
		DataTypeSelectDialog dlg = new DataTypeSelectDialog(this, siteName,
			dataTypes);
		dlg.allowMultipleSelection(false);
		launchDialog(dlg);
		String dtinfo[] = dlg.getSelection();
		if (dtinfo != null)
		{
			dataTypeField.setText(dtinfo[0]);
			intervalCombo.setSelectedItem(dtinfo[2]);
			if (theDb.isCwms() || theDb.isOpenTSDB())
			{
				paramTypeCombo.setSelectedItem(dtinfo[1]);
				durationCombo.setSelectedItem(dtinfo[3]);
				versionField.setText(dtinfo[4]);
			}
			else if (theDb.isHdb())
			{
				tabselCombo.setSelectedIndex(
					dtinfo[3].charAt(0)=='R' ? 0 : 1);
				modelIdField.setText(
					dtinfo[3].charAt(0)=='M' ? dtinfo[4] : "-1");
			}
		}
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
