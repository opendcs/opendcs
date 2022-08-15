package opendcs.opentsdb.hydrojson.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Properties;

import decodes.db.Site;
import decodes.db.SiteList;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.StringPair;
import ilex.util.TextUtil;
import opendcs.dai.SiteDAI;
import opendcs.dao.DaoBase;
import opendcs.dao.DatabaseConnectionOwner;
import opendcs.opentsdb.hydrojson.beans.DecodesPlatform;
import opendcs.opentsdb.hydrojson.beans.DecodesPlatformSensor;
import opendcs.opentsdb.hydrojson.beans.DecodesTransportMedium;
import opendcs.opentsdb.hydrojson.ErrorCodes;
import opendcs.opentsdb.hydrojson.beans.PlatformRef;
import opendcs.opentsdb.hydrojson.errorhandling.WebAppException;

public class PlatformDAO
	extends DaoBase
{
	public static String module = "PlatformDAO";

	public PlatformDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, module);
	}
	
	
	public ArrayList<PlatformRef> readPlatformSpecs(String tmtype)
		throws DbIoException
	{
		ArrayList<PlatformRef> ret = new ArrayList<PlatformRef>();
		
		String q = "select ID, AGENCY, SITEID, CONFIGID, DESCRIPTION, PLATFORMDESIGNATOR"
			+ " from PLATFORM";
		if (tmtype != null)
		{
			// Note: "goes" matches goes, goes-self-timed or goes-random
			tmtype = tmtype.toLowerCase();
			if (tmtype.equals("goes"))
				tmtype = sqlString("goes") + "," + sqlString("goes-self-timed") + ","
					+ sqlString("goes-random");
			else
				tmtype = sqlString(tmtype);
	
			q = q + " where exists(select PLATFORMID from TRANSPORTMEDIUM where lower(MEDIUMTYPE) IN ("
				  + tmtype + ") and PLATFORM.ID = PLATFORMID)";
		}
		q = q + " order by ID";
System.out.println(q);
		ResultSet rs = doQuery(q);
		String action = "reading PLATFORM";
		try (SiteDAI siteDAO = db.makeSiteDAO())
		{
			while(rs.next())
			{
				PlatformRef ps = new PlatformRef();
				ps.setPlatformId(rs.getLong(1));
				ps.setAgency(rs.getString(2));
				ps.setSiteId(rs.getLong(3));
				ps.setConfigId(rs.getLong(4));
				ps.setDescription(rs.getString(5));
				ps.setDesignator(rs.getString(6));
				
				ret.add(ps);
			}
			
			action = "reading SITE";
			siteDAO.setManualConnection(getConnection());
			SiteList siteList = new SiteList();
			// The cache is static, so synchronize on the DAO class
			synchronized(siteDAO.getClass())
			{
				siteDAO.fillCache();
				siteDAO.read(siteList);
			}
			
			for (PlatformRef ps : ret)
			{
				Site site = siteList.getSiteById(DbKey.createDbKey(ps.getSiteId()));
				String platname = site != null ? site.getPreferredName().getNameValue() : "unknownSite";
				if (ps.getDesignator() != null && ps.getDesignator().length() > 0)
					platname = platname + "-" + ps.getDesignator();
				ps.setName(platname);
			}
			
			action = "reading TRANSPORTMEDIUM";
			q = "select PLATFORMID, MEDIUMTYPE, MEDIUMID from TRANSPORTMEDIUM";
//			if (tmtype != null)
//				q = q + " where lower(MEDIUMTYPE) IN (" + tmtype + ")";
			q = q + " order by PLATFORMID, MEDIUMTYPE";
			rs = doQuery(q);
			long lastId = DbKey.NullKey.getValue();
			PlatformRef testSpec = new PlatformRef();
			Comparator<PlatformRef> keyTest = new Comparator<PlatformRef>()
				{
					@Override
					public int compare(PlatformRef o1, PlatformRef o2)
					{
						long x = o1.getPlatformId() - o2.getPlatformId();
						return x > 0 ? 1 : x < 0 ? -1 : 0;
					}
			
				};
System.out.println(q);
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
		}
		catch(SQLException ex)
		{
			throw new DbIoException(module + " error while " + action + ": " + ex);
		}
		
		return ret;
	}
	
	/**
	 * Return true if the passed site ID is used by at least one platform or
	 * platform sensor.
	 * @param siteId
	 */
	public boolean isSiteUsed(DbKey siteId)
		throws DbIoException
	{
		String q = "select count(*) from PLATFORM where SITEID = " + siteId;
		ResultSet rs = doQuery(q);
		try
		{
			if (rs.next() && rs.getInt(1) > 0)
				return true;
			q = "select count(*) from PLATFORMSENSOR where SITEID = " + siteId;
			rs = doQuery(q);
			if (rs.next() && rs.getInt(1) > 0)
				return true;
			return false;
		}
		catch(SQLException ex)
		{
			throw new DbIoException(module + " error while counting site refs: " + ex);
		}
	}
	
	public boolean isConfigUsed(long configId)
		throws DbIoException
	{
		String q = "select count(*) from PLATFORM where CONFIGID = " + configId;
		ResultSet rs = doQuery(q);
		try
		{
			if (rs.next() && rs.getInt(1) > 0)
				return true;
			return false;
		}
		catch(SQLException ex)
		{
			throw new DbIoException(module + " error while counting config refs: " + ex);
		}

	}


	public DecodesPlatform readPlatform(long platformId)
		throws DbIoException, WebAppException

	{
		String q = "select AGENCY, ISPRODUCTION, SITEID, CONFIGID, DESCRIPTION, LASTMODIFYTIME,"
			+ " PLATFORMDESIGNATOR from PLATFORM where ID = " + platformId;
		ResultSet rs = doQuery(q);
		try
		{
			if (!rs.next())
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, "No such platform with id="
					+ platformId);
			DecodesPlatform ret = new DecodesPlatform();
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
			ret.setLastModified(db.getFullDate(rs, 6));
			ret.setDesignator(rs.getString(7));
			
			q = "select PROP_NAME, PROP_VALUE from PLATFORMPROPERTY"
				+ " where PLATFORMID = " + platformId;
			rs = doQuery(q);
			while(rs.next())
				ret.getProperties().setProperty(rs.getString(1), rs.getString(2));
			
			q = "select SENSORNUMBER, SITEID, DD_NU from PLATFORMSENSOR"
				+ " where PLATFORMID = " + platformId;
			rs = doQuery(q);
			while(rs.next())
			{
				DecodesPlatformSensor dps = new DecodesPlatformSensor();
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
				+ " where PLATFORMID = " + platformId + " order by SENSORNUMBER, PROP_NAME";
			rs = doQuery(q);
		  next_prop:
			while(rs.next())
			{
				int sn = rs.getInt(1);
				for (DecodesPlatformSensor dps : ret.getPlatformSensors())
					if (dps.getSensorNum() == sn)
					{
						String n = rs.getString(2);
						String v = rs.getString(3);
						if (n.equalsIgnoreCase("min"))
						{
							try { dps.setMin(Double.parseDouble(v.trim())); }
							catch(Exception ex)
							{
								Logger.instance().warning(module + ".readPlatform() "
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
								Logger.instance().warning(module + ".readPlatform() "
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
				String m = "delete from PLATFORMSENSORPROPERTY where PLATFORMID = " + platformId
					+ " and SENSORNUMBER = " + sn;
				doModify(m);
			}
			
			q = "select MEDIUMTYPE, MEDIUMID, SCRIPTNAME, CHANNELNUM, ASSIGNEDTIME,"
				+ " TRANSMITWINDOW, TRANSMITINTERVAL, TIMEADJUSTMENT, "
				+ " TIMEZONE, LOGGERTYPE, BAUD, STOPBITS, PARITY, DATABITS, DOLOGIN,"
				+ " USERNAME, PASSWORD"
				+ " from TRANSPORTMEDIUM where PLATFORMID = " + platformId;
			rs = doQuery(q);
			while(rs.next())
			{
				DecodesTransportMedium dtm = new DecodesTransportMedium();
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
					dtm.setDoLogin(TextUtil.str2boolean(s));
				dtm.setUsername(rs.getString(16));
				dtm.setPassword(rs.getString(17));
				
				ret.getTransportMedia().add(dtm);
			}
			return ret;

		}
		catch(SQLException ex)
		{
			throw new DbIoException(module + ".readPlatform(" + platformId 
				+ ") error in query '" + q + "': " + ex);
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
	 */
	public DecodesPlatform writePlatform(DecodesPlatform platform)
		throws DbIoException, WebAppException
	{
		// Check for null site. Cannot write platform without site assignment
		if (platform.getSiteId() == DbKey.NullKey.getValue())
			throw new WebAppException(ErrorCodes.NOT_ALLOWED,
				module + " Cannot write platform without site assignment.");
		
		// Check that Site + Designator is unique
		String q = "select ID from PLATFORM where SITEID = " + platform.getSiteId();
		if (platform.getDesignator() != null && platform.getDesignator().length() == 0)
			platform.setDesignator(null);
		if (platform.getDesignator() == null)
			q = q + " and PLATFORMDESIGNATOR is null";
		else
			q = q + " and lower(PLATFORMDESIGNATOR) = " + sqlString(platform.getDesignator().toLowerCase());
		if (platform.getPlatformId() != DbKey.NullKey.getValue())
			q = q + " and ID ! = " + platform.getPlatformId();
		
		ResultSet rs = doQuery(q);
		try
		{
			if (rs.next())
				throw new WebAppException(ErrorCodes.NOT_ALLOWED, 
					"Cannot write Platform '" + platform.getName() 
					+ "' because another PLATFORM with id=" + rs.getLong(1) 
					+ " already has that the same siteID (" + platform.getSiteId() 
					+ ") and designator (" + platform.getDesignator() + ")");
			
			// Check for TM clash
			for (DecodesTransportMedium tm : platform.getTransportMedia())
			{
				q = "select PLATFORMID from TRANSPORTMEDIUM"
					+ " where lower(MEDIUMTYPE) = " + sqlString(tm.getMediumType().toLowerCase())
					+ " and lower(MEDIUMID) = " + sqlString(tm.getMediumId().toLowerCase());
				if (platform.getPlatformId() != DbKey.NullKey.getValue())
					q = q + " and PLATFORMID != " + platform.getPlatformId();
				rs = doQuery(q);
				if (rs.next())
					throw new WebAppException(ErrorCodes.NOT_ALLOWED, 
						"Cannot write Platform '" + platform.getName() 
						+ "' because another PLATFORM with id=" + rs.getLong(1) 
						+ " already has a transport medium " + tm.getMediumType() + ":" + tm.getMediumId());
			}
		}
		catch(SQLException ex)
		{
			throw new DbIoException(module + ".writePlatform error in query '" + q + "': " + ex);
		}

		platform.setLastModified(new Date());
		if (platform.getPlatformId() == DbKey.NullKey.getValue())
			insert(platform);
		else
			update(platform);
		return platform;
	}
	
	private void insert(DecodesPlatform platform)
		throws DbIoException, WebAppException
	{
		platform.setPlatformId(getKey("PLATFORM").getValue());
		String q = "insert into PLATFORM(ID, AGENCY, ISPRODUCTION, SITEID, CONFIGID, DESCRIPTION"
				+ ", LASTMODIFYTIME, PLATFORMDESIGNATOR)"
			+ " values ("
			+ platform.getPlatformId() + ", "
			+ sqlString(platform.getAgency()) + ", "
			+ sqlBoolean(platform.isProduction()) + ", "
			+ platform.getSiteId() + ", "
			+ platform.getConfigId() + ", "
			+ sqlString(platform.getDescription()) + ", "
			+ db.sqlDate(platform.getLastModified()) + ", "
			+ sqlString(platform.getDesignator())
			+ ")";
		doModify(q);
		
		deleteSubordinates(platform.getPlatformId());
		writeSubordinates(platform);
	}
	
	private void update(DecodesPlatform platform)
		throws DbIoException, WebAppException
	{
		String q = "update PLATFORM set " +
			"AGENCY = " + sqlString(platform.getAgency()) + ", " +
			"ISPRODUCTION = " + sqlBoolean(platform.isProduction()) + ", " +
			"SITEID = " + platform.getSiteId() + ", " +
			"CONFIGID = " + platform.getConfigId() + ", " +
			"DESCRIPTION = " + sqlString(platform.getDescription()) + ", " +
			"LASTMODIFYTIME = " + db.sqlDate(platform.getLastModified()) + ", " +
			"PLATFORMDESIGNATOR = " + sqlString(platform.getDesignator())
			+ " where ID = " + platform.getPlatformId();
		doModify(q);
	
		deleteSubordinates(platform.getPlatformId());
		writeSubordinates(platform);
	}
	
	private void deleteSubordinates(long platformId)
		throws DbIoException
	{
		String q = "delete from PLATFORMPROPERTY where PLATFORMID = " + platformId;
		doModify(q);
		
		q = "delete from PLATFORMSENSORPROPERTY where PLATFORMID = " + platformId;
		doModify(q);

		q = "delete from PLATFORMSENSOR where PLATFORMID = " + platformId;
		doModify(q);
		
		q = "delete from TRANSPORTMEDIUM where PLATFORMID = " + platformId;
		doModify(q);
		
	}
	
	private void writeSubordinates(DecodesPlatform platform)
		throws DbIoException, WebAppException
	{
		
		for(Object k : platform.getProperties().keySet())
		{
			String q = "insert into PLATFORMROPERTY(PLATFORMID, PROP_NAME, PROP_VALUE)"
				+ " values("
				+ platform.getPlatformId() + ", "
				+ sqlString((String)k) + ", "
				+ sqlString(platform.getProperties().getProperty((String)k))
				+ ")";
			doModify(q);
		}


		for(DecodesPlatformSensor dps : platform.getPlatformSensors())
		{
			String q = "insert into PLATFORMSENSOR(PLATFORMID, SENSORNUMBER, SITEID, DD_NU) "
			+ "values("
			+ platform.getPlatformId() + ", "
			+ dps.getSensorNum() + ", "
			+ dps.getActualSiteId() + ", "
			+ dps.getUsgsDdno()
			+")";
			doModify(q);
			
			Properties props = new Properties();
			PropertiesUtil.copyProps(props, dps.getSensorProps());
			if (dps.getMin() != null)
				props.setProperty("min", ""+dps.getMin());
			if (dps.getMax() != null)
				props.setProperty("max", ""+dps.getMax());
			
			for(Object k : props.keySet())
			{
				q = "insert into PLATFORMSENSORPROPERTY(PLATFORMID, SENSORNUMBER, PROP_NAME, PROP_VALUE)"
					+ " values("
					+ platform.getPlatformId() + ", "
					+ dps.getSensorNum() + ", "
					+ sqlString((String)k) + ", "
					+ sqlString(props.getProperty((String)k))
					+ ")";
				doModify(q);
			}
		}
		
		for(DecodesTransportMedium dtm : platform.getTransportMedia())
		{
			String q = "insert into TRANSPORTMEDIUM(PLATFORMID, MEDIUMTYPE, MEDIUMID, SCRIPTNAME, CHANNELNUM, "
				+ "ASSIGNEDTIME, TRANSMITWINDOW, TRANSMITINTERVAL, TIMEADJUSTMENT, "
				+ "TIMEZONE, LOGGERTYPE, BAUD, STOPBITS, PARITY, DATABITS, DOLOGIN, "
				+ "USERNAME, PASSWORD) "
				+ "values("
				+ platform.getPlatformId() + ", "
				+ sqlString(dtm.getMediumType()) + ", "
				+ sqlString(dtm.getMediumId()) + ", "
				+ sqlString(dtm.getScriptName()) + ", "
				+ dtm.getChannelNum() + ", "
				+ dtm.getAssignedTime() + ", "
				+ dtm.getTransportWindow() + ", "
				+ dtm.getTransportInterval() + ", "
				+ dtm.getTimeAdjustment() + ", "
				+ sqlString(dtm.getTimezone()) + ", "
				+ sqlString(dtm.getLoggerType()) + ", "
				+ dtm.getBaud() + ", "
				+ dtm.getStopBits() + ", "
				+ sqlString(dtm.getParity()) + ", "
				+ dtm.getDataBits() + ", "
				+ sqlBoolean(dtm.getDoLogin()) + ", "
				+ sqlString(dtm.getUsername()) + ", "
				+ sqlString(dtm.getPassword())
				+ ")";
			doModify(q);
		}
	}

	public void deletePlatform(long platformId)
		throws DbIoException, WebAppException
	{
		deleteSubordinates(platformId);
		String q = "delete from PLATFORM where ID = " + platformId;
		doModify(q);
	}

	
}
