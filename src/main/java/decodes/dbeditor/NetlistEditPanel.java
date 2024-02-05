/*
 *  $Id$
 */
package decodes.dbeditor;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;

import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Collections;

import lrgs.gui.PdtSelectDialog;
import ilex.util.EnvExpander;
import ilex.util.TextUtil;
import decodes.db.*;
import decodes.gui.*;
import decodes.util.DecodesSettings;
import decodes.util.NwsXrefEntry;
import decodes.util.PdtEntry;

/**
 * This panel edits a single network list. Opened from the NetlistListPanel.
 */
@SuppressWarnings("serial")
public class NetlistEditPanel extends DbEditorTab implements ChangeTracker, EntityOpsController
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	private EntityOpsPanel entityOpsPanel = new EntityOpsPanel(this);
	private JTextField nameField = new JTextField();
	private EnumComboBox siteNameTypeCombo = new EnumComboBox(Constants.enum_SiteName);
	private EnumComboBox mediumTypeCombo = new EnumComboBox(Constants.enum_TMType);

	private DbEditorFrame parent;
	private NetworkList theObject, origObject;
	private NetlistContentsTableModel tableModel;
	private JTable netlistContentsTable;
	private JTextField lastModifiedField = new JTextField();
	private boolean goingThroughInit = true;
	private JButton selectFromPdtButton = null;
	private PdtSelectDialog pdtSelectDialog = null;
	private static JFileChooser nlFileChooser = null;

	/**
	 * Construct new panel to edit specified object.
	 * 
	 * @param ob
	 *            the object to edit in this panel.
	 */
	public NetlistEditPanel(NetworkList ob)
	{
		try
		{
			origObject = ob;
			theObject = origObject.copy();
			setTopObject(origObject);
			if (theObject.transportMediumType == null)
				theObject.transportMediumType = Constants.medium_Goes;
			if (theObject.siteNameTypePref == null)
			{
				theObject.siteNameTypePref = DecodesSettings.instance().siteNameTypePreference;
				if (theObject.siteNameTypePref == null)
					theObject.siteNameTypePref = Constants.snt_NWSHB5;
			}
			tableModel = new NetlistContentsTableModel(theObject);
			netlistContentsTable = new SortingListTable(tableModel, new int[]
			{ 15, 25, 60 });
			guiInit();
			fillFields();
			if (nlFileChooser == null)
			{
				nlFileChooser = new JFileChooser();
				nlFileChooser.setCurrentDirectory(
					new File(EnvExpander.expand("$DCSTOOL_USERDIR")));
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	/**
	 * This method only called in dbedit. Associates this panel with enclosing
	 * frame.
	 * 
	 * @param parent
	 *            Enclosing frame
	 */
	void setParent(DbEditorFrame parent)
	{
		this.parent = parent;
	}

	/** Fills the GUI controls with values from the object. */
	private void fillFields()
	{
		nameField.setText(theObject.name);
		siteNameTypeCombo.setSelection(theObject.siteNameTypePref);
		mediumTypeCombo.setSelection(theObject.transportMediumType);
		lastModifiedField.setText(theObject.lastModifyTime == null ? ""
			: Constants.defaultDateFormat.format(theObject.lastModifyTime));
		goingThroughInit = false;
		mediumTypeSelected();
	}

	/**
	 * Gets the data from the fields & puts it back into the object.
	 */
	private void getDataFromFields()
	{
		tableModel.putTableDataInObject(theObject);
		theObject.name = nameField.getText();
		theObject.siteNameTypePref = siteNameTypeCombo.getSelection();
		theObject.transportMediumType = mediumTypeCombo.getSelection();
	}

	/** Initializes GUI components */
	private void guiInit() throws Exception
	{
		this.setLayout(new BorderLayout());
		JPanel mainPanel = new JPanel(new BorderLayout());
		this.add(mainPanel, BorderLayout.CENTER);
		JPanel northParamPanel = new JPanel(new GridBagLayout());
		mainPanel.add(northParamPanel, BorderLayout.NORTH);
		JPanel centerTablePanel = new JPanel(new BorderLayout());
		mainPanel.add(centerTablePanel, BorderLayout.CENTER);
		this.add(entityOpsPanel, BorderLayout.SOUTH);

		// North Params Panel
		northParamPanel.add(new JLabel(dbeditLabels.getString("NetlistEditPanel.NetListName")),
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(4, 10, 2, 0), 0, 0));
		nameField.setEditable(false);
		northParamPanel.add(nameField, 
			new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 2, 2, 15), 30, 0));
		northParamPanel.add(new JLabel(dbeditLabels.getString("NetlistEditPanel.MediumType")),
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 0), 0, 0));
		northParamPanel.add(mediumTypeCombo, 
			new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 15), 30, 0));
		mediumTypeCombo.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					mediumTypeSelected();
				}
			});

		northParamPanel.add(new JLabel(dbeditLabels.getString("NetlistEditPanel.NameType")),
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 0), 0, 0));
		northParamPanel.add(siteNameTypeCombo, 
			new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 15), 30, 0));
		northParamPanel.add(new JLabel(dbeditLabels.getString("NetlistEditPanel.LastModified")),
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 10, 0), 0, 0));
		northParamPanel.add(lastModifiedField, 
			new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 2, 10, 100), 30, 0));
		siteNameTypeCombo.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				modifyNetworkListEntries();
			}
		});
		lastModifiedField.setEnabled(true);
		lastModifiedField.setEditable(false);

		// Center Table Panel with button panel along the right margin
		JScrollPane tableScrollPane = new JScrollPane();
		centerTablePanel.add(tableScrollPane, BorderLayout.CENTER);
		tableScrollPane.getViewport().add(netlistContentsTable, null);
		JPanel eastButtonPanel = new JPanel(new GridBagLayout());
		centerTablePanel.add(eastButtonPanel, BorderLayout.EAST);

		JButton addButton = new JButton(dbeditLabels.getString("NetlistEditPanel.SelectPlatforms"));
		addButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				addDecodesPlatformsPressed();
			}
		});
		eastButtonPanel.add(addButton,
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(4, 8, 4, 8), 0, 0));
		
		JButton addPlatformIdButton = 
			new JButton(dbeditLabels.getString("NetlistEditPanel.AddManual"));
		addPlatformIdButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				addManualPressed();
			}
		});
		eastButtonPanel.add(addPlatformIdButton,
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(4, 8, 4, 8), 0, 0));
		
		JButton editPlatformIdButton = 
			new JButton(dbeditLabels.getString("NetlistEditPanel.EditManual"));
		editPlatformIdButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				editManualPressed();
			}
		});
		eastButtonPanel.add(editPlatformIdButton,
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(4, 8, 4, 8), 0, 0));

		selectFromPdtButton = 
			new JButton(dbeditLabels.getString("NetlistEditPanel.SelectFromPDT"));
		selectFromPdtButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				selectFromPdtButtonPressed();
			}
		});
		eastButtonPanel.add(selectFromPdtButton,
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(4, 8, 4, 8), 0, 0));
		
		JButton importDotNlButton = 
			new JButton(dbeditLabels.getString("NetlistEditPanel.ImportDotNL"));
		importDotNlButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				importDotNlButtonPressed();
			}
		});
		eastButtonPanel.add(importDotNlButton,
			new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(4, 8, 4, 8), 0, 0));
		
		
		JButton removeButton = new JButton(dbeditLabels.getString("NetlistEditPanel.RemovePlatform"));
		removeButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				removePressed();
			}
		});
		eastButtonPanel.add(removeButton, 
			new GridBagConstraints(0, 5, 1, 1, 0.0, 1.0,
				GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
				new Insets(12, 8, 4, 8), 0, 0));
		
		netlistContentsTable.addMouseListener(
			new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 2)
					{
       					editManualPressed();
					}
				}
			});

	}

	protected void editManualPressed()
	{
		int row = netlistContentsTable.getSelectedRow();
		if (row == -1)
		{
			TopFrame.instance().showError(dbeditLabels.getString("NetlistEditPanel.EditError"));
			return;
		}
		NetworkListEntry nle = tableModel.getObjectAt(netlistContentsTable.convertRowIndexToModel(row));
		NetlistEntryDialog dlg = new NetlistEntryDialog(nle);
		launchDialog(dlg);
		if (dlg.wasOkPressed())
			tableModel.replace(nle, row);
	}

	protected void mediumTypeSelected()
	{
		selectFromPdtButton.setEnabled(
			mediumTypeCombo.getSelection().toLowerCase().startsWith("goes"));
	}

	protected void importDotNlButtonPressed()
	{
		if (JFileChooser.APPROVE_OPTION != nlFileChooser.showOpenDialog(this))
			return;
		File f = nlFileChooser.getSelectedFile();
		lrgs.common.NetworkList lrgsNl;
		try
		{
			lrgsNl = new lrgs.common.NetworkList(f);
			for(lrgs.common.NetworkListItem item : lrgsNl)
			{
				NetworkListEntry nle = new NetworkListEntry(theObject,
					item.addr.toString());
				nle.setDescription(item.description);
				nle.setPlatformName(item.name);
				tableModel.add(nle);
			}		
		}
		catch (IOException ex)
		{
			parent.showError("Cannot open or parse network list '" + f.getPath() + "': " + ex);
		}
	}

	protected void selectFromPdtButtonPressed()
	{
		System.out.println("Select From PDT Pressed.");
		if (pdtSelectDialog == null)
			pdtSelectDialog = new PdtSelectDialog(null);
		launchDialog(pdtSelectDialog);
		if (!pdtSelectDialog.isCancelled())
		{
			PdtEntry entries[] = pdtSelectDialog.getSelections();
			if (entries != null)
				for(PdtEntry ent : entries)
				{
					NetworkListEntry nle = new NetworkListEntry(theObject,
						ent.dcpAddress.toString());
					if (tableModel.contains(nle.transportId))
						continue;
					NwsXrefEntry nwsXrefEntry = ent.getNwsXrefEntry();
					if (nwsXrefEntry != null)
						nle.setPlatformName(nwsXrefEntry.getNwsId());
					nle.setDescription(ent.getDescription());
					tableModel.add(nle);
				}
		}
	}

	protected void addManualPressed()
	{
		NetworkListEntry nle = new NetworkListEntry(theObject, "");
		NetlistEntryDialog dlg = new NetlistEntryDialog(nle);
		launchDialog(dlg);
		if (dlg.wasOkPressed())
			tableModel.add(nle);
	}

	private void modifyNetworkListEntries()
	{
		if (goingThroughInit)
			return;
		// Get new Prefered Site Name Type
		String siteNameTypePref = siteNameTypeCombo.getSelection();
		// Read new Site Names
		try
		{
			for (Iterator<NetworkListEntry> it = theObject.iterator(); it.hasNext();)
			{
				NetworkListEntry nle = it.next();
				Platform p = Database.getDb().platformList.getPlatform(
					theObject.transportMediumType, nle.transportId);
				if (p != null)
				{
					// Find the right site name for this network list site
					// name type preference
					Site pSite = p.getSite();
					if (pSite != null)
					{// FIRST - see if it can find a site name for this type
						SiteName sn = pSite.getName(siteNameTypePref);
						if (sn != null)
							nle.setPlatformName(sn.getNameValue());
						else
						{
							// **In this case it cannot find any site name
							// for the nl.siteNameTypePref so use the default or
							// what ever site name it has.
							// this is like it was before
							nle.setPlatformName(p.getSiteName(false));
						}
					}
					else
					{
						nle.setPlatformName(p.getSiteName(false));
					}
				}
			}
			// Update the table
			tableModel.refresh(theObject);
		}
		catch (DatabaseException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	/**
	 * Called when the 'Add' button is pressed. Starts a new modal
	 * PlatformSelectDialog.
	 */
	void addDecodesPlatformsPressed()
	{
		PlatformSelectDialog dlg = new PlatformSelectDialog(
			DbEditorFrame.instance(), mediumTypeCombo.getSelection());
		dlg.setMultipleSelection(true);
		launchDialog(dlg);
		Platform toAdd[] = dlg.getSelectedPlatforms();
		for (int i = 0; i < toAdd.length; i++)
		{
			Platform p = toAdd[i];

			TransportMedium tm = p.getTransportMedium(mediumTypeCombo.getSelection());

			if (tm == null)
				tm = p.getTransportMedium(Constants.medium_Goes);
			if (tm == null)
				tm = p.getTransportMedium(Constants.medium_GoesST);
			if (tm == null)
				tm = p.getTransportMedium(Constants.medium_GoesRD);
			if (tm == null)
			{
				System.out.println(dbeditLabels.getString("NetlistEditPanel.GOESTransport") + " "
					+ toAdd[i].makeFileName() + " -- " + mediumTypeCombo.getSelection());
				continue;
			}

			NetworkListEntry nle = new NetworkListEntry(theObject, tm.getMediumId());
			Site s = p.getSite();
			String sn = null;
			if (s != null)
				sn = s.getPreferredName().getNameValue();
			if (sn != null)
				nle.setPlatformName(sn);
			else
				nle.setPlatformName(p.makeFileName());
			// Get a description from either platform or site record.
			String desc = s.getDescription();
			if (desc == null)
			{
				desc = p.description;
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
			tableModel.add(nle);
		}
	}

	/**
	 * Called when the 'Remove' button is pressed. Selected platform(s) are
	 * removed from the list.
	 */
	void removePressed()
	{
		int nrows = netlistContentsTable.getSelectedRowCount();
		if (nrows == 0)
		{
			TopFrame.instance().showError(dbeditLabels.getString("NetlistEditPanel.RemoveError"));
			return;
		}

		int rows[] = netlistContentsTable.getSelectedRows();
		NetworkListEntry obs[] = new NetworkListEntry[nrows];
		for (int i = 0; i < nrows; i++)
			obs[i] = tableModel.getObjectAt(rows[i]);

		String msg = nrows == 1 ? dbeditLabels.getString("NetlistEditPanel.DeleteSingular")
			: dbeditLabels.getString("NetlistEditPanel.DeletePlural");
		int r = JOptionPane.showConfirmDialog(this, msg);
		if (r == JOptionPane.OK_OPTION)
			for (int i = 0; i < nrows; i++)
				tableModel.deleteObject(obs[i]);
	}

	/**
	 * From ChangeTracker interface.
	 * 
	 * @return true if changes have been made to this screen since the last time
	 *         it was saved.
	 */
	public boolean hasChanged()
	{
		getDataFromFields();
		return !theObject.equals(origObject);
	}

	/**
	 * From ChangeTracker interface, save the changes back to the database &amp;
	 * reset the hasChanged flag.
	 * 
	 * @return true if object was successfully saved.
	 */
	public boolean saveChanges()
	{
		getDataFromFields();
		try
		{
			theObject.write();
		}
		catch (DatabaseException e)
		{
			TopFrame.instance().showError(
				dbeditLabels.getString("NetlistEditPanel.SaveError") + " " + e);
			return false;
		}

		// Replace the old datasource in the list.
		// This also updates the SourceListPanel.
		Database.getDb().networkListList.remove(origObject);
		Database.getDb().networkListList.add(theObject);
		parent.getNetlistListPanel().resort();

		// Make a new copy in case user wants to keep editing.
		origObject = theObject;
		theObject = origObject.copy();
		setTopObject(origObject);
		lastModifiedField.setText(Constants.defaultDateFormat.format(new Date()));
		return true;
	}

	/** @see EntityOpsController */
	public String getEntityName()
	{
		return dbeditLabels.getString("NetlistEditPanel.NetlistText");
	}

	public void commitEntity()
	{
		saveChanges();
	}

	/** @see EntityOpsController */
	public void closeEntity()
	{
		if (hasChanged())
		{
			int r = JOptionPane.showConfirmDialog(this,
				dbeditLabels.getString("NetlistEditPanel.SavePrompt"));
			if (r == JOptionPane.CANCEL_OPTION)
				return;
			else if (r == JOptionPane.YES_OPTION)
			{
				if (!saveChanges())
					return;
			}
			else if (r == JOptionPane.NO_OPTION)
				;
		}
		DbEditorTabbedPane tp = parent.getNetworkListTabbedPane();
		tp.remove(this);
	}

	/**
	 * Called from File - CloseAll to close this tab, abandoning any changes.
	 */
	public void forceClose()
	{
		DbEditorTabbedPane tp = parent.getNetworkListTabbedPane();
		tp.remove(this);
	}

	/** Does nothing. */
	public void help()
	{
	}
}

class NetlistContentsTableModel extends AbstractTableModel implements SortingListTableModel
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	static String columnNames[] =
	{ dbeditLabels.getString("NetlistEditPanel.TableColumn1"),
		dbeditLabels.getString("NetlistEditPanel.TableColumn2"),
		dbeditLabels.getString("NetlistEditPanel.TableColumn3") };
	private int lastSortColumn = -1;
	private Vector<NetworkListEntry> theList;

	public NetlistContentsTableModel(NetworkList ob)
	{
		super();
		theList = new Vector<NetworkListEntry>();
		for (Iterator it = ob.iterator(); it.hasNext();)
		{
			NetworkListEntry nle = (NetworkListEntry) it.next();
			theList.add(nle);
		}
		refill();
		sortByColumn(0);
	}
	
	boolean contains(String id)
	{
		for(NetworkListEntry nle : theList)
			if (id.equals(nle.transportId))
				return true;
		return false;
	}

	void refill()
	{
	}

	public void refresh(NetworkList ob)
	{
		theList.clear();
		for (Iterator it = ob.iterator(); it.hasNext();)
		{
			NetworkListEntry nle = (NetworkListEntry) it.next();
			theList.add(nle);
		}
		resort();
		fireTableDataChanged();
	}

	public int getRowCount()
	{
		return theList.size();
	}

	public int getColumnCount()
	{
		return columnNames.length;
	}

	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	public boolean isCellEditable(int r, int c) { return false; }

	NetworkListEntry getObjectAt(int r)
	{
		return (NetworkListEntry) getRowObject(r);
	}

	public Object getRowObject(int r)
	{
		if (r >= 0 && r < getRowCount())
			return theList.elementAt(r);
		else
			return null;
	}

	void add(NetworkListEntry ob)
	{
		for (Iterator it = theList.iterator(); it.hasNext();)
		{
			NetworkListEntry lob = (NetworkListEntry) it.next();
			if (ob.transportId.equals(lob.transportId))
			{
				it.remove();
				break;
			}
		}
		theList.add(ob);
		fireTableDataChanged();
	}

	void deleteObject(NetworkListEntry ob)
	{
		theList.remove(ob);
		fireTableDataChanged();
	}
	
	void replace(NetworkListEntry ob, int row)
	{
		theList.set(row, ob);
		fireTableDataChanged();
	}

	public Object getValueAt(int r, int c)
	{
		NetworkListEntry ob = getObjectAt(r);
		if (ob == null)
			return "";
		else
			return getNLEColumn(ob, c);
	}

	public static String getNLEColumn(NetworkListEntry ob, int c)
	{
		switch (c)
		{
		case 0:
			return ob.transportId;
		case 1:
			return ob.getPlatformName();
		case 2:
			return ob.getDescription();
		default:
			return "";
		}
	}

	public void resort()
	{
		if (lastSortColumn != -1)
			sortByColumn(lastSortColumn);
	}

	public void sortByColumn(int c)
	{
		lastSortColumn = c;
		Collections.sort(theList, new NLEComparator(c));
		fireTableDataChanged();
	}

	public void putTableDataInObject(NetworkList theObject)
	{
		theObject.clear();
		for (Iterator it = theList.iterator(); it.hasNext();)
		{
			NetworkListEntry nle = (NetworkListEntry) it.next();
			theObject.addEntry(nle);
		}
	}

	/*
	 * void replace(NetworkList oldOb, NetworkList newOb) {
	 * theList.remove(oldOb); theList.add(newOb); if (lastSortColumn != -1)
	 * sortByColumn(lastSortColumn); else fireTableDataChanged(); }
	 */
}

class NLEComparator implements Comparator
{
	int column;

	public NLEComparator(int column)
	{
		this.column = column;
	}

	/**
	 * Compare the eqMod names of the specified type.
	 */
	public int compare(Object ob1, Object ob2)
	{
		if (ob1 == ob2)
			return 0;
		NetworkListEntry ds1 = (NetworkListEntry) ob1;
		NetworkListEntry ds2 = (NetworkListEntry) ob2;

		String s1 = NetlistContentsTableModel.getNLEColumn(ds1, column);
		String s2 = NetlistContentsTableModel.getNLEColumn(ds2, column);

		return TextUtil.strCompareIgnoreCase(s1, s2);
	}

	public boolean equals(Object ob)
	{
		return false;
	}
}
