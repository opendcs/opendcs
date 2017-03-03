/*
 *  $Id$
 *  
 *  Open Source Software
 *  
 *  $Log$
 *  Revision 1.5  2015/04/15 19:59:46  mmaloney
 *  Fixed synchronization bugs when the same data sets are being processed by multiple
 *  routing specs at the same time. Example is multiple real-time routing specs with same
 *  network lists. They will all receive and decode the same data together.
 *
 *  Revision 1.4  2015/03/19 18:02:03  mmaloney
 *  Fixed caching of lists so that when platform is committed, the in-memory lists are updated.
 *
 *  Revision 1.3  2015/01/14 17:22:51  mmaloney
 *  Polling implementation
 *
 *  Revision 1.2  2014/08/29 18:24:35  mmaloney
 *  6.1 Schema Mods
 *
 *  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 *  OPENDCS 6.0 Initial Checkin
 *
 *  Revision 1.6  2013/03/21 18:27:40  mmaloney
 *  DbKey Implementation
 *
 */
package decodes.dbeditor;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.Iterator;
import java.util.Date;
import java.util.ResourceBundle;

import ilex.util.AsciiUtil;
import ilex.util.LoadResourceBundle;
import ilex.util.PropertiesUtil;
import ilex.util.Logger;
import ilex.util.TextUtil;
import decodes.gui.*;
import decodes.db.*;
import decodes.util.DecodesSettings;


/**
Panel to edit an open Platform object.
Opened from PlatformListPanel. Also used by PlatformWizard.
 */
public class PlatformEditPanel extends DbEditorTab
implements HistoricalVersionController, ChangeTracker, EntityOpsController,
ConfigSelectController
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	TitledBorder titledBorder1;
	TitledBorder titledBorder2;
	TitledBorder titledBorder3;
	EntityOpsPanel entityOpsPanel;
	JPanel jPanel7 = new JPanel();
	JTextField configField = new JTextField();
	JPanel jPanel4 = new JPanel();
	JPanel jPanel3 = new JPanel();
	JPanel topLeftPanel = new JPanel();
	JPanel jPanel1 = new JPanel();
	JTextArea descriptionArea = new JTextArea();
	JButton chooseConfigButton = new JButton();
	JButton editConfigButton = new JButton();
	JScrollPane jScrollPane1 = new JScrollPane();
	JTextField ownerAgencyField = new JTextField();
	JTextField expirationField = new JTextField();
	JButton makeHistVersionButton = new JButton();
	JTextField siteNameField = new JTextField();
	JTextField designatorField = new JTextField();
	JTextField lastModifiedField = new JTextField();
	JButton chooseSiteButton = new JButton();
	JLabel lastModLabel = new JLabel();
	JLabel ownerLabel = new JLabel();
	JLabel configLabel = new JLabel();
	JLabel siteLabel = new JLabel();
	JLabel designatorLabel = new JLabel();
	BorderLayout borderLayout1 = new BorderLayout();
	BorderLayout borderLayout2 = new BorderLayout();
	JCheckBox isProductionCheck = new JCheckBox();
	JPanel jPanel8 = new JPanel();
	JScrollPane jScrollPane2 = new JScrollPane();
	JTable transportMediaTable;
	JButton deleteTransportMediaButton = new JButton();
	JButton editTransportMediaButton = new JButton();
	GridBagLayout gridBagLayout1 = new GridBagLayout();
	JButton addTransportMediaButton = new JButton();
	JPanel jPanel5 = new JPanel();
	JTable platformSensorTable;
	JButton sensorInfoButton = new JButton();
	JScrollPane jScrollPane3 = new JScrollPane();
	GridBagLayout gridBagLayout2 = new GridBagLayout();
	JPanel jPanel6 = new JPanel();
	GridLayout gridLayout1 = new GridLayout();
	GridBagLayout gridBagLayout4 = new GridBagLayout();
	GridBagLayout gridBagLayout5 = new GridBagLayout();
	BorderLayout borderLayout3 = new BorderLayout();
	DbEditorFrame parent;

	Platform origPlatform; // Original (unchanged) platform
	Platform thePlatform;

	JLabel expirationLabel = new JLabel();  // Copy that we're editing
	TransportListTableModel transportTableModel;
	SensorInfoTableModel sensorTableModel;
	JButton platformPropertiesButton = new JButton();
	GridBagLayout gridBagLayout3 = new GridBagLayout();

	/** True if this is running inside dbedit, false for platwiz. */
	//public static boolean inDbEdit = true;
	private boolean inDbEdit = true;

	/** The referenced config, used for populating sensor lists. */
	private PlatformConfig refConfig = null;

	/** No-args constructor does basic component construction */
	public PlatformEditPanel()
	{
		super();
		try 
		{
			transportTableModel = new TransportListTableModel();
			transportMediaTable = new JTable(transportTableModel);
			transportMediaTable.getTableHeader().setReorderingAllowed(false);
			sensorTableModel = new SensorInfoTableModel();
			platformSensorTable = new JTable(sensorTableModel);
			platformSensorTable.getTableHeader().setReorderingAllowed(false);
			jbInit();
			TableColumnAdjuster.adjustColumnWidths(platformSensorTable,
					new int[] { 10, 15, 20, 45 });
			TableColumnAdjuster.adjustColumnWidths(transportMediaTable,
					new int[] { 25, 25, 35, 15 });
			entityOpsPanel = new EntityOpsPanel(this);
			
			transportMediaTable.addMouseListener(new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 2)
						editTransportMediaPressed();
				}
			});

			platformSensorTable.addMouseListener(new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 2)
						sensorInfoPressed();
				}
			});
			
			
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	  Constructs new PlatformEditPanel for specified platform.
	  @param p the object to edit in this panel.
	 */
	public PlatformEditPanel(Platform p)
	{
		this();
		setPlatform(p);
	}

	/**
	 * Constructor used by the Platform Wizard
	 * @param inDbEdit
	 */
	public PlatformEditPanel(boolean inDbEdit)
	{
		super();
		try 
		{
			this.inDbEdit = inDbEdit;
			transportTableModel = new TransportListTableModel();
			transportMediaTable = new JTable(transportTableModel);
			transportMediaTable.getTableHeader().setReorderingAllowed(false);
			sensorTableModel = new SensorInfoTableModel();
			platformSensorTable = new JTable(sensorTableModel);
			platformSensorTable.getTableHeader().setReorderingAllowed(false);
			jbInit();
			TableColumnAdjuster.adjustColumnWidths(platformSensorTable,
					new int[] { 10, 15, 20, 45 });
			TableColumnAdjuster.adjustColumnWidths(transportMediaTable,
					new int[] { 25, 25, 35, 15 });
			entityOpsPanel = new EntityOpsPanel(this);
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	/** 
	  Sets the platform that this panel is editing. 
	  @param p the platform to edit
	 */
	public void setPlatform(Platform p)
	{
		origPlatform = p;
		thePlatform = p.copy();
		//MJM 2006 05/12
		//		setTopObject(origPlatform);
		setTopObject(thePlatform);
		transportTableModel.setPlatform(thePlatform);
		refConfig = thePlatform.getConfig();
		sensorTableModel.setPlatform(thePlatform, refConfig);
		fillFields();
	}

	/**
	 * This method specifically for the DBEdit Import function.
	 * We are passed an imported platform. Set 'thePlatform' but leave
	 * 'origPlatform' unchanged.
	 * @param p the platform just imported
	 */
	public void setImportedPlatform(Platform p)
	{
		thePlatform.copyFrom(p);
		//		transportTableModel.setPlatform(thePlatform);
		Platform.configSoftLink = false;
		refConfig = thePlatform.getConfig();
		Platform.configSoftLink = true;
		sensorTableModel.setPlatform(thePlatform, refConfig);
		fillFields();
	}

	/**
	  This method only called inside dbedit.
	  @param parent the enclosing DbEditorFrame.
	 */
	void setParent(DbEditorFrame parent)
	{
		this.parent = parent;
		this.add(entityOpsPanel, BorderLayout.SOUTH);
	}

	/** Fills the fields from the platform */
	private void fillFields()
	{
		configField.setText(
				refConfig == null ? "" : refConfig.getDisplayName());
		descriptionArea.setText(thePlatform.description);
		expirationField.setText(
				thePlatform.expiration == null ? "" :
					decodes.db.Constants.defaultDateFormat.format(
							thePlatform.expiration).toString());
		siteNameField.setText(thePlatform.getSiteName());
		lastModifiedField.setText(
				thePlatform.lastModifyTime == null ? "" :
					decodes.db.Constants.defaultDateFormat.format(
							thePlatform.lastModifyTime).toString());
		isProductionCheck.setSelected(thePlatform.isProduction);
		if ( !inDbEdit ) {
			if ( thePlatform.agency == null || thePlatform.agency.trim().isEmpty() ) {
				if ( thePlatform.getSiteName().indexOf('-') > 0 ) {
					String[] comp = thePlatform.getSiteName().split("-");
					thePlatform.agency = comp[0];
				}
			}
			String desig = thePlatform.getPlatformDesignator();
			if ( desig == null || desig.trim().isEmpty() ) {
				if (DecodesSettings.instance().setPlatformDesignatorName ) {
					String[] comp = refConfig.getDisplayName().split("-");
					String newdesignator = Database.getDb().platformList.getDesignator(thePlatform.getSite().getDisplayName(),comp[0]);
					thePlatform.setPlatformDesignator(newdesignator);
				}
			}
		}
		ownerAgencyField.setText(thePlatform.agency);

		String d = thePlatform.getPlatformDesignator();
		designatorField.setText(d == null ? "" : d);
	}

	/** Initializes GUI components */
	private void jbInit() throws Exception {
		titledBorder1 = new TitledBorder(
				BorderFactory.createLineBorder(
						new Color(153, 153, 153),2),
						genericLabels.getString("description"));
		titledBorder2 = new TitledBorder(
				BorderFactory.createLineBorder(
						new Color(153, 153, 153),2),
						dbeditLabels.getString("PlatformEditPanel.tm"));
		titledBorder3 = new TitledBorder(
				BorderFactory.createLineBorder(
						new Color(153, 153, 153),2),
						dbeditLabels.getString("PlatformEditPanel.sensorInfo"));
		this.setLayout(borderLayout2);
		jPanel7.setLayout(borderLayout3);
		configField.setEditable(false);
		configField.setToolTipText(
				dbeditLabels.getString("PlatformEditPanel.configFieldTT"));
		jPanel4.setLayout(borderLayout1);
		jPanel4.setBorder(titledBorder1);
		jPanel3.setLayout(gridBagLayout4);
		topLeftPanel.setLayout(gridBagLayout3);
		jPanel1.setLayout(gridBagLayout5);
		descriptionArea.setPreferredSize(new Dimension(422, 45));
		chooseConfigButton.setText(
				genericLabels.getString("choose"));
		chooseConfigButton.addActionListener(
				new java.awt.event.ActionListener()
				{
					public void actionPerformed(ActionEvent e) 
					{
						chooseConfigButtonPressed();
					}
				});
		editConfigButton.setText(
				genericLabels.getString("edit"));
		editConfigButton.addActionListener(
				new java.awt.event.ActionListener()
				{
					public void actionPerformed(ActionEvent e) 
					{
						editConfigButton_actionPerformed();
					}
				});
		expirationField.setEnabled(false);
		expirationField.setEditable(false);
		expirationField.setText("	 ");
		expirationField.setToolTipText(
				dbeditLabels.getString("PlatformEditPanel.expFieldTT"));
		makeHistVersionButton.setText(
				dbeditLabels.getString("PlatformEditPanel.makeHistVersion"));
		makeHistVersionButton.setToolTipText(
				dbeditLabels.getString("PlatformEditPanel.makeHistVersionTT"));

		makeHistVersionButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				makeHistVersionButton_actionPerformed(e);
			}
		});
		siteNameField.setEditable(false);
		siteNameField.setToolTipText(
				dbeditLabels.getString("PlatformEditPanel.siteNameTT"));
		designatorField.setToolTipText(
				dbeditLabels.getString("PlatformEditPanel.designatorTT"));
		lastModifiedField.setEnabled(false);
		lastModifiedField.setToolTipText(
				dbeditLabels.getString("PlatformEditPanel.lastModTT"));
		lastModifiedField.setEditable(false);
		chooseSiteButton.setText(
				genericLabels.getString("choose"));
		chooseSiteButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				chooseSiteButtonPressed();
			}
		});
		lastModLabel.setText(
				dbeditLabels.getString("PlatformEditPanel.lastModLabel"));
		ownerLabel.setText(
				dbeditLabels.getString("PlatformEditPanel.ownerLabel"));
		ownerAgencyField.setToolTipText(
				dbeditLabels.getString("PlatformEditPanel.ownerTT"));
		configLabel.setText(
				dbeditLabels.getString("PlatformEditPanel.configLabel"));
		siteLabel.setText(
				dbeditLabels.getString("PlatformEditPanel.siteLabel"));
		designatorLabel.setText(
				dbeditLabels.getString("PlatformEditPanel.designatorLabel"));
		isProductionCheck.setText(
				dbeditLabels.getString("PlatformEditPanel.isProductionLabel"));
		isProductionCheck.setToolTipText(
				dbeditLabels.getString("PlatformEditPanel.isProductionTT"));
		isProductionCheck.setHorizontalAlignment(SwingConstants.CENTER);
		isProductionCheck.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				isProductionCheck_actionPerformed(e);
			}
		});
		jPanel8.setLayout(gridLayout1);
		deleteTransportMediaButton.setText(
				genericLabels.getString("delete"));
		deleteTransportMediaButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				deleteTransportMediaButton_actionPerformed(e);
			}
		});
		editTransportMediaButton.setText(
				genericLabels.getString("edit"));
		editTransportMediaButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				editTransportMediaPressed();
			}
		});
		addTransportMediaButton.setText(
				genericLabels.getString("add"));
		addTransportMediaButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addTransportMediaButton_actionPerformed(e);
			}
		});
		jPanel5.setBorder(titledBorder2);
		jPanel5.setLayout(gridBagLayout1);

		sensorInfoButton.setText(
				dbeditLabels.getString("PlatformEditPanel.editSensor"));
		sensorInfoButton.addActionListener(
				new java.awt.event.ActionListener() 
				{
					public void actionPerformed(ActionEvent e) 
					{
						sensorInfoPressed();
					}
				});

		jPanel6.setLayout(gridBagLayout2);
		jPanel6.setBorder(titledBorder3);
		gridLayout1.setRows(2);
		gridLayout1.setColumns(1);
		expirationLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		expirationLabel.setText(
				dbeditLabels.getString("PlatformEditPanel.expirationLabel"));
		platformPropertiesButton.setText(
				dbeditLabels.getString("PlatformEditPanel.PlatformProps"));
		platformPropertiesButton.setToolTipText(
				dbeditLabels.getString("PlatformEditPanel.PlatformPropsTT"));
		platformPropertiesButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				platformPropertiesButton_actionPerformed(e);
			}
		});
		this.add(jPanel7, BorderLayout.CENTER);
		jPanel7.add(jPanel1, BorderLayout.NORTH);
		jPanel1.add(topLeftPanel, 
				new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
						GridBagConstraints.CENTER, GridBagConstraints.BOTH, 
						new Insets(4, 4, 4, 4), 0, 0));

		// Top Left Panel with Site, Designator, Config, Owner & Properties
		topLeftPanel.add(siteLabel, 
				new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
						GridBagConstraints.EAST, GridBagConstraints.NONE, 
						new Insets(3, 0, 3, 4), 0, 0));
		topLeftPanel.add(siteNameField, 
				new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
						GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
						new Insets(3, 0, 3, 0), 112, 0));
		if (inDbEdit)
			topLeftPanel.add(chooseSiteButton, 
					new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
							GridBagConstraints.WEST, GridBagConstraints.NONE, 
							new Insets(3, 10, 3, 0), 9, 0));

		topLeftPanel.add(designatorLabel, 
				new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
						GridBagConstraints.EAST, GridBagConstraints.NONE, 
						new Insets(3, 0, 3, 4), 0, 0));
		topLeftPanel.add(designatorField, 
				new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
						GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
						new Insets(3, 0, 3, 0), 112, 0));

		topLeftPanel.add(configLabel, 
				new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
						GridBagConstraints.EAST, GridBagConstraints.NONE, 
						new Insets(3, 0, 3, 4), 0, 0));
		topLeftPanel.add(configField, 
				new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
						GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
						new Insets(3, 0, 3, 0), 112, 0));
		if (inDbEdit)
		{
			topLeftPanel.add(chooseConfigButton, 
					new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
							GridBagConstraints.WEST, GridBagConstraints.NONE, 
							new Insets(3, 10, 3, 5), 9, 0));
			topLeftPanel.add(editConfigButton, 
					new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0,
							GridBagConstraints.WEST, GridBagConstraints.NONE, 
							new Insets(3, 0, 3, 15), 9, 0));
		}

		topLeftPanel.add(ownerLabel, 
				new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
						GridBagConstraints.EAST, GridBagConstraints.NONE, 
						new Insets(3, 5, 3, 4), 0, 0));
		topLeftPanel.add(ownerAgencyField, 
				new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0,
						GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
						new Insets(3, 0, 3, 0), 112, 0));

		topLeftPanel.add(platformPropertiesButton, 
				new GridBagConstraints(0, 4, 3, 1, 0.0, 0.0,
						GridBagConstraints.CENTER, GridBagConstraints.NONE, 
						new Insets(5, 0, 5, 0), 117, 0));


		jPanel1.add(jPanel3, new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0
				,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0));
		jPanel3.add(lastModLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
				,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
		jPanel3.add(lastModifiedField, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
				,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 4, 2, 10), 0, 0));
		jPanel3.add(expirationField, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
				,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 4, 2, 10), 0, 0));
		jPanel3.add(expirationLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
				,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		if (inDbEdit)
			jPanel3.add(isProductionCheck, new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0
					,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(3, 0, 5, 0), 0, 0));
		if (inDbEdit)
			jPanel3.add(makeHistVersionButton, new GridBagConstraints(0, 3, 2, 1, 0.0, 0.0
					,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 11, 4, 13), 0, 0));
		jPanel1.add(jPanel4, new GridBagConstraints(0, 1, 2, 1, 1.0, 1.5
				,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0));
		jPanel4.add(jScrollPane1, BorderLayout.CENTER);
		jPanel7.add(jPanel8, BorderLayout.CENTER);
		jPanel8.add(jPanel6, null);
		jPanel6.add(jScrollPane3, 
				new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
						GridBagConstraints.CENTER, GridBagConstraints.BOTH, 
						new Insets(4, 5, 4, 5), 0, 0));

		jPanel6.add(sensorInfoButton, 
				new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
						GridBagConstraints.NORTH, GridBagConstraints.NONE, 
						new Insets(2, 12, 2, 12), 0, 0));

		jScrollPane3.getViewport().add(platformSensorTable, null);
		jPanel8.add(jPanel5, null);
		jPanel5.add(jScrollPane2, new GridBagConstraints(0, 0, 1, 3, 1.0, 1.0
				,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(4, 5, 4, 5), 0, 0));
		jPanel5.add(addTransportMediaButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
				,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 12, 2, 12), 0, 0));
		jPanel5.add(editTransportMediaButton, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
				,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 12, 2, 12), 16, 0));
		jPanel5.add(deleteTransportMediaButton, new GridBagConstraints(1, 2, 1, 1, 0.0, 1.0
				,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(2, 12, 2, 12), 0, 0));
		jScrollPane2.getViewport().add(transportMediaTable, null);
		jScrollPane1.getViewport().add(descriptionArea, null);
	}

	/** Called when Site 'Choose' button is pressed. */
	void chooseSiteButtonPressed()
	{
		SiteSelectDialog dlg = new SiteSelectDialog();
		launchDialog(dlg);
		Site site = dlg.getSelectedSite();
		if (site != null) // selection was made?
		{
			thePlatform.setSite(site);
			siteNameField.setText(thePlatform.getSiteName());
			
			/*  For USGS, use the name type as the agency code */
			if ( thePlatform.getSiteName().indexOf('-') > 0 
			 && Database.getDb().getDbIo().isNwis())
			{
				String[] comp = thePlatform.getSiteName().split("-");
				ownerAgencyField.setText(comp[0]);
			}
			sensorTableModel.fillValuesNew();
		}
	}

	/** Called when Config 'Choose' button is pressed. */
	void chooseConfigButtonPressed()
	{
		ConfigSelectDialog csd = new ConfigSelectDialog(parent, this);
		if (refConfig != null)
			csd.setSelection(refConfig.getDisplayName());
		launchDialog(csd);
		/* if setPlatformDesignatorName in properties file is true,
			 default designator  -- SED 05/15/2008 */
		if (DecodesSettings.instance().setPlatformDesignatorName ) {
			String[] comp = refConfig.getDisplayName().split("-");
			String newdesignator = Database.getDb().platformList.getDesignator(thePlatform.getSite().getDisplayName(),comp[0]);
			designatorField.setText(newdesignator);
		}
		sensorTableModel.fillValues();
	}

	/** Called when Config 'Edit' button is pressed. */
	void editConfigButton_actionPerformed()
	{
		// Get the config.
		if (refConfig == null)
		{
			parent.showError(
					dbeditLabels.getString("PlatformEditPanel.noConfigError"));
			return;
		}
		String cfgName = refConfig.getDisplayName();
		PlatformConfig cfg = Database.getDb().platformConfigList.get(cfgName);
		if (cfg == null)
		{
			parent.showError(
					dbeditLabels.getString("PlatformEditPanel.cantFindConfigError"));
			return;
		}

		// Call ConfigsListPanel.doOpen
		parent.getConfigsListPanel().doOpen(cfg);

		// Make the Config tab the currently selected tab
		parent.activateConfigsTab();
	}

	/** Called when Config 'Make Historical Version' button is pressed. */
	void makeHistVersionButton_actionPerformed(ActionEvent e)
	{
		HistoricalVersionDialog dlg = new HistoricalVersionDialog(
				thePlatform, this);
		launchDialog(dlg);
	}

	/** 
	  Makes a historical version of the platform with specified expiration.
	  @param expiration the expiration date/time.
	 */
	public void makeHistoricalVersion(Date expiration)
	throws DatabaseException
	{
		Platform histVersion = thePlatform.noIdCopy();

		histVersion.expiration = expiration;
		Database.getDb().platformList.add(histVersion);
		histVersion.write();
		Database.getDb().platformList.write();
	}

	/** @return the name for the platform being edited. */
	public String entityName()
	{
		return thePlatform.makeFileName();
	}

	/** Called when sensor properties button pressed. */
	void sensorInfoPressed()
	{
		int r = platformSensorTable.getSelectedRow();
		if (r == -1)
		{
			parent.showError(
					dbeditLabels.getString("PlatformEditPanel.selectSensorError"));
			return;
		}
		PlatformSensor ps = sensorTableModel.getObjectAt(r);
		PlatformSensorEditDialog dlg = 
			new PlatformSensorEditDialog(parent, 
					dbeditLabels.getString("PlatformEditPanel.editPlatInfoDlgTitle"),
					true);
		dlg.fillFields(ps, refConfig.getSensor(ps.sensorNumber));

		//		PropertiesEditDialog dlg = new PropertiesEditDialog(
		//			"Sensor " + ps.sensorNumber, ps.properties);

		launchDialog(dlg);
		sensorTableModel.fireTableDataChanged();
	}

	//	/** Called when 'clear sensor site' button pressed. */
	//	void clearSensorSiteButton_actionPerformed(ActionEvent e)
	//	{
	//		int r = platformSensorTable.getSelectedRow();
	//		if (r == -1)
	//		{
	//			DbEditorFrame.instance().showError("Select sensor, then press Clear...");
	//			return;
	//		}
	//		PlatformSensor ps = sensorTableModel.getObjectAt(r);
	//		ps.site = null;
	//		//ps.siteId = Constants.undefinedId;
	//		sensorTableModel.fireTableDataChanged();
	//	}
	//
	//	/** Called when 'select sensor site' button pressed. */
	//	void selectSensorSiteButton_actionPerformed(ActionEvent e)
	//	{
	//		int r = platformSensorTable.getSelectedRow();
	//		if (r == -1)
	//		{
	//			DbEditorFrame.instance().showError("Select sensor, then press Select Site...");
	//			return;
	//		}
	//		PlatformSensor ps = sensorTableModel.getObjectAt(r);
	//
	//		SiteSelectDialog dlg = new SiteSelectDialog();
	//		launchDialog(dlg);
	//		Site site = dlg.getSelectedSite();
	//		if (site != null) // selection was made?
	//		{
	//			ps.site = site;
	//			//ps.siteId = site.siteId;
	//		}
	//		sensorTableModel.fireTableDataChanged();
	//	}

	/** Called when Transport Medium 'Add' button pressed. */
	void addTransportMediaButton_actionPerformed(ActionEvent e)
	{
		TransportMedium tm = new TransportMedium(thePlatform);
		transportTableModel.add(tm);
		TransportMediaEditDialog dlg = new TransportMediaEditDialog(
				thePlatform, tm, transportTableModel);
		launchDialog(dlg);
		if (!dlg.okPressed)
			transportTableModel.remove(tm);
	}

	/** Called when Transport Medium 'Edit' button pressed. */
	void editTransportMediaPressed()
	{
		TransportMedium tm = transportTableModel.getObjectAt(
			transportMediaTable.getSelectedRow());
		if (tm == null)
		{
			parent.showError(
				dbeditLabels.getString("PlatformEditPanel.selectTmEdit"));
			return;
		}
		TransportMediaEditDialog dlg = new TransportMediaEditDialog(
			thePlatform, tm, transportTableModel);
		launchDialog(dlg);
	}

	/** Called when Transport Medium 'Delete' button pressed. */
	void deleteTransportMediaButton_actionPerformed(ActionEvent e)
	{
		TransportMedium tm = transportTableModel.getObjectAt(
				this.transportMediaTable.getSelectedRow());
		if (tm == null)
		{
			parent.showError(
					dbeditLabels.getString("PlatformEditPanel.selectTmDelete"));
			return;
		}
		int r = JOptionPane.showConfirmDialog(this,
				LoadResourceBundle.sprintf(
						dbeditLabels.getString("PlatformEditPanel.confirmDelete"),
						tm.getMediumId()));
		if (r == JOptionPane.OK_OPTION)
			transportTableModel.remove(tm);
	}

	/** Called when 'Is Production' checkbox is toggled. */
	void isProductionCheck_actionPerformed(ActionEvent e)
	{
		thePlatform.isProduction = isProductionCheck.isSelected();
	}

	/**
	 * @return true if changes have been made to this screen since the last
	 * time it was saved.
	 * (from ChangeTracker interface)
	 */
	public boolean hasChanged()
	{
		// If editing in progress, stop & save the latest change.
		if (transportMediaTable.isEditing())
		{
			TableCellEditor tc = transportMediaTable.getCellEditor();
			tc.stopCellEditing();
		}
		if (platformSensorTable.isEditing())
		{
			TableCellEditor tc = platformSensorTable.getCellEditor();
			tc.stopCellEditing();
		}

		getDataFromFields();
		return !thePlatform.equals(origPlatform);
	}

	/**
	 * Saves the changes back to the database & reset the hasChanged flag.
	 * (from ChangeTracker interface)
	 * @return true if save was successful.
	 */
	public boolean saveChanges()
	{
		getDataFromFields();

		// Write the changes out to the database.
		try
		{
			thePlatform.write();
			// Replace origPlatform in PlatformList with the modified platform.
			Database.getDb().platformList.removePlatform(origPlatform);
			Database.getDb().platformList.add(thePlatform);
			Database.getDb().platformList.write();
			parent.getPlatformListPanel().replacePlatform(origPlatform, thePlatform);
			parent.getPlatformListPanel().reSort();

			// If any transport media have changed, and those media were in a netlist,
			// ask user if she wants to update the list.
			ArrayList<ListTmAssoc> affectedLists = new ArrayList<ListTmAssoc>();
			StringBuilder listStr = new StringBuilder();
			for(Iterator<TransportMedium> oldTmIt = origPlatform.getTransportMedia();
				oldTmIt.hasNext(); )
			{
				TransportMedium oldTm = oldTmIt.next();
				TransportMedium newTm = thePlatform.getTransportMedium(oldTm.getMediumType());
//System.out.println("oldTm: type=" + oldTm.getMediumType() + ", id=" + oldTm.getMediumId());
//System.out.println("newTm: " + 
//	(newTm == null ? "null" : 
//	("type=" + newTm.getMediumType() + ", id=" + newTm.getMediumId())));

				if (newTm != null && newTm.getMediumId().equals(oldTm.getMediumId()))
					continue;

				// Else either the TM was deleted or changed.
				for(NetworkList netlist : Database.getDb().networkListList.getList())
					if (netlist.contains(oldTm))
					{
						affectedLists.add(new ListTmAssoc(oldTm, newTm, netlist));
						listStr.append(netlist.name + ", ");
					}
			}
			
			if (affectedLists.size() != 0)
			{
				String m = "The network lists: "
					+ listStr.toString()
					+ "contain this Platform. Do you want to update the lists with "
					+ "the new transport medium ID?";
				int res = JOptionPane.showConfirmDialog(null, 
					AsciiUtil.wrapString(m, 60), 
					"Update Network List?", JOptionPane.YES_NO_OPTION);
				if(res == JOptionPane.OK_OPTION)
				{
					for (ListTmAssoc lta : affectedLists)
					{
						lta.netlist.removeEntry(lta.oldTm.getMediumId());
						if (lta.newTm != null)
						{
							NetworkListEntry nle = 
								new NetworkListEntry(lta.netlist, lta.newTm.getMediumId());
						
							Site s = thePlatform.getSite();
							String sn = null;
							if (s != null)
								sn = s.getPreferredName().getNameValue();
							if (sn != null)
								nle.setPlatformName(sn);
							else
								nle.setPlatformName(thePlatform.makeFileName());
							// Get a description from either platform or site record.
							String desc = s.getDescription();
							if (desc == null)
							{
								desc = thePlatform.description;
								// netlist entry description is only the 1st line of platform
								// desc.
								if (desc != null)
								{
									int idx = desc.indexOf('\r');
									if (idx == -1)
										idx = desc.indexOf('\n');
									if (idx != -1)
										desc = desc.substring(0, idx);
								}
							}
							nle.setDescription(desc);
							lta.netlist.addEntry(nle);
							lta.netlist.write();
						}
					}				
				}
			}
		}
		catch(DatabaseException e)
		{
			parent.showError(e.toString());
			return false;
		}

		// Make a new copy in case user wants to keep editing.
		origPlatform = thePlatform;
		thePlatform = origPlatform.copy();

		setTopObject(thePlatform);
		transportTableModel.setPlatform(thePlatform);
		sensorTableModel.setPlatform(thePlatform, refConfig);
		lastModifiedField.setText(
				thePlatform.lastModifyTime == null ? "" :
					decodes.db.Constants.defaultDateFormat.format(
							thePlatform.lastModifyTime).toString());
		return true;
	}

	/** 
	  Moves data from fields back into the Platform. 
	  @return the internal copy of the platform being edited.
	 */
	public Platform getDataFromFields()
	{
		thePlatform.description = descriptionArea.getText();
		if (TextUtil.isAllWhitespace(thePlatform.description))
			thePlatform.description = null;
		thePlatform.agency = ownerAgencyField.getText();
		thePlatform.isProduction = isProductionCheck.isSelected();
		String d = designatorField.getText().trim();
		thePlatform.setPlatformDesignator(d.length() == 0 ? null : d);
		return thePlatform;
	}

	/**
	 * (from EntityOpsController interface)
	 */
	public String getEntityName() { return "Platform"; }

	/**
	 * (from EntityOpsController interface)
	 */
	public void commitEntity()
	{
		saveChanges();
		Database.getDb().platformConfigList.countPlatformsUsing();
	}

	/**
	 * (from EntityOpsController interface)
	 */
	public void closeEntity()
	{
		if (hasChanged())
		{
			int r = JOptionPane.showConfirmDialog(this,
					genericLabels.getString("saveChanges"));
			if (r == JOptionPane.CANCEL_OPTION)
				return;
			else if (r == JOptionPane.YES_OPTION)
			{
				if (!saveChanges())
					return;
			}
			else if (r == JOptionPane.NO_OPTION)

			{
				if (!thePlatform.idIsSet())
				{
					// This is a new platform never saved, and we will abandon.
					// Remove it from the list.
					PlatformListPanel plp = parent.getPlatformListPanel();
					plp.abandonNewPlatform(origPlatform);
				}
			}
		}
		DbEditorTabbedPane platformsTabbedPane 
		= parent.getPlatformsTabbedPane();
		platformsTabbedPane.remove(this);
	}

	/**
	 * Called from the File - Close All menu item to close and possibly
	 * abandon any changes.
	 */
	public void forceClose()
	{
		if (!thePlatform.idIsSet())
		{
			// This is a new platform never saved, and we will abandon.
			// Remove it from the list.
			PlatformListPanel plp = parent.getPlatformListPanel();
			plp.abandonNewPlatform(origPlatform);
		}
		DbEditorTabbedPane platformsTabbedPane 
		= parent.getPlatformsTabbedPane();
		platformsTabbedPane.remove(this);
	}

	/**
	 * (from EntityOpsController interface)
	 */
	public void help()
	{
	}

	/**
	  From the ConfigSelectController interface, called when user selects
	  a new configuration to be associated with this platform.
	  @param config the new configuration.
	 */
	public void selectConfig(PlatformConfig config)
	{
		int r = JOptionPane.YES_OPTION;
		if (refConfig != null)
		{
			r = JOptionPane.showConfirmDialog(this,
					dbeditLabels.getString("PlatformEditPanel.confirmConfig"),
					dbeditLabels.getString("PlatformEditPanel.confirmConfigTitle"),
					JOptionPane.YES_NO_OPTION);
		}

		if (r == JOptionPane.YES_OPTION)
		{
			try { config.read(); }
			catch(DatabaseException ex)
			{
				parent.showError(
						LoadResourceBundle.sprintf(
								dbeditLabels.getString(
								"PlatformEditPanel.configReadError"),
								config.configName)
								+ ex);
			}
			refConfig = config;
			thePlatform.setConfigName(refConfig.configName);
			configField.setText(refConfig.configName);
			sensorTableModel.setPlatform(thePlatform, refConfig);
			sensorTableModel.fillValues();
		}
	}

	void platformPropertiesButton_actionPerformed(ActionEvent e)
	{
		PropertiesEditDialog dlg = new PropertiesEditDialog(
				genericLabels.getString("platform")+ " "
				+ entityName(), thePlatform.getProperties());
		dlg.setPropertiesOwner(this.thePlatform);
		launchDialog(dlg);
	}
}

/**
Table model for the transport media list.
 */
class TransportListTableModel extends AbstractTableModel
{
	static String columnNames[] =
	{
		PlatformEditPanel.genericLabels.getString("type"),
		PlatformEditPanel.genericLabels.getString("ID"),
		PlatformEditPanel.dbeditLabels.getString("PlatformEditPanel.scriptName"),
		PlatformEditPanel.genericLabels.getString("Selector")
	};
	private Vector media;

	/** Construct new table model */
	public TransportListTableModel()
	{
		media = new Vector();
	}

	/** sets the platform */
	void setPlatform(Platform p)
	{
		media = p.transportMedia;
		fireTableDataChanged();
	}

	/** @return number of transport media */
	public int getRowCount()
	{
		return media.size();
	}

	/** @return number of columns */
	public int getColumnCount() { return columnNames.length; }

	/**
	  @return the value at specified row/column as a string
	  @param r the row
	  @param c the column
	 */
	public Object getValueAt(int r, int c)
	{
		TransportMedium tm = (TransportMedium)media.elementAt(r);
		switch(c)
		{
		case 0: return tm.getMediumType();
		case 1: return tm.getMediumId();
		case 2:
			return tm.scriptName == null ? "" : tm.scriptName;
		case 3:
			return tm.getSelector();
		default:
			return "";
		}
	}

	/** @return name of specified column */
	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	/** @returns transport medium at specified row. */
	TransportMedium getObjectAt(int r)
	{
		if (r < 0 || r >= media.size())
			return null;
		return (TransportMedium)media.elementAt(r);
	}

	/** 
	  Adds new transport medium to the vector.
	  @param ob the TM to add.
	 */
	void add(TransportMedium ob)
	{
		media.add(ob);
		fireTableDataChanged();
	}

	/**
	  Removes a transport medium from the vector
	  @param ob the TM to remove
	 */
	void remove(TransportMedium ob)
	{
		media.remove(ob);
		fireTableDataChanged();
	}
}

/**
Model for the table of sensor information
 */
class SensorInfoTableModel extends AbstractTableModel
{
	static String columnNames[] =
	{
		PlatformEditPanel.genericLabels.getString("sensor"),
		PlatformEditPanel.genericLabels.getString("name"),
		PlatformEditPanel.dbeditLabels.getString("PlatformEditPanel.actualSite"),
		PlatformEditPanel.genericLabels.getString("properties")
	};

	/** reference to platform */
	private Platform thePlatform;
	private PlatformConfig refConfig;

	/** construct new empty model */
	public SensorInfoTableModel()
	{
		thePlatform = null;
	}

	/** 
	  Sets the platform being edited. 
	  @param p the platform
	 */
	void setPlatform(Platform p, PlatformConfig pc)
	{
		thePlatform = p;
		refConfig = pc;
		fillValues();
	}

	public void fillValuesNew()
	{
		for(Iterator it = thePlatform.getPlatformSensors(); it.hasNext(); )
		{
			PlatformSensor ps = (PlatformSensor)it.next();
			ps.setUsgsDdno(0);
			ps.clearValidDdnos();
		}
		fillValues();
	}
	/** Fills model with values from the platform. */
	public void fillValues()
	{
		if (thePlatform == null)
			return;
		// First clear the flags in each platform sensor.
		for(Iterator it = thePlatform.getPlatformSensors(); it.hasNext(); )
		{
			PlatformSensor ps = (PlatformSensor)it.next();
			ps.guiCheck = false;
		}

		// Add or check-off PlatformSensors based on the ConfigSensors.
		int ncs = 0;
		if (refConfig != null)
		{
			ncs = refConfig.getNumSensors();
			for(Iterator it = refConfig.getSensors(); it.hasNext(); )
			{
				ConfigSensor cs = (ConfigSensor)it.next();
				PlatformSensor ps =
					thePlatform.getPlatformSensor(cs.sensorNumber);
				if (ps != null)
				{
					Logger.instance().debug3("Using existing platform sensor #" + cs.sensorNumber);
					ps.guiCheck = true;
				}
				else // (ps == null)
				{
					Logger.instance().debug3("Adding new sensor from config #" + cs.sensorNumber + ", " + cs.sensorName);
					ps = new PlatformSensor(thePlatform, cs.sensorNumber);
					thePlatform.addPlatformSensor(ps);
					ps.guiCheck = true;
				}
				if ( ps != null ) {
					if ( ps.getUsgsDdno() == 0 ) {
						String ddids[] = ps.getValidDdnos();
						if ( ddids.length == 1 ) {
							int ddid = Integer.parseInt(ddids[0].substring(0,ddids[0].indexOf('-')).trim());
							ps.setUsgsDdno(ddid);
						}
					} 			
				}
			}
		}

		// Finally remove any platform sensors not checked-off, meaning that
		// the corresponding config sensor no longer exists.
		for(Iterator it = thePlatform.getPlatformSensors(); it.hasNext(); )
		{
			PlatformSensor ps = (PlatformSensor)it.next();
			if (!ps.guiCheck)
				it.remove();
		}

		fireTableDataChanged();
	}

	/** @return number of rows (sensors) */
	public int getRowCount()
	{
		return thePlatform == null ? 0 : thePlatform.platformSensors.size();
	}

	/** @return number of columns */
	public int getColumnCount() { return columnNames.length; }

	/** 
	  @return value at specified row/column as a string. 
	  @param r row
	  @param c column
	 */
	public Object getValueAt(int r, int c)
	{
		PlatformSensor ps = getObjectAt(r);
		switch(c)
		{
		case 0: return "" + ps.sensorNumber;
		case 1:
		{
			if (refConfig == null)
				return "null";
			ConfigSensor cs = refConfig.getSensor(ps.sensorNumber);
			return cs == null ? "" : cs.sensorName;
		}
		case 2:
			if (ps.site == null)
				return "";
			else
			{
				SiteName sn = ps.site.getPreferredName();
				return
					sn == null ? "" :
					sn.getNameType() + ":" + sn.getNameValue();
			}
		case 3:
		{
			int usgsDdno = ps.getUsgsDdno();
			String rs = usgsDdno == 0 ? "" : ("DDNO="+usgsDdno+", ");
			return rs + PropertiesUtil.props2string(ps.getProperties());
		}
		default:
			return "";
		}
	}

	/**
	  @return name of specified column.
	  @param col the column
	 */
	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	/**
	  @return PlatformSensor object at specified row.
	  @param r the row.
	 */
	PlatformSensor getObjectAt(int r)
	{
		if (r < 0 || r >= thePlatform.platformSensors.size())
			return null;
		return (PlatformSensor)thePlatform.platformSensors.elementAt(r);
	}

	/**
	  Adds a PlatformSensor to the model.
	  @param ob the PlatformSensor
	 */
	void add(PlatformSensor ob)
	{
		thePlatform.platformSensors.add(ob);
	}

	/**
	  Removes a PlatformSensor from the model.
	  @param ob the PlatformSensor
	 */
	void remove(PlatformSensor ob)
	{
		thePlatform.platformSensors.remove(ob);
	}
}

/**
 * Used for detecting associations to network lists when a TM is changed.
 * @author mmaloney
 *
 */
class ListTmAssoc
{
	TransportMedium oldTm;
	TransportMedium newTm;
	NetworkList netlist;
	
	public ListTmAssoc(TransportMedium oldTm, TransportMedium newTm, NetworkList netlist)
	{
		super();
		this.oldTm = oldTm;
		this.newTm = newTm;
		this.netlist = netlist;
	}
}
