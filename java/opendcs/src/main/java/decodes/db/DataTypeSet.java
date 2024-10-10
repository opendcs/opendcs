/*
*  $Id$
*/
package decodes.db;

import java.util.HashMap;
import java.util.Collection;
import java.util.Iterator;

import decodes.sql.DbKey;
import ilex.util.StringPair;

/**
 * Stores the set of all known data types.
 * This also includes methods for retrieving a DataType object from its
 * standard name and code (for example, 'SHEF-PE' and 'RF').
 */
public class DataTypeSet extends DatabaseObject
{
	/**
	* This stores all of the DataType objects.
	* The key to this HashMap is a StringPair, which holds the name of the
	* data type standard (e.g. "SHEF-PE") and the code (e.g. "RF").
	* The strings in the key are always uppercase.
	*/
	private HashMap<StringPair, DataType> knownDataTypes
		= new HashMap<StringPair, DataType>();

	/// Cross reference of DataType objects to SQL ID.
	private IdRecordList dtIdList = new IdRecordList("DataType");

	/**
	* Default constructor.
	*/
	public DataTypeSet()
	{
	}

	/**
	* Overrides the DatabaseObject method: returns "DataTypeSet".
	*/
	public String getObjectType() { return "DataTypeSet"; }

	/**
	* Searches the set for a match and returns it (null if not found).
	* standard and code should be passed in uppercase; they are not
	* converted to uppercase before the search.
	*/
	public DataType get(String standard, String code)
	{
		StringPair sp = new StringPair(standard.toUpperCase(), code.toUpperCase());
		return knownDataTypes.get(sp);
	}

	public DataType get(DbKey dataTypeId, String standard, String code)
	{
		DataType dt = getById(dataTypeId);
		if (dt != null)
			return dt;
		dt = get(standard, code);
		if (dt != null)
			return dt;
		dt = new DataType(standard, code);
		dt.forceSetId(dataTypeId);
		add(dt);
		return dt;
	}

	/** Gets a data type from its SQL ID. */
	public DataType getById(DbKey id)
	{
		return (DataType)dtIdList.get(id);
	}

	public DataType readById(DbKey id)
		throws DatabaseException
	{
		return myDatabase.getDbIo().readDataType(id);
	}

	/**
	* Adds the passed data type to the set.
	* This is called only from the DataType.getDataType method.
	*/
	public void add(DataType dt)
	{
		StringPair sp = new StringPair(dt.getStandard().toUpperCase(),
			dt.getCode().toUpperCase());
		knownDataTypes.put(sp, dt);
		dtIdList.add(dt);
	}

	/**
	* Deletes a data type from the set.
	*/
	public void remove(DataType dt)
	{
		StringPair sp = new StringPair(dt.getStandard().toUpperCase(),
			dt.getCode().toUpperCase());
		knownDataTypes.remove(sp);
		dtIdList.remove(dt);
	}

	/**
	* Turns off the 'saved' flag in all DataType objects.
	* This is called prior to outputting a DataTypeEquivalenceList
	* XML file. To do that we need to make sure we only output each
	* DataType object once.
	*/
	public void resetSaved()
	{
		for(Iterator<DataType> it = knownDataTypes.values().iterator(); it.hasNext(); )
		{
			DataType dt = it.next();
			dt.saved = false;
		}
	}

	/**
	* Returns all the DataType objects as a Collection.
	*/
	public Collection<DataType> values()
	{
		return knownDataTypes.values();
	}

	/**
	* Returns an Iterator that can be used to examine the DataTypes.
	*/
	public Iterator<DataType> iterator()
	{
		return values().iterator();
	}


	@Override
	public void prepareForExec()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		throw new InvalidDatabaseException("Not implemented");
	}

	@Override
	public boolean isPrepared()
	{
		return false;
	}

	public void read()
		throws DatabaseException
	{
		myDatabase.getDbIo().readDataTypeSet(this);
	}

	public void write()
		throws DatabaseException
	{
		myDatabase.getDbIo().writeDataTypeSet(this);
	}

	public void clear()
	{
		knownDataTypes.clear();
		dtIdList.clear();
	}
}
