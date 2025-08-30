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

import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JComboBox;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.EnumValue;
import decodes.db.DbEnum;
import decodes.db.Database;

/**
Extends JComboBox to display the choice of values for a DECODES enumeration.
*/
@SuppressWarnings("serial")
public class EnumComboBox extends JComboBox<String>
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private String enumName;
	private DbEnum dbEnum;

	/**
	  Constructor used where user has already made a selection.
	  @param enumName the name of the enumeration.
	  @param currentValue the current setting for the combo box.
	  Call with currentValue=="" to insert a blank no-selection at the top.
	*/
	public EnumComboBox(String enumName, String currentValue)
	{
		this(enumName);
		if (currentValue == null || currentValue.length() == 0)
			this.insertItemAt("", 0);
		setSelection(currentValue);
	}

	/**
	  Constructor that uses default value.
	  @param enumName the name of the enumeration.
	*/
	public EnumComboBox(String enumName)
	{
		super();
		this.enumName = enumName;

		// find enum in current database
		dbEnum = Database.getDb().getDbEnum(enumName);
		if (dbEnum == null)
		{
			log.warn("EnumComboBox with invalid enumName '{}'", enumName);
			return;
		}

		// populate JCombo with enum values
		for(Iterator it = dbEnum.values().iterator(); it.hasNext(); )
		{
			EnumValue ev = (EnumValue)it.next();
			addItem(ev.getValue());
		}

		// Initially display 'default' value if one is specified.
		String def = dbEnum.getDefault();
		if (def != null)
			setSelection(def);
	}

	/**
	  Sets the current selection.
	  @param s the enum abbreviation selected.
	*/
	public void setSelection(String s)
	{
		if (s == null)
		{
			setSelectedIndex(0);
			return;
		}

		int n = getItemCount();
		for(int i = 0; i < n; i++)
		{
			String t = (String)getItemAt(i);
			if (s.equalsIgnoreCase(t))
			{
				setSelectedIndex(i);
				return;
			}
		}
	}

	/**
	  @return the current selection as a String.
	*/
	public String getSelection()
	{
		return (String)getSelectedItem();
	}

	public EnumValue getSelectedEnumValue()
	{
		String s = getSelection();
		if (s == null)
			return null;
		return dbEnum.findEnumValue(s);
	}

	/**
	 * This method returns all items in the combobox in Arraylist<String>.
	 * @return ArrayList<String> . Array list populated with values from combobox.
	 */
	public ArrayList<String> getAllItems()
	{
		ArrayList<String> allItems = new ArrayList<String>();
		for(int i=0;i<this.getItemCount();i++)
		{
			allItems.add((String)this.getItemAt(i));
		}

		return allItems;
	}
}
