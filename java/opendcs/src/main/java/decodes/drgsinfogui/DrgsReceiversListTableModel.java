package decodes.drgsinfogui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import decodes.gui.SortingListTableModel;

/**
 * Table Model to display the DRGS Receiver XMl file in a SWING table.
 *
 */
class DrgsReceiversListTableModel extends AbstractTableModel implements
		SortingListTableModel
{
	private String module;
	private static String[] columnNames;
	private DrgsReceiversListFrame parent;
	private Vector<DrgsReceiverIdent> vec;
	private int sortColumn = -1;
	private ArrayList<DrgsReceiverIdent> drgsList;

	/**
	 * Constructor. Initialize DRGS Receiver List
	 * @param parent
	 */
	public DrgsReceiversListTableModel(DrgsReceiversListFrame parent)
	{
		super();
		this.parent = parent;
		module = parent.module + ":" + "DrgsReceiversTableModel";
		columnNames = new String[6];
		columnNames[0] = parent.codeColumn;
		columnNames[1] = parent.locationColumn;
		columnNames[2] = parent.emailColumn;
		columnNames[3] = parent.descriptionColumn;
		columnNames[4] = parent.contactColumn;
		columnNames[5] = parent.phoneColumn;
		getDrgsReceiverList();
	}

	private void getDrgsReceiverList()
	{
		vec = null;
		//Read all DRGS Receiver Identification records from the XML file. 
		drgsList = DrgsReceiverIo.readDrgsReceiverInfo();
		vec = new Vector<DrgsReceiverIdent>(drgsList);
		sortByColumn(0);
	}

	/**Return the List so that we can save it in the XML file */
	public Vector<DrgsReceiverIdent> getDRGSList()
	{
		return vec;
	}
	
	/** Delete a Drgs record from list*/
	public void deleteDrgsReceiverAt(int index)
	{
		DrgsReceiverIdent dr = (DrgsReceiverIdent) vec.elementAt(index);
		deleteDrgsReceiver(dr);
	}

	private void deleteDrgsReceiver(DrgsReceiverIdent drObj)
	{
		int drIndex = vec.indexOf(drObj);
		if (drIndex != -1)
		{
			vec.remove(drIndex);
		}
		fireTableDataChanged();
	}
	
	/**
	 * Mofify a DRGS record in the list.
	 * 
	 * @param drOld
	 * @param drNew
	 */
	public void modifyDRGSList(DrgsReceiverIdent drOld,
			 									DrgsReceiverIdent drNew)
	{
		int drIndex = vec.indexOf(drOld);
		if (drIndex != -1)
		{
			vec.set(drIndex, drNew);
		}
		else
		{
			vec.add(drNew);
		}
		fireTableDataChanged();
	}

	/**
	 * Verify if the given code exists or not in the DRGS list.
	 * 
	 * @param code
	 * @return true if given code exists, false otherwise
	 */
	public boolean drgsCodeExits(String code)
	{
		boolean result = false;
		for (DrgsReceiverIdent dr : vec)
		{
			if (dr.getCode().equalsIgnoreCase(code))
			{
				result = true;
				break;
			}
		}
		return result;
	}
	
	/**
	 * Return Drgs record at specific position.
	 * 
	 * @param r
	 * @return
	 */
	public DrgsReceiverIdent getDrgsReceiverIdentAt(int r)
	{
		return (DrgsReceiverIdent) getRowObject(r);
	}

	public int getColumnCount()
	{
		return columnNames.length;
	}

	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	public boolean isCellEditable(int r, int c)
	{
		return false;
	}

	public int getRowCount()
	{
		return vec.size();
	}

	public Object getValueAt(int r, int c)
	{
		return DrgsReceiversColumnizer.getColumn(getDrgsReceiverIdentAt(r), c);
	}

	public Object getRowObject(int r)
	{
		return vec.elementAt(r);
	}

	public void sortByColumn(int c)
	{
		sortColumn = c;
		Collections.sort(vec, new DrgsReceiversColumnComparator(c));
	}

	public void reSort()
	{
		if (sortColumn >= 0)
			sortByColumn(sortColumn);
	}
}

/**
 * Helper class to retrieve Drgs fields by column number. Used for displaying
 * values in the table and for sorting.
 */
class DrgsReceiversColumnizer
{
	static String getColumn(DrgsReceiverIdent drgs, int c)
	{
		switch (c)
		{
		case 0: // Code
		{
			if (drgs != null)
				return drgs.getCode();
			else
				return "";
		}
		case 1: // Location
			if (drgs != null)
				return drgs.getLocation();
			else
				return "";
		case 2: // E-mail
			if (drgs != null)
				return drgs.getEmailAddr();
			else
				return "";
		case 3: // Description
			if (drgs != null)
				return drgs.getDescription();
			else
				return "";
		case 4: // Contact
			if (drgs != null)
				return drgs.getContact();
			else
				return "";
		case 5: // Phone #
			if (drgs != null)
				return drgs.getPhoneNum();
			else
				return "";
		default:
			return "";
		}
	}
}

class DrgsReceiversColumnComparator implements Comparator
{
	int col;

	DrgsReceiversColumnComparator(int col)
	{
		this.col = col;
	}

	public int compare(Object drgsIdent1, Object drgsIdent2)
	{
		if (drgsIdent1 == drgsIdent2)
			return 0;
		DrgsReceiverIdent d1 = (DrgsReceiverIdent) drgsIdent1;
		DrgsReceiverIdent d2 = (DrgsReceiverIdent) drgsIdent2;
		return DrgsReceiversColumnizer.getColumn(d1, col).compareToIgnoreCase(
				DrgsReceiversColumnizer.getColumn(d2, col));
	}
}