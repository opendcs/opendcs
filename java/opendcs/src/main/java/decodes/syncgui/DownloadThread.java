/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.syncgui;

import java.io.InputStream;
import javax.swing.SwingUtilities;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.LinkedList;
import java.net.URL;
import ilex.util.Pair;

/**
Reads files from the hub in the background in a separate thread.
*/
public class DownloadThread extends Thread
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
				log.atError().setCause(ex).log("failed to read.");
				showLeftStatus("Error: " + ex);
				dr.readFailed(relurl, ex);
			}
		}
	}

	private void showLeftStatus(final String msg)
	{
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
