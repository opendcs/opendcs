/*
*  $Id$
*/
package decodes.dbeditor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.table.*;
import java.util.stream.Collectors;
import java.util.Vector;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.List;
import java.util.ArrayList;

import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import ilex.util.LoadResourceBundle;
import ilex.util.Logger;

import decodes.gui.*;
import decodes.db.*;

/**
Displays a sorting-list of Platform objects in the database.
 */
public class PlatformListPanel extends JPanel
	implements ListOpsController
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();
	List<RowFilter<TableModel, Integer>> filters = new ArrayList<RowFilter<TableModel, Integer>>();
	TableRowSorter sorter = null;
	Vector<Platform> vectplatf = null;

	BorderLayout borderLayout1 = new BorderLayout();
	ListOpsPanel listOpsPanel;
	JLabel jLabelTitle = new JLabel();
	PlatformSelectPanel platformSelectPanel = new PlatformSelectPanel(null);
	DbEditorFrame parent;
	int newIndex = 1;

	JPanel filterComboBoxesPanel = new JPanel();
	JLabel platformLbl = new JLabel("Platform:");
	JComboBox platformCbx = null;

	JLabel agencyLbl = new JLabel("Agency:");
	JComboBox agencyCbx = null;

	JLabel transportLbl = new JLabel("Transport-ID:");
	JComboBox transportCbx = null;

	JLabel configLbl = new JLabel("Config:");
	JComboBox configCbx = null;

	/** Constructor. */
	public PlatformListPanel() {
		parent = null;
		listOpsPanel = new ListOpsPanel(this);
		listOpsPanel.enableCopy(true);
		platformSelectPanel.setParentPanel(this);
		try {
			jbInit();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Sets the parent frame object. Each list panel needs to know this.
	 * 
	 * @param parent the DbEditorFrame
	 */
	void setParent(DbEditorFrame parent) 
	{
		this.parent = parent;
	}

	/** Initializes GUI components. */
	private void jbInit() throws Exception 
	{
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		jLabelTitle.setHorizontalAlignment(SwingConstants.CENTER);
		jLabelTitle.setText(dbeditLabels.getString("PlatformListPanel.title"));
		this.add(jLabelTitle);

		filterComboBoxesPanel.setLayout(new GridBagLayout());

		loadFilterLists();

		// Add text boxes here
		filterComboBoxesPanel.add(platformLbl, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 0, 0));
		filterComboBoxesPanel.add(platformCbx, new GridBagConstraints(20, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 0, 0));
		filterComboBoxesPanel.add(agencyLbl, new GridBagConstraints(30, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 0, 0));
		filterComboBoxesPanel.add(agencyCbx, new GridBagConstraints(40, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 0, 0));

		filterComboBoxesPanel.add(transportLbl, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 0, 0));
		filterComboBoxesPanel.add(transportCbx, new GridBagConstraints(20, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 0, 0));
		filterComboBoxesPanel.add(configLbl, new GridBagConstraints(30, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 0, 0));
		filterComboBoxesPanel.add(configCbx, new GridBagConstraints(40, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 0, 0));

		platformCbx.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateFilters();
			}
		});

		agencyCbx.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateFilters();
			}
		});

		transportCbx.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateFilters();
			}
		});

		configCbx.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateFilters();
			}
		});

		sorter = new TableRowSorter<TableModel>(platformSelectPanel.getPlatformListTable().getModel());// sorter
		platformSelectPanel.getPlatformListTable().setRowSorter(sorter);

		this.add(filterComboBoxesPanel);
		this.add(platformSelectPanel, BorderLayout.CENTER); // The JTable
		this.add(listOpsPanel);
	}

	/**
	 * Load the combo boxes with the filter lists
	 */
	private void loadFilterLists() 
	{
		vectplatf = Database.getDb().platformList.getPlatformVector();
		if (vectplatf.size() > 0) 
		{
			Vector<String> platformNames = new Vector<String>(vectplatf.size() + 1);
			platformNames.add(0, "");
			for (int x = 1; x < vectplatf.size(); x++) 
			{
				platformNames.add(x, vectplatf.get(x).getDisplayName());
			}
			for (int x = 0; x < platformNames.size(); x++) 
			{
				if (null == platformNames.get(x)) 
				{
					platformNames.remove(x);
				}
			}
			Collections.sort(platformNames);
			platformCbx = new JComboBox(platformNames);
			platformCbx.setSize(new Dimension(25, 25));
			platformCbx.setSelectedIndex(-1);

			// Agency filter list
			Vector<String> agencies = new Vector<String>(vectplatf.size() + 1);
			agencies.add(0, "");
			for (int y = 1; y < vectplatf.size(); y++) 
			{
				agencies.add(y, vectplatf.get(y).getAgency());
			}
			agencies = agencies.stream().distinct().collect(Collectors.toCollection(Vector::new));
			for (int x = 0; x < agencies.size(); x++) 
			{
				if (null == agencies.get(x)) 
				{
					agencies.remove(x);
				}
			}

			Collections.sort(agencies);
			agencyCbx = new JComboBox(agencies);
			agencyCbx.setSize(new Dimension(25, 25));
			agencyCbx.setSelectedIndex(-1);

			// Transport Id filter list
			Vector<String> transportIds = new Vector<String>(vectplatf.size() + 1);
			transportIds.add(0, "");
			for (int x = 1; x < vectplatf.size(); x++) 
			{
				transportIds.add(x, vectplatf.get(x).getPreferredTransportId());
			}
			transportIds = transportIds.stream().distinct().collect(Collectors.toCollection(Vector::new));
			for (int x = 0; x < transportIds.size(); x++) 
			{
				if (null == transportIds.get(x)) 
				{
					transportIds.remove(x);
				}
			}
			Collections.sort(transportIds);
			transportCbx = new JComboBox(transportIds);
			transportCbx.setSize(new Dimension(25, 25));
			transportCbx.setSelectedIndex(-1);

			// Config filter list
			Vector<String> configNames = new Vector<String>(vectplatf.size() + 1);
			configNames.add(0, "");
			for (int x = 1; x < vectplatf.size(); x++) 
			{
				configNames.add(x, vectplatf.get(x).getConfigName());
			}
			configNames = configNames.stream().distinct().collect(Collectors.toCollection(Vector::new));
			for (int x = 0; x < configNames.size(); x++) 
			{
				if (null == configNames.get(x)) 
				{
					configNames.remove(x);
				}
			}
			Collections.sort(configNames);
			configCbx = new JComboBox(configNames);
			configCbx.setSize(new Dimension(25, 25));
			configCbx.setSelectedIndex(-1);
		}
	}

	/**
	 * Setting up filter options
	 */
	private void updateFilters() {
		filters.clear();

		if ((platformCbx.getSelectedIndex() > 0) && (platformCbx.getSelectedItem().toString().length() > 0))
		{
			RowFilter<TableModel, Integer> filterName = RowFilter
				.regexFilter(platformCbx.getSelectedItem().toString(), 0);
			filters.add(filterName);			
		}

		if ((agencyCbx.getSelectedIndex() > 0) && (agencyCbx.getSelectedItem().toString().length() > 0))
		{
			RowFilter<TableModel, Integer> filterAgency = RowFilter
				.regexFilter(agencyCbx.getSelectedItem().toString(), 1);
			filters.add(filterAgency);			
		}

		if ((transportCbx.getSelectedIndex() > 0) && (transportCbx.getSelectedItem().toString().length() > 0)) 
		{
			RowFilter<TableModel, Integer> filterTransport = RowFilter
				.regexFilter(transportCbx.getSelectedItem().toString(), 2);
			filters.add(filterTransport);			
		}

		if ((configCbx.getSelectedIndex() > 0) && (configCbx.getSelectedItem().toString().length() > 0))
		{
			RowFilter<TableModel, Integer> filterConfig = RowFilter
				.regexFilter(configCbx.getSelectedItem().toString(), 3);
			filters.add(filterConfig);
		}

		if (!filters.isEmpty()) 
		{
			RowFilter<TableModel, Integer> comboFilter = RowFilter.andFilter(filters); // sorter setup
			sorter.setRowFilter(comboFilter);
		} 
		else 
		{
			sorter.setRowFilter(null);
		}
	}

	/** @return type of entity that this panel edits. */
	public String getEntityType() { return "Platform"; }

	public Platform getSelectedPlatform()
	{
		return platformSelectPanel.getSelectedPlatform();
	}

	/**
	 * Get the selected Platform from the filtered list
	 * 
	 * @return Platform
	 */
	public Platform getSelectedItem() 
	{
		int rowNumb = this.platformSelectPanel.getPlatformListTable().getSelectedRow();
		int rowModel = this.platformSelectPanel.getPlatformListTable().convertRowIndexToModel(rowNumb);
		PlatformSelectTableModel model = (PlatformSelectTableModel) platformSelectPanel.getPlatformListTable()
																					   .getModel();
		Platform platform = model.getPlatformAt(rowModel);
		return platform;
	}

	/** Called when the 'Open' button is pressed. */
	public void openPressed() 
	{
		int rows = this.platformSelectPanel.getPlatformListTable().getRowCount();
		// System.out.println("Open rows: " + rows);
		Platform p = getSelectedItem();
		// Platform p = platformSelectPanel.getSelectedPlatform();
		if (p == null)
		{
			TopFrame.instance().showError(
					dbeditLabels.getString("PlatformListPanel.selectOpen"));
		} 
		else 
		{
			try {
				p.read();
				doOpen(p);
			} 
			catch (Exception ex) {
				TopFrame.instance().showError(
						LoadResourceBundle.sprintf(
								dbeditLabels.getString("PlatformListPanel.cannotOpen"),
								p.makeFileName(), ex.toString()));
			}
		}
	}

	/** Called when the 'New' button is pressed. */
	public void newPressed()
	{
		doNew();
	}

	public PlatformEditPanel doNew()
	{
		Platform ob = new Platform();
		platformSelectPanel.addPlatform(ob);
		return doOpen(ob);
	}

	/** Called when the 'Copy' button is pressed. */
	public void copyPressed()
	{
		Platform p = platformSelectPanel.getSelectedPlatform();
		if (p == null)
		{
			TopFrame.instance().showError(
					dbeditLabels.getString("PlatformListPanel.selectCopy"));
		} 
		else
		{
			if (!p.isComplete())
			{
				try { p.read(); }
				catch (DatabaseException e)
				{
					TopFrame.instance().showError(
							LoadResourceBundle.sprintf(
									dbeditLabels.getString("PlatformListPanel.cannotRead"),
									p.makeFileName(), e.toString()));
					return;
				}
			}

			Platform newOb = p.noIdCopy();
			platformSelectPanel.addPlatform(newOb);
			doOpen(newOb);
		}
	}

	/** Called when the 'Delete' button is pressed. */
	public void deletePressed() {
		Platform p = getSelectedItem();
		Platform ob = platformSelectPanel.getSelectedPlatform();
		if (ob == null)
			TopFrame.instance().showError(
					dbeditLabels.getString("PlatformListPanel.selectDelete"));
		else {
			DbEditorTabbedPane platformsTabbedPane = parent.getPlatformsTabbedPane();
			DbEditorTab tab = platformsTabbedPane.findEditorFor(ob);
			if (tab != null) {
				TopFrame.instance().showError(
						dbeditLabels.getString("PlatformListPanel.platformEdited"));
				return;
			}
			int ok = JOptionPane.showConfirmDialog(this,
					dbeditLabels.getString("PlatformListPanel.confirmDelete"),
					dbeditLabels.getString("PlatformListPanel.confirmDeleteTitle"),
					JOptionPane.YES_NO_OPTION);
			if (ok == JOptionPane.YES_OPTION) {
				platformSelectPanel.deletePlatform(ob);
				try {
					Database.getDb().platformList.write();
				} catch (DatabaseException e) {
					TopFrame.instance().showError(
							dbeditLabels.getString("PlatformListPanel.errorDeleting")
									+ e);
				}
			}
		}
	}

	/** Called when the 'Help' button is pressed. */
	public void refreshPressed()
	{
	}

	/**
	  Opens a PlatformEditPanel for the passed platform.
	  @param ob the object to be edited.
	  @return the PlatformEditPanel opened.
	 */
	public PlatformEditPanel doOpen(Platform ob)
	{
		DbEditorTabbedPane tp = parent.getPlatformsTabbedPane();
		DbEditorTab tab = tp.findEditorFor(ob);
		if (tab != null)
		{
			tp.setSelectedComponent(tab);
			return (PlatformEditPanel)tab;
		}
		else
		{
			PlatformEditPanel newTab = new PlatformEditPanel(ob);
			newTab.setParent(parent);
			String title = ob.getPreferredTransportId();
			if (title.equals("unknown"))
				title = "New DCP " + (newIndex++);
			tp.add(title, newTab);
			tp.setSelectedComponent(newTab);
			return newTab;
		}
	}

	/**
	  After saving, and edit panel will need to replace the old object
	  with the newly modified one. It calls this method to do this.
	  @param oldp the old object
	  @param newp the new object
	 */
	public void replacePlatform(Platform oldp, Platform newp)
	{
		platformSelectPanel.replacePlatform(oldp, newp);
	}

	/** Resorts the list by the current column selection. */
	public void reSort()
	{
		platformSelectPanel.reSort();
	}

	/**
	 * Called if a new platform is abandoned before it was ever saved.
	 * @param p the platform.
	 */
	void abandonNewPlatform(Platform p)
	{
		platformSelectPanel.deletePlatform(p);
	}
}
