/**
 * $Id$
 * 
 * $Log$
 * Revision 1.2  2014/07/03 12:53:41  mmaloney
 * debug improvements.
 *
 * 
 * This software was written by Cove Software, LLC ("COVE") under contract
 * to the United States Government. No warranty is provided or implied other 
 * than specific contractual terms between COVE and the U.S. Government.
 *
 * Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
 */
package opendcs.dao;

import ilex.util.TextUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import decodes.db.PlatformStatus;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import opendcs.dai.PlatformStatusDAI;

public class PlatformStatusDAO 
	extends DaoBase 
	implements PlatformStatusDAI
{
	private String ps_attrs = "platform_id, last_contact_time, last_message_time, " +
			"last_failure_codes, last_error_time, last_schedule_entry_status_id, " +
			"annotation";
	
	public PlatformStatusDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, "PlatformStatusDAO");
	}

	@Override
	public synchronized PlatformStatus readPlatformStatus(DbKey platformId)
		throws DbIoException
	{
		String q = "select " + ps_attrs + " from platform_status "
			+ "where platform_id = " + platformId;
		
		ResultSet rs = doQuery(q);
		try
		{
			if (rs != null && rs.next())
				return rs2ps(rs);
		}
		catch (SQLException ex)
		{
			String msg = "Cannot parse rs for '" + q + "': " + ex;
			warning(msg);
		}
		return null;
	}
	
	private PlatformStatus rs2ps(ResultSet rs)
		throws SQLException
	{
		PlatformStatus ps = new PlatformStatus(DbKey.createDbKey(rs, 1));
		ps.setLastContactTime(db.getFullDate(rs, 2));
		ps.setLastMessageTime(db.getFullDate(rs, 3));
		ps.setLastFailureCodes(rs.getString(4));
		ps.setLastErrorTime(db.getFullDate(rs, 5));
		ps.setLastScheduleEntryStatusId(DbKey.createDbKey(rs, 6));
		ps.setAnnotation(rs.getString(7));
		
		return ps;
	}

	@Override
	public synchronized void writePlatformStatus(PlatformStatus platformStatus) 
		throws DbIoException
	{
		PlatformStatus existing = readPlatformStatus(platformStatus.getPlatformId());
		String q = "";
		if (existing != null)
		{
			q = "update platform_status set ";
			String sets = "";
			if (!datesEqual(platformStatus.getLastContactTime(), existing.getLastContactTime()))
				sets = "last_contact_time = " + db.sqlDate(platformStatus.getLastContactTime());
			if (!datesEqual(platformStatus.getLastMessageTime(), existing.getLastMessageTime()))
			{
				if (sets.length() > 0)
					sets = sets + ", ";
				sets = sets + 
					"last_message_time = " + db.sqlDate(platformStatus.getLastMessageTime());
			}
			if (!TextUtil.strEqual(platformStatus.getLastFailureCodes(), 
				existing.getLastFailureCodes()))
			{
				if (sets.length() > 0)
					sets = sets + ", ";
				sets = sets + 
					"last_failure_codes = " + sqlString(platformStatus.getLastFailureCodes());
			}
			if (!datesEqual(platformStatus.getLastErrorTime(), existing.getLastErrorTime()))
			{
				if (sets.length() > 0)
					sets = sets + ", ";
				sets = sets + 
					"last_error_time = " + db.sqlDate(platformStatus.getLastErrorTime());
			}
			if (!platformStatus.getLastScheduleEntryStatusId().equals(
				existing.getLastScheduleEntryStatusId()))
			{
				if (sets.length() > 0)
					sets = sets + ", ";
				sets = sets + "last_schedule_entry_status_id = " 
					+ platformStatus.getLastScheduleEntryStatusId();
			}
			if (!TextUtil.strEqual(platformStatus.getAnnotation(), 
				existing.getAnnotation()))
			{
				if (sets.length() > 0)
					sets = sets + ", ";
				sets = sets + 
					"annotation = " + sqlString(platformStatus.getAnnotation());
			}
			if (sets.length() == 0)
				return; // No changes
			q = q + sets + " where platform_id = " + platformStatus.getPlatformId();
		}
		else
		{
			q = "insert into platform_status(" + ps_attrs + ") values("
				+ platformStatus.getPlatformId() + ", "
				+ db.sqlDate(platformStatus.getLastContactTime()) + ", "
				+ db.sqlDate(platformStatus.getLastMessageTime()) + ", "
				+ sqlString(platformStatus.getLastFailureCodes()) + ", "
				+ db.sqlDate(platformStatus.getLastErrorTime()) + ", "
				+ platformStatus.getLastScheduleEntryStatusId() + ", "
				+ sqlString(platformStatus.getAnnotation())
				+ ")";
		}
		doModify(q);
	}
	
	private boolean datesEqual(Date d1, Date d2)
	{
		if (d1 != null)
			return d2 != null ? d1.equals(d2) : false;
		else
			return d2 == null;
	}

	@Override
	public ArrayList<PlatformStatus> listPlatformStatus() throws DbIoException
	{
		String q = "select " + ps_attrs + " from platform_status";
		// For CWMS, join with TransportMedium so we get the VPD filtering
		// by db_office_code.
		if (db.isCwms())
			q = q + " where platform_id in (select distinct a.id from platform a, "
			+ "transportmedium b where a.id = b.platformid)";

		ArrayList<PlatformStatus> ret = new ArrayList<PlatformStatus>();
		ResultSet rs = doQuery(q);
		try
		{
			while (rs != null && rs.next())
				ret.add(rs2ps(rs));
		}
		catch (SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			warning(msg);
		}

		return ret;
	}

	@Override
	public void deletePlatformStatus(DbKey platformId) throws DbIoException
	{
		String q = "delete from platform_status "
			+ "where platform_id = " + platformId;
		doModify(q);
	}

}
