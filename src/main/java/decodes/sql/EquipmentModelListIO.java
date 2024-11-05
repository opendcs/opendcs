/*
* $Id$
*
* $State$
*
* $Log$
* Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
* OPENDCS 6.0 Initial Checkin
*
* Revision 1.3  2013/03/21 18:27:39  mmaloney
* DbKey Implementation
*
* Revision 1.2  2010/12/08 13:40:49  mmaloney
* Specify Columns in INSERT statements.
*
* Revision 1.1  2008/04/04 18:21:04  cvs
* Added legacy code to repository
*
* Revision 1.11  2007/04/20 14:30:54  ddschwit
* Changed SELECT * to SELECT <columnlist>
*
* Revision 1.10  2004/09/02 12:15:27  mjmaloney
* javadoc
*
* Revision 1.9  2003/11/15 20:28:35  mjmaloney
* Mods to transparently support either V5 or V6 database.
*
* Revision 1.8  2002/10/06 14:23:58  mjmaloney
* SQL Development.
*
* Revision 1.7  2002/10/04 13:32:12  mjmaloney
* SQL dev.
*
* Revision 1.6  2002/09/19 17:18:02  mjmaloney
* SQL dev.
*
* Revision 1.5  2002/08/29 05:48:49  chris
* Added RCS keyword headers.
*/

package decodes.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import opendcs.dai.PropertiesDAI;

import ilex.util.Logger;

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
		Logger.instance().log(Logger.E_DEBUG1,"Reading EquipmentModels...");
		_emList = emList;

		// First read the EquipmentModel table

		Statement stmt = createStatement();
		
		String q = "SELECT id, name, company, model, description, equipmentType  " +
		           "FROM EquipmentModel";
		
		ResultSet rs = stmt.executeQuery( q );

		if (rs != null) _makeEquipmentModels(rs, false);
		stmt.close();
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

		Statement stmt = createStatement();
		
		String q = "SELECT id, name, company, model, description, equipmentType  " +
				   "FROM EquipmentModel WHERE ID = " + id;
		
		ResultSet rs = stmt.executeQuery( q );
			

		if (rs == null)
			throw new DatabaseException("No equipment model found " +
				"with ID " + id);

		EquipmentModel eqm = _makeEquipmentModels(rs, true);
		stmt.close();
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
		catch (DbIoException e)
		{
			throw new DatabaseException(e.getMessage());
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
		catch (DbIoException e)
		{
			throw new DatabaseException(e.getMessage());
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
		catch (DbIoException e)
		{
			throw new DatabaseException(e.getMessage());
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
		Statement stmt = createStatement();
		ResultSet rs = stmt.executeQuery(
			"SELECT id FROM EquipmentModel where name = "
			+ sqlReqString(name));

		DbKey ret = Constants.undefinedId;
		if (rs != null && rs.next())
			ret = DbKey.createDbKey(rs, 1);

		stmt.close();
		return ret;
	}
}
