package org.opendcs.odcsapi.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import org.opendcs.odcsapi.beans.ApiPlatform;
import org.opendcs.odcsapi.beans.ApiPlatformSensor;
import org.opendcs.odcsapi.beans.ApiPlatformStatus;
import org.opendcs.odcsapi.beans.ApiSiteName;
import org.opendcs.odcsapi.beans.ApiTransportMedium;
import org.opendcs.odcsapi.beans.ApiPlatformRef;
import org.opendcs.odcsapi.beans.ApiSiteRef;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiPropertiesUtil;
import org.opendcs.odcsapi.util.ApiTextUtil;

import java.util.logging.Logger;

public class ApiPlatformDAO
	extends ApiDaoBase
{
	public static String module = "PlatformDAO";
	
	private String q = "";

	public ApiPlatformDAO(DbInterface dbi)
	{
		super(dbi, module);
	}
	
	
	public ArrayList<ApiPlatformRef> getPlatformRefs(String tmtype)
		throws DbException, SQLException
	{
		ArrayList<ApiPlatformRef> ret = new ArrayList<ApiPlatformRef>();
		
		q = "select ID, AGENCY, SITEID, CONFIGID, DESCRIPTION, PLATFORMDESIGNATOR"
			+ " from PLATFORM";
		ArrayList<Object> args = new ArrayList<Object>();
		if (tmtype != null)
		{
			// tmtype, if provided, must be a single string with no embedded spaces.
			tmtype = getSingleWord(tmtype);

			// Note: "goes" matches goes, goes-self-timed or goes-random
			tmtype = tmtype.toLowerCase();
			if (tmtype.equals("goes"))
			{
				tmtype = "?, ?, ?";
				args.add("goes");
				args.add("goes-self-timed");
				args.add("goes-random");
			}
			else
			{
				tmtype = "?";
				args.add(tmtype);
			}
	
			q = q + " where exists(select PLATFORMID from TRANSPORTMEDIUM where lower(MEDIUMTYPE) IN (?) and PLATFORM.ID = PLATFORMID)";
			args.add(tmtype);
		}
		q = q + " order by ID";
		Connection conn = null;
		ResultSet rs = doQueryPs(conn, q, args.toArray());
		String action = "reading PLATFORM";
		try (ApiSiteDAO siteDAO = new ApiSiteDAO(dbi))
		{
			while(rs.next())
			{
				ApiPlatformRef ps = new ApiPlatformRef();
				ps.setPlatformId(rs.getLong(1));
				ps.setAgency(rs.getString(2));
				ps.setSiteId(rs.getLong(3));
				if (rs.wasNull())
					ps.setSiteId(null);
				ps.setConfigId(rs.getLong(4));
				if (rs.wasNull())
					ps.setConfigId(null);
				ps.setDescription(rs.getString(5));
				ps.setDesignator(rs.getString(6));
				
				ret.add(ps);
			}
			
			action = "reading SITE";
			Collection<ApiSiteRef> siteRefs = siteDAO.getSiteRefs();
			
			for (ApiPlatformRef ps : ret)
			{
				String pref = null;
				for (ApiSiteRef siteRef : siteRefs)
					if (ps.getSiteId() == siteRef.getSiteId())
					{
						for(String t : siteRef.getSitenames().keySet())
							if (t.equalsIgnoreCase(DbInterface.siteNameTypePreference))
							{
								pref = siteRef.getSitenames().get(t);
								break;
							}
						if (pref == null)
						{
							for(String x : siteRef.getSitenames().values())
							{
								pref = x;
								break;
							}
						}
						if (pref == null) pref = "unknownSite";
						break;
					}
				if (pref != null)
				{
					String platname = pref;
					if (ps.getDesignator() != null && ps.getDesignator().length() > 0)
						platname = platname + "-" + ps.getDesignator();
					ps.setName(platname);
				}
				else // no siteid association, use platform ID as name
					ps.setName("ID=" + ps.getPlatformId());			}
			
			action = "reading TRANSPORTMEDIUM";
			q = "select PLATFORMID, MEDIUMTYPE, MEDIUMID from TRANSPORTMEDIUM";
			q = q + " order by PLATFORMID, MEDIUMTYPE";
			rs = doQuery(q);
			long lastId = -1; // NOTE: Invalid key
			ApiPlatformRef testSpec = new ApiPlatformRef();
			Comparator<ApiPlatformRef> keyTest = new Comparator<ApiPlatformRef>()
				{
					@Override
					public int compare(ApiPlatformRef o1, ApiPlatformRef o2)
					{
						long x = o1.getPlatformId() - o2.getPlatformId();
						return x > 0 ? 1 : x < 0 ? -1 : 0;
					}
			
				};
			while(rs.next())
			{
				long platformId = rs.getLong(1);
				if (platformId == lastId)
					continue;
				testSpec.setPlatformId(platformId);
				int idx = Collections.binarySearch(ret, testSpec, keyTest);
				if (idx < 0)
				{
					continue;
				}

				ret.get(idx).getTransportMedia().setProperty(rs.getString(2), rs.getString(3));
			}
			
			q = "select p.ID, pc.NAME from PLATFORM p, PLATFORMCONFIG pc "
				+ "where p.CONFIGID = pc.ID";
			rs = doQuery(q);
			while(rs.next())
			{
				long platformId = rs.getLong(1);
				String configName = rs.getString(2);
				testSpec.setPlatformId(platformId);
				int idx = Collections.binarySearch(ret, testSpec, keyTest);
				if (idx < 0)
				{
					continue;
				}
				ret.get(idx).setConfig(configName);
			}
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
		
		return ret;
	}
	
	public ApiPlatform readPlatform(long platformId)
		throws DbException, WebAppException, SQLException

	{
		q = "select AGENCY, ISPRODUCTION, SITEID, CONFIGID, DESCRIPTION, LASTMODIFYTIME,"
			+ " PLATFORMDESIGNATOR from PLATFORM where ID = ?";
		Connection conn = null;
		ResultSet rs = doQueryPs(conn, q, platformId);
		try (ApiSiteDAO siteDAO = new ApiSiteDAO(dbi))
		{
			if (!rs.next())
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, "No such platform with id="
					+ platformId);
			ApiPlatform ret = new ApiPlatform();
			ret.setPlatformId(platformId);
			ret.setAgency(rs.getString(1));
			boolean b = rs.getBoolean(2);
			if (!rs.wasNull())
				ret.setProduction(b);
			long l = rs.getLong(3);
			if (!rs.wasNull())
				ret.setSiteId(l);
			l = rs.getLong(4);
			if (!rs.wasNull())
				ret.setConfigId(l);
			ret.setDescription(rs.getString(5));
			ret.setLastModified(dbi.getFullDate(rs, 6));
			ret.setDesignator(rs.getString(7));
			
			q = "select PROP_NAME, PROP_VALUE from PLATFORMPROPERTY"
				+ " where PLATFORMID = ?";
			rs = doQueryPs(conn, q, platformId);
			while(rs.next())
				ret.getProperties().setProperty(rs.getString(1), rs.getString(2));
			
			q = "select SENSORNUMBER, SITEID, DD_NU from PLATFORMSENSOR"
				+ " where PLATFORMID = ?";
			rs = doQueryPs(conn, q, platformId);
			while(rs.next())
			{
				ApiPlatformSensor dps = new ApiPlatformSensor();
				dps.setSensorNum(rs.getInt(1));
				l = rs.getLong(2);
				if (!rs.wasNull())
					dps.setActualSiteId(l);
				int i = rs.getInt(3);
				if (!rs.wasNull())
					dps.setUsgsDdno(i);
				ret.getPlatformSensors().add(dps);
			}
			
			q = "select SENSORNUMBER, PROP_NAME, PROP_VALUE from PLATFORMSENSORPROPERTY"
				+ " where PLATFORMID = ? order by SENSORNUMBER, PROP_NAME";
			rs = doQueryPs(conn, q, platformId);
		  next_prop:
			while(rs.next())
			{
				int sn = rs.getInt(1);
				for (ApiPlatformSensor dps : ret.getPlatformSensors())
					if (dps.getSensorNum() == sn)
					{
						String n = rs.getString(2);
						String v = rs.getString(3);
						if (n.equalsIgnoreCase("min"))
						{
							try { dps.setMin(Double.parseDouble(v.trim())); }
							catch(Exception ex)
							{
								Logger.getLogger(ApiConstants.loggerName).warning(module + ".readPlatform() "
									+ "PLATFORMSENSORPROPERTY(platformId=" + platformId
									+ ", sensor#=" + sn + ", n=" + n + " has non-numeric value '"
									+ v + "' -- ignored.");
							}
						}
						else if (n.equalsIgnoreCase("max"))
						{
							try { dps.setMax(Double.parseDouble(v.trim())); }
							catch(Exception ex)
							{
								Logger.getLogger(ApiConstants.loggerName).warning(module + ".readPlatform() "
									+ "PLATFORMSENSORPROPERTY(platformId=" + platformId
									+ ", sensor#=" + sn + ", n=" + n + " has non-numeric value '"
									+ v + "' -- ignored.");
							}
						}
						else
							dps.getSensorProps().setProperty(n, v);
						
						continue next_prop;
					}
				// Fell through - rogue property with no matching sensor.
				String m = "delete from PLATFORMSENSORPROPERTY where PLATFORMID = ? and SENSORNUMBER = ?";
				doModifyV(m, platformId, sn);
			}
			
			q = "select MEDIUMTYPE, MEDIUMID, SCRIPTNAME, CHANNELNUM, ASSIGNEDTIME,"
				+ " TRANSMITWINDOW, TRANSMITINTERVAL, TIMEADJUSTMENT, "
				+ " TIMEZONE, LOGGERTYPE, BAUD, STOPBITS, PARITY, DATABITS, DOLOGIN,"
				+ " USERNAME, PASSWORD"
				+ " from TRANSPORTMEDIUM where PLATFORMID = ?";
			rs = doQueryPs(conn, q, platformId);
			while(rs.next())
			{
				ApiTransportMedium dtm = new ApiTransportMedium();
				dtm.setMediumType(rs.getString(1));
				dtm.setMediumId(rs.getString(2));
				dtm.setScriptName(rs.getString(3));
				int i = rs.getInt(4);
				if (!rs.wasNull())
					dtm.setChannelNum(i);
				i = rs.getInt(5);
				if (!rs.wasNull())
					dtm.setAssignedTime(i);
				i = rs.getInt(6);
				if (!rs.wasNull())
					dtm.setTransportWindow(i);
				i = rs.getInt(7);
				if (!rs.wasNull())
					dtm.setTransportInterval(i);
				i = rs.getInt(8);
				if (!rs.wasNull())
					dtm.setTimeAdjustment(i);
				dtm.setTimezone(rs.getString(9));
				dtm.setLoggerType(rs.getString(10));
				i = rs.getInt(11);
				if (!rs.wasNull())
					dtm.setBaud(i);
				i = rs.getInt(12);
				if (!rs.wasNull())
					dtm.setStopBits(i);
				dtm.setParity(rs.getString(13));
				i = rs.getInt(14);
				if (!rs.wasNull())
					dtm.setDataBits(i);
				String s = rs.getString(15);
				if (s != null)
					dtm.setDoLogin(ApiTextUtil.str2boolean(s));
				dtm.setUsername(rs.getString(16));
				dtm.setPassword(rs.getString(17));
				
				ret.getTransportMedia().add(dtm);
			}
			
			
			ArrayList<ApiSiteName> siteNames = siteDAO.getSiteNames(ret.getSiteId());
			String pref = null;
			for (ApiSiteName sn : siteNames)
				if (sn.getNameType().equalsIgnoreCase(DbInterface.siteNameTypePreference))
				{
					pref = sn.getNameValue();
					break;
				}
			if (pref == null)
			{
				if (siteNames.size() > 0)
					pref = siteNames.get(0).getNameValue();
				else
					pref = "unknownSite";
			}
			
			if (ret.getDesignator() != null && ret.getDesignator().length() > 0)
				pref = pref + "-" + ret.getDesignator();
			ret.setName(pref);
			
			return ret;

		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}
	
	
	/**
	 * Call with platform.id == DbKey.nullKey if this is a new platform. ID will be assigned
	 * in the returned object. For existing platforms, call with valid key.
	 * LMT in returned object will be updated.
	 * @param platform
	 * @return
	 * @throws DbIoException
	 * @throws WebAppException
	 * @throws SQLException 
	 */
	public ApiPlatform writePlatform(ApiPlatform platform)
		throws DbException, WebAppException, SQLException
	{
		// Check for null site. Cannot write platform without site assignment
		if (platform.getSiteId() == null)
			throw new WebAppException(ErrorCodes.NOT_ALLOWED,
				module + " Cannot write platform without site assignment.");
		
		ArrayList<Object> args = new ArrayList<Object>();
		// Check that Site + Designator is unique
		q = "select ID from PLATFORM where SITEID = ?";
		args.add(platform.getSiteId());
		if (platform.getDesignator() != null && platform.getDesignator().length() == 0)
		{
			platform.setDesignator(null);
		}
		if (platform.getDesignator() == null)
		{
			q = q + " and PLATFORMDESIGNATOR is null";
		}
		else
		{
			q = q + " and lower(PLATFORMDESIGNATOR) = ?";
			args.add(platform.getDesignator().toLowerCase());
		}
		if (platform.getPlatformId() != null)
		{
			q = q + " and ID ! = ?";
			args.add(platform.getPlatformId());
		}
		
		Connection conn = null;
		ResultSet rs = doQueryPs(conn, q, args.toArray());
		try
		{
			if (rs.next())
				throw new WebAppException(ErrorCodes.NOT_ALLOWED, 
					"Cannot write Platform '" + platform.getName() 
					+ "' because another PLATFORM with id=" + rs.getLong(1) 
					+ " already has that the same siteID (" + platform.getSiteId() 
					+ ") and designator (" + platform.getDesignator() + ")");
			
			// Check for TM clash
			for (ApiTransportMedium tm : platform.getTransportMedia())
			{
				args = new ArrayList<Object>();
				q = "select PLATFORMID from TRANSPORTMEDIUM"
					+ " where lower(MEDIUMTYPE) = ?"
					+ " and lower(MEDIUMID) = ?";
				args.add(tm.getMediumType().toLowerCase());
				args.add(tm.getMediumId().toLowerCase());
				if (platform.getPlatformId() != null)
				{
					q = q + " and PLATFORMID != ?";
					args.add(platform.getPlatformId());
				}
				rs = doQueryPs(conn, q, args.toArray());
				if (rs.next())
					throw new WebAppException(ErrorCodes.NOT_ALLOWED, 
						"Cannot write Platform '" + platform.getName() 
						+ "' because another PLATFORM with id=" + rs.getLong(1) 
						+ " already has a transport medium " + tm.getMediumType() + ":" + tm.getMediumId());
			}
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}

		platform.setLastModified(new Date());
		if (platform.getPlatformId() == null)
			insert(platform);
		else
			update(platform);
		return platform;
	}
	
	private void insert(ApiPlatform platform)
		throws DbException, WebAppException
	{
		platform.setPlatformId(getKey("PLATFORM"));
		q = "insert into PLATFORM(ID, AGENCY, ISPRODUCTION, SITEID, CONFIGID, DESCRIPTION"
				+ ", LASTMODIFYTIME, PLATFORMDESIGNATOR)"
			+ " values (?, ?, ?, ?, ?, ?, ?, ?)";
		doModifyV(q, platform.getPlatformId(), platform.getAgency(), platform.isProduction(), platform.getSiteId(),
				platform.getConfigId(), platform.getDescription(), dbi.sqlDateV(platform.getLastModified()), platform.getDesignator());
		deleteSubordinates(platform.getPlatformId());
		writeSubordinates(platform);
	}
	
	private void update(ApiPlatform platform)
		throws DbException, WebAppException
	{
		q = "update PLATFORM set " +
			"AGENCY = ?, ISPRODUCTION = ?, SITEID = ?, CONFIGID = ?, DESCRIPTION = ?, " +
			"LASTMODIFYTIME = ?, PLATFORMDESIGNATOR = ?  where ID = ?";
		doModifyV(q, platform.getAgency(), platform.isProduction(), platform.getSiteId(), platform.getConfigId(), platform.getDescription(),
				dbi.sqlDateV(platform.getLastModified()), platform.getDesignator(), platform.getPlatformId()
				);
		deleteSubordinates(platform.getPlatformId());
		writeSubordinates(platform);
	}
	
	private void deleteSubordinates(long platformId)
		throws DbException
	{
		q = "delete from PLATFORMPROPERTY where PLATFORMID = ?";
		doModifyV(q, platformId);
		
		q = "delete from PLATFORMSENSORPROPERTY where PLATFORMID = ?";
		doModifyV(q, platformId);

		q = "delete from PLATFORMSENSOR where PLATFORMID = ?";
		doModifyV(q, platformId);
		
		q = "delete from TRANSPORTMEDIUM where PLATFORMID = ?";
		doModifyV(q, platformId);
		
	}
	
	private void writeSubordinates(ApiPlatform platform)
		throws DbException, WebAppException
	{
		
		for(Object k : platform.getProperties().keySet())
		{
			q = "insert into PLATFORMPROPERTY(PLATFORMID, PROP_NAME, PROP_VALUE)"
				+ " values(?, ?, ?)";
			doModifyV(q, platform.getPlatformId(), (String)k, platform.getProperties().getProperty((String)k));
		}


		for(ApiPlatformSensor dps : platform.getPlatformSensors())
		{
			q = "insert into PLATFORMSENSOR(PLATFORMID, SENSORNUMBER, SITEID, DD_NU) "
			+ "values(?, ?, ?, ?)";
			doModifyV(q, platform.getPlatformId(), dps.getSensorNum(), dps.getActualSiteId(), dps.getUsgsDdno());
			
			Properties props = new Properties();
			ApiPropertiesUtil.copyProps(props, dps.getSensorProps());
			if (dps.getMin() != null)
				props.setProperty("min", ""+dps.getMin());
			if (dps.getMax() != null)
				props.setProperty("max", ""+dps.getMax());
			
			for(Object k : props.keySet())
			{
				q = "insert into PLATFORMSENSORPROPERTY(PLATFORMID, SENSORNUMBER, PROP_NAME, PROP_VALUE)"
					+ " values(?, ?, ?, ?)";
				doModifyV(q, platform.getPlatformId(), dps.getSensorNum(), (String)k, props.getProperty((String)k));
			}
		}
		
		for(ApiTransportMedium dtm : platform.getTransportMedia())
		{
			q = "insert into TRANSPORTMEDIUM(PLATFORMID, MEDIUMTYPE, MEDIUMID, SCRIPTNAME, CHANNELNUM, "
				+ "ASSIGNEDTIME, TRANSMITWINDOW, TRANSMITINTERVAL, TIMEADJUSTMENT, "
				+ "TIMEZONE, LOGGERTYPE, BAUD, STOPBITS, PARITY, DATABITS, DOLOGIN, "
				+ "USERNAME, PASSWORD) "
				+ "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			doModifyV(q, platform.getPlatformId(), dtm.getMediumType(), dtm.getMediumId(), dtm.getScriptName(), dtm.getChannelNum(),
					dtm.getAssignedTime(), dtm.getTransportWindow(), dtm.getTransportInterval(), dtm.getTimeAdjustment(),
					dtm.getTimezone(), dtm.getLoggerType(), dtm.getBaud(), dtm.getStopBits(), dtm.getParity(),
					dtm.getDataBits(), sqlBoolean(dtm.getDoLogin()),dtm.getUsername(),dtm.getPassword());
		}
	}

	public void deletePlatform(long platformId)
		throws DbException, WebAppException
	{
		deleteSubordinates(platformId);
		q = "delete from PLATFORM where ID = ?";
		doModifyV(q, platformId);
	}
	
	public String platformName2transportId(String name)
		throws DbException, SQLException
	{
		int hyphen = name.lastIndexOf('-');
		String desig = null;
		if (hyphen > 0 && name.length() > hyphen)
		{
			desig = name.substring(hyphen+1);
			name = name.substring(0, hyphen);
		}
		
		Long siteId = null;
		try (ApiSiteDAO siteDAO = new ApiSiteDAO(dbi))
		{
			siteId = siteDAO.name2id(name);
		}
		if (siteId == null)
			return null;

		String q = "select tm.MEDIUMTYPE, tm.MEDIUMID "
			+ "from TRANSPORTMEDIUM tm, PLATFORM p "
			+ "where tm.PLATFORMID = p.id "
			+ "and p.SITEID = ?"
			+ " and p.PLATFORMDESIGNATOR ?";
		String desigValue = desig != null ? ("= " + this.getSingleWord(desig)) : "is null";
		Connection conn = null;
		ResultSet rs = doQueryPs(conn, q, siteId, desigValue);
		String ret = null;
		try
		{
			while(rs.next())
			{
				String t = rs.getString(1);
				String id = rs.getString(2);
				if (ret == null
				 || t.toLowerCase().startsWith("goes"))
				{
					ret = id;
				}
			}
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
		return ret;
	}
	
	public ArrayList<ApiPlatformStatus> getPlatformStatus(Long netlistId)
		throws DbException, SQLException
	{
		ArrayList<ApiPlatformStatus> ret = new ArrayList<ApiPlatformStatus>();
		
		ArrayList<ApiPlatformRef> platrefs = getPlatformRefs(null);
		HashMap<Long, ApiPlatformRef> platId2ref = new HashMap<Long, ApiPlatformRef>();
		for(ApiPlatformRef pr : platrefs)
			platId2ref.put(pr.getPlatformId(), pr);

		ArrayList<Object> args = new ArrayList<Object>();
		String q = "select distinct ps.PLATFORM_ID, ps.LAST_CONTACT_TIME, ps.LAST_MESSAGE_TIME,"
			+ "	ps.LAST_FAILURE_CODES, ps.LAST_ERROR_TIME, ps.LAST_SCHEDULE_ENTRY_STATUS_ID,"
			+ "	ps.ANNOTATION, rs.name"
			+ "	from PLATFORM_STATUS ps, SCHEDULE_ENTRY_STATUS ses, SCHEDULE_ENTRY se,"
			+ "		ROUTINGSPEC rs"
			+ "	where ps.LAST_SCHEDULE_ENTRY_STATUS_ID = ses.SCHEDULE_ENTRY_STATUS_ID"
			+ "		and ses.SCHEDULE_ENTRY_ID = se.SCHEDULE_ENTRY_ID"
			+ "		and se.ROUTINGSPEC_ID = rs.ID";
		if (netlistId != null)
		{
			q = "select distinct ps.PLATFORM_ID, ps.LAST_CONTACT_TIME, ps.LAST_MESSAGE_TIME,"
			  + " ps.LAST_FAILURE_CODES, ps.LAST_ERROR_TIME, ps.LAST_SCHEDULE_ENTRY_STATUS_ID,"
			  + " ps.ANNOTATION, rs.name"
			  + " from PLATFORM_STATUS ps, SCHEDULE_ENTRY_STATUS ses, SCHEDULE_ENTRY se,"
			  + "      ROUTINGSPEC rs, TRANSPORTMEDIUM tm, NETWORKLISTENTRY nle"
			  + " where ps.LAST_SCHEDULE_ENTRY_STATUS_ID = ses.SCHEDULE_ENTRY_STATUS_ID"
			  + "      and ses.SCHEDULE_ENTRY_ID = se.SCHEDULE_ENTRY_ID"
			  + "      and se.ROUTINGSPEC_ID = rs.ID"
			  + "      and tm.PLATFORMID = ps.PLATFORM_ID"
			  + "      and lower(nle.TRANSPORTID) = lower(tm.MEDIUMID)"
			  + "      and nle.NETWORKLISTID = ?";
			  args.add(netlistId);
		}
		
		try
		{
			Connection conn = null;
			ResultSet rs = doQueryPs(conn, q, args.toArray());
			while(rs.next())
			{
				ApiPlatformStatus ps = new ApiPlatformStatus();
				long platId = rs.getLong(1);
				ps.setPlatformId(platId);
				long x = rs.getLong(2);
				if (!rs.wasNull())
					ps.setLastContact(new Date(x));
				x = rs.getLong(3);
				if (!rs.wasNull())
					ps.setLastMessage(new Date(x));
				ps.setLastMsgQuality(rs.getString(4));
				x = rs.getLong(5);
				if (!rs.wasNull())
					ps.setLastError(new Date(x));
				ps.setLastRoutingExecId(rs.getLong(6));
				ps.setAnnotation(rs.getString(7));
				ps.setRoutingSpecName(rs.getString(8));
				
				ApiPlatformRef pr = platId2ref.get(ps.getPlatformId());
				if (pr != null)
				{
					ps.setPlatformName(pr.getName());
					ps.setSiteId(pr.getSiteId());
				}
				ret.add(ps);
			}
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
		
		return ret;
	}
}
