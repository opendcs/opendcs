/*
 * $Id$
 * 
 * Open Source Software
 * 
 * $Log$
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.2  2013/03/21 18:27:39  mmaloney
 * DbKey Implementation
 *
 */

package decodes.sql;

import decodes.db.DatabaseException;
import decodes.db.DecodesScript;
import decodes.db.ScriptSensor;
import decodes.db.UnitConverterDb;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Vector;

import ilex.util.Logger;

/**
Handles SQL IO for script sensor objects.
*/
public class ScriptSensorIO extends SqlDbObjIo
{
	/**
	* This is a reference to the UnitConverterIO object that handles I/O
	* for the UnitConverters.
	*/
	UnitConverterIO _unitConverterIO;


	/** 
	  Constructor.  
	  @param dbio the owning SqlDatabaseIO object.
	  @param ucio The related UnitConverterIO needed to save script EU conv.
	*/
	ScriptSensorIO(SqlDatabaseIO dbio, UnitConverterIO ucio)
	{
		super(dbio);
		_unitConverterIO = ucio;
	}

	/**
	* Reads all the ScriptSensors associated with a single
	* DecodesScript.
	* @param ds the DecodesScript.
	*/
	public void readScriptSensors(DecodesScript ds)
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

		if (rs != null) {
			while (rs.next()) {
				int sensorNum = rs.getInt(1);
				DbKey ucid = DbKey.createDbKey(rs, 2);

				ScriptSensor ss = new ScriptSensor(ds, sensorNum);
				ds.scriptSensors.add(ss);
				ss.rawConverter = _unitConverterIO.readUnitConverter(ucid);
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
	public void insertScriptSensors(DecodesScript ds)
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
	public void insert(ScriptSensor ss, DbKey dsId)
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

	/**
	* Deletes all the ScriptSensors belonging to a particular DecodesScript,
	* if any.
	* The DecodesScript must have already had its SQL database ID set.
	* This also deletes the UnitConverterDbs belonging to these ScriptSensors
	* (their rawConverters), if any.
	* @param ds the DecodesScript.
	*/
	public void deleteScriptSensors(DecodesScript ds)
		throws SQLException, DatabaseException
	{

		// First delete the UnitConverterDbs
		// MJM 20050606 Modification for Oracle Compatibility:
		// String q = "DELETE FROM UnitConverter WHERE id = "
		//	+ "ScriptSensor.unitConverterId AND ScriptSensor.decodesScriptId = "
		//	+ ds.getId();
		String q = "DELETE FROM UnitConverter WHERE EXISTS (SELECT 'x' FROM "
                 + "ScriptSensor WHERE UnitConverter.id = "
            + "ScriptSensor.unitConverterId AND ScriptSensor.decodesScriptId = "            + ds.getId() + ")";
		tryUpdate(q);

		// Now delete the ScriptSensors
		q = "DELETE FROM ScriptSensor " +
			"WHERE DecodesScriptID = " + ds.getId();
		tryUpdate(q);
	}
}

