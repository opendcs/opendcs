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

import javax.swing.JComboBox;

import org.opendcs.gui.GuiHelpers;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.Constants;

/**
Combo box for selecting from constant list of Equipement Types.
*/
public class EquipmentTypeSelector extends JComboBox
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	static String equipTypes[] = {
		Constants.eqType_dcp, Constants.eqType_sensor,
		Constants.eqType_transport };

	/** Constructor. */
    public EquipmentTypeSelector()
	{
		super(equipTypes);
        try 
		{
            jbInit();
        }
        catch(Exception ex) 
		{
            GuiHelpers.logGuiComponentInit(log, ex);
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
