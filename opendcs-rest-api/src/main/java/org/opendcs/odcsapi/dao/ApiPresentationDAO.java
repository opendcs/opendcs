/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import org.opendcs.odcsapi.beans.ApiPresentationElement;
import org.opendcs.odcsapi.beans.ApiPresentationGroup;
import org.opendcs.odcsapi.beans.ApiPresentationRef;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.util.ApiTextUtil;

public class ApiPresentationDAO
	extends ApiDaoBase
{
	public static String module = "ApiPresentationDAO";

	public ApiPresentationDAO(DbInterface dbi)
	{
		super(dbi, module);
	}

	public ArrayList<ApiPresentationRef> getPresentationRefs()
		throws DbException
	{
		ArrayList<ApiPresentationRef> ret = new ArrayList<ApiPresentationRef>();
		
		String q = "select ID, NAME, INHERITSFROM, LASTMODIFYTIME, ISPRODUCTION"
				+ " from PRESENTATIONGROUP order by ID";
		ResultSet rs = doQuery(q);
		try
		{
			while (rs.next())
			{
				ApiPresentationRef ref = new ApiPresentationRef();
				ref.setGroupId(rs.getLong(1));
				ref.setName(rs.getString(2));
				long x = rs.getLong(3);
				if (!rs.wasNull())
					ref.setInheritsFromId(x);
				ref.setLastModified(dbi.getFullDate(rs, 4));
				ref.setProduction(ApiTextUtil.str2boolean(rs.getString(5)));
				ret.add(ref);
			}
			
			for(ApiPresentationRef ref : ret)
			{
				if (ref.getInheritsFromId() != null)
					for(ApiPresentationRef r2 : ret)
					{
						if (ref.getInheritsFromId() == r2.getGroupId())
						{
							ref.setInheritsFrom(r2.getName());
							break;
						}
					}
			}
			return ret;
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}
	
	public ApiPresentationGroup getPresentation(long id)
		throws DbException, WebAppException, SQLException
	{
		String q = "select ID, NAME, INHERITSFROM, LASTMODIFYTIME, ISPRODUCTION"
				+ " from PRESENTATIONGROUP where ID = ?";
		Connection conn = null;
		ResultSet rs = doQueryPs(conn, q, id);
		try
		{
			if (!rs.next())
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, "No PresentationGroup with ID=" + id);
			
			ApiPresentationGroup ret = new ApiPresentationGroup();
			ret.setGroupId(rs.getLong(1));
			ret.setName(rs.getString(2));
			long x = rs.getLong(3);
			if (!rs.wasNull())
				ret.setInheritsFromId(x);
			ret.setLastModified(dbi.getFullDate(rs, 4));
			ret.setProduction(ApiTextUtil.str2boolean(rs.getString(5)));

			if (ret.getInheritsFromId() != null)
			{
				q = "select NAME from PRESENTATIONGROUP where ID = ?";
				rs = doQueryPs(conn, q, ret.getInheritsFromId());
				if (rs.next())
					ret.setInheritsFrom(rs.getString(1));
			}
			
			q = "select dt.STANDARD, dt.CODE, dp.UNITABBR, dp.MAXDECIMALS, dp.MAX_VALUE, dp.MIN_VALUE"
				+ " from DATATYPE dt, DATAPRESENTATION dp"
				+ " where dp.GROUPID = ?"
				+ " and dp.DATATYPEID = dt.ID"
				+ " order by dt.STANDARD, dt.CODE";
			rs = doQueryPs(conn, q, ret.getGroupId());
			while(rs.next())
			{
				ApiPresentationElement dpe = new ApiPresentationElement();
				dpe.setDataTypeStd(rs.getString(1));
				dpe.setDataTypeCode(rs.getString(2));
				dpe.setUnits(rs.getString(3));
				dpe.setFractionalDigits(rs.getInt(4));
				Double d = rs.getDouble(5);
				if (!rs.wasNull())
					dpe.setMax(d);
				d = rs.getDouble(6);
				if (!rs.wasNull())
					dpe.setMin(d);
				ret.getElements().add(dpe);
			}
			return ret;

		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}

	public void writePresentation(ApiPresentationGroup presGrp)
		throws DbException, WebAppException, SQLException
	{
		// Check to make sure name is unique throw WebAppException if conflict
		// I.e. check for list with same name but different key.
		ArrayList<Object> args = new ArrayList<Object>();
		String q = "select ID from PRESENTATIONGROUP where lower(NAME) = ?"; 
		//+ sqlString(presGrp.getName().toLowerCase());
		args.add(presGrp.getName().toLowerCase());
		if (presGrp.getGroupId() != null)
		{
			q = q + " and ID != ?";
			args.add(presGrp.getGroupId());
		}
		Connection conn = null;
		ResultSet rs = doQueryPs(conn, q, args.toArray());
		try
		{
			if (rs.next())
				throw new WebAppException(ErrorCodes.NOT_ALLOWED, 
					"Cannot write PresentationGroup with name '" + presGrp.getName() 
					+ "' because another group with id=" + rs.getLong(1) 
					+ " also has that name.");
			presGrp.setLastModified(new Date());
			if (presGrp.getGroupId() == null)
				insert(presGrp);
			else
				update(presGrp);
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}

	private void update(ApiPresentationGroup presGrp)
		throws DbException, SQLException
	{
		String q = "update PRESENTATIONGROUP set" +
			" NAME = ?, INHERITSFROM = ?, LASTMODIFYTIME = ?, ISPRODUCTION = ? where ID = ?";
		doModifyV(q, presGrp.getName(), presGrp.getInheritsFromId(), 
				dbi.sqlDateV(presGrp.getLastModified()), sqlBoolean(presGrp.isProduction()),
						presGrp.getGroupId());
		deleteSubordinates(presGrp.getGroupId());
		writeSubordinates(presGrp);
	}

	private void insert(ApiPresentationGroup presGrp)
		throws DbException, SQLException
	{
		presGrp.setGroupId(getKey(DbInterface.Sequences.PRESENTATIONGROUP));

		String q = "insert into PRESENTATIONGROUP(ID, NAME, INHERITSFROM, LASTMODIFYTIME, ISPRODUCTION)"
			+ " values(?, ?, ?, ?, ?)";
		
		doModifyV(q, presGrp.getGroupId(), presGrp.getName(), presGrp.getInheritsFromId(),
				dbi.sqlDateV(presGrp.getLastModified()), sqlBoolean(presGrp.isProduction()));
		
		deleteSubordinates(presGrp.getGroupId());
		writeSubordinates(presGrp);
	}

	private void writeSubordinates(ApiPresentationGroup presGrp)
		throws DbException, SQLException
	{
		try (ApiDataTypeDAO dtDao = new ApiDataTypeDAO(dbi))
		{
			for(ApiPresentationElement dpe : presGrp.getElements())
			{
				Long dtId = dtDao.lookup(dpe.getDataTypeStd(), dpe.getDataTypeCode());
				if (dtId == null)
					dtId = dtDao.create(dpe.getDataTypeStd(), dpe.getDataTypeCode());
				long id = this.getKey(DbInterface.Sequences.DATAPRESENTATION);
				String q = "insert into DATAPRESENTATION(ID, GROUPID, DATATYPEID, UNITABBR, "
					+ "MAXDECIMALS, MAX_VALUE, MIN_VALUE) values (?, ?, ?, ?, ?, ?, ?)";

				doModifyV(q, id, presGrp.getGroupId(), dtId, dpe.getUnits(),
						dpe.getFractionalDigits(), dpe.getMax(), dpe.getMin());
			}
		}
	}

	private void deleteSubordinates(long groupId)
		throws DbException
	{
		String q = "delete from DATAPRESENTATION where GROUPID = ?";
		doModifyV(q, groupId);
	}
	
	public void deletePresentation(long groupId)
		throws DbException
	{
		deleteSubordinates(groupId);
		String q = "delete from PRESENTATIONGROUP where ID = ?";
		doModifyV(q, groupId);
	}
	
	/**
	 * If the presentation group referened by groupId is used by one or more routing
	 * specs, return a list of routing spec IDs and names. If groupId is not used,
	 * return null.
	 * @param groupId
	 * @return
	 * @throws SQLException 
	 */
	public String routSpecsUsing(long groupId)
		throws DbException, SQLException
	{
		String q = "select rs.ID, rs.NAME"
				+ " from ROUTINGSPEC rs, PRESENTATIONGROUP pg"
				+ " where lower(rs.PRESENTATIONGROUPNAME) = lower(pg.NAME)"
				+ " AND pg.ID = ?";
		
		StringBuilder sb = new StringBuilder();
		Connection conn = null;
		ResultSet rs = doQueryPs(conn, q, groupId);
		try
		{
			while(rs.next())
			{
				if (sb.length() > 0)
					sb.append(", ");
				sb.append(rs.getLong(1) + ":" + rs.getString(2));
			}
			return sb.length() == 0 ? null : sb.toString();
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}
}
