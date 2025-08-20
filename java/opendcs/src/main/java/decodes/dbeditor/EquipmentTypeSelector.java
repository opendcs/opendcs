/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/09/20 14:18:46  mjmaloney
*  Javadocs
*
*  Revision 1.2  2001/11/01 01:02:51  mike
*  Equipment list & edit panels.
*
*/

package decodes.dbeditor;

import java.awt.*;
import javax.swing.JComboBox;

import decodes.db.Constants;

/**
Combo box for selecting from constant list of Equipement Types.
*/
public class EquipmentTypeSelector extends JComboBox
{
	static String equipTypes[] = {
		Constants.eqType_dcp, Constants.eqType_sensor,
		Constants.eqType_transport };

	/** Constructor. */
    public EquipmentTypeSelector()
	{
		super(equipTypes);
        try {
            jbInit();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

	/**
	  Sets the current selection.
	  @param et the selection.
	*/
	public void setSelection(String et)
	{
		for(int i=0; i<equipTypes.length; i++)
			if (et.equalsIgnoreCase(equipTypes[i]))
			{
				this.setSelectedIndex(i);
				return;
		    }
	}

	/** @return the current selection. */
	public String getSelection()
	{
		return (String)this.getSelectedItem();
	}

	/** GUI component initialization. */
    private void jbInit() throws Exception
	{
    }
}
