/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:01  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/09/20 14:18:49  mjmaloney
*  Javadocs
*
*  Revision 1.2  2001/10/20 14:41:08  mike
*  Work on Config Editor Panel
*
*/
package decodes.dbeditor;

import java.awt.*;
import javax.swing.JComboBox;

import decodes.db.Constants;

/**
Combo box for canned 'Recording Mode' selection (Fixed or Variable).
*/
public class RecordingModeCombo extends JComboBox
{
	public static final String modeFixed = "Fixed";
	public static final String modeVariable = "Variable";

	static String types[] = { modeFixed, modeVariable };

	/** Constructor. */
    public RecordingModeCombo()
	{
		super(types);
        try {
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
	  @param recordingMode the current selection.
	*/
	public void setSelection(char recordingMode)
	{
		if (Character.toLowerCase(recordingMode)
		 == Character.toLowerCase(Constants.recordingModeVariable))
			this.setSelectedItem(modeVariable);
		else
			this.setSelectedItem(modeFixed);
	}

	/**
	  @return the current selection.
	*/
	public char getSelection()
	{
		if (this.getSelectedItem() == modeVariable)
			return Constants.recordingModeVariable;
		else
			return Constants.recordingModeFixed;
	}
}
