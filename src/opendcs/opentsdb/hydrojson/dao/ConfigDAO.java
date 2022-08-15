package opendcs.opentsdb.hydrojson.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import opendcs.dao.DaoBase;
import opendcs.dao.DatabaseConnectionOwner;
import opendcs.opentsdb.hydrojson.ErrorCodes;
import opendcs.opentsdb.hydrojson.beans.ApiConfigRef;
import opendcs.opentsdb.hydrojson.beans.ApiConfigScript;
import opendcs.opentsdb.hydrojson.beans.ApiConfigScriptSensor;
import opendcs.opentsdb.hydrojson.beans.ApiConfigSensor;
import opendcs.opentsdb.hydrojson.beans.ApiPlatformConfig;
import opendcs.opentsdb.hydrojson.beans.ApiScriptFormatStatement;
import opendcs.opentsdb.hydrojson.beans.ApiUnitConverter;
import opendcs.opentsdb.hydrojson.errorhandling.WebAppException;

public class ConfigDAO
	extends DaoBase
{
	public static String module = "ConfigDAO";

	public ConfigDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, module);
	}

	public ArrayList<ApiConfigRef> getConfigRefs()
			throws DbIoException
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
			throw new DbIoException(module + " error in query '" + q + "': " + ex);
		}
	}
	
	public ApiPlatformConfig getConfig(long id)
		throws DbIoException, WebAppException
	{
		ApiPlatformConfig ret = new ApiPlatformConfig();
		
		String q = "select ID, NAME, DESCRIPTION"
				+ " from PLATFORMCONFIG"
				+ " where ID = " + id;
		ResultSet rs = doQuery(q);
		
		try
		{
			if (!rs.next())
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, "No PLATFORMCONFIG with id=" + id);
			
			ret.setConfigId(rs.getLong(1));
			ret.setName(rs.getString(2));
			ret.setDescription(rs.getString(3));
				
			q = "select SENSORNUMBER, SENSORNAME, RECORDINGMODE, RECORDINGINTERVAL,"
				+ " TIMEOFFIRSTSAMPLE, ABSOLUTEMIN, ABSOLUTEMAX, STAT_CD"
				+ " FROM CONFIGSENSOR"
				+ " where CONFIGID = " + id;
			rs = doQuery(q);
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
				+ " and csdt.CONFIGID = " + id
				+ " order by csdt.SENSORNUMBER, dt.STANDARD";
			rs = doQuery(q);
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
				+ " where CONFIGID = " + id;
			rs = doQuery(q);
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
			
			q = "select ID, NAME, DATAORDER"
				+ " from DECODESSCRIPT"
				+ " where CONFIGID = " + id;
			rs = doQuery(q);
			while(rs.next())
			{
				int scriptId = rs.getInt(1);
				ApiConfigScript dcs = new ApiConfigScript();
				dcs.setName(rs.getString(2));
				String s = rs.getString(3);
				if (!rs.wasNull() && s.length() > 0)
					dcs.setDataOrder(s.charAt(0));
				ret.getScripts().add(dcs);
				
				q = "select ss.SENSORNUMBER, uc.TOUNITSABBR, uc.ALGORITHM,"
					+ " uc.A, uc.B, uc.C, uc.D, uc.E, uc.F"
					+ " from SCRIPTSENSOR ss, UNITCONVERTER uc"
					+ " where ss.UNITCONVERTERID = uc.ID and ss.DECODESSCRIPTID = " + scriptId;
				ResultSet rs2 = doQuery2(q);
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
					dcs.getScriptSensors().add(dcss);
				}
				
				q = "select SEQUENCENUM, LABEL, FORMAT"
					+ " from FORMATSTATEMENT"
					+ " where DECODESSCRIPTID = " + scriptId
					+ " order by SEQUENCENUM";
				rs2 = doQuery2(q);
				while(rs2.next())
				{
					ApiScriptFormatStatement dsfs = new ApiScriptFormatStatement();
					dsfs.setSequenceNum(rs2.getInt(1));
					dsfs.setLabel(rs2.getString(2));
					dsfs.setFormat(rs2.getString(3));
					dcs.getFormatStatements().add(dsfs);
				}
				
			}
			
			return ret;
		}
		catch(SQLException ex)
		{
			throw new DbIoException(module + " error in query '" + q + "': " + ex);
		}
	}

	public void writeConfig(ApiPlatformConfig config)
		throws DbIoException, WebAppException
	{
		// Check to make sure name is unique throw WebAppException if conflict
		// I.e. check for list with same name but different key.
		String q = "select ID from PLATFORMCONFIG where lower(NAME) = " 
			+ sqlString(config.getName().toLowerCase());
		if (config.getConfigId() != DbKey.NullKey.getValue())
			q = q + " and ID != " + config.getConfigId();
		ResultSet rs = doQuery(q);
		try
		{
			if (rs.next())
				throw new WebAppException(ErrorCodes.NOT_ALLOWED, 
					"Cannot write config with name '" + config.getName() 
					+ "' because another config with id=" + rs.getLong(1) 
					+ " also has that name.");
			
			if (config.getConfigId() == DbKey.NullKey.getValue())
				insert(config);
			else
				update(config);
		}
		catch (SQLException e)
		{
			throw new DbIoException("writeConfig Error while '" + q + "'");
		}
	}
		
	private void update(ApiPlatformConfig config)
		throws DbIoException
	{
		deleteSubordinates(config.getConfigId());
		String q = "update PLATFORMCONFIG set " +
			"NAME = " + sqlString(config.getName()) + ", " +
			"DESCRIPTION = " + sqlString(config.getDescription()) + ", " +
			"EQUIPMENTID = null " +
			"where ID = " + config.getConfigId();
		doModify(q);

		writeSubordinates(config);
	}
	
	private void insert(ApiPlatformConfig config)
			throws DbIoException
	{
		config.setConfigId(getKey("PLATFORMCONFIG").getValue());

		String q = "insert into PLATFORMCONFIG(ID, NAME, DESCRIPTION)"
			+ " values (" + config.getConfigId() + ", " + sqlString(config.getName())
			+ ", " + sqlString(config.getDescription()) + ")";
		doModify(q);

		writeSubordinates(config);
	}

	private void deleteSubordinates(long configId)
		throws DbIoException
	{
		String q = "delete from CONFIGSENSORDATATYPE where CONFIGID = " + configId;
		doModify(q);
		
		q = "delete from CONFIGSENSORPROPERTY where CONFIGID = " + configId;
		doModify(q);
		
		q = "delete from CONFIGSENSOR where CONFIGID = " + configId;
		doModify(q);
		
		// Chicken & egg: Have to delete unit converters used in script sensors, but
		// Can't delete UC's first because reference constraint.
		StringBuilder ucIdList = new StringBuilder();
		q = "select ss.UNITCONVERTERID from SCRIPTSENSOR ss, DECODESSCRIPT ds"
			+ " where ss.DECODESSCRIPTID = ds.ID"
			+ " and ds.CONFIGID = " + configId;
		ResultSet rs = doQuery(q);
		try
		{
			while(rs.next())
				ucIdList.append((ucIdList.length() == 0 ? "" : ",") + rs.getInt(1));
		}
		catch (SQLException e)
		{
			throw new DbIoException("ConfigDAO.deleteSubordinates: Error in query '" + q + "': " + e);
		}
		
		q ="delete from SCRIPTSENSOR ss"
			+ " where ss.DECODESSCRIPTID IN ("
				+ "select ds.ID from DECODESSCRIPT ds"
				+ " where ds.CONFIGID = " + configId + ")";
		doModify(q);
		if (ucIdList.length() > 0)
		{
			q = "delete from UNITCONVERTER uc where uc.ID in (" + ucIdList.toString() + ")";
System.out.println(q);
			doModify(q);
		}
		
		q ="delete from UNITCONVERTER uc"
			+ " where uc.ID IN ("
				+ "select ss.UNITCONVERTERID from SCRIPTSENSOR ss, DECODESSCRIPT ds"
				+ " where ss.DECODESSCRIPTID = ds.ID"
				+ " and ds.CONFIGID = " + configId + ")";
		doModify(q);
		
		q ="delete from FORMATSTATEMENT fs"
			+ " where fs.DECODESSCRIPTID IN ("
				+ "select ds.ID from DECODESSCRIPT ds"
				+ " where ds.CONFIGID = " + configId + ")";
		doModify(q);
		
		q ="delete from DECODESSCRIPT"
			+ " where CONFIGID = " + configId;
		doModify(q);
	}
	
	private void writeSubordinates(ApiPlatformConfig config)
		throws DbIoException
	{
		DecodesDataTypeDAO dtDao = new DecodesDataTypeDAO(db);
		dtDao.setManualConnection(getConnection());
		for(ApiConfigSensor dcs : config.getConfigSensors())
		{
			Double min = dcs.getAbsoluteMin();
			Double max = dcs.getAbsoluteMax();
			String q = "insert into CONFIGSENSOR(CONFIGID, SENSORNUMBER, SENSORNAME, RECORDINGMODE,"
				+ " RECORDINGINTERVAL,"
				+ " TIMEOFFIRSTSAMPLE, ABSOLUTEMIN, ABSOLUTEMAX, STAT_CD)"
				+ " values("
				+ config.getConfigId() + ", " 
				+ dcs.getSensorNumber() + ", " 
				+ sqlString(dcs.getSensorName()) + ", " 
				+ sqlString("" + dcs.getRecordingMode()) + ", "
				+ dcs.getRecordingInterval() + ", "
				+ dcs.getTimeOfFirstSample() + ", "
				+ (min == null ? "null" : min) + ", "
				+ (max == null ? "null" : max) + ", "
				+ sqlString(dcs.getUsgsStatCode()) + ")";
			doModify(q);
			
			for (String dtstd : dcs.getDataTypes().keySet())
			{
				String dtcd = dcs.getDataTypes().get(dtstd);
				Long id = dtDao.lookup(dtstd, dtcd);
				if (id == null)
					id = dtDao.create(dtstd, dtcd);
				q = "insert into CONFIGSENSORDATATYPE(CONFIGID, SENSORNUMBER, DATATYPEID)"
					+ " values(" + config.getConfigId() + ", " 
					+ dcs.getSensorNumber() + ", " + id + ")";
				doModify(q);
			}
		
			for(Object k : dcs.getProperties().keySet())
			{
				String pn = (String)k;
				String pv = dcs.getProperties().getProperty(pn);
				q = "insert into CONFIGSENSORPROPERTY(CONFIGID, SENSORNUMBER, PROP_NAME, PROP_VALUE)"
					+ " values (" + config.getConfigId() + ", " + dcs.getSensorNumber()
					+ ", " + sqlString(pn) + ", " + sqlString(pv) + ")";
				doModify(q);
			}
		}
		
		dtDao.close();
		
		for(ApiConfigScript acs : config.getScripts())
		{
			long scriptId = getKey("DECODESSCRIPT").getValue();
			String q = "insert into DECODESSCRIPT(ID, CONFIGID, NAME, DATAORDER)"
				+ " values(" + scriptId + ", "
				+ config.getConfigId() + ", "
				+ sqlString(acs.getName()) + ", "
				+ sqlString("" + acs.getDataOrder()) + ")";
			doModify(q);
			
			int seq = 0;
			for(ApiScriptFormatStatement asfs : acs.getFormatStatements())
			{
				q = "insert into FORMATSTATEMENT(DECODESSCRIPTID, SEQUENCENUM, LABEL, FORMAT)"
					+ " values(" + scriptId + ", "
					+ seq++ + ", "
					+ sqlString(asfs.getLabel()) + ", "
					+ sqlString(asfs.getFormat()) + ")";
				doModify(q);
			}
			
			for(ApiConfigScriptSensor acss : acs.getScriptSensors())
			{
				long ucid = getKey("UNITCONVERTER").getValue();
				
				q = "insert into UNITCONVERTER(ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, "
					+ "A, B, C, D, E, F) values("
					+ ucid + ", "
					+ sqlString("raw") + ", " + sqlString(acss.getUnitConverter().getToAbbr()) + ", "
					+ sqlString(acss.getUnitConverter().getAlgorithm())	+ ", "
					+ acss.getUnitConverter().getA() + ", "
					+ acss.getUnitConverter().getB() + ", "
					+ acss.getUnitConverter().getC() + ", "
					+ acss.getUnitConverter().getD() + ", "
					+ acss.getUnitConverter().getE() + ", "
					+ acss.getUnitConverter().getF() + ")";
System.out.println("configDao.writeSubordinates: " + q);
				doModify(q);
				
				q = "insert into SCRIPTSENSOR(DECODESSCRIPTID, SENSORNUMBER, UNITCONVERTERID)"
					+ " values(" + scriptId + ", " + acss.getSensorNumber() + ", " + ucid + ")";
System.out.println("configDao.writeSubordinates: " + q);
				doModify(q);
			}
		}		
	}

	public void deleteConfig(long configId)
		throws DbIoException, WebAppException
	{
		deleteSubordinates(configId);
		String q = "delete from PLATFORMCONFIG where ID = " + configId;
		doModify(q);
	}


}
