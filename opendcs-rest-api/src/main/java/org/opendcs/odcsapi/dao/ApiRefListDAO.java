/*
 *  Copyright 2025 OpenDCS Consortium and its Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.opendcs.odcsapi.beans.ApiInterval;
import org.opendcs.odcsapi.hydrojson.DbInterface;

public class ApiRefListDAO extends ApiDaoBase
{
	public ApiRefListDAO(DbInterface dbi)
	{
		super(dbi, "ApiRefListDAO");
	}
	
	public HashMap<Long, ApiInterval> getIntervals()
		throws DbException
	{
		HashMap<Long, ApiInterval> ret = new HashMap<Long, ApiInterval>();
		
		String q = "select INTERVAL_ID, NAME, CAL_CONSTANT, CAL_MULTIPLIER from INTERVAL_CODE";
		try
		{
			ResultSet rs = doQuery(q);
			while(rs.next())
			{
				Long id = rs.getLong(1);
				ret.put(id, new ApiInterval(id, rs.getString(2), rs.getString(3), rs.getInt(4)));
			}
			return ret;
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}
	
	public ApiInterval writeInterval(ApiInterval intv)
		throws DbException
	{
		if (intv.getIntervalId() != null)
		{
			// update
			String q = "update INTERVAL_CODE set NAME = ?, CAL_CONSTANT = ?, CAL_MULTIPLIER = ?"
				+ " where INTERVAL_ID = ?";
			doModifyV(q, intv.getName(), intv.getCalConstant(), (Integer)intv.getCalMultilier(), intv.getIntervalId());
		}
		else
		{
			//insert
			String q = "insert into INTERVAL_CODE(INTERVAL_ID, NAME, CAL_CONSTANT, CAL_MULTIPLIER)"
				+ " values(?,?,?,?)";
			intv.setIntervalId(getKey(DbInterface.Sequences.INTERVAL_CODE));
			doModifyV(q, intv.getIntervalId(), intv.getName(), intv.getCalConstant(), 
				(Integer)intv.getCalMultilier());
		}
		return intv;	
	}
	
	public void deleteInterval(Long id)
		throws DbException
	{
		String q = " delete from INTERVAL_CODE where INTERVAL_ID = ?";
		doModifyV(q, id);
	}
}
