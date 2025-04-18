/*
 * $Id$
 * 
 * $Log$
 * Revision 1.3  2014/08/22 17:23:10  mmaloney
 * 6.1 Schema Mods and Initial DCP Monitor Implementation
 *
 * Revision 1.2  2014/07/03 12:53:41  mmaloney
 * debug improvements.
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * This software was written by Cove Software, LLC ("COVE") under contract
 * to the United States Government. No warranty is provided or implied other 
 * than specific contractual terms between COVE and the U.S. Government.
 *
 * Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
 */
package opendcs.dao;

import decodes.cwms.CwmsFlags;
import decodes.db.DbEnum;
import decodes.db.EnumList;
import decodes.db.EnumValue;
import decodes.db.ValueNotFoundException;
import decodes.sql.DbKey;
import decodes.sql.DecodesDatabaseVersion;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesIdentifier;
import ilex.var.TimedVariable;
import opendcs.dai.EnumDAI;
import opendcs.dai.TimeSeriesDAI;
import org.opendcs.database.SimpleTransaction;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Data Access Object for writing/reading waterTempProfile objects to/from a SQL database
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public class WaterTempProfileDao extends DaoBase
{
	private static final Logger log = LoggerFactory.getLogger(WaterTempProfileDao.class);
	private static DbObjectCache<DbEnum> cache = new DbObjectCache<DbEnum>(3600000, false);

	public WaterTempProfileDao(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, "EnumSqlDao");
	}

	@Override
	public DataTransaction getTransaction() throws OpenDcsDataException
	{
		try
		{
			return new SimpleTransaction(db.getConnection());
		}
		catch (SQLException ex)
		{
			throw new OpenDcsDataException("Unable to get connection.", ex);
		}
	}
	
	private String getEnumColumns(int dbVer)
	{
		return "id, name"
			+ (dbVer >= DecodesDatabaseVersion.DECODES_DB_10 ? ", defaultValue, description "
			: dbVer >= DecodesDatabaseVersion.DECODES_DB_6 ? ", defaultvalue " 
			: " ");
	}
	
	private DbEnum rs2Enum(ResultSet rs, int dbVer)
		throws SQLException
	{
		DbKey id = DbKey.createDbKey(rs, 1);
		DbEnum en = new DbEnum(id, rs.getString(2));

		if (dbVer >= DecodesDatabaseVersion.DECODES_DB_6)
		{
			String def = rs.getString(3);
			if (!rs.wasNull())
				en.setDefault(def.trim());
		}
		if (dbVer >= DecodesDatabaseVersion.DECODES_DB_10)
		{
			en.setDescription(rs.getString(4));
		}
		return en;
	}

	private List<CTimeSeries> rs2CTSList(ResultSet rs)
			throws SQLException
	{
		List<CTimeSeries> ctsList = new ArrayList<>();
		for(result : rs)
		{

			DbKey id = DbKey.createDbKey(rs, "ts_code");
			double value = rs.getDouble("value");
			long qualityCode = rs.getLong("quality_code");
			Date date = rs.getDate("date_time");
			TimeSeriesDAI bob;
			TimeSeriesIdentifier TSID = bob.findTimeSeriesIdentifier(id);
			CTimeSeries cts = new CTimeSeries(TSID);


//			Date timeStamp = db.getFullDate(rs, 1);
//			double value = rs.getDouble(2);
//
//			// Check for missing, deleted, or rejected data
//			if (rs.wasNull())
//			{
//				return null;
//			}
//			long lf = rs.getLong(3);

			int f =    CwmsFlags.cwmsQuality2flag(qualityCode);
			if ((f & CwmsFlags.VALIDITY_MISSING) != 0)
			{
				return null;
			}

			TimedVariable tv = new TimedVariable(value);
			tv.setTime(date);
			tv.setFlags(f);

			cts.addSample(tv);
			ctsList.add(cts);
		}
		return ctsList;

	}

	private void readValues(DaoBase dao, DbEnum dbenum) throws SQLException, DbIoException
	{
		int dbVer = db.getDecodesDatabaseVersion();

		String q = 
			"SELECT enumId, enumValue, description, " +
			"execClass, editClass";
		if (dbVer >= DecodesDatabaseVersion.DECODES_DB_6)
			q = q + ", sortNumber";
		q = q + " FROM EnumValue WHERE EnumID = ?";// + dbenum.getId();
		//ResultSet rs = doQuery2(q);
		dao.doQuery(q,(rs)-> {
			rs2EnumValue(rs, dbenum);
		},dbenum.getId());
	}
	
	private void rs2EnumValue(ResultSet rs, DbEnum dbEnum)
		throws SQLException
	{
		String enumValue = rs.getString(2);
		String description = rs.getString(3);
		String execClass = rs.getString(4);
		String editClass = rs.getString(5);

		int sn = 0;
		boolean setSortNumber = false;
		if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_6)
		{
			sn = rs.getInt(6);
			if (!rs.wasNull())
				setSortNumber = true;
		}
		EnumValue ev = dbEnum.replaceValue(enumValue, description, execClass, editClass);
		if (setSortNumber)
			ev.setSortNumber(sn);
	}

	/**
	* Write a single EnumValue to the database.
	* Assume no conflict with EnumValues already in the database.
	* @param ev the EnumValue
	*/
	private void writeEnumValue(DaoBase dao, EnumValue ev) throws DbIoException
	{
		ArrayList<Object> args = new ArrayList<>();
		args.add(ev.getDbenum().getId().getValue());
		args.add(ev.getValue());
		args.add(ev.getDescription());
		args.add(ev.getExecClassName());
		args.add(ev.getEditClassName());
		String q =
			"INSERT INTO EnumValue VALUES(" +
				"?," + /*ev.getDbenum().getId() + ", " +*/
				"?," + /*sqlString(ev.getValue()) + ", " +*/
				"?," + /*sqlString(ev.getDescription()) + ", " +*/
				"?," + /*sqlString(ev.getExecClassName()) + ", " +*/
				"?"; /*sqlString(ev.getEditClassName());*/
		if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_6)
		{
			q += ")";
		}			
		else if (ev.getSortNumber() == EnumValue.UNDEFINED_SORT_NUMBER)
		{
			q += ", NULL)";
		}
		else
		{
			q = q + ", ?)";
			args.add(ev.getSortNumber());
		}
		try
		{
			dao.doModify(q,args.toArray());
		} 
		catch(SQLException er)
		{
			debug3(er.getLocalizedMessage());
			throw new DbIoException("Failed to add enum to database", er);
		}
		
	}

	@Override
	public Optional<DbEnum> getWaterTempProfiles(DataTransaction tx, String WTPID) throws OpenDcsDataException
	{
		synchronized(cache)
		{
//			DbEnum ret = cache.getByUniqueName(enumName);
//			if (ret != null)
//			{
//				return Optional.of(ret);
//			}
			List<CTimeSeries> ret;
			String start = WTPID;
			int index = start.indexOf("-D");
			if (index != -1) {
				start = start.substring(0, index);
			}
			String end = WTPID;
			index = end.indexOf("m.");
			if (index != -1) {
				end = end.substring(index+2);
			}

			String q = "SELECT DATE_TIME, VALUE, QUALITY_CODE, TS_CODE\n" +
					"FROM CWMS_V_TSV\n" +
					"WHERE TS_CODE IN (\n" +
					"SELECT TS_CODE\n" +
					"FROM CWMS_V_TS_ID\n" +
					"WHERE REGEXP_LIKE(CWMS_TS_ID, '^" + start + ".*" + end + "$', 'i')\n" +
					")";

			//getTimeSeriesIdentifier
			Connection conn = tx.connection(Connection.class)
						        .orElseThrow(() -> new OpenDcsDataException("JDBC Connection not available in this transaction."));
			try (DaoHelper helper = new DaoHelper(this.db, "helper-WTP", conn))
			{
				ret = helper.getResults(q, rs -> rs2CTSList(rs));
				if (ret == null)
				{
					warning("No such enum '" + enumName + "'");
					return Optional.empty();
				}
				else
				{
					readValues(helper, ret);
					cache.put(ret);
					return Optional.of(ret);
				}		
			}
			catch (DbIoException | SQLException ex)
			{
				throw new OpenDcsDataException("Error retrieving Enum values",ex);
			}
		}
	}

	@Override
	public DbEnum writeEnum(DataTransaction tx, DbEnum dbEnum) throws OpenDcsDataException
	{
		// should this be part of DataTransaction?
		int dbVer = db.getDecodesDatabaseVersion();
		String q = "";
		ArrayList<Object> args = new ArrayList<>();
		if (dbEnum.idIsSet())
		{			
			args.add(dbEnum.getUniqueName());
			q = "update enum set name = ?";// + sqlString(dbenum.getUniqueName());
			if (dbVer >= DecodesDatabaseVersion.DECODES_DB_6)
			{
				q = q + ", defaultvalue = ?";// + sqlString(dbenum.getDefault());
				args.add(dbEnum.getDefault());
				if (dbVer >= DecodesDatabaseVersion.DECODES_DB_10)
					q = q + ", description = ?";// + sqlString(dbenum.getDescription());
					args.add(dbEnum.getDescription());
			}
			q = q + " where id = ?" /*+ dbenum.getId()*/;
			args.add(dbEnum.getId().getValue());
		}
		else // New enum, allocate a key and insert
		{
			DbKey id;
			try
			{
				id = getKey("Enum");
			}
			catch (DbIoException ex)
			{
				throw new OpenDcsDataException("Unable to generate new key for dbEnum", ex);
			}
			dbEnum.forceSetId(id);
			q = "insert into enum";
			if (dbVer < DecodesDatabaseVersion.DECODES_DB_6)
			{
				q = q + "(id, name) values (?,?)"; 
					//+ id + ", " + sqlString(dbenum.getUniqueName()) + ")";
				args.add(id.getValue());
				args.add(dbEnum.getUniqueName());
			}
			else if (dbVer < DecodesDatabaseVersion.DECODES_DB_10)
			{
				q = q + "(id, name, defaultValue) values (?,?,?)";
				args.add(id.getValue());
				args.add(dbEnum.getUniqueName());
				args.add(dbEnum.getDefault());
			}
			else
			{
				q = q + "(id, name, defaultValue, description) values (?,?,?,?)";
				args.add(id.getValue());
				args.add(dbEnum.getUniqueName());
				args.add(dbEnum.getDefault());
				args.add(dbEnum.getDescription());
			}
			cache.put(dbEnum);
		}
		
		Connection conn = tx.connection(Connection.class)
							.orElseThrow(() -> new OpenDcsDataException("Unable to get JDBC connection to perform DbEnum Save."));		
		try (DaoHelper helper = new DaoHelper(this.db, q, conn))
		{
			helper.doModify(q,args.toArray());

			// Delete all enum values. They'll be re-added below.
			//info("writeEnum deleting values from enum '" + dbenum.enumName + "'");
			q = "DELETE FROM EnumValue WHERE enumId = ?";
			helper.doModify(q, dbEnum.getId().getValue());
			
			for (Iterator<EnumValue> it = dbEnum.iterator(); it.hasNext(); )
			{
				writeEnumValue(helper, it.next());
			}
			return dbEnum;
		}
		catch(DbIoException | SQLException ex)
		{
			throw new OpenDcsDataException("enum modify/delete failed for " + dbEnum.toString(), ex);
		}
	}

}
