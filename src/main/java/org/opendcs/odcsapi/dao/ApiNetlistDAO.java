package org.opendcs.odcsapi.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.opendcs.odcsapi.beans.ApiNetList;
import org.opendcs.odcsapi.beans.ApiNetListItem;
import org.opendcs.odcsapi.beans.ApiNetlistRef;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;

public class ApiNetlistDAO
	extends ApiDaoBase
{
	public ApiNetlistDAO(DbInterface dbi)
	{
		super(dbi,"ApiNetlistDAO");
	}
	
	public List<ApiNetlistRef> readNetlistRefs(String tmtype)
		throws DbException, SQLException
	{
		ArrayList<ApiNetlistRef> ret = new ArrayList<ApiNetlistRef>();
		
		String q = "select ID, NAME, TRANSPORTMEDIUMTYPE, SITENAMETYPEPREFERENCE, LASTMODIFYTIME"
			+ " from NETWORKLIST";
		ArrayList<Object> args = new ArrayList<Object>();
		if (tmtype != null)
		{
			// tmtype, if provided, must be a single string with no embedded spaces & no spec chars.
			String tmtypeModded = getSingleWord(tmtype).toLowerCase();
			
			String qtmt = "";
			
			if (tmtypeModded.contentEquals("goes"))
			{
				qtmt = "?, ?, ?";
				args.add("goes");
				args.add("goes-self-times");
				args.add("goes-random");
			}
			else
			{
				qtmt = "?";
				args.add("goes");
			}
			q = q + " where lower(TRANSPORTMEDIUMTYPE) IN (" + qtmt + ")";
		}
		q = q + " order by ID";
		Connection conn = null;
		ResultSet rs = doQueryPs(conn, q, args.toArray());
		String action = "reading NETWORKLIST";
		try
		{
			while(rs.next())
			{
				ApiNetlistRef nls = new ApiNetlistRef();
				nls.setNetlistId(rs.getLong(1));
				nls.setName(rs.getString(2));
				nls.setTransportMediumType(rs.getString(3));
				nls.setSiteNameTypePref(rs.getString(4));
				nls.setLastModifyTime(dbi.getFullDate(rs, 5));
				ret.add(nls);
			}
			
			action = "counting platforms";
			for(ApiNetlistRef nls : ret)
			{
				q = "select count(*) from networklistentry where NETWORKLISTID = ?";
				rs = doQueryPs(conn, q, nls.getNetlistId());
				while(rs.next())
				{
					nls.setNumPlatforms(rs.getInt(1));
				}
			}
			
		}
		catch (Exception ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
		
		return ret;
	}

	public ApiNetList readNetworkList(long netlistId)
		throws DbException
	{
		String q = "select ID, NAME, TRANSPORTMEDIUMTYPE, SITENAMETYPEPREFERENCE, LASTMODIFYTIME"
				+ " from NETWORKLIST where ID = ?";
		
		try
		{
			Connection conn = null;
			ResultSet rs = doQueryPs(conn, q, netlistId);
			ApiNetList ret = new ApiNetList();
			if (rs.next())
			{
				ret.setNetlistId(rs.getLong(1));
				ret.setName(rs.getString(2));
				ret.setTransportMediumType(rs.getString(3));
				ret.setSiteNameTypePref(rs.getString(4));
				ret.setLastModifyTime(dbi.getFullDate(rs, 5));
				
				q = "select TRANSPORTID, PLATFORM_NAME, DESCRIPTION from NETWORKLISTENTRY"
					+ " where NETWORKLISTID = ?"
					+ " order by TRANSPORTID";
				rs = doQueryPs(conn, q, netlistId);

				while(rs.next())
				{
					String tmid = rs.getString(1);
					ret.getItems().put(tmid, 
						new ApiNetListItem(tmid, rs.getString(2), rs.getString(3)));
				}
				return ret;
			}
			else
				return null;
		}
		catch (SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}
	
	public Long getNetlistId(String nlname)
		throws DbException
	{
		String q = "select ID from NETWORKLIST where lower(NAME) = ?";
		class LH { Long id = null; };
		final LH lh = new LH();
		Connection conn = null;
		doQueryV(conn, q,
			new ResultSetConsumer()
			{
				@Override
				public void accept(ResultSet rs) throws SQLException
				{
					if (rs.next())
					{
						lh.id = rs.getLong(1);
						if (rs.wasNull())
							lh.id = null;
					}
				}
			
			}, nlname);
		return lh.id;
	}
	
	public void writeNetlist(ApiNetList netlist)
		throws DbException, WebAppException, SQLException
	{
		// Check to make sure name is unique throw WebAppException if conflict
		// I.e. check for list with same name but different key.
		ArrayList<Object> args = new ArrayList<Object>();
		String q = "select ID from NETWORKLIST where lower(NAME) = ?"; 
		args.add(netlist.getName().toLowerCase());
		if (netlist.getNetlistId() != null)
		{
			q = q + " and ID != ?";
			args.add(netlist.getNetlistId());
		}
		netlist.setLastModifyTime(new Date());
		Connection conn = null;
		ResultSet rs = doQueryPs(conn, q, args.toArray());
		try
		{
			if (rs.next())
				throw new WebAppException(ErrorCodes.NOT_ALLOWED, 
					"Cannot write networklist with name '" + netlist.getName() 
					+ "' because another networklist with id=" + rs.getLong(1) 
					+ " also has that name.");
			
			if (netlist.getNetlistId() == null)
				insert(netlist);
			else
				update(netlist);
		}
		catch (SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}

	private void update(ApiNetList netlist)
		throws DbException
	{
		String q = "update NETWORKLIST set "
					+ "NAME = ?, TRANSPORTMEDIUMTYPE = ?, SITENAMETYPEPREFERENCE = ?, LASTMODIFYTIME = ? where ID = ?";
		doModifyV(q, netlist.getName(), netlist.getTransportMediumType(), netlist.getSiteNameTypePref(), dbi.sqlDateV(netlist.getLastModifyTime()), netlist.getNetlistId());
		q = "delete from NETWORKLISTENTRY where NETWORKLISTID = ?";
		doModifyV(q, netlist.getNetlistId());
		for(ApiNetListItem ani : netlist.getItems().values())
			insertItem(netlist.getNetlistId(), ani);
	}

	private void insert(ApiNetList netlist)
		throws DbException
	{
		netlist.setNetlistId(getKey("NETWORKLIST"));
		String q = "insert into NETWORKLIST(ID, NAME, TRANSPORTMEDIUMTYPE, "
			+ "SITENAMETYPEPREFERENCE, LASTMODIFYTIME) values (?, ?, ?, ?, ?)";

		doModifyV(q, netlist.getNetlistId(), netlist.getName(), netlist.getTransportMediumType(), 
				netlist.getSiteNameTypePref(), dbi.sqlDateV(netlist.getLastModifyTime()));
		for(ApiNetListItem ani : netlist.getItems().values())
			insertItem(netlist.getNetlistId(), ani);
	}

	private void insertItem(Long netlistId, ApiNetListItem ani)
		throws DbException
	{
		String q = "insert into NETWORKLISTENTRY(NETWORKLISTID, TRANSPORTID, "
				+ "PLATFORM_NAME, DESCRIPTION) values (?, ?, ?, ?)";

		doModifyV(q, netlistId, ani.getTransportId(), ani.getPlatformName(), ani.getDescription());
	}

	/**
	 * If the past netlist ID is used by any routing specs, return a String with
	 * a displayable list of them.
	 * If it is not, return null;
	 * @param netlistId
	 * @return
	 * @throws DbIoException
	 */
	public String netlistUsedByRs(long netlistId)
		throws DbException
	{
		String q = "select rs.ID, rs.NAME"
			+ " from ROUTINGSPECNETWORKLIST rsnl, ROUTINGSPEC rs, NETWORKLIST nl"
			+ " where rsnl.ROUTINGSPECID = rs.ID"
			+ " and lower(rsnl.NETWORKLISTNAME) = lower(nl.NAME)"
			+ " and nl.ID = ?";
		try
		{
			Connection conn = null;
			StringBuilder sb = new StringBuilder();
			int n = 0;
			ResultSet rs = doQueryPs(conn, q, netlistId);
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
	
	public void deleteNetlist(long netlistId)
		throws DbException
	{
		String q = "delete from NETWORKLISTENTRY where NETWORKLISTID = ?";
		doModifyV(q, netlistId);
		q = "delete from NETWORKLIST where ID = ?";
		doModifyV(q, netlistId);
	}
}
