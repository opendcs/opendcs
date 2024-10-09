package decodes.decwiz;

import decodes.gui.TopFrame;
import javax.swing.JPanel;
import javax.swing.JDialog;

public abstract class DecWizPanel
	extends JPanel
{
	/** Constructor. */
	public DecWizPanel()
	{
		super();
	}

	/**
	 * @return title to be displayed at the top of the frame when this panel
	 * is active.
	 */
	public abstract String getTitle();

	/**
	 * Called when this panel is activated.
	 */
	public abstract void activate();

	/**
	 * Called when this panel is de-activated.
	 * @return true of OK to move on, false if error (panel will not switch).
	 */
	public abstract boolean deactivate();

	/**
	 * Shows an error message, centered in the frame.
	 * @param msg the message to display.
	 */
	public void showError(String msg)
	{
		TopFrame.instance().showError(msg);
	}

	/**
	 * Launches a dialog, centered in the frame.
	 * @param msg the message to display.
	 */
	public void launchDialog(JDialog dlg)
	{
		TopFrame.instance().launchDialog(dlg);
	}

	public FileIdPanel getFileIdPanel()
	{
		return ((DecWizFrame)TopFrame.instance()).getFileIdPanel();
	}

	public DecodePanel getDecodePanel()
	{
		return ((DecWizFrame)TopFrame.instance()).getDecodePanel();
	}

	public SavePanel getSavePanel()
	{
		return ((DecWizFrame)TopFrame.instance()).getSavePanel();
	}
}
