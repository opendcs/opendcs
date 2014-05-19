package decodes.gui;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import java.util.Iterator;

import decodes.db.EnumValue;
import decodes.db.Database;

/**
Used within a JTable for having a combo-box in a table cell.
The enum-value choices are presented to the user when the cell is clicked.
*/
public class EnumCellEditor extends DefaultCellEditor
{
	/**
	  Constructor.
	  param enumName the name of the DECODES enumeration to display.
	*/
	public EnumCellEditor(String enumName)
	{
		super(new JComboBox());

		// find enum in current database
		decodes.db.DbEnum en = Database.getDb().getDbEnum(enumName);
		if (en == null)
			return;

		// populate JCombo with enum values
		JComboBox jcb = (JComboBox)this.getComponent();
		for(Iterator it = en.values().iterator(); it.hasNext(); )
		{
			EnumValue ev = (EnumValue)it.next();
			jcb.addItem(ev.value);
		}
	}

	/** Called when cursor moved to different cell. */
	public boolean stopCellEditing()
	{
		fireEditingStopped();
		return true;
	}
}
