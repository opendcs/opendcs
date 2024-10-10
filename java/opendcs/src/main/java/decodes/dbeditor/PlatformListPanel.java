/*
*  $Id$
*/
package decodes.dbeditor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import java.util.stream.Collectors;
import java.util.Vector;
import java.util.Collections;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import ilex.util.LoadResourceBundle;

import decodes.gui.*;
import decodes.db.*;
import decodes.dbeditor.platform.PlatformSelectTableModel;

/**
Displays a sorting-list of Platform objects in the database.
 */
public class PlatformListPanel extends JPanel implements ListOpsController
{
	private static final String NO_PLATFORM_NAMES = "<no platform names>";
	private static final String NO_AGENCY = "<no agency>";
	private static final String NO_TRANSPORT_MEDIUM = "<no transport medium>";
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();
	List<RowFilter<TableModel, Integer>> filters = new ArrayList<RowFilter<TableModel, Integer>>();
	TableRowSorter<?> sorter = null;
	Vector<Platform> vectplatf = null;

	BorderLayout borderLayout1 = new BorderLayout();
	ListOpsPanel listOpsPanel;
	JLabel jLabelTitle = new JLabel();
	PlatformSelectPanel platformSelectPanel = new PlatformSelectPanel(null);
	DbEditorFrame parent;
	int newIndex = 1;

	JPanel filterComboBoxesPanel = new JPanel();
	JLabel platformLbl = new JLabel("Platform:");
	JComboBox<String> platformCbx = null;

	JLabel agencyLbl = new JLabel("Agency:");
	JComboBox<String> agencyCbx = null;

	JLabel transportLbl = new JLabel("Transport-ID:");
	JComboBox<String> transportCbx = null;

	JLabel configLbl = new JLabel("Config:");
	JComboBox<String> configCbx = null;

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
		Vector<String> platformNames = vectplatf.stream()
												.map(Platform::getDisplayName)
												.filter(a -> a != null)
												.distinct()
												.collect(Collectors.toCollection(Vector::new));
		Collections.sort(platformNames);
		platformNames.add(0,"");
		platformNames.add(1, NO_PLATFORM_NAMES);
		platformCbx = new JComboBox<>(platformNames.toArray(new String[0]));
		platformCbx.setSize(new Dimension(25, 25));
		platformCbx.setSelectedIndex(-1);

		// Agency filter list
		Vector<String> agencies = vectplatf.stream()
										   .map(Platform::getAgency)
										   .filter(a -> a != null)
										   .distinct()
										   .collect(Collectors.toCollection(Vector::new));
		Collections.sort(agencies);
		agencies.add(0, "");
		agencies.add(1, NO_AGENCY);
		agencyCbx = new JComboBox<>(agencies);
		agencyCbx.setSize(new Dimension(25, 25));
		agencyCbx.setSelectedIndex(-1);

		// Transport Id filter list
		Vector<String> transportIds = vectplatf.stream()
											   .filter(p -> p.getTransportMedia().hasNext())
											   .map(Platform::getPreferredTransportId)
											   .distinct()
											   .collect(Collectors.toCollection(Vector::new));
		Collections.sort(transportIds);
		transportIds.add(0, "");
		transportIds.add(1, NO_TRANSPORT_MEDIUM);
		transportCbx = new JComboBox<>(transportIds);
		transportCbx.setSize(new Dimension(25, 25));
		transportCbx.setSelectedIndex(-1);

		// Config filter list
		Vector<String> configNames = vectplatf.stream()
										      .filter(p -> p.getConfig() != null)
											  .map(Platform::getConfigName)
											  .distinct()
											  .collect(Collectors.toCollection(Vector::new));
		configNames.add(0, "");
		Collections.sort(configNames);
		configCbx = new JComboBox<>(configNames);
		configCbx.setSize(new Dimension(25, 25));
		configCbx.setSelectedIndex(-1);

	}


	/**
	 * Setting up filter options
	 */
	private void updateFilters() {
		filters.clear();

		if ((platformCbx.getSelectedIndex() > 0) && (platformCbx.getSelectedItem().toString().length() > 0))
		{
			final String filterText = (String)platformCbx.getSelectedItem();
			if (filterText.equals(NO_PLATFORM_NAMES))
			{
				filters.add(new PlatformSelectTableModel.ColumnEmptyRowFilter(genericLabels.getString("platform")));
			}
			else
			{
				RowFilter<TableModel, Integer> filterName = RowFilter
					.regexFilter(platformCbx.getSelectedItem().toString(), 0);
				filters.add(filterName);
			}
		}

		if ((agencyCbx.getSelectedIndex() > 0) && (agencyCbx.getSelectedItem().toString().length() > 0))
		{
			final String filterText = (String)agencyCbx.getSelectedItem();
			if (filterText.equals(NO_AGENCY))
			{
				filters.add(new PlatformSelectTableModel.ColumnEmptyRowFilter(dbeditLabels.getString("PlatformSelectPanel.agency")));
			}
			else
			{
				RowFilter<TableModel, Integer> filterAgency = RowFilter
					.regexFilter(agencyCbx.getSelectedItem().toString(), 1);
				filters.add(filterAgency);
			}
		}

		if ((transportCbx.getSelectedIndex() > 0) && (transportCbx.getSelectedItem().toString().length() > 0)) 
		{
			final String filterText = (String)transportCbx.getSelectedItem();
			if (filterText.equals(NO_TRANSPORT_MEDIUM))
			{
				filters.add(new PlatformSelectTableModel.ColumnEmptyRowFilter(dbeditLabels.getString("PlatformSelectPanel.transport")));
			}
			else
			{
				RowFilter<TableModel, Integer> filterTransport = RowFilter
					.regexFilter(transportCbx.getSelectedItem().toString(), 2);
				filters.add(filterTransport);
			}
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

	/**
	 * Called if a new platform is abandoned before it was ever saved.
	 * @param p the platform.
	 */
	void abandonNewPlatform(Platform p)
	{
		platformSelectPanel.deletePlatform(p);
	}
}
