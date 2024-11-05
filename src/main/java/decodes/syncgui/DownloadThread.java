


package decodes.syncgui;

import java.io.IOException;
import java.io.InputStream;
import javax.swing.SwingUtilities;
import java.util.LinkedList;
import java.net.URL;
import ilex.util.Pair;
import ilex.util.Logger;

/**
Reads files from the hub in the background in a separate thread.
*/
public class DownloadThread extends Thread
{
	LinkedList queue;
	boolean _shutdown;

	public DownloadThread()
	{
		queue = new LinkedList();
		_shutdown = false;
	}

	/**
	  Add a file to the queue to be read.
	  @param relpath the path relative to HUB_HOME URL.
	  @param listener to call back when download is complete or error.
	  @param obj Object to callback the listener with
	*/
	public synchronized void enqueue(String relpath, DownloadReader reader)
	{
		queue.addLast( new Pair(relpath, reader) );
	}

	/** Clears the queue */
	public synchronized void clear()
	{
		queue.clear();
	}

	/** 
	  Gets next element from the queue (called internally only).
	  @return next Qelem, or null if queue empty.
	*/
	Pair dequeue()
	{
		if (queue.size() == 0)
			return null;
		return (Pair)queue.removeFirst();
	}

	/** Tells the thread to shut down. */
	public void shutdown() { _shutdown = true; }

	/** Thread run method */
	public void run()
	{
		while(!_shutdown)
		{
			Pair qe = dequeue();
			if (qe == null)
			{
				try { sleep(100L); } catch(InterruptedException ex) {}
				continue;
			}
			String relurl = (String)qe.first;
			DownloadReader dr = (DownloadReader)qe.second;
			try
			{
				String msg = "Reading " + relurl;
				showLeftStatus(msg);
				String hh = SyncConfig.instance().getHubHome();
				String urlstr = hh + "/" + relurl;
				URL url = new URL(urlstr);
				InputStream strm = url.openStream();
				dr.readFile(relurl, strm);
				strm.close();
				showLeftStatus("Complete");
			}
			catch(Exception ex)
			{
				showLeftStatus("Error: " + ex);
				dr.readFailed(relurl, ex);
			}
		}
	}

	private void showLeftStatus(final String msg)
	{
		Logger.instance().log(
			msg.startsWith("Err")?Logger.E_WARNING:Logger.E_INFORMATION,msg);

		SwingUtilities.invokeLater(
			new Runnable()
			{
				public void run()
				{
					SyncGuiFrame.instance().showLeftStatus(msg);
				}
			});
	}
}
