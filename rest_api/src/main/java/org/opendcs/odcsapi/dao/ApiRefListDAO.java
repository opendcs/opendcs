package org.opendcs.odcsapi.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimeZone;

import org.opendcs.odcsapi.beans.ApiInterval;
import org.opendcs.odcsapi.beans.ApiRefList;
import org.opendcs.odcsapi.beans.ApiRefListItem;
import org.opendcs.odcsapi.beans.ApiSeason;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;

public class ApiRefListDAO extends ApiDaoBase
{
	private String q = "";
	
	public ApiRefListDAO(DbInterface dbi)
	{
		super(dbi, "ApiRefListDAO");
	}

	class rliwrap 
	{
		Long rlid = null; ApiRefListItem rli = null;
		rliwrap(Long rlid, ApiRefListItem rli)
		{
			this.rlid = rlid;
			this.rli = rli;
		}
	}
	
	public HashMap<String, ApiRefList> getAllRefLists()
		throws DbException
	{
		HashMap<String, ApiRefList> ret = new HashMap<String, ApiRefList>();
		HashMap<Long, ApiRefList> tmplist = new HashMap<Long, ApiRefList>();
		
		q = "select ID, NAME, DEFAULTVALUE, DESCRIPTION from ENUM "
				+ "where lower(NAME) != 'season'";
		try
		{
			ResultSet rs = doQuery(q);
			while(rs.next())
			{
				ApiRefList rl = rs2ApiRefList(rs);
				ret.put(rl.getEnumName(), rl);
				tmplist.put(rl.getReflistId(), rl);
			}
			
			q = "select ENUMID, ENUMVALUE, DESCRIPTION, EXECCLASS, EDITCLASS, SORTNUMBER "
				+ "from ENUMVALUE";
			rs = doQuery(q);
			while(rs.next())
			{
				rliwrap w = rs2ApiRefListItem(rs);
				ApiRefList rl = tmplist.get(w.rlid);
				if (rl != null)
					rl.getItems().put(w.rli.getValue(), w.rli);
			}
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
		
		return ret;
	}
	
	private ApiRefList rs2ApiRefList(ResultSet rs)
		throws SQLException
	{
		// Assume that next() has already been called.
		ApiRefList rl = new ApiRefList();
		rl.setReflistId(rs.getLong(1));
		rl.setEnumName(rs.getString(2));
		rl.setDefaultValue(rs.getString(3));
		rl.setDescription(rs.getString(4));
		
		return rl;
	}
	
	private rliwrap rs2ApiRefListItem(ResultSet rs)
		throws SQLException
	{
		// assume next() has already been called.
		ApiRefListItem rli = new ApiRefListItem();
		Long eid = rs.getLong(1);
		rli.setValue(rs.getString(2));
		rli.setDescription(rs.getString(3));
		rli.setExecClassName(rs.getString(4));
		rli.setEditClassName(rs.getString(5));
		rli.setSortNumber(rs.getInt(6));
		if (rs.wasNull())
			rli.setSortNumber(null);
		return new rliwrap(eid, rli);
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
			intv.setIntervalId(getKey("INTERVAL_CODE"));
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
	
	private void insertItem(Long enumId, ApiRefListItem item)
		throws DbException
	{
		String q = "delete from ENUMVALUE where ENUMID = ?"
			+ " and lower(ENUMVALUE) = ?";
		doModifyV(q, enumId, item.getValue().toLowerCase());
		
		q = "insert into ENUMVALUE(ENUMID, ENUMVALUE, DESCRIPTION, EXECCLASS, EDITCLASS, SORTNUMBER)"
			+ " values(?, ?, ?, ?, ?, ?)";
		doModifyV(q, enumId, item.getValue(), item.getDescription(), item.getExecClassName(),
			item.getEditClassName(), item.getSortNumber());
	}
	
	public ApiRefList getRefList(Long id)
		throws DbException
	{
		q = "select ID, NAME, DEFAULTVALUE, DESCRIPTION from ENUM where ID = ?";
		try
		{
			ApiRefList rl = null;

			Connection conn = null;
			ResultSet rs = doQueryPs(conn, q, id);
			if (rs.next())
				rl = rs2ApiRefList(rs);
			else
				return null;
			
			q = "select ENUMID, ENUMVALUE, DESCRIPTION, EXECCLASS, EDITCLASS, SORTNUMBER "
				+ "from ENUMVALUE where ENUMID = ?";
			rs = doQueryPs(conn, q, id);
			while (rs.next())
			{
				rliwrap w = rs2ApiRefListItem(rs);
				rl.getItems().put(w.rli.getValue(), w.rli);
			}
			return rl;
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}

	
	public ApiRefList writeRefList(final ApiRefList arl)
		throws DbException
	{
		String q = "";
		if (arl.getReflistId() == null)
		{
			// No ID in passed object, try to map name to ID.
			q = "select ID from ENUM where NAME = ?";
			arl.setReflistId(this.lookupId(q, arl.getEnumName()));
			if (arl.getReflistId() == null)
				// Still no ID, assume this is a new ENUM.
				arl.setReflistId(getKey("ENUM"));
		}
		ApiRefList oldlist = getRefList(arl.getReflistId());
		if (oldlist == null)
		{
			// This is a num ENUM. Just do INSERTS.
			q = "insert into ENUM(ID, NAME, DEFAULTVALUE, DESCRIPTION) values (?,?,?,?)";
			doModifyV(q, arl.getReflistId(), arl.getEnumName(), 
				arl.getDefaultValue(), arl.getDescription());
			for(ApiRefListItem item : arl.getItems().values())
				insertItem(arl.getReflistId(), item);
			return arl;
		}
		
		// Else there is an existing list with this ID.
		q = "update ENUM set NAME = ?, DEFAULTVALUE = ?, DESCRIPTION = ? "
			+ "where ID = ?";
		doModifyV(q, arl.getEnumName(), arl.getDefaultValue(), 
			arl.getDescription(), arl.getReflistId());
		q = "delete from ENUMVALUE where ENUMID = ?";
		doModifyV(q, arl.getReflistId());
		for(ApiRefListItem item : arl.getItems().values())
			insertItem(arl.getReflistId(), item);
		return arl;
	}

	public void deleteRefList(Long reflistId) 
		throws DbException
	{
		doModifyV("delete from ENUMVALUE where ENUMID = ?", reflistId);
		doModifyV("delete from ENUM where ID = ?", reflistId);
	}

	public ArrayList<ApiSeason> getAllSeasons()
		throws DbException, WebAppException
	{
		Long seasonRlId = getSeasonReflistId();
		try
		{
			ArrayList<ApiSeason> ret = new ArrayList<ApiSeason>();
			
			q = "select ENUMVALUE, DESCRIPTION, EDITCLASS, SORTNUMBER "
				+ "from ENUMVALUE "
				+ "where ENUMID = ?"
				+ " order by EDITCLASS";
			Connection conn = null;
			ResultSet rs = doQueryPs(conn, q, seasonRlId);
			while(rs.next())
			{
				ApiSeason season = new ApiSeason();
				season.setAbbr(rs.getString(1));
				season.setName(rs.getString(2));
				String s = rs.getString(3);
				Integer sn = rs.getInt(4);
				if (!rs.wasNull())
					season.setSortNumber(sn);
				
				// EDIT class contains start end tz. Dates are in the form MM/dd-HH:mm
				String s_e_tz[] = s.split(" ");
				if (s_e_tz.length < 1)
					continue;
				season.setStart(s_e_tz[0]);
				if (s_e_tz.length >= 2)
					season.setEnd(s_e_tz[1]);
				if (s_e_tz.length >= 3)
					season.setTz(s_e_tz[2]);
				ret.add(season);
			}
			return ret;
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}

	}

	public ApiSeason getSeason(String abbr) 
		throws DbException, WebAppException
	{
		Long seasonRlId = getSeasonReflistId();
		final ApiSeason ret = new ApiSeason();
		
		String abbrLower = abbr.toLowerCase();
		q = "select ENUMVALUE, DESCRIPTION, EDITCLASS, SORTNUMBER "
			+ "from ENUMVALUE "
			+ "where ENUMID = ? and lower(ENUMVALUE) = ?";
		Connection conn = null;
		doQueryV(conn, q, 
			new ResultSetConsumer()
			{
				@Override
				public void accept(ResultSet rs) throws SQLException
				{
					if (rs.next())
					{
						ret.setAbbr(rs.getString(1));
						ret.setName(rs.getString(2));
						Integer sn = rs.getInt(4);
						if (!rs.wasNull())
							ret.setSortNumber(sn);

						String s = rs.getString(3);
						// EDIT class contains start end tz. Dates are in the form MM/dd-HH:mm
						String s_e_tz[] = s.split(" ");
						if (s_e_tz.length >= 1)
							ret.setStart(s_e_tz[0]);
						if (s_e_tz.length >= 2)
							ret.setEnd(s_e_tz[1]);
						if (s_e_tz.length >= 3)
							ret.setTz(s_e_tz[2]);
					}
				}
			
			}, seasonRlId, abbrLower);
		if (ret.getAbbr() == null)
		{
			throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT,
				"No such season with abbr '" + abbr + "'.");
		}
		return ret;
	}
	
	public void writeSeason(ApiSeason season)
		throws DbException, WebAppException
	{
		Long seasonRlId = null;
		try { seasonRlId = getSeasonReflistId(); }
		catch(WebAppException ex)
		{
			// This can only mean that there is no "season" enumeration. Create it.
			seasonRlId = getKey("ENUM");
			
			String q = "insert into ENUM(ID, NAME, DEFAULTVALUE, DESCRIPTION)"
					+ " values(?, ?, ?, ?)";
			doModifyV(q, seasonRlId, "Season", null, "Seasons for conditional processing");
		}
		
		String fromAbbr = season.getFromabbr();
		System.out.println("From ABBR Found: |" + fromAbbr + "|");
		ApiSeason fromExisting = null;
		if (fromAbbr != null)
		{
			try { fromExisting = getSeason(fromAbbr); }
			catch(WebAppException ex)
			{
				System.out.println("NO FROM ABBR!");
				if (ex.getStatus() != ErrorCodes.NO_SUCH_OBJECT)
					throw ex;
				fromExisting = null;
			}
		}
		
		ApiSeason existing = null;
		try { existing = getSeason(season.getAbbr()); }
		catch(WebAppException ex)
		{
			if (ex.getStatus() != ErrorCodes.NO_SUCH_OBJECT)
				throw ex;
			existing = null;
		}
		
		String s_e_tz = "" + season.getStart() + " " + season.getEnd();
		if (season.getTz() != null)
			s_e_tz = s_e_tz + " " + season.getTz();
		
		String q = null;
		if (fromExisting != null)
		{
			//Can't change an existing one to another existing one.  Throw an error.
			if (existing != null)
			{
				throw new WebAppException(ErrorCodes.BAD_CONFIG,
						"Cannot update season from '" + fromAbbr + "' to '" + season.getAbbr() + "'.  The season '" + season.getAbbr() + "' already exists.");
			}
			System.out.println("Updating from existing.");
			q = "update ENUMVALUE set ENUMVALUE = ?, DESCRIPTION = ?, EDITCLASS = ?, SORTNUMBER = ? "
					+ "where ENUMID = ? and lower(ENUMVALUE) = ?";
				doModifyV(q, season.getAbbr(), season.getName(), s_e_tz, season.getSortNumber(),
					seasonRlId, 
					fromAbbr.toLowerCase());
		}
		else if (fromAbbr == null && existing == null) // new season?
		{
			q = "insert into ENUMVALUE(ENUMID, ENUMVALUE, DESCRIPTION, EDITCLASS, SORTNUMBER)"
				+ " values(?,?,?,?,?)";
			doModifyV(q, seasonRlId, season.getAbbr(), season.getName(), s_e_tz, season.getSortNumber());
		}
		else if (fromAbbr == null)
		{
			q = "update ENUMVALUE set ENUMVALUE = ?, DESCRIPTION = ?, EDITCLASS = ?, SORTNUMBER = ? "
				+ "where ENUMID = ? and lower(ENUMVALUE) = ?";
			doModifyV(q, season.getAbbr(), season.getName(), s_e_tz, season.getSortNumber(),
				seasonRlId, 
				season.getAbbr().toLowerCase());
		}
		else
		{
			throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT,
					"No such season with abbr '" + fromAbbr + "'.");
		}
	}
	
	public void deleteSeason(String abbr)
		throws DbException, WebAppException
	{
		Long seasonRlId = getSeasonReflistId();
		String q = "delete from ENUMVALUE where ENUMID = ? and lower(ENUMVALUE) = ?";
		doModifyV(q, seasonRlId, abbr.toLowerCase());
	}
	
	private Long getSeasonReflistId()
		throws WebAppException, DbException
	{
		q = "select ID from ENUM "
			+ "where lower(NAME) = ?";
		try
		{
			Connection conn = null;
			ResultSet rs = doQueryPs(conn, q, "season");
			if (!rs.next())
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT,
					"Database is missing the 'Season' enumeration.");
			
			return rs.getLong(1);
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}
}
