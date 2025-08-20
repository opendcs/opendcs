package decodes.tsdb.groupedit;

import java.awt.Dimension;
import java.awt.Point;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import decodes.gui.TopFrame;

/**
Base class for editor tabs.
This is extended by TsDefinitionPanel.java, TsGroupDefinition.java,
TsAlarmSeverityPanel.java and TsAlarmActionPanel.java.
*/
public abstract class TsDbGrpEditorTab extends JPanel
{
	/** Holds the object that this tab is editing. */
	//IdDatabaseObject topObject;
	private String tabName = "";
	
	/** the JFrame */
	//JFrame parentFrame;

	/** Constructs new DbEditorTab */
    public TsDbGrpEditorTab()
	{
    }

	/**
	  Launches the passed modal dialog at a reasonable position on the screen.
	  @param dlg the dialog to launch.
	*/
	public void launchDialog(JDialog dlg)
	{
		dlg.validate();
		dlg.setLocationRelativeTo(TopFrame.instance());
		dlg.setVisible(true);
	}

	/**
	 * Close this editor tab, abandoning any changes made.
	 */
	public abstract void forceClose();

	public String getTabName()
	{
		return tabName;
	}

	public void setTabName(String tabName)
	{
		this.tabName = tabName;
	}
}
