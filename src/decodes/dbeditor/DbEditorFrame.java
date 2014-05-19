/*
*	$Id$
*/

package decodes.dbeditor;

import java.awt.*;
import java.awt.event.*;
import java.util.ResourceBundle;
import javax.swing.*;

import ilex.util.Logger;
import ilex.util.LoadResourceBundle;
import decodes.gui.*;
import decodes.sql.DecodesDatabaseVersion;
import decodes.util.DecodesSettings;
import decodes.util.ResourceFactory;
import decodes.db.Database;

/**
This is the JFrame that encloses the entire DECODES Database Editor.
It contains nested tabbed panes for each of the types of objects.
*/
public class DbEditorFrame extends decodes.gui.TopFrame
{
	JPanel contentPane;
	JMenuBar jMenuBar1 = new JMenuBar();
	JMenu jMenuFile = new JMenu();
	JMenuItem jMenuFileSaveAll = new JMenuItem();
	JMenuItem jMenuFileCloseAll = new JMenuItem();
	JMenuItem jMenuFileImport = new JMenuItem();
	JMenuItem jMenuFileExport = new JMenuItem();
	JMenuItem jMenuFileExit = new JMenuItem();
	JMenu jMenuHelp = new JMenu();
	JMenuItem jMenuHelpAbout = new JMenuItem();
	JLabel statusBar = new JLabel();
	JTabbedPane topLevelTabs = new JTabbedPane();
	JPanel sitesTab = new JPanel();
	JPanel platformsTab = new JPanel();
	JPanel configsTab = new JPanel();
	JPanel equipmentTab = new JPanel();
	JPanel presentationTab = new JPanel();
	JPanel routingTab = new JPanel();
	DbEditorTabbedPane sitesTabbedPane = new DbEditorTabbedPane();
	JPanel sitesListTab = new JPanel();
	JPanel siteEditTab = new JPanel();
	DbEditorTabbedPane equipmentTabbedPane = new DbEditorTabbedPane();
	JPanel equipmentListTab = new JPanel();
	EquipmentListPanel equipmentListPanel = new EquipmentListPanel();
	DbEditorTabbedPane platformsTabbedPane = new DbEditorTabbedPane();
	JPanel platformsListTab = new JPanel();
	PlatformListPanel platformListPanel = new PlatformListPanel();
	DbEditorTabbedPane configsTabbedPane = new DbEditorTabbedPane();
	JPanel configsListTab = new JPanel();
	ConfigsListPanel configsListPanel = new ConfigsListPanel();
	JPanel configEditTab = new JPanel();
	DbEditorTabbedPane presentationTabbedPane = new DbEditorTabbedPane();
	JPanel presentationListTab = new PresentationGroupListPanel();
	PresentationGroupListPanel presentationGroupListPanel = new PresentationGroupListPanel();
	DbEditorTabbedPane routingTabbedPane = new DbEditorTabbedPane();
	JPanel routingListTab = new JPanel();
	RoutingSpecListPanel routingSpecListPanel = new RoutingSpecListPanel();
	JPanel sourcesTab = new JPanel();
	JPanel netlistTab = new JPanel(new BorderLayout());
	DbEditorTabbedPane netlistTabbedPane = new DbEditorTabbedPane();
	JPanel netlistListTab = new JPanel();
	NetlistListPanel netlistListPanel = new NetlistListPanel();
	DbEditorTabbedPane sourcesTabbedPane = new DbEditorTabbedPane();
	JPanel sourcesListTab = new JPanel();
	SourcesListPanel sourcesListPanel = new SourcesListPanel();
	SiteListPanel siteListPanel = new SiteListPanel();
	
	JPanel scheduleTab = new JPanel(new BorderLayout());
	JPanel scheduleListTab = new JPanel(new BorderLayout());
	DbEditorTabbedPane scheduleTabbedPane = new DbEditorTabbedPane();
	ScheduleListPanel scheduleListPanel = null;

	private static DbEditorFrame _instance = null;
	private static ResourceBundle genericLabels = null;
	private static ResourceBundle dbeditLabels = null;

	/**Construct the frame*/
	public DbEditorFrame()
	{
		exitOnClose = true;

		genericLabels = DecodesDbEditor.getGenericLabels();
		dbeditLabels = DecodesDbEditor.getDbeditLabels();

		try { jbInit(); }
		catch(Exception e)
		{
				e.printStackTrace();
		}
		siteListPanel.setParent(this);
		platformListPanel.setParent(this);
		configsListPanel.setParent(this);
		equipmentListPanel.setParent(this);
		presentationGroupListPanel.setParent(this);
		sourcesListPanel.setParent(this);
		netlistListPanel.setParent(this);
		routingSpecListPanel.setParent(this);

		// Default operation is to do nothing when user hits 'X' in upper
		// right to close the window. We will catch the closing event and
		// do the same thing as if user had hit File - Exit.
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(
			new WindowAdapter()
			{
				public void windowClosing(WindowEvent e)
				{
					jMenuFileExit_actionPerformed(null);
				}
			});
		_instance = this;
		trackChanges("DbEditFrame");
	}

	public static DbEditorFrame instance() { return _instance; }

	/**
	 * @return resource bundle containing generic labels for the selected
	 * language.
	 */
	public static ResourceBundle getGenericLabels() 
	{
		if (genericLabels == null)
		{
			genericLabels = DecodesDbEditor.getGenericLabels();
		}
		return genericLabels;
	}

	/**
	 * @return resource bundle containing DB-Editor labels for the selected
	 * language.
	 */
	public static ResourceBundle getDbeditLabels()
	{
		if (dbeditLabels == null)
		{
			dbeditLabels = DecodesDbEditor.getDbeditLabels();
		}
		return dbeditLabels;
	}

	/**Component initialization*/
	private void jbInit() 
		throws Exception	
	{
		//setIconImage(
		//	Toolkit.getDefaultToolkit().createImage(
		//		DbEditorFrame.class.getResource("[Your Icon]")));
		contentPane = (JPanel) this.getContentPane();
		contentPane.setLayout(new BorderLayout());
		this.setSize(new Dimension(900, 850));
		this.setTitle(dbeditLabels.getString("dbedit.frameTitle"));
		statusBar.setText(" ");
		jMenuFile.setText(genericLabels.getString("file"));
		jMenuFileExit.setText(genericLabels.getString("exit"));
		jMenuFileExit.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					jMenuFileExit_actionPerformed(e);
				}
			});
		jMenuFileSaveAll.setText(genericLabels.getString("commitAll"));
		jMenuFileSaveAll.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					jMenuFileSaveAll_actionPerformed();
				}
			});
		jMenuFileSaveAll.setToolTipText(
			dbeditLabels.getString("dbedit.commitAllMenuToolTip"));
		jMenuFileCloseAll.setText(genericLabels.getString("closeAll"));
		jMenuFileCloseAll.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					jMenuFileCloseAll_actionPerformed();
				}
			});
		jMenuFileCloseAll.setToolTipText(
			dbeditLabels.getString("dbedit.fileCloseAllMenuToolTip"));
		jMenuFileImport.setText(genericLabels.getString("import"));
		jMenuFileImport.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					jMenuFileImport_actionPerformed();
				}
			});
		jMenuFileImport.setToolTipText(
			dbeditLabels.getString("dbedit.fileImportMenuToolTip"));
		jMenuFileExport.setText(genericLabels.getString("export"));
		jMenuFileExport.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					jMenuFileExport_actionPerformed();
				}
			});
		jMenuFileExport.setToolTipText(
			dbeditLabels.getString("dbedit.fileExportMenuToolTip"));

		jMenuHelp.setText(genericLabels.getString("help"));
		jMenuHelpAbout.setText(genericLabels.getString("about"));
		jMenuHelpAbout.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e) {
				jMenuHelpAbout_actionPerformed(e);
			}
		});
		sitesTab.setLayout(new BorderLayout());
		platformsTab.setLayout(new BorderLayout());
		configsTab.setLayout(new BorderLayout());
		equipmentTab.setLayout(new BorderLayout());
		presentationTab.setLayout(new BorderLayout());
		routingTab.setLayout(new BorderLayout());
		sitesListTab.setLayout(new BorderLayout());
		siteEditTab.setLayout(new BorderLayout());
		equipmentListTab.setLayout(new BorderLayout());
		platformsListTab.setLayout(new BorderLayout());
		configsListTab.setLayout(new BorderLayout());
		configEditTab.setLayout(new BorderLayout());
		presentationListTab.setLayout(new BorderLayout());
		routingListTab.setLayout(new BorderLayout());
		netlistListTab.setLayout(new BorderLayout());
		sourcesTab.setLayout(new BorderLayout());
		sourcesListTab.setLayout(new BorderLayout());
		contentPane.setPreferredSize(new Dimension(850, 1236));
		jMenuFile.add(jMenuFileSaveAll);
		jMenuFile.add(jMenuFileCloseAll);
		jMenuFile.addSeparator();
		jMenuFile.add(jMenuFileImport);
		jMenuFile.add(jMenuFileExport);
		jMenuFile.addSeparator();
		jMenuFile.add(jMenuFileExit);
		jMenuHelp.add(jMenuHelpAbout);
		jMenuBar1.add(jMenuFile);
		jMenuBar1.add(jMenuHelp);
		this.setJMenuBar(jMenuBar1);

		contentPane.add(statusBar, BorderLayout.SOUTH);
		contentPane.add(topLevelTabs, BorderLayout.CENTER);
		sitesTab.add(sitesTabbedPane, BorderLayout.CENTER);
		sitesTabbedPane.add(sitesListTab, 
			genericLabels.getString("list"));
		sitesListTab.add(siteListPanel, BorderLayout.CENTER);
		topLevelTabs.add(platformsTab,
			dbeditLabels.getString("dbedit.platformsTabLabel"));
		topLevelTabs.add(sitesTab,
			dbeditLabels.getString("dbedit.sitesTabLabel"));
		platformsTab.add(platformsTabbedPane, BorderLayout.CENTER);
		platformsTabbedPane.add(platformsListTab,
			genericLabels.getString("list"));
		platformsListTab.add(platformListPanel, BorderLayout.CENTER);
		topLevelTabs.add(configsTab,
			dbeditLabels.getString("dbedit.configsTabLabel"));
		configsTab.add(configsTabbedPane, BorderLayout.CENTER);
		configsTabbedPane.add(configsListTab, 
			genericLabels.getString("list"));
		configsListTab.add(configsListPanel, BorderLayout.CENTER);
		topLevelTabs.add(equipmentTab,
			dbeditLabels.getString("dbedit.equipmentTabLabel"));
		equipmentTab.add(equipmentTabbedPane, BorderLayout.CENTER);
		equipmentTabbedPane.add(equipmentListTab,
			genericLabels.getString("list"));
		equipmentListTab.add(equipmentListPanel, BorderLayout.CENTER);
		topLevelTabs.add(presentationTab,
			dbeditLabels.getString("dbedit.presentationTabLabel"));
		presentationTab.add(presentationTabbedPane, BorderLayout.CENTER);
		presentationTabbedPane.add(presentationListTab,
			genericLabels.getString("list"));
		presentationListTab.add(presentationGroupListPanel,BorderLayout.CENTER);
		topLevelTabs.add(routingTab,
			dbeditLabels.getString("dbedit.routingTabLabel"));
		routingTab.add(routingTabbedPane, BorderLayout.CENTER);
		routingTabbedPane.add(routingListTab,
			genericLabels.getString("list"));
		routingListTab.add(routingSpecListPanel, BorderLayout.CENTER);
		topLevelTabs.add(sourcesTab,
			dbeditLabels.getString("dbedit.sourcesTabLabel"));
		sourcesTab.add(sourcesTabbedPane, BorderLayout.CENTER);
		sourcesTabbedPane.add(sourcesListTab,
			genericLabels.getString("list"));
		sourcesListTab.add(sourcesListPanel, BorderLayout.CENTER);
		
		topLevelTabs.add(netlistTab,
			dbeditLabels.getString("dbedit.netlistsTabLabel"));
		netlistTab.add(netlistTabbedPane, BorderLayout.CENTER);
		netlistTabbedPane.add(netlistListTab,
			genericLabels.getString("list"));
		netlistListTab.add(netlistListPanel, BorderLayout.CENTER);

		if (DecodesSettings.instance().editDatabaseTypeCode == DecodesSettings.DB_XML
		 || Database.getDb().getDbIo().getDecodesDatabaseVersion() >=
		 		DecodesDatabaseVersion.DECODES_DB_10)
		{
			scheduleListPanel = new ScheduleListPanel();
			scheduleListPanel.setParent(this);
			topLevelTabs.add(scheduleTab,
				dbeditLabels.getString("ScheduleEntryPanel.EntityName"));
			scheduleTab.add(scheduleTabbedPane, BorderLayout.CENTER);
			scheduleTabbedPane.add(scheduleListTab,
				genericLabels.getString("list"));
			scheduleListTab.add(scheduleListPanel, BorderLayout.CENTER);
		}
	}

	/**File | Exit action performed*/
	public void jMenuFileExit_actionPerformed(ActionEvent e) 
	{
		DbEditorTab openEd;
		if ((openEd = sitesTabbedPane.findFirstOpenEditor()) != null)
		{
			topLevelTabs.setSelectedComponent(sitesTab);
			sitesTabbedPane.setSelectedComponent(openEd);
		}
		else if ((openEd = platformsTabbedPane.findFirstOpenEditor()) != null)
		{
			topLevelTabs.setSelectedComponent(platformsTab);
			platformsTabbedPane.setSelectedComponent(openEd);
		}
		else if ((openEd = configsTabbedPane.findFirstOpenEditor()) != null)
		{
			topLevelTabs.setSelectedComponent(configsTab);
			configsTabbedPane.setSelectedComponent(openEd);
		}
		else if ((openEd = equipmentTabbedPane.findFirstOpenEditor()) != null)
		{
			topLevelTabs.setSelectedComponent(equipmentTab);
			equipmentTabbedPane.setSelectedComponent(openEd);
		}
		else if ((openEd = presentationTabbedPane.findFirstOpenEditor())!= null)
		{
			topLevelTabs.setSelectedComponent(presentationTab);
			presentationTabbedPane.setSelectedComponent(openEd);
		}
		else if ((openEd = routingTabbedPane.findFirstOpenEditor()) != null)
		{
			topLevelTabs.setSelectedComponent(routingTab);
			routingTabbedPane.setSelectedComponent(openEd);
		}
		else if ((openEd = sourcesTabbedPane.findFirstOpenEditor()) != null)
		{
			topLevelTabs.setSelectedComponent(sourcesTab);
			sourcesTabbedPane.setSelectedComponent(openEd);
		}
		else if ((openEd = netlistTabbedPane.findFirstOpenEditor()) != null)
		{
			topLevelTabs.setSelectedComponent(netlistTab);
			netlistTabbedPane.setSelectedComponent(openEd);
		}

		if (openEd != null)
		{
			showError(
				dbeditLabels.getString("dbedit.errmsgPleaseClose"));
		}
		else if (exitOnClose) 
		{
			Database db = Database.getDb();
			db.getDbIo().close();
			System.exit(0);
		}
		else
			dispose();
	}

	/**Help | About action performed
		@param e ignored.
	*/
	public void jMenuHelpAbout_actionPerformed(ActionEvent e) 
	{
		JDialog dlg = ResourceFactory.instance().getAboutDialog(
			this, "DBEDIT", "DECODES DB Editor");
		Dimension dlgSize = dlg.getPreferredSize();
		Dimension frmSize = getSize();
		Point loc = getLocation();
		dlg.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x, 
			(frmSize.height - dlgSize.height) / 2 + loc.y);
		dlg.setVisible(true);
	}

	/** @return the tabbed pane for Sites. */
	public DbEditorTabbedPane getSitesTabbedPane()
	{
		return sitesTabbedPane;
	}

	/** @return the tabbed pane for Platform. */
	public DbEditorTabbedPane getPlatformsTabbedPane()
	{
		return platformsTabbedPane;
	}

	/** @return the tabbed pane for Configs. */
	public DbEditorTabbedPane getConfigsTabbedPane()
	{
		return configsTabbedPane;
	}

	/** @return the PlatformListPanel. */
	public PlatformListPanel getPlatformListPanel()
	{
		return platformListPanel;
	}

	/** @return the SiteListPanel. */
	public SiteListPanel getSiteListPanel()
	{
		return siteListPanel;
	}

	/** @return the ConfigListPanel. */
	public ConfigsListPanel getConfigsListPanel()
	{
		return configsListPanel;
	}

	/** @return the tabbed pane for EquipmentModels. */
	public DbEditorTabbedPane getEquipmentTabbedPane()
	{
		return equipmentTabbedPane;
	}

	/** @return the EquipmentListPanel. */
	public EquipmentListPanel getEquipmentListPanel()
	{
		return equipmentListPanel;
	}

	/** @return the PresentationGroupListPanel. */
	public PresentationGroupListPanel getPresentationGroupListPanel()
	{
		return presentationGroupListPanel;
	}

	/** @return the tabbed pane for Presentation Groups. */
	public DbEditorTabbedPane getPresentationTabbedPane()
	{
		return presentationTabbedPane;
	}

	/** @return the SourcesListPanel. */
	public SourcesListPanel getSourcesListPanel()
	{
		return sourcesListPanel;
	}

	/** @return the tabbed pane for Data Sources. */
	public DbEditorTabbedPane getSourcesTabbedPane()
	{
		return sourcesTabbedPane;
	}

	/** @return the NetlistListPanel. */
	public NetlistListPanel getNetlistListPanel()
	{
		return netlistListPanel;
	}

	/** @return the tabbed pane for Network Lists. */
	public DbEditorTabbedPane getNetworkListTabbedPane()
	{
		return netlistTabbedPane;
	}

	/** @return the RoutingSpecListPanel. */
	public RoutingSpecListPanel getRoutingSpecListPanel()
	{
		return routingSpecListPanel;
	}

	/** @return the tabbed pane for Routing Specs. */
	public DbEditorTabbedPane getRoutingSpecTabbedPane()
	{
		return routingTabbedPane;
	}
	
	public ScheduleListPanel getScheduleListPanel()
	{
		return scheduleListPanel;
	}

	/** @return the tabbed pane for Network Lists. */
	public DbEditorTabbedPane getScheduleListTabbedPane()
	{
		return scheduleTabbedPane;
	}


	private void jMenuFileSaveAll_actionPerformed()
	{
		sitesTabbedPane.saveAll();
		equipmentTabbedPane.saveAll();
		configsTabbedPane.saveAll();
		platformsTabbedPane.saveAll();
		presentationTabbedPane.saveAll();
		routingTabbedPane.saveAll();
		sourcesTabbedPane.saveAll();
		netlistTabbedPane.saveAll();
	}

	private void jMenuFileCloseAll_actionPerformed()
	{
		platformsTabbedPane.closeAll();
		sitesTabbedPane.closeAll();
		configsTabbedPane.closeAll();
		equipmentTabbedPane.closeAll();
		presentationTabbedPane.closeAll();
		routingTabbedPane.closeAll();
		sourcesTabbedPane.closeAll();
		netlistTabbedPane.closeAll();
	}

	private void jMenuFileImport_actionPerformed()
	{
		ImportDialog dlg = new ImportDialog();
		dlg.setParent(this);
		launchDialog(dlg);
	}

	private void jMenuFileExport_actionPerformed()
	{
		ExportDialog dlg = new ExportDialog();
		launchDialog(dlg);
	}

	public void activateConfigsTab()
	{
		topLevelTabs.setSelectedComponent(configsTab);
	}
}
