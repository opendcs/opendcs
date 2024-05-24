/*
 *  $Id$
 */

package decodes.gui;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;

import org.cobraparser.html.style.TableCellRenderState;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.TreeSet;
import java.util.Enumeration;

import javax.swing.border.*;

import decodes.util.DynamicPropertiesOwner;
import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;

import java.awt.event.*;

import ilex.util.Logger;
import ilex.util.StringPair;
import ilex.util.TextUtil;

/**
 * A panel that allows you to edit a group of Properties.
 * 
 * @see PropertiesEditDialog
 */
@SuppressWarnings("serial")
public class PropertiesEditPanel extends JPanel
{
	private static ResourceBundle genericLabels = null;
	private JScrollPane jScrollPane1 = new JScrollPane();
//	private SortingListTable propertiesTable;
	private JTable propertiesTable;
	private PropertiesTableModel ptm;
	private TitledBorder titledBorder1;
	private JButton editButton = new JButton();
	private JButton addButton = new JButton();
	private GridBagLayout gridBagLayout1 = new GridBagLayout();
	private JDialog ownerDialog;
	private JFrame ownerFrame;
	private HashMap<String, PropertySpec> propHash = null;
	private PropertiesOwner propertiesOwner = null;

	/** Will be true after any changes were made. */
	public boolean changesMade;

	/**
	 * Constructs a PropertiesEditPanel for the passed Properties set.
	 * 
	 * @param properties
	 *            The properties set to edit.
	 */
	public PropertiesEditPanel(Properties properties)
	{
		this(properties, true);
	}

	public PropertiesEditPanel(Properties properties, boolean canAddAndDelete)
	{
		try
		{
			genericLabels = PropertiesEditDialog.getGenericLabels();
			ownerDialog = null;
			ownerFrame = null;
			ptm = new PropertiesTableModel(properties);
			propertiesTable = new SortingListTable(ptm, new int[]{30, 70});
			propertiesTable.getTableHeader().setReorderingAllowed(false);
			jbInit(canAddAndDelete);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		changesMade = false;
	}

	public void setTitle(String title)
	{
		titledBorder1.setTitle(title);
	}

	/**
	 * Sets the owner dialog (if there is one).
	 * 
	 * @param dlg
	 *            the owner dialog
	 */
	public void setOwnerDialog(JDialog dlg)
	{
		ownerDialog = dlg;
	}

	/**
	 * Sets the owner frame.
	 * 
	 * @param frm
	 *            the owner frame
	 */
	public void setOwnerFrame(JFrame frm)
	{
		ownerFrame = frm;
	}

	/**
	 * Populates the panel with the passed set.
	 * 
	 * @param properties
	 *            The properties to edit.
	 */
	public void setProperties(Properties properties)
	{
		ptm.setProperties(properties);
		changesMade = false;
	}

	/*
	 * This internal class is used to add tooltip for known properties in the
	 * table
	 */
	class ttCellRenderer extends DefaultTableCellRenderer
	{
		public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int col)
		{
			JLabel cr = (JLabel) super.getTableCellRendererComponent(table, value, isSelected,
				hasFocus, row, col);
			cr.setOpaque(false);
			if (propHash != null)
			{
				// property name is in column 0
				int modelRow = table.convertRowIndexToModel(row);
				String pn = ((String) ptm.getValueAt(modelRow, 0)).toUpperCase();
				PropertySpec ps = propHash.get(pn);
				cr.setToolTipText(ps != null ? ps.getDescription() : "");
			}
			if (value instanceof Color)
			{
				Color c = (Color)value;
				cr.setOpaque(true);
				cr.setBackground(c);
				cr.setText("0x" + Integer.toHexString(c.getRGB()).substring(2));
			}
			return cr;
		}
	}

	/** Initializes GUI components. */
	private void jbInit(boolean canAddAndDelete) throws Exception
	{
		titledBorder1 = new TitledBorder(
			BorderFactory.createLineBorder(new Color(153, 153, 153), 2),
			genericLabels.getString("properties"));
		this.setLayout(gridBagLayout1);
		this.setBorder(titledBorder1);
		JButton deleteButton = new JButton(genericLabels.getString("delete"));
		deleteButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				deleteButton_actionPerformed(e);
			}
		});
		addButton.setText(genericLabels.getString("add"));
		addButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				addButton_actionPerformed(e);
			}
		});
		editButton.setText(genericLabels.getString("edit"));
		editButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				editPressed();
			}
		});
		this.add(jScrollPane1, 
			new GridBagConstraints(0, 0, 1, 3, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, 
				new Insets(2, 4, 2, 4), 0, 0));
		if (canAddAndDelete)
			this.add(addButton, 
				new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 
					GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
					new Insets(2, 4, 2, 4), 20, 0));
		this.add(editButton,
			new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 4, 2, 4), 20, 0));
		if (canAddAndDelete)
			this.add(deleteButton, 
				new GridBagConstraints(1, 2, 1, 1, 0.0, 1.0,
					GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, 
					new Insets(2, 4, 2, 4), 20, 0));
		jScrollPane1.getViewport().add(propertiesTable, null);
		propertiesTable.setModel(ptm);
		
		propertiesTable.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2)
				{
					editPressed();
				}
			}
		} );

	}

	/**
	 * Called when 'Add' button is pressed. Displays a sub-dialog for the user
	 * to enter name and value.
	 * 
	 * @param e
	 *            ignored.
	 */
	void addButton_actionPerformed(ActionEvent e)
	{
		PropertyEditDialog dlg = null;
		PropertySpec propSpec = null;
		
		if (propertiesOwner != null
		 && (propertiesOwner instanceof DynamicPropertiesOwner)
		 && ((DynamicPropertiesOwner)propertiesOwner).dynamicPropsAllowed())
		{
			propSpec = new PropertySpec("", PropertySpec.STRING, "");
			propSpec.setDynamic(true);
		}
			
		if (ownerDialog != null)
			dlg = new PropertyEditDialog(ownerDialog, "", "", propSpec);
		else if (ownerFrame != null)
			dlg = new PropertyEditDialog(ownerFrame, "", "", propSpec);
		else
			dlg = new PropertyEditDialog(TopFrame.instance(), "", "", propSpec);
		dlg.setLocation(50, 50);
		dlg.setLocationRelativeTo(this);
		dlg.setVisible(true);
		StringPair sp = dlg.getResult();
		if (sp != null)
			ptm.add(sp);
		else
			return;
		
		if (propSpec != null && propSpec.isDynamic())
		{
			((DynamicPropertiesOwner)propertiesOwner).setDynamicPropDescription(
				sp.first, propSpec.getDescription());
			propHash.put(propSpec.getName().toUpperCase(), propSpec);
//System.out.println("Put '" + propSpec.getName() + "' desc=" + propSpec.getDescription()
//+ " into propHash, dynamic=" + propSpec.isDynamic());
//PropertySpec tps = propHash.get(propSpec.getName());
//if (tps == null)
//System.out.println("After putting, propHash still doesn't have '" + propSpec.getName() + "'");
		}
		
		changesMade = true;
	}

	/**
	 * Called when the 'Edit' button is pressed. Displays a sub-dialog for user
	 * to edit the selected name/value.
	 * 
	 * @param e
	 *            ignored.
	 */
	void editPressed()
	{
		int tablerow = propertiesTable.getSelectedRow();
		if (tablerow == -1)
			return;
		//Get the correct row from the table model
		int modelrow = propertiesTable.convertRowIndexToModel(tablerow);
		StringPair sp = ptm.propAt(modelrow);
		PropertySpec propSpec = null;
		if (propHash != null)
			propSpec = propHash.get(sp.first.toUpperCase());
//System.out.println("Editing prop '" + sp.first + "' isDynamic=" + 
//(propSpec != null && propSpec.isDynamic()));

		PropertyEditDialog dlg = null;
		Logger.instance().debug3("Editing propspec=" + propSpec);
		if (ownerDialog != null)
			dlg = new PropertyEditDialog(ownerDialog, sp.first, sp.second, propSpec);
		else if (ownerFrame != null)
			dlg = new PropertyEditDialog(ownerFrame, sp.first, sp.second, propSpec);
		else
			dlg = new PropertyEditDialog(TopFrame.instance(), sp.first, sp.second, propSpec);
		dlg.setLocation(50, 50);
		dlg.setLocationRelativeTo(this);
		dlg.setVisible(true);
		StringPair res = dlg.getResult();
		if (res != null)
		{
			ptm.setPropAt(modelrow, res);
			changesMade = true;
			saveChanges();
		}
		
		if (propSpec != null && propSpec.isDynamic())
		{
			((DynamicPropertiesOwner)propertiesOwner).setDynamicPropDescription(
				propSpec.getName(), propSpec.getDescription());
			propHash.put(propSpec.getName().toUpperCase(), propSpec);
		}
	}

	/**
	 * Called when the 'Delete' button is pressed. Removes the selected property
	 * from the table.
	 * 
	 * @param e
	 *            ignored.
	 */
	void deleteButton_actionPerformed(ActionEvent e)
	{
		int r = propertiesTable.getSelectedRow();
		if (r == -1)
			return;
		StringPair sp = ptm.propAt(r);
		PropertySpec propSpec = 
			propHash == null ? null : propHash.get(sp.first.toUpperCase());
		if (propSpec != null)
		{
			propHash.remove(sp.first.toUpperCase());
			if (propSpec.isDynamic())
				((DynamicPropertiesOwner)propertiesOwner).setDynamicPropDescription(
					propSpec.getName(), null);

		}

		ptm.deletePropAt(r);
		changesMade = true;
	}

	/** @return true if anything in the table has been changed. */
	public boolean hasChanged()
	{
		return ptm.hasChanged();
	}

	/** Saves the changes back to the properties set. */
	public void saveChanges()
	{
		ptm.saveChanges();
	}

	/**
	 * Used by the RetProcAdvancedPanel class to repaint the table when the new
	 * button is pressed.
	 */
	public void redrawTable()
	{
		ptm.redrawTable();
	}

	/**
	 * Adds empty properties with the passed names, but doesn't overwrite any
	 * existing property settings.
	 * 
	 * @param propnames
	 *            array of property names.
	 */
	public void addEmptyProps(String[] propnames)
	{
		ptm.addEmptyProps(propnames);
	}

	/**
	 * Adds a default property name and value, only if a property with this name
	 * doesn't already exist.
	 */
	// public void addDefaultProperty(String name, String value)
	// {
	// for(int i=0; i<ptm.props.size(); i++)
	// if (name.equals(((Property)ptm.props.elementAt(i)).name))
	// return;
	// ptm.props.add(new Property(name, value));
	// System.out.println("Added new property: " + name + "=" + value);
	// ptm.fireTableDataChanged();
	// }

	/**
	 * Return the property value if the name is defined in the table, or null if
	 * not.
	 * 
	 * @param name
	 *            the name of the property
	 * @return the value of the property or null if undefined.
	 */
	public String getProperty(String name)
	{
		for (int row = 0; row < this.ptm.getRowCount(); row++)
		{
			StringPair sp = ptm.propAt(row);
			if (sp != null && sp.first.equalsIgnoreCase(name))
				return sp.second;
		}
		return null;
	}
	
	public void rmProperty(String name)
	{
		for (int row = 0; row < this.ptm.getRowCount(); row++)
		{
			StringPair sp = ptm.propAt(row);
			if (sp != null && sp.first.equalsIgnoreCase(name))
			{
				ptm.deletePropAt(row);
				changesMade = true;
				return;
			}
		}
		
	}
	
	public void setProperty(String name, String value)
	{
		for (int row = 0; row < this.ptm.getRowCount(); row++)
		{
			StringPair sp = ptm.propAt(row);
			if (sp != null && sp.first.equalsIgnoreCase(name))
			{
				sp.second = value;
				ptm.setPropAt(row, sp);
				return;
			}
		}
		// Fell through -- this is a new property
		ptm.add(new StringPair(name, value));
	}

	/**
	 * Call this method before setProperties(). The known properties will be
	 * displayed in the table along with tool tips. Editor dialogs for known
	 * properties will be tailored to the data type.
	 * 
	 * @param propertiesOwner
	 *            The object owning the properties.
	 */
	public void setPropertiesOwner(PropertiesOwner propertiesOwner)
	{
		this.propertiesOwner = propertiesOwner;
		if (propertiesOwner == null)
		{
			propHash = null;
			return;
		}
		// For quick access, construct a hash with upper-case names.
		propHash = new HashMap<String, PropertySpec>();
		for (PropertySpec ps : propertiesOwner.getSupportedProps())
			propHash.put(ps.getName().toUpperCase(), ps);
		if (propertiesOwner instanceof DynamicPropertiesOwner)
		{
			DynamicPropertiesOwner dpo = (DynamicPropertiesOwner)propertiesOwner;
			if (dpo.dynamicPropsAllowed())
				for(PropertySpec ps : dpo.getDynamicPropSpecs())
					propHash.put(ps.getName().toUpperCase(), ps);
		}

		// Set column renderer so that tooltip is property description
		TableColumn col = propertiesTable.getColumnModel().getColumn(0);
		col.setCellRenderer(new ttCellRenderer());
		col = propertiesTable.getColumnModel().getColumn(1);
		col.setCellRenderer(new ttCellRenderer());
		ptm.setPropHash(propHash);
	}
	
}

/**
 * Table model for the properties shown in the panel. Table is two-columns for
 * name and value.
 */
@SuppressWarnings("serial")
class PropertiesTableModel extends AbstractTableModel
	implements SortingListTableModel
{
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(PropertiesTableModel.class);
	private static ResourceBundle genericLabels = PropertiesEditDialog.getGenericLabels();
	/** The properties as a list of StringPair object. */
	ArrayList<StringPair> props = new ArrayList<StringPair>();

	/** Column names */
	static String columnNames[];

	/** The Properties set that we're editing. */
	Properties origProps;

	/** flag to keep track of changes */
	boolean changed;

	HashMap<String, PropertySpec> propHash = null;

	/** Constructs a new table model for the passed Properties set. */
	public PropertiesTableModel(Properties properties)
	{
		columnNames = new String[2];
		columnNames[0] = genericLabels.getString("name");
		columnNames[1] = genericLabels.getString("PropertiesEditDialog.value");
		setProperties(properties);
	}

	/**
	 * Sets a hash of property specs for known properties for this object. Known
	 * properties will be shown in the table even if no value is assigned. Can
	 * be called multiple times if the property specs change. Subsequent calls
	 * will remove any unassigned properties that are no longer spec'ed.
	 * 
	 * @param propHash
	 *            the has of specs
	 */
	public void setPropHash(HashMap<String, PropertySpec> propHash)
	{
		this.propHash = propHash;
		for (String ucName : propHash.keySet())
		{
			boolean found = false;
			for (StringPair prop : props)
			{
				if (prop.first.equalsIgnoreCase(ucName))
				{
					found = true;
					break;
				}
			}
			if (!found)
			{
				StringPair sp = new StringPair(propHash.get(ucName).getName(), "");
				props.add(sp);
				Logger.instance().debug3("Added spec'ed property '" + sp.first + "'");
			}
		}
		// Now remove the unassigned props that are no longer spec'ed
		for (Iterator<StringPair> spit = props.iterator(); spit.hasNext();)
		{
			StringPair sp = spit.next();
			if ((sp.second == null || sp.second.length() == 0) // value
																// unassigned
				&& propHash.get(sp.first.toUpperCase()) == null) // not in spec
																	// hash
			{
				spit.remove();
				Logger.instance().debug3("Removed unspec'ed property '" + sp.first + "'");
			}
		}
		Logger.instance().debug3("Property table now has " + props.size() + " rows.");
		this.fireTableDataChanged();
	}

	/** Sets the properties set being edited. */
	public void setProperties(Properties properties)
	{
		origProps = properties;
		changed = false;

		if (origProps == null)
			return;

		props.clear();
		TreeSet<String> names = new TreeSet<String>();
		Enumeration<?> pe = properties.propertyNames();
		while (pe.hasMoreElements())
			names.add((String) pe.nextElement());
		if (propHash != null)
			for (PropertySpec ps : propHash.values())
				names.add(ps.getName());

		for (String name : names)
		{
			String value = properties.getProperty(name);
			if (value == null)
				value = "";
			props.add(new StringPair(name, value));
		}
	}

	/** Returns number of rows */
	public int getRowCount()
	{
		return props.size();
	}

	/** Returns number of columns */
	public int getColumnCount()
	{
		return columnNames.length;
	}

	/** Returns column name */
	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	/** Returns a String value at specified row/column */
	public Object getValueAt(int row, int column)
	{
		if (row >= getRowCount())
			return null;
		StringPair sp = props.get(row);
		if (column == 0)
			return sp.first;
		if (sp.first != null 
		 && (sp.first.toLowerCase().contains("password") || sp.first.toLowerCase().contains("passwd"))
		 && !sp.first.equalsIgnoreCase("passwordCheckerClass")
		 && sp.second != null && sp.second.length() > 0)
			return "****";
		PropertySpec ps = propHash.get(sp.first.toUpperCase());
		if (ps != null && ps.getType().equals(PropertySpec.COLOR))
		{
			log.trace("Returning a Color object.");
			if (sp.second.toLowerCase().startsWith("0x"))
			{
				return new Color(Integer.parseInt(sp.second.substring(2), 16));
			}
			else
			{
				return sp.second;
			}
		}
		else
		{
			return sp.second;
		}
	}
	
	/**
	 * Return the current value of a given property, or null if it's not set.
	 * @param propName the name of the property to retrieve.
	 * @return the current value of a given property, or null if it's not set.
	 */
	public String getCurrentPropValue(String propName)
	{
		for(StringPair sp : props)
			if (sp.first.equalsIgnoreCase(propName))
				return sp.second;
		return null;
	}

	/** Adds a new property to the model. */
	public void add(StringPair prop)
	{
		if (TextUtil.isAllWhitespace(prop.first))
		{
			TopFrame.instance().showError(genericLabels.getString("PropertiesEditDialog.blankErr"));
			return;
		}

		props.add(prop);
		fireTableDataChanged();
		changed = true;
	}

	/** Returns selected row-property as a StringPair object. */
	public StringPair propAt(int row)
	{
		if (row >= getRowCount())
			return null;
		return props.get(row);
	}

	/** Sets selected row-property from a StringPair object. */
	public void setPropAt(int row, StringPair prop)
	{
		if (row >= getRowCount())
			return;
		if (TextUtil.isAllWhitespace(prop.first))
		{
			TopFrame.instance().showError(genericLabels.getString("PropertiesEditDialog.blankErr"));
			return;
		}
		props.set(row, prop);
		fireTableDataChanged();
		changed = true;
	}

	void addEmptyProps(String[] propnames)
	{
		int numAdded = 0;
		for (String nm : propnames)
		{
			int row = 0;
			for (; row < getRowCount(); row++)
			{
				StringPair sp = propAt(row);
				if (nm.equalsIgnoreCase(sp.first))
					break;
			}
			if (row >= getRowCount()) // fell through - not found.
			{
				add(new StringPair(nm, ""));
				numAdded++;
			}
		}
		if (numAdded > 0)
		{
			fireTableDataChanged();
			changed = true;
		}
	}

	/** Deletes specified row */
	public void deletePropAt(int row)
	{
		if (row >= getRowCount())
			return;
		props.remove(row);
		fireTableDataChanged();
		changed = true;
	}

	/** Returns true if anything has been changed. */
	public boolean hasChanged()
	{
		return changed;
	}

	/** Saves changes from the model back to the java.util.Properties set. */
	public void saveChanges()
	{
		origProps.clear();
		for (int i = 0; i < props.size(); i++)
		{
			StringPair sp = props.get(i);
			if (sp.second != null && sp.second.trim().length() > 0)
				origProps.setProperty(sp.first, sp.second);
		}
		changed = false;
	}

	/**
	 * Used by the RetProcAdvancedPanel class to repaint the table when the new
	 * button is pressed.
	 */
	public void redrawTable()
	{
		saveChanges();
		fireTableDataChanged();
	}

	@Override
	public void sortByColumn(int column)
	{
		if (column == 0)
			Collections.sort(props,
				new Comparator<StringPair>()
				{
					@Override
					public int compare(StringPair o1, StringPair o2)
					{
						return o1.first.compareTo(o2.first);
					}
				});
		else
			Collections.sort(props,
				new Comparator<StringPair>()
				{
					@Override
					public int compare(StringPair o1, StringPair o2)
					{
						String s1 = o1.second;
						String s2 = o2.second;
						if (s1 == null || s1.trim().length() == 0)
						{
							if (s2 == null || s2.trim().length() == 0)
								return o1.first.compareToIgnoreCase(o2.first);
							else // sort non-empty strings to the front
								return 1;
						}
						else // s1 is not empty
						{
							if (s2 == null || s2.trim().length() == 0)
								return -1;
							int r = s1.compareToIgnoreCase(s2);
							if (r != 0)
								return r;
							return o1.first.compareToIgnoreCase(o2.first);
						}
					}
				});
		fireTableDataChanged();
	}

	@Override
	public Object getRowObject(int row)
	{
		return props.get(row);
	}
}
