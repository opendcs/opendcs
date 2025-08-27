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
import java.util.TimeZone;
import java.util.Arrays;

import org.opendcs.gui.GuiHelpers;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.TextUtil;

/**
Combo Box for selecting from the (many) Java Time Zones.
*/
@SuppressWarnings("serial")
public class TimeZoneSelector extends JComboBox<String>
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/** List of known java time zone IDs. */
	static String ids[] = TimeZone.getAvailableIDs();

	/** Constructor. */
    public TimeZoneSelector()
	{
			addItem(""); // no selection is valid.
			Arrays.sort(ids);
			for(int i=0; i<ids.length; i++)
			{
				addItem(ids[i]);
			}

			// Default is no TZ selected (blank)
			setSelectedIndex(0);
			setEditable(true);
    }

	/**
	  Sets the current selection.
	  @param tz the selection.
	*/
	public void setTZ(String tz)
	{
		if (tz == null || TextUtil.isAllWhitespace(tz))
			setSelectedIndex(0);
		else
		{
			if (tz.equalsIgnoreCase("Z"))
				tz = "UTC";

			for(int i=1; i<ids.length; i++)
				if (tz.equalsIgnoreCase(ids[i]))
				{
					setSelectedIndex(i+1);
					return;
				}
			addItem(tz);
			setSelectedItem(tz);
		}
	}

	/**
	 * @return currently selected timezone as a string.
	 */
	public String getTZ()
	{
		if (getSelectedIndex() == 0)
			return null;
		return (String)getSelectedItem();
	}
}
