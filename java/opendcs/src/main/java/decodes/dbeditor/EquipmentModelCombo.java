/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2008/01/07 16:16:05  mmaloney
*  internationalization
*
*  Revision 1.3  2004/09/20 14:18:46  mjmaloney
*  Javadocs
*
*  Revision 1.2  2002/03/31 21:09:38  mike
*  bug fixes
*
*  Revision 1.1  2001/05/03 14:15:55  mike
*  dev
*
*/
package decodes.dbeditor;

import java.util.ResourceBundle;

import javax.swing.JComboBox;
import java.util.Iterator;
import decodes.db.*;

/**
Combo box for selecting an EquipmentModel.
*/
public class EquipmentModelCombo extends JComboBox
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();

	static final String none = genericLabels.getString("noneComboItem");

	/** Constructor. */
    public EquipmentModelCombo()
	{
		super();
		addItem(none);
		Iterator it = Database.getDb().equipmentModelList.iterator();
		while(it.hasNext())
		{
			EquipmentModel em = (EquipmentModel)it.next();
			addItem(em.name);
		}
    }

	/**
	  Sets the current selection from the name of an Equipment Model.
	  @param v the current selection.
	*/
	public void set(String v)
	{
		if (v == null)
		{
			setSelectedIndex(0);
			return;
		}
		int n = getItemCount();
		for(int i=0; i<n; i++)
		{
			String s = (String)getItemAt(i);
			if (s.equalsIgnoreCase(v))
				setSelectedIndex(i);
		}
	}

	/**
	  Sets the current selection from an Equipment Model object.
	  If null, set selection to the first (empty) slot in the list.
	  @param em the current selection.
	*/
	public void set(EquipmentModel em)
	{
		if (em == null)
			setSelectedIndex(0);
		else
			setSelectedItem(em.name);
	}

	/**
	  @return the current selection.
	*/
	public EquipmentModel getSelection()
	{
		String name = (String)getSelectedItem();
		if (name == none)
			return null;
		return Database.getDb().equipmentModelList.get(name);
	}
}
