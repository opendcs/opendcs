package decodes.dupdcpgui;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;

/**
Used within a JTable for having a combo-box in a table cell.
The choices are presented to the user when the cell is clicked.
*/
public class JEnumCellEditor extends DefaultCellEditor
{
	/**
	  Constructor.
	  param choices[] 
	*/
	public JEnumCellEditor(String[] choices)
	{
		super(new JComboBox());

		if (choices == null)
			return;

		// populate JCombo with choices values
		JComboBox jcb = (JComboBox)this.getComponent();
		int len = choices.length;
		for (int x=0;x<len;x++)
		{
			jcb.addItem(choices[x]);
		}
	}

	/** Called when cursor moved to different cell. */
	public boolean stopCellEditing()
	{
		fireEditingStopped();
		return true;
	}
}
