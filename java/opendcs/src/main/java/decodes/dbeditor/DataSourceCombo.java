/*
*  $Id$
*/
package decodes.dbeditor;

import java.awt.*;
import javax.swing.JComboBox;
import java.util.Iterator;

import decodes.db.Database;
import decodes.db.DataSourceList;
import decodes.db.DataSource;

/**
 * This class displays a list of data sources as a combo box. The sources
 * are taken from the database.
 */
public class DataSourceCombo extends JComboBox
{
	/**
	  Constructor.
	  @param includeBlank call with true to allow a blank select at start of
	  list.
	*/
	public DataSourceCombo(boolean includeBlank)
	{
		super();
		if (includeBlank)
			addItem("");
		for(Iterator<DataSource> it = 
			Database.getDb().dataSourceList.iterator(); it.hasNext(); )
		{
			DataSource ds = it.next();
			addItem(ds.getName());
		}
	}

	/** No-args constructor, equal to this(false) */
    public DataSourceCombo()
	{
		this(false);
    }

	/**
	  Sets the current selection.
	  @param nm the current selection.
	*/
	public void setSelection(String nm)
	{
		for(int i=0; i<this.getItemCount(); i++)
		{
			String s = (String)this.getItemAt(i);
			if (s.equalsIgnoreCase(nm))
			{
				this.setSelectedIndex(i);
				return;
		    }
		}
	}

	/**
	  @return the current selection (DataSource object).
	*/
	public DataSource getSelection()
	{
		String nm = (String)getSelectedItem();
		if (nm == null)
			return null;
		return Database.getDb().dataSourceList.get(nm);
	}

	/**
	  @return the current selection (DataSource name).
	*/
	public String getSelectedName()
	{
		DataSource ds = getSelection();
		if (ds == null)
			return null;
		else
			return ds.getName();
	}

	/** 
	  Excludes the named data source from the combo. 
	  Used when some selections have already been made.
	  @param dsName the name to exclude.
	*/
	public void exclude(String dsName)
	{
		removeItem(dsName);
	}
}
