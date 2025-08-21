/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.dbeditor;

import java.awt.*;
import javax.swing.JComboBox;

import org.opendcs.gui.GuiHelpers;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.Iterator;

import decodes.db.Database;
import decodes.db.EnumValue;
import decodes.util.DecodesSettings;

/**
Combo box for selecting a data type standard.
Note - class name is a misnomer: This selects the 'standard' from the Enum
containing the known standards. The 'Code' is a type-in field.
*/
public class DataTypeCodeCombo extends JComboBox<String>
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
        catch (Exception ex)
		{
        	GuiHelpers.logGuiComponentInit(log, ex);
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
