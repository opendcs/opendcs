/**
 * Copyright 2024 The OpenDCS Consortium and contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package decodes.dbeditor;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.border.*;
import java.lang.reflect.Constructor;
import java.util.Properties;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.slf4j.LoggerFactory;

import ilex.gui.Help;
import ilex.util.LoadResourceBundle;
import ilex.util.PropertiesUtil;

import decodes.gui.*;
import decodes.datasource.DataSourceExec;
import decodes.db.*;


/**
This panel edits a DataSource object.
Opened from the SourceListPanel.
*/
public class SourceEditPanel extends DbEditorTab
	implements ChangeTracker, EntityOpsController
{
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(SourceEditPanel.class);
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	EntityOpsPanel entityOpsPanel = new EntityOpsPanel(this);
	JPanel jPanel2 = new JPanel();
	JLabel jLabel1 = new JLabel();
	JTextField nameField = new JTextField();
	PropertiesEditPanel propertiesEditPanel;
	JLabel jLabel2 = new JLabel();
	EnumComboBox sourceTypeCombo =
		new EnumComboBox(Constants.enum_DataSourceType);

	JPanel jPanel3 = new JPanel();
	TitledBorder titledBorder1;
	JScrollPane jScrollPane1 = new JScrollPane();
	JList groupMemberList;
	JPanel jPanel4 = new JPanel();
	JButton deleteMemberButton = new JButton();
	JButton addMemberButton = new JButton();
	JButton upMemberButton = new JButton();
	JButton downMemberButton = new JButton();
	FlowLayout flowLayout1 = new FlowLayout();
	BorderLayout borderLayout2 = new BorderLayout();
	GridBagLayout gridBagLayout1 = new GridBagLayout();

	DbEditorFrame parent;
	DataSource dataSource, origDataSource;
	Properties theProperties;
	GroupMemberListModel groupMemberListModel;

	ArrayList<DatabaseObject> affectedItems = null;


	private Database getDb(){
		return Database.getDb();
	}

	/**
	  Construct new panel to edit specified object.
	  @param ob the object to edit in this panel.
	*/
	public SourceEditPanel(DataSource ob)
	{
		try
		{
 			origDataSource = ob;
			dataSource = origDataSource.copy();
			setTopObject(origDataSource);
			if (dataSource.getDataSourceArg() == null)
				dataSource.setDataSourceArg("");
			theProperties = PropertiesUtil.string2props(dataSource.getDataSourceArg());
			propertiesEditPanel = PropertiesEditPanel.from(theProperties);
			propertiesEditPanel.setOwnerFrame(getParentFrame());

			groupMemberListModel = new GroupMemberListModel(dataSource);
			groupMemberList = new JList(groupMemberListModel);
			jbInit();
			fillFields();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	  This method only called in dbedit.
	  Associates this panel with enclosing frame.
	  @param parent   Enclosing frame
	*/
	void setParent(DbEditorFrame parent)
	{
		this.parent = parent;
	}

	/** Fills the GUI controls with values from the object. */
	private void fillFields()
	{
		nameField.setText(dataSource.getName());
		sourceTypeCombo.setSelection(dataSource.dataSourceType);
		sourceTypeSelected();
		enabling();
	}

	private void enabling(){
		String dsType = (String)sourceTypeCombo.getSelectedItem();
		boolean isGroup = dsType.toLowerCase().endsWith("roup");
		addMemberButton.setEnabled(isGroup);
		deleteMemberButton.setEnabled(isGroup);
		upMemberButton.setEnabled(isGroup);
		downMemberButton.setEnabled(isGroup);
		groupMemberList.setEnabled(isGroup);
	}

	/**
	  Gets the data from the fields & puts it back into the object.
	*/
	private void getDataFromFields()
	{
		dataSource.setName(nameField.getText());
		dataSource.dataSourceType = (String)sourceTypeCombo.getSelectedItem();
		propertiesEditPanel.getModel().saveChanges();
		dataSource.setDataSourceArg(PropertiesUtil.props2string(theProperties));
	}

	/** Initializes GUI components */
	private void jbInit()
		throws Exception
	{
		titledBorder1 = new TitledBorder(
			BorderFactory.createLineBorder(new Color(153, 153, 153), 2),
			dbeditLabels.getString("SourceEditPanel.groupMembers"));
		this.setLayout(new BorderLayout());
		JPanel jPanel1 = new JPanel();

		GridLayout gridLayout1 = new GridLayout();
		jPanel1.setLayout(gridLayout1);
		jLabel1.setText(genericLabels.getString("nameLabel"));
		jPanel2.setLayout(gridBagLayout1);
		nameField.setEditable(false);
		jLabel2.setText(genericLabels.getString("typeLabel"));
		sourceTypeCombo.addActionListener(e -> sourceTypeSelected());
		jPanel3.setEnabled(false);
		jPanel3.setBorder(titledBorder1);
		jPanel3.setLayout(borderLayout2);
		groupMemberList.setEnabled(false);
		groupMemberList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		upMemberButton.setEnabled(false);
		upMemberButton.setText(genericLabels.getString("upAbbr"));
		upMemberButton.addActionListener(this::upMemberButton_actionPerformed);
		downMemberButton.setEnabled(false);
		downMemberButton.setText(genericLabels.getString("down"));
		downMemberButton.addActionListener(this::downMemberButton_actionPerformed);
		deleteMemberButton.setEnabled(false);
		deleteMemberButton.setText(genericLabels.getString("delete"));
		deleteMemberButton.addActionListener(this::deleteMemberButton_actionPerformed);
		addMemberButton.setEnabled(false);
		addMemberButton.setText(genericLabels.getString("add"));
		addMemberButton.addActionListener(this::addMemberButton_actionPerformed);
		jPanel4.setLayout(flowLayout1);
		flowLayout1.setHgap(15);
		gridLayout1.setColumns(4);
		gridLayout1.setHgap(5);
		sourceTypeCombo.addActionListener(e -> sourceTypeSelected());
		this.add(entityOpsPanel, BorderLayout.SOUTH);
		this.add(jPanel1, BorderLayout.CENTER);
		jPanel1.add(jPanel2, null);
		jPanel2.add(jLabel1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
			GridBagConstraints.NONE, new Insets(2, 10, 2, 2), 0, 0));
		jPanel2.add(nameField, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 2, 5, 12), 0, 0));
		jPanel2.add(jPanel3, new GridBagConstraints(0, 2, 3, 1, 1.0, 1.0,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(8, 3, 3, 3), 61, 46));
		jPanel3.add(jScrollPane1, BorderLayout.CENTER);
		jPanel3.add(jPanel4, BorderLayout.SOUTH);
		jPanel4.add(addMemberButton, null);
		jPanel4.add(deleteMemberButton, null);
		jPanel4.add(upMemberButton, null);
		jPanel4.add(downMemberButton, null);
		jPanel2
			.add(sourceTypeCombo, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 2, 10, 12),
				0, 0));
		jPanel2.add(jLabel2, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
			GridBagConstraints.NONE, new Insets(2, 0, 2, 2), 0, 0));
		jScrollPane1.getViewport().add(groupMemberList, null);
		jPanel1.add(propertiesEditPanel, null);
	}

	/**
	  Called when a selection is made on the source-type combo box.
	  Enables/Disables the GUI controls appropriately for this type of 
	  data source.
	*/
	private void sourceTypeSelected()
	{
		enabling();
		String dsType = (String)sourceTypeCombo.getSelectedItem();

		DbEnum dsEnum = getDb().enumList.getEnum(Constants.enum_DataSourceType);
		if (dsEnum != null)
		{
			EnumValue dsEv = dsEnum.findEnumValue(dsType);
			if (dsEv != null)
			{
				try
				{
					Class<?> dsClass = dsEv.getExecClass();
					Constructor<?> constructor = dsClass.getConstructor(DataSource.class, Database.class);
					DataSourceExec exec = (DataSourceExec)constructor.newInstance(dataSource,getDb());
					propertiesEditPanel.getModel().setPropertiesOwner(exec);
				}
				catch(Exception ex) {
					log.error("Error setting properties for '{}'",dsType,ex);
				}
			}
		}
	}

	/**
	  Called when the 'Add Group Member' button is pressed.
	  Starts a modal DataSourceSelectDialog.
	  @param e ignored.
	*/
	void addMemberButton_actionPerformed(ActionEvent e)
	{
		DataSourceSelectDialog dlg = new DataSourceSelectDialog();
		dlg.exclude(dataSource.getName());
		for(Iterator it = dataSource.groupMembers.iterator(); it.hasNext(); )
		{
			DataSource ds = (DataSource)it.next();
			dlg.exclude(ds.getName());
		}
		if (dlg.count() == 0)
		{
			TopFrame.instance().showError(
				dbeditLabels.getString("SourceEditPanel.noMoreSources"));
			return;
		}
		launchDialog(dlg);
		if (dlg.okPressed())
		{
			DataSource nds = dlg.getSelection();
			if (nds != null)
			{
				dataSource.groupMembers.add(nds);
				groupMemberListModel.fireChanged();
			}
		}
	}

	/**
	  Called when the 'Delete Member' button is pressed.
	  @param e ignored.
	*/
	void deleteMemberButton_actionPerformed(ActionEvent e)
	{
		int idx = groupMemberList.getSelectedIndex();
		if (idx == -1)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.selectDelete"),
					getEntityName()));
			return;
		}

		dataSource.groupMembers.remove(idx);
		groupMemberListModel.fireChanged();
	} 

	/**
	  Called when the 'Up Member' button is pressed.
	  @param e ignored.
	*/
	void upMemberButton_actionPerformed(ActionEvent e)
	{
		int idx = groupMemberList.getSelectedIndex();
		if (idx == -1)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.selectDelete"),
					getEntityName()));
			return;
		}
		int prev = idx -1;
		if ( prev != -1 ) {
			DataSource p = (DataSource)groupMemberListModel.getObjectAt(prev);
			DataSource c = (DataSource)groupMemberListModel.getObjectAt(idx);
			groupMemberListModel.insertElementAt(c, prev);
			groupMemberListModel.insertElementAt(p, idx);
			groupMemberList.setSelectedIndex(prev);
		}
		groupMemberListModel.fireChanged();
 	}
	/**
	  Called when the 'Down Member' button is pressed.
	  @param e ignored.
	*/
	void downMemberButton_actionPerformed(ActionEvent e)
	{
		int idx = groupMemberList.getSelectedIndex();
		if (idx == -1)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.selectDelete"),
					getEntityName()));
			return;
		}
		int next = idx + 1;
		if ( next < groupMemberListModel.getSize() ) {
			DataSource c = (DataSource)groupMemberListModel.getObjectAt(idx);
			DataSource n = (DataSource)groupMemberListModel.getObjectAt(next);
			groupMemberListModel.insertElementAt(c, next);
			groupMemberListModel.insertElementAt(n, idx);
			groupMemberList.setSelectedIndex(next);
		}
		groupMemberListModel.fireChanged();
 	}

	/**
	 * From ChangeTracker interface.
	 * @return true if changes have been made to this
	 * screen since the last time it was saved.
	 */
	public boolean hasChanged()
	{
		getDataFromFields();
		return !dataSource.equals(origDataSource);
	}

	/**
	 * From ChangeTracker interface, save the changes back to the database 
	 * &amp; reset the hasChanged flag.
	 * @return true if object was successfully saved.
	 */
	public boolean saveChanges()
	{
		getDataFromFields();
		try
		{
			dataSource.write();
		}
		catch(DatabaseException e)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					genericLabels.getString("cannotSave"),
					getEntityName(), e.toString()));
			return false;
		}
		affectedItems = new ArrayList<DatabaseObject>();
		affectedItems.add(dataSource);

		// Replace the old datasource in the list.
		// This also updates the SourceListPanel.
		getDb().dataSourceList.getList().remove(origDataSource);
		getDb().dataSourceList.add(dataSource);
		parent.getSourcesListPanel().tableModel.fireTableDataChanged();

		// Replace DataSource in every Group using this data source
		for(Iterator<DataSource> it = getDb().dataSourceList.getList().iterator(); it.hasNext(); )
		{
			DataSource ds = it.next();
			if (ds.getName().equalsIgnoreCase(dataSource.getName()))
				continue;
			if (ds.dataSourceType.toLowerCase().endsWith("roup"))
			{
				boolean changed = false;
				for(int i = 0; i < ds.groupMembers.size(); i++)
				{
					DataSource ds2 = (DataSource)ds.groupMembers.elementAt(i);
					if (ds2 != null && ds2.getName().equalsIgnoreCase(dataSource.getName()))
					{
						ds.groupMembers.setElementAt(dataSource, i);
						changed = true;
						break;
					}
				}
				if (changed)
				{
					affectedItems.add(ds);
					try { ds.write(); }
					catch (DatabaseException e)
					{
						log.warn("Cannot write data source '{}'",ds.getName(),e.toString());
					}
				}
			}
		}

		// Replace data source in every RoutingSpec using this source or a
		// group containing this data source.
		DataSource newDataSource = null;
		for(Iterator<RoutingSpec> it = getDb().routingSpecList.iterator();
			it.hasNext(); )
		{
			RoutingSpec rs = it.next();
			boolean affected = false;
			if (rs.dataSource != null)
			{
				for(DatabaseObject dbo : affectedItems)
					if (dbo instanceof DataSource
					 && rs.dataSource.getName().equalsIgnoreCase(
							((DataSource)dbo).getName())  )
					{
						affected = true;
						newDataSource = ((DataSource)dbo);
						break;
					}
			}
						
			if (affected)
			{
				rs.dataSource = newDataSource;
				affectedItems.add(rs);
				try { rs.write(); }
				catch (DatabaseException e)
				{
					log.warn("Cannot write routing spec '{}'", rs.getName(),e);
				}
			}
		}

		// Make a new copy in case user wants to keep editing.
		origDataSource = dataSource;
		dataSource = origDataSource.copy();
		setTopObject(origDataSource);
		return true;
	}

	/** @see EntityOpsController */
	public String getEntityName() { return "DataSource"; }

	/** @see EntityOpsController */
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
				genericLabels.getString("saveChanges"));
			if (r == JOptionPane.CANCEL_OPTION) {
				return;
			}
			else if (r == JOptionPane.YES_OPTION)
			{
				if (!saveChanges())
					return;
			}

		}
		if ( dataSource != null && dataSource.getId() == Constants.undefinedId
		 && !getDb().getDbIo().getDatabaseType().equals("XML"))
		{
			DataSource stale = getDb().dataSourceList.get(dataSource.getName());
			getDb().dataSourceList.remove(stale);
		}
		DbEditorTabbedPane tp = parent.getSourcesTabbedPane();
		tp.remove(this);
	}

	/**
	 * Called from File - CloseAll to close this tab, abandoning any changes.
	 */
	public void forceClose()
	{
		DbEditorTabbedPane tp = parent.getSourcesTabbedPane();
		tp.remove(this);
	}

	@Override
	public void help()
	{
		Help.open();
	}
}

class GroupMemberListModel extends AbstractListModel
{
	DataSource theDs;

	GroupMemberListModel(DataSource ds)
	{
		theDs = ds;
	}

	public int getSize()
	{
		int ret = theDs.groupMembers.size();
		return ret;
	}

	public Object getObjectAt(int index) {
		DataSource ds = theDs.groupMembers.elementAt(index);
		return(ds);
	}
	public Object getElementAt(int index)
	{
		DataSource ds = theDs.groupMembers.elementAt(index);
		return ds != null ? ds.getName() : "";
	}
	public void insertElementAt(DataSource ds, int index)
	{
		theDs.groupMembers.setElementAt(ds, index);
	}

	public void fireChanged()
	{
		this.fireContentsChanged(this, 0, getSize());
	}
}
