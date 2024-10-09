/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:03  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/31 16:30:26  mjmaloney
*  javadocs
*
*  Revision 1.1  2003/11/20 18:33:41  mjmaloney
*  Created UnitsComboBox
*
*/
package decodes.gui;

import java.util.Iterator;
import java.util.Collections;
import java.util.Vector;
import javax.swing.JComboBox;

import ilex.util.Logger;
import ilex.util.TextUtil;

import decodes.db.EngineeringUnit;
import decodes.db.EngineeringUnitList;
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
