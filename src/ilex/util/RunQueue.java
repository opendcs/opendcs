/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2004/08/30 14:50:30  mjmaloney
*  Javadocs
*
*  Revision 1.3  2001/02/28 15:57:17  mike
*  Don't add a runnable object to run queue if it's already present.
*
*  Revision 1.2  2000/06/07 15:09:37  mike
*  dev
*
*  Revision 1.1  2000/03/25 22:06:35  mike
*  Created
*
*/
package ilex.util;

import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Collections;

/**
* RunQueue provides a thread that processes runnable objects placed
* in a queue. It is used to synchronize activities involving a resource
* that cannot be multi-threaded.
* <p>
* For example, CORBA operations in a complex GUI application should be
* handled in a run queue. This guarantees that they will be executed in
* series.
*/
public class RunQueue extends Thread
{
	private List queue;
	private List periodicQueue;
	private long checkDelay;

	/**
	* Constructs a new RunQueue for processing Runnable objects in series.
	* A RunQueue is a general purpose utility for establishing background
	* threads. Use it to guarantee that tasks are executed in a coordinated
	* manner, similar to the GUI event handling thread in AWT.
	* <p>
	* It can also be used to establish a thread that executes the same
	* task periodically.
	* <p>
	* By default the timer delay for checking the queue is 200 ms. If
	* you want a different value, use the other version of the construct.
	*/
	public RunQueue( )
	{
		super();
		queue = Collections.synchronizedList(new LinkedList());
		checkDelay = 200L;   // Check queue every 200ms. when idle.
		periodicQueue = null;
	}

	/**
	* Constructs a new RunQueue with a specified timer delay.
	* @param checkDelay number of msec to pause after each check
	*/
	public RunQueue( long checkDelay )
	{
		this();
		this.checkDelay = checkDelay;
	}

	/**
	* Thread run method continually checks run queue and executes
	* Runnable objects on a timer.
	* Do not call this method. Rather call Thread.start().
	*/
	public void run( )
	{
		while(true)
		{
			checkPeriodicQueue();
			if (!queue.isEmpty())
			{
				Runnable task = (Runnable)queue.remove(0);
				task.run();
			}
			else
			{
				try { Thread.sleep(checkDelay); }
				catch (InterruptedException ie){}
			}
		}
	}

	/**
	* Adds a Runnable object to the queue.
	* The passed task will be executed the next time that this RunQueue
	* wakes up.
	* @param task the task to add
	*/
	public void add( Runnable task )
	{
		if (!queue.contains(task)) // Don't add if already present.
			queue.add(task);
	}

	/**
	* Adds a Runnable object to be executed repeatedly at the specified
	* period.
	* If period is less than the check delay for this run queue, it
	* will be set to the check delay, and therefore executed every
	* cycle.
	* @param task the task to add
	* @param periodMsec the number of msec to pause between runs
	*/
	public void startPeriodic( Runnable task, long periodMsec )
	{
		if (periodicQueue == null)
			periodicQueue = new LinkedList();
		periodicQueue.add(new PeriodicTask(task, periodMsec));
	}

	/**
	* Tells the RunQueue to stop executing the specified task.
	* @param task the task to stop
	*/
	public void stopPeriodic( Runnable task )
	{
		if (periodicQueue != null && !periodicQueue.isEmpty())
		{
			ListIterator li = periodicQueue.listIterator();
			while(li.hasNext())
			{
				PeriodicTask pt = (PeriodicTask)li.next();
				if (pt.getRunnable() == task)
				{
					periodicQueue.remove(pt);
					return;
				}
			}
		}
	}

	private void checkPeriodicQueue( )
	{
		if (periodicQueue != null && !periodicQueue.isEmpty())
		{
			long now = System.currentTimeMillis();
			ListIterator li = periodicQueue.listIterator();
			while(li.hasNext())
			{
				PeriodicTask pt = (PeriodicTask)li.next();
				if (pt.isTimeToExecute(now))
					add(pt.getRunnableForExec(now));
			}
		}
	}

/*
	public static void main(String[] args) // For test only!!!
	{
		RunQueue rq = new RunQueue();
		Runnable testrun =
			new Runnable()
			{
				public void run()
				{
					System.out.println("test "+
						(System.currentTimeMillis()/1000));
				}
			};
		rq.startPeriodic(testrun, 2000L);
		rq.start();
	}
*/
}

class PeriodicTask
{
	private Runnable task;
	private long periodMsec;
	private long lastExec;

	/**
	* @param task
	* @param periodMsec
	*/
	PeriodicTask( Runnable task, long periodMsec )
	{
		this.task = task;
		this.periodMsec = periodMsec;
	}

	/**
	* @param now
	* @return
	*/
	boolean isTimeToExecute( long now )
	{
		return now >= lastExec + periodMsec;
	}

	/**
	* @return
	*/
	Runnable getRunnable( )
	{
		return task;
	}

	/**
	* @param now
	* @return
	*/
	Runnable getRunnableForExec( long now )
	{
		lastExec = now;
		return task;
	}
}
