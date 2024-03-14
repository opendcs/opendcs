package decodes.dbeditor;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import javax.xml.parsers.ParserConfigurationException;

import org.opendcs.database.SimpleDataSource;
import org.xml.sax.SAXException;

import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.JScrollPane;
import javax.swing.JFileChooser;

import ilex.gui.CheckableItem;
import ilex.gui.CheckBoxList;
import ilex.gui.JobDialog;
import ilex.util.AsciiUtil;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.LoadResourceBundle;
import ilex.xml.XmlOutputStream;
import ilex.xml.XmlObjectWriter;

import decodes.db.*;
import decodes.gui.TopFrame;
import decodes.gui.GuiDialog;
import decodes.util.DecodesException;
import decodes.xml.DatabaseParser;
import decodes.xml.PlatformParser;
import decodes.xml.XmlDbTags;
import decodes.xml.XmlDatabaseIO;
import decodes.xml.TopLevelParser;


/**
This class implements the dialog displayed for File - Export.
*/
public class ImportDialog extends GuiDialog
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();
	
	
	private JPanel topLevelPanel = new JPanel();
	private BorderLayout topLevelBorderLayout = new BorderLayout();

	private JPanel fileSelectPanel = new JPanel();
	private GridBagLayout fileSelectGBLayout = new GridBagLayout();
	private JLabel ftiLabel = new JLabel(  
			dbeditLabels.getString("ImportDialog.FileLabel"));
	private JTextField ftiField = new JTextField();
	private JButton browseButton = new JButton(  
			dbeditLabels.getString("ImportDialog.Browse"));
	private JButton scanButton = new JButton(  
			dbeditLabels.getString("ImportDialog.ScanButton"));

	private JPanel recordSelectPanel = new JPanel();
	private BorderLayout recordSelectBorderLayout = new BorderLayout();
	private Border rseb = BorderFactory.createEtchedBorder(Color.white,
		new Color(165, 163, 151));
	private Border recordSelectBorder = 
		new TitledBorder(rseb,   
				dbeditLabels.getString("ImportDialog.ImportBorder"));
	private JPanel allNonePanel = new JPanel();
	private FlowLayout allNoneFlowLayout = new FlowLayout(
		java.awt.FlowLayout.LEFT, 15, 5);
	private JButton allButton = new JButton(   
			dbeditLabels.getString("ImportDialog.AllButton"));
	private JButton noneButton = new JButton(   
			dbeditLabels.getString("ImportDialog.NoneButton"));

	private JScrollPane objectListScrollPane = new JScrollPane();

	private CheckBoxList objectList = new CheckBoxList();

	private JPanel okCancelPanel = new JPanel();
	private FlowLayout okCancelFlowLayout = new FlowLayout(
		java.awt.FlowLayout.CENTER, 15, 5);
	private JButton importButton = new JButton(   
			dbeditLabels.getString("ImportDialog.ImportButton"));
	private JButton cancelButton = new JButton(   
			dbeditLabels.getString("ImportDialog.CancelButton"));

	static private JFileChooser fileChooser = new JFileChooser(
		EnvExpander.expand("$DECODES_INSTALL_DIR"));

	private Database stageDb = null;
	private XmlDatabaseIO stageDbIo = null;
	private TopLevelParser topParser = null;
	Database editDb;

	DbEditorFrame parent = null;

	/**
	 * Constructor.
	 */
	public ImportDialog()
	{
		super(getDbEditFrame(),    
				dbeditLabels.getString("ImportDialog.Title"), 
			true);
		try
		{
			setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			jbInit();
			pack();
		}
		catch (Exception exception)
		{
			exception.printStackTrace();
		}
	}

	public void setParent(DbEditorFrame parent) { this.parent = parent; }

	private void jbInit() throws Exception
	{
		getContentPane().add(topLevelPanel);
		topLevelPanel.setLayout(topLevelBorderLayout);
		topLevelPanel.add(fileSelectPanel, java.awt.BorderLayout.NORTH);
		topLevelPanel.add(recordSelectPanel, java.awt.BorderLayout.CENTER);
		topLevelPanel.add(okCancelPanel, java.awt.BorderLayout.SOUTH);

		fileSelectPanel.setLayout(fileSelectGBLayout);
		fileSelectPanel.add(ftiLabel,
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 25, 5, 2), 0, 0));
		fileSelectPanel.add(ftiField,
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 0, 5, 0), 0, 0));
		ftiField.setPreferredSize(new Dimension(260, 27));
		fileSelectPanel.add(browseButton,
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 5, 5, 5), 0, 0));
		fileSelectPanel.add(scanButton,
			new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 5, 5, 5), 0, 0));
		scanButton.setToolTipText(    
				dbeditLabels.getString("ImportDialog.ScanButtonTT"));

		recordSelectPanel.setLayout(recordSelectBorderLayout);
		recordSelectPanel.setBorder(recordSelectBorder);
		recordSelectPanel.add(allNonePanel, java.awt.BorderLayout.NORTH);
		recordSelectPanel.add(objectListScrollPane, 
			java.awt.BorderLayout.CENTER);

		allNonePanel.setLayout(allNoneFlowLayout);
		allNonePanel.add(allButton);
		allNonePanel.add(noneButton);
		allButton.setToolTipText(    
				dbeditLabels.getString("ImportDialog.AllButtonTT"));
		noneButton.setToolTipText(    
				dbeditLabels.getString("ImportDialog.NoneButtonTT"));

		objectListScrollPane.getViewport().add(objectList);

		okCancelPanel.setLayout(okCancelFlowLayout);
		okCancelPanel.add(importButton);
		okCancelPanel.add(cancelButton);
		importButton.setToolTipText(
				dbeditLabels.getString("ImportDialog.ImportButtonTT"));
		
		browseButton.setPreferredSize(new Dimension(100, 27));
		browseButton.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					browseButtonPressed();
				}
			});
		scanButton.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					scanButtonPressed();
				}
			});
		allButton.setPreferredSize(new Dimension(100, 27));
		allButton.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					setAllSelections(true);
				}
			});
		allButton.setEnabled(true);
		noneButton.setPreferredSize(new Dimension(100, 27));
		noneButton.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					setAllSelections(false);
				}
			});
		noneButton.setEnabled(true);
		importButton.setPreferredSize(new Dimension(100, 27));
		importButton.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					importButtonPressed();
				}
			});
		cancelButton.setPreferredSize(new Dimension(100, 27));
		cancelButton.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					cancelButtonPressed();
				}
			});
	}


	private void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	private void browseButtonPressed()
	{
		if (fileChooser.showOpenDialog(this) == fileChooser.APPROVE_OPTION)
		{
			File f = fileChooser.getSelectedFile();
			ftiField.setText(f.getPath());
		}
	}

	private void scanButtonPressed()
	{
		String fn = ftiField.getText().trim();
		if (fn.length() == 0)
		{
			showError(
					dbeditLabels.getString("ImportDialog.ScanButtonError1"));
			return;
		}

		try
		{
			// Save the current (real) database
			editDb = Database.getDb();

			// Create temporary, empty database & set it as the default database
			initStageDb();

			// Use XML parsers to scan the selected file.
			Platform.configSoftLink = false;
			importFile(fn);
			Platform.configSoftLink = true;

			// Restore the current (real) database
			Database.setDb(editDb);

			// Populate the list model with the scanned items.
			populateList();
		}
		catch(Exception ex)
		{
			String msg = 
				dbeditLabels.getString("ImportDialog.ScanButtonError2")+" " + ex;
			Logger.instance().failure(msg);
			showError(msg);
		}
		finally
		{
			Database.setDb(editDb);
		}
	}

	/** 
	 * Initialize the staging database. XML files will be read into this. 
	 */
	private void initStageDb()
		throws SAXException, ParserConfigurationException, DatabaseException
	{
		Logger.instance().debug3(
				dbeditLabels.getString("ImportDialog.InitStageDebug"));
		stageDb = new decodes.db.Database();
		Database.setDb(stageDb);
		javax.sql.DataSource ds = new SimpleDataSource("", "", "");
		stageDbIo = new XmlDatabaseIO(ds);
		stageDb.setDbIo(stageDbIo);
		topParser = stageDbIo.getParser();

		// Copy the dataType, enums, & other 'setup' info into stageDb.
		for(Iterator it = editDb.enumList.iterator(); it.hasNext(); )
		{
			decodes.db.DbEnum en = (decodes.db.DbEnum)it.next();
			decodes.db.DbEnum stageEnum = new decodes.db.DbEnum(en.enumName);
			stageEnum.forceSetId(en.getId());
			stageEnum.setDefault(en.getDefault());
			for(Iterator vit = en.iterator(); vit.hasNext(); )
			{
				EnumValue ev = (EnumValue)vit.next();
				EnumValue stageEv = stageEnum.replaceValue(ev.getValue(), 
					ev.getDescription(), ev.getExecClassName(), ev.getEditClassName());
				stageEv.setSortNumber(ev.getSortNumber());
			}
		}
		stageDb.engineeringUnitList = editDb.engineeringUnitList;
		stageDb.dataTypeSet = editDb.dataTypeSet;
		stageDb.unitConverterSet = editDb.unitConverterSet;
	}

	private void importFile(String fn)
		throws SAXException, IOException
	{
		Logger.instance().info(
				dbeditLabels.getString("ImportDialog.ImportFileDialog") + fn + "'");
		DatabaseObject ob = topParser.parse(new File(fn));

		// Some file types are invalid in dbedit
		if (ob instanceof PlatformList)
		{
			showError(
					dbeditLabels.getString("ImportDialog.ImportFileError1"));
			return;
		}
		if (ob instanceof EnumList
		 || ob instanceof EngineeringUnitList)
		{
			showError(LoadResourceBundle.sprintf(
					dbeditLabels.getString("ImportDialog.ImportFileError2"),  ob.getObjectType()));
			return;
		}

		// Add entities to stage db.
		if (ob instanceof Platform)
		{
			Platform p = (Platform)ob;
			stageDb.platformList.add(p);
		}
		else if (ob instanceof Site)
			stageDb.siteList.addSite((Site)ob);
		else if (ob instanceof RoutingSpec)
			stageDb.routingSpecList.add((RoutingSpec)ob);
		else if (ob instanceof NetworkList)
			stageDb.networkListList.add((NetworkList)ob);
		else if (ob instanceof PresentationGroup)
			stageDb.presentationGroupList.add((PresentationGroup)ob);
	}

	private void populateList()
	{
		DefaultListModel listmod = objectList.getDefaultModel();
		listmod.clear();
		for(Iterator it = stageDb.platformList.iterator(); it.hasNext(); )
		{
			Platform p = (Platform)it.next();
			addDBO(p, false);
//			Site s = p.getSite();
//			if (s != null)
//				addDBO(s, true);
//			PlatformConfig pc = p.getConfig();
//			if (pc != null)
//				addDBO(pc, true);
		}
		for(Iterator it = stageDb.siteList.iterator(); it.hasNext(); )
		{
			Site p = (Site)it.next();
			addDBO(p, false);
		}
		for(PlatformConfig pc : stageDb.platformConfigList.values())
			addDBO(pc, false);
		for(Iterator it = stageDb.presentationGroupList.iterator(); 
			it.hasNext(); )
		{
			PresentationGroup p = (PresentationGroup)it.next();
			addDBO(p, false);
		}
		for(Iterator it = stageDb.dataSourceList.iterator(); 
			it.hasNext(); )
		{
			DataSource p = (DataSource)it.next();
			addDBO(p, false);
		}
		for(Iterator it = stageDb.routingSpecList.iterator(); it.hasNext(); )
		{
			RoutingSpec p = (RoutingSpec)it.next();
			addDBO(p, false);
		}
		for(Iterator it = stageDb.networkListList.iterator(); it.hasNext(); )
		{
			NetworkList p = (NetworkList)it.next();
			addDBO(p, false);
		}
	}

	private void addDBO(DatabaseObject dbo, boolean indent)
	{
		DefaultListModel listmod  = objectList.getDefaultModel();
		int n = listmod.size();
		for(int i=0; i<n; i++)
		{
			CheckableDatabaseObject cdo =
				(CheckableDatabaseObject)listmod.get(i);
			DatabaseObject ldbo = cdo.getDBO();
			if (dbo == ldbo
			 || (dbo.getObjectType().equals(ldbo.getObjectType())
			     && dbo.equals(ldbo)))
				return;
		}
		listmod.addElement(new CheckableDatabaseObject(dbo, indent));
	}

	private void setAllSelections(boolean selected)
	{
		objectList.selectAll(selected);
	}

	private void importButtonPressed()
	{
		DefaultListModel listmod  = objectList.getDefaultModel();
		int n = listmod.size();
		if (n == 0)
		{
			showError(
					dbeditLabels.getString("ImportDialog.ImportButtonError1"));
			return;
		}

		int nChecked = 0;
		for(int i=0; i<n; i++)
		{
			CheckableDatabaseObject cdo =
				(CheckableDatabaseObject)listmod.get(i);
			if (cdo.isSelected())
				nChecked++;
		}
		int maxRecs = 20;
		if (nChecked > maxRecs)
		{
			int r = JOptionPane.showConfirmDialog(this,
				AsciiUtil.wrapString(
					LoadResourceBundle.sprintf(
						dbeditLabels.getString(
							"ImportDialog.ImportButtonConfirm1"),
						maxRecs),
					60),
				dbeditLabels.getString("ImportDialog.ImportButtonConfirm2"),
				JOptionPane.YES_NO_OPTION);
			if (r == JOptionPane.YES_OPTION)
				importAll();
			return;
		}

		for(int i=0; i<n; i++)
		{
			CheckableDatabaseObject cdo =
				(CheckableDatabaseObject)listmod.get(i);
			if (cdo.isSelected())
			{
				DatabaseObject dbo = cdo.getDBO();
				DatabaseObject editDbo = openDBO(dbo);
				if (dbo instanceof Platform)
				{
					Platform editPlatform = (Platform)editDbo;
					Platform p = (Platform)dbo;
					Site s = p.getSite();
					if (s != null)
					{
						Site editSite = (Site)openDBO(s);
						editPlatform.setSite(editSite);
					}
					Platform.configSoftLink = false;
					PlatformConfig pc = p.getConfig();
					Platform.configSoftLink = true;
					if (pc != null)
						openDBO(pc);
					EquipmentModel em = pc.equipmentModel;
					if (em != null)
						openDBO(em);
				}
			}
		}
		JOptionPane.showMessageDialog(this, 
			AsciiUtil.wrapString(
					dbeditLabels.getString("ImportDialog.ImportButtonConfirm3")
					, 60), dbeditLabels.getString(
					"ImportDialog.ImportButtonConfirm4"), 
			JOptionPane.INFORMATION_MESSAGE);
		closeDlg();
	}
		
	/**
	 * Opens the database object for editing.
	 * The object may already exist in this database, in which case the
	 * existing object is opened and values from the passed object are added
	 * to the GUI controls.
	 * @return the actual database object opened, different from the one passed.
	 */
	private DatabaseObject openDBO(DatabaseObject dbo)
	{
		if (dbo instanceof Platform)
		{
			PlatformListPanel platformListPanel = parent.getPlatformListPanel();

			Platform impp = (Platform)dbo;
			Platform editp = 
				editDb.platformList.getByFileName(impp.makeFileName());
			PlatformEditPanel pep;
			if (editp != null)
{Logger.instance().debug3("Opening existing platform"+" " + impp.makeFileName());
				pep = platformListPanel.doOpen(editp);
}
			else
			{
Logger.instance().debug3("Opening panel for new platform."+" ");
				pep = platformListPanel.doNew();
			}
			pep.setImportedPlatform(impp);
			return pep.thePlatform;
		}
		else if (dbo instanceof PlatformConfig)
		{
			ConfigsListPanel clPanel = parent.getConfigsListPanel();

			PlatformConfig imppc = (PlatformConfig)dbo;
			PlatformConfig editpc = 
				editDb.platformConfigList.get(imppc.getName());
			if (editpc == null)
			{
				editpc = new PlatformConfig(imppc.getName());
				clPanel.configSelectPanel.addConfig(editpc);
			}
			ConfigEditPanel cep = clPanel.doOpen(editpc);
			cep.setImportedConfig(imppc);
			return editpc;
		}
		else if (dbo instanceof Site)
		{
			SiteListPanel siteListPanel = parent.getSiteListPanel();
			Site imps = (Site)dbo;
			Site edits = null;
			for(Iterator it = imps.getNames(); edits == null && it.hasNext(); )
			{
				SiteName sn = (SiteName)it.next();
				edits = editDb.siteList.getSite(sn);
			}
			if (edits == null)
			{
				try
				{
					edits = SiteNameEntryDialog.siteFactory.makeNewSite(
						imps.getPreferredName(), editDb);
					edits.isNew = true;
					editDb.siteList.addSite(edits);
					siteListPanel.siteSelectPanel.addSite(edits);
				}
				catch(DecodesException ex) 
				{
					showError(
							dbeditLabels.getString("ImportDialog.OpenError") + ex);
					return null;
				}
			}

			for(Iterator it = imps.getNames(); it.hasNext(); )
				edits.addName((SiteName)it.next());
			for(Enumeration pe = imps.getPropertyNames(); pe.hasMoreElements(); )
			{
				String nm = (String)pe.nextElement();
				edits.setProperty(nm, imps.getProperty(nm));
			}
			SiteEditPanel sep = siteListPanel.doOpen(edits);
			sep.fillFields(imps);
			return edits;
		}
		else if (dbo instanceof EquipmentModel)
		{
			EquipmentListPanel elPanel = parent.getEquipmentListPanel();

			EquipmentModel impem = (EquipmentModel)dbo;
			EquipmentModel editem = 
				editDb.equipmentModelList.get(impem.getName());
			if (editem == null)
			{
				editem = impem;
				editem.setDatabase(editDb);
				elPanel.equipmentModelSelectPanel.addEquipmentModel(editem);
			}
			EquipmentEditPanel eep = elPanel.doOpen(editem);
			eep.setImportedEquipmentModel(impem);
			return editem;
		}
		return null;
	}

	/**
	 * Unconditionally imports the database object.
	 * @param dbo the database object.
	 */
	private void importDBO(DatabaseObject dbo)
		throws DatabaseException
	{
		if (dbo instanceof Platform)
		{
			PlatformListPanel platformListPanel = parent.getPlatformListPanel();
			Platform impp = (Platform)dbo;
			Platform oldp = 
				editDb.platformList.getByFileName(impp.makeFileName());

			if (oldp != null)
			{
				oldp.copyFrom(impp);
				oldp.write();
			}
			else
			{
				impp.setDatabase(editDb);
				impp.clearId();
				impp.write();
				platformListPanel.platformSelectPanel.addPlatform(impp);
			}
		}
		else if (dbo instanceof PlatformConfig)
		{
			ConfigsListPanel clPanel = parent.getConfigsListPanel();

			PlatformConfig imppc = (PlatformConfig)dbo;
			PlatformConfig oldpc = 
				editDb.platformConfigList.get(imppc.getName());
			if (oldpc != null)
			{
				oldpc.copyFrom(imppc);
				oldpc.write();
			}
			else
			{
				imppc.setDatabase(editDb);
				imppc.clearId();
				imppc.write();
				clPanel.configSelectPanel.addConfig(imppc);
			}
		}
		else if (dbo instanceof Site)
		{
			SiteListPanel siteListPanel = parent.getSiteListPanel();
			Site imps = (Site)dbo;
			Site edits = null;
			for(Iterator it = imps.getNames(); edits == null && it.hasNext(); )
			{
				SiteName sn = (SiteName)it.next();
				edits = editDb.siteList.getSite(sn);
			}
			if (edits == null)
			{
				try
				{
					edits = SiteNameEntryDialog.siteFactory.makeNewSite(
						imps.getPreferredName(), editDb);
					edits.isNew = true;
					editDb.siteList.addSite(edits);
					siteListPanel.siteSelectPanel.addSite(edits);
				}
				catch(DecodesException ex) 
				{
					showError(dbeditLabels.getString("ImportDialog.ImportError")
							+" " + ex);
					return;
				}
			}
			edits.copyFrom(imps);

			for(Iterator it = imps.getNames(); it.hasNext(); )
				edits.addName((SiteName)it.next());

			edits.write();
			imps.setId(edits.getId());
		}
		else if (dbo instanceof EquipmentModel)
		{
			EquipmentListPanel elPanel = parent.getEquipmentListPanel();

			EquipmentModel impem = (EquipmentModel)dbo;
			EquipmentModel editem = 
				editDb.equipmentModelList.get(impem.getName());
			if (editem != null)
			{
				editem.copyFrom(impem);
				editem.write();
			}
			else
			{
				impem.setDatabase(editDb);
				impem.clearId();
				impem.write();
				elPanel.equipmentModelSelectPanel.addEquipmentModel(impem);
			}
		}
	}

	private void cancelButtonPressed()
	{
		closeDlg();
	}


	private void importAll()
	{
		final JobDialog dlg =
			new JobDialog(this, 
					dbeditLabels.getString("ImportDialog.JobDialogTitle"), true);
		dlg.setCanCancel(true);

		Thread backgroundJob =
			new Thread()
			{
				public void run()
				{
					try { sleep(2000L); } catch(InterruptedException ex) {}
					DefaultListModel listmod  = objectList.getDefaultModel();
					int n_imported = 0;
					int n_errors = 0;
					int n = listmod.size();
					for(int i=0; i<n && !dlg.wasCancelled() && dlg.isVisible();
						i++)
					{
						CheckableDatabaseObject cdo =
							(CheckableDatabaseObject)listmod.get(i);
						if (cdo.isSelected())
						{
							try 
							{
								IdDatabaseObject dbo = 
									(IdDatabaseObject)cdo.getDBO();

// NO - import platform last so that sites & configs are assigned IDs.
//								importDBO(dbo);
								n_imported++;
								if (dbo instanceof Platform)
								{
									Platform p = (Platform)dbo;
									Site s = p.getSite();
									if (s != null)
									{
										dlg.addToProgress(
												dbeditLabels.getString(
												"ImportDialog.JobDialogImport")
											+ s.getDisplayName() + "'");
										importDBO(s);
										dlg.addToProgress(
												dbeditLabels.getString(
												"ImportDialog.JobDialogImportDone"));
										n_imported++;
									}
									Platform.configSoftLink = false;
									PlatformConfig pc = p.getConfig();
									Platform.configSoftLink = true;
									
									EquipmentModel em = null; // josue added this line
									
									if (pc != null)
									{
										dlg.addToProgress(
												dbeditLabels.getString("ImportDialog.JobDialogImportConf")
											+ pc.getDisplayName() + "'");
										importDBO(pc);
										dlg.addToProgress(
												dbeditLabels.getString("ImportDialog.JobDialogImportDone"));
										n_imported++;
										
										em = pc.equipmentModel; // josue added this line
									}
									
									//EquipmentModel em = pc.equipmentModel; // josue commented out this line
									if (em != null)
									{
										dlg.addToProgress(
											dbeditLabels.getString("ImportDialog.JobDialogImportModel")
											+ pc.getDisplayName() + "'");
										importDBO(em);
										dlg.addToProgress(
												dbeditLabels.getString("ImportDialog.JobDialogImportDone"));
										n_imported++;
									}
									// Finally import the Platform object LAST.
									dlg.addToProgress(
											dbeditLabels.getString("ImportDialog.JobDialogImporting")+" "
										+ dbo.getObjectType() + " '"
										+ dbo.getDisplayName() + "'");
									importDBO(p);
									dlg.addToProgress(
											dbeditLabels.getString("ImportDialog.JobDialogImportDone"));
								}
								else
								{
									dlg.addToProgress(
											dbeditLabels.getString("ImportDialog.JobDialogImporting")+" "
										+ dbo.getObjectType() + " '"
										+ dbo.getDisplayName() + "'");
									importDBO(dbo);
									dlg.addToProgress(
											dbeditLabels.getString("ImportDialog.JobDialogImportDone"));
								}
							}
							catch(DatabaseException ex)
							{
								showError("Error on import: " + ex);
								n_errors++;
							}
						}
					}
					dlg.addToProgress(
						"" + n_imported 
						+ " "+
						LoadResourceBundle.sprintf(
								dbeditLabels.getString(
								"ImportDialog.JobDialogRecords1"), n_errors) 
						+ " errors were encountered). ");
					dlg.addToProgress(
							dbeditLabels.getString(
							"ImportDialog.JobDialogRecords2"));
					dlg.addToProgress("");
					dlg.addToProgress(dbeditLabels.getString(
					"ImportDialog.JobDialogRecords3"));
				}
			};

		backgroundJob.start();
		TopFrame.instance().launchDialog(dlg);
		closeDlg();
	}
}
