/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2008/01/03 21:16:40  mmaloney
*  internationalization
*
*  Revision 1.2  2004/09/20 14:18:43  mjmaloney
*  Javadocs
*
*  Revision 1.1  2003/11/20 00:59:04  mjmaloney
*  Add DataOrder to Decoding Script Dialog.
*  Add elevation, elev-units, and description to site edit panel.
*
*/
package decodes.dbeditor;

import java.awt.*;
import javax.swing.JComboBox;
import java.util.ResourceBundle;

import ilex.util.LoadResourceBundle;
import decodes.db.Constants;

/**
This class is a combo-box for data order choices. It allows the owner to
get/set the data order by the single character codes defined in db.Constants.
*/
public class DataOrderCombo extends JComboBox
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static String orders[];
	static
	{
	/** First character of these values MUST agree with dataOrder constants: */
		orders = new String[]{ 
			genericLabels.getString("Ascending"),
			genericLabels.getString("Descending"),
			"Undefined"
		};
	}

	/** Constructor */
    public DataOrderCombo()
	{
		super(orders);
        try 
		{
            jbInit();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

	/** Initialize GUI components. */
    private void jbInit() throws Exception {
    }

	/**
	  Sets the current selection.
	  @param dataOrder the current selection.
	*/
	public void setSelection(char dataOrder)
	{
		if (Character.toUpperCase(dataOrder) == Constants.dataOrderDescending)
			this.setSelectedItem(orders[1]);
		else if (Character.toUpperCase(dataOrder) == Constants.dataOrderAscending)
			this.setSelectedItem(orders[0]);
		else
			this.setSelectedItem(orders[2]);
	}

	/**
	  @return the current selection.
	*/
	public char getSelection()
	{
		String s = (String)this.getSelectedItem();
		return s.charAt(0);
	}
}
