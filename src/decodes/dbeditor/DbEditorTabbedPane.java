/*
*  $Id$
*/

package decodes.dbeditor;

import java.awt.Component;
import javax.swing.JTabbedPane;
import decodes.db.IdDatabaseObject;

/**
Abstract class extended by all top-level tabbed panes in the database
editor.
*/
public class DbEditorTabbedPane extends JTabbedPane 
{

	/** Constructor. */
    public DbEditorTabbedPane()
	{
    }

	/**
	 * Searches the tabbed pane for an editor panel for the passed object.
	 * @return If found, the DbEditorTab. If not, null.
	 */
	public DbEditorTab findEditorFor(IdDatabaseObject testobj)
	{
		String testdn = testobj.getDisplayName();
		int n = getTabCount();
		for(int i = 0; i < n; i++)
		{
			Component cmp = getComponentAt(i);
			if (cmp != null && cmp instanceof DbEditorTab)
			{
				DbEditorTab tab = (DbEditorTab)cmp;
				IdDatabaseObject itobj = tab.getTopObject();
				if (testobj == itobj
				 || (testdn.length() > 0 
					&& testdn.equals(itobj.getDisplayName())))
					return tab;
			}
		}
		return null;
	}

	/**
	  Called before the application closes. Finds the first open editor where
	  the user has made a change.
	  @return the DbEditorTab containing the open editor panel.
	*/
	public DbEditorTab findFirstOpenEditor()
	{
		int n = getTabCount();
		for(int i = 0; i < n; i++)
		{
			Component cmp = getComponentAt(i);
			if (cmp != null 
			 && cmp instanceof DbEditorTab
			 && cmp instanceof ChangeTracker)
			{
				ChangeTracker ct = (ChangeTracker)cmp;
				if (ct.hasChanged())
					return (DbEditorTab)cmp;
			}
		}
		return null;
	}

	/**
	 * Called from the File - CloseAll menu item to Close all open tabs,
	 * abandoning any changes that have been made.
	 */
	public void closeAll()
	{
		for(int i = 0; i < getTabCount(); )
		{
			Component cmp = getComponentAt(i);
			if (cmp != null && cmp instanceof DbEditorTab)
			{
				DbEditorTab tab = (DbEditorTab)cmp;
				tab.forceClose();
			}
			else
				i++;
		}
	}

	/**
	 * Called from the File - SaveAll menu item to Commit all open tabs.
	 */
	public void saveAll()
	{
		int n = getTabCount();
		for(int i = 0; i < n; i++)
		{
			Component cmp = getComponentAt(i);
			if (cmp != null 
			 && cmp instanceof DbEditorTab
			 && cmp instanceof EntityOpsController)
			{
				EntityOpsController tab = (EntityOpsController)cmp;
				tab.commitEntity();
			}
		}
	}
}
