/*
 * $Id$
 * 
 * This software was written by Cove Software, LLC ("COVE") under contract
 * to Alberta Environment and Sustainable Resource Development (Alberta ESRD).
 * No warranty is provided or implied other than specific contractual terms 
 * between COVE and Alberta ESRD.
 *
 * Copyright 2014 Alberta Environment and Sustainable Resource Development.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package decodes.polling;

import ilex.util.Logger;

import java.util.ArrayList;
import java.util.Date;

import decodes.db.Platform;
import decodes.db.PlatformStatus;
import decodes.db.TransportMedium;
import decodes.routing.DacqEventLogger;

public class PollingThreadController
	extends Thread
{
	public static String module = "PollingThreadController";
	
	protected PollingDataSource dataSource;
	
	/** The pool manages the ports that this controls. */
	protected PortPool portPool;
	
	/** Complete list of transport media to poll */
	protected ArrayList<TransportMedium> aggTMList = new ArrayList<TransportMedium>();
	
	/** The active threads currently being managed */
	protected ArrayList<PollingThread> threads = new ArrayList<PollingThread>();

	/** The current index in the aggTMList */
	protected int lastPTidx = 0;
	
	protected boolean _shutdown = false;
	
	protected static final long PAUSE_MSEC = 500L;

	// no thread should take > 10 min -- probably hung
	protected static final long THREAD_MAX_RUN_TIME = 600000L;
	
	protected int successfullPolls = 0, failedPolls = 0;
	
	protected int pollNumTries = 5;
	
	protected int maxBacklogHours = 48;
	protected int minBacklogHours = 2;
	
	protected String saveSessionFile = null;
	
	protected DacqEventLogger dacqEventLogger = null;
	protected int numPorts = 10;
	
	
	/** PollingDataSource creates the controller */
	public PollingThreadController(PollingDataSource dataSource, ArrayList<TransportMedium> aggTMList,
		PortPool portPool)
	{
		super();
		this.dataSource = dataSource;
		this.aggTMList = aggTMList;
		this.portPool = portPool;
		
		// In a modem pool, the max # of threads will be limited by available ports
		// In a TCP client pool, the max # of client threads to start.
		// In a listening socket, the max # of clients to accept.
		this.numPorts = portPool.getNumPorts();
	}

	@Override
	public void run()
	{
		_shutdown = false;

		// Construct a PollingThread runnable for each Transport Medium
		Date now = new Date();
		for(int idx = 0; idx < aggTMList.size(); idx++)
		{
			Platform p = aggTMList.get(idx).platform;
			if (p == null)
			{
				// This should not happen.
				dataSource.log(Logger.E_WARNING, module + " Not polling TM '" 
					+ aggTMList.get(idx).getMediumId() +"' because no associated platform.");
				continue;
			}
			if (p.getIgnoreSeason() != null && p.getIgnoreSeason().isInSeason(now))
			{
				dataSource.log(Logger.E_INFORMATION, module + " Not polling platform "
					+ p.makeFileName() + " because we are in its ignoreSeason '"
					+ p.getIgnoreSeason().getName() + "'");
				continue;
			}
			if (p.getProcessSeason() != null && !p.getProcessSeason().isInSeason(now))
			{
				dataSource.log(Logger.E_INFORMATION, module + " Not polling platform "
					+ p.makeFileName() + " because we are NOT in its processSeason '"
					+ p.getProcessSeason().getName() + "'");
				continue;
			}
			threads.add(new PollingThread(this, dataSource, aggTMList.get(idx)));
		}
		if (threads.size() == 0)
		{
			dataSource.log(Logger.E_WARNING, "There are no stations to poll in the list.");
			_shutdown = true;
		}
		dataSource.log(Logger.E_DEBUG1, module + " starting. " + threads.size() 
			+ " sessions will be attempted."
			+ " #waiting=" + countThreads(PollingThreadState.Waiting));
		
		long debugmsec = 0L;
		lastPTidx = 0;
		try
		{
			while(!_shutdown)
			{
				if (countThreads(PollingThreadState.Waiting) == 0
				 && countThreads(PollingThreadState.Running) == 0)
				{
					dataSource.log(Logger.E_INFORMATION, module + " Polling complete. All stations polled.");
					_shutdown = true;
					break;
				}
				
				PollingThread pt = getNextWaitingThread();
				if (pt != null)
				{
					IOPort ioPort = portPool.allocatePort();
					if (ioPort == null)
					{
	//					dataSource.log(Logger.E_DEBUG2, module + " No ports available, will try later.");
						// go back so that we try the same polling thread again next time.
						if (--lastPTidx < 0)
							lastPTidx = threads.size()-1;
					}
					else // start a new poll on the allocated port.
					{
						pt.setState(PollingThreadState.Running);
						pt.setSaveSessionFile(saveSessionFile);
						pt.setIoPort(ioPort);
						pt.setThreadStart(new Date());
						dataSource.log(Logger.E_DEBUG1, module + " starting " 
							+ pt.getModule()
							+ ", TM " + pt.getTransportMedium()
							+ " on port number " + ioPort.getPortNum()
							+ ", pollPriority=" + pt.getPollPriority());
						(new Thread(pt)).start();
					}
				}
				if (System.currentTimeMillis() - debugmsec > 10000L)
				{
					checkDeadThreads();
					dataSource.log(Logger.E_INFORMATION, 
						module + " Threads: total=" + threads.size()
						+ ", waiting=" + countThreads(PollingThreadState.Waiting) 
						+ ", running=" + countThreads(PollingThreadState.Running)
						+ ", success=" + countThreads(PollingThreadState.Success)
						+ ", failed=" + countThreads(PollingThreadState.Failed)
						);
					debugmsec = System.currentTimeMillis();
				}
				
				try { sleep(PAUSE_MSEC); } catch(InterruptedException ex) {}
			}
			
			// Kill any PollingThreads that are still alive.
			dataSource.log(Logger.E_INFORMATION, module + " checking " + threads.size() + " polling threads");
			int nk = 0;
			for(PollingThread pt : threads)
				if (pt.getState() == PollingThreadState.Running)
				{
					pt.shutdown();
					nk++;
				}
			
			// Wait up to 30 sec until all the kids have called pollComplete().
			if (nk > 0)
				dataSource.log(Logger.E_INFORMATION, module + " Will wait up to 30 sec for " + nk + " polling threads to terminate.");
			else
				dataSource.log(Logger.E_INFORMATION, module + " All threads terminated, proceeding with shutdown.");
	
			long x = System.currentTimeMillis();
			while(countThreads(PollingThreadState.Running) > 0
				&& System.currentTimeMillis() - x < 30000L)
			{	
				try { sleep(PAUSE_MSEC); }
				catch(InterruptedException ex) {}
			}
		}
		finally
		{
			portPool.close();
		}
		dataSource.log(Logger.E_INFORMATION, "Polling finished. "
			+ aggTMList.size() + " stations polled, " + successfullPolls + " success, "
			+ failedPolls + " failed.");
		dataSource.pollingComplete();
	}
	
	protected void checkDeadThreads()
	{
		for(PollingThread pt : threads)
			if (pt.getState() == PollingThreadState.Running
			 && pt.getThreadStart() != null
			 && System.currentTimeMillis() - pt.getThreadStart().getTime() > 
				THREAD_MAX_RUN_TIME)
			{
				dataSource.log(Logger.E_WARNING, "Killing dead thread " + pt.getModule());
				pt.shutdown();
				pt.setState(PollingThreadState.Failed);
				pollComplete(pt);
			}
	}

	private PollingThread getNextWaitingThread()
	{
		// Starting with thread after last returned index, find thread with highest
		// priority.
		if (++lastPTidx >= threads.size())
			lastPTidx = 0;
		int p1idx=-1, p2idx=-1, p3idx=-1;
		for(int n = 0; n < threads.size(); n++)
		{
			int idx = (lastPTidx + n) % threads.size();
			PollingThread pt = threads.get(idx);
			if (pt.getState() == PollingThreadState.Waiting)
			{
				if (pt.getPollPriority() == 1)
				{
					p1idx = idx;
					break;
				}
				else if (pt.getPollPriority() == 2)
					p2idx = idx;
				else
					p3idx = idx;
			}
		}
		if (p1idx != -1)
			return threads.get(lastPTidx = p1idx);
		else if (p2idx != -1)
			return threads.get(lastPTidx = p2idx);
		else if (p3idx != -1)
			return threads.get(lastPTidx = p3idx);
		// Went didn't find any waiting thread
		return null;
	}
	
	public void shutdown()
	{
		for(PollingThread pt : threads)
			pt.shutdown();
		portPool.close();
		_shutdown = true;
	}

	/**
	 * Called from the polling thread after it completes.
	 * @param pollingThread
	 */
	public void pollComplete(PollingThread pollingThread)
	{
		pollingThread.info(module + ".pollComplete result=" + pollingThread.getState().toString()
			+ ", attempt #" + pollingThread.getNumTries());
		
		PollException pex = pollingThread.getTerminatingException();
		boolean portError = (pex != null)
			&& (pex instanceof DialException)
			&& ((DialException)pex).isPortError();
			
		portPool.releasePort(pollingThread.getIoPort(), pollingThread.getState(), portError);
		if (pollingThread.getState() == PollingThreadState.Success)
		{
			successfullPolls++;
		}
		else if (!_shutdown && pollingThread.getNumTries() < pollNumTries)
		{
			pollingThread.info("Polling attempt " + pollingThread.getNumTries()
				+ " failed. Will retry.");
			pollingThread.reset();
		}
		else
		{
			pollingThread.failure("Polling attempt " + pollingThread.getNumTries()
				+ " failed. Max retries reached.");
			failedPolls++;
			PlatformStatus platstat = pollingThread.getPlatformStatus();
			// Assert error through RS Thread will increment # errors.
			dataSource.getRoutingSpecThread().assertPlatformError("Polling Failed", platstat);
		}
	}
	
	/**
	 * Count the number of threads with a matching state.
	 * @param threadState
	 * @return the number of threads with a matching state.
	 */
	protected int countThreads(PollingThreadState threadState)
	{
		int n = 0;
		for(PollingThread pt : threads)
			if (pt.getState() == threadState)
				n++;
		return n;
	}

	public int getPollNumTries()
	{
		return pollNumTries;
	}

	public void setPollNumTries(int pollNumTries)
	{
		this.pollNumTries = pollNumTries;
	}

	public void setMaxBacklogHours(int maxBacklogHours)
	{
		this.maxBacklogHours = maxBacklogHours;
	}

	public int getMaxBacklogHours()
	{
		return maxBacklogHours;
	}

	public void setSaveSessionFile(String saveSessionFile)
	{
		this.saveSessionFile = saveSessionFile;
	}

	public int getMinBacklogHours()
	{
		return minBacklogHours;
	}

	public void setMinBacklogHours(int minBacklogHours)
	{
		this.minBacklogHours = minBacklogHours;
	}

	public void setDacqEventLogger(DacqEventLogger dacqEventLogger)
	{
		this.dacqEventLogger = dacqEventLogger;
	}

	public DacqEventLogger getDacqEventLogger()
	{
		return dacqEventLogger;
	}

	public PollingDataSource getDataSource()
	{
		return dataSource;
	}

}
