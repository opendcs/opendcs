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

import java.util.ResourceBundle;

import decodes.db.Constants;

/**
This class is a combo-box for data order choices. It allows the owner to
get/set the data order by the single character codes defined in db.Constants.
*/
public class DataOrderCombo extends JComboBox<String>
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
        catch(Exception ex) 
		{
            GuiHelpers.logGuiComponentInit(log, ex);
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
