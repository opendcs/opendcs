/*
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
package opendcs.dai;

import java.util.ArrayList;
import java.util.Date;

import decodes.polling.DacqEvent;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;

/**
 * Defines methods for reading and writing data acquisition events.
 */
public interface DacqEventDAI
	extends DaiBase
{
	/**
	 * Close the DAO and release any allocated resources.
	 */
	public void close();

	/**
	 * Writes a data acquistion event to the database.
	 * @param evt the event.
	 * @throws DbIoException
	 */
	public void writeEvent(DacqEvent evt)
		throws DbIoException;

	/**
	 * Deletes all events before the specified cutoff
	 * @param cutoff the cutoff date. All events before this are deleted
	 * @throws DbIoException
	 */
	public void deleteBefore(Date cutoff)
		throws DbIoException;
	
	/**
	 * Read events that contain the specified text.
	 * Used to read device name, as well as generalized search capability.
	 * Events are returned in time order.
	 * <p/>
	 * If there are any events already in evtList, the search is restricted
	 * to events with times later than the last event in the list. Thus this
	 * method can be called periodically to refresh the same list.
	 * 
	 * @param text The text to search for
	 * @param evtList the event list to add to.
	 * @return number of events added to the list
	 */
	public int readEventsContaining(String text, ArrayList<DacqEvent> evtList)
		throws DbIoException;
	
	/**
	 * Read events for a specific schedule entry status ID.
	 * Events are added to the end of the passed array in time order.
	 * If evtList is not empty, only events after the last in the array are read.
	 * @param scheduleEntryStatusId
	 * @param evtList
	 * @return number of events added to the list
	 * @throws DbIoException
	 */
	public int readEventsForScheduleStatus(DbKey scheduleEntryStatusId, ArrayList<DacqEvent> evtList)
		throws DbIoException;

	/**
	 * Read events for a specific Platform by its ID.
	 * Events are added to the end of the passed array in time order.
	 * If evtList is not empty, only events after the last in the array are read.
	 * @param platformId the ID of the platform for matching events
	 * @param evtList
	 * @return number of events added to the list
	 * @throws DbIoException
	 */
	public int readEventsForPlatform(DbKey platformId, ArrayList<DacqEvent> evtList)
		throws DbIoException;
	
	
	/**
	 * Delete any events for the specified platform ID.
	 * @param platformId the surrogate key of the platform
	 * @throws DbIoException on any SQL error
	 */
	public void deleteEventsForPlatform(DbKey platformId)
		throws DbIoException;

	/**
	 * Find first event ID after a given time.
	 * @param since the time
	 * @return first event ID after since time, or DbKey.NullKey if null.
	 */
	public DbKey getFirstIdAfter(Date since)
		throws DbIoException;
	
	/**
	 * Read all events with event time &gt;= the specified time.
	 * @param since the time or null to read all events
	 * @param evtList the array into which events are placed.
	 * @return the number of events placed into the passed array.
	 * @throws DbIoException
	 */
	public int readEventsAfter(Date since, ArrayList<DacqEvent> evtList)
		throws DbIoException;
	
	/**
	 * Read all events with event ID &gt; the specified ID.
	 * @param eventId the ID
	 * @param evtList the array into which events are placed.
	 * @return the number of events placed into the passed array.
	 * @throws DbIoException
	 */
	public int readEventsAfter(DbKey eventId, ArrayList<DacqEvent> evtList)
		throws DbIoException;


}
