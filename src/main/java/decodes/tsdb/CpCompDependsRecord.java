/*
*  $Id$
*
*  This is open-source software written by Cove Software LLC under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  This source code is provided completely without warranty.
*  
*  $Log$
*  Revision 1.4  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*  Revision 1.3  2012/08/01 12:59:58  mmaloney
*  equals() must take an Object arg.
*
*  Revision 1.2  2012/08/01 12:46:44  mmaloney
*  CompDependsUpdater debugging.
*
*  Revision 1.1  2012/07/05 18:26:32  mmaloney
*  First checked-in versions.
*
*/
package decodes.tsdb;

import decodes.sql.DbKey;

/**
 * This class holds a single record in the CP_COMP_DEPENDS table
 * @author mmaloney Mike Maloney, Cove Software LLC
 */
public class CpCompDependsRecord
{
	private DbKey tsKey;
	private DbKey compId;
	
	public CpCompDependsRecord(DbKey tsKey, DbKey compId)
	{
		this.tsKey = tsKey;
		this.compId = compId;
	}

	public DbKey getTsKey()
	{
		return tsKey;
	}

	public DbKey getCompId()
	{
		return compId;
	}
	
	public boolean equals(Object rhso)
	{
		CpCompDependsRecord rhs = (CpCompDependsRecord)rhso;
		return this.tsKey.equals(rhs.tsKey) && this.compId.equals(rhs.compId);
	}

	public int hashCode()
	{
		return tsKey.hashCode() + compId.hashCode();
	}
}
