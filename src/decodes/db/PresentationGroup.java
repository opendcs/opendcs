/*
*  $Id$
*/

package decodes.db;

import decodes.decoder.*;
import decodes.util.DecodesSettings;

import java.util.Date;
import java.util.Vector;
import java.util.HashMap;
import java.util.Iterator;

import ilex.util.Logger;
import ilex.util.TextUtil;

/**
* A PresentationGroup is a named container of DataPresentation objects.
* It "owns" the DataPresentation objects, in that when the PresentationGroup
* is deleted from the database, they are also deleted.
*/
public class PresentationGroup extends IdDatabaseObject
{
	// Note: _id stored in IdDatabaseObject superclass

	/**
	* The name of this PresentationGroup.  Note that this is unique
	* among all the PresentationGroups in this database.
	*/
	public String groupName;

	/**
	* This is the name of this PresentationGroup's "parent", if there is
	* one.  If not, this will be null.
	*/
	public String inheritsFrom;

	/** Last time this object was modified. */
	public Date lastModifyTime;

	/** True means this object is read to be put in the production database. */
	public boolean isProduction;

	/**
	* This is the list of DataPresentation objects in this group.
	*/
	public Vector<DataPresentation> dataPresentations = new Vector<DataPresentation>();

	/**
	* This is a reference to this PresentationGroup's parent, if any.
	* This is the group whose name is stored in the inheritsFrom member.
	*/
	public PresentationGroup parent;

//	/**
//	* This is constructed during the execution of the prepareForExec()
//	* method.  This is the combined map of all DP's in this group,
//	* including the DP's in this group's ancestors, if any.
//	*/
//	HashMap<MapKey, DataPresentation> presentationMap = new HashMap<MapKey, DataPresentation>();

	/** internal transient flag to prevent infinite recursion. */
	private boolean preparing;

	/** Default DP */
	DataPresentation defaultPresentation = null;
	
	private boolean _wasRead = false;
	public boolean wasRead() { return _wasRead; }

	/**
	* Constructor.
	*/
	public PresentationGroup()
	{
		super();  // Sets _id to Constants.undefinedId;
		clear();
		groupName = null;
		defaultPresentation = null;
	}

	/**
	  Zeros out all attributes as if object was newly created.
	*/
	public void clear()
	{
		inheritsFrom = null;
		lastModifyTime = null;
		isProduction = false;
		parent = null;
		clearList();
		preparing = false;
	}
	
	public void clearList()
	{
		dataPresentations.clear();
//		presentationMap.clear();
	}

	/**
	  Constructor with a name.
	  @param name the name of the group
	*/
	public PresentationGroup(String name)
	{
		this();
		this.groupName = name;
	}

	/**
	  Makes a string containing the suitable for use as a filename.
	  @return a string suitable as a file name for this group
	*/
	public String makeFileName()
	{
		StringBuffer ret = new StringBuffer(groupName);
		for(int i=0; i<ret.length(); i++)
			if (Character.isWhitespace(ret.charAt(i)))
				ret.setCharAt(i, '-');
		return ret.toString();
	}

	/**
	  Overrides the DatabaseObject method; this returns "PresentationGroup".
	  @return "PresentationGroup"
	*/
	public String getObjectType() {
		return "PresentationGroup";
	}

	/**
	  Adds a DataPresentation element to this group.
	  @param dp the DataPresentation to add
	*/
	public void addDataPresentation(DataPresentation dp)
	{
//System.out.println("PresentationGroup.addDataPresentation: dt=" 
//+ dp.dataType.getDisplayName() + ", units=" + dp.unitsAbbr);
		// MJM If there is a duplicate, remove it first.
		int n = dataPresentations.size();
		for(int i = 0; i<n; i++)
		{
			DataPresentation edp = dataPresentations.get(i);
		
			if (edp.getDataType() == dp.getDataType())
//			 && TextUtil.strEqualIgnoreCase(edp.getEquipmentModelName(),
//				 dp.getEquipmentModelName()))
			{
				dataPresentations.remove(i);
//System.out.println("PresentationGroup.addDataPresentation: removed duplicate first.");
				break;
			}
		}
		dataPresentations.add(dp);
	}

	/**
	  Makes a deep copy of this presentation group.
	  @return the copy
	*/
	public PresentationGroup copy()
	{
		PresentationGroup ret = new PresentationGroup(groupName);
		try { ret.setId(getId()); }
		catch(DatabaseException ex) {} // won't happen.

		ret.inheritsFrom = this.inheritsFrom;
		ret.lastModifyTime = new Date();
		ret.isProduction = this.isProduction;

		for(Iterator<DataPresentation> it = dataPresentations.iterator(); it.hasNext(); )
		{
			DataPresentation dp = it.next();
			ret.addDataPresentation(dp.copy(ret));
		}

		ret.parent = this.parent;
		return ret;
	}

	/**
	  Makes a deep copy but with a null database ID.
	  @return the copy
	*/
	public PresentationGroup noIdCopy()
	{
		PresentationGroup ret = copy();
		ret.clearId();
		for(Iterator<DataPresentation> it = ret.dataPresentations.iterator(); it.hasNext(); )
		{
			DataPresentation dp = (DataPresentation)it.next();
			dp.clearId();
		}
		return ret;
	}

	/**
	* Compares two PresentationGroups.  They are considered equal if the
	* groupName and inheritsFrom members match, and if, for every
	* DataPresentation in this group's list, there is a matching
	* DataPresentation in the other group's list.
	*/
	public boolean equals(Object rhs)
	{
		if (!(rhs instanceof PresentationGroup))
			return false;
		PresentationGroup pg = (PresentationGroup)rhs;

		// Check for identity
		if (this == pg) return true;

		if (!groupName.equals(pg.groupName)
			|| isProduction != pg.isProduction)
			return false;
		if (!TextUtil.strEqual(inheritsFrom, pg.inheritsFrom))
			return false;

		if (dataPresentations.size() != pg.dataPresentations.size())
			return false;
		for(Iterator<DataPresentation> it = dataPresentations.iterator(); it.hasNext(); )
		{
			DataPresentation dp = it.next();
			Iterator<DataPresentation> pgit;
			boolean matchFound = false;
			for(pgit = pg.dataPresentations.iterator();
				!matchFound && pgit.hasNext(); )
			{
				DataPresentation pgdp = pgit.next();
				matchFound = dp.equals(pgdp);
			}
			if (!matchFound)
				return false;
		}
		return true;
	}

	/**
	* @return the number of DataPresentations in this group, not including
	* any DataPresentations in this group's ancestors, if any.
	*/
	public int size() {
		return dataPresentations.size();
	}

	/** @return an Iterator into collection of DataPresentation objects. */
	public Iterator<DataPresentation> iterator()
	{
		return dataPresentations.iterator();
	}

	/**
	* This overrides the DatabaseObject method.
	*/
	public void prepareForExec()
		throws InvalidDatabaseException
	{
		if (preparing)
			throw new InvalidDatabaseException(
				"Circular reference to presentation group '" +
				groupName + "'");
		try
		{
			if (!wasRead())
				read();
			
//			addToMap(dataPresentations);

			if (inheritsFrom == null || inheritsFrom.length() == 0)
				parent = null;
			else
			{
				// Get & recursively prepare parent(s).
				parent = myDatabase.presentationGroupList.find(inheritsFrom);
				parent.prepareForExec();

				// Add parent's DP's to my map, if they're not already there.
//				addToMap(parent.dataPresentations);
			}
		}
		catch(DatabaseException ex)
		{
			String msg = "Cannot read presentation group " + getDisplayName()
				+ ": " + ex;
			System.err.println(msg);
			ex.printStackTrace(System.err);
			Logger.instance().failure(msg);
		}
		finally
		{
			preparing = false;
		}
	}

	/**
	* Overrides the DatabaseObject method.
	*/
	public boolean isPrepared()
	{
		return false;
	}

	/**
	* Overrides the DatabaseObject method.
	*/
	public void validate()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
	}

//	/**
//	* Used for the key in the presentation element map.
//	* The key is based on the datatype and (if defined) equipment model.
//	*/
//	class MapKey
//	{
//		DataType dataType;
//		String emName;
//		int myHashCode;
//
//		MapKey(DataType dt, String emn)
//		{
//			dataType = dt;
//			emName = emn == null ? "" : emn;
//			emName = emName.toLowerCase();
//			myHashCode = dt == null ? 0 : dataType.hashCode();
//			myHashCode ^= emName.hashCode();
//		}
//
//		public int hashCode()
//		{
//			return myHashCode;
//		}
//
//		public boolean equals(Object obj)
//		{
//			if (!(obj instanceof MapKey))
//				return false;
//			MapKey rhs = (MapKey)obj;
//			boolean dtEqual = 
//				(dataType == null && rhs.dataType == null)
//			 || (dataType != null && rhs.dataType != null 
//				&& dataType.equals(rhs.dataType));
//			return dtEqual && emName.equals(rhs.emName);
//		}
//	}

//	/**
//	* This method adds all of the DataPresentation objects in the
//	* argument to this group's presentationMap HashMap.
//	*/
//	private void addToMap(Vector<DataPresentation> dataPresentations)
//	{
//		// Make a map of presentation elements in this group.
//		for(DataPresentation dp : dataPresentations)
//		{
//			if (!dp.isPrepared())
//				dp.prepareForExec();
//
////System.out.println("addToMap: Adding datatype='" + dp.dataType.getDisplayName()
////+ "' to map, with abbr=" + dp.unitsAbbr);
//
////			MapKey key = new MapKey(dp.getDataType(), dp.getEquipmentModelName());
//			MapKey key = new MapKey(dp.getDataType(), "");
//			if (presentationMap.get(key) != null)
//				continue;  // Already in the map!
//
//			presentationMap.put(key, dp);
//	
//			if (dp.getDataType().getCode().equalsIgnoreCase("default"))
//				defaultPresentation = dp;
//		}
//	}

	/**
	* This overrides the DatabaseObject method.  This reads this
	* PresentationGroup from the database.
	*/
	public void read()
		throws DatabaseException
	{
//System.out.println("Reading Presentation Group '" + this.groupName + "'");
		myDatabase.getDbIo().readPresentationGroup(this);
		_wasRead = true;
	}

	/**
	* This overrides the DatabaseObject method.  This writes the
	* PresentationGroup back out to the database.
	*/
	public void write()
		throws DatabaseException
	{
		lastModifyTime = new Date();
		preparing = true;
		if (parent != null && !parent.preparing)
			parent.write();
		myDatabase.getDbIo().writePresentationGroup(this);
		preparing = false;
	}

	/**
	* @return a DataPresentation element by data type,
	* or null if not found.
	*/
	public DataPresentation findDataPresentation(DataType dt)
	{
		for (DataPresentation dp : dataPresentations)
			if (dp.getDataType().equals(dt))
				return dp;
		
		// MJM OpenDCS 6.5 For CWMS, try to match Base Param only.
		int idx = -1;
		if (dt.getStandard().equalsIgnoreCase(Constants.datatype_CWMS)
		 && (idx = dt.getCode().indexOf('-')) > 0)
		{
			DataType paseParam = DataType.getDataType(Constants.datatype_CWMS, dt.getCode().substring(0, idx));
			for (DataPresentation dp : dataPresentations)
				if (dp.getDataType().equals(paseParam))
					return dp;
		}

		// Recursively search parent groups.
		if (parent != null)
			return parent.findDataPresentation(dt);

		// No luck, use overall default.
		if (defaultPresentation == null)
			initDefaultPresentation();
		return defaultPresentation;
	}
	
	public DataPresentation findDataPresentation(Sensor sensor)
	{
		for (DataPresentation dp : dataPresentations)
		{
			for(Iterator<DataType> dtit = sensor.getAllDataTypes(); dtit.hasNext(); )
			{
				DataType dt = dtit.next();
				if (dt == null)
					continue;
				if (dp.getDataType().equals(dt))
					return dp;
			}
		}
		
		// MJM OpenDCS 6.5 For CWMS, try to match Base Param only.
		int idx = -1;
		DataType dt = sensor.getDataType(Constants.datatype_CWMS);
		if (dt != null && (idx = dt.getCode().indexOf('-')) > 0)
		{
			DataType paseParam = DataType.getDataType(Constants.datatype_CWMS, dt.getCode().substring(0, idx));
			for (DataPresentation dp : dataPresentations)
				if (dp.getDataType().equals(paseParam))
					return dp;
		}

		if (parent != null)
			return parent.findDataPresentation(sensor);

		if (defaultPresentation == null)
			initDefaultPresentation();
		return defaultPresentation;
	}

	/**
	  Initializes a default presentation used when there is none defined
	  in the database.
	  Reasonable rounding rules are implemented.
	*/
	private void initDefaultPresentation()
	{
		defaultPresentation = new DataPresentation();
		defaultPresentation.setMaxDecimals(DecodesSettings.instance().defaultMaxDecimals);

		defaultPresentation.prepareForExec();
	}

	public String getDisplayName()
	{
		return groupName;
	}
}
