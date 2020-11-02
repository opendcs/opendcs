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

import decodes.sql.DbKey;

public class RecordRangeHandle
{
	private DbKey appId;
	private ArrayList<Integer> recnums;
	private HashSet<Integer> failed_recnums;

	public RecordRangeHandle(DbKey appId)
	{
		this.appId = appId;
		recnums = new ArrayList<Integer>();
		failed_recnums = new HashSet<Integer>();
	}

	/** @return the application ID */
	public DbKey getAppId() { return appId; }

	/**
	 * Adds a record number.
	 * @param recnum the record number
	 */
	public void addRecNum(int recnum) { recnums.add(new Integer(recnum)); }

	/**
	 * Returns a string containing comma-separated integer record numbers.
	 * The returned integers are also removed from the list.
	 * @param max the maximum number of integers to return.
	 */
	public String getRecNumList(int max)
	{
		StringBuilder sb = new StringBuilder();
		int n = recnums.size();
		int x=0;
		for(; x<max && x<n; x++)
		{
			if (x > 0)
				sb.append(", ");
			sb.append(recnums.get(x).toString());
		}
		if (x > 0)
		{
			ArrayList<Integer> oldrn = recnums;
			recnums = new ArrayList<Integer>();
			for(; x<n; x++)
				recnums.add(oldrn.get(x));
		}
		return sb.toString();
	}

	/**
	 * Returns a string containing comma-separated integer failed rec-numbers.
	 * The returned integers are also removed from the list.
	 * @param max the maximum number of integers to return.
	 */
	public String getFailedRecNumList(int max)
	{
		StringBuilder sb = new StringBuilder();
		int x=0;
		for(Iterator<Integer> iit = failed_recnums.iterator(); 
			iit.hasNext() && x < max; x++)
		{
			if (x > 0)
				sb.append(", ");
			Integer rn = iit.next();
			sb.append(rn.toString());
			iit.remove();
		}
		return sb.toString();
	}


	/** @return number of record numbers in the list. */
	public int size() { return recnums.size(); }

	/** @return list of tasklist rec #s that had failed computations. */
	public HashSet<Integer> getFailedRecnums() { return failed_recnums; }

	/** Called after a computation fails, marks this rec# as failed. */
	public void markComputationFailed(Integer recnum)
	{
		recnums.remove(recnum);
		failed_recnums.add(recnum);
	}
}
