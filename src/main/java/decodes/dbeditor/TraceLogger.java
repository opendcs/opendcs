
package decodes.dbeditor;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import ilex.util.Logger;

/**
TraceLogger captures and displays  'trace' log messages in a dialog, during interactive operation like decoding.
*/
public class TraceLogger extends Logger
{
	/** The trace dialog. */
	TraceDialog dlg;

	private BlockingQueue<String> queue = new ArrayBlockingQueue<>(5000);
	private Thread writerThread;
	private AtomicBoolean closeOperations = new AtomicBoolean(false);

	/**
	 Constructor.
	*/
	public TraceLogger(String procName)
	{
		super(procName);
		this.dlg = null;
		writerThread = new Thread(() ->
		{
			while (closeOperations.get() == false)
			{
				try
				{
					String line = queue.poll(1, TimeUnit.SECONDS);
					if (dlg != null)
					{
						dlg.addText(line);
					}
				}
				catch (InterruptedException ex)
				{
					// go back to operation, in this scenario most likely closeOperations is true and
					// and we're ditching the logging
				}
			}
		},
		"TraceDialog-Writer");
		writerThread.setDaemon(true);
		writerThread.start();
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
		closeOperations.set(true);
	}

	public synchronized void doLog(int priority, String text)
	{
		try
		{
			queue.put(text + "\n");
		}
		catch (InterruptedException ex)
		{
			// NOTE: use of STDERR is intentional here. This is an error with logging itself.
			System.err.println("Unable to Write message to log queue.");
			ex.printStackTrace();
		}
	}
}
