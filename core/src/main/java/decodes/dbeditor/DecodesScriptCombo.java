/*
*  $Id$
*/
package decodes.dbeditor;

import javax.swing.JComboBox;
import java.util.Iterator;
import java.util.ResourceBundle;

import ilex.util.LoadResourceBundle;
import decodes.db.*;

/**
Combo box for selecting a DECODES script.
*/
public class DecodesScriptCombo extends JComboBox
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();

	static final String none = "(" + genericLabels.getString("none") + ")";
	PlatformConfig myPC;

	/** 
	  Constructor. 
	  @param pc the PlatformConfig containing the scripts to include in the 
	  list.
	*/
    public DecodesScriptCombo(PlatformConfig pc, TransportMedium tm)
    {
		super();
		addItem(none);
		if (pc == null)
			return;
		myPC = pc;

		for(Iterator it = myPC.getScripts(); it.hasNext();)
		{
			DecodesScript ds = (DecodesScript)it.next();
			addItem(ds.scriptName);
		}


		if (pc.getNumScripts() == 1 && tm != null)
			tm.scriptName = (String)getItemAt(1); 
    }

	/**
	  Sets the current selection.
	  @param name the current selection.
	*/
	public void set(String name)
	{
		if (name == null)
		{
			setSelectedIndex(0);
			return;
		}
		int n = getItemCount();
		for(int i=0; i<n; i++)
		{
			String s = (String)getItemAt(i);
			if (s.equalsIgnoreCase(name))
			{
				this.setSelectedIndex(i);
				return;
			}
		}
	}

	/**
	  @return the current selection.
	*/
	public String getSelection()
	{
		String name = (String)getSelectedItem();
		if (name == none)
			return null;
		return name;
 	}
}
