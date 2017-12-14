/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*  
*  $Log$
*  Revision 1.7  2017/08/22 19:57:22  mmaloney
*  Refactor
*
*  Revision 1.6  2017/05/31 21:32:13  mmaloney
*  GUI improvements for HDB
*
*  Revision 1.5  2016/10/11 19:03:55  mmaloney
*  Final GUI Prototype
*
*  Revision 1.4  2015/07/27 18:38:00  mmaloney
*  When re-applying algorithm, don't delete properties where the algorithm
*  property has an asterisk. E.g. algoprop="ex_*", don't delete anything that starts
*  with "ex_".
*
*  Revision 1.3  2015/06/04 21:43:22  mmaloney
*  Some refactoring to allow ProcessEditPanel under new Proc Monitor GUI
*
*  Revision 1.2  2014/05/22 12:27:24  mmaloney
*  CWMS fix: Wasn't displaying Location after creating new TS.
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.36  2013/08/19 12:21:12  mmaloney
*  dev
*
*  Revision 1.35  2013/08/18 22:11:08  mmaloney
*  dev
*
*  Revision 1.34  2013/08/18 21:57:40  mmaloney
*  dev
*
*  Revision 1.33  2013/08/18 20:00:27  mmaloney
*  dev
*
*  Revision 1.32  2013/08/18 19:48:45  mmaloney
*  Implement EffectiveStart/End relative properties
*
*  Revision 1.31  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*/
package decodes.tsdb.compedit;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.TimeZone;
import java.text.ParseException;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import opendcs.dai.ComputationDAI;
import ilex.gui.DateTimeCalendar;
import ilex.util.AsciiUtil;
import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.util.PropertiesUtil;
import ilex.util.LoadResourceBundle;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.SiteName;
import decodes.dbeditor.SiteSelectPanel;
import decodes.gui.PropertiesEditPanel;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbAlgoParm;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbComputation;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalIncrement;
import decodes.tsdb.TsGroup;
import decodes.tsdb.TsdbDatabaseVersion;
import decodes.tsdb.TsdbDateFormat;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.algo.AW_AlgorithmBase;
import decodes.tsdb.comprungui.CompRunGuiFrame;
import decodes.tsdb.groupedit.TsGroupSelectDialog;
import decodes.util.DecodesSettings;


/**
 * Edit Panel for a computation
 */
public class ComputationsEditPanel 
	extends EditPanel 
{
	private static final long serialVersionUID = 1L;
	private JTextField nameField = new JTextField();
	private JTextField algorithmField = new JTextField();
	private JCheckBox enabledCheck = new JCheckBox();
	private JTextField idField = new JTextField();
	private JTextField modifiedField = new JTextField();
	private JComboBox processCombo = new JComboBox();
	private JTextField groupField = new JTextField();

	private JScrollPane tablePane = null;
	private JTextArea commentsArea = null;
	private PropertiesEditPanel propertiesPanel = null;
	private JButton deleteParamButton = new JButton();
	private JButton editParamButton = new JButton();

	private JTable compParmTable = null;
	CompParmTableModel compParmTableModel = null;

	private DbComputation editedObject = null;
	private DbCompAlgorithm currentAlgorithm = null;
	private String appStrings[];
	private Properties propCopy = null;
	private boolean runCompGUIUp = false;
	private CompRunGuiFrame rcframe = null;
	ResourceBundle ceResources = null;
	ResourceBundle genResources = null;
	private Properties hiddenProps = new Properties();
	SiteSelectPanel siteSelectPanel = new SiteSelectPanel();
	private TsGroup selectedGroup = null;
	private TimeZone guiTimeZone = null;
	private TsdbDateFormat tsdbDateFormat = null;

	private JComboBox sinceMethodCombo = new JComboBox(
		new String[] {"No limit", "Now -", "Calendar" });
	private JPanel sinceContentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
	private DateTimeCalendar sinceDateTimeCal = null;
	private JPanel sinceNowMinusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
	private JComboBox sinceNowMinusCombo = new JComboBox(
		new String[] { "4 hour", "8 hours", "1 day" });
	
	private JComboBox untilMethodCombo = new JComboBox(
		new String[] {"No limit", "Now", "Now +", "Calendar" });
	private JPanel untilContentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
	private DateTimeCalendar untilDateTimeCal = null;
	private JPanel untilNowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
	private JPanel untilNowPlusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
	private JComboBox untilNowPlusCombo = new JComboBox(
		new String[] { "4 hour", "8 hours", "1 day" });
	private JLabel untilNowExpl = new JLabel();
	
	public ComputationsEditPanel()
	{
		runCompGUIUp = false;
		ceResources = CAPEdit.instance().compeditDescriptions;
		genResources = CAPEdit.instance().genericDescriptions;
		try
		{
			guiTimeZone = TimeZone.getTimeZone(DecodesSettings.instance().guiTimeZone);
		}
		catch(Exception ex)
		{
			Logger.instance().warning("Invalid guiTimeZone setting '"
				+ guiTimeZone + "' -- defaulting to UTC");
			guiTimeZone = TimeZone.getTimeZone("UTC");
		}
		tsdbDateFormat = new TsdbDateFormat(guiTimeZone);
		buildPanels();
		setTopFrame(CAPEdit.instance().getFrame());
	}
	
	private void buildPanels()
	{
		setLayout(new BorderLayout());
		this.add(buildCenterPanel(), java.awt.BorderLayout.CENTER);
		this.add(getButtonPanel(), java.awt.BorderLayout.SOUTH);
	}
	
	private JPanel buildCenterPanel() 
	{
		JPanel ret = new JPanel(new GridBagLayout());

		ret.add(buildInputPanel(),
			new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(2,2,2,2), 0, 0));
		ret.add(buildCommentsPanel(),
			new GridBagConstraints(0, 1, 1, 1, 1.0, .3,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(2,2,2,2), 0, 60));
		ret.add(buildParametersPanel(), 
			new GridBagConstraints(0, 2, 1, 1, 1.0, 0.35,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(2,2,2,2), 0, 60));

		propertiesPanel = new PropertiesEditPanel(new Properties());
		propertiesPanel.setTitle(
			ceResources.getString("ComputationsEditPanel.PropertiesPanelTitle"));
		propertiesPanel.setOwnerFrame(CAPEdit.instance().getFrame());
		ret.add(propertiesPanel,
			new GridBagConstraints(0, 3, 1, 1, 1.0, 0.35,
				GridBagConstraints.NORTH, GridBagConstraints.BOTH,
				new Insets(2,2,2,2), 0, 60));
				
		return ret;
	}

	private JPanel buildCommentsPanel() 
	{
		JPanel commentsPanel = new JPanel();
		commentsPanel.setLayout(new BorderLayout());
		commentsPanel.setBorder(
			new TitledBorder(
				BorderFactory.createLineBorder(java.awt.Color.gray, 2),
				ceResources.getString("ComputationsEditPanel.CommentsBorder")));
		
		commentsArea = new JTextArea();
		commentsArea.setWrapStyleWord(true);
		commentsArea.setLineWrap(true);
		commentsArea.setToolTipText(
			ceResources.getString("ComputationsEditPanel.CommentsError1"));

		JScrollPane commentsScrollPane = new JScrollPane();
		commentsScrollPane.setHorizontalScrollBarPolicy(
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		commentsScrollPane.setViewportView(commentsArea);
		
		commentsPanel.add(commentsScrollPane, java.awt.BorderLayout.CENTER);
		return commentsPanel;
	}

	protected JPanel buildParametersPanel()
	{
		JPanel paramsPanel = new JPanel();
		paramsPanel.setLayout(new BorderLayout());
		paramsPanel.setBorder(
			new TitledBorder(
				BorderFactory.createLineBorder(java.awt.Color.gray, 2),
				ceResources.getString("ComputationsEditPanel.ParametersTitle")));
		paramsPanel.add(getTablePane(), BorderLayout.CENTER);

		editParamButton.setText(genResources.getString("edit"));
		editParamButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e) 
				{
					editParamButtonPressed();
				}
			});

		JPanel tableButtonPanel = new JPanel(new GridBagLayout());
		tableButtonPanel.add(editParamButton,
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
				new Insets(8,4,4,4), 30, 0));
		
		deleteParamButton.setText(genResources.getString("delete"));
		deleteParamButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e) {
					deleteParamButtonPressed();
				}
			});
		tableButtonPanel.add(deleteParamButton,
			new GridBagConstraints(0, 1, 1, 1, 0.0, 1.0,
				GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
				new Insets(4,4,4,4), 30, 0));

		
		paramsPanel.add(tableButtonPanel, java.awt.BorderLayout.EAST);
		return paramsPanel;
	}

	public void setEditedObject(DbComputation dc)
	{
		editedObject = dc;

		// Fill in the Process combo with list of apps.
		ArrayList<CompAppInfo> apps =
			CAPEdit.instance().processesListPanel.procTableModel.myvector;
		appStrings = new String[apps.size()+1];
		appStrings[0] = CAPEdit.instance().compeditDescriptions
				    		.getString("ComputationsEditPanel.None");
		processCombo.addItem(appStrings[0]);
		for(int i=0; i<apps.size(); i++)
		{
			CompAppInfo cai = apps.get(i);
			appStrings[i+1] = "" + cai.getAppId() + ": " + cai.getAppName();
			processCombo.addItem(appStrings[i+1]);
		}

		// fill in controls:
		nameField.setText(editedObject.getName());
		idField.setText("" + editedObject.getId());
		algorithmField.setText(editedObject.getAlgorithmName());
		groupField.setText(editedObject.getGroupName());
		selectedGroup = editedObject.getGroup();

		currentAlgorithm = dc.getAlgorithm();

		enabledCheck.setSelected(editedObject.isEnabled());
		commentsArea.setText(editedObject.getComment());

		Properties compProps = editedObject.getProperties();
		propCopy = new Properties();
		hiddenProps.clear();
		for(Enumeration pnames = compProps.propertyNames();
			pnames.hasMoreElements(); )
		{
			String pname = (String)pnames.nextElement();
			String value = compProps.getProperty(pname);
			if (TextUtil.endsWithIgnoreCase(pname, "_EU")
			 || TextUtil.endsWithIgnoreCase(pname, "_MISSING")
			 || pname.equalsIgnoreCase("EffectiveStart")
			 || pname.equalsIgnoreCase("EffectiveEnd"))
			{
				setHiddenProperty(pname, value);
			}
			else
			{
				propCopy.setProperty(pname, value);
			}
		}
		
		propertiesPanel.setProperties(propCopy);
		setPropertiesPanelOwner();

		String s;
		Date d = editedObject.getValidStart();
		if (d != null)
		{
			sinceDateTimeCal.setDate(d);
			sinceMethodCombo.setSelectedIndex(2); // Calendar
		}
		else if ((s = editedObject.getProperty("EffectiveStart")) != null)
		{
			sinceMethodCombo.setSelectedIndex(1);
			int idx = s.indexOf('-');
			if (idx > s.length()-1)
				sinceNowMinusCombo.setSelectedItem(s.substring(idx+1).trim());
		}
		else
			sinceMethodCombo.setSelectedIndex(0);
		sinceMethodComboChanged();

		d = editedObject.getValidEnd();
		if (d != null)
		{
			untilDateTimeCal.setDate(d);
			untilMethodCombo.setSelectedIndex(3);
		}
		else if ((s = editedObject.getProperty("EffectiveEnd")) != null)
		{
			if (s.trim().equalsIgnoreCase("now"))
				untilMethodCombo.setSelectedIndex(1);
			else
			{
				untilMethodCombo.setSelectedIndex(2);
				int idx = s.indexOf('+');
				if (idx > s.length()-1)
					untilNowPlusCombo.setSelectedItem(s.substring(idx+1).trim());
			}
		}
		else
			untilMethodCombo.setSelectedIndex(0);
		untilMethodComboChanged();
		
		d = editedObject.getLastModified();
		modifiedField.setText(d == null ? "" : tsdbDateFormat.format(d));

		DbKey appId = editedObject.getAppId();
		if (appId == Constants.undefinedId)
			processCombo.setSelectedIndex(0);
		else
		{
			String appIdStr = "" + appId;
			for(int i=1; i<appStrings.length; i++)
			{
				String cmpStr = appStrings[i].substring(0, 
					appStrings[i].indexOf(':'));
				if (cmpStr.equals(appIdStr))
				{
					processCombo.setSelectedIndex(i);
					break;
				}
			}
		}

		for(Iterator<DbCompParm> dcpi = dc.getParms(); dcpi.hasNext(); )
		{
			DbCompParm dcp = dcpi.next();
			DbAlgoParm dap = currentAlgorithm.getParm(dcp.getRoleName());
			if (dap != null)
				dcp.setAlgoParmType(dap.getParmType());
			
			if (!dcp.getSiteDataTypeId().isNull())
			{
				try { CAPEdit.instance().theDb.expandSDI(dcp); }
				catch(Exception ex)
				{
					showError(CAPEdit.instance().compeditDescriptions
				    		.getString("ComputationsEditPanel.ExpandError") + ex);
					System.err.println(CAPEdit.instance().compeditDescriptions
				    		.getString("ComputationsEditPanel.ExpandError") + ex);
					ex.printStackTrace(System.err);
				}
			}
		}
	
		compParmTableModel.fill(editedObject);
	}

	public DbComputation getEditedObject()
	{
		return editedObject;
	}

	/**
	 * Hidden properties do not show up in the properties panel, they are edited
	 * in other GUI elements.
	 * @param name the name of the hidden property
	 * @return the value
	 */
	public String getHiddenProperty(String name)
	{
		return hiddenProps.getProperty(name);
	}
	
	public void setHiddenProperty(String name, String value)
	{
		if (value == null)
			hiddenProps.remove(name);
		else
			hiddenProps.setProperty(name, value);
	}
	
	/**
	 * This method initializes jPanel
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel buildInputPanel() 
	{
		JPanel inputPanel = new JPanel(new GridBagLayout());
		
		sinceDateTimeCal = new DateTimeCalendar(guiTimeZone.getID(), new Date(0L), "dd/MMM/yyyy", 
			guiTimeZone.getID());
		untilDateTimeCal = new DateTimeCalendar(guiTimeZone.getID(), new Date(), "dd/MMM/yyyy", 
			guiTimeZone.getID());

		untilNowExpl.setText(ceResources.getString("ComputationsEditPanel.NoFuture"));

		// Comutation Name
		inputPanel.add(
			new JLabel(ceResources.getString("ComputationsEditPanel.CompName")),
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(6,10,3,2), 0, 0));
		nameField.setEditable(false);
		nameField.setToolTipText(CAPEdit.instance().compeditDescriptions
	    		.getString("ComputationsEditPanel.NameToolTip"));
		inputPanel.add(nameField,
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
				new Insets(6,0,3,4), 0, 0));

		JButton changeNameButton = new JButton(
			ceResources.getString("ComputationsEditPanel.ChangeButton"));
		changeNameButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e) {
					changeNameButtonPressed();
				}
			});
		inputPanel.add(changeNameButton, 
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
				new Insets(6,3,3,0), 0, 0));
				
		// Computation ID
		inputPanel.add(
			new JLabel(ceResources.getString("ComputationsEditPanel.CompID")),
			new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(6,10,3,2), 0, 0));
		idField.setEditable(false);
		idField.setToolTipText(
				CAPEdit.instance().compeditDescriptions
				.getString("ComputationsEditPanel.IDFieldToolTip"));
		inputPanel.add(idField,
			new GridBagConstraints(5, 0, 2, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(6,0,3,6), 100, 0));

		// Algorithm
		inputPanel.add(
			new JLabel(ceResources.getString("ComputationsEditPanel.Algorithm")),
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3,10,3,2), 0, 0));
		algorithmField.setEditable(false);
		algorithmField.setToolTipText(
				ceResources.getString("ComputationsEditPanel.AlgorithmToolTip"));
		inputPanel.add(algorithmField,
			new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
				new Insets(3,0,3,4), 0, 0));
		JButton changeAlgoButton = new JButton(
			genResources.getString("select"));
		changeAlgoButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e) {
					changeAlgoButtonPressed();
				}
			});
		inputPanel.add(changeAlgoButton, 
			new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
				new Insets(3,3,3,0), 0, 0));

		// Last Modified
		inputPanel.add(
			new JLabel(ceResources.getString("ComputationsEditPanel.ModifiedLabel")),
			new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3,10,3,2), 0, 0));
		modifiedField.setEditable(false);
		modifiedField.setToolTipText(
			ceResources.getString("ComputationsEditPanel.ModifiedToolTip"));
		inputPanel.add(modifiedField,
			new GridBagConstraints(5, 1, 2, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(3,0,3,6), 130, 0));

		// Enabled Checkbox and Loading Application (Process) Name
		enabledCheck.setText(
			ceResources.getString("ComputationsEditPanel.enabledLabel"));
		enabledCheck.setSelected(true);
		enabledCheck.setToolTipText(
			ceResources.getString("ComputationsEditPanel.EnabledCheckToolTip"));
		inputPanel.add(enabledCheck, 
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3,10,3,2), 0, 0));
		processCombo.setToolTipText(
			ceResources.getString("ComputationsEditPanel.ProcessToolTip"));
		inputPanel.add(processCombo,
			new GridBagConstraints(1, 2, 1, 1, 0.0, 1.0,
				GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
				new Insets(3,0,3,4), 0, 0));

		// Effective Start
		inputPanel.add(
			new JLabel(ceResources.getString("ComputationsEditPanel.StartLabel")),
			new GridBagConstraints(4, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3,20,3,2), 0, 0));
		sinceDateTimeCal.setToolTipText(
			LoadResourceBundle.sprintf(
				ceResources.getString("ComputationsEditPanel.TimeToolTip"),
				tsdbDateFormat.getDefaultFormat()));
		
		// Since Method Combo specifies how user wants to set the valid start (since) time
		sinceNowMinusCombo.setEditable(true);
		sinceNowMinusPanel.add(sinceNowMinusCombo);
		sinceMethodCombo.setSelectedIndex(0);
		sinceMethodCombo.addActionListener(
			new java.awt.event.ActionListener() 
			{
				public void actionPerformed(ActionEvent e) 
				{
					sinceMethodComboChanged();
				}
			});
		
		inputPanel.add(sinceMethodCombo,
			new GridBagConstraints(5, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 2, 2, 4), 0, 0));
		
		// sinceContentPanel changes depending on combo setting
		inputPanel.add(sinceContentPanel,
			new GridBagConstraints(6, 2, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 2, 2, 4), 100, 0));

		// default is no limit, so sinceContentPanel will be empty by default
		sinceContentPanel.removeAll();
		
		// Default since calendar to Jan 1 of this year.
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(guiTimeZone);
		cal.setTime(new Date());
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.set(Calendar.MONTH, 0);
		sinceDateTimeCal.setDate(cal.getTime());
		
		// Group
		inputPanel.add(
			new JLabel(ceResources.getString("CompParmDialog.Group")),
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3,10,3,2), 0, 0));
		groupField.setEditable(false);
		groupField.setToolTipText(
			ceResources.getString("ComputationsEditPanel.GroupToolTip"));
		inputPanel.add(groupField,
			new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
				new Insets(3,0,3,4), 0, 0));
		

		JButton changeGroupButton = new JButton(
			genResources.getString("select"));
		changeGroupButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e) {
					selectGroupPressed();
				}
			});
		inputPanel.add(changeGroupButton, 
			new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
				new Insets(3,3,3,0), 0, 0));

		TimeSeriesDb tsdb = CAPEdit.instance().getTimeSeriesDb();
		if (tsdb.isHdb() && tsdb.getTsdbVersion() < TsdbDatabaseVersion.VERSION_6)
		{
			groupField.setEnabled(false);
			changeGroupButton.setEnabled(false);
		}

		// Effective End
		inputPanel.add(
			new JLabel(ceResources.getString("ComputationsEditPanel.EndLabel")),
			new GridBagConstraints(4, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3,20,3,2), 0, 0));

		untilDateTimeCal.setToolTipText(
			LoadResourceBundle.sprintf(
				ceResources.getString("ComputationsEditPanel.TimeToolTip"),
				tsdbDateFormat.getDefaultFormat()));
		
		// Until Method Combo specifies how user wants to set the valid end (until) time
		untilNowPlusCombo.setEditable(true);
		untilNowPlusPanel.add(untilNowPlusCombo);
		untilMethodCombo.setSelectedIndex(0);
		untilMethodCombo.addActionListener(
			new java.awt.event.ActionListener() 
			{
				public void actionPerformed(ActionEvent e) 
				{
					untilMethodComboChanged();
				}
			});
		
		inputPanel.add(untilMethodCombo,
			new GridBagConstraints(5, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 2, 2, 4), 0, 0));
		
		// sinceContentPanel changes depending on combo setting
		inputPanel.add(untilContentPanel,
			new GridBagConstraints(6, 3, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 2, 2, 4), 100, 0));

		// default is no limit, so sinceContentPanel will be empty by default
		untilContentPanel.removeAll();
		
		// Default until calendar to now of this year.
		cal.setTime(new Date());
		untilDateTimeCal.setDate(cal.getTime());
		
		untilDateTimeCal.setToolTipText(
			LoadResourceBundle.sprintf(
				ceResources.getString("ComputationsEditPanel.TimeToolTip"),
				tsdbDateFormat.getDefaultFormat()));
		
		return inputPanel;
	}

	private JScrollPane getTablePane() {
		if (tablePane == null) {
			tablePane = new JScrollPane();
			tablePane.setViewportView(getCompParmTable());
		}
		return tablePane;
	}

	protected JTable getCompParmTable() 
	{
		if (compParmTableModel == null) 
		{
			compParmTableModel = new CompParmTableModel(this);
			compParmTable = new SortingListTable(compParmTableModel,
				compParmTableModel.columnWidths);
			compParmTable.addMouseListener(
				new MouseAdapter()
				{
					public void mouseClicked(MouseEvent e)
					{
						if (e.getClickCount() == 2)
						{
							editParamButtonPressed();
						}
					}
				});

		}
		return compParmTable;
	}

	protected void doCommit() 
	{
		String nm = nameField.getText().trim();
		if (nm.length() == 0)
		{
			showError(CAPEdit.instance().compeditDescriptions
					.getString("ComputationsEditPanel.CommentsError1"));
			return;
		}

		ComputationsListPanel lp = CAPEdit.instance().computationsListPanel;
		if (lp.compListTableModel.compExists(nm)
		 && editedObject.getId() == Constants.undefinedId)
		{
			showError(CAPEdit.instance().compeditDescriptions
					.getString("ComputationsEditPanel.CommentsError2"));
			return;
		}

		ArrayList<String> newParms = new ArrayList<String>();
		if (currentAlgorithm != null)
		{
			for (Iterator<DbAlgoParm> it = currentAlgorithm.getParms();
				it.hasNext(); )
			{
				DbAlgoParm dap = it.next();
				String name = dap.getRoleName();
				DbCompParm dcp = compParmTableModel.findByName(name, -1);
				if (dcp == null)
					newParms.add(name);
			}
		}
		int size = newParms.size();
		if (size > 0 && currentAlgorithm != null)
		{
			StringBuilder sb = new StringBuilder(
					LoadResourceBundle.sprintf(
					CAPEdit.instance().compeditDescriptions
					.getString("ComputationsEditPanel.AlgoMessage1") , currentAlgorithm.getName()) 
				);
			for(int i=0; i<size; i++)
			{
				if (i > 0)
					sb.append(", ");
				sb.append(newParms.get(i));
			}
			sb.append(CAPEdit.instance().compeditDescriptions
					.getString("ComputationsEditPanel.AlgoMessage2"));
			int ok = JOptionPane.showConfirmDialog(this, 
				AsciiUtil.wrapString(sb.toString(), 60));
			if (ok != JOptionPane.YES_OPTION)
				return;
		}
		
		String ps = (String)processCombo.getSelectedItem();
		if (enabledCheck.isSelected() &&
			ps.equals(
				CAPEdit.instance().compeditDescriptions.getString(
					"ComputationsEditPanel.None")))
		{
			int ok = JOptionPane.showConfirmDialog(this, 
				AsciiUtil.wrapString(
					CAPEdit.instance().compeditDescriptions.getString(
						"ComputationsEditPanel.EnabledNoProc"), 60));
			if (ok != JOptionPane.YES_OPTION)
				return;
		}
		
		saveToObject(editedObject);

		ComputationDAI computationDAO = CAPEdit.instance().theDb.makeComputationDAO();
		try 
		{
			computationDAO.writeComputation(editedObject); 
			idField.setText("" + editedObject.getId());
			Date d = editedObject.getLastModified();
			modifiedField.setText(d == null ? "" : tsdbDateFormat.format(d));
			lp.doRefresh();
		}
		catch(DbIoException ex)
		{
			showError(CAPEdit.instance().compeditDescriptions
					.getString("ComputationsEditPanel.CommentsError5") + ex);
		}
		finally
		{
			computationDAO.close();
		}
	}

	public DbComputation getEditedDbComputation()
	{
		DbComputation dbCompToRun = 
			new DbComputation(Constants.undefinedId,"");
		saveToObject(dbCompToRun);
		dbCompToRun.setId(editedObject.getId());
		return dbCompToRun;
	}

	private void saveToObject(DbComputation ob)
	{
		String nm = nameField.getText().trim();
		ob.setName(nm);
		ob.setComment(commentsArea.getText());
		ob.setEnabled(enabledCheck.isSelected());
		
		switch(sinceMethodCombo.getSelectedIndex())
		{
		case 0: // no limit
			ob.setValidStart(null);
			PropertiesUtil.rmIgnoreCase(hiddenProps, "EffectiveStart");
			break;
		case 1: // now - N interval
			ob.setValidStart(null);
			hiddenProps.setProperty("EffectiveStart", "now - " + sinceNowMinusCombo.getSelectedItem());
			break;
		case 2: // Calendar
			ob.setValidStart(sinceDateTimeCal.getDate());
			PropertiesUtil.rmIgnoreCase(hiddenProps, "EffectiveStart");
			break;
		}

		switch(untilMethodCombo.getSelectedIndex())
		{
		case 0: // no limit
			ob.setValidEnd(null);
			PropertiesUtil.rmIgnoreCase(hiddenProps, "EffectiveEnd");
			break;
		case 1: // now
			ob.setValidEnd(null);
			hiddenProps.setProperty("EffectiveEnd", "now");
			break;
		case 2: // now + N interval
			ob.setValidEnd(null);
			hiddenProps.setProperty("EffectiveEnd", "now + " + untilNowPlusCombo.getSelectedItem());
			break;
		case 3: // Calendar
			ob.setValidEnd(untilDateTimeCal.getDate());
			PropertiesUtil.rmIgnoreCase(hiddenProps, "EffectiveEnd");
			break;
		}
		
		propertiesPanel.saveChanges();
		ob.getProperties().clear();
		PropertiesUtil.copyProps(ob.getProperties(), propCopy);
		PropertiesUtil.copyProps(ob.getProperties(), hiddenProps);

		String algoName = algorithmField.getText();
		ob.setAlgorithmName(algoName);
		ob.setAlgorithm(currentAlgorithm);

		String ps = (String)processCombo.getSelectedItem();
		if (ps.equals(CAPEdit.instance().compeditDescriptions
	    		.getString("ComputationsEditPanel.None")))
		{
			ob.setAppId(Constants.undefinedId);
			ob.setApplicationName(null);
		}
		else
		{
			int idx = ps.indexOf(':');
			try { ob.setAppId(DbKey.createDbKey(Long.parseLong(ps.substring(0, idx)))); }
			catch(NumberFormatException ex) {}
			ob.setApplicationName(ps.substring(idx+2));
		}
		
		// Handle group name field
		ob.setGroup(selectedGroup);

		// fill in controls:
		compParmTableModel.saveTo(ob);
	}

	protected void doClose() 
	{
		DbComputation testCopy = editedObject.copyNoId();
		saveToObject(testCopy);
		if (!editedObject.equalsNoId(testCopy))
		{
			int r = JOptionPane.showConfirmDialog(this, 
					CAPEdit.instance().compeditDescriptions
		    		.getString("ComputationsEditPanel.ClosePrompt"));
			if (r == JOptionPane.CANCEL_OPTION)
				return;
			else if (r == JOptionPane.YES_OPTION)
				doCommit();
		}
		if (rcframe != null)
		{
			if (rcframe.closeFromParent() == false)
				return;
		}
		JTabbedPane tabbedPane = CAPEdit.instance().getComputationsTab();
		tabbedPane.remove(this);
	}


	/**
	 * This method initializes jButton1
	 * 
	 * @return javax.swing.JButton
	 */
	private void changeNameButtonPressed()
	{
	    String newName = JOptionPane.showInputDialog(
	    		CAPEdit.instance().compeditDescriptions
	    		.getString("ComputationsEditPanel.InputName"));
		if (newName == null)
			return;
		newName = newName.trim();
		if (newName.equals(editedObject.getName()))
			return;
		if (newName.length() == 0)
		{
			showError(CAPEdit.instance().compeditDescriptions
		    		.getString("ComputationsEditPanel.NameError1"));
			return;
		}
		ComputationsListPanel lp = CAPEdit.instance().computationsListPanel;
		if (lp.compListTableModel.compExists(newName))
		{
			CAPEdit.instance().getFrame().showError(
					CAPEdit.instance().compeditDescriptions
		    		.getString("ComputationsEditPanel.NameError2"));
			return;
		}
		nameField.setText(newName);
		JTabbedPane tab = CAPEdit.instance().computationsTab;
		int idx = tab.indexOfComponent(this);
		if (idx != -1)
			tab.setTitleAt(idx, newName);
	}

	private void changeAlgoButtonPressed()
	{
		AlgoSelectDialog dlg = new AlgoSelectDialog(nameField.getText(), 
			currentAlgorithm);
		CAPEdit.instance().getFrame().launchDialog(dlg);
		if (dlg.okPressed)
		{
			if (currentAlgorithm != null 
			 && currentAlgorithm != dlg.selectedAlgo)
			{
				ArrayList<String> parms2delete = new ArrayList<String>();
				ArrayList<String> props2delete = new ArrayList<String>();
				StringBuilder plist = new StringBuilder();
				int n=0;
				for(int i=0; i < compParmTableModel.getRowCount(); i++)
				{
					DbCompParm dcp = (DbCompParm)
						compParmTableModel.getRowObject(i);

					// If new algorithm doesn't have param with same role name
					if (dlg.selectedAlgo.getParm(dcp.getRoleName()) == null)
					{
						parms2delete.add(dcp.getRoleName());
						if (n > 0) plist.append(", ");
						n++;
						plist.append(dcp.getRoleName());
					}
				}
				propertiesPanel.saveChanges();
			nextCompProp:
				for(Enumeration cpnenum = propCopy.propertyNames(); cpnenum.hasMoreElements(); )
				{
					String pname = (String)cpnenum.nextElement();
					if (dlg.selectedAlgo.getProperty(pname) == null)
					{
						// MJM 20150727 check for algo props that have wildcards.
						for(Enumeration apnenum = dlg.selectedAlgo.getPropertyNames();
							apnenum.hasMoreElements(); )
						{
							String apn = (String)apnenum.nextElement();
							int starIdx = apn.indexOf('*');
							if (starIdx >= 0 && pname.startsWith(apn.substring(0, starIdx)))
								continue nextCompProp;
						}
						
						if (n > 0) plist.append(",");
						n++;
						plist.append(" " + pname);
						props2delete.add(pname);
					}
				}
				if (plist.length() > 0)
				{
					String msg = 
						LoadResourceBundle.sprintf(
							CAPEdit.instance().compeditDescriptions.getString(
								"ComputationsEditPanel.AlgoButtonPrompt"),
							plist.toString());
					int ok = JOptionPane.showConfirmDialog(this, 
						AsciiUtil.wrapString(msg, 60), "Param Remove",
							JOptionPane.YES_NO_OPTION);
					if (ok != JOptionPane.YES_OPTION)
						return;

					for(String pname : parms2delete)
						compParmTableModel.removeParm(pname);
	
					for(String pname : props2delete)
						propCopy.remove(pname);
				}
			}

			currentAlgorithm = dlg.selectedAlgo;
			algorithmField.setText(currentAlgorithm.getName());
			ArrayList<DbCompParm> newParms = new ArrayList<DbCompParm>();
			for (Iterator<DbAlgoParm> it = currentAlgorithm.getParms();
				it.hasNext(); )
			{
				DbAlgoParm dap = it.next();
				String name = dap.getRoleName();
				String algoParmType = dap.getParmType();
				DbCompParm dcp = compParmTableModel.findByName(name, -1);
				if (dcp == null)
				{
					dcp=new DbCompParm(name, Constants.undefinedId, "", "", 0);
					newParms.add(dcp);
				}
				dcp.setAlgoParmType(algoParmType);
			}

			for(DbCompParm dcp : newParms)
				compParmTableModel.add(dcp);
			for(Enumeration pnenum = currentAlgorithm.getPropertyNames();
				pnenum.hasMoreElements(); )
			{
				String pname = (String)pnenum.nextElement();
				String value = currentAlgorithm.getProperty(pname);
				if (TextUtil.endsWithIgnoreCase(pname, "_EU")
				 || TextUtil.endsWithIgnoreCase(pname, "_MISSING"))
				{
					if (getHiddenProperty(pname) == null)
						setHiddenProperty(pname, value);
				}
				else
				{
					if (propCopy.getProperty(pname) == null)
						propCopy.setProperty(pname, value);
				}
			}
			propertiesPanel.setProperties(propCopy);
			setPropertiesPanelOwner();

			propertiesPanel.redrawTable();
		}
	}
	
	private void setPropertiesPanelOwner()
	{
		if (currentAlgorithm == null)
			return;
		try
		{
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			String clsName = currentAlgorithm.getExecClass();
			Logger.instance().debug3("Instantiating new algo exec '" + clsName + "'");
			Class<?> cls = cl.loadClass(clsName);
			DbAlgorithmExecutive executive = (DbAlgorithmExecutive)cls.newInstance();
			if (executive instanceof AW_AlgorithmBase)
			{
				((AW_AlgorithmBase)executive).initForGUI();
				propertiesPanel.setPropertiesOwner((AW_AlgorithmBase)executive);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	private void selectGroupPressed()
	{
		TsGroupSelectDialog dlg = new TsGroupSelectDialog(
			CAPEdit.instance().getFrame(), true);
		CAPEdit.instance().getFrame().launchDialog(dlg);
		TsGroup grp = dlg.getSelectedGroup();
		if (grp != null)
		{
			groupField.setText(grp.getGroupName());
			selectedGroup = grp;
		}
		else
		{
			groupField.setText("");
			selectedGroup = null;
		}
	}
	
	private void deleteParamButtonPressed()
	{
		int r = compParmTable.getSelectedRow();
		if (r == -1)
		{
			showError(CAPEdit.instance().compeditDescriptions
		    		.getString("ComputationsEditPanel.DeleteError1"));
			return;
		}
		DbCompParm dcp = (DbCompParm)compParmTableModel.getRowObject(r);
		int ok = JOptionPane.showConfirmDialog(this,
				LoadResourceBundle.sprintf(CAPEdit.instance().compeditDescriptions
	    		.getString("ComputationsEditPanel.DeleteError2") 
			,dcp.getRoleName()));
		if (ok == JOptionPane.YES_OPTION)
			compParmTableModel.deleteAt(r);
	}

	private void editParamButtonPressed()
	{
		int r = compParmTable.getSelectedRow();
		if (r == -1)
		{
			showError(CAPEdit.instance().compeditDescriptions
		    		.getString("ComputationsEditPanel.EditError"));
			return;
		}
		DbCompParm dcp = (DbCompParm)compParmTableModel.getRowObject(r);

		CompParmDialog compParmDialog = 
			new CompParmDialog(dcp.isInput(), siteSelectPanel);
		compParmDialog.setInfo(this, r, nameField.getText().trim(), dcp);
		CAPEdit.instance().getFrame().launchDialog(compParmDialog);
//System.out.println("after dlg close, tabsel='" + dcp.getTableSelector() + "', locspec='"
//+dcp.getLocSpec() + "', paramspec='" + dcp.getParamSpec() + "', ver='" + dcp.getVersion() + "'");
		if (compParmDialog.okPressed)
		{
			if (!DbKey.isNull(dcp.getSiteDataTypeId()))
			{
				int n = compParmTableModel.getRowCount();
				for(int i=0; i<n; i++)
				{
					DbCompParm otherParm = 
						(DbCompParm)compParmTableModel.getRowObject(i);
					if (otherParm != dcp
					 && DbKey.isNull(otherParm.getSiteDataTypeId()))
					{
						otherParm.setInterval(dcp.getInterval());
					}
				}
			}
			compParmTableModel.fireTableDataChanged();
		}
	}
	
	protected JPanel getButtonPanel() 
	{
		JPanel buttonPanel = new JPanel();
		GridBagConstraints closeConstraints = new GridBagConstraints();
		closeConstraints.gridx = 1;
		closeConstraints.insets = new java.awt.Insets(0,6,6,0);
		closeConstraints.weightx = 1.0D;
		closeConstraints.anchor = java.awt.GridBagConstraints.WEST;
		closeConstraints.gridy = 0;
		GridBagConstraints commitConstraints = new GridBagConstraints();
		commitConstraints.gridx = 0;
		commitConstraints.insets = new java.awt.Insets(0,6,6,0);
		commitConstraints.gridy = 0;
		GridBagConstraints runCompConstraints = new GridBagConstraints();
		runCompConstraints.gridx = 2;
		runCompConstraints.insets = new java.awt.Insets(0,0,6,6);
		runCompConstraints.anchor = java.awt.GridBagConstraints.EAST;
		runCompConstraints.gridy = 0;
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridBagLayout());
		buttonPanel.setPreferredSize(new java.awt.Dimension(430,37));
		buttonPanel.add(getCommitButton(), commitConstraints);
		buttonPanel.add(getCloseButton(), closeConstraints);
		buttonPanel.add(getRunCompButton(), runCompConstraints);
		
		return buttonPanel;
	}
	
	private JButton getRunCompButton() 
	{
		JButton runCompButton = new JButton();
		runCompButton.setText(CAPEdit.instance().compeditDescriptions
	    		.getString("ComputationsEditPanel.RunCompButton"));
		runCompButton.setName("runCompButton");
		runCompButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e) {
					doRunComp();
				}
			});
		
		return runCompButton;
	}
	
	/**
	 * By pressing this button the Run Computations GUI will
	 * come up as a separate frame.
	 */
	private void doRunComp()
	{
		if (runCompGUIUp == false)
		{
			if (rcframe == null)
				rcframe = new CompRunGuiFrame(false);
			rcframe.setVisible(true);
			rcframe.setDb(CAPEdit.instance().theDb);
			rcframe.setParent(this);
			rcframe.setTitle(CAPEdit.instance().compeditDescriptions
				.getString("ComputationsEditPanel.RunCompTitle") + nameField.getText());
			runCompGUIUp = true;	
		}
		else
		{
			if (rcframe != null)
			{
				rcframe.setVisible(true);
				rcframe.toFront();
				rcframe.setFocusable(true);	
			}
		}
	}

	public void setRunCompGUIUp(boolean flag)
	{
		this.runCompGUIUp = flag;
	}
	
	/**
	 * Used by the comp-parm dialog
	 * @return true if a group has been assigned to this computation
	 */
	public boolean hasGroupInput()
	{
		return groupField.getText().trim().length() > 0;
	}
	/**
	 * &sed by the comp-parm dialog
	 * @param name the name of the property
	 * @return the property value from the properties panel, or null if no match
	 */
	public String getProperty(String name)
	{
		return propertiesPanel.getProperty(name);
	}

	public DbCompAlgorithm getAlgorithm()
	{
		return currentAlgorithm;
	}
	
	private void sinceMethodComboChanged()
	{
		sinceContentPanel.removeAll();
		switch(sinceMethodCombo.getSelectedIndex())
		{
		case 0: // no limit -- leave empty
			break;
		case 1: // now -
			sinceContentPanel.add(sinceNowMinusPanel);
			break;
		case 2: // Calendar
			sinceContentPanel.add(sinceDateTimeCal);
			break;
		}
		// Force panel to repaint itself.
		sinceContentPanel.setVisible(false);
		sinceContentPanel.setVisible(true);
	}
	private void untilMethodComboChanged()
	{
		untilContentPanel.removeAll();
		switch(untilMethodCombo.getSelectedIndex())
		{
		case 0: // no limit -- leave empty
			break;
		case 1: // now
			untilContentPanel.add(untilNowExpl);
			break;
		case 2: // now +
			untilContentPanel.add(untilNowPlusPanel);
			break;
		case 3: // Calendar
			untilContentPanel.add(untilDateTimeCal);
			break;
		}
		// Force panel to repaint itself.
		untilContentPanel.setVisible(false);
		untilContentPanel.setVisible(true);
	}
}

class CompParmTableModel 
	extends AbstractTableModel implements SortingListTableModel 
{
	ArrayList<DbCompParm> myvector = new ArrayList<DbCompParm>();

	ResourceBundle rb = CAPEdit.instance().compeditDescriptions;
	String columnNames[];
	int columnWidths[];
	ComputationsEditPanel parent = null;

	CompParmTableModel(ComputationsEditPanel parent)
	{
		this.parent = parent;
		TimeSeriesDb tsdb = CAPEdit.instance().getTimeSeriesDb();
		String tabSelLab = tsdb.getTableSelectorLabel();
		if (tabSelLab.length() > 10)
			tabSelLab = tabSelLab.substring(0, 10);
		if (tsdb.isHdb())
		{
			columnNames = 
				new String[]
				{ 
					rb.getString("ComputationsEditPanel.TableColumnRole"), 
					rb.getString("ComputationsEditPanel.TableColumnSite"), 
					rb.getString("ComputationsEditPanel.TableColumnDatatype"), 
					rb.getString("ComputationsEditPanel.TableColumnInterval"), 
					"Real/Model",
					"Model ID",
					rb.getString("ComputationsEditPanel.TableColumnDeltaT")
				};
				columnWidths = new int[]{ 15, 15, 15, 15, 15, 15, 10 };
		}
		else
		{
			columnNames = 
				new String[]
				{ 
					rb.getString("ComputationsEditPanel.TableColumnRole"),
					rb.getString("ComputationsEditPanel.TableColumnLocation"),
					rb.getString("ComputationsEditPanel.TableColumnParam"),
					rb.getString("ComputationsEditPanel.TableColumnParamType"),
					rb.getString("ComputationsEditPanel.TableColumnInterval"), 
					rb.getString("ComputationsEditPanel.TableColumnDuration"), 
					rb.getString("ComputationsEditPanel.TableColumnVersion")
				};
			columnWidths = new int[]{ 15, 15, 15, 15, 15, 15, 10 };
		}
	}

	public void sortByColumn(int c) 
	{
		Collections.sort(myvector, new ComputationsEditComparator(c, this));
		fireTableDataChanged();
	}

	public Object getRowObject(int arg0) {
		return myvector.get(arg0);
	}

	public int getRowCount() {
		return myvector.size();
	}

	public int getColumnCount() 
	{
		return columnWidths.length;
	}

	public void deleteAt(int r)
	{
		myvector.remove(r);
		fireTableDataChanged();
	}

	public void removeParm(String pname)
	{
		for(int i=0; i<myvector.size(); i++)
			if (myvector.get(i).getRoleName().equals(pname))
			{
				deleteAt(i);
				return;
			}
	}

	public void fill(DbComputation dc)
	{
		for(Iterator<DbCompParm> pit = dc.getParms(); pit.hasNext(); )
		{
			DbCompParm dcp = pit.next();
			myvector.add(dcp);
		}
		Collections.sort(myvector, new ComputationsEditComparator(-1, this));
		fireTableDataChanged();
	}

	public void add(DbCompParm dcp)
	{
		myvector.add(dcp);
		fireTableDataChanged();
	}

	public String getColumnName(int col) 
	{
		return columnNames[col];
	}

	public Object getValueAt(int rowIndex, int columnIndex) 
	{
		if (myvector.get(rowIndex) != null)
			return getNlColumn(myvector.get(rowIndex), columnIndex);
		else
			return "";
	}

	public String getNlColumn(DbCompParm compParm, int columnIndex) 
	{
		TimeSeriesDb tsdb = CAPEdit.instance().theDb;

		switch (columnIndex) 
		{
		case 0:
			return compParm.getRoleName();
		case 1:
		  {
			SiteName sn = compParm.getSiteName();
//if(tsdb.isCwms())System.out.println("locspec='" + compParm.getLocSpec() + "'");
			return sn != null ? sn.getNameValue() :
				parent.hasGroupInput() ? 
				(tsdb.isCwms() && compParm.getLocSpec().length() > 0 ? compParm.getLocSpec() : "<var>") : "";
		  }
		case 2:
		  {
			DataType dt = compParm.getDataType();
			if (dt == null)
				return parent.hasGroupInput() ? 
					(tsdb.isCwms() && compParm.getParamSpec().length() > 0 ? compParm.getParamSpec() : "<var>") : "";
			else
				return dt.getCode();
		  }
		case 3:
			if (!tsdb.isCwms())
			{
				String s = compParm.getInterval();
				if (s == null || s.trim().length() == 0)
					return parent.hasGroupInput() ? "<var>" : "";
				return s;
			}
			else // CWMS - return 6-part time series Identifier
			{
				String s = compParm.getParamType();
				if (s == null || s.trim().length() == 0)
					return parent.hasGroupInput() ? "<var>" : "";
				return s;
			}
		case 4:
			if (!tsdb.isCwms())
			{
				String s = compParm.getTableSelector();
				if (s == null || s.trim().length() == 0)
					return parent.hasGroupInput() ? "<var>" : "";
				return s;
			}
			else
			{
				String s = compParm.getInterval();
				if (s == null || s.trim().length() == 0)
					return parent.hasGroupInput() ? "<var>" : "";
				return s;
			}
		case 5:
			if (tsdb.isCwms())
			{
				String s = compParm.getDuration();
				if (s == null || s.trim().length() == 0)
					return parent.hasGroupInput() ? "<var>" : "";
				return s;
			}
			else if (tsdb.isHdb())
			{
				int modelId = compParm.getModelId();
				if (modelId != Constants.undefinedIntKey)
					return "" + compParm.getModelId();
				else 
					return parent.hasGroupInput() ? "<var>" : "N/A";
			}
		case 6:
			if (tsdb.isHdb())
			{
				String s = "" + compParm.getDeltaT();
				if (compParm.getDeltaTUnits() != null)
					s = s + " " + compParm.getDeltaTUnits();
				return s;
			}
			else // CWMS
			{
				String s = compParm.getVersion();
				if (s == null || s.trim().length() == 0)
					return parent.hasGroupInput() ? "<var>" : "";
				return s;
			}
		default:
			return "";
		}
	}

	void saveTo(DbComputation dc)
	{
		dc.clearParms();
		for(DbCompParm dcp : myvector)
			dc.addParm(dcp);
	}

	DbCompParm findByName(String roleName, int otherThan)
	{
		for(int r=0; r < myvector.size(); r++)
		{
			DbCompParm dcp = myvector.get(r);
			if (roleName.equalsIgnoreCase(dcp.getRoleName())
			 && r != otherThan)
				return dcp;
		}
		return null;
	}
}

class ComputationsEditComparator 
	implements Comparator<DbCompParm>
{
	int column;
	CompParmTableModel model;

	public ComputationsEditComparator(int column, CompParmTableModel model)
	{
		this.column = column;
		this.model = model;
	}

	/**
	 * Compare the eqMod names of the specified type.
	 */
	public int compare(DbCompParm ds1, DbCompParm ds2)
	{
		if (ds1 == ds2)
			return 0;

		if (column == -1) // Initial sorting
		{
			if (ds1.isInput() && ds2.isOutput())
				return -1;
			else if (ds1.isOutput() && ds2.isInput())
				return 1;
			String s1 = model.getNlColumn(ds1, 0);
			String s2 = model.getNlColumn(ds2, 0);
			return s1.compareToIgnoreCase(s2);
		}

		String s1 = model.getNlColumn(ds1, column);
		String s2 = model.getNlColumn(ds2, column);

		return s1.compareToIgnoreCase(s2);
	}

	public boolean equals(Object ob) {
		return false;
	}
}
