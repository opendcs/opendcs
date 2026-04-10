/*
 * This software was written by Cove Software, LLC ("COVE") under contract
 * to Alberta Environment and Sustainable Resource Development (Alberta ESRD).
 * No warranty is provided or implied other than specific contractual terms 
 * between COVE and Alberta ESRD.
 *
 * Copyright 2014 Alberta Environment and Sustainable Resource Development.
 * 
 * Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
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

import java.util.ArrayList;
import java.util.Date;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.Platform;
import decodes.db.PlatformStatus;
import decodes.db.TransportMedium;
import decodes.routing.DacqEventLogger;

public class PollingThreadController extends Thread
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
				log.warn("Not polling TM '{}' because no associated platform.",
						 aggTMList.get(idx).getMediumId());
				continue;
			}
			if (p.getIgnoreSeason() != null && p.getIgnoreSeason().isInSeason(now))
			{
				log.warn("Not polling platform {} because we are in its ignoreSeason '{}'",
						 p.makeFileName(), p.getIgnoreSeason().getName());
				continue;
			}
			if (p.getProcessSeason() != null && !p.getProcessSeason().isInSeason(now))
			{
				log.info("Not polling platform {} because we are NOT in its processSeason '{}'",
						 p.makeFileName(), p.getProcessSeason().getName() + "'");
				continue;
			}
			PollingThread pt = new PollingThread(this, dataSource, aggTMList.get(idx));
			pt.setMaxTries(pollNumTries);
			threads.add(pt);
		}
		if (threads.size() == 0)
		{
			log.warn("There are no stations to poll in the list.");
			_shutdown = true;
		}
		log.debug("starting. {} sessions will be attempted. #waiting={}",
				  threads.size(), countThreads(PollingThreadState.Waiting));
		
		long debugmsec = 0L;
		lastPTidx = 0;
		try
		{
			while(!_shutdown)
			{
				if (countThreads(PollingThreadState.Waiting) == 0
				 && countThreads(PollingThreadState.Running) == 0)
				{
					log.info(" Polling complete. All stations polled.");
					_shutdown = true;
					break;
				}
				
				PollingThread pt = getNextWaitingThread();
				if (pt != null)
				{
					IOPort ioPort = portPool.allocatePort();
					if (ioPort == null)
					{
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
						log.debug("starting {}, TM {} on port number {}, pollPriority={}",
								  pt.getModule(), pt.getTransportMedium(), ioPort.getPortNum(), pt.getPollPriority());
						(new Thread(pt)).start();
					}
				}
				if (System.currentTimeMillis() - debugmsec > 10000L)
				{
					checkDeadThreads();
					log.info("Threads: total={}, waiting={}, running={}, success={}, failed={}",
							 threads.size(), countThreads(PollingThreadState.Waiting),
							 countThreads(PollingThreadState.Running), countThreads(PollingThreadState.Success),
							 countThreads(PollingThreadState.Failed));
					debugmsec = System.currentTimeMillis();
				}
				
				try { sleep(PAUSE_MSEC); } catch(InterruptedException ex) {}
			}
			
			// Kill any PollingThreads that are still alive.
			log.info("checking {} polling threads", threads.size());
			int nk = 0;
			for(PollingThread pt : threads)
				if (pt.getState() == PollingThreadState.Running)
				{
					pt.shutdown();
					nk++;
				}
			
			// Wait up to 30 sec until all the kids have called pollComplete().
			if (nk > 0)
			{
				log.info(" Will wait up to 30 sec for {} polling threads to terminate.", nk);
			}
			else
			{
				log.info(" All threads terminated, proceeding with shutdown.");
			}
	
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
		log.info("Polling finished. {} stations polled, {} success, {} failed.",
				 aggTMList.size(), successfullPolls, failedPolls);
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
				log.warn("Killing dead thread {}", pt.getModule());
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
		log.info("pollComplete result={}, attempt #{}",
				 pollingThread.getState().toString(), pollingThread.getNumTries());
		
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
			log.info("Polling attempt {} failed. Will retry.", pollingThread.getNumTries());
			pollingThread.reset();
		}
		else
		{
			log.error("Polling attempt {} failed. Max retries reached.", pollingThread.getNumTries());
			failedPolls++;
			PlatformStatus platstat = pollingThread.getPlatformStatus();
			// Assert error through RS Thread will increment # errors.
			dataSource.getRoutingSpecThread().assertPlatformError("Polling Failed", platstat, null);
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
