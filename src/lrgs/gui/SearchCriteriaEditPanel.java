/*
*  $Id$
*  
*  Open Source Software written by Cove Software, LLC
*  
*  $Log$
*  Revision 1.1  2013/02/28 16:44:26  mmaloney
*  New SearchCriteriaEditPanel implementation.
*
*/
package lrgs.gui;

import ilex.gui.DateTimeCalendar;
import ilex.util.AsciiUtil;
import ilex.util.EnvExpander;
import ilex.util.IDateFormat;
import ilex.util.LoadResourceBundle;
import ilex.util.Logger;
import ilex.util.StringPair;
import ilex.util.TextUtil;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.TimeZone;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.table.AbstractTableModel;

import decodes.db.Database;
import decodes.db.NetworkList;
import decodes.db.Platform;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.gui.TopFrame;
import decodes.util.DecodesSettings;
import decodes.util.Pdt;
import decodes.util.PdtEntry;

import lrgs.common.DcpAddress;
import lrgs.common.DcpMsgFlag;
import lrgs.common.SearchCriteria;

@SuppressWarnings("serial")
public class SearchCriteriaEditPanel
	extends JPanel
{
	private SearchCriteria origSearchCrit = null;
	private PlatSelectModel platSelectModel = new PlatSelectModel();
	private SortingListTable platSelectTable = new SortingListTable(platSelectModel,
		new int[] { 30, 70 });
	private JComboBox sinceMethodCombo = null;
	private JPanel sinceContentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
	
	private JPanel sinceNowMinusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
	private JComboBox sinceNowMinusCombo = new JComboBox(
		new String[] { "30 minutes", "1 hour", "2 hours", "1 day" });
	
	private DateTimeCalendar sinceDateTime = new DateTimeCalendar("(UTC)",
		new Date(),"dd/MMM/yyyy", "UTC");
	private JTextField sinceFileTimeField = new JTextField(12);
	
	private JPanel sinceFileTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

	private JComboBox untilMethodCombo = null;
	private JPanel untilContentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
	private JPanel untilNowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
	private JPanel untilNowMinusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
	private JComboBox untilNowMinusCombo = new JComboBox(
		new String[] { "30 minutes", "1 hour", "2 hours" });

//	private JPanel untilCalendarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
	private DateTimeCalendar untilDateTime = new DateTimeCalendar("(UTC)",
		new Date(),"dd/MMM/yyyy", "UTC");;
	private JPanel untilRealTimePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
	private JCheckBox thirtySecDelayCheck = null;
	private JComboBox applyToCombo = null;
	private JCheckBox ascendingOrderCheck = null;
	
	private JCheckBox goesSelfTimedCheck = null;
	private JCheckBox goesRandomCheck = null;
	private JCheckBox goesQualityCheck = null;
	private JCheckBox goesSpacecraftCheck = null;
	private JComboBox goesSpacecraftCombo = null;
	
	private JCheckBox iridiumCheck = new JCheckBox("Iridium");
	private JCheckBox networkDcpCheck = null;
	private JCheckBox modemDcpCheck = null;
	private JCheckBox parityCheck = null;
	private JComboBox parityCheckCombo = null;
	private JFileChooser nlFileChooser = new JFileChooser();
	private PdtSelectDialog pdtSelectDialog = null;
	private TopFrame parent = null;
	private JFileChooser sinceFileChooser = new JFileChooser();
	private JPanel dateTimePanel = new JPanel(new GridBagLayout());
	
	private static ResourceBundle scLabels = null;
	private static ResourceBundle genericLabels = null;
	private boolean allowRealTime = false;
	private JButton selectFromPdtButton = null;
	JButton addGoesChannelButton = null;
	JButton sinceFileTimeBrowsButton = null;
	JButton clearAllTypesButton = null;
	JButton selectAllTypesButton = null;
	
	public SearchCriteriaEditPanel()
	{
		super(new GridBagLayout());
		if (scLabels == null)
		{
			DecodesSettings settings = DecodesSettings.instance();
			scLabels = LoadResourceBundle.getLabelDescriptions(
				"decodes/resources/msgaccess", settings.language);
			genericLabels = LoadResourceBundle.getLabelDescriptions(
				"decodes/resources/generic", settings.language);
		}

		guiInit();
		setToDefaults();
	}

	public void setSearchCrit(SearchCriteria searchCrit)
	{
		this.origSearchCrit = searchCrit;
		fillFields();
	}
	
	public void setTopFrame(TopFrame parent)
	{
		Logger.instance().debug3("SearchCriteriaEditPanel.setTopFrame("
			+ (parent == null ? "NULL" : "") + ")");

		this.parent = parent;
	}
	
	/**
	 * @return true if anything has changed since last call to setSearchCrit.
	 */
	public boolean hasChanged()
	{
		if (origSearchCrit == null)
			return false;
		SearchCriteria test = new SearchCriteria();
		fillSearchCrit(test);
		return !test.equals(origSearchCrit);
	}
	
	/**
	 * Construct all the GUI elements in the panel.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void guiInit()
	{
		// Construct the controls with international strings
		sinceMethodCombo = new JComboBox(
			new String[] 
			{
				scLabels.getString("SearchCriteriaEditor.now")+" -",
				scLabels.getString("SearchCriteriaEditor.calendar"),
				scLabels.getString("SearchCriteriaEditor.filetime")
			});
		untilMethodCombo = new JComboBox(
			new String[] 
			{
				scLabels.getString("SearchCriteriaEditor.now"),
				scLabels.getString("SearchCriteriaEditor.now")+" -",
				scLabels.getString("SearchCriteriaEditor.calendar"),
				scLabels.getString("SearchCriteriaEditor.realTime")
			});
		thirtySecDelayCheck = new JCheckBox("30 sec delay to avoid duplicates");
		applyToCombo = new JComboBox(
			new String[] 
			{
				scLabels.getString("SearchCriteriaEditor.localRecvTime"),
				scLabels.getString("SearchCriteriaEditor.platformXmitTime"),
				genericLabels.getString("both")
			});
		ascendingOrderCheck = new JCheckBox(
			scLabels.getString("SearchCriteriaEditor.ascTimeOrder"));
		goesSelfTimedCheck = new JCheckBox(
			scLabels.getString("SearchCriteriaEditor.goesSelfTimed"));
		goesRandomCheck = new JCheckBox(
			scLabels.getString("SearchCriteriaEditor.goesRandom"));
		goesQualityCheck = new JCheckBox(
			scLabels.getString("SearchCriteriaEditor.qualityNotifications"));
		
		goesSpacecraftCheck = new JCheckBox("GOES " +
			scLabels.getString("SearchCriteriaEditor.spacecraft"));
		goesSpacecraftCombo = new JComboBox(
			new String[] 
			{ 
				scLabels.getString("SearchCriteriaEditor.east"),
				scLabels.getString("SearchCriteriaEditor.west"),
			});
		networkDcpCheck = new JCheckBox(
			scLabels.getString("SearchCriteriaEditor.networkDCP"));
		modemDcpCheck = new JCheckBox("Modem DCP");
		parityCheck = new JCheckBox("Parity: ");
		parityCheckCombo = new JComboBox(
			new String[] 
			{
				genericLabels.getString("good"),
				genericLabels.getString("bad"),
			});

		
		// Construct main panel
		JPanel platformSelectPanel = new JPanel(new GridBagLayout());
		JPanel platformTypesPanel = new JPanel();
		this.add(dateTimePanel,
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(4, 4, 4, 4), 0, 0));
		this.add(platformSelectPanel,
			new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, 
				new Insets(4, 4, 4, 4), 0, 0));
		this.add(platformTypesPanel,
			new GridBagConstraints(1, 0, 1, 2, 0.0, 0.0,
				GridBagConstraints.NORTH, GridBagConstraints.BOTH, 
				new Insets(4, 4, 4, 4), 0, 0));

		// Platform selection panel in lower left
		platformSelectPanel.setBorder(
			BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(EtchedBorder.RAISED), 
				scLabels.getString("SearchCriteriaEditor.platformSelection")));

		platSelectTable.getTableHeader().setReorderingAllowed(false);
		JScrollPane platformSelectScrollPane = new JScrollPane();
		platformSelectScrollPane.getViewport().add(platSelectTable, null);
		platformSelectPanel.add(platformSelectScrollPane,
			new GridBagConstraints(0, 0, 3, 5, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, 
				new Insets(4, 4, 4, 4), 0, 0));
		
		JButton addIdButton = new JButton(
			scLabels.getString("SearchCriteriaEditor.enterPlatformID"));
		addIdButton.addActionListener(
			new java.awt.event.ActionListener() 
			{
				public void actionPerformed(ActionEvent e) 
				{
					addIdButtonPressed();
				}
			});
		platformSelectPanel.add(addIdButton,
			new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(0, 4, 2, 4), 0, 0));
		
		JButton addNameButton = new JButton(
			scLabels.getString("SearchCriteriaEditor.enterPlatformName"));
		addNameButton.addActionListener(
			new java.awt.event.ActionListener() 
			{
				public void actionPerformed(ActionEvent e) 
				{
					addNameButtonPressed();
				}
			});
		platformSelectPanel.add(addNameButton,
			new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 4, 2, 4), 0, 0));

		selectFromPdtButton = new JButton(
			scLabels.getString("SearchCriteriaEditor.selectFromPDT"));
		selectFromPdtButton.addActionListener(
			new java.awt.event.ActionListener() 
			{
				public void actionPerformed(ActionEvent e) 
				{
					selectFromPdtButtonPressed();
				}
			});
		platformSelectPanel.add(selectFromPdtButton,
			new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 4, 2, 4), 0, 0));

		JButton addDbListButton = new JButton(
			scLabels.getString("SearchCriteriaEditor.addNetworkList"));
		addDbListButton.addActionListener(
			new java.awt.event.ActionListener() 
			{
				public void actionPerformed(ActionEvent e) 
				{
					addDbListButtonPressed(null);
				}
			});
		platformSelectPanel.add(addDbListButton,
			new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 4, 2, 4), 0, 0));

//		JButton addFileListButton = new JButton("Add File Network List");
//		addFileListButton.addActionListener(
//			new java.awt.event.ActionListener() 
//			{
//				public void actionPerformed(ActionEvent e) 
//				{
//					addFileListButtonPressed();
//				}
//			});
//		platformSelectPanel.add(addFileListButton,
//			new GridBagConstraints(3, 4, 1, 1, 0.0, 0.0,
//				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
//				new Insets(2, 4, 2, 4), 0, 0));
	
		addGoesChannelButton = new JButton(
			scLabels.getString("SearchCriteriaEditor.addGoesChannel"));
		addGoesChannelButton.addActionListener(
			new java.awt.event.ActionListener() 
			{
				public void actionPerformed(ActionEvent e) 
				{
					addGoesChannelButtonPressed();
				}
			});
		platformSelectPanel.add(addGoesChannelButton,
			new GridBagConstraints(3, 4, 1, 1, 0.0, 1.0,
				GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 4, 0, 4), 0, 0));
		
		JButton editSelectionButton = new JButton(genericLabels.getString("edit"));
		editSelectionButton.addActionListener(
			new java.awt.event.ActionListener() 
			{
				public void actionPerformed(ActionEvent e) 
				{
					editSelectionButtonPressed();
				}
			});
		platformSelectPanel.add(editSelectionButton,
			new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(4, 4, 4, 4), 20, 0));
	
		JButton removeSelectionButton = new JButton(genericLabels.getString("remove"));
		removeSelectionButton.addActionListener(
			new java.awt.event.ActionListener() 
			{
				public void actionPerformed(ActionEvent e) 
				{
					removeSelectionButtonPressed();
				}
			});
		platformSelectPanel.add(removeSelectionButton,
			new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(4, 4, 4, 4), 20, 0));
		
		JButton clearSelectionButton = new JButton(genericLabels.getString("clear"));
		clearSelectionButton.addActionListener(
			new java.awt.event.ActionListener() 
			{
				public void actionPerformed(ActionEvent e) 
				{
					clearSelectionButtonPressed();
				}
			});
		platformSelectPanel.add(clearSelectionButton,
			new GridBagConstraints(2, 5, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(4, 4, 4, 4), 20, 0));
	
		// DateTime Panel in upper left
		dateTimePanel.setBorder(
			BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(EtchedBorder.RAISED),
				genericLabels.getString("datetime")));
			
		dateTimePanel.add(
			new JLabel(scLabels.getString("SearchCriteriaEditor.since") + ":"),
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 5, 2, 2), 0, 0));
		sinceMethodCombo.setSelectedIndex(0);
		sinceMethodCombo.addActionListener(
			new java.awt.event.ActionListener() 
			{
				public void actionPerformed(ActionEvent e) 
				{
					sinceMethodComboChanged();
				}
			});
		dateTimePanel.add(sinceMethodCombo,
			new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 4), 0, 0));
		sinceContentPanel.add(sinceNowMinusPanel);
		
		dateTimePanel.add(sinceContentPanel,
			new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 4, 2, 4), 200, 0));
		
		sinceNowMinusCombo.setEditable(true);
		sinceNowMinusPanel.add(sinceNowMinusCombo);

		sinceFileTimePanel.add(sinceFileTimeField);
		sinceFileTimeBrowsButton = 
			new JButton("Browse");
		sinceFileTimeBrowsButton.addActionListener(
			new java.awt.event.ActionListener() 
			{
				public void actionPerformed(ActionEvent e) 
				{
					sinceFileTimeBrowseButtonPressed();
				}
			});
		sinceFileTimePanel.add(sinceFileTimeBrowsButton);
		
		// Default since calendar to midnight yesterday UTC.
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("UTC"));
		cal.setTime(new Date());
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.add(Calendar.DAY_OF_MONTH, -1);
		sinceDateTime.setDate(cal.getTime());

		dateTimePanel.add(
			new JLabel(scLabels.getString("SearchCriteriaEditor.until") + ":"),
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 5, 2, 2), 0, 0));
		untilMethodCombo.setSelectedIndex(0);
		untilMethodCombo.addActionListener(
			new java.awt.event.ActionListener() 
			{
				public void actionPerformed(ActionEvent e) 
				{
					untilMethodComboChanged();
				}
			});
		dateTimePanel.add(untilMethodCombo,
			new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 4), 0, 0));
		untilNowPanel.add(new JLabel(
			scLabels.getString("SearchCriteriaEditor.untilNowComment")));
		untilContentPanel.add(untilNowPanel);
		dateTimePanel.add(untilContentPanel,
			new GridBagConstraints(2, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 4, 2, 4), 200, 0));
		
		untilRealTimePanel.add(thirtySecDelayCheck);
		untilNowMinusCombo.setEditable(true);
		untilNowMinusPanel.add(untilNowMinusCombo);
		
		dateTimePanel.add(new JLabel(
			scLabels.getString("SearchCriteriaEditor.applyTo")),
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 5, 2, 2), 0, 0));
		dateTimePanel.add(applyToCombo,
			new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 4), 0, 0));
		
		dateTimePanel.add(ascendingOrderCheck,
			new GridBagConstraints(2, 2, 2, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 4, 2, 4), 0, 0));

		// Platform Types panel on the Right Side
		platformTypesPanel.setBorder(
			BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(EtchedBorder.RAISED), 
				scLabels.getString("SearchCriteriaEditor.platMsgTypes")));
		platformTypesPanel.setLayout(new GridBagLayout());
		platformTypesPanel.add(goesSelfTimedCheck,
			new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 4, 2, 4), 0, 0));
		platformTypesPanel.add(goesRandomCheck,
			new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 4, 2, 4), 0, 0));
		platformTypesPanel.add(goesQualityCheck,
			new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 4, 2, 4), 0, 0));
		platformTypesPanel.add(goesSpacecraftCheck,
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 4, 2, 1), 0, 0));
		goesSpacecraftCheck.addActionListener(
			new java.awt.event.ActionListener() 
			{
				public void actionPerformed(ActionEvent e) 
				{
					goesSpacecraftChecked();
				}
			});
		platformTypesPanel.add(goesSpacecraftCombo,
			new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 1, 2, 4), 0, 0));
		platformTypesPanel.add(iridiumCheck,
			new GridBagConstraints(0, 4, 2, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 4, 2, 4), 0, 0));
		platformTypesPanel.add(networkDcpCheck,
			new GridBagConstraints(0, 5, 2, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 4, 2, 4), 0, 0));
		platformTypesPanel.add(modemDcpCheck,
			new GridBagConstraints(0, 6, 2, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 4, 2, 4), 0, 0));
		platformTypesPanel.add(parityCheck,
			new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 4, 2, 1), 0, 0));
		parityCheck.addActionListener(
			new java.awt.event.ActionListener() 
			{
				public void actionPerformed(ActionEvent e) 
				{
					parityChecked();
				}
			});
		platformTypesPanel.add(parityCheckCombo, 
			new GridBagConstraints(1, 7, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 1, 2, 4), 0, 0));
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		clearAllTypesButton = new JButton(
			scLabels.getString("SearchCriteriaEditor.clearAll"));
		clearAllTypesButton.addActionListener(
			new java.awt.event.ActionListener() 
			{
				public void actionPerformed(ActionEvent e) 
				{
					clearAllTypesButtonPressed();
				}
			});
		p.add(clearAllTypesButton);
		selectAllTypesButton = new JButton(
			scLabels.getString("SearchCriteriaEditor.selectAll"));
		selectAllTypesButton.addActionListener(
			new java.awt.event.ActionListener() 
			{
				public void actionPerformed(ActionEvent e) 
				{
					selectAllTypesButtonPressed();
				}
			});
		p.add(selectAllTypesButton);
		platformTypesPanel.add(p,
			new GridBagConstraints(0, 8, 2, 1, 0.0, 1.0,
				GridBagConstraints.NORTH, GridBagConstraints.NONE, 
				new Insets(2, 4, 2, 4), 0, 0));

//		// Kludge to set the preferred height of the selection panel to the
//		// same height as the message types panel.
//		d = platformTypesPanel.getPreferredSize();
//		int h = d.height;
//		d = platformSelectPanel.getPreferredSize();
//		d.height = h;
//		platformSelectPanel.setPreferredSize(d);
		
		String dir = EnvExpander.expand("$DECODES_INSTALL_DIR/netlist");
		File nldir = new File(dir);
		if(!nldir.isDirectory())
			nldir = new File(EnvExpander.expand("$LRGSHOME/netlist"));
		if (nldir.isDirectory())
			nlFileChooser.setCurrentDirectory(nldir);
		
		sinceFileChooser.setCurrentDirectory(new File(EnvExpander.expand("$HOME")));
	}

	protected void parityChecked()
	{
		parityCheckCombo.setEnabled(parityCheck.isSelected());
	}

	protected void goesSpacecraftChecked()
	{
		goesSpacecraftCombo.setEnabled(goesSpacecraftCheck.isSelected());
	}

	protected void addGoesChannelButtonPressed()
	{
		String s = JOptionPane.showInputDialog(this,
			"Enter GOES Channel:", "GOES Channel Entry",
			JOptionPane.PLAIN_MESSAGE);
		if (s == null)
			return;
		s = s.trim();
		if (s.length() == 0)
			return;
		try { Integer.parseInt(s); }
		catch(Exception ex)
		{
			showError("Invalid GOES Channel '" + s + "'. Enter integer channel number.");
			return;
		}
		platSelectModel.add(PlatSelectModel.GoesChannelLabel, s);
	}

	protected void sinceFileTimeBrowseButtonPressed()
	{
		sinceFileChooser.showOpenDialog(this);
		File file = sinceFileChooser.getSelectedFile();
		if (file == null)
			return;
		sinceFileTimeField.setText(file.getPath());
	}

	protected void sinceMethodComboChanged()
	{

		// There should be only one component in sinceContentPanel. Remove it.
		sinceContentPanel.remove(0);
		JPanel p = null;
		switch(sinceMethodCombo.getSelectedIndex())
		{
		//new String[] {"Now -", "Calendar", "File Time" });
		case 0:
			p = sinceNowMinusPanel;
			break;
		case 1:
			p = sinceDateTime;
			break;
		case 2:
			p = sinceFileTimePanel;
			break;
		}
		
		sinceContentPanel.add(p);
		
		SwingUtilities.invokeLater(
		new Runnable()
		{
			public void run()
			{
				sinceMethodCombo.revalidate();
				sinceMethodCombo.repaint();
				sinceContentPanel.revalidate();
				sinceContentPanel.repaint();
			}
		});
	}

	protected void untilMethodComboChanged()
	{
		// There should be only one component in untilContentPanel. Remove it.
		untilContentPanel.remove(0);
		JPanel p = null;
		switch(untilMethodCombo.getSelectedIndex())
		{
		// new String[] {"Now", "Now -", "Calendar", "Real Time" });
		case 0:
			p = untilNowPanel;
			break;
		case 1:
			p = untilNowMinusPanel;
			break;
		case 2:
			p = untilDateTime;
			break;
		case 3:
			p = untilRealTimePanel;
			break;
		}
		untilContentPanel.add(p);
		
		SwingUtilities.invokeLater(
		new Runnable()
		{
			public void run()
			{
				untilMethodCombo.revalidate();
				untilMethodCombo.repaint();
				untilContentPanel.revalidate();
				untilContentPanel.repaint();
			}
		});
	}

	protected void clearSelectionButtonPressed()
	{
		if (platSelectModel.getRowCount() > 3)
		{
			int r = JOptionPane.showConfirmDialog(this,
				"Remove these " + platSelectModel.getRowCount()
				+ " items from the search criteria?");
			if (r != JOptionPane.OK_OPTION)
				return;
		}
		platSelectModel.clear();
	}

	protected void selectAllTypesButtonPressed()
	{
		goesSelfTimedCheck.setSelected(true);
		goesRandomCheck.setSelected(true);
		goesQualityCheck.setSelected(true);
		goesSpacecraftCheck.setSelected(true);
		goesSpacecraftCombo.setEnabled(true);
		iridiumCheck.setSelected(true);
		networkDcpCheck.setSelected(true);
		modemDcpCheck.setSelected(true);
		parityCheck.setSelected(true);
		parityCheckCombo.setEnabled(true);
	}

	protected void clearAllTypesButtonPressed()
	{
		goesSelfTimedCheck.setSelected(false);
		goesRandomCheck.setSelected(false);
		goesQualityCheck.setSelected(false);
		goesSpacecraftCheck.setSelected(false);
		goesSpacecraftCombo.setEnabled(false);
		iridiumCheck.setSelected(false);
		networkDcpCheck.setSelected(false);
		modemDcpCheck.setSelected(false);
		parityCheck.setSelected(false);
		parityCheckCombo.setEnabled(false);
	}

	protected void editSelectionButtonPressed()
	{
		int row = platSelectTable.getSelectedRow();
		if (row == -1)
			return;
		StringPair sp = platSelectModel.getEntryAt(row);
		if (sp == null)
			return;
		if (sp.first == PlatSelectModel.PlatformIdLabel)
		{
			DcpAddress addr = new DcpAddress(sp.second);
			
			String s = JOptionPane.showInputDialog(this, 
				"Enter Platform ID:", addr.toString());
			if (s == null)
				return;
			s = s.trim();
			if (s.length() == 0)
			{
				showError("Use 'Remove' button to delete this item.");
				return;
			}
			addr = new DcpAddress(s);
			sp.second = addr.toString();
			Pdt pdt = Pdt.instance();
			if (pdt != null)
			{
				PdtEntry pdte = pdt.find(addr);
				if (pdte != null && pdte.getDescription() != null)
					sp.second = sp.second + " :" + pdte.getDescription();
			}

			platSelectModel.valueChanged(row);
		}
		else if (sp.first == PlatSelectModel.DbNetlistLabel)
		{
			addDbListButtonPressed(sp.second);
		}
		else if (sp.first == PlatSelectModel.FileNetlistLabel)
		{
			nlFileChooser.setSelectedFile(new File(sp.second));
			nlFileChooser.showOpenDialog(this);
			File file = nlFileChooser.getSelectedFile();
			if (file == null || file.getPath().equals(sp.second))
				return;
			sp.second = file.getPath();
			platSelectModel.valueChanged(row);
		}
		else if (sp.first == PlatSelectModel.PlatformNameLabel)
		{
			String s = JOptionPane.showInputDialog(this, 
				"Enter Platform Name:", sp.second);
			if (s == null)
				return;
			s = s.trim();
			if (s.length() == 0)
			{
				showError("Use 'Remove' button to delete this item.");
				return;
			}
			sp.second = s;
			platSelectModel.valueChanged(row);
		}
		else if (sp.first == PlatSelectModel.GoesChannelLabel)
		{
			String s = JOptionPane.showInputDialog(this, 
				"Enter GOES Channel:", sp.second);
				if (s == null)
				return;
			s = s.trim();
			if (s.length() == 0)
				return;
			try { Integer.parseInt(s); }
			catch(Exception ex)
			{
				showError("Invalid GOES Channel '" + s + "'. Enter integer channel number.");
				return;
			}
			sp.second = s;
			platSelectModel.valueChanged(row);
		}
	}

	protected void removeSelectionButtonPressed()
	{
		int rows[] = platSelectTable.getSelectedRows();
		if (rows == null)
			return;
		// Have to remove them top to bottom so that indexes aren't changed.
		Arrays.sort(rows);
		for(int idx = rows.length-1; idx >= 0; idx--)
			platSelectModel.deleteEntryAt(rows[idx]);
	}

	protected void selectFromPdtButtonPressed()
	{
		if (pdtSelectDialog == null)
			pdtSelectDialog = new PdtSelectDialog(null);
		if (parent == null) System.out.println("parent is null!!!");
		parent.launchDialog(pdtSelectDialog);
		if (!pdtSelectDialog.isCancelled())
		{
			PdtEntry entries[] = pdtSelectDialog.getSelections();
			if (entries != null)
				for(PdtEntry ent : entries)
				{
					String desc = ent.getDescription();
					platSelectModel.add(PlatSelectModel.PlatformIdLabel,
						ent.dcpAddress.toString() + 
						((desc != null && desc.length() > 0) ? 
							" :" + desc : ""));
				}
		}
	}

	protected void addFileListButtonPressed()
	{
		nlFileChooser.showOpenDialog(this);
		File file = nlFileChooser.getSelectedFile();
		if (file == null)
			return;
		platSelectModel.add(PlatSelectModel.FileNetlistLabel, file.getPath());
	}

	protected void addDbListButtonPressed(String selection)
	{
		Database db = Database.getDb();
		if (db == null || db.networkListList == null)
		{
			showError("There is no DECODES database to select from.");
			return;
		}
		String nlNames[] = new String[db.networkListList.size() + 2];
		nlNames[0] = "<All>";
		nlNames[1] = "<Production>";
		int idx = 0;
		for(NetworkList nl : db.networkListList.getList())
			nlNames[2 + idx++] = nl.name;
			
		String selectedList = (String)JOptionPane.showInputDialog(this, "Select Network List:", 
			"Select Network List", JOptionPane.QUESTION_MESSAGE, 
			null, nlNames, selection != null ? selection : nlNames[0]);
		if (selectedList != null)
			platSelectModel.add(PlatSelectModel.DbNetlistLabel, selectedList);
	}

	protected void addNameButtonPressed()
	{
		Database db = Database.getDb();
		String name = null;
		if (db != null && db.platformList != null && db.platformList.size() > 0)
		{
			String names[] = new String[db.platformList.size()];
			int i=0;
			for(Platform p : db.platformList.getPlatformVector())
				names[i++] = p.getDisplayName();
			name = (String)JOptionPane.showInputDialog(this, "Enter Platform Name:", 
				"Platform Name", JOptionPane.PLAIN_MESSAGE, null, 
				names, names[0]);
		}
		else
		{
			name = JOptionPane.showInputDialog(this, "Enter Platform Name:", 
				"Platform Name", JOptionPane.PLAIN_MESSAGE);
		}
		if (name == null)
			return;
		name = name.trim();
		if (name.length() == 0)
			return;
		platSelectModel.add(PlatSelectModel.PlatformNameLabel, name);
	}

	protected void addIdButtonPressed()
	{
		String s = JOptionPane.showInputDialog(this,
			"Enter Platform ID:", "Platform ID Entry",
			JOptionPane.PLAIN_MESSAGE);
		if (s == null)
			return;
		s = s.trim();
		if (s.length() == 0)
			return;
		platSelectModel.addID(s);
	}

	private void setToDefaults()
	{
		this.clearAllTypesButtonPressed();
		this.sinceMethodCombo.setSelectedIndex(0);
		this.untilMethodCombo.setSelectedIndex(0);
		this.ascendingOrderCheck.setSelected(false);
		this.platSelectModel.clear();
	}
	
	/**
	 * Fill all the GUI fields and controls with contents of origSearchCrit.
	 */
	public void fillFields()
	{
		// How to apply time range? Default is LRGS.
		String daddsSince = origSearchCrit.getDapsSince();
		String lrgsSince = origSearchCrit.getLrgsSince();
		String daddsUntil = origSearchCrit.getDapsUntil();
		String lrgsUntil = origSearchCrit.getLrgsUntil();
		
		if (daddsSince == null || daddsSince.length() == 0)
			applyToCombo.setSelectedIndex(0);
		else if (lrgsSince == null || lrgsSince.length() == 0)
			applyToCombo.setSelectedIndex(1);
		else
			applyToCombo.setSelectedIndex(2);
		
		// Fill in Since Time Fields
		String since = lrgsSince;
		if (since == null || since.length() == 0)
			since = daddsSince;
		if (since == null || since.length() == 0)
			since = "now - 1 hour";
		String until = lrgsUntil;
		if (until == null || until.length() == 0)
			until = daddsUntil;
		if (until == null || until.length() == 0)
			until = allowRealTime ? null : "now";
		
		if(IDateFormat.isRelative(since))
		{
			sinceMethodCombo.setSelectedIndex(0);
			int minus = since.indexOf('-');
			if (minus != -1)
			{
				since = since.substring(minus + 1).trim();
				sinceNowMinusCombo.setSelectedItem(since);
			}
			else
				sinceNowMinusCombo.setSelectedIndex(0);
		}
		else if (since.toLowerCase().startsWith("filetime"))
		{
			sinceMethodCombo.setSelectedIndex(2);
			int paren = since.indexOf("(");
			if (paren != -1)
			{
				since = since.substring(paren+1).trim();
				if (since.endsWith(")"))
					since = since.substring(0, since.length()-1);
				sinceFileTimeField.setText(since);
			}
			else
				sinceFileTimeField.setText("");
		}
		else
		{
			sinceMethodCombo.setSelectedIndex(1);
			sinceDateTime.setDate(IDateFormat.parse(since));
		}

		// FIll in until time fields
		thirtySecDelayCheck.setSelected(origSearchCrit.getRealtimeSettlingDelay());
		if (until == null || until.length() == 0)
		{
			untilMethodCombo.setSelectedIndex(3);
		}
		else if (until.trim().equalsIgnoreCase("now"))
		{
			untilMethodCombo.setSelectedIndex(0);
		}
		else if (IDateFormat.isRelative(until))
		{
			untilMethodCombo.setSelectedIndex(1);
			int minus = until.indexOf('-');
			if (minus != -1)
			{
				until = until.substring(minus + 1).trim();
				untilNowMinusCombo.setSelectedItem(until);
			}
			else
				untilNowMinusCombo.setSelectedIndex(0);
		}
		else
		{
			untilMethodCombo.setSelectedIndex(2);
			untilDateTime.setDate(IDateFormat.parse(until));
		}

		clearAllTypesButtonPressed();

		ascendingOrderCheck.setSelected(origSearchCrit.getAscendingTimeOnly());
		if (origSearchCrit.spacecraft != SearchCriteria.SC_ANY)
		{
			goesSpacecraftCombo.setSelectedIndex(
				origSearchCrit.spacecraft == SearchCriteria.SC_EAST ? 0 : 1);
			goesSpacecraftCheck.setSelected(true);
			goesSpacecraftCombo.setEnabled(true);
		}
		else
		{
			goesSpacecraftCheck.setSelected(false);
			goesSpacecraftCombo.setEnabled(false);
		}

		if (origSearchCrit.numSources == 0
		 && origSearchCrit.DapsStatus == 'Y')
		{
			selectAllTypesButtonPressed();
		}
		else
		{
			boolean goesSelected = false;
			boolean anythingChecked = false;
			for(int idx = 0; idx < origSearchCrit.numSources; idx++)
			{
				int src = origSearchCrit.sources[idx];
				switch(src)
				{
				case DcpMsgFlag.SRC_DOMSAT:
				case DcpMsgFlag.SRC_DRGS:
				case DcpMsgFlag.SRC_NOAAPORT:
				case DcpMsgFlag.SRC_LRIT:
					goesSelected = true;
					break;
				case DcpMsgFlag.SRC_NETDCP:
					networkDcpCheck.setSelected(true);
					anythingChecked = true;
					break;
				case DcpMsgFlag.SRC_DDS:
					break;
				case DcpMsgFlag.SRC_IRIDIUM:
					iridiumCheck.setSelected(true);
					anythingChecked = true;
					break;
				case DcpMsgFlag.SRC_OTHER:
					modemDcpCheck.setSelected(true);
					anythingChecked = true;
					break;
				case DcpMsgFlag.MSG_TYPE_GOES_ST:
					goesSelfTimedCheck.setSelected(true);
					anythingChecked = true;
					break;
				case DcpMsgFlag.MSG_TYPE_GOES_RD:
					goesRandomCheck.setSelected(true);
					anythingChecked = true;
					break;
				}
			}
			if (origSearchCrit.DapsStatus == SearchCriteria.EXCLUSIVE)
			{
				goesSelfTimedCheck.setSelected(false);
				goesRandomCheck.setSelected(false);
				goesQualityCheck.setSelected(true);
			}
			else if (origSearchCrit.DapsStatus == SearchCriteria.REJECT)
			{
				goesQualityCheck.setSelected(false);
				if (goesSelected)
				{
					goesSelfTimedCheck.setSelected(true);
					goesRandomCheck.setSelected(true);
				}
			}
			else if (origSearchCrit.DapsStatus == SearchCriteria.ACCEPT)
				goesQualityCheck.setSelected(true);
		}
		if (origSearchCrit.parityErrors == SearchCriteria.REJECT)
		{
			parityCheckCombo.setSelectedIndex(0);
			parityCheck.setSelected(true);
			parityCheckCombo.setEnabled(true);
		}
		else if (origSearchCrit.parityErrors == SearchCriteria.EXCLUSIVE)
		{
			parityCheckCombo.setSelectedIndex(1);
			parityCheck.setSelected(true);
			parityCheckCombo.setEnabled(true);
		}
		else
		{
			parityCheck.setSelected(false);
			parityCheckCombo.setEnabled(false);
		}
		
		if (origSearchCrit.NetlistFiles != null)
			for(String netlistName : origSearchCrit.NetlistFiles)
			{
				Database db = Database.getDb();
				if (db != null && db.networkListList != null
				 && db.networkListList.find(netlistName) != null)
					platSelectModel.add(
						PlatSelectModel.DbNetlistLabel, netlistName);
				else
					platSelectModel.add(
						PlatSelectModel.FileNetlistLabel, netlistName);
			}

		if (origSearchCrit.DcpNames != null)
		{
			for(String nm : origSearchCrit.DcpNames)
				platSelectModel.add(
					PlatSelectModel.PlatformNameLabel, nm);
		}
		if (origSearchCrit.ExplicitDcpAddrs!=null)
		{
			for(DcpAddress addr : origSearchCrit.ExplicitDcpAddrs)
				platSelectModel.addID(addr.toString());
		}
		if (origSearchCrit.channels != null)
			for(int chan : origSearchCrit.channels)
				platSelectModel.add(
					PlatSelectModel.GoesChannelLabel, ""+chan);
	}
	
	/**
	 * Parse the fields into a SearchCriteria object.
	 */
	public void fillSearchCrit(SearchCriteria sc)
	{
		sinceDateTime.stopEditing();
		untilDateTime.stopEditing();
		sc.clear();
		
		for(int i=0; i<platSelectModel.getRowCount(); i++)
		{
			StringPair sp = platSelectModel.getEntryAt(i);
			if (sp.first.equals(PlatSelectModel.DbNetlistLabel))
				sc.addNetworkList(sp.second);
			else if (sp.first.equals(PlatSelectModel.FileNetlistLabel))
				sc.addNetworkList(sp.second);
			else if (sp.first.equals(PlatSelectModel.PlatformNameLabel))
				sc.addDcpName(sp.second);
			else if (sp.first.equals(PlatSelectModel.PlatformIdLabel))
			{
				String s = sp.second;
				int idx = s.indexOf(" :");
				if (idx > 0)
					s = s.substring(0, idx);
				sc.addDcpAddress(new DcpAddress(s));
			}
			else if (sp.first.equals("GOES Channel"))
				sc.addChannelToken(sp.second);
		}
		
		String since = "";
		if (sinceMethodCombo.getSelectedIndex() == 0) // "now - "
			since = "now - " + sinceNowMinusCombo.getSelectedItem();
		else if (sinceMethodCombo.getSelectedIndex() == 1) // calendar
			since = IDateFormat.toString(sinceDateTime.getDate(), false);
		else // filetime
			since = "filetime(" + sinceFileTimeField.getText() + ")";
		
		String until = "";
		if (untilMethodCombo.getSelectedIndex() == 0)
			until = "now";
		else if (untilMethodCombo.getSelectedIndex() == 1)
			until = "now - " + untilNowMinusCombo.getSelectedItem();
		else if (untilMethodCombo.getSelectedIndex() == 2)
			until = IDateFormat.toString(untilDateTime.getDate(), false);
		else // realtime
		{
			until = "";
			sc.setRealtimeSettlingDelay(thirtySecDelayCheck.isSelected());
		}

		if (applyToCombo.getSelectedIndex() == 0 || applyToCombo.getSelectedIndex() == 2)
		{
			sc.setLrgsSince(since);
			sc.setLrgsUntil(until);
		}
		if (applyToCombo.getSelectedIndex() == 1 || applyToCombo.getSelectedIndex() == 2)
		{
			sc.setDapsSince(since);
			sc.setDapsUntil(until);
		}
		
		sc.setAscendingTimeOnly(ascendingOrderCheck.isSelected());
		if (goesSpacecraftCheck.isSelected())
			sc.spacecraft = goesSpacecraftCombo.getSelectedIndex() == 0
				? SearchCriteria.SC_EAST : SearchCriteria.SC_WEST;
	
		boolean sourcesChecked = false;
		if (goesSelfTimedCheck.isSelected())
		{
			sc.addSource(DcpMsgFlag.MSG_TYPE_GOES_ST);
			sourcesChecked = true;
		}
		if (goesRandomCheck.isSelected())
		{
			sourcesChecked = true;
			sc.addSource(DcpMsgFlag.MSG_TYPE_GOES_RD);
		}
		if (networkDcpCheck.isSelected())
		{
			sc.addSource(DcpMsgFlag.SRC_NETDCP);
			sourcesChecked = true;
		}
		if (iridiumCheck.isSelected())
		{
			sc.addSource(DcpMsgFlag.SRC_IRIDIUM);
			sourcesChecked = true;
		}
		if (modemDcpCheck.isSelected())
		{
			sc.addSource(DcpMsgFlag.SRC_OTHER);
			sourcesChecked = true;
		}

		if (goesQualityCheck.isSelected())
		{
			if (goesSelfTimedCheck.isSelected() || goesRandomCheck.isSelected())
				sc.DapsStatus = SearchCriteria.ACCEPT;
			else
				sc.DapsStatus = SearchCriteria.EXCLUSIVE;
		}
		else
			sc.DapsStatus = sourcesChecked ? SearchCriteria.REJECT :
				SearchCriteria.UNSPECIFIED;

		if (parityCheck.isSelected())
		{
			if (parityCheckCombo.getSelectedIndex() == 0)
				sc.parityErrors = SearchCriteria.REJECT;
			else
				sc.parityErrors = SearchCriteria.EXCLUSIVE;
		}
	}
	

	/** 
	  Starts a modal error dialog with the passed message. 
	  @param msg the error message
	*/
	public void showError(String msg)
	{
		Logger.instance().log(Logger.E_FAILURE, msg);
		JOptionPane.showMessageDialog(this,
			AsciiUtil.wrapString(msg, 60), "Error!", JOptionPane.ERROR_MESSAGE);
	}


	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		TopFrame frame = new TopFrame();
		frame.setTitle("Search Criteria Editor");
		SearchCriteriaEditPanel scePanel = new SearchCriteriaEditPanel();
		scePanel.setTopFrame(frame);
		frame.add(scePanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setLocationRelativeTo(null);
		

		frame.setVisible(true);
		
	}

	public void setAllowRealTime(boolean allowRealTime)
	{
		this.allowRealTime = allowRealTime;
	}

	/**
	 * Called from the routing spec edit panel when a data source is selected.
	 * Enable or disable the appropriate controls depending on whether an LRGS
	 * has been selected, or some other type.
	 * @param isLrgsType true if an LRGS data source has been selected.
	 */
	public void setIsLrgs(boolean isLrgsType)
	{
		sinceMethodCombo.setEnabled(isLrgsType);
		sinceNowMinusCombo.setEnabled(isLrgsType);
		sinceDateTime .setEnabled(isLrgsType);
		sinceFileTimeField.setEnabled(isLrgsType);
		untilMethodCombo.setEnabled(isLrgsType);
		untilNowMinusCombo.setEnabled(isLrgsType);
		untilDateTime.setEnabled(isLrgsType);
		thirtySecDelayCheck.setEnabled(isLrgsType);
		applyToCombo.setEnabled(isLrgsType);
		ascendingOrderCheck.setEnabled(isLrgsType);
		goesSelfTimedCheck.setEnabled(isLrgsType);
		goesRandomCheck.setEnabled(isLrgsType);
		goesQualityCheck.setEnabled(isLrgsType);
		goesSpacecraftCheck.setEnabled(isLrgsType);
		goesSpacecraftCombo.setEnabled(isLrgsType);
		iridiumCheck.setEnabled(isLrgsType);
		networkDcpCheck.setEnabled(isLrgsType);
		modemDcpCheck.setEnabled(isLrgsType);
		parityCheck.setEnabled(isLrgsType);
		parityCheckCombo.setEnabled(isLrgsType);
		selectFromPdtButton.setEnabled(isLrgsType);
		addGoesChannelButton.setEnabled(isLrgsType);
		sinceFileTimeBrowsButton.setEnabled(isLrgsType);
		clearAllTypesButton.setEnabled(isLrgsType);
		selectAllTypesButton.setEnabled(isLrgsType);
	}
}

@SuppressWarnings("serial")
class PlatSelectModel extends AbstractTableModel
	implements SortingListTableModel
{
	private ArrayList<StringPair> entries = new ArrayList<StringPair>();
	public static final String DbNetlistLabel = "Netlist";
	public static final String FileNetlistLabel = "File Netlist";
	public static final String PlatformNameLabel = "Platform Name";
	public static final String PlatformIdLabel = "Platform ID";
	public static final String GoesChannelLabel = "GOES Channel";
	private int sortColumn = -1;
	
	@Override
	public int getColumnCount()
	{
		return 2;
	}
	
	public String getColumnName(int c)
	{
		return c==0 ? "Type" : "Value";
	}

	@Override
	public int getRowCount()
	{
		return entries.size();
	}

	@Override
	public Object getValueAt(int row, int col)
	{
		StringPair sp = entries.get(row);
		return col == 0 ? sp.first : sp.second;
	}
	
	public StringPair getEntryAt(int row)
	{
		return entries.get(row);
	}
	
	public void addID(String value)
	{
		Pdt pdt = Pdt.instance();
		if (pdt != null)
		{
			PdtEntry pdte = pdt.find(new DcpAddress(value));
			if (pdte != null && pdte.getDescription() != null)
				value = value + " :" + pdte.getDescription();
		}
		
		add(PlatSelectModel.PlatformIdLabel, value);
	}
	
	public void add(String type, String value)
	{
		entries.add(new StringPair(type,value));
		fireTableDataChanged();
	}
	
	public void deleteEntryAt(int row)
	{
		entries.remove(row);
		fireTableDataChanged();
	}
	
	public void modifyValueAt(int row, String newValue)
	{
		entries.get(row).second = newValue;
	}
	
	public void clear()
	{
		entries.clear();
		fireTableDataChanged();
	}
	
	public void valueChanged(int row)
	{
		this.fireTableCellUpdated(row, 1);
	}

	@Override
	public void sortByColumn(int column)
	{
		sortColumn = column;
		Collections.sort(entries, new StringPairComparator(sortColumn));
		fireTableDataChanged();
	}

	@Override
	public Object getRowObject(int row)
	{
		return getEntryAt(row);
	}
}

class StringPairComparator implements Comparator<StringPair>
{
	private int col = 0;
	public StringPairComparator(int col)
	{
		this.col = col;
	}
	@Override
	public int compare(StringPair sp1, StringPair sp2)
	{
		if (col == 0)
			return TextUtil.strCompareIgnoreCase(sp1.first, sp2.first);
		else
			return TextUtil.strCompareIgnoreCase(sp1.second, sp2.second);
	}
	
}
