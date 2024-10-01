/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:01  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2004/09/20 14:18:49  mjmaloney
*  Javadocs
*
*  Revision 1.3  2001/11/19 01:58:21  mike
*  dev
*
*  Revision 1.2  2001/11/15 01:55:04  mike
*  Implemented RoutingSpec Edit Screens
*
*/
package decodes.dbeditor;

import javax.swing.JComboBox;
import java.util.Iterator;

import decodes.db.Database;
import decodes.db.PresentationGroup;

/**
Combo Box for selecting a presentation group.
*/
@SuppressWarnings("serial")
public class PresentationGroupCombo 
	extends JComboBox
{
	static final String none = "(none)";

	/** Constructor. */
    public PresentationGroupCombo()
	{
		super();
		addItem(none);

		for(Iterator<PresentationGroup> it = Database.getDb().presentationGroupList.iterator();
			it.hasNext(); )
		{
			PresentationGroup pg = it.next();
			addItem(pg.groupName);
		}
    }
    
    /**
     * Used from PresentationGroup Edit Panel to exclude the group being
     * edited from the selection for "inherits from". (A group can't inherit
     * from itself).
     * @param name the name of the presentation group to exclude
     */
    public void exclude(String name)
    {
    	this.removeItem(name);
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
				break;
		    }
		}
	}

	/**
	  @return the current selection.
	*/
	public PresentationGroup getSelection()
	{
		String nm = (String)getSelectedItem();
		if (nm == null || nm == none)
			return null;
		return Database.getDb().presentationGroupList.find(nm);
	}
}
