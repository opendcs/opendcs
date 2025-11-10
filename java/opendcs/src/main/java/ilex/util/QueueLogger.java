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
package ilex.util;

import java.util.Vector;
import java.util.concurrent.SubmissionPublisher;

import org.opendcs.logging.LoggingEvent;

/**
* Fixed length queue with mechanisms for tracking current and next position.
* Uses new RingBuffer and Logging Event mechanisms to populate.
*
*/
public class QueueLogger
{
	/** Messages stored internally in a vector */
	private final PublicRRVector<String> messages;

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
	public QueueLogger(SubmissionPublisher<LoggingEvent> logBuffer)
	{
		messages = new PublicRRVector<>(MAX_MESSAGES, QUEUE_INCREMENT);
		startIndex = 0;
		logBuffer.consume(le ->
		{
			doLog(le);
		});
	}

	/**
	* Logs a message into the queue.
	* @param event The raw event from the log.
	*/
	public void doLog(LoggingEvent event)
	{
		String msg = event.toString();
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
			return messages.get(idx);
		else if (idx == n)
			return null;
		else
			throw new IndexRangeException("too high");
	}

	private static class PublicRRVector<T> extends Vector<T>
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
}
