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
package decodes.dcpmon;


import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import opendcs.dai.XmitRecordDAI;
import opendcs.dao.DatabaseConnectionOwner;
import decodes.db.Database;
import decodes.tsdb.DbIoException;
import lrgs.common.DcpMsg;

/**
This class is responsible for writing XmitRimport ilex.util.Logger;ecords to the SQL database.
It maintains a queue. XmitRecords are only written after they've lived
in the queue for agesec seconds. The reason is that we may get several
DCP 'Messages' that effect the same XmitRecord (DAPS Status Messages,
duplicates, etc.). These separate messages will likely arrive withing
a short time frame. Hence the XmitRecord has a chance to 'settle' before
it is written to the database. This reduces the number of database writes.
*/
public class XRWriteThread extends Thread
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
		log.trace("XRWriteThread.enqueue: {} queue.size={}, failureCodes={}",
				  xr.getHeader(), sz, xr.getXmitFailureCodes());
		if (sz > queueMaxSize)
		{
			log.warn("Cannot enqueue: queue size is too big ({})", sz);
			return false;
		}

		for(XRWrapper xrw : q)
			if (xrw.xr == xr)
				return true; // already in queue

		q.add(new XRWrapper(xr));

		if ((++callNum % 100) == 0)
		{
		  log.debug("Enqueue: Q size = {}", (sz = q.size()));
		}

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
			{
				log.debug("Dequeue: Q size = {}", sz);
			}
			return xrw.xr;
		}
		else
			return null;
	}

	/**
	 * Search the queue for a message with matching address and xmit time.
	 * The time may be off by as much as maxTimeDiffMS.
	 *
	 * @param msg A message object with address defined
	 * @param xmitTime java date with the xmit time of the message.
	 * @return the XmitRecord if one exists, or null if not.
	 */
	public synchronized DcpMsg find(DcpMsg msg, Date xmitTime)
	{
		XmitMediumType mediumType = XmitMediumType.flags2type(msg.getFlagbits());
		String mediumId = msg.getDcpAddress().toString();
		XRWrapper xrw = findInQueue(mediumType, mediumId, xmitTime);
		if (xrw != null)
		{
			return xrw.xr;
		}
		DatabaseConnectionOwner dbo = (DatabaseConnectionOwner)Database.getDb().getDbIo();
		try(XmitRecordDAI xmitRecordDao = dbo.makeXmitRecordDao(31))
		{
			return xmitRecordDao.findDcpTranmission(mediumType, mediumId, xmitTime);
		}
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
		log.info(module + " started.");

		DatabaseConnectionOwner dbo = (DatabaseConnectionOwner)Database.getDb().getDbIo();

		// 4/30/19 setNumDaysStorage will call deleteOldTableData. After that, call delete periodically.

		long lastDeleteOld = System.currentTimeMillis();

		while(!_shutdown)
		{
			try { sleep(1000L); } catch(InterruptedException ex) {}
			try(XmitRecordDAI xmitRecordDao = dbo.makeXmitRecordDao(31);)
			{
				xmitRecordDao.setNumDaysStorage(DcpMonitorConfig.instance().numDaysStorage);
				processQueue(xmitRecordDao);

				if (System.currentTimeMillis() - lastDeleteOld > MS_PER_DAY)
				{
					try
					{
						xmitRecordDao.deleteOldTableData();
					}
					catch (DbIoException ex)
					{
						log.atError().setCause(ex).log("Exception in deleteOldTableData.");
					}
				}
			}
		}
		log.info(module + " exiting.");
	}

	public synchronized void processQueue(XmitRecordDAI xmitRecordDao)
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
				log.info("XRWriteThread.processQueue ignoring message with header '{}'" +
						 " because it has an Invalid DCP Address.",
						 xr.getHeader());
				continue;
			}

			if (xr.getXmitTime() != null
			 && (System.currentTimeMillis() - xr.getXmitTime().getTime()
				 > DcpMonitorConfig.instance().numDaysStorage * MS_PER_DAY))
			{
				log.warn("Discarding too-old message: {}", xr.getHeader());
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
		DatabaseConnectionOwner dbo = (DatabaseConnectionOwner)Database.getDb().getDbIo();
		try(XmitRecordDAI xmitRecordDao = dbo.makeXmitRecordDao(31);)
		{
			return xmitRecordDao.getLastLocalRecvTime();
		}
		catch(Exception ex)
		{
			log.atWarn().setCause(ex).log("Error in getLastLocalRecvTime.");
			return null;
		}
	}
}
