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
*  
*  $Log$
*  Revision 1.3  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*/
package decodes.tsdb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.opendcs.tsdb.FailedTaskListEntry;
import org.opendcs.tsdb.TaskListEntry;

import decodes.sql.DbKey;

public class RecordRangeHandle
{
	private DbKey appId;
	private ArrayList<TaskListEntry> recNums;
	private HashSet<FailedTaskListEntry> failedRecNums;

	public RecordRangeHandle(DbKey appId)
	{
		this.appId = appId;
		recNums = new ArrayList<>();
		failedRecNums = new HashSet<>();
	}

	/** @return the application ID */
	public DbKey getAppId()
	{
		return appId;
	}

	/**
	 * Adds a record number.
	 * @param entry the record number
	 */
	public void addRecNum(TaskListEntry entry)
	{
		recNums.add(entry);
	}

	/**
	 * Returns a string containing comma-separated integer record numbers.
	 * The returned integers are also removed from the list.
	 * @param max the maximum number of integers to return. (ignored)
	 */
	public List<? extends TaskListEntry> getRecNumList(int max)
	{
		List<TaskListEntry> entries = new ArrayList<>();
		if (max <= 0)
		{
			entries.addAll(this.recNums);
			recNums.clear();
		}
		else
		{
			int x = 0;
			Iterator<? extends TaskListEntry> it = recNums.iterator();
			while (x < max && it.hasNext())
			{
				x++;
				TaskListEntry e = it.next();
				entries.add(e);
				it.remove();
			}
		}
		return entries;
	}

	/**
	 * Returns a string containing comma-separated integer failed rec-numbers.
	 * The returned integers are also removed from the list.
	 * @param max the maximum number of integers to return.
	 */
	public List<FailedTaskListEntry> getFailedRecNumList(long max)
	{
		List<FailedTaskListEntry> entries = new ArrayList<>();
		if (max <= 0)
		{
			entries.addAll(this.failedRecNums);
			failedRecNums.clear();
		}
		else
		{
			int x=0;
			for(Iterator<FailedTaskListEntry> iit = failedRecNums.iterator();
				iit.hasNext() && x < max; x++)
			{
				FailedTaskListEntry e = iit.next();
				entries.add(e);
				iit.remove();
			}
		}
		return entries;
	}


	/** @return number of record numbers in the list. */
	public int size()
	{
		return recNums.size();
	}

	/** @return list of tasklist rec #s that had failed computations. */
	public HashSet<FailedTaskListEntry> getFailedRecnums()
	{
		return failedRecNums;
	}

	/** Called after a computation fails, marks this rec# as failed. */
	public void markComputationFailed(TaskListEntry taskRecord)
	{
		recNums.remove(taskRecord);
		failedRecNums.add(new FailedTaskListEntry(taskRecord));
	}
}
