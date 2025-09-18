/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import opendcs.dai.PropertiesDAI;

import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.EquipmentModel;
import decodes.db.EquipmentModelList;
import decodes.db.DatabaseException;
import opendcs.dao.DaoBase;
import decodes.tsdb.DbIoException;

/**
* This class is used to read and write the EquipmentModel objects
* from the SQL database.  It reads and writes these SQL database
* tables:
* <ul>
*   <li>EquipmentModel</li>
*   <li>EquipmentProperty</li>
* </ul>
* <p>
*   This also provides methods for retrieving EquipmentModel objects
*   by their SQL database ID numbers, whereby if the EquipmentModel
*   is not already in the list, this class attempts to read it from
*   the database.
* </p>
*/
public class EquipmentModelListIO extends SqlDbObjIo
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/**
	* Transient reference to the EquipmentModelList that we're operating
	* on.
	*/
	EquipmentModelList _emList;

	/**
	* Constructor.
	* @param dbio the SqlDatabaseIO to which this IO object belongs
	*/
	public EquipmentModelListIO(SqlDatabaseIO dbio)
	{
		super(dbio);
	}

	/**
	* Read the EquipmentModelList from the SQL database.
	* This is written to expect that some of the EquipmentModels may have
	* already been read.  If that's the case, then those objects' data
	* is overwritten from the (fresh) data from the database.
	* @param emList the EquipmentModelList to read
	*/
	public void read(EquipmentModelList emList)
		throws DatabaseException, SQLException
	{
		log.debug("Reading EquipmentModels...");
		_emList = emList;

		// First read the EquipmentModel table



		String q = "SELECT id, name, company, model, description, equipmentType  " +
		           "FROM EquipmentModel";
		try (Statement stmt = createStatement();
			 ResultSet rs = stmt.executeQuery(q);)
		{
			_makeEquipmentModels(rs, false);
		}
	}

	/**
	* Get an EquipmentModel, by its ID number.
	* If the requested EquipmentModel isn't already in the list, this
	* attempts to read it from the database.  The second argument is
	* required because it might be required to read a new EquipmentModel
	* from the SQL database.  When that happens, this method needs to add
	* it to the existing Database object.
	* @param id the database ID
	* @param db the Database reference
	*/
	public EquipmentModel getEquipmentModel(DbKey id, Database db)
		throws DatabaseException, SQLException
	{
		EquipmentModel em = db.equipmentModelList.getById(id);
		if (em != null) return em;

		_emList = db.equipmentModelList;



		String q = "SELECT id, name, company, model, description, equipmentType  " +
				   "FROM EquipmentModel WHERE ID = " + id;
		EquipmentModel eqm = null;
		try (Statement stmt = createStatement();
			 ResultSet rs = stmt.executeQuery( q );)
		{

			eqm = _makeEquipmentModels(rs, true);
		}
		return eqm;
	}

	/**
	* This private method takes a ResultSet which is the result of a
	* "SELECT columnlist FROM EquipmentModel ..." query, and creates one or
	* more EquipmentModel object from it.  One EquipmentModel object
	* is created for every row of the ResultSet.
	* The argument rs has already been checked to ensure that it is
	* not null.
	* <p>
	*   If the isNew argument is true, then the caller has already
	*   guaranteed that the EquipmentModel(s) are not in the database
	*   collections.
	*   _equipModels RecordList, so this method will always create a
	*   new EquipmentModel for each row.
	* </p>
	* <p>
	*   If isNew is false, this method first checks
	*   _equipModels to see if an EquipmentModel with the same ID
	*   already exists.  If it does, then that object's data is over-
	*   written.  If it doesn't, then a new object is created.
	* </p>
	* @param rs  the JDBC result set
	* @param isNew true if this is a new EquipmentModel
	*/
	private EquipmentModel _makeEquipmentModels(ResultSet rs, boolean isNew)
		throws DatabaseException, SQLException
	{
		EquipmentModel em = null;
		PropertiesDAI propsDao = _dbio.makePropertiesDAO();
		((DaoBase)propsDao).setManualConnection(connection());

		try
		{
			while (rs.next())
			{
				DbKey id = DbKey.createDbKey(rs, 1);
				String name = rs.getString(2);

				// Resolve the EquipmentModel object for this database record.

				em = null;
				if (!isNew)
					em = _emList.getById(id);

				if (em == null) {
					em = new EquipmentModel(name);
					em.setId(id);
					_emList.add(em);
				}

				String company = rs.getString(3);
				if (!rs.wasNull()) em.company = company;

				String model = rs.getString(4);
				if (!rs.wasNull()) em.model = model;

				String desc = rs.getString(5);
				if (!rs.wasNull()) em.description = desc;

				String type = rs.getString(6);
				if (!rs.wasNull()) em.equipmentType = type;

				// Read the EquipmentProperty table for this e.m.
				propsDao.readProperties("EquipmentProperty", "EquipmentID", id, em.properties);
			}
		}
		catch (DbIoException ex)
		{
			throw new DatabaseException("Unable to create equipment model", ex);
		}
		finally
		{
			propsDao.close();
		}
		return em;
	}

	/**
	* Writes an EquipmentModel to the database.
	* @param eqm the EquipmentModel
	*/
	public void write(EquipmentModel eqm)
		throws DatabaseException, SQLException
	{
		if (eqm.idIsSet())
			update(eqm);
		else
		{
			DbKey id = name2id(eqm.name);
			if (id != null && !id.isNull())
			{
				eqm.setId(id);
				update(eqm);
			}
			else
				insert(eqm);
		}
	}

	/**
	* Updates an already-existing Site in the database.
	* @param eqm the EquipmentModel
	*/
	private void update(EquipmentModel eqm)
		throws DatabaseException, SQLException
	{
		String q =
			"UPDATE EquipmentModel SET " +
			  "name = " + sqlString(eqm.name) + ", " +
			  "company = " + sqlOptString(eqm.company) + ", " +
			  "model = " + sqlOptString(eqm.model) + ", " +
			  "description = " + sqlOptString(eqm.description) + ", " +
			  "equipmentType = " + sqlOptString(eqm.equipmentType) + " " +
			"WHERE id = " + eqm.getId();

		executeUpdate(q);

		// Now do the properties.  We'll take the easy route, and delete
		// them all and then re-insert them.

		insertProperties(eqm);
	}

	/**
	* Inserts a new EquipmentModel into the database.  The EquipmentModel
	* must not have an SQL Database ID value set already.
	* @param eqm the EquipmentModel
	*/
	private void insert(EquipmentModel eqm)
		throws DatabaseException, SQLException
	{
		DbKey id = getKey("EquipmentModel");
		eqm.setId(id);
		eqm.getDatabase().equipmentModelList.add(eqm);

		String q =
			"INSERT INTO EquipmentModel(id, name, company, model, description,"
			+ " equipmenttype) VALUES (" +
			  eqm.getId() + ", " +
			  sqlString(eqm.name) + ", " +
			  sqlOptString(eqm.company) + ", " +
			  sqlOptString(eqm.model) + ", " +
			  sqlOptString(eqm.description) + ", " +
			  sqlOptString(eqm.equipmentType) +
			")";

		executeUpdate(q);

		insertProperties(eqm);
	}

	/**
	* This inserts all of the properties associated with an EquipmentModel.
	* @param eqm the EquipmentModel
	*/
	private void insertProperties(EquipmentModel eqm)
		throws DatabaseException, SQLException
	{
		PropertiesDAI propsDao = _dbio.makePropertiesDAO();

		try { propsDao.writeProperties("EquipmentProperty", "EquipmentID", eqm.getId(), eqm.properties); }
		catch (DbIoException ex)
		{
			throw new DatabaseException("Unable to write properties", ex);
		}
		finally
		{
			propsDao.close();
		}
	}

	/**
	* Delete an EquipmentModel from the database.  The EquipmentModel must
	* have its SQL database ID value set.  This also, of course, deletes all
	* the EquipmentProperty records associated with this EquipmentModel.
	* <p>
	* Referential Integrity:  we'll need to change any other objects that
	* refer to this EquipmentModel.
	* @param eqm the EquipmentModel
	*/
	public void delete(EquipmentModel eqm)
		throws DatabaseException, SQLException
	{
		DbKey id = eqm.getId();

		PropertiesDAI propsDao = _dbio.makePropertiesDAO();
		try { propsDao.deleteProperties("EquipmentProperty", "EquipmentID", eqm.getId()); }
		catch (DbIoException ex)
		{
			throw new DatabaseException("Unable to delete equipment model", ex);
		}
		finally
		{
			propsDao.close();
		}

		String q = "DELETE FROM EquipmentModel WHERE ID = " + id;
		executeUpdate(q);
	}

	private DbKey name2id(String name)
		throws DatabaseException, SQLException
	{
		DbKey ret = Constants.undefinedId;
		try (Statement stmt = createStatement();
			 ResultSet rs = stmt.executeQuery(
				"SELECT id FROM EquipmentModel where name = "
				+ sqlReqString(name));)
		{
			if (rs.next())
			{
				ret = DbKey.createDbKey(rs, 1);
			}
		}
		return ret;
	}
}
