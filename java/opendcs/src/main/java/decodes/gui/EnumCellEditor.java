package decodes.gui;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import java.util.Iterator;

import decodes.db.EnumValue;
import decodes.db.Database;
import decodes.db.DbEnum;

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
	public EnumCellEditor(DbEnum dbEnum)
	{
		super(new JComboBox<EnumValue>());
		
		if (dbEnum == null)
			return;

		// populate JCombo with enum values
		@SuppressWarnings("unchecked") // we just set this up above
		var jcb = (JComboBox<EnumValue>)this.getComponent();
		var it = dbEnum.iterator();
		while(it.hasNext())
		{
			var ev = it.next();
			jcb.addItem(ev);
		}
	}

	/** Called when cursor moved to different cell. */
	public boolean stopCellEditing()
	{
		fireEditingStopped();
		return true;
	}
}
