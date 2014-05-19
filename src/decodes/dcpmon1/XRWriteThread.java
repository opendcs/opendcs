/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package decodes.dcpmon1;

import ilex.util.Logger;

import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import lrgs.common.DcpAddress;

/**
This class is responsible for writing XmitRecords to the SQL database.
It maintains a queue. XmitRecords are only written after they've lived
in the queue for agesec seconds. The reason is that we may get several
DCP 'Messages' that effect the same XmitRecord (DAPS Status Messages,
duplicates, etc.). These separate messages will likely arrive withing
a short time frame. Hence the XmitRecord has a chance to 'settle' before
it is written to the database. This reduces the number of database writes.
*/
public class XRWriteThread extends Thread
{
	/** The age in seconds that entries must be to be archived to database */
	private int agesec = 5;
	private static int QueueMax = 1000;

	private static final String module = "XRWriteThread";
	public static long numQueued = 0L;
	public static long lastMsgMsec = 0L;

	/** This wraps XmitRecord and adds an insert time. */
	class XRWrapper
	{
		XmitRecord xr;
		long insertTime;

		XRWrapper(XmitRecord xr)
		{
			this.xr = xr;
			this.insertTime = System.currentTimeMillis();
		}
	}
	/** The queue */
	private ConcurrentLinkedQueue<XRWrapper> q;

	/** shutdown flag */
	private boolean _shutdown;

	/** The singleton instance */
	private static XRWriteThread _instance = null;

	/** The singleton access method */
	public static XRWriteThread instance()
	{
		if (_instance == null)
			_instance = new XRWriteThread();
		return _instance;
	}

	/** Private Constructor */
	private XRWriteThread()
	{
		q = new ConcurrentLinkedQueue<XRWrapper>();
		_shutdown = false;
	}

	/** Called when application has been shutdown to flush the queue. */
	public void shutdown()
	{
		_shutdown = true;
	}
	
int callNum = 0;
	/**
	 * Add an xmit record to the queue. First search the queue to see if
	 * it is already present. If it is do nothing.
	 * @return true if enqueued, false if queue is full.
	 */
	public synchronized boolean enqueue(XmitRecord xr)
	{
		int sz = q.size();
		if (sz > QueueMax)
		{
			Logger.instance().warning("Cannot enqueue: queue "
				+ "size is too big (" + sz + ")");
			return false;
		}

		for(XRWrapper xrw : q)
			if (xrw.xr == xr)
				return true; // already in queue

		Logger.instance().debug3("Enqueue: Before adding XR into Q; Q size = " + q.size());
		q.add(new XRWrapper(xr));
		Logger.instance().debug3("Enqueue: After adding XR into Q; Q size = " + q.size());
		
		if ((++callNum % 100) == 0)
		  Logger.instance().debug1("Enqueue: Q size = " + (sz = q.size()));
		
		numQueued++;
		lastMsgMsec = xr.getGoesTimeMsec();
		
		return true;
	}

int dCallNum = 0;
	/**
	 * If there is an XmitRecord on the queue for longer than 'agesec' seconds,
	 * or if we are shutting down, return it.
	 * @return XmitRecord or null if none ready to write.
	 */
	public synchronized XmitRecord dequeue()
	{
		XRWrapper xrw = q.peek();
		if (xrw == null)
			return null;
		int sz = q.size();
		if (sz >= (QueueMax/2)
		 || _shutdown
		 || System.currentTimeMillis() - xrw.insertTime >= (agesec*1000L))
		{
			Logger.instance().debug3("Dequeue: Before polling XR from Q; Q size = " + q.size());
			q.poll();
			Logger.instance().debug3("Dequeue: After polling XR from Q; Q size = " + q.size());

			if ((++dCallNum % 100) == 0)
				Logger.instance().debug1("Dequeue: Q size = " + sz);
			return xrw.xr;
		}
		else
			return null;
	}

	/**
	 * Search the queue for a message with matching address, day
	 * and second of day. Second of day may be off by as much as 2 min.
	 * @param dcpAddr the dcp address
	 * @param tsms the time stamp as a millisecond value
	 * @return the XmitRecord if one exists, or null if not.
	 */
	public synchronized XmitRecord find(DcpAddress dcpAddress, long tsms)
	{
		for(Iterator<XRWrapper> it = q.iterator(); it.hasNext(); )
		{
			XRWrapper xrw = it.next();
			if (xrw.xr.getDcpAddress().equals(dcpAddress))
			{
				long tdiff = xrw.xr.getGoesTimeMsec() - tsms;
				if (tdiff > -120000 && tdiff < 120000)
					return xrw.xr;
			}
		}
		return null;
	}

	/** The Thread run method. */
	public void run()
	{
		Logger.instance().info("XRWriteThread started.");
		while(!_shutdown)
		{
			try { sleep(1000L); } catch(InterruptedException ex) {}
			processQueue();
		}
		Logger.instance().info("XRWriteThread exiting.");
	}

	public synchronized void processQueue()
	{
		XmitRecord xr;
		long lastMsec = 0L;
		int n = 0;

		Logger.instance().debug3("Dequeue: Dequeue XR started; Q size = " + q.size());
		while((xr = dequeue()) != null)
		{
			lastMsec = xr.getGoesTimeMsec();
			decodes.dcpmon1.DcpMonitor.instance().saveDcpTranmission(xr);
			if (++n > 50)
				doCommit();
		}
		doCommit();
		Logger.instance().debug3("Dequeue: Dequeue XR ended; Q size = " + q.size());

		Logger.instance().debug1("After writing " + n 
			+ ", queue size=" + q.size()
			+ (lastMsec == 0L ? "" :
				(", last time stamp = " + new Date(lastMsec))));
	}
	private void doCommit()
	{
		try { decodes.dcpmon1.DcpMonitor.instance().theDb.commit(); }
		catch(decodes.tsdb.DbIoException ex)
		{
			Logger.instance().warning("Error committing: " + ex);
		}
	}
}
