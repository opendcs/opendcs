package decodes.dbeditor;

import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JDialog;
import java.awt.Point;
import java.awt.Dimension;

import decodes.db.IdDatabaseObject;
import decodes.gui.TopFrame;
import decodes.platwiz.PlatformWizard;

/**
Base class for editor tabs.
This is extended by each of the 'xxxEditPanel.java' classes.
*/
public abstract class DbEditorTab extends JPanel
{
	/** Holds the object that this tab is editing. */
	IdDatabaseObject topObject;

	/** the JFrame */
	//JFrame parentFrame;

	/** Constructs new DbEditorTab */
    public DbEditorTab()
	{
    }

	/** 
	  Sets the object that this panel is editing. 
	  @param obj the object
	*/
	public void setTopObject(IdDatabaseObject obj)
	{
		topObject = obj;
	}

	/** @return the object that this panel is editing. */
	public IdDatabaseObject getTopObject()
	{
		return topObject;
	}

	/**
	  Launches the passed modal dialog at a reasonable position on the screen.
	  @param dlg the dialog to launch.
	*/
	public void launchDialog(JDialog dlg)
	{
		dlg.validate();
		dlg.setLocationRelativeTo(getParentFrame());
		dlg.setVisible(true);
	}
	
	protected JFrame getParentFrame()
	{
		JFrame jf = DbEditorFrame.instance();
		if (jf == null)
		{
			PlatformWizard pw = PlatformWizard.instance();
			if (pw != null && pw.getPlatwizFrame() != null)
				jf = pw.getPlatwizFrame();
			else
				jf = TopFrame.instance();
		}
		return jf;
	}

	/**
	 * Close this editor tab, abandoning any changes made.
	 */
	public abstract void forceClose();
}
