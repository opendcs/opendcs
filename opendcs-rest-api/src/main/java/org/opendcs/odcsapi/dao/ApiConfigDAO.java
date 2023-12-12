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
import java.util.Arrays;

import org.opendcs.odcsapi.beans.ApiConfigRef;
import org.opendcs.odcsapi.beans.ApiConfigScript;
import org.opendcs.odcsapi.beans.ApiConfigScriptSensor;
import org.opendcs.odcsapi.beans.ApiConfigSensor;
import org.opendcs.odcsapi.beans.ApiPlatformConfig;
import org.opendcs.odcsapi.beans.ApiScriptFormatStatement;
import org.opendcs.odcsapi.beans.ApiUnitConverter;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;

public class ApiConfigDAO
	extends ApiDaoBase
{
	public static String module = "ConfigDAO";

	public ApiConfigDAO(DbInterface dbi)
	{
		super(dbi, module);
	}

	public ArrayList<ApiConfigRef> getConfigRefs()
			throws DbException
	{
		ArrayList<ApiConfigRef> ret = new ArrayList<ApiConfigRef>();
		String q = "select ID, NAME, DESCRIPTION"
			+ " from PLATFORMCONFIG order by ID";
		ResultSet rs = doQuery(q);
		try
		{
			while (rs.next())
			{
				ApiConfigRef ref = new ApiConfigRef();
				ref.setConfigId(rs.getLong(1));
				ref.setName(rs.getString(2));
				ref.setDescription(rs.getString(3));
				ret.add(ref);
			}
			
			q = "select p.CONFIGID, count(p.CONFIGID)"
				+ " from PLATFORM p group by p.CONFIGID";
			rs = doQuery(q);
			while(rs.next())
			{
				long id = rs.getLong(1);
				int count = rs.getInt(2);
				for(ApiConfigRef ref : ret)
					if (ref.getConfigId() == id)
					{
						ref.setNumPlatforms(count);
						break;
					}
			}
			return ret;
		}
		catch(SQLException ex)
		{
			throw new DbException(module, ex, "Error in query '" + q + "'");
		}
	}
	
	public ApiPlatformConfig getConfig(long configId)
		throws DbException, WebAppException, SQLException
	{
		ApiPlatformConfig ret = new ApiPlatformConfig();
		
		String q = "select ID, NAME, DESCRIPTION"
				+ " from PLATFORMCONFIG"
				+ " where ID = ?";
		Connection conn = null;
		ResultSet rs = doQueryPs(conn, q, configId);
		
		try
		{
			if (!rs.next())
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, 
					"No PLATFORMCONFIG with id=" + configId);
			
			ret.setConfigId(rs.getLong(1));
			ret.setName(rs.getString(2));
			ret.setDescription(rs.getString(3));
			
			ret.setNumPlatforms(numPlatformsUsing(configId));
				
			q = "select SENSORNUMBER, SENSORNAME, RECORDINGMODE, RECORDINGINTERVAL,"
				+ " TIMEOFFIRSTSAMPLE, ABSOLUTEMIN, ABSOLUTEMAX, STAT_CD"
				+ " FROM CONFIGSENSOR"
				+ " where CONFIGID = ?";

			conn = null;
			rs = doQueryPs(conn, q, configId);
			while(rs.next())
			{
				ApiConfigSensor dcs = new ApiConfigSensor();
				dcs.setSensorNumber(rs.getInt(1));
				dcs.setSensorName(rs.getString(2));
				String s = rs.getString(3);
				if (!rs.wasNull() && s.length() > 0)
				dcs.setRecordingMode(s.charAt(0));
				dcs.setRecordingInterval(rs.getInt(4));
				dcs.setTimeOfFirstSample(rs.getInt(5));
				double d = rs.getDouble(6);
				if (!rs.wasNull())
					dcs.setAbsoluteMin(d);
				d = rs.getDouble(7);
				if (!rs.wasNull())
					dcs.setAbsoluteMax(d);
				dcs.setUsgsStatCode(rs.getString(8));
				ret.getConfigSensors().add(dcs);
			}
			
			q = "select csdt.SENSORNUMBER, dt.STANDARD, dt.CODE"
				+ " from CONFIGSENSORDATATYPE csdt, DATATYPE dt"
				+ " where csdt.DATATYPEID = dt.ID"
				+ " and csdt.CONFIGID = ?"
				+ " order by csdt.SENSORNUMBER, dt.STANDARD";
			rs = doQueryPs(conn, q, configId);
			while(rs.next())
			{
				int sn = rs.getInt(1);
				ApiConfigSensor dcs = null;
				for(ApiConfigSensor x : ret.getConfigSensors())
					if (x.getSensorNumber() == sn)
					{
						dcs = x;
						break;
					}
				if (dcs == null)
					continue;
				dcs.getDataTypes().put(rs.getString(2), rs.getString(3));
			}
			
			q = "select SENSORNUMBER, PROP_NAME, PROP_VALUE"
				+ " from CONFIGSENSORPROPERTY"
				+ " where CONFIGID = ?";
			rs = doQueryPs(conn, q, configId);
			while(rs.next())
			{
				int sn = rs.getInt(1);
				ApiConfigSensor dcs = null;
				for(ApiConfigSensor x : ret.getConfigSensors())
					if (x.getSensorNumber() == sn)
					{
						dcs = x;
						break;
					}
				if (dcs == null)
					continue;
				dcs.getProperties().put(rs.getString(2), rs.getString(3));
			}
			
			q = "select ID, NAME, DATAORDER, SCRIPT_TYPE"
				+ " from DECODESSCRIPT"
				+ " where CONFIGID = ?";
			rs = doQueryPs(conn, q, configId);
			while(rs.next())
			{
				int scriptId = rs.getInt(1);
				final ApiConfigScript dcs = new ApiConfigScript();
				dcs.setName(rs.getString(2));
				dcs.setHeaderType(rs.getString(4));
				String s = rs.getString(3);
				if (!rs.wasNull() && s.length() > 0)
					dcs.setDataOrder(s.charAt(0));
				ret.getScripts().add(dcs);
				
				q = "select ss.SENSORNUMBER, uc.TOUNITSABBR, uc.ALGORITHM,"
					+ " uc.A, uc.B, uc.C, uc.D, uc.E, uc.F, uc.ID"
					+ " from SCRIPTSENSOR ss, UNITCONVERTER uc"
					+ " where ss.UNITCONVERTERID = uc.ID and ss.DECODESSCRIPTID = ?";
				ResultSet rs2 = doQueryPs(conn, q, configId);
				
				while(rs2.next())
				{
					ApiConfigScriptSensor dcss = new ApiConfigScriptSensor();
					dcss.setSensorNumber(rs2.getInt(1));
					ApiUnitConverter duc = new ApiUnitConverter();
					dcss.setUnitConverter(duc);
					duc.setFromAbbr("raw");
					duc.setToAbbr(rs2.getString(2));
					duc.setAlgorithm(rs2.getString(3));
					double d = rs2.getDouble(4);
					if (!rs2.wasNull())
						duc.setA(d);
					d = rs2.getDouble(5);
					if (!rs2.wasNull())
						duc.setB(d);
					d = rs2.getDouble(6);
					if (!rs2.wasNull())
						duc.setC(d);
					d = rs2.getDouble(7);
					if (!rs2.wasNull())
						duc.setD(d);
					d = rs2.getDouble(8);
					if (!rs2.wasNull())
						duc.setE(d);
					d = rs2.getDouble(9);
					if (!rs2.wasNull())
						duc.setF(d);
					duc.setUcId(rs2.getLong(10));
					dcs.getScriptSensors().add(dcss);
				}
	
				q = "select SEQUENCENUM, LABEL, FORMAT"
					+ " from FORMATSTATEMENT"
					+ " where DECODESSCRIPTID = ?"
					+ " order by SEQUENCENUM";
				doQueryV(conn, q,
					new ResultSetConsumer()
					{
						@Override
						public void accept(ResultSet rs)
							throws SQLException
						{
							while(rs.next())
							{
								ApiScriptFormatStatement dsfs = new ApiScriptFormatStatement();
								dsfs.setSequenceNum(rs.getInt(1));
								dsfs.setLabel(rs.getString(2));
								dsfs.setFormat(rs.getString(3));
								dcs.getFormatStatements().add(dsfs);
							}
						}
					}, (Integer)scriptId);
			}
			
			return ret;
		}
		catch(SQLException ex)
		{
			throw new DbException(module, ex, "Error in query '" + q + "'");
		}
	}
	
	/**
	 * 
	 * @param configId the unique ID of the config
	 * @return number of platform records that use this config
	 */
	public int numPlatformsUsing(Long configId)
		throws DbException
	{
		if (configId == null)
			return 0;
				
		String q = "select count(*) from PLATFORM where CONFIGID = ?";
		try
		{
			Connection conn = null;
			ResultSet rs = doQueryPs(conn, q, configId);
			
			if (rs.next())
				return rs.getInt(1);
			else
				return 0;
		}
		catch(SQLException ex)
		{
			throw new DbException(module, ex, "Error in query '" + q + "'");
		}
	}

	public void writeConfig(ApiPlatformConfig config)
		throws DbException, WebAppException, SQLException
	{
		// Check to make sure name is unique throw WebAppException if conflict
		// I.e. check for list with same name but different key.
		String q = "select ID from PLATFORMCONFIG where lower(NAME) = ?"; 
		ArrayList<Object> args = new ArrayList<Object>();
		args.add(config.getName().toLowerCase());
		if (config.getConfigId() != null)
		{
			q = q + " and ID != ?";
			args.add(config.getConfigId());
		}
		Connection conn = null;
		ResultSet rs = doQueryPs(conn, q, args.toArray());

		try
		{
			if (rs.next())
				throw new WebAppException(ErrorCodes.NOT_ALLOWED, 
					"Cannot write config with name '" + config.getName() 
					+ "' because another config with id=" + rs.getLong(1) 
					+ " also has that name.");
			
			if (config.getConfigId() == null)
				insert(config);
			else
				update(config);
		}
		catch (SQLException ex)
		{
			throw new DbException(module, ex, "Error in query '" + q + "'");
		}
	}
		
	private void update(ApiPlatformConfig config)
		throws DbException, SQLException
	{
		deleteSubordinates(config.getConfigId());
		String q = "update PLATFORMCONFIG set NAME = ?, DESCRIPTION = ?, EQUIPMENTID = null where ID = ?";
		doModifyV(q, config.getName(), config.getDescription(), config.getConfigId());
		writeSubordinates(config);
	}
	
	private void insert(ApiPlatformConfig config)
			throws DbException, SQLException
	{
		config.setConfigId(getKey("PLATFORMCONFIG"));

		String q = "insert into PLATFORMCONFIG(ID, NAME, DESCRIPTION)"
			+ " values (?, ?, ?)";
		doModifyV(q, config.getConfigId(), config.getName(), config.getDescription());
		writeSubordinates(config);
	}

	private void deleteSubordinates(long configId)
		throws DbException, SQLException
	{
		String q = "delete from CONFIGSENSORDATATYPE where CONFIGID = ?";
		doModifyV(q, configId);
		
		q = "delete from CONFIGSENSORPROPERTY where CONFIGID = ?";
		doModifyV(q, configId);
		
		q = "delete from CONFIGSENSOR where CONFIGID = ?";
		doModifyV(q, configId);
		
		// Chicken & egg: Have to delete unit converters used in script sensors, but
		// Can't delete UC's first because reference constraint.
		StringBuilder ucIdList = new StringBuilder();
		ArrayList<Integer> ucIdArgs = new ArrayList<Integer>();
		q = "select ss.UNITCONVERTERID from SCRIPTSENSOR ss, DECODESSCRIPT ds"
			+ " where ss.DECODESSCRIPTID = ds.ID"
			+ " and ds.CONFIGID = ?";
		Connection conn = null;
		ResultSet rs = doQueryPs(conn, q, configId);

		try
		{
			while(rs.next())
			{
				ucIdList.append((ucIdList.length() == 0 ? "" : ",") + rs.getInt(1));
				ucIdArgs.add(rs.getInt(1));
			}
		}
		catch (SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
		
		q ="delete from SCRIPTSENSOR ss"
			+ " where ss.DECODESSCRIPTID IN ("
				+ "select ds.ID from DECODESSCRIPT ds"
				+ " where ds.CONFIGID = ?)";
		doModifyV(q, configId);
		if (ucIdArgs.size() > 0)
		{
			String[] placeHolders = new String[ucIdArgs.size()];
			Arrays.fill(placeHolders, "%s");
			String[] qMarks = new String[ucIdArgs.size()];
			Arrays.fill(qMarks, "?");
			String tempQueryStr = "delete from UNITCONVERTER uc where uc.ID in (" + String.join(",", placeHolders) + ")";
			System.out.println("Query Str: " + tempQueryStr);
			q = String.format(tempQueryStr, qMarks);
			doModifyV(q, ucIdArgs.toArray());
		}
		
		q ="delete from UNITCONVERTER uc"
			+ " where uc.ID IN ("
				+ "select ss.UNITCONVERTERID from SCRIPTSENSOR ss, DECODESSCRIPT ds"
				+ " where ss.DECODESSCRIPTID = ds.ID"
				+ " and ds.CONFIGID = ?)";
		doModifyV(q, configId);
		
		q ="delete from FORMATSTATEMENT fs"
			+ " where fs.DECODESSCRIPTID IN ("
				+ "select ds.ID from DECODESSCRIPT ds"
				+ " where ds.CONFIGID = ?)";
		doModifyV(q, configId);
		
		q ="delete from DECODESSCRIPT"
			+ " where CONFIGID = ?";
		doModifyV(q, configId);
	}
	
	private void writeSubordinates(ApiPlatformConfig config)
		throws DbException, SQLException
	{
		ApiDataTypeDAO dtDao = new ApiDataTypeDAO(dbi);
		for(ApiConfigSensor dcs : config.getConfigSensors())
		{
			Double min = dcs.getAbsoluteMin();
			Double max = dcs.getAbsoluteMax();
			String q = "insert into CONFIGSENSOR(CONFIGID, SENSORNUMBER, SENSORNAME, RECORDINGMODE,"
				+ " RECORDINGINTERVAL,"
				+ " TIMEOFFIRSTSAMPLE, ABSOLUTEMIN, ABSOLUTEMAX, STAT_CD)"
				+ " values(?, ?, ?, ?, ?, ?, ?, ?, ?)";
			
			doModifyV(q, config.getConfigId(), dcs.getSensorNumber(), dcs.getSensorName(), dcs.getRecordingMode(), dcs.getRecordingInterval(),
					dcs.getTimeOfFirstSample(), (min == null ? null : min), (max == null ? null : max), dcs.getUsgsStatCode());
			
			for (String dtstd : dcs.getDataTypes().keySet())
			{
				String dtcd = dcs.getDataTypes().get(dtstd);
				Long id = dtDao.lookup(dtstd, dtcd);
				if (id == null)
					id = dtDao.create(dtstd, dtcd);
				q = "insert into CONFIGSENSORDATATYPE(CONFIGID, SENSORNUMBER, DATATYPEID)"
					+ " values(?, ?, ?)";
				doModifyV(q, config.getConfigId(), dcs.getSensorNumber(), id);
			}
		
			for(Object k : dcs.getProperties().keySet())
			{
				String pn = (String)k;
				String pv = dcs.getProperties().getProperty(pn);
				q = "insert into CONFIGSENSORPROPERTY(CONFIGID, SENSORNUMBER, PROP_NAME, PROP_VALUE)"
					+ " values (?, ?, ?, ?)";
				doModifyV(q, config.getConfigId(), dcs.getSensorNumber(), pn, pv);
			}
		}
		
		dtDao.close();
		
		for(ApiConfigScript acs : config.getScripts())
		{
			long scriptId = getKey("DECODESSCRIPT");
			String q = "insert into DECODESSCRIPT(ID, CONFIGID, NAME, SCRIPT_TYPE, DATAORDER)"
				+ " values(?, ?, ?, ?, ?)";
			doModifyV(q, scriptId, config.getConfigId(), acs.getName(), acs.getHeaderType(), acs.getDataOrder());
			
			int seq = 0;
			for(ApiScriptFormatStatement asfs : acs.getFormatStatements())
			{
				q = "insert into FORMATSTATEMENT(DECODESSCRIPTID, SEQUENCENUM, LABEL, FORMAT)"
					+ " values(?, ?, ?, ?)";
				doModifyV(q, scriptId, seq++, asfs.getLabel(), asfs.getFormat());
			}
			
			for(ApiConfigScriptSensor acss : acs.getScriptSensors())
			{
				long ucid = getKey("UNITCONVERTER");
				
				q = "insert into UNITCONVERTER(ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, "
					+ "A, B, C, D, E, F) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
				doModifyV(q, ucid, "raw", acss.getUnitConverter().getToAbbr(), acss.getUnitConverter().getAlgorithm(), acss.getUnitConverter().getA(),
						acss.getUnitConverter().getB(), acss.getUnitConverter().getC(), acss.getUnitConverter().getD(), acss.getUnitConverter().getE(),
						acss.getUnitConverter().getF());
				
				q = "insert into SCRIPTSENSOR(DECODESSCRIPTID, SENSORNUMBER, UNITCONVERTERID)"
					+ " values(?, ?, ?)";
				doModifyV(q, scriptId, acss.getSensorNumber(), ucid);
			}
		}		
	}

	public void deleteConfig(long configId)
		throws DbException, WebAppException, SQLException
	{
		deleteSubordinates(configId);
		String q = "delete from PLATFORMCONFIG where ID = ?";
		doModifyV(q, configId);
	}
}
