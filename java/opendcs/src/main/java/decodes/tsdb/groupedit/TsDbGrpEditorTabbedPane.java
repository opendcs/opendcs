package decodes.tsdb.groupedit;

import java.awt.Component;

import javax.swing.JTabbedPane;

import decodes.dbeditor.ChangeTracker;

/**
 * Abstract class extended by all top-level tabbed panes in the time series
 * database editor.
 */
public class TsDbGrpEditorTabbedPane extends JTabbedPane
{
	/** Constructor. */
	public TsDbGrpEditorTabbedPane()
	{
	}

	/**
	 * Searches the tabbed pane for an editor panel for the passed tab name.
	 * 
	 * @return If found, the TsDbEditorTab. If not, null.
	 */
	public TsDbGrpEditorTab findEditorFor(String newTabName)
	{
		int n = getTabCount();
		for (int i = 0; i < n; i++)
		{
			Component cmp = getComponentAt(i);
			if (cmp != null && cmp instanceof TsDbGrpEditorTab)
			{
				TsDbGrpEditorTab tab = (TsDbGrpEditorTab) cmp;
				String openedTabName = tab.getTabName();
				if (newTabName.length() > 0 
						&& newTabName.equals(openedTabName))
					return tab;
			}
		}
		return null;
	}
	
	/**
	 * Called before the application closes. Finds the first open editor where
	 * the user has made a change.
	 * 
	 * @return the DbEditorTab containing the open editor panel.
	 */
	public TsDbGrpEditorTab findFirstOpenEditor()
	{
		int n = getTabCount();
		for (int i = 0; i < n; i++)
		{
			Component cmp = getComponentAt(i);
			if (cmp != null && cmp instanceof TsDbGrpEditorTab
					&& cmp instanceof ChangeTracker)
			{
				ChangeTracker ct = (ChangeTracker) cmp;
				if (ct.hasChanged())
					return (TsDbGrpEditorTab) cmp;
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
		for (int i = 0; i < getTabCount();)
		{
			Component cmp = getComponentAt(i);
			if (cmp != null && cmp instanceof TsDbGrpEditorTab)
			{
				TsDbGrpEditorTab tab = (TsDbGrpEditorTab) cmp;
				tab.forceClose();
			} else
				i++;
		}
	}

	/**
	 * Called from the File - SaveAll menu item to Commit all open tabs.
	 */
	public void saveAll()
	{
		int n = getTabCount();
		for (int i = 0; i < n; i++)
		{
			Component cmp = getComponentAt(i);
			if (cmp != null && cmp instanceof TsDbGrpEditorTab
					&& cmp instanceof TsEntityOpsController)
			{
				TsEntityOpsController tab = (TsEntityOpsController) cmp;
				tab.saveEntity();
			}
		}
	}
}
