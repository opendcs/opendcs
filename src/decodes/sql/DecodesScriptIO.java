/*
* $Id$
* 
* Open source software
* 
* $Log$
* Revision 1.3  2013/03/21 18:27:39  mmaloney
* DbKey Implementation
*
*/
package decodes.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import ilex.util.Logger;

import decodes.db.Constants;
import decodes.db.DatabaseException;
import decodes.db.DecodesScript;
import decodes.db.PlatformConfig;

/**
* This class is responsible for reading and writing the DecodesScript
* table and the related table FormatStatement.
*/
public class DecodesScriptIO extends SqlDbObjIo
{
	/**
	* A reference to the FormatStatementIO object which is used to read
	* and write the FormatStatement table.
	*/
	FormatStatementIO _formatStatementIO;

	/**
	* A reference to the ScriptSensorIO object which is used to read and
	* write the ScriptSensor table.
	*/
	ScriptSensorIO _scriptSensorIO;


	/** 
	  Constructor. 
	  @param dbio the SqlDatabaseIO to which this IO object belongs
	  @param fsio used to read/write FormatStatements
	  @param ssio used to read/write ScriptSensors
	*/
	DecodesScriptIO(SqlDatabaseIO dbio,
					FormatStatementIO fsio,
					ScriptSensorIO ssio)
	{
		super(dbio);
		_formatStatementIO = fsio;
		_scriptSensorIO = ssio;
	}

	/**
	* Read all the DecodesScripts associated with one PlatformConfig.
	* The PlatformConfig must have had its SQL database ID set.
	* @param pc the PlatformConfig that owns the script
	*/
	public void readDecodesScripts(PlatformConfig pc)
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

		DecodesScript ds;

		ds = new DecodesScript(pc, name);
		ds.setDataOrder(dataOrder);
		ds.setId(id);
		_formatStatementIO.readFormatStatements(ds);
		_scriptSensorIO.readScriptSensors(ds);
		ds.scriptType = type;
		pc.addScript(ds);
	}

	/**
	* Inserts all the DecodesScripts associated with one PlatformConfig
	* into the database.
	* The PlatformConfig must have had its SQL database ID set.
	* @param pc the PlatformConfig
	*/
	public void insertDecodesScripts(PlatformConfig pc)
		throws DatabaseException, SQLException
	{
		DbKey pcId = pc.getId();
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
	public void insert(DecodesScript ds, DbKey pcId)
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

		_formatStatementIO.insertFormatStatements(ds);
		_scriptSensorIO.insertScriptSensors(ds);
	}

	/**
	* Delete all the DecodesScripts belonging to a particular PlatformConfig.
	* All of the DecodesScripts belonging to this PlatformConfig must have
	* already had their SQL database ID values set.
	* @param pc the PlatformConfig
	*/
	public void deleteDecodesScripts(PlatformConfig pc)
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

	/**
	* Delete a single DecodesScript from the SQL database.  This
	* DecodesScript must have already had its SQL database ID set.
	* The DecodesScript object, after this, will have its ID unset.
	* @param ds the DecodesScript
	*/
	public void delete(DecodesScript ds)
		throws DatabaseException, SQLException
	{
		if (!ds.idIsSet())
		{
			DbKey id = scriptName2id(ds.platformConfig.getId(), ds.scriptName);
			if (id == null || id.isNull())
				return;
			ds.setId(id);
		}

		// First delete all the ScriptSensors and FormatStatements
		// belonging to that DecodesScript.

		_scriptSensorIO.deleteScriptSensors(ds);
		_formatStatementIO.deleteFormatStatements(ds);

		String q = "DELETE FROM DecodesScript WHERE ID = " + ds.getId();
		executeUpdate(q);
	}

	private DbKey scriptName2id(DbKey cfgId, String name)
		throws DatabaseException, SQLException
	{
		Logger.instance().log(Logger.E_DEBUG1, 
			"Looking up script ID for configId " + cfgId + ", name="
			+ name);

		Statement stmt = createStatement();
		ResultSet rs = stmt.executeQuery(
			"SELECT id FROM DecodesScript where configId = "
			+ cfgId + " AND name = " + sqlReqString(name));

		DbKey ret = Constants.undefinedId;
		if (rs != null && rs.next())
			ret = DbKey.createDbKey(rs, 1);

		stmt.close();
		return ret;
	}
}

