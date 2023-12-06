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

import org.opendcs.odcsapi.beans.ApiDataSourceGroupMember;
import org.opendcs.odcsapi.beans.ApiDataSourceRef;
import org.opendcs.odcsapi.beans.ApiDataSource;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.util.ApiPropertiesUtil;

public class ApiDataSourceDAO extends ApiDaoBase
{
	public static String module = "ApiDataSourceDAO";

	public ApiDataSourceDAO(DbInterface dbi)
	{
		super(dbi, module);
	}

	public ArrayList<ApiDataSourceRef> readDataSourceRefs()
		throws DbException
	{
		ArrayList<ApiDataSourceRef> ret = new ArrayList<ApiDataSourceRef>();
		
		String q = "select ds.ID, ds.NAME, ds.DATASOURCETYPE, ds.DATASOURCEARG"
			+ " from DATASOURCE ds";

		ResultSet rs = doQuery(q);
		try
		{
			while(rs.next())
			{
				ApiDataSourceRef dsr = new ApiDataSourceRef();
				dsr.setDataSourceId(rs.getLong(1));
				dsr.setName(rs.getString(2));
				dsr.setType(rs.getString(3));
				dsr.setArguments(rs.getString(4));
				ret.add(dsr);
			}
			
			q = " select RS.DATASOURCEID, count(RS.DATASOURCEID)"
				+ " from ROUTINGSPEC RS GROUP BY RS.DATASOURCEID";
			rs = doQuery(q);
			while(rs.next())
			{
				long id = rs.getLong(1);
				int count = rs.getInt(2);
				for(ApiDataSourceRef dsr : ret)
					if (dsr.getDataSourceId() == id)
					{
						dsr.setUsedBy(count);
						break;
					}
			}
			
		}
		catch (SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
		
		return ret;
	}
	
	public Long getDataSourceId(String dsName)
		throws DbException, SQLException		
	{
		Connection conn = null;
		String q = "select ds.ID from DATASOURCE ds where ds.NAME = ?";
		ResultSet rs = doQueryPs(conn, q, dsName);
		try
		{
			if (rs.next())
				return rs.getLong(1);
			else
				return null;
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}
	
	public ApiDataSource readDataSource(long dataSourceId)
		throws DbException, SQLException
	{
		Connection conn = null;
		String q = "select ds.ID, ds.NAME, ds.DATASOURCETYPE, ds.DATASOURCEARG"
				+ " from DATASOURCE ds"
				+ " where ds.ID = ?";
		ResultSet rs = doQueryPs(conn, q, dataSourceId);
		try
		{
			ApiDataSource dds = new ApiDataSource();
			if(rs.next())
			{
				dds.setDataSourceId(rs.getLong(1));
				dds.setName(rs.getString(2));
				dds.setType(rs.getString(3));
				String args = rs.getString(4);
				if (args != null)
					dds.setProps(ApiPropertiesUtil.string2props(args));
			}
			else
				return null;
			
			q = "select count(*)"
				+ " from ROUTINGSPEC"
				+ " where DATASOURCEID = ?";
			rs = doQueryPs(conn, q, dataSourceId);
			if (rs.next())
				dds.setUsedBy(rs.getInt(1));
			
			q = " select gm.MEMBERID, ds.NAME"
					+ " from DATASOURCEGROUPMEMBER gm, DATASOURCE ds"
					+ " where gm.MEMBERID = ds.ID and gm.GROUPID = ?"
					+ " order by gm.SEQUENCENUM";
			rs = doQueryPs(conn, q, dataSourceId);
			while(rs.next())
			{
				ApiDataSourceGroupMember dsgm = new ApiDataSourceGroupMember();
				dsgm.setDataSourceId(rs.getLong(1));
				dsgm.setDataSourceName(rs.getString(2));
				dds.getGroupMembers().add(dsgm);
			}
			return dds;
		}
		catch (SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}

	public ApiDataSource writedDataSource(ApiDataSource ds)
		throws DbException, WebAppException, SQLException
	{
		// Check to make sure name is unique throw WebAppException if conflict
		// I.e. check for list with same name but different key.
		ArrayList<Object> args = new ArrayList<Object>();
		String q = "select ID from DATASOURCE where lower(NAME) = ?"; 
		args.add(ds.getName().toLowerCase());
		if (ds.getDataSourceId() != null)
		{
			q = q + " and ID != ?";
			args.add(ds.getDataSourceId());
		}
		Connection conn = null;
		ResultSet rs = doQueryPs(conn, q, args.toArray());
		try
		{
			if (rs.next())
				throw new WebAppException(ErrorCodes.NOT_ALLOWED, 
					"Cannot write DataSource with name '" + ds.getName() 
					+ "' because another DataSource with id=" + rs.getLong(1) 
					+ " also has that name.");

			if (ds.getDataSourceId() == null)
			{
				ds.setDataSourceId(getKey("DATASOURCE"));
				insert(ds);
			}
			else
				update(ds);
			return ds;
		}
		catch (SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}

	/**
	* Update a pre-existing DataSource into the database.
	* @param ds the DataSource
	*/
	private void update(ApiDataSource ds)
		throws DbException
	{
		String q = "update DATASOURCE set " +
			"NAME = ?, DATASOURCETYPE = ?, DATASOURCEARG = ? where ID = ?";
		doModifyV(q, ds.getName(), ds.getType(), ApiPropertiesUtil.props2string(ds.getProps()), ds.getDataSourceId());
		writeGroupMembers(ds);
	}

	/**
	* Insert a new DataSource into the database.
	* @param ds the DataSource
	*/
	private void insert(ApiDataSource ds)
		throws DbException
	{
		Long id = getKey("DataSource");
		ds.setDataSourceId(id);
		String args = ApiPropertiesUtil.props2string(ds.getProps());
		String q = "INSERT INTO DataSource(id, name, datasourcetype, datasourcearg) "
			+ "VALUES (?, ?, ?, ?)";
		doModifyV(q, id, ds.getName(), ds.getType(), args);

		writeGroupMembers(ds);
	}

	/**
	* This inserts records into the DataSourceGroupMember table, one for
	* each member of a group-type DataSource.
	* @param ds the DataSource
	*/
	private void writeGroupMembers(ApiDataSource ds)
		throws DbException
	{
		String q = "delete from DATASOURCEGROUPMEMBER where GROUPID = ?";
		doModifyV(q, ds.getDataSourceId());
		
		for(int seq=0; seq < ds.getGroupMembers().size(); seq++)
		{
			q = "INSERT INTO DATASOURCEGROUPMEMBER(GROUPID, SEQUENCENUM, MEMBERID) VALUES (?, ?, ?)";
			doModifyV(q, ds.getDataSourceId(), seq, ds.getGroupMembers().get(seq).getDataSourceId());
		}
	}
	
	public String datasourceUsedByRs(long datasourceId)
			throws DbException
	{
		String q = "select rs.ID, rs.NAME"
			+ " from ROUTINGSPEC rs"
			+ " where rs.DATASOURCEID = ?";
		
		try
		{
			StringBuilder sb = new StringBuilder();
			int n = 0;
			Connection conn = null;
			ResultSet rs = doQueryPs(conn, q, datasourceId);
			while (rs.next())
			{
				if (n > 0)
					sb.append(", ");
				sb.append("" + rs.getLong(1) + ":" + rs.getString(2));
				n++;
			}
			
			if (n > 0)
				return sb.toString();
			else
				return null;
				
		}
		catch (SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}

	public void deleteDatasource(long id)
		throws DbException
	{
		// If this DS is a member of a group, or if it IS a group, delete the associations:
		String q = "delete from DATASOURCEGROUPMEMBER " +
				   "where GROUPID = ? or MEMBERID = ?";
		doModifyV(q, id, id);
		
		q = "DELETE FROM DataSource WHERE ID = ?";
		doModifyV(q, id);
	}
}
