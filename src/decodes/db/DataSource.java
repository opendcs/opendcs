/*
*  $Id$
*/
package decodes.db;

import decodes.datasource.DataSourceExec;
import decodes.datasource.DataSourceException;
import decodes.datasource.RawMessage;
import decodes.sql.DbKey;

import ilex.util.TextUtil;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;

import java.util.Vector;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;

/**
* This is the database DataSource object.  It holds the raw
* information defined in the database (or XML file). For editors and
* installers, that is all that this class needs to do. When running
* the decoder, this class delegates to a real active data source
* object (in the decodes.decoder package).
*/
public class DataSource extends IdDatabaseObject
{
	// Note - _id member stored in IdDatabaseObject superclass.

	/** Unique name for this DataSource */
	private String name;

	/** Matches an enumvalue for data source type */
	public String dataSourceType;

	/** Data source arg will vary depending on the type */
	public String dataSourceArg;

	/**
	* This is a reference to the other DataSource objects that belong to
	* this DataSource group.  This is only used if this is a data source
	* group.  If this DataSource is not a group, this will be an empty Vector.
	*/
	public Vector<DataSource> groupMembers;

	/**
	* This is a reference to a java.util.Properties object which holds the
	* arguments for this DataSource.
	*/
	public Properties arguments;

	// Dynamic executable data
	boolean _isPrepared;

	/**
	* This is used in editor: # of routing specs using this source.
	*/
	public int numUsedBy;

	/**
	* Constructor.
	*/
	public DataSource()
	{
		super(); // Sets _id to Constants.undefinedId

		setName(null);
		dataSourceType = null;
		dataSourceArg = null;
		groupMembers = new Vector<DataSource>();
		arguments = null;
		numUsedBy = 0;
	}

	/**
	  Construct from a name and a data source type.  The type should match
	  one of the DataSourceType enum values, but no checking is done to
	  ensure that.
	  @param name the name of this DataSource
	  @param type the type of this DataSource
	*/
	public DataSource(String name, String type)
	{
		this();
		this.setName(name);
		this.dataSourceType = type;
	}

	/**
	  This static method returns true if the DataSourceType enum value
	  passed in corresponds to a group type.  As of this writing, there
	  are only two:  roundrobingroup and hotbackupgroup.  The criterion
	  for determining that a type is a group is that it ends with the
	  string "roup" (not case sensitive).
	  @param type the type to check
	*/
	public static boolean isGroupType(String type)
	{
		 return type.toLowerCase().endsWith("roup");
	}

	/**
	@return true if this particular DataSource is a group type.
	*/
	public boolean isGroupType()
	{
		return isGroupType(dataSourceType);
	}

	/**
	Constructs DataSource object given a database ID.
	*/
	public DataSource(DbKey dataSourceId)
	{
		this();
		try { this.setId(dataSourceId); }
		catch(DatabaseException ex) {} // won't happen.
	}

	/**
	@return a copy of this DataSource.
	*/
	public DataSource copy()
	{
		DataSource ret = new DataSource(getName(), dataSourceType);
		try { ret.setId(getId()); }
		catch(DatabaseException ex) {} // won't happen.

		ret.dataSourceArg = dataSourceArg;
		int i = 0;
		for(Iterator<DataSource> it = groupMembers.iterator(); it.hasNext(); )
			ret.addGroupMember(i++, it.next());
		if (dataSourceArg == null || TextUtil.isAllWhitespace(dataSourceArg))
			ret.arguments = new Properties();
		else
			ret.arguments = PropertiesUtil.string2props(dataSourceArg);
		ret._isPrepared = false;
		ret.numUsedBy = numUsedBy;
		return ret;
	}

	/**
	  @param rhs right-hand-side
	  @return true if the passed DataSource is equal to this one.
	*/
	public boolean equals(Object rhs)
	{
		if (rhs == null || !(rhs instanceof DataSource))
			return false;
		DataSource ds = (DataSource)rhs;
		if (this == ds)
			return true;
		if (!getName().equalsIgnoreCase(ds.getName()))
			return false;
		if (!dataSourceType.equalsIgnoreCase(ds.dataSourceType))
			return false;
		if (ds.dataSourceArg == null)
			ds.dataSourceArg = "";
		if (this.dataSourceArg == null)
			dataSourceArg = "";
		Properties p1 = PropertiesUtil.string2props(dataSourceArg.toLowerCase());
		Properties p2 = PropertiesUtil.string2props(ds.dataSourceArg.toLowerCase());
		if (!PropertiesUtil.propertiesEqual(p1, p2))
			return false;
		if (groupMembers.size() != ds.groupMembers.size())
			return false;
		for(int i = 0 ; i < groupMembers.size(); i++)
		{
			DataSource gm1 = (DataSource)groupMembers.elementAt(i);
			DataSource gm2 = (DataSource)ds.groupMembers.elementAt(i);
			if (gm1 != gm2)
				return false;
		}
		//Fix the bug with the assigned -1 datasource ID
		try {
			((DataSource)rhs).setId(this.getId());
			return true;
		} catch (DatabaseException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	  Makes a string from this DataSource's name, suitable for use as a
	  filename.
	  @return String suitable for use as a filename.
	*/
	public String makeFileName()
	{
		StringBuffer ret = new StringBuffer(getName());
		for(int i=0; i<ret.length(); i++)
			if (Character.isWhitespace(ret.charAt(i)))
				ret.setCharAt(i, '-');
		return ret.toString();
	}

	/**
	  Counts the number of routing specs using this data source.
	  The number is stored internally in the 'numUsedBy' variable.
	*/
	public void countUsedBy()
	{
		numUsedBy = 0;
		for(Iterator<RoutingSpec> rit = Database.getDb().routingSpecList.iterator();
			rit.hasNext(); )
		{
			RoutingSpec rs = rit.next();
			if (rs.dataSource != null
			 && rs.dataSource.getName().equalsIgnoreCase(getName()))
				numUsedBy++;
		}
	}

	/**
	  This overrides the DatabaseObject method, and returns "DataSource".
	  @return "DataSource
	*/
	public String getObjectType() {
		 return "DataSource";
	}

	/**
	  Adds a DataSource to this group.  
	  Note that no checking is done to verify that this object is a group.
	  @param sequenceNum the order in which to try this member
	  @param member the member
	*/
	public void addGroupMember(int sequenceNum, DataSource member)
	{
		if (groupMembers.size() <= sequenceNum)
			groupMembers.setSize(sequenceNum+1);
		groupMembers.setElementAt(member, sequenceNum);
	}
	
	public void rmGroupMember(String memberName)
	{
		for(Iterator<DataSource> dsit = groupMembers.iterator(); dsit.hasNext(); )
		{
			DataSource gm = dsit.next();
			if (gm.getName().equalsIgnoreCase(memberName))
			{
				dsit.remove();
				return;
			}
		}
	}

	/**
	* @return the number of members, if this DataSource is a group or if not,
	* return zero.
	*/
	public int numGroupMembers()
	{
		int num=0;
		for(int i=0; i<groupMembers.size(); i++)
		{
			DataSource ds = (DataSource)groupMembers.elementAt(i);
			if (ds != null)
				num++;
		}
		return num;
	}

	/** @return true if the named DataSource is in this group. */
	public boolean isInGroup(String name)
	{
		for(int i=0; i<groupMembers.size(); i++)
		{
			DataSource ds = (DataSource)groupMembers.elementAt(i);
			if (ds != null && ds.getName().equalsIgnoreCase(name))
				return true;
		}
		return false;
	}
	
	/**
	* Overrides the DatabaseObject method.
	*/
	public void prepareForExec()
		throws InvalidDatabaseException
	{
		// Parse arguments from string into Properties object. This is simply
		// a convenience for the executable classes.
		arguments = PropertiesUtil.string2props(dataSourceArg);
	}

	/**
	  Makes an executable data source that corresponds to this database
	  object DataSource. This is used when initializing a new routing
	  spec. All executable context info (like sockets, files, directories)
	  has to be stored in the executable class (not this class) so that
	  multiple concurrent RoutingSpecs can share the same DataSource.
	  @return return executable data source delegate
	  @throws InvalidDatabaseException if cannot instantiate a class for
	  the given name.
	*/
	public DataSourceExec makeDelegate()
		throws InvalidDatabaseException
	{
		if (!isPrepared())
			prepareForExec();

		// Get the Data Source Type Enumeration from the database
		Database db = Database.getDb();
		DbEnum dsTypes = db.getDbEnum("DataSourceType");
		if (dsTypes == null)
			throw new InvalidDatabaseException(
				"Cannot prepare data source '" + getName() +
				"' No Enumeration for DataSourceType");

		// Lookup the value corresponding to this object's type.
		EnumValue mySourceType = dsTypes.findEnumValue(dataSourceType);
		if (mySourceType == null)
			throw new InvalidDatabaseException(
				"Cannot prepare data source '" + getName() +
				"': No Data Source Enumeration Value for '" +
				dataSourceType+"'");

		// Instantiate a concrete data source to delegate to.
		DataSourceExec ret;
		try
		{
			Class dsClass = mySourceType.getExecClass();
			if (dsClass == null)
				Logger.instance().log(Logger.E_FAILURE,
					"No exec class defined for data source type '"
					+ mySourceType.value + "'");
			Logger.instance().debug2("Making data source delegate '" 
				+ mySourceType.value + "' class='" + dsClass.getCanonicalName()
				+ "'");
			ret = (DataSourceExec)dsClass.newInstance();
			if (ret == null)
				Logger.instance().log(Logger.E_FAILURE,
					"newInstance for class '" + dsClass.getName()
					+ "' returned null");
			ret.setDataSource(this);
		}
		catch(Exception e)
		{
			throw new InvalidDatabaseException(
				"Cannot prepare data source '" + getName() +
				"': Cannot instantiate a Data Source of type '" + dataSourceType
				+ "': " + e.toString());
		}
		return ret;
	}

	@Override
	public boolean isPrepared()
	{
		return arguments != null;
	}

	/**
	* Overrides the DatabaseObject method.  This re-reads this DataSource
	* object from the database, using this object's name (_not_ it's
	* SQL database ID number) to identify it in the database.
	*/
	public void read()
		throws DatabaseException
	{
		myDatabase.getDbIo().readDataSource(this);
	}

	/**
	* Overrides the DatabaseObject method.  This writes this DataSource
	* object back out to the database.
	*/
	public void write()
		throws DatabaseException
	{
		myDatabase.getDbIo().writeDataSource(this);
	}

	public String getDisplayName()
	{
		return getName();
	}

	/**
	 * @return the name
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	public Properties getArguments()
	{
		return arguments;
	}
}

