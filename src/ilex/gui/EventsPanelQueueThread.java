package ilex.gui;

import ilex.util.QueueLogger;
import ilex.util.IndexRangeException;

/**
This class works with ilex.util.QueueLogger and ilex.gui.EventsPanel.
It is a thread that continually listens for new events on the queue
and adds them to the panel when they occur.
*/
public class EventsPanelQueueThread
	extends Thread
{
	/** The QueueLogger to read from */
	private QueueLogger qLogger;

	/** The EventsPanel to write to */
	private EventsPanel evtPanel;

	/** shutdown flag */
	private boolean _shutdown;

	/**
	 * Constructor is passed the QueueLogger to read and the EventsPanel
	 * to feed.
	 * @param ql The QueueLogger
	 * @param ep The EventsPanel
	 */
	public EventsPanelQueueThread(QueueLogger ql, EventsPanel ep)
	{
		qLogger = ql;
		evtPanel = ep;
		_shutdown = false;
	}

	/** the Thread run method */
	public void run()
	{
		try { sleep(1000L); } catch(InterruptedException ex) {}
		int idx = qLogger.getStartIdx();
		while(!_shutdown)
		{
			try
			{
				String msg = qLogger.getMsg(idx);
				if (msg == null)
					sleep(500L);
				else
				{
					evtPanel.addLine(msg);
					idx++;
				}
			}
			catch(InterruptedException ex) 
			{}
			catch(IndexRangeException ex)
			{
				idx = qLogger.getNextIdx();
			}
		}
	}

	/** Sets the shutdown flag, causing the thread to exit. */
	public void shutdown()
	{
		_shutdown = true;
	}
}
