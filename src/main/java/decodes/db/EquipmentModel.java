/*
*  $Id$
*/

package decodes.db;

import ilex.util.TextUtil;
import ilex.util.PropertiesUtil;
import java.util.Properties;
import java.util.Enumeration;

/**
* This class encapsulates information about a piece of equipment.
*/
public class EquipmentModel extends IdDatabaseObject
{
	// _id is stored in the IdDatabaseObject superclass.

	/** Unique name with no embedded spaces. */
	public String name;

	/** Type of equipment -- must match enum value. */
	public String equipmentType;

	/**
	* The manufacturer.
	* This will be null if the company is not known.
	*/
	public String company;

	/**
	* The model number.
	* This will be null if the model number is not known.
	*/
	public String model;

	/**
	* A description.  This will be null if there is no description.
	*/
	public String description;

	/**
	* A list of properties for this EquipmentModel.
	* This is never null.
	*/
	public Properties properties;

	/**
	* Default constructor.  At a minimum, the name of this object must
	* be set after the object has been constructed.
	*/
	public EquipmentModel()
	{
		super(); // sets _id to Constants.undefinedId;

		name = null;
		company = null;
		model = null;
		description = null;
		equipmentType = Constants.eqType_dcp;
		properties = new Properties();
	}

	/**
	* Construct from a name.  The case of the name is significant.
	  @param name the name of this equipment model
	*/
	public EquipmentModel(String name)
	{
		this();
		this.name = name;
	}


	/** Makes a deep copy of this equipment model.  
	  @return depp copy of this equipment model
	*/
	public EquipmentModel copy()
	{
		EquipmentModel ret = new EquipmentModel(name);
		try { ret.setId(getId()); }
		catch(DatabaseException ex) {} // won't happen.
		ret.name = name;
		ret.equipmentType = equipmentType;
		ret.company = company;
		ret.model = model;
		ret.description = description;
		ret.properties = (Properties)properties.clone();
		return ret;
	}

	/**
	 * Used for importing to make this object a copy of the passed object.
	 * Only the meaningful parameters are copied, not the id.
	 * @param rhs equipment model to copy from.
	 */
	public void copyFrom(EquipmentModel rhs)
	{
		this.name = rhs.name;
		this.equipmentType = rhs.equipmentType;
		this.company = rhs.company;
		this.model = rhs.model;
		this.description = rhs.description;
		this.properties = (Properties)rhs.properties.clone();
	}

	/**
	* This returns true if this EquipmentModel can be considered equal
	* to another.  Two EquipmentModels are equal if all of the data
	* members except _id are equal.
	*/
	public boolean equals(Object ob)
	{
		if (!(ob instanceof EquipmentModel))
			return false;
		EquipmentModel em = (EquipmentModel)ob;
		if (this == em)
			return true;

		if (!name.equals(em.name))
			return false;

		if (!TextUtil.strEqualIgnoreCase(equipmentType, em.equipmentType))
			return false;
		if (!TextUtil.strEqual(company, em.company))
			return false;
		if (!TextUtil.strEqual(model, em.model))
			return false;
		if (!TextUtil.strEqual(description, em.description))
			return false;
		if (!PropertiesUtil.propertiesEqual(properties, em.properties))
			return false;
		return true;
	}

	/**
	* This overrides the DatabaseObject's getObjectType() method.
	* This returns 'EquipmentModel'.
	*/
	public String getObjectType() { return "EquipmentModel"; }

	/**
	  Makes a string containing this EquipmentModel's name suitable for
	  use as a filename. Any embedded spaces in the name (bad) are replaced
	  by a hyphen.
	  @return String file name
	*/
	public String makeFileName()
	{
		StringBuffer ret = new StringBuffer(name);
		for(int i=0; i<ret.length(); i++)
			if (Character.isWhitespace(ret.charAt(i)))
				ret.setCharAt(i, '-');
		return ret.toString();
	}

	public String getName() { return name; }

	/**
	* This overrides the DatabaseObject's prepareForExec() method.
	* This does nothing.
	*/
	public void prepareForExec()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
	}

	/**
	* This overrides the DatabaseObject's isPrepared() method.
	* This returns true.
	*/
	public boolean isPrepared()
	{
		return true;
	}

	/**
	* This overrides the DatabaseObject's validate() method.
	* This checks to make sure that this EquipmentModel has a valid name.
	*/
	public void validate()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		if (name == null || name.length() == 0 ||
		 TextUtil.isAllWhitespace(name))
			throw new InvalidDatabaseException("No EquipmentModel name.");
	}

	/**
	* This overrides the DatabaseObject's read() method.
	* This reads a single EquipmentModel from the database.
	* This uses this EquipmentModel's name (not it's ID number) to
	* uniquely identify the record in the database.
	*/
	public void read()
		throws DatabaseException
	{
		myDatabase.getDbIo().readEquipmentModel(this);
	}

	/**
	* This overrides the DatabaseObject's write() method.
	* This writes a single EquipmentModel to the database.
	*/
	public void write()
		throws DatabaseException
	{
		myDatabase.getDbIo().writeEquipmentModel(this);
	}

	public String getDisplayName()
	{
		return name;
	}
}

