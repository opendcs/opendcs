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
package decodes.gui;

import java.util.Iterator;
import java.util.Collections;
import java.util.Vector;
import javax.swing.JComboBox;

import ilex.util.TextUtil;

import decodes.db.EngineeringUnit;
import decodes.db.Database;


/**
UnitsComboBox allows you to display a pull-down list of engineering units
that measure a given physical quantity (e.g. length, volume)
*/
public class UnitsComboBox extends JComboBox
{
	Vector values;

	/**
	  Constructs a new UnitsComboBox containing all of the EU's defined in
	  the database that measure the given physical quantity.
	  @param measures the physical quantity to be displayed.
	  @param currentValue the current value to show in the combo.
	*/
	public UnitsComboBox(String measures, String currentValue)
	{
		values = new Vector();
		String defaultValue = null;
		for(Iterator it = Database.getDb().engineeringUnitList.iterator();
			it.hasNext(); )
		{
			EngineeringUnit eu = (EngineeringUnit)it.next();
			if (TextUtil.strEqualIgnoreCase(measures, eu.measures))
			{
				String s = eu.abbr + " (" + eu.getName() + ")";
				if (TextUtil.strEqualIgnoreCase(eu.abbr, currentValue))
					defaultValue = s;
				values.add(s);
			}
		}
		Collections.sort(values);
		for(Iterator it = values.iterator(); it.hasNext(); )
			addItem(it.next());

		if (defaultValue != null)
			setSelectedItem(defaultValue);
		setEditable(true);
	}

	/**
	  Sets the selected EU abbreviation.
	  @param abbr the EU abbreviation.
	*/
	public void setSelectedAbbr(String abbr)
	{
		int n = getItemCount();
		for(int i = 0; i < n; i++)
		{
			String t = disp2abbr((String)getItemAt(i));
			if (abbr.equalsIgnoreCase(t))
			{
				setSelectedIndex(i);
				return;
			}
		}
		setSelectedItem(abbr);
	}

	private String disp2abbr(String disp)
	{
		int i = disp.indexOf(" (");
		if (i == -1)
			return disp;
		return disp.substring(0, i);
	}

	/** @return the selected EU abbreviation. */
	public String getSelectedAbbr()
	{
		return disp2abbr((String)getSelectedItem());
	}
}
