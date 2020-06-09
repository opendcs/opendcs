package decodes.dbeditor;

import java.awt.*;
import javax.swing.JComboBox;
import java.util.Iterator;

import decodes.db.Database;
import decodes.db.EnumValue;
import decodes.util.DecodesSettings;

/**
Combo box for selecting a data type standard.
Note - class name is a misnomer: This selects the 'standard' from the Enum
containing the known standards. The 'Code' is a type-in field.
*/
public class DataTypeCodeCombo extends JComboBox
{
	/** Constructor. */
    public DataTypeCodeCombo()
	{
		String pref = DecodesSettings.instance().dataTypeStdPreference;
        try
		{
			decodes.db.DbEnum dte = Database.getDb().getDbEnum("DataTypeStandard");
			if (dte != null)
			{
				String prefv = null;
				for(Iterator it = dte.values().iterator(); it.hasNext(); )
				{
					EnumValue ev = (EnumValue)it.next();
					if (ev.getValue().equalsIgnoreCase(pref))
						prefv = ev.getValue();
					this.addItem(ev.getValue());
				}
				if (prefv != null)
					this.setSelectedItem(prefv);
			}

            jbInit();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

	/**
	  Sets the current selection.
	  @param std the current selection.
	*/
	public void setDataTypeStandard(String std)
	{
		int n = this.getItemCount();
		for(int i=0; i<n; i++)
		{
			String s = (String)this.getItemAt(i);
			if (s.equalsIgnoreCase(std))
			{
				this.setSelectedIndex(i);
				return;
			}
		}
	}

	/**
	  @return the current selection.
	*/
	public String getDataTypeStandard()
	{
		return (String)this.getSelectedItem();
	}

	/** Initialize GUI components. */
    private void jbInit() throws Exception {
    }
}
