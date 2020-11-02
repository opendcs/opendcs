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
package opendcs.dao;

import ilex.util.TextUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import decodes.polling.DeviceStatus;
import decodes.tsdb.DbIoException;
import opendcs.dai.DeviceStatusDAI;

public class DeviceStatusDAO extends DaoBase 
	implements DeviceStatusDAI
{
	public static final String tableName = "SERIAL_PORT_STATUS";
	public static final String tableColumns = "PORT_NAME, IN_USE, LAST_USED_BY_PROC, "
		+ "LAST_USED_BY_HOST, LAST_ACTIVITY_TIME, LAST_RECEIVE_TIME, LAST_MEDIUM_ID, "
		+ "LAST_ERROR_TIME, PORT_STATUS";

	public DeviceStatusDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, "DeviceStatusDAO");
	}

	@Override
	public ArrayList<DeviceStatus> listDeviceStatuses()
		throws DbIoException
	{
		String q = "select " + tableColumns + " from " + tableName;
		ArrayList<DeviceStatus> ret = new ArrayList<DeviceStatus>();
		ResultSet rs = doQuery(q);
		try
		{
			while(rs != null && rs.next())
				ret.add(rs2devStat(rs));
		}
		catch (SQLException ex)
		{
			throw new DbIoException("listDeviceStatuses: Error in query '" + q + "': " + ex);
		}

		return ret;
	}

	@Override
	public DeviceStatus getDeviceStatus(String portName) throws DbIoException
	{
		String q = "select " + tableColumns + " from " + tableName
			+ " where lower(PORT_NAME) = " + sqlString(portName.toLowerCase());
		ResultSet rs = doQuery(q);
		try
		{
			if (rs == null || !rs.next())
				return null;
			return rs2devStat(rs);
		}
		catch (SQLException ex)
		{
			throw new DbIoException("getDeviceStatus(" + portName + ") Error in query '" + q + "': " + ex);
		}
	}

	@Override
	public void writeDeviceStatus(DeviceStatus deviceStatus) 
		throws DbIoException
	{
		DeviceStatus existing = getDeviceStatus(deviceStatus.getPortName());
		if (existing != null)
		{
			// Construct UPDATE statement for changed fields only
			StringBuilder changes = new StringBuilder();
			
			if (deviceStatus.isInUse() != existing.isInUse())
				changes.append((changes.length()>0?", " : "") 
					+ "IN_USE = " + sqlBoolean(deviceStatus.isInUse()));
			if (!TextUtil.strEqual(deviceStatus.getLastUsedByProc(), existing.getLastUsedByProc()))
				changes.append((changes.length()>0?", " : "") 
					+ "LAST_USED_BY_PROC = " + sqlString(deviceStatus.getLastUsedByProc()));
			if (!TextUtil.strEqual(deviceStatus.getLastUsedByHost(), existing.getLastUsedByHost()))
				changes.append((changes.length()>0?", " : "") 
					+ "LAST_USED_BY_HOST = " + sqlString(deviceStatus.getLastUsedByHost()));
			if (!dateEqual(deviceStatus.getLastActivityTime(), existing.getLastActivityTime()))
				changes.append((changes.length()>0?", " : "") 
					+ "LAST_ACTIVITY_TIME = " + db.sqlDate(deviceStatus.getLastActivityTime()));
			if (!dateEqual(deviceStatus.getLastReceiveTime(), existing.getLastReceiveTime()))
				changes.append((changes.length()>0?", " : "") 
					+ "LAST_RECEIVE_TIME = " + db.sqlDate(deviceStatus.getLastReceiveTime()));
			if (!TextUtil.strEqual(deviceStatus.getLastMediumId(), existing.getLastMediumId()))
				changes.append((changes.length()>0?", " : "") 
					+ "LAST_MEDIUM_ID = " + sqlString(deviceStatus.getLastMediumId()));
			if (!dateEqual(deviceStatus.getLastErrorTime(), existing.getLastErrorTime()))
				changes.append((changes.length()>0?", " : "") 
					+ "LAST_ERROR_TIME = " + db.sqlDate(deviceStatus.getLastErrorTime()));
			if (!TextUtil.strEqual(deviceStatus.getPortStatus(), existing.getPortStatus()))
				changes.append((changes.length()>0?", " : "") 
					+ "PORT_STATUS = " + sqlString(deviceStatus.getPortStatus()));
			
			if (changes.length() == 0)
				debug2("writeDeviceStatus(" + deviceStatus.getPortName() + ") -- no changes.");
			else
			{
				String q = "UPDATE " + tableName + " SET " + changes.toString()
					+ " WHERE lower(port_name) = " + sqlString(deviceStatus.getPortName().toLowerCase());
				doModify(q);
			}
		}
		else
		{
			String q = "INSERT INTO " + tableName +"(" + tableColumns + ") VALUES("
				+ sqlString(deviceStatus.getPortName())
				+ ", " + sqlBoolean(deviceStatus.isInUse())
				+ ", " + sqlString(deviceStatus.getLastUsedByProc())
				+ ", " + sqlString(deviceStatus.getLastUsedByHost())
				+ ", " + db.sqlDate(deviceStatus.getLastActivityTime())
				+ ", " + db.sqlDate(deviceStatus.getLastReceiveTime())
				+ ", " + sqlString(deviceStatus.getLastMediumId())
				+ ", " + db.sqlDate(deviceStatus.getLastErrorTime())
				+ ", " + sqlString(deviceStatus.getPortStatus())
				+ ")";
			doModify(q);
		}
	}
	
	// Compare dates allowing null
	private boolean dateEqual(Date d1, Date d2)
	{
		if (d1 == null)
			return d2 == null;
		else if (d2 == null)
			return false;
		else
			return d1.equals(d2);
	}
	
	private DeviceStatus rs2devStat(ResultSet rs)
		throws SQLException
	{
		DeviceStatus ret = new DeviceStatus(rs.getString(1));
		ret.setInUse(TextUtil.str2boolean(rs.getString(2)));
		ret.setLastUsedByProc(rs.getString(3));
		ret.setLastUsedByHost(rs.getString(4));
		ret.setLastActivityTime(db.getFullDate(rs, 5));
		ret.setLastReceiveTime(db.getFullDate(rs, 6));
		ret.setLastMediumId(rs.getString(7));
		ret.setLastErrorTime(db.getFullDate(rs, 8));
		ret.setPortStatus(rs.getString(9));
		return ret;
	}

	@Override
	public void close()
	{
		super.close();
	}


}
