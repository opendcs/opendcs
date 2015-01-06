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
	
	private int maxBacklogHours = 24;
	
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
		dataSource.log(Logger.E_DEBUG1, module + " starting");
		
		// Construct a PollingThread runnable for each Transport Medium
		while (tmIdx < aggTMList.size())
			threads.add(new PollingThread(this, dataSource, aggTMList.get(tmIdx++)));
		if (threads.size() == 0)
		{
			dataSource.log(Logger.E_WARNING, "There are no stations to poll in the list.");
			_shutdown = true;
		}
		dataSource.log(Logger.E_DEBUG1, module + " " + threads.size() + " session threads will run.");
		
		tmIdx = 0;
		while(!_shutdown)
		{
			if (countThreads(PollingThreadState.Waiting) == 0
			 && countThreads(PollingThreadState.Running) == 0)
			{
				dataSource.log(Logger.E_INFORMATION, "Polling complete. All stations polled.");
				_shutdown = true;
				break;
			}
			
			PollingThread pt = getNextWaitingThread();
			if (pt != null)
			{
				pt.setState(PollingThreadState.Running);
				pt.setSaveSessionFile(saveSessionFile);
				IOPort ioPort = portPool.allocatePort();
				if (ioPort == null)
				{
					dataSource.log(Logger.E_DEBUG1, "No ports available, will try later.");
					// go back so that we try the same polling thread again next time.
					if (--tmIdx < 0)
						tmIdx = threads.size()-1;
				}
				else // start a new poll on the allocated port.
				{
					pt.setIoPort(ioPort);
					(new Thread(pt)).start();
				}
			}
			
			try { sleep(PAUSE_MSEC); } catch(InterruptedException ex) {}
		}
		
		// Kill any PollingThreads that are still alive.
		for(PollingThread pt : threads)
			if (pt.getState() == PollingThreadState.Running)
				pt.shutdown();
		
		// Wait up to 30 sec until all the kids have called pollComplete().
		long x = System.currentTimeMillis();
		while(countThreads(PollingThreadState.Running) > 0
			&& System.currentTimeMillis() - x < 30000L)
		{	
			try { sleep(PAUSE_MSEC); }
			catch(InterruptedException ex) {}
		}
		portPool.close();
		dataSource.log(Logger.E_INFORMATION, "Polling finished. "
			+ aggTMList.size() + " stations polled, " + successfullPolls + " success, "
			+ failedPolls + " failed.");
		dataSource.pollingComplete();
	}
	
	private PollingThread getNextWaitingThread()
	{
		if (tmIdx >= threads.size())
			tmIdx = 0;
		int start = tmIdx;
		do
		{
			PollingThread pt = threads.get(tmIdx);
			if (++tmIdx >= threads.size())
				tmIdx = 0;
			if (pt.getState() == PollingThreadState.Waiting)
				return pt;
		} while(tmIdx != start);
		// Went all the way around and didn't find a waiting thread
		return null;
	}
	
	public void shutdown()
	{
		_shutdown = true;
	}

	/**
	 * Called from the polling thread after it completes.
	 * @param pollingThread
	 */
	public void pollComplete(PollingThread pollingThread)
	{
		portPool.releasePort(pollingThread.getIoPort(), pollingThread.getState());
		if (pollingThread.getState() == PollingThreadState.Success)
		{
			dataSource.log(Logger.E_DEBUG1, "Polling of " + pollingThread.getTransportMedium().toString()
				+ " was successfull.");
			successfullPolls++;
		}
		else if (!_shutdown && pollingThread.getNumTries() < pollNumTries)
		{
			dataSource.log(Logger.E_DEBUG1, "Polling of " + pollingThread.getTransportMedium().toString()
				+ " failed. Will retry.");
			pollingThread.reset();
		}
		else
		{
			dataSource.log(Logger.E_DEBUG1, "Polling of " + pollingThread.getTransportMedium().toString()
				+ " failed. " + pollingThread.getNumTries() + " attempts have been made.");
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

}
