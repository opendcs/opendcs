/*
*	$Id$
*
*	$Log$
*	Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*	OPENDCS 6.0 Initial Checkin
*	
*	Revision 1.1  2008/04/04 18:21:10  cvs
*	Added legacy code to repository
*	
*	Revision 1.5  2005/09/28 21:54:54  mmaloney
*	LRGS 5.3 prep
*	
*	Revision 1.4  2005/04/06 12:21:18  mjmaloney
*	dev
*	
*	Revision 1.3  2004/08/30 15:35:16  mjmaloney
*	Renamed BadIndexException to IndexRangeException because of class with ilex.
*	var class.
*	
*	Revision 1.2  2004/08/30 14:50:30  mjmaloney
*	Javadocs
*	
*	Revision 1.1  2004/05/06 20:57:10  mjmaloney
*	Implemented QueueLogger to be used by servers that export events.
*	
*/
package ilex.util;

import java.util.Vector;

/**
* Concrete subclass of Logger that stores messages in a queue of fixed
* size. When size is reached the oldest messages are deleted. Messages
* can be retrieved by index.
*/
public class QueueLogger extends Logger
{
	/** Messages stored internally in a vector */
	private PublicRRVector messages;

	/**
	* This is the index of the first element in the vector. Initially this
	* is set to 0. As queue fills and deletes old messages, this increases.
	*/
	int startIndex;

	/** This many messages are stored on the queue */
	public static final int MAX_MESSAGES = 1000;

	/** This many messages are deleted when the queue is full. */
	public static final int QUEUE_INCREMENT = 100;

	/**
	* Construct with a process name.
	* If you don't want to use the process name string, pass the
	* empty string (i.e. "").
	* @param procName the process name
	*/
	public QueueLogger( String procName, int minLogPriority )
	{
		super(procName,minLogPriority);
		messages = new PublicRRVector(MAX_MESSAGES, QUEUE_INCREMENT);
		startIndex = 0;
	}

	/**
	* Discards the queue.
	*/
	public void close( ) 
	{
		messages = null;
	}

	/**
	* Logs a message into the queue.
	* @param priority the priority.
	* @param text the formatted message.
	*/
	public void doLog( int priority, String text )
	{
		String msg = standardMessage(priority, text);
		addToQueue(msg);
	}

	/**
	* Adds a formatted message to the queue.
	* @param msg the message
	*/
	public synchronized void addToQueue( String msg )
	{
		if (messages.size() >= MAX_MESSAGES)
		{
			messages.removeRange(0, QUEUE_INCREMENT);
			startIndex += QUEUE_INCREMENT;
		}
		messages.add(msg);
	}

	/**
	* @return the index of the oldest message in the queue.
	*/
	public int getStartIdx( ) { return startIndex; }

	/**
	* @returns the index of the next (not yet written) message
	*/
	public int getNextIdx( ) { return startIndex + messages.size(); }

	/**
	* Returns the specified message from the queue.
	* Returns null if the requested index is the next index to be written,
	* which essentially means to try again later.
	* @param idx
	* @returns specified message from queue, or null if index is next.
	* @throws IndexRangeException if the requested index is before the start of
	* the queue or after the next value.
	*/
	public synchronized String getMsg( int idx ) throws IndexRangeException
	{
		if (idx < startIndex)
			throw new IndexRangeException("too low");
		idx -= startIndex;
		int n = messages.size();
		if (idx < n)
			return (String)messages.get(idx);
		else if (idx == n)
			return null;
		else
			throw new IndexRangeException("too high");
	}
}

class PublicRRVector extends Vector
{
	/**
	* @param max
	* @param inc
	*/
	PublicRRVector( int max, int inc )
	{
		super(max, inc);
	}

	// Have to overload this to make it public.
	/**
	* @param from
	* @param to
	*/
	public void removeRange( int from, int to )
	{
		super.removeRange(from, to);
	}
}
