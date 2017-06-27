/*
* $Id$
*/
package decodes.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.Iterator;

import opendcs.dai.PropertiesDAI;

import ilex.util.Logger;
import ilex.util.PropertiesUtil;

import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.ConfigSensor;
import decodes.db.DatabaseException;
import decodes.db.PlatformConfig;
import decodes.db.PlatformConfigList;
import decodes.db.DataType;
import decodes.tsdb.DbIoException;

/**
* This class is used to read and write the PlatformConfig objects
* from the SQL database.  It reads and writes these SQL database
* tables:
* <ul>
*   <li>PlatformConfig</li>
*   <li>ConfigSensor</li>
*   <li>ConfisSensorProperty</li>
* </ul>
*/
public class ConfigListIO extends SqlDbObjIo
{
	/**
	* Transient reference to the PlatformConfigList that we're currently 
	* operating on.
	*/
	PlatformConfigList _pcList;

	/**
	* This is used to read and write the DecodesScripts.
	*/
	DecodesScriptIO _decodesScriptIO;

	/**
	* Constructor.
	*/
	public ConfigListIO(SqlDatabaseIO dbio, DecodesScriptIO dsio)
	{
		super(dbio);
		_decodesScriptIO = dsio;
	}

	/**
	* Read the PlatformConfigList from the SQL database.
	* @param pcList PlatformConfigList to populate
	*/
	public void read(PlatformConfigList pcList)
		throws DatabaseException, SQLException
	{
		Logger.instance().log(Logger.E_DEBUG1,"Reading PlatformConfigs...");
		_pcList = pcList;

		// Read entire PlatformConfig table & convert each entry to an object.
		Statement stmt = createStatement();

		String q = "SELECT id, name, description, equipmentId "
		 	+ " from PlatformConfig";

		Logger.instance().debug3("Executing '" + q + "'");
		ResultSet rs = stmt.executeQuery(q);
		while(rs != null && rs.next())
		{
			DbKey id = DbKey.createDbKey(rs, 1);
			PlatformConfig pc = _pcList.getById(id);
			if (pc == null)
			{
				pc = new PlatformConfig();
				pc.setId(id);
			}

			pc.configName = rs.getString(2);
			pc.description = rs.getString(3);
			if (pc.description == null)
				pc.description = "";

			DbKey equipId = DbKey.createDbKey(rs, 4);
			if (!rs.wasNull())
			{
				pc.equipmentModel = 
					pcList.getDatabase().equipmentModelList.getById(equipId);
			}

			_pcList.add(pc);
		}

		stmt.close();
		Logger.instance().debug1("PlatformConfigs done, read "
			+ _pcList.size() + " configs.");
	}

	/**
	* Uses the data in a single row of a ResultSet to populate a
	* PlatformConfig object.  The ID is used to determine which
	* PlatformConfig object should get the data.
	* If the PlatformConfig with that ID is already
	* in memory, then it is used.  Otherwise, a new PlatformConfig is
	* created.
	* The ResultSet should have already been checked to see that the
	* current row contains valid data.
	* @param id the database ID
	* @param rs the JDBC result set
	*/
	private PlatformConfig putConfig(DbKey id, ResultSet rs)
		throws DatabaseException, SQLException
	{
		if (_pcList == null)
			_pcList = Database.getDb().platformConfigList;

		PlatformConfig pc = _pcList.getById(id);

		if (pc == null)
		{
			pc = new PlatformConfig();
			pc.setId(id);
		}

		pc.configName = rs.getString(2);
		pc.description = rs.getString(3);
		if (pc.description == null)
			pc.description = "";

		DbKey equipId = DbKey.createDbKey(rs, 4);
		boolean hasEquip = !rs.wasNull();

		_pcList.add(pc);

		// Now we want to get the EquipmentModel for each Platform
		// Config, if there is one.
		if (hasEquip) {
			pc.equipmentModel =
				_dbio._equipmentModelListIO.getEquipmentModel(
					equipId, _pcList.getDatabase());
		}

		readConfigSensors(id, pc);

		_decodesScriptIO.readDecodesScripts(pc);

		return pc;
	}

	/**
	* Read all the ConfigSensors associated with one PlatformConfig.
	* @param platformConfigId database surrogate platform config ID
	* @param pc the PlatformConfig to populate
	*/
	private void readConfigSensors(DbKey platformConfigId, PlatformConfig pc)
		throws DatabaseException, SQLException
	{
		Statement stmt = createStatement();
		String q = 
			"SELECT * FROM ConfigSensor WHERE ConfigId = " + platformConfigId;
		ResultSet rs = stmt.executeQuery(q);

		if (rs != null) 
		{
			while (rs.next()) 
			{
				int sensorNum = rs.getInt(2);
				String sensorName = rs.getString(3);

				ConfigSensor cs = new ConfigSensor(pc, sensorNum);
				cs.sensorName = sensorName;

				DbKey dataTypeId = Constants.undefinedId;
				String recordingMode;
				int recInterval;
				int timeSecs;
				DbKey equipId = Constants.undefinedId;
				boolean hasEquip;
				double absMin;
				boolean hasAbsMin;
				double absMax;
				boolean hasAbsMax;
				String usgsStatCode = null;

				if (getDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_6)
				{
					dataTypeId = DbKey.createDbKey(rs, 4);
					recordingMode = rs.getString(5);
					recInterval = rs.getInt(6);
					Time timeOfFirstSample = rs.getTime(7);
					timeSecs = timeOfFirstSample.getHours() * 3600 +
						timeOfFirstSample.getMinutes() * 60 +
						timeOfFirstSample.getSeconds();
					equipId = DbKey.createDbKey(rs, 8);
					hasEquip = !rs.wasNull();
					absMin = rs.getFloat(9);
					hasAbsMin = !rs.wasNull();
					absMax = rs.getFloat(10);
					hasAbsMax = !rs.wasNull();
				}
				else // DB Version is 6 or later
				{
					// DataType ID no longer stored in ConfigSensor
					recordingMode = rs.getString(4);
					recInterval = rs.getInt(5);
					timeSecs = rs.getInt(6);
					if (rs.wasNull())
						timeSecs = 0;
					equipId = DbKey.createDbKey(rs, 7);
					hasEquip = !rs.wasNull();
					absMin = rs.getFloat(8);
					hasAbsMin = !rs.wasNull();
					absMax = rs.getFloat(9);
					hasAbsMax = !rs.wasNull();
					
					if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_7)
						usgsStatCode = rs.getString(10);
				}

				if (hasEquip) {
					cs.equipmentModel =
						_dbio._equipmentModelListIO.getEquipmentModel(
							equipId, _pcList.getDatabase());
				}

				if (hasAbsMin) cs.absoluteMin = absMin;
				if (hasAbsMax) cs.absoluteMax = absMax;
				cs.recordingInterval = recInterval;
				cs.recordingMode = recordingMode.charAt(0);
				cs.timeOfFirstSample = timeSecs;
				cs.setUsgsStatCode(usgsStatCode);

				if (getDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_6)
					cs.addDataType(
						cs.getDatabase().dataTypeSet.getById(dataTypeId));

				pc.addSensor(cs);

			}
			// Read the ConfigSensorProperty table
			readConfigSensorProps(pc, stmt);
			if (getDatabaseVersion()  >= DecodesDatabaseVersion.DECODES_DB_6)
			{
				// DB Version 6 or later, read ConfigSensorDataType table.
				Statement dtstmt = createStatement();
				ResultSet dtrs = dtstmt.executeQuery(
					"SELECT sensorNumber, dataTypeId, standard, code "
					+ "FROM ConfigSensorDataType, DataType "
					+ "WHERE ConfigId = " + platformConfigId
					+ "AND ConfigSensorDataType.dataTypeId = DataType.id");
				while (dtrs.next()) 
				{
					int sensorNum = dtrs.getInt(1);
					DbKey dataTypeId = DbKey.createDbKey(dtrs, 2);
					String std = dtrs.getString(3);
					String code = dtrs.getString(4);
					DataType dt = 
						pc.getDatabase().dataTypeSet.get(dataTypeId, std, code);
					ConfigSensor cs = pc.getSensor(sensorNum);
					if (cs != null)
						cs.addDataType(dt);
				}
				dtstmt.close();
				
			}
		}
		stmt.close();
	}

//	/**
//	* This reads the ConfigSensorProperty table to get the properties
//	* for a particular sensor which belongs to a particular PlatformConfig.
//	* @param platformConfigId database surrogate platform config ID
//	* @param sensorNum the sensor number
//	* @param cs the ConfigSensor to populate
//	*/
//	private void readConfigSensorProps(DbKey platformConfigId,
//		int sensorNum, ConfigSensor cs)
//		throws SQLException
//	{
//		PropertiesDAI propertiesDAO = this._dbio.makePropertiesDAO();
//	
//		try
//		{
//			propertiesDAO.readProperties("ConfigSensorProperty", "configId", 
//				"sensorNumber", platformConfigId, sensorNum, cs.getProperties());
//		}
//		catch (DbIoException e)
//		{
//			throw new SQLException(e.getMessage());
//		}
//		finally
//		{
//			propertiesDAO.close();
//		}
//		String s = PropertiesUtil.getIgnoreCase(cs.getProperties(), "StatisticsCode");
//		if (s != null)
//		{
//			cs.setUsgsStatCode(s);
//			PropertiesUtil.rmIgnoreCase(cs.getProperties(), "StatisticsCode");
//		}
//	}
	
	private void readConfigSensorProps(PlatformConfig cfg, Statement stmt)
		throws SQLException
	{
		String q = "select * from ConfigSensorProperty where configId = " + cfg.getKey();
		ResultSet rs = stmt.executeQuery(q);
		while(rs != null && rs.next())
		{
			int sensorNum = rs.getInt(2);
			String propName = rs.getString(3);
			String propValue = rs.getString(4);
			
			ConfigSensor cs = cfg.getSensor(sensorNum);
			if (cs != null)
				cs.setProperty(propName, propValue);
		}
	}

	/**
	* This reads one PlatformConfig from the database, including all its
	* ancillary data (ConfigSensors, etc.)  If a PlatformConfig with the
	* desired ID number is already in memory, this re-reads its data.
	* This returns a reference to the PlatformConfig.
	* @param id the database ID
	*/
	public PlatformConfig readConfig(DbKey id)
		throws DatabaseException, SQLException
	{
		Statement stmt = null;
		
		try
		{
			stmt = createStatement();
		
			String q = "SELECT id, name, description, equipmentId " +
					   "FROM PlatformConfig WHERE ID = " + id;
			
			debug3("Executing '" + q + "'");
			ResultSet rs = stmt.executeQuery(q);
	
			if (rs == null || !rs.next())
				throw new DatabaseException(
					"No PlatformConfig found with ID " + id);
	
			PlatformConfig ret = putConfig(id, rs);
			return ret;
		}
		finally
		{
			if (stmt != null)
				try { stmt.close(); } catch(Exception ex) {}
		}
	}


	/**
	* This returns a reference to a PlatformConfig object, given its
	* ID number.  If the PlatformConfig is not yet in memory, this
	* attempts to read it from the database.
	* @param id the database ID
	*/
	public PlatformConfig getConfig(DbKey id)
		throws DatabaseException, SQLException
	{
		if (_pcList == null)
			_pcList = Database.getDb().platformConfigList;

		PlatformConfig pc = _pcList.getById(id);
		if (pc != null)
			return pc;

		return readConfig(id);
	}

	/**
	* Write a PlatformConfig out to the SQL database.
	* @param pc the PlatformConfig to write
	*/
	public void write(PlatformConfig pc)
		throws DatabaseException, SQLException
	{
		//System.out.println("	ConfigListIO.write(pc)");

		if (pc.idIsSet()) 
			update(pc);
		else
		{
			DbKey id = name2id(pc.configName);
			if (id != null && !id.isNull())
			{
				pc.setId(id);
				update(pc);
			}
			else
				insert(pc);
		}
	}

	/**
	* Update an already existing PlatformConfig in the SQL database.
	* @param pc the PlatformConfig to update
	*/
	public void update(PlatformConfig pc)
		throws DatabaseException, SQLException
	{
		String q =
			"SELECT Name from PlatformConfig where Name = "+
							sqlString(pc.configName);
		Statement stmt = createStatement();
		debug3("Executing '" + q + "'");
		ResultSet rs = stmt.executeQuery(q);
		if (rs == null || !rs.next()) {
			q =
				"UPDATE PlatformConfig SET " +
			  	"Name = " + sqlString(pc.configName) + ", " +
			  	"Description = " + sqlString(pc.description) + ", " +
			  	"EquipmentID = " + sqlOptHasId(pc.equipmentModel) + " " +
				"WHERE id = " + pc.getId();
		} else {
			q = "UPDATE PlatformConfig SET " +
			  "Description = " + sqlString(pc.description) + ", " +
			  "EquipmentID = " + sqlOptHasId(pc.equipmentModel) + " " +
			"WHERE id = " + pc.getId();
		}
		stmt.close();
		executeUpdate(q); 

		// Now update the ConfigSensors.  Take the easy road, and first
		// delete them all, then re-insert them.

		deleteConfigSensors(pc);
		insertConfigSensors(pc);

		// Now do the DecodesScripts, the same way.
		_decodesScriptIO.deleteDecodesScripts(pc);
		_decodesScriptIO.insertDecodesScripts(pc);
	}

	public PlatformConfig newPlatformConfig(PlatformConfig pc, String deviceId, String originator) 
		throws DatabaseException, SQLException
	{
		int seqNo;
		int maxSeq = 0;
		String prefix = deviceId+"-"+originator+"-";
		String q =
			"SELECT name FROM PlatformConfig where name like "
			+ sqlReqString(prefix+"%");

		Statement stmt = createStatement();
		ResultSet rs = stmt.executeQuery(q);
		while(rs != null && rs.next())
		{
			String seq = rs.getString(1).substring(prefix.length());
			// Added this to make sure seq is an integer dds -- 12JAN09
			if (seq.matches("[+-]?[0-9]+")) {
				seqNo = Integer.parseInt(seq);
				if ( seqNo > maxSeq )
					maxSeq = seqNo;
			}
		}
		maxSeq++;	
		int[] seqNumber = new int[maxSeq];
		for (int i = 0; i < maxSeq; i++ ) {
			seqNumber[i] = 0;
		}
		q =
			"SELECT name FROM PlatformConfig where name like "
			+ sqlReqString(prefix+"%");
    	rs = stmt.executeQuery(q);
    	while(rs != null && rs.next())
		{
			String seq = rs.getString(1).substring(prefix.length());
			// Added this to make sure seq is an integer dds -- 12JAN09
			if (seq.matches("[+-]?[0-9]+")) {
				seqNo = Integer.parseInt(seq) - 1;
				seqNumber[seqNo] = 1;
			}
		}
		int nextSeq = maxSeq;
		for (int i = 0; i < maxSeq; i++ ) {
			if ( seqNumber[i] == 0 ) {
				nextSeq = i+1;
				break;
			}
		}
		String newSeq = String.format("%03d",nextSeq);
		String newName=prefix+newSeq;
		stmt.close();
		if ( pc == null ) 
			pc = new PlatformConfig(newName);
		else
			pc.configName = newName;
		insert(pc);
		_dbio.commit();
		return pc;
	}
	/**
	* Insert a new PlatformConfig into the SQL database.
	* @param pc the PlatformConfig to insert
	*/
	public void insert(PlatformConfig pc)
		throws DatabaseException, SQLException
	{
		DbKey id = getKey("PlatformConfig");
		pc.setId(id);
		pc.getDatabase().platformConfigList.add(pc);

		String q =
			"INSERT INTO PlatformConfig(id, name, description, equipmentid) "
			+ "VALUES (" +
			  id + ", " +
			  sqlString(pc.configName) + ", " +
			  sqlString(pc.description) + ", " +
			  sqlOptHasId(pc.equipmentModel) +
			")";
	
		executeUpdate(q);

		// Insert the ConfigSensors associated with that PlatformConfig.

		insertConfigSensors(pc);

		// Insert the DecodesScripts
		_decodesScriptIO.insertDecodesScripts(pc);
	}

	/**
	* Insert all the ConfigSensors belonging to one PlatformConfig.
	* @param pc the PlatformConfig containing sensors to insert
	*/
	public void insertConfigSensors(PlatformConfig pc)
		throws DatabaseException, SQLException
	{
		Iterator<ConfigSensor> i = pc.getSensors();
		while (i.hasNext()) {
			insert(i.next());
		}
	}

	/**
	* Insert one ConfigSensor.
	* @param cs the ConfigSensor to insert
	*/
	public void insert(ConfigSensor cs)
		throws DatabaseException, SQLException
	{
		if (cs.sensorName == null || cs.sensorName.trim().length() == 0)
		{
			warning("PlatformConfig '" + cs.platformConfig.getName() + "' sensor number "
				+ cs.sensorNumber + " is missing required sensorName. Set to UNKNOWN");
			cs.sensorName = "UNKNOWN";
		}
		if (getDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_6)
		{
			DataType dt = cs.getDataType();
			if (dt == null)
			{
				Logger.instance().log(Logger.E_FAILURE,
					"Config '" + cs.platformConfig.configName + "' sensor "
					+cs.sensorNumber 
					+" missing DataType -- Assigned to data type UNKNOWN");
				cs.addDataType(DataType.getDataType(Constants.datatype_SHEF,
					"UNKNOWN"));
			}

			// If this ConfigSensor's data-type is new, we need to first write
			// it to the database
			if (!dt.idIsSet())
				_dbio.writeDataType(dt);

//			DbKey pcId = cs.platformConfig.getId();

			// Convert the ConfigSensor's time-of-first-sample member (which
			// is in seconds) into a JDBC Time type
			int tofs = cs.timeOfFirstSample;
			int hours = tofs / 3600;
			tofs -= hours * 3600;
			int minutes = tofs / 60;
			tofs -= minutes * 60;

			String tofs_Str = "'" + hours + ":" + minutes + ":" + tofs + "'";

			String q = "INSERT INTO ConfigSensor VALUES (" +
					 cs.platformConfig.getId() + ", " +
					 cs.sensorNumber + ", " +
					 sqlReqString(cs.sensorName) + ", " +
					 dt.getId() + ", " +
					 "'" + cs.recordingMode + "', " +
					 cs.recordingInterval + ", " +
					 tofs_Str + ", " +
					 sqlOptHasId(cs.equipmentModel) + ", " +
					 sqlOptDouble(cs.absoluteMin) + ", " +
					 sqlOptDouble(cs.absoluteMax) +
				   ")";

			executeUpdate(q);
		}
		else
		{
			String statCode = cs.getUsgsStatCode();
			if (statCode == null)
			{
				statCode = PropertiesUtil.rmIgnoreCase(
					cs.getProperties(), "StatisticsCode");
			}
			String q = "INSERT INTO ConfigSensor VALUES (" +
				cs.platformConfig.getId() + ", " +
				cs.sensorNumber + ", " +
				sqlReqString(cs.sensorName) + ", " +
				"'" + cs.recordingMode + "', " +
				cs.recordingInterval + ", " +
				cs.timeOfFirstSample + ", " +
				sqlOptHasId(cs.equipmentModel) + ", " +
				sqlOptDouble(cs.absoluteMin) + ", " +
				sqlOptDouble(cs.absoluteMax);

			if (getDatabaseVersion() >= 7)
				q = q + ", " + sqlOptString(cs.getUsgsStatCode());
			else if (statCode != null)
				cs.getProperties().setProperty("StatisticsCode", statCode);

			q += ")";
			executeUpdate(q);

			for(Iterator<DataType> dtit = cs.getDataTypes(); dtit.hasNext(); )
			{
				DataType dt = dtit.next();
				// If this data-type is new, first write it to the database
				if (!dt.idIsSet())
					_dbio.writeDataType(dt);
				q = "INSERT INTO ConfigSensorDataType VALUES (" +
					cs.platformConfig.getId() + ", " +
					cs.sensorNumber + ", " +
					dt.getId() + " )";
				executeUpdate(q);
			}
		}

		insertProperties(cs);
	}

	/**
	* Insert all the ConfigSensorProperty records associated with a
	* ConfigSensor object.
	* @param cs the ConfigSensor to insert properties for
	*/
	private void insertProperties(ConfigSensor cs)
		throws DatabaseException, SQLException
	{
		PropertiesDAI propertiesDAO = _dbio.makePropertiesDAO();
		try
		{
			propertiesDAO.writeProperties("ConfigSensorProperty", "configId", "sensorNumber", 
				cs.platformConfig.getId(), cs.sensorNumber, cs.getProperties());
		}
		catch (DbIoException e)
		{
			throw new DatabaseException(e.getMessage());
		}
		finally
		{
			propertiesDAO.close();
		}
	}

	/**
	* Delete a PlatformConfig from the database.  This also deletes all
	* the ConfigSensor and ConfigSensorProperty rows that belong to this
	* PlatformConfig.
	* This also deletes the DecodesScripts, FormatStatements, and
	* ScriptSensors belonging to this PlatformConfig.
	* @param pc the PlatformConfig to delete
	*/
	public void delete(PlatformConfig pc)
		throws DatabaseException, SQLException
	{
		deleteConfigSensors(pc);
		_decodesScriptIO.deleteDecodesScripts(pc);
		String q =
			"DELETE FROM PlatformConfig WHERE ID = " + pc.getId();
		executeUpdate(q);
	}

	/**
	  Deletes the config sensors for a particular PlatformConfig.
	  Also deletes any properties associated with those ConfigSensors.
	  Also deletes any ConfigSensorDataType records.
	* @param pc the PlatformConfig to delete sensors from
	*/
	public void deleteConfigSensors(PlatformConfig pc)
		throws DatabaseException, SQLException
	{
		String q = "DELETE FROM ConfigSensorProperty WHERE ConfigId = " + pc.getId();
		tryUpdate(q);

		if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_6)
		{
			q = "DELETE FROM ConfigSensorDataType WHERE ConfigId = " + pc.getId();
			tryUpdate(q);
		}
		
		q = "DELETE FROM ConfigSensor WHERE ConfigId = " + pc.getId();
		tryUpdate(q);

	}

	/**
	  Converts a platform config name to numeric ID, doing database lookup
	  if necessary.
	  @return surrogate key ID
	*/
	private DbKey name2id(String pcname)
		throws DatabaseException, SQLException
	{
		String q = 
			"SELECT id, name FROM PlatformConfig where name = "
			+ sqlReqString(pcname);

		Statement stmt = createStatement();
		ResultSet rs = stmt.executeQuery(q);

		DbKey ret = Constants.undefinedId;
		if (rs != null && rs.next())
			ret = DbKey.createDbKey(rs, 1);

		stmt.close();
		return ret;
	}
}
