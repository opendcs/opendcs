/*
 *  $Id$
 */
package decodes.dbeditor;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

import javax.swing.table.*;
import javax.swing.border.*;
import java.util.ResourceBundle;

import ilex.gui.Help;
import ilex.util.TextUtil;

import decodes.db.*;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.sql.DecodesDatabaseVersion;
import decodes.xml.XmlDatabaseIO;

/**
 * Panel to edit an open PresentationGroup. This panel is opened from the
 * PresentationGroupListPanel.
 */
@SuppressWarnings("serial")
public class PresentationGroupEditPanel extends DbEditorTab implements
	ChangeTracker, EntityOpsController
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	EntityOpsPanel entityOpsPanel = new EntityOpsPanel(this);
	TitledBorder titledBorder1;

	DbEditorFrame parent;
	PresentationGroup theObject, origObject;
	DataPresentationTableModel dataPresentationTableModel;
	int dpIdx;
	JTextField nameField = new JTextField();
	JCheckBox isProductionCheckBox = new JCheckBox();
    PresentationGroupCombo inheritsFromCombo = new PresentationGroupCombo();

	SortingListTable dataPresentationTable = null;
	private boolean supportsMinMaxValue = false;

	/** no-args ctor for JBuilder */
	public PresentationGroupEditPanel()
	{
		try
		{
			jbInit();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	/**
	 * Construct new panel to edit specified object.
	 * 
	 * @param ob
	 *            the object to edit in this panel.
	 */
	public PresentationGroupEditPanel(PresentationGroup ob)
	{
		dpIdx = -1;
		try
		{
			jbInit();
			origObject = ob;
			theObject = origObject.copy();
			setTopObject(origObject);

			dataPresentationTable.getSelectionModel().setSelectionMode(
				ListSelectionModel.SINGLE_SELECTION);
			fillFields();
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
	 * @param parent Enclosing frame
	 */
	void setParent(DbEditorFrame parent)
	{
		this.parent = parent;
	}

	/** Fills the GUI controls with values from the object. */
	private void fillFields()
	{
		nameField.setText(theObject.groupName);
		inheritsFromCombo.exclude(theObject.groupName);
		if (theObject.inheritsFrom != null)
			inheritsFromCombo.setSelection(theObject.inheritsFrom);
		else
			inheritsFromCombo.setSelectedIndex(0); // no selection
		isProductionCheckBox.setSelected(theObject.isProduction);
		dataPresentationTableModel.setObject(theObject);
	}

	/**
	 * Gets the data from the fields & puts it back into the object.
	 * 
	 * @return the internal copy of the object being edited.
	 */
	private boolean getDataFromFields()
	{
		theObject.inheritsFrom = inheritsFromCombo.getSelectedIndex() == 0
			? null : (String)inheritsFromCombo.getSelectedItem();
		
		theObject.isProduction = isProductionCheckBox.isSelected();

		theObject.clearList();
		for(DataPresentation dp : dataPresentationTableModel.dps)
			theObject.addDataPresentation(dp);

		return true;
	}

	/** Initializes GUI components */
	private void jbInit() throws Exception
	{
		DatabaseIO dbio = Database.getDb().getDbIo();
		if (dbio instanceof XmlDatabaseIO)
			supportsMinMaxValue = true;
		else
			if (dbio.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_10)
				supportsMinMaxValue = true;

		dataPresentationTableModel = new DataPresentationTableModel(supportsMinMaxValue);
		dataPresentationTable = new SortingListTable(
			dataPresentationTableModel,
			DataPresentationTableModel.columnWidths);

		JButton deletePresentationButton = new JButton(genericLabels.getString("delete"));
		deletePresentationButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					deletePresentationPressed();
				}
			});

		JButton addPresentationButton = new JButton(genericLabels.getString("add"));
		addPresentationButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					addPresentationPressed();
				}
			});

		JPanel peButtonPanel = new JPanel(new GridBagLayout());
		
		JButton editPEButton = new JButton(genericLabels.getString("edit"));
		editPEButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					editPEPressed();
				}
			});
		
		JPanel pePanel = new JPanel(new BorderLayout());

		JPanel titlePanel = new JPanel(new GridBagLayout());
		isProductionCheckBox.setText(dbeditLabels.getString("PresentationGroupEditPanel.production"));
		nameField.setEditable(false);
		titledBorder1 = new TitledBorder("");
		
		pePanel.setBorder(
			new TitledBorder(dbeditLabels.getString("PresentationGroupEditPanel.elements")));
			
		this.setLayout(new BorderLayout());
		JPanel topPanel = new JPanel(new BorderLayout());
		
		this.add(entityOpsPanel, BorderLayout.SOUTH);
		this.add(topPanel, BorderLayout.CENTER);
		
		topPanel.add(pePanel, BorderLayout.CENTER);
		
//		topPanel.add(jSplitPane1, BorderLayout.CENTER);
//		jSplitPane1.add(pePanel, JSplitPane.TOP);
		
		pePanel.add(peButtonPanel, BorderLayout.EAST);
		
		peButtonPanel.add(addPresentationButton, 
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(4, 8, 4, 8), 0, 0));
		peButtonPanel.add(editPEButton, 
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(4, 8, 4, 8), 0, 0));
		peButtonPanel.add(deletePresentationButton, 
			new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0, 
				GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
				new Insets(4, 8, 4, 8), 0, 0));
		
		JScrollPane peScrollPane = new JScrollPane();
		pePanel.add(peScrollPane, BorderLayout.CENTER);
		
		
//		JPanel rrPanel = new JPanel(new BorderLayout());
//		rrPanel.setBorder(BorderFactory.createLineBorder(SystemColor.controlText, 1));
//		jSplitPane1.add(rrPanel, JSplitPane.BOTTOM);
//		JPanel rrButtonPanel = new JPanel(new GridBagLayout());
//		rrPanel.add(rrButtonPanel, BorderLayout.EAST);
//		rrButtonPanel.add(addRoundingRuleButton, new GridBagConstraints(0, 0, 1, 1,
//			0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
//			new Insets(4, 8, 4, 8), 0, 0));
//		rrButtonPanel.add(deleteRoundingRuleButton, new GridBagConstraints(0, 1, 1,
//			1, 0.0, 1.0, GridBagConstraints.NORTH,
//			GridBagConstraints.HORIZONTAL, new Insets(4, 8, 4, 8), 0, 0));
//		rrPanel.add(roundingRulesTitlePanel, BorderLayout.NORTH);
//		roundingRulesTitlePanel.add(new JLabel(dbeditLabels.getString("PresentationGroupEditPanel.rr")), null);
//		roundingRulesTitlePanel.add(dataTypeField, null);
//		JScrollPane rrScrollPane = new JScrollPane();
//		rrPanel.add(rrScrollPane, BorderLayout.CENTER);
		
		topPanel.add(titlePanel, BorderLayout.NORTH);
		titlePanel.add(new JLabel(dbeditLabels.getString("PresentationGroupEditPanel.groupName")),
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(6, 15, 9, 0), 0, 0));
		titlePanel.add(nameField, 
			new GridBagConstraints(1, 0, 1, 1, 0.5, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
			new Insets(6, 0, 9, 10), 0, 0));
		titlePanel.add(new JLabel(dbeditLabels.getString("PresentationGroupEditPanel.inheritsFrom")), 
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(6, 15, 9, 0), 0, 0));
		titlePanel.add(inheritsFromCombo, 
			new GridBagConstraints(3, 0, 1, 1, 0.5, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(6, 0, 9, 2), 60, 0));
		titlePanel.add(isProductionCheckBox, 
			new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(6, 20, 9, 10), 0, 0));

		peScrollPane.getViewport().add(dataPresentationTable, null);

		dataPresentationTable.addMouseListener(
			new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 2)
						editPEPressed();
				}
			});

		
	}

	protected void editPEPressed()
	{
		int row = dataPresentationTable.getSelectedRow();
		if (row == -1)
		{
			parent.showError(
				dbeditLabels.getString("PresentationGroupEditPanel.selectEdit"));
			return;
		}
		DataPresentation pres = dataPresentationTableModel.getObjectAt(row);
		EditPresentationDialog dlg = new EditPresentationDialog(pres, supportsMinMaxValue);
		launchDialog(dlg);
		if (dlg.isCancelled())
			return;
		dataPresentationTableModel.rowUpdated(row);
	}

	/**
	 * Called when the 'Add Presentation' button is pressed.
	 */
	void addPresentationPressed()
	{
		DataPresentation newPres = new DataPresentation();
		newPres.setGroup(this.theObject);

		EditPresentationDialog dlg = new EditPresentationDialog(newPres, supportsMinMaxValue);
		launchDialog(dlg);
		if (dlg.isCancelled())
			return;
		
		if (dataPresentationTableModel.findByDataType(newPres.getDataType()) != null)
		{
			parent.showError(
				dbeditLabels.getString("PresentationGroupEditPanel.dtAlreadyExists"));
			return;
		}
		dataPresentationTableModel.add(newPres);
	}

	/**
	 * Called when the 'Delete Presentation' button is pressed.
	 */
	void deletePresentationPressed()
	{
		int idx = dataPresentationTable.getSelectedRow();
		if (idx == -1)
		{
			DbEditorFrame.instance().showError(
				dbeditLabels.getString("PresentationGroupEditPanel.selectDelete"));
			return;
		}

		int rsp = JOptionPane.showConfirmDialog(this, 
			dbeditLabels.getString("PresentationGroupEditPanel.confirmDelete"),
			dbeditLabels.getString("PresentationGroupEditPanel.confirmDeleteTitle"),
			JOptionPane.YES_NO_OPTION);
		if (rsp == JOptionPane.YES_OPTION)
			dataPresentationTableModel.removeAt(idx);
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
		if (!getDataFromFields())
			return false;

		// Write the changes out to the database.
		try
		{
			theObject.lastModifyTime = new Date();
			theObject.write();
		}
		catch (DatabaseException e)
		{
			DbEditorFrame.instance().showError(e.toString());
			return false;
		}

		// Replace origConfig in ConfigList with the modified config.
		Database.getDb().presentationGroupList.remove(origObject);
		Database.getDb().presentationGroupList.add(theObject);

		// Make a new copy in case user wants to keep editing.
		int idx = dataPresentationTable.getSelectedRow();
		origObject = theObject;
		theObject = origObject.copy();
		setTopObject(origObject);
		dataPresentationTableModel.setObject(theObject);
		if (idx >= 0 && idx < dataPresentationTable.getRowCount())
		{
			dataPresentationTable.setRowSelectionInterval(idx, idx);
//			valueChanged(null);
		}
		return true;
	}

	/** @see EntityOpsController */
	public String getEntityName()
	{
		return "PresentationGroup";
	}

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
		DbEditorTabbedPane dbtp = parent.getPresentationTabbedPane();
		dbtp.remove(this);
	}

	/**
	 * Called from File - CloseAll to close this tab, abandoning any changes.
	 */
	public void forceClose()
	{
		DbEditorTabbedPane dbtp = parent.getPresentationTabbedPane();
		dbtp.remove(this);
	}

	@Override
	public void help()
	{
		Help.open();
	}

//	/**
//	 * Called when list selection made in dataPresentationTable. Populates the
//	 * lower part of the screen with the rouding rules for the selected
//	 * data-presentation.
//	 * 
//	 * @param e
//	 *            ignored.
//	 */
//	public void valueChanged(ListSelectionEvent e)
//	{
//		int idx = dataPresentationTable.getSelectedRow();
//		if (idx == dpIdx)
//			return;
//		dpIdx = idx;
//		if (dpIdx == -1)
//			setRoundingRulesView(null);
//		else
//			setRoundingRulesView(dataPresentationTableModel.getObjectAt(dpIdx));
//	}

//	/**
//	 * Populates the lower part of the screen with the rouding rules for the
//	 * selected data-presentation.
//	 */
//	private void setRoundingRulesView(DataPresentation dp)
//	{
//		roundingRulesTableModel.setObject(dp);
//		if (dp != null)
//			dataTypeField.setText(dp.getDataType().toString());
//	}

//	/**
//	 * Called when the 'Select Equipment Model' button is pressed.
//	 * 
//	 * @param e
//	 *            ignored.
//	 */
//	void equipmentModelButton_actionPerformed(ActionEvent e)
//	{
//		int idx = dataPresentationTable.getSelectedRow();
//		DataPresentation dp = null;
//		if (idx != -1)
//			dp = dataPresentationTableModel.getObjectAt(idx);
//		if (idx == -1 || dp == null)
//		{
//			DbEditorFrame.instance().showError(
//				dbeditLabels
//					.getString("PresentationGroupEditPanel.selectPresEq"));
//			return;
//		}
//
//		EquipmentModelSelectDialog dlg = new EquipmentModelSelectDialog();
//		if (dp.getEquipmentModelName() != null && dp.getEquipmentModelName().length() > 0)
//			dlg.setSelection(Database.getDb().equipmentModelList
//				.get(dp.getEquipmentModelName()));
//
//		launchDialog(dlg);
//		if (!dlg.cancelled())
//		{
//			EquipmentModel mod = dlg.getSelectedEquipmentModel();
//			if (mod == null)
//				dp.setEquipmentModelName("");
//			else
//				dp.setEquipmentModelName(mod.name);
//			dataPresentationTable.repaint();
//		}
//	}
}

@SuppressWarnings("serial")
class DataPresentationTableModel 
	extends AbstractTableModel 
	implements SortingListTableModel
{
	static String columnNames[] =
	{
		PresentationGroupEditPanel.dbeditLabels.getString("PresentationGroupEditPanel.dtStandard"),
		PresentationGroupEditPanel.dbeditLabels.getString("PresentationGroupEditPanel.dtCode"),
		PresentationGroupEditPanel.genericLabels.getString("units"),
		PresentationGroupEditPanel.dbeditLabels.getString("PresentationGroupEditPanel.maxDec"),
		"Min Value",
		"Max Value"
	};
	static int columnWidths[] = { 17,17,17,17,17,17 };
	private NumberFormat numFmt = NumberFormat.getNumberInstance();

	ArrayList<DataPresentation> dps = new ArrayList<DataPresentation>();
	private int sortColumn = 0;

	public DataPresentationTableModel(boolean supportsMinMaxVersion)
	{
		numFmt.setGroupingUsed(false);
		numFmt.setMaximumFractionDigits(5);
	}

	public void removeAt(int idx)
	{
		dps.remove(idx);
		this.fireTableDataChanged();
	}

	void setObject(PresentationGroup ob)
	{
		dps.clear();
		dps.addAll(ob.dataPresentations);
		fireTableDataChanged();
	}
	
	void rowUpdated(int row)
	{
		fireTableDataChanged();
	}
	
	void add(DataPresentation dp)
	{
		dps.add(dp);
		sortByColumn(sortColumn);
	}

	public int getRowCount()
	{
		return dps.size();
	}

	public int getColumnCount()
	{
		int ret = columnNames.length;
		return ret;
	}

	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	public DataPresentation getObjectAt(int r)
	{
		if (r < 0 || r >= getRowCount())
			return null;
		return dps.get(r);
	}

	public Object getValueAt(int r, int c)
	{
		DataPresentation dp = getObjectAt(r);
		if (dp == null)
			return "";
		switch (c)
		{
		case 0:
			return dp.getDataType() == null ? "" : dp.getDataType().getStandard();
		case 1:
			return dp.getDataType() == null ? "" : dp.getDataType().getCode();
		case 2:
			return dp.getUnitsAbbr() == null ? "" : dp.getUnitsAbbr();
		case 3:
		{
			int md = dp.getMaxDecimals();
			return md < 0 || md > 10 ? "" : md;
		}
		case 4:
			return dp.getMinValue() == Constants.undefinedDouble ? "" :
				numFmt.format(dp.getMinValue());
		case 5:
			return dp.getMaxValue() == Constants.undefinedDouble ? "" :
				numFmt.format(dp.getMaxValue());
		default:
			return "";
		}
	}
	
	/**
	 * @return the data presentation for the passed data type, or null if none exists.
	 * @param dt the data type
	 */
	public DataPresentation findByDataType(DataType dt)
	{
		for (DataPresentation dp : dps)
			if (dp.getDataType() == dt)
				return dp;
		return null;
	}

	@Override
	public void sortByColumn(int column)
	{
		sortColumn = column;
		Collections.sort(dps, new Comparator<DataPresentation>()
		{
			@Override
			public int compare(DataPresentation o1, DataPresentation o2)
			{
				int ret = 0;
				switch (sortColumn)
				{
				case 2:
					ret = TextUtil.strCompareIgnoreCase(o1.getUnitsAbbr(), o2.getUnitsAbbr());
					if (ret != 0)
						return ret;
				case 3:
					ret = o1.getMaxDecimals() - o2.getMaxDecimals();
					if (ret != 0)
						return ret;
				case 0:
					ret = o1.getDataType().getStandard().compareTo(o2.getDataType().getStandard());
					if (ret != 0)
						return ret;
					// else fall through and compare data type code
				case 1:
				default:
					return o1.getDataType().getCode().compareTo(o2.getDataType().getCode());
				}
			}
		});
		fireTableDataChanged();
	}

	@Override
	public Object getRowObject(int row)
	{
		return dps.get(row);
	}
}

//class RoundingRulesTableModel extends AbstractTableModel
//{
//	static String columnNames[] =
//	{
//		PresentationGroupEditPanel.dbeditLabels
//			.getString("PresentationGroupEditPanel.upperLim"),
//		PresentationGroupEditPanel.dbeditLabels
//			.getString("PresentationGroupEditPanel.sigdig") };
//
//	private DataPresentation theObject;
//
//	public RoundingRulesTableModel(DataPresentation ob)
//	{
//		setObject(theObject);
//	}
//
//	public void setObject(DataPresentation ob)
//	{
//		theObject = ob;
//		fillValues();
//	}
//
//	public void fillValues()
//	{
//		fireTableDataChanged();
//	}
//
//	public int getRowCount()
//	{
//		return theObject == null ? 0 : theObject.roundingRules.size();
//	}
//
//	public int getColumnCount()
//	{
//		return columnNames.length;
//	}
//
//	public String getColumnName(int col)
//	{
//		return columnNames[col];
//	}
//
//	public RoundingRule getObjectAt(int r)
//	{
//		if (r < 0 || r >= getRowCount())
//			return null;
//		return (RoundingRule) theObject.roundingRules.elementAt(r);
//	}
//
//	public Object getValueAt(int r, int c)
//	{
//		RoundingRule rr = getObjectAt(r);
//		if (rr == null)
//			return null;
//
//		switch (c)
//		{
//		case 0:
//			if (rr.getUpperLimit() == Double.MAX_VALUE)
//				return "max";
//			else
//				return "" + rr.getUpperLimit();
//		case 1:
//			return "" + rr.sigDigits;
//		default:
//			return "";
//		}
//	}
//
//	public boolean isCellEditable(int r, int c)
//	{
//		return true;
//	}
//
//	public void setValueAt(Object aValue, int r, int c)
//	{
//		String s = (String) aValue;
//		RoundingRule rr = getObjectAt(r);
//		if (rr == null)
//			return;
//
//		if (c == 0 && s.equalsIgnoreCase("max"))
//			rr.setUpperLimit(Double.MAX_VALUE);
//		else
//		{
//			double v = 0.0;
//			try
//			{
//				v = Double.parseDouble(s);
//			}
//			catch (NumberFormatException e)
//			{
//				DbEditorFrame
//					.instance()
//					.showError(
//						columnNames[c]
//							+ " "
//							+ PresentationGroupEditPanel.dbeditLabels
//								.getString("PresentationGroupEditPanel.mustBeNumber"));
//				return;
//			}
//
//			switch (c)
//			{
//			case 0:
//				rr.setUpperLimit(v);
//				break;
//			case 1:
//				rr.sigDigits = (int) v;
//				break;
//			}
//		}
//
//		fireTableCellUpdated(r, c);
//	}
//}
