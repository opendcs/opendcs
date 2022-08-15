package opendcs.opentsdb.hydrojson.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import opendcs.dao.DaoBase;
import opendcs.dao.DatabaseConnectionOwner;
import opendcs.opentsdb.hydrojson.ErrorCodes;
import opendcs.opentsdb.hydrojson.beans.NetList;
import opendcs.opentsdb.hydrojson.beans.NetListItem;
import opendcs.opentsdb.hydrojson.beans.NetlistRef;
import opendcs.opentsdb.hydrojson.errorhandling.WebAppException;

public class NetlistDAO
	extends DaoBase
{
	public NetlistDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb,"NetlistDAO");
	}
	
	public List<NetlistRef> readNetlistRefs(String tmtype)
		throws DbIoException
	{
		ArrayList<NetlistRef> ret = new ArrayList<NetlistRef>();
		
		String q = "select ID, NAME, TRANSPORTMEDIUMTYPE, SITENAMETYPEPREFERENCE, LASTMODIFYTIME"
				+ " from NETWORKLIST";
		if (tmtype != null)
		{
			// "goes" should get goes, goes-self-timed, and goes-random.
			tmtype = tmtype.toLowerCase();
			String qtmt = tmtype.equals("goes")
				? sqlString("goes") + "," + sqlString("goes-self-timed") + "," + sqlString("goes-random")
				: sqlString(tmtype);				
			q = q + " where lower(TRANSPORTMEDIUMTYPE) IN (" + qtmt + ")";
		}
		q = q + " order by ID";
System.out.println(q);
		ResultSet rs = doQuery(q);
		String action = "reading TRANSPORTMEDIUM";
		try
		{
			while(rs.next())
			{
				NetlistRef nls = new NetlistRef();
				nls.setNetlistId(rs.getLong(1));
				nls.setName(rs.getString(2));
				nls.setTransportMediumType(rs.getString(3));
				nls.setSiteNameTypePref(rs.getString(4));
				nls.setLastModifyTime(db.getFullDate(rs, 5));
				ret.add(nls);
			}
			
			action = "counting platforms";
			for(NetlistRef nls : ret)
			{
				q = "select count(*) from networklistentry where NETWORKLISTID = " + nls.getNetlistId();
				rs = doQuery(q);
				while(rs.next())
				{
					nls.setNumPlatforms(rs.getInt(1));
				}
			}
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new DbIoException(module + " Error while " + action + ": " + e);
		}
		
		return ret;
	}

	public NetList readNetworkList(long netlistId)
		throws DbIoException
	{
		String q = "select ID, NAME, TRANSPORTMEDIUMTYPE, SITENAMETYPEPREFERENCE, LASTMODIFYTIME"
				+ " from NETWORKLIST";
		q = q + " where ID = " + netlistId;
		
		try
		{
			ResultSet rs = doQuery(q);
			NetList ret = new NetList();
			if (rs.next())
			{
				ret.setNetlistId(rs.getLong(1));
				ret.setName(rs.getString(2));
				ret.setTransportMediumType(rs.getString(3));
				ret.setSiteNameTypePref(rs.getString(4));
				ret.setLastModifyTime(db.getFullDate(rs, 5));
				
				q = "select TRANSPORTID, PLATFORM_NAME, DESCRIPTION from NETWORKLISTENTRY"
					+ " where NETWORKLISTID = " + netlistId
					+ " order by TRANSPORTID";
				rs = doQuery(q);

				while(rs.next())
				{
					String tmid = rs.getString(1);
					ret.getItems().put(tmid, 
						new NetListItem(tmid, rs.getString(2), rs.getString(3)));
				}
				return ret;
			}
			else
				return null;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			throw new DbIoException(module + " Error while reading network list: " + e);
		}
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
		throws DbIoException
	{
		String q = "select rs.ID, rs.NAME"
			+ " from ROUTINGSPECNETWORKLIST rsnl, ROUTINGSPEC rs, NETWORKLIST nl"
			+ " where rsnl.ROUTINGSPECID = rs.ID"
			+ " and lower(rsnl.NETWORKLISTNAME) = lower(nl.NAME)"
			+ " and nl.ID = " + netlistId;
		try
		{
			StringBuilder sb = new StringBuilder();
			int n = 0;
			for(ResultSet rs = doQuery(q); rs.next(); n++)
			{
				if (n > 0)
					sb.append(", ");
				sb.append("" + rs.getLong(1) + ":" + rs.getString(2));
			}
			if (n > 0)
				return sb.toString();
			else
				return null;
				
		}
		catch (SQLException ex)
		{
			ex.printStackTrace();
			throw new DbIoException(module + " Error in query '" + q + "': " + ex);
		}
	}
	
	public void deleteNetlist(long netlistId)
		throws DbIoException
	{
		String q = "delete from NETWORKLISTENTRY where NETWORKLISTID = " + netlistId;
		doModify(q);
		q = "delete from NETWORKLIST where ID = " + netlistId;
		doModify(q);
	}
	
	/**
	 * Make sure there is no name clash with a different NL. Throw if there is.
	 * @param nl
	 * @throws WebAppException
	 */
	public void checkNameClash(NetList nl)
		throws DbIoException, WebAppException
	{
		// Check to make sure name is unique throw WebAppException if conflict
		// I.e. check for list with same name but different key.
		String q = "select ID from NETWORKLIST where lower(NAME) = " 
			+ sqlString(nl.getName().toLowerCase());
		if (nl.getNetlistId() != DbKey.NullKey.getValue())
			q = q + " and ID != " + nl.getNetlistId();
System.out.println(module + ".checkNameClash: Check for dup name: " + q);
		ResultSet rs = doQuery(q);
		try
		{
			if (rs.next())
				throw new WebAppException(ErrorCodes.NOT_ALLOWED, 
					"Cannot write NetworkList with name '" + nl.getName() 
					+ "' because another NetworkList with id=" + rs.getLong(1) 
					+ " also has that name.");
		}
		catch(SQLException ex)
		{
			throw new DbIoException(module + ".checkNameClash error in query '" + q + "': " + ex);
		}
	}
	

}
