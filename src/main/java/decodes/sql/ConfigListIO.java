/*
* $Id$
*/
package decodes.sql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import opendcs.dai.PropertiesDAI;

import ilex.util.Logger;
import ilex.util.PropertiesUtil;

import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.ConfigSensor;
import decodes.db.DatabaseException;
import decodes.db.DecodesScript;
import decodes.db.DecodesScriptException;
import decodes.db.DecodesScriptReader;
import decodes.db.FormatStatement;
import decodes.db.PlatformConfig;
import decodes.db.PlatformConfigList;
import decodes.db.ScriptSensor;
import decodes.db.UnitConverterDb;
import decodes.db.DataType;
import decodes.tsdb.DbIoException;
import opendcs.dao.DaoBase;

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
	private PlatformConfigList _pcList;
	
	/**
	* This is a reference to the UnitConverterIO object that handles I/O
	* for the UnitConverters.
	*/
	private UnitConverterIO _unitConverterIO;


	/**
	* Constructor.
	*/
	public ConfigListIO(SqlDatabaseIO dbio, UnitConverterIO ucio)
	{
		super(dbio);
		_unitConverterIO = ucio;
	}
	
	@Override
	public void setConnection(Connection conn)
	{
		super.setConnection(conn);
		_unitConverterIO.setConnection(conn);
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

		readDecodesScripts(pc);

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
	private void update(PlatformConfig pc)
		throws DatabaseException, SQLException
	{
		String q =
			"SELECT Name from PlatformConfig where Name = "+
							sqlString(pc.configName);
		Statement stmt = createStatement();
		debug3("Executing '" + q + "'");
		ResultSet rs = stmt.executeQuery(q);
		
		String desc = pc.description;
		if (desc.length() > 399)
			desc = desc.substring(0, 399);

		if (rs == null || !rs.next()) 
		{
			q =
				"UPDATE PlatformConfig SET " +
			  	"Name = " + sqlString(pc.configName) + ", " +
			  	"Description = " + sqlString(desc) + ", " +
			  	"EquipmentID = " + sqlOptHasId(pc.equipmentModel) + " " +
				"WHERE id = " + pc.getId();
		}
		else
		{
			q = "UPDATE PlatformConfig SET " +
			  "Description = " + sqlString(desc) + ", " +
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
		deleteDecodesScripts(pc);
		insertDecodesScripts(pc);
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
		return pc;
	}
	/**
	* Insert a new PlatformConfig into the SQL database.
	* @param pc the PlatformConfig to insert
	*/
	private void insert(PlatformConfig pc)
		throws DatabaseException, SQLException
	{
		DbKey id = getKey("PlatformConfig");
		pc.setId(id);
		pc.getDatabase().platformConfigList.add(pc);
		
		String desc = pc.description;
		if (desc.length() > 399)
			desc = desc.substring(0, 399);

		String q =
			"INSERT INTO PlatformConfig(id, name, description, equipmentid) "
			+ "VALUES (" +
			  id + ", " +
			  sqlString(pc.configName) + ", " +
			  sqlString(desc) + ", " +
			  sqlOptHasId(pc.equipmentModel) +
			")";
	
		executeUpdate(q);

		// Insert the ConfigSensors associated with that PlatformConfig.

		insertConfigSensors(pc);

		// Insert the DecodesScripts
		insertDecodesScripts(pc);
	}

	/**
	* Insert all the ConfigSensors belonging to one PlatformConfig.
	* @param pc the PlatformConfig containing sensors to insert
	*/
	private void insertConfigSensors(PlatformConfig pc)
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
	private void insert(ConfigSensor cs)
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
		((DaoBase)propertiesDAO).setManualConnection(connection);
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
		deleteDecodesScripts(pc);
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
	private void deleteConfigSensors(PlatformConfig pc)
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
	
	//============================================================================
	// DECODES Script Read/Write Methods
	//============================================================================

	/**
	* Read all the DecodesScripts associated with one PlatformConfig.
	* The PlatformConfig must have had its SQL database ID set.
	* @param pc the PlatformConfig that owns the script
	*/
	private void readDecodesScripts(PlatformConfig pc)
		throws DatabaseException, SQLException
	{
		Statement stmt = createStatement();
		String q = "SELECT * FROM DecodesScript WHERE ConfigId = " + pc.getId();
		
		//debug3("Executing '" + q + "'");
		ResultSet rs = stmt.executeQuery(q);

		if (rs != null) 
		{
			while (rs.next()) 
				ingestRow(rs, pc);
		}

		stmt.close();
	}

	/**
	* This "ingests" the information about a DecodesScript from one row
	* of a ResultSet.
	* @param pcId the platform config ID
	* @param rs  the JDBC result set
	* @param pc the PlatformConfig that owns the script
	*/
	private void ingestRow(ResultSet rs, PlatformConfig pc)
		throws DatabaseException, SQLException
	{
		DbKey id = DbKey.createDbKey(rs, 1);
		String name = rs.getString(3);
		String type = rs.getString(4);
		char dataOrder = Constants.dataOrderUndefined;
		if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_6)
		{
			String s = rs.getString(5);
			if (s != null && s.length() > 0)
				dataOrder = s.charAt(0);
		}
		try
		{
			DecodesScript ds = DecodesScript.from(new SQLDecodesScriptReader(connection(), id))
											.scriptName(name)
											.build();
			ds.setDataOrder(dataOrder);
			ds.setId(id);
			//readFormatStatements(ds);
			readScriptSensors(ds);
			ds.scriptType = type;
			pc.addScript(ds);
		}
		catch (DecodesScriptException | IOException ex)
		{
			throw new DatabaseException("Unable to read decodes script (" + name + ") from database", ex);
		}
	}

	/**
	* Inserts all the DecodesScripts associated with one PlatformConfig
	* into the database.
	* The PlatformConfig must have had its SQL database ID set.
	* @param pc the PlatformConfig
	*/
	private void insertDecodesScripts(PlatformConfig pc)
		throws DatabaseException, SQLException
	{
		DbKey pcId = pc.getId();
		_dbio._unitConverterIO.setContext("Platform Config " + pc.getName());
		//System.out.println("		" +
		//	"DecodesScriptIO.insertDecodesScripts(pc==" + pcId + ")");

		Vector<DecodesScript> v = pc.decodesScripts;
		for (int i = 0; i < v.size(); ++i) {
			insert(v.get(i), pcId);
		}
	}

	//----------------------------------------------------------------------
	/**
	* This inserts a single DecodesScript into the database, using the
	* given PlatformConfig ID number.  The DecodesScript object may or
	* may not have had its SQL database ID number set.  If not, then it
	* is assigned one.
	* <p>
	*   This also inserts all the FormatStatements and ScriptSensors
	*   associated with the DecodesScript.
	* </p>
	* @param ds the DecodesScript
	* @param pcId the database ID
	*/
	private void insert(DecodesScript ds, DbKey pcId)
		throws DatabaseException, SQLException
	{
		DbKey dsKey;
		if (ds.idIsSet())
			dsKey = ds.getId();
		else
		{
			dsKey = getKey("DecodesScript");
			ds.setId(dsKey);
		}

		//System.out.println("		  " +
		//	"DecodesScriptIO.insert(ds #" + id + ", pcId == " + pcId + ")");

		String q;
		if (getDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_6)
		{
			q = "INSERT INTO DecodesScript VALUES (" +
				 dsKey + ", " +
				 pcId + ", " +
				 sqlReqString(ds.scriptName) + ", " +
				 sqlReqString(ds.scriptType) +
			   ")";
			if (ds.getDataOrder() != Constants.dataOrderUndefined)
				Logger.instance().log(Logger.E_WARNING,
					"DecodesScript.dataOrder not supported in database version "
					+ getDatabaseVersion() + " -- please see DECODES manual"
					+ " for instructions on upgrading your SQL database.");
		}
		else
			q = "INSERT INTO DecodesScript VALUES (" +
				 dsKey + ", " +
				 pcId + ", " +
				 sqlReqString(ds.scriptName) + ", " +
				 sqlReqString(ds.scriptType) + ", " +
				 "'" + ds.getDataOrder() + "'" +
			   ")";
		executeUpdate(q);

		insertFormatStatements(ds);
		insertScriptSensors(ds);
	}

	/**
	* Delete all the DecodesScripts belonging to a particular PlatformConfig.
	* All of the DecodesScripts belonging to this PlatformConfig must have
	* already had their SQL database ID values set.
	* @param pc the PlatformConfig
	*/
	private void deleteDecodesScripts(PlatformConfig pc)
		throws DatabaseException, SQLException
	{
		// First delete all unit converters belonging to script sensors 
		// belonging to scripts belonging to this config.

		Statement stmt = createStatement();

// MJM 2006 10/20 the EXISTS clause works but takes a very long time
// on Postgres if there are a large number of configs/scripts/UCs.
// Therefore I replaced with a two-step approach: Get the IDs and then
// delete them using an IN clause.
		String q = "select unitconverterid FROM ScriptSensor,DecodesScript"
			+ " WHERE ScriptSensor.decodesScriptId = DecodesScript.id"
			+ " AND DecodesScript.configId = " + pc.getId();
		debug3("Executing '" + q + "'");
		ResultSet rs = stmt.executeQuery(q);
		int n=0;
		StringBuilder inClause = new StringBuilder(" IN (");
		if (rs != null) 
		{
			while (rs.next()) 
			{
				if (n++ > 0)
					inClause.append(", ");
				inClause.append("" + rs.getLong(1));
			}
			inClause.append(")");
		}
		stmt.close();

//		// This syntax works on Postgres but not Oracle 
//		//		String q = "DELETE from UnitConverter "
//		//			+ "where id = ScriptSensor.UnitConverterId "
//		//			+ "and ScriptSensor.decodesScriptId = DecodesScript.id "
//		//			+ "and DecodesScript.configId = " + pc.getId();
//		//========================================================
//		// Changed to the following which works on both:
//		String q = "DELETE FROM UnitConverter "
//			+ "WHERE EXISTS (SELECT 'x' "
//			+ "FROM ScriptSensor,DecodesScript "
//			+ "WHERE UnitConverter.id = ScriptSensor.UnitConverterId "
//			+ "and ScriptSensor.decodesScriptId = DecodesScript.id "
//			+ "and DecodesScript.configId = " + pc.getId() + ")";
//		tryUpdate(q);

		// Delete all script sensors from any scripts for this config.

		// MJM -- likewise -- mods for Oracle compatibility:
		//	q = "DELETE from ScriptSensor where DecodesScriptId = "
		//		+ "DecodesScript.id and DecodesScript.configId = "
		//		+ pc.getId();
		q = "DELETE FROM ScriptSensor WHERE EXISTS ( select 'x' FROM "
			+ "DecodesScript WHERE ScriptSensor.DecodesScriptId = "
			+ "DecodesScript.id and DecodesScript.configId = "
			+ pc.getId() + ")";
		tryUpdate(q);

		if (n > 0)
		{
			q = "DELETE FROM UnitConverter where id " 
				+ inClause.toString();
			tryUpdate(q);
		}

		// Delete any format statements from any scripts for this config.
		// MJM -- likewise -- mods for Oracle compatibility:
		//	q = "DELETE from FormatStatement where DecodesScriptId = "
		//		+ "DecodesScript.id and DecodesScript.configId = "
		//		+ pc.getId();
		q = "DELETE FROM FormatStatement WHERE EXISTS ( SELECT 'x' FROM "
			+ "DecodesScript WHERE FormatStatement.DecodesScriptId = "
			+ "DecodesScript.id and DecodesScript.configId = "
			+ pc.getId() + ")";
		tryUpdate(q);

		// Finally, delete the scripts.
		q = "DELETE FROM DecodesScript WHERE configId = " + pc.getId();
		tryUpdate(q);
	}

	//======================================================================
	// Format Statement Methods
	//======================================================================
	
	/**
	* Reads all the FormatStatements for a particular DecodesScript.
	* The DecodesScript must have already had its SQL database ID set.
	* Note that we aren't guaranteed that these FormatStatements haven't
	* already been read.
	* @param ds the DecodesScript
	*/
	private void readFormatStatements(DecodesScript ds)
		throws DatabaseException, SQLException
	{
		Statement stmt = createStatement();
		ResultSet rs = stmt.executeQuery(
			"SELECT decodesScriptId, sequenceNum, " +
			"label, format " +
			"FROM FormatStatement " +
			"WHERE DecodesScriptId = " + ds.getId() + " " +
			"ORDER BY SequenceNum"
		);

		if (rs != null) {
			while (rs.next()) {
				int seqNum = rs.getInt(2);
				String label = rs.getString(3);
				String format = rs.getString(4);
				if (format == null) format = "";

				FormatStatement fmt = new FormatStatement(ds, seqNum);
				fmt.label = label;
				fmt.format = format;

				//ds.formatStatements.add(fmt);
			}
		}

		stmt.close();
	}


	/**
	* Insert all the FormatStatements associated with a particular
	* DecodesScript.  The DecodesScript must have already had its
	* SQL database IO set.
	* @param ds the DecodesScript
	*/
	private void insertFormatStatements(DecodesScript ds)
		throws DatabaseException, SQLException
	{

		Vector<FormatStatement> v = ds.getFormatStatements();
		for (int i = 0; i < v.size(); ++i) 
		{
			FormatStatement fs = v.get(i);
			if (fs.label == null || fs.label.trim().length() == 0)
			{
				if (fs.format == null || fs.format.trim().length() == 0)
					continue; // ignore empty format statement.
				else
					fs.label = "nolabel_" + i;
			}
			try { insert(fs, ds.getId()); }
			catch(SQLException ex)
			{
				Logger.instance().log(Logger.E_FAILURE,
					"SQLException trying to insert format statement: " + ex);
			}
		}
	}

	/**
	* Insert a single FormatStatement into the SQL database, using
	* the DecodesScript ID provided as the second argument.
	* @param fs the FormatStatement
	* @param dsId the database ID of the DecodesScript
	*/
	private void insert(FormatStatement fs, DbKey dsId)
		throws DatabaseException, SQLException
	{
		String q = "INSERT INTO FormatStatement VALUES (" +
					 dsId + ", " +
					 fs.sequenceNum + ", " +
					 sqlReqString(fs.label) + ", " +
					 escapeString(fs.format) +
				   ")";
		executeUpdate(q);
	}

	
	//=====================================================================
	// Script Sensor Methods
	//=====================================================================
	
	/**
	* Reads all the ScriptSensors associated with a single
	* DecodesScript.
	* @param ds the DecodesScript.
	*/
	private void readScriptSensors(DecodesScript ds)
		throws SQLException, DatabaseException
	{
		DbKey dsId = ds.getId();

		// Note:  the UnitConverter here always has the fromUnits set
		// to "raw".

		Statement stmt = createStatement();
		String q = "SELECT SensorNumber, UnitConverterId " +
			"FROM ScriptSensor " + "WHERE DecodesScriptId = " + dsId;
		Logger.instance().debug3("Query: " + q);
		ResultSet rs = stmt.executeQuery(q);

		if (rs != null)
		{
			StringBuilder inList = new StringBuilder();
			while (rs.next())
			{
				int sensorNum = rs.getInt(1);
				DbKey ucid = DbKey.createDbKey(rs, 2);
				
				if (inList.length() > 0)
					inList.append(", ");
				inList.append("" + ucid);

				ScriptSensor ss = new ScriptSensor(ds, sensorNum);
				ds.scriptSensors.add(ss);
				ss.setUnitConverterId(ucid);
//				ss.rawConverter = _unitConverterIO.readUnitConverter(ucid);
			}
			
			if (inList.length() > 0)
			{
				String inClause = "(" + inList.toString() + ")";
				ArrayList<UnitConverterDb> ucs = _unitConverterIO.readUCsIn(inClause);
				for (ScriptSensor ss : ds.scriptSensors)
				{
					for(UnitConverterDb uc : ucs)
						if (ss.getUnitConverterId().equals(uc.getId()))
						{
							ss.rawConverter = uc;
							break;
						}
				}
			}
		}

		stmt.close();
	}

	/**
	* Inserts all the ScriptSensors belonging to a particular
	* DecodesScript, if any.  The DecodesScript must have already
	* had its SQL database ID set.
	* @param ds the DecodesScript.
	*/
	private void insertScriptSensors(DecodesScript ds)
		throws SQLException, DatabaseException
	{
		DbKey id = ds.getId();
		Vector<ScriptSensor> v = ds.scriptSensors;
		for (int i = 0; i < v.size(); ++i)
			insert(v.get(i), id);
	}

	/**
	* Insert a single ScriptSensor with the given DecodesScript ID.
	* @param ss the ScriptSensor to insert
	* @param dsId SQL Database Key for DecodesScript
	*/
	private void insert(ScriptSensor ss, DbKey dsId)
		throws SQLException, DatabaseException
	{
		//System.out.println("			  " +
		//	"ScriptSensorIO.insert(ss, dsId == " + dsId + ")");

		UnitConverterDb rc = ss.rawConverter;
		if (rc != null) 
		{
			if (rc.toAbbr == null || rc.toAbbr.trim().length() == 0)
				rc.toAbbr = rc.fromAbbr;
			if (rc.idIsSet())
				_unitConverterIO.insert(rc);
			else
				_unitConverterIO.addNew(rc);
		}

		String q = "INSERT INTO ScriptSensor(decodesScriptId, sensorNumber, "
			+ "unitConverterId) VALUES (" + dsId + ", " 
			+ ss.sensorNumber + ", " + sqlOptHasId(ss.rawConverter) + ")";
		executeUpdate(q);
	}
	
}
