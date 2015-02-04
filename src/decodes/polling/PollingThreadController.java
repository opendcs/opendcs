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

import decodes.db.TransportMedium;

public class PollingThreadController
	extends Thread
{
	public static final String module = "PollingThreadController";
	
	private PollingDataSource dataSource;
	
	/** The pool manages the ports that this controls. */
	private PortPool portPool;
	
	/** Complete list of transport media to poll */
	private ArrayList<TransportMedium> aggTMList = new ArrayList<TransportMedium>();
	
	/** The active threads currently being managed */
	private ArrayList<PollingThread> threads = new ArrayList<PollingThread>();

	/** The current index in the aggTMList */
	private int tmIdx = 0;
	
	private boolean _shutdown = false;
	
	private static final long PAUSE_MSEC = 500L;
	
	private int successfullPolls = 0, failedPolls = 0;
	
	private int pollNumTries = 3;
	
	private int maxBacklogHours = 48;
	private int minBacklogHours = 2;
	
	private String saveSessionFile = null;
	
	/** PollingDataSource creates the controller */
	public PollingThreadController(PollingDataSource dataSource, ArrayList<TransportMedium> aggTMList,
		PortPool portPool)
	{
		super();
		this.dataSource = dataSource;
		this.aggTMList = aggTMList;
		this.portPool = portPool;
		
		// In a modem pool, the max # of threads will be limited by available ports
		int numPorts = portPool.getNumPorts();
	}

	@Override
	public void run()
	{
		_shutdown = false;

		// Construct a PollingThread runnable for each Transport Medium
		while (tmIdx < aggTMList.size())
			threads.add(new PollingThread(this, dataSource, aggTMList.get(tmIdx++)));
		if (threads.size() == 0)
		{
			dataSource.log(Logger.E_WARNING, "There are no stations to poll in the list.");
			_shutdown = true;
		}
		dataSource.log(Logger.E_DEBUG1, module + " starting. " + threads.size() 
			+ " sessions will be attempted."
			+ " #waiting=" + countThreads(PollingThreadState.Waiting));
		
long debugmsec = 0L;
		tmIdx = 0;
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
						if (--tmIdx < 0)
							tmIdx = threads.size()-1;
					}
					else // start a new poll on the allocated port.
					{
						dataSource.log(Logger.E_DEBUG1, module + " starting " + pt.getModule()
							+ ", TM " + pt.getTransportMedium()
							+ " on port number " + ioPort.getPortNum());
						pt.setState(PollingThreadState.Running);
						pt.setSaveSessionFile(saveSessionFile);
						pt.setIoPort(ioPort);
						(new Thread(pt)).start();
					}
				}
				if (System.currentTimeMillis() - debugmsec > 10000L)
				{
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
	
	private PollingThread getNextWaitingThread()
	{
		if (tmIdx >= threads.size())
			tmIdx = 0;
		// Only check each thread once.
		for(int n = 0; n < threads.size(); n++)
		{
			PollingThread pt = threads.get(tmIdx);
			if (++tmIdx >= threads.size())
				tmIdx = 0;
			if (pt.getState() == PollingThreadState.Waiting)
				return pt;
		}
		// Went all the way around and didn't find a waiting thread
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
		pollingThread.info(module + ".pollComplete result=" + pollingThread.getState().toString());
		
		portPool.releasePort(pollingThread.getIoPort(), pollingThread.getState(),
			pollingThread.getTerminatingException() != null 
			&& (pollingThread.getTerminatingException() instanceof DialException));
		if (pollingThread.getState() == PollingThreadState.Success)
		{
			successfullPolls++;
		}
		else if (!_shutdown && pollingThread.getNumTries() < pollNumTries)
		{
			pollingThread.warning("Polling attempt " + pollingThread.getNumTries()
				+ " failed. Will retry.");
			pollingThread.reset();
		}
		else
		{
			pollingThread.warning("Polling attempt " + pollingThread.getNumTries()
				+ " failed. Max retries reached.");
			failedPolls++;
		}
	}
	
	/**
	 * Count the number of threads with a matching state.
	 * @param threadState
	 * @return the number of threads with a matching state.
	 */
	private int countThreads(PollingThreadState threadState)
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

}
