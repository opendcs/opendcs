/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.3  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*  Revision 1.2  2009/08/23 14:54:55  mjmaloney
*  SFWMD Import
*
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.8  2004/08/26 13:29:22  mjmaloney
*  Added javadocs
*
*  Revision 1.7  2002/09/19 17:18:02  mjmaloney
*  SQL dev.
*
*  Revision 1.6  2001/10/16 01:09:35  mike
*  dev
*
*  Revision 1.5  2001/10/16 01:02:11  mike
*  dev
*
*  Revision 1.4  2001/04/21 20:19:23  mike
*  Added read & write methods to all DatabaseObjects
*
*  Revision 1.3  2001/04/12 12:30:27  mike
*  dev
*
*  Revision 1.2  2001/04/02 00:42:33  mike
*  DatabaseObject is now an abstract base-class.
*
*  Revision 1.1  2001/03/23 22:16:44  mike
*  Added EquipmentModelList.
*
*/
package decodes.db;

import java.util.HashMap;
import java.util.Collection;
import java.util.Iterator;

import decodes.sql.DbKey;

/**
EquipmentModelList is a collection of all known EquipmentModel objects.
*/
public class EquipmentModelList extends DatabaseObject
{
	/** EquipmentModel objects hashed on the name. */
	private HashMap<String, EquipmentModel> modelHash;

	/** Cross reference of EquipmentModel objects to SQL ID. */
	private IdRecordList eqModIdList;

	/** Default constructor. */
	public EquipmentModelList()
	{
		modelHash = new HashMap<String, EquipmentModel>();
		eqModIdList = new IdRecordList("EquipmentModel");
	}

	/** @return "EquipmentModelList" */
	public String getObjectType() { return "EquipmentModelList"; }

	/**
	  Adds a model object to the collection.
	  The name (and ID if used) must be set prior to adding to the collection.
	  @param mod the model to add.
	*/
	public void add(EquipmentModel mod)
	{
		modelHash.put(mod.name, mod);
		eqModIdList.add(mod);
	}

	/**
	  Removes an equipment model record from the list.
	  @param mod the model to remove.
	*/
	public void remove(EquipmentModel mod)
	{
		modelHash.remove(mod.name);
		eqModIdList.remove(mod);
	}

	/**
	  Retrieve an EquipmentModel by name.
	  @return EquipmentModel or null if no match.
	*/
	public EquipmentModel get(String name)
	{
		return (EquipmentModel)modelHash.get(name);
	}

	/**
	  Retrieve an EquipmentModel by database ID.
	  @return EquipmentModel or null if no match.
	*/
	public EquipmentModel getById(DbKey id)
	{
		return (EquipmentModel)eqModIdList.get(id);
	}

	/** @return EquipmentModel objects as a Collection */
	public Collection<EquipmentModel> values()
	{
		return modelHash.values();
	}

	/** @return EquipmentModel objects as an iterator */
	public Iterator<EquipmentModel> iterator()
	{
		return modelHash.values().iterator();
	}

	/** @return number of EquipmentModel objects */
	public int size()
	{
		return modelHash.size();
	}

	/**
	  From DatabaseObject
	*/
	public void prepareForExec()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		throw new InvalidDatabaseException("Not implemented");
	}

	/**
	  From DatabaseObject
	*/
	public boolean isPrepared()
	{
		return false;
	}

	/**
	  From DatabaseObject
	*/
	public void validate()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		throw new InvalidDatabaseException("Not implemented");
	}

	public void read()
		throws DatabaseException
	{
		myDatabase.getDbIo().readEquipmentModelList(this);
	}

	public void write()
		throws DatabaseException
	{
		Iterator it = values().iterator();
		while(it.hasNext())
		{
			EquipmentModel ob = (EquipmentModel)it.next();
			ob.write();
		}
	}

	public void clear()
	{
		modelHash.clear();
		eqModIdList.clear();
	}
}

