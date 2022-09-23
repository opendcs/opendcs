/**
 * $Id$
 * 
 * $Log$
 * Revision 1.3  2015/01/06 16:09:33  mmaloney
 * First cut of Polling Modules
 *
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
import decodes.sql.DecodesDatabaseVersion;
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
		if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_11)
			return null;
		
		String q = "select " + ps_attrs + " from platform_status "
			+ "where platform_id = ?";
		try
		{
			return getSingleResult(q,rs->rs2ps(rs),platformId);
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
		if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_11)
			return;

		PlatformStatus existing = readPlatformStatus(platformStatus.getPlatformId());
		String q = "";
		ArrayList<Object> parameters = new ArrayList<>();
		if (existing != null)
		{
			q = "update platform_status set ";
			String sets = "";
			if (!datesEqual(platformStatus.getLastContactTime(), existing.getLastContactTime()))
			{
				sets += "last_contact_time = ?";
				parameters.add(platformStatus.getLastContactTime());
			}
			if (!datesEqual(platformStatus.getLastMessageTime(), existing.getLastMessageTime()))
			{
				if (sets.length() > 0)
					sets = sets + ", ";
				sets = sets + "last_message_time = ?";//
				parameters.add(platformStatus.getLastMessageTime());
			}
			if (!TextUtil.strEqual(platformStatus.getLastFailureCodes(), 
				existing.getLastFailureCodes()))
			{
				if (sets.length() > 0)
					sets = sets + ", ";
				sets = sets + "last_failure_codes = ?";
				parameters.add(NullableParameter.of(platformStatus.getLastFailureCodes(),String.class));
			}
			if (!datesEqual(platformStatus.getLastErrorTime(), existing.getLastErrorTime()))
			{
				if (sets.length() > 0)
					sets = sets + ", ";
				sets = sets + 
					"last_error_time = ?";
				parameters.add(platformStatus.getLastErrorTime());
			}
			if (!platformStatus.getLastScheduleEntryStatusId().equals(
				existing.getLastScheduleEntryStatusId()))
			{
				if (sets.length() > 0)
					sets = sets + ", ";
				sets = sets + "last_schedule_entry_status_id = ?";
				parameters.add(platformStatus.getLastScheduleEntryStatusId());
			}
			if (!TextUtil.strEqual(platformStatus.getAnnotation(), 
				existing.getAnnotation()))
			{
				if (sets.length() > 0)
					sets = sets + ", ";
				sets = sets + 
					"annotation = ?";
				parameters.add(NullableParameter.of(platformStatus.getAnnotation(),String.class));
			}
			if (sets.length() == 0)
				return; // No changes
			q = q + sets + " where platform_id = ?";
			parameters.add(platformStatus.getPlatformId());
		}
		else
		{

			q = "insert into platform_status(" + ps_attrs + ") values(";
			parameters.add(platformStatus.getPlatformId());
			parameters.add(NullableParameter.of(platformStatus.getLastContactTime(),Date.class));
			parameters.add(NullableParameter.of(platformStatus.getLastMessageTime(),Date.class));
			parameters.add(NullableParameter.of(platformStatus.getLastFailureCodes(),String.class));
			parameters.add(NullableParameter.of(platformStatus.getLastErrorTime(),Date.class));
			parameters.add(platformStatus.getLastScheduleEntryStatusId());
			parameters.add(NullableParameter.of(platformStatus.getAnnotation(),String.class));
			q += ")";
		}
		try
		{
			doModify(q,parameters.toArray());
		}
		catch(SQLException ex)
		{
			String msg = "Failed to update/insert platform status. Query '%s'";
			throw new DbIoException(String.format(msg,q),ex);
		}
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
		
		if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_11)
			return new ArrayList<PlatformStatus>();

		try
		{
			return (ArrayList<PlatformStatus>)getResults(q,rs->rs2ps(rs));
		}
		catch (SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			warning(msg);
			throw new DbIoException(msg,ex);
		}
	}

	@Override
	public void deletePlatformStatus(DbKey platformId) throws DbIoException
	{
		if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_11)
			return;

		String q = "delete from platform_status where platform_id = ?";
		try
		{
			doModify(q,platformId);
		}
		catch(SQLException ex)
		{
			String msg = "Error deleting platform status. Query '%s'";
			throw new DbIoException(String.format(msg,q),ex);
		}
	}

}
