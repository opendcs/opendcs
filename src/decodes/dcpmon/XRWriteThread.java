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
package decodes.dcpmon;

import ilex.util.Logger;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import opendcs.dai.XmitRecordDAI;
import opendcs.dao.DatabaseConnectionOwner;
import decodes.db.Database;
import decodes.tsdb.DbIoException;
import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;

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
	private static final String module = "XRWriteThread";

	/** The age in msec that entries must be to be archived to database */
	private long ageToArchiveMS = 30000L;

	/** Maximum number of messages in the queue */
	private static int queueMaxSize = 10000;

	/** Messages with xmit times within this threshold are considered the same message */
	public static final long maxTimeDiffMS = 20000L;

	public static long numQueued = 0L;
	public static long lastMsgMsec = 0L;
	private DcpMonitor dcpMonitor = null;
	private XmitRecordDAI xmitRecordDao = null;
	private static final long MS_PER_DAY = 3600L * 24L * 1000L;
	public static long numWrittenToday = 0L;
	private int daynum = -1;


	/** This wraps XmitRecord and adds an insert time. */
	class XRWrapper
	{
		DcpMsg xr;
		long insertTime;

		XRWrapper(DcpMsg xr)
		{
			this.xr = xr;
			this.insertTime = System.currentTimeMillis();
		}
	}
	/** The queue */
	private ConcurrentLinkedQueue<XRWrapper> q = new ConcurrentLinkedQueue<XRWrapper>();

	/** shutdown flag */
	private boolean _shutdown;

	public XRWriteThread()
	{
		dcpMonitor = DcpMonitor.instance();
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
	public synchronized boolean enqueue(DcpMsg xr)
	{
		int sz = q.size();
Logger.instance().debug3("XRWriteThread.enqueue: " + xr.getHeader() + " queue.size=" + sz 
	+", failureCodes=" + xr.getXmitFailureCodes());
		if (sz > queueMaxSize)
		{
			Logger.instance().warning("Cannot enqueue: queue "
				+ "size is too big (" + sz + ")");
			return false;
		}

		for(XRWrapper xrw : q)
			if (xrw.xr == xr)
				return true; // already in queue

		q.add(new XRWrapper(xr));
		
		if ((++callNum % 100) == 0)
		  Logger.instance().debug1("Enqueue: Q size = " + (sz = q.size()));
		
		numQueued++;
		lastMsgMsec = xr.getXmitTime().getTime();
		
		return true;
	}

int dCallNum = 0;
	/**
	 * If there is an XmitRecord on the queue for longer than ageToArchiveMS,
	 * or if we are shutting down, return it.
	 * @return XmitRecord or null if none ready to write.
	 */
	public synchronized DcpMsg dequeue()
	{
		XRWrapper xrw = q.peek();
		if (xrw == null)
			return null;
		int sz = q.size();
		if (sz >= (queueMaxSize/2)
		 || _shutdown
		 || System.currentTimeMillis() - xrw.insertTime >= (ageToArchiveMS))
		{
			q.poll();
			if ((++dCallNum % 100) == 0)
				Logger.instance().debug1("Dequeue: Q size = " + sz);
			return xrw.xr;
		}
		else
			return null;
	}

	/**
	 * Search the queue for a message with matching address and xmit time.
	 * The time may be off by as much as maxTimeDiffMS.
	 * @param dcpAddr the dcp address
	 * @param tsms the time stamp as a millisecond value
	 * @return the XmitRecord if one exists, or null if not.
	 */
	public synchronized DcpMsg find(DcpMsg msg, Date xmitTime)
	{
		XmitMediumType mediumType = XmitMediumType.flags2type(msg.getFlagbits());
		String mediumId = msg.getDcpAddress().toString();
		XRWrapper xrw = findInQueue(mediumType, mediumId, xmitTime);
		if (xrw != null)
			return xrw.xr;
		try { return xmitRecordDao.findDcpTranmission(mediumType, mediumId, xmitTime); }
		catch(DbIoException ex)
		{
			dcpMonitor.handleDbIoException("Finding Xmit", ex);
			return null;
		}
	}
	
	/**
	 * Not synchronized, must only be called from within a synchronized method.
	 * If match is still in the queue, return its wrapper. Otherwise return null
	 * @param dcpAddress
	 * @param xmitTime
	 * @return wrapper if found, null if not
	 */
	private XRWrapper findInQueue(XmitMediumType mediumType, String mediumId, Date xmitTime)
	{
		for(Iterator<XRWrapper> it = q.iterator(); it.hasNext(); )
		{
			XRWrapper xrw = it.next();
			if (XmitMediumType.flags2type(xrw.xr.getFlagbits()) == mediumType
			 && xrw.xr.getDcpAddress().toString().equalsIgnoreCase(mediumId))
			{
				long tdiff = xrw.xr.getXmitTime().getTime() - xmitTime.getTime();
				if (tdiff > -maxTimeDiffMS && tdiff < maxTimeDiffMS)
					return xrw;
			}
		}
		return null;
	}

	/** The Thread run method. */
	public void run()
	{
		dcpMonitor.info(module + " started.");
		DatabaseConnectionOwner dbo = (DatabaseConnectionOwner)Database.getDb().getDbIo();
		xmitRecordDao = dbo.makeXmitRecordDao(31);
		xmitRecordDao.setNumDaysStorage(DcpMonitorConfig.instance().numDaysStorage);
		while(!_shutdown)
		{
			try { sleep(1000L); } catch(InterruptedException ex) {}
			processQueue();
		}
		xmitRecordDao.close();
		dcpMonitor.info(module + " exiting.");
	}

	public synchronized void processQueue()
	{
		Calendar cal = Calendar.getInstance();
		if (cal.get(Calendar.DAY_OF_YEAR) != daynum)
		{
			numWrittenToday = 0;
			daynum = cal.get(Calendar.DAY_OF_YEAR);
		}
		DcpMsg xr;
		while(!_shutdown && (xr = dequeue()) != null)
		{
			if (dcpMonitor.isIgnoreInvalidAddr() && xr.hasXmitFailureCode('I'))
			{
				Logger.instance().info("XRWriteThread.processQueue ignoring message with header '"
					+ xr.getHeader() + "' because it has an Invalid DCP Address.");
				continue;
			}
			
			if (xr.getXmitTime() != null
			 && (System.currentTimeMillis() - xr.getXmitTime().getTime()
				 > DcpMonitorConfig.instance().numDaysStorage * MS_PER_DAY))
			{
				Logger.instance().warning("Discarding too-old message: "
					+ xr.getHeader());
				continue;
			}
			try
			{
				xmitRecordDao.saveDcpTranmission(xr);
				numWrittenToday++;
			}
			catch(DbIoException ex)
			{
				dcpMonitor.handleDbIoException(module + ":processQueue", ex);
			}
		}
	}
	
	/**
	 * If the existing message is still in the queue, replace it with the passed
	 * replacement and return true. Otherwise return false.
	 * @param existing the existing message that might still be in the queue
	 * @param replacement the replacement
	 * @return true if existing was replaced in the queue
	 */
	public synchronized boolean replace(DcpMsg existing, DcpMsg replacement)
	{
		XRWrapper xrw = findInQueue(XmitMediumType.flags2type(existing.getFlagbits()), 
			existing.getDcpAddress().toString(), existing.getXmitTime());
		if (xrw != null)
		{
			xrw.xr = replacement;
			return true;
		}
		return false;
	}

	public synchronized Date getLastLocalRecvTime()
	{
		try { return xmitRecordDao.getLastLocalRecvTime(); }
		catch(Exception ex)
		{
			Logger.instance().warning(module + " getLastLocalRecvTime: " + ex);
			return null;
		}
	}
}
