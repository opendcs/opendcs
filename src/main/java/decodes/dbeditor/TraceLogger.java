
package decodes.dbeditor;

import ilex.util.Logger;

/**
Used to grabe 'trace' log messages when Decode button is pressed.
*/
public class TraceLogger extends Logger
{
	/** The trace dialog. */
	TraceDialog dlg;

	/**
	 Constructor.
	*/
	public TraceLogger(String procName)
	{
		super(procName);
		this.dlg = null;
	}

	/**
	 * Sets the dialog.
	 * @param dlg the TraceDialog.
	 */
	public synchronized void setDialog(TraceDialog dlg)
	{
		this.dlg = dlg;
	}

	/** Does nothing. */
	public void close()
	{
	}

	public synchronized void doLog(int priority, String text)
	{
		if (dlg != null)
			dlg.addText(text + "\n");
	}
}
