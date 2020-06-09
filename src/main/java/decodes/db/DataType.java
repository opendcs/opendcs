/*
*  $Id$
*/
package decodes.db;

import ilex.util.TextUtil;
import decodes.sql.DbKey;

/**
This class encapsulates a single data-type, which is identified by
a standard and a code. 
DataType is immutable. You create a new DataType by calling the internal
static getDataType method which guarantees that only one object exists for
a given standard/code pair.
<p>
The case of the standard and code is not significant.
*/
public class DataType extends IdDatabaseObject
{
	// _id is stored in the IdDatabaseObject superclass.

	/**
	* A string defining the data-type standard.
	* This must match one of the DataTypeStandard enum values.
	* Currently the allowable values are SHEF-PE, NOS-CODE, or EPA-CODE.
	*/
	private String standard;

	/**
	* This identifies the data type.  The form of this string depends on
	* the standard.
	*/
	private String code;

	/**
	* A circular linked-list of equivalences.
	* If this DataType is not equivalent to any others, then this will be
	* null.
	*/
	public DataType equivRing;
	
	/**
	 * Display name
	 */
	public String displayName = null;

	/** This is used during XML output.  */
	public boolean saved;

	public DataType(String standard, String code)
	{
		super();
		this.standard = standard;
		this.code = code;
		equivRing = null;
	}

	/**
	 * @return the standard of this data type.
	 */
	public String getStandard() { return standard; }

	/**
	 * @return the code of this data type.
	 */
	public String getCode() { return code; }

	/**
	  Gets a DataType object for the passed standard and code.
	  This method searches the set of known data types. If no match is
	  found, a new DataType object is created and added to the set.
	  The case of the standard and code arguments is not significant --
	  both are converted to uppercase before being used.
	  Having a single set ensures that there will be no duplicates and
	  that equivalences will be known.
	  @param standard the standard, e.g. SHEF-PE or EPA-CODE
	  @param code the code
	*/
	public static synchronized DataType
		getDataType(String standard, String code)
	{
		// Enforce upper case standard
		standard = standard.toUpperCase();
		
		// Search the singleton DataTypeSet, but only if there is one.
		DataType dt = null;
		Database db = Database.getDb();
		if (db != null)
			dt = db.dataTypeSet.get(standard, code);

		if (dt == null)
		{
		    dt = new DataType(standard, code);
			if (db != null)
		    	db.dataTypeSet.add(dt);
		}
		return dt;
	}


	/**
	  Special version of this method called from SQL database IO.
	  Same as the above method but this also takes a database ID.
	  @param standard the standard, e.g. SHEF-PE or EPA-CODE
	  @param code the code
	  @param id the database ID
	*/
	public static synchronized DataType
		getDataType(String standard, String code, DbKey id)
	{
		// Enforce upper case standard and code:
		standard = standard.toUpperCase();
		
		// Allow code to be mixed case
//		code = code.toUpperCase();

		// Search the singleton DataTypeSet
		DataType dt = null;
		Database db = Database.getDb();
		if (db != null)
		{
			dt = db.dataTypeSet.get(standard, code);
			try { dt.setId(id); }
			catch(Exception ex) {}
		}
		
		if (dt == null)
		{
		  dt = new DataType(standard, code);
			try { dt.setId(id); }
			catch(Exception ex) {}
			if (db != null)
		    db.dataTypeSet.add(dt);
		}
		return dt;
	}

	/**
	 * Return a data type from its ID. If one is not defined in cache,
	 * attempt to read it from the database.
	 */
	public static synchronized DataType getDataType(DbKey id)
	{
		if (DbKey.isNull(id))
			return null;
		if (Database.getDb() == null)
			return null;
		DataTypeSet dts = Database.getDb().dataTypeSet;
		DataType dt = dts.getById(id);
		if (dt != null)
			return dt;
		try { return dts.readById(id); }
		catch(DatabaseException ex) { return null; }
	}

	/**
	* Every DataType maintains a ring structure of other data types
	* that are considered equivalent.
	* Calling this method asserts an equivalence between both data types.
	* If either data type was already asserted to be equivalent to a
	* third (or more) data type, all are now considered equivalent.
	  @param target the DataType that this is equivalent to
	*/
	public void assertEquivalence(DataType target)
	{
		if (isEquivalent(target))
			return;  // Equivalence is already asserted.

		// Save ptr to next in ring. If none, point back to myself.
		DataType next = equivRing;
		if (next == null)
		    next = this;

		// Point me to the passed DataType to add it to the ring.
		equivRing = target;

		// If passed DT has no ring currently, set it to 'next'.
		if (target.equivRing == null)
		{
		    target.equivRing = next;
		}
		else
		{
		    // Passed DT already has a ring, find its end point,
		    // set to next.
		    DataType dt;
		    for(dt = target.equivRing; dt.equivRing != target;
		        dt = dt.equivRing);
		    dt.equivRing = next;
		}
	}

	/**
	 * Deasserts equivalence. This data type will no longer be considered
	 * equivalent to anything.
	 */
	public void deAssertEquivalence()
	{
		// If I am a member of a ring...
		if (equivRing != null && equivRing != this)
		{
			// Remove me from the ring of my peers.
			DataType dt;
			for(dt = equivRing; dt.equivRing != this; dt = dt.equivRing);
			dt.equivRing = this.equivRing;

			// If the ring is now empty, null it out.
			if (dt.equivRing == dt)
				dt.equivRing = null;
		}

		equivRing = null;
	}

	/** @return true if the passed data type is equivalent to this one. */
	public boolean isEquivalent(DataType target)
	{
		if (this == target)
			return true;
		for(DataType dt = equivRing; dt!=null && dt!=this; dt = dt.equivRing)
			if (target.equals(dt))
				return true;
		return false;
	}

	/**
	* @return an equivalent data type in the specified standard, if one
	* exists or null if no equivalent in the specified standard is
	* founds.
	*/
	public DataType findEquivalent(String std)
	{
		if (standard.equalsIgnoreCase(std))
			return this;

		if (equivRing == null) // No equivalences defined
			return null;

		for(DataType dt = equivRing; dt != this; dt = dt.equivRing)
			if (dt.standard.equalsIgnoreCase(std))
				return dt;

		return null;	// No matching standard found.
	}

	/**
	* Overrides Object.hashCode().
	* @return hash of standard ^ hash of code
	*/
	public int hashCode()
	{
		return standard.toLowerCase().hashCode() ^
	    	code.toLowerCase().hashCode();
	}

	/**
	* Overrides Object.toString().
	  @return standard:code
	*/
	public String toString()
	{
		return standard + ":" + code;
	}

	public String getDisplayName()
	{
		if (displayName != null)
			return displayName;
		return toString();
	}

	/**
	* Overrides Object.equals().
	* This returns true if and only if the argument is a DataType and
	* the standard and code are equal.
	  @param obj the other data type
	  @return true if this is equal to obj
	*/
	public boolean equals(Object obj)
	{
		if (!(obj instanceof DataType))
		    return false;
		DataType dt = (DataType)obj;
		if (dt == this)
			return true;
		return standard.equalsIgnoreCase(dt.standard)
		  && code.equalsIgnoreCase(dt.code)
		  && TextUtil.strEqualIgnoreCase(displayName, dt.getDisplayName());
	}

	// ---------- DatabaseObject methods ----------------

	/** @return "DataType" */
	public String getObjectType() { return "DataType"; }

	@Override
	public void prepareForExec() {}

	@Override
	public boolean isPrepared() { return true; }

	/** Does nothing. */
	public void read() {}

	/** Does nothing. */
	public void write() {}

	public void setDisplayName(String displayName)
	{
		this.displayName = displayName;
	}

}
