/*
*  $Id$
*/
package decodes.db;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import java.util.Date;
import java.util.Vector;
import java.util.Iterator;
import java.util.Properties;

/**
 * This class encapsulates a DECODES RoutingSpec.
 */
public class RoutingSpec 
	extends IdDatabaseObject
{
	// _id is stored in the IdDatabaseObject superclass.

	/**
	* The name of this RoutingSpec.  Note that the name is also unique
	* among all the RoutingSpecs of this database.  In the XML database,
	* this is used to uniquely identify a RoutingSpec.
	*/
	private String name;

	/** True if equations for derived parameters are to be used. */
	public boolean enableEquations;

	/** True if performance measurements are to be output (not used yet). */
	public boolean usePerformanceMeasurements;

	/** Output format to be used - should match Enum value. */
	public String outputFormat;

	/**
	* This stores the time zone abbreviation.
	* This will be null if the output time zone is not known.
	*/
	public String outputTimeZoneAbbr;

	/** Name of presentation group determining units and precision. */
	public String presentationGroupName;

	/** Retrieve messages with time-stamp greater than or equal to this time. */
	public String sinceTime;

	/** Stop retrieving when time-stamp greater until time is seen. */
	public String untilTime;

	/**
	  Specifies what type of interface will consume the decoded data.
	  e.g. file, pipe, directory.
	*/
	public String consumerType;
	
	/** Argument to consumer. E.g. if file, this will be the file name. */
	public String consumerArg;

	/** Time this record was last modified. */
	public Date lastModifyTime;

	/**
	  True means that this routing spec is ready to be installed in the
	  production database.
	*/
	public boolean isProduction;

	/**
	  Retrieve only messages from platforms whos ID is contained in the
	  named network lists.
	*/
	public Vector<String> networkListNames;

	/** Properties that modify special features of the routing spec. */
	private Properties properties = new Properties();

	/** The data source that will be used to retrieve data. */
	public DataSource dataSource;

	// ==================================================================
	// Executable Links made by prepareForExec (not used elsewhere)
	// ==================================================================

	/** Prior to output, time stamps will be converted to this time zone. */
	public java.util.TimeZone outputTimeZone;

	/** Consumer object that will receive the decoded data. */
// Not used??
//	public DataConsumer dataConsumer;

	/**
	* The list of NetworkLists corresponding to the list of names in
	* networkListNames.  Note that this will be the empty Vector until
	* it's needed, at which time (in prepareForExec()),
	* the names will be resolved into references.
	*/
	public Vector<NetworkList> networkLists;

	/** True if this routing spec has successfully been prepared. */
	private boolean _isPrepared;

	/**
	* Constructor.
	*/
	public RoutingSpec()
	{
		super(); // sets _id to Constants.undefinedId;

		setName(null);

		//dataSourceId = Constants.undefinedId;;
		enableEquations = false;
		usePerformanceMeasurements = false;
		outputFormat = null;
		outputTimeZoneAbbr = null;
		presentationGroupName = null;
		sinceTime = null;
		untilTime = null;
		consumerType = null;
		consumerArg = null;
		lastModifyTime = null;
		isProduction = false;
		networkListNames = new Vector<String>();
		dataSource = null;
		outputTimeZone = null;
//		dataConsumer = null;
		networkLists = new Vector<NetworkList>();
		_isPrepared = false;
	}

	/**
	  Construct RoutingSpec with a given name.
	  @param name the name
	*/
	public RoutingSpec(String name)
	{
		this();
		this.setName(name);
	}


	/** 
	  Makes a unique string suitable for use as a filename. 
	  @return String suitable for use as a file name
	*/
	public String makeFileName()
	{
		StringBuffer ret = new StringBuffer(getName());
		for(int i=0; i<ret.length(); i++)
			if (Character.isWhitespace(ret.charAt(i)))
				ret.setCharAt(i, '-');
		return ret.toString();
	}

	/** @return "RoutingSpec" */
	public String getObjectType() { return "RoutingSpec"; }

	/**
	  Used by the editor - make a deep copy of this object
	  Note:  the dataSource member is not copied.
	  @return deep copy
	*/
	public RoutingSpec copy()
	{
		RoutingSpec ret = new RoutingSpec(getName());
		copy(ret, this);
		return ret;
	}
	
	public static void copy(RoutingSpec copyTo, RoutingSpec copyFrom)
	{
		try { copyTo.setId(copyFrom.getId()); }
		catch(DatabaseException ex) {} // won't happen.

		copyTo.dataSource = copyFrom.dataSource;
		copyTo.enableEquations = copyFrom.enableEquations;
		copyTo.usePerformanceMeasurements = copyFrom.usePerformanceMeasurements;
		copyTo.outputFormat = copyFrom.outputFormat;
		copyTo.outputTimeZoneAbbr = copyFrom.outputTimeZoneAbbr;
		copyTo.presentationGroupName = copyFrom.presentationGroupName;
		copyTo.sinceTime = copyFrom.sinceTime;
		copyTo.untilTime = copyFrom.untilTime;
		copyTo.consumerType = copyFrom.consumerType;
		copyTo.consumerArg = copyFrom.consumerArg;
		copyTo.isProduction = copyFrom.isProduction;
		for(Iterator<String> it=copyFrom.networkListNames.iterator(); it.hasNext(); )
			copyTo.addNetworkListName(it.next());
		PropertiesUtil.copyProps(copyTo.properties, copyFrom.properties);
	}

	/**
	  Returns true if this routing spec equals the passed one.
	  Note: SQL ID is not checked for equality.
	*/
	public boolean equals(Object ob)
	{
		if (!(ob instanceof RoutingSpec))
			return false;
		RoutingSpec rs = (RoutingSpec)ob;
		if (this == rs)
			return true;
		if (!rs.getName().equals(getName())
		 || rs.enableEquations != enableEquations
		 || rs.usePerformanceMeasurements != usePerformanceMeasurements
		 || !TextUtil.strEqualIgnoreCase(rs.outputFormat, outputFormat)
		 || !TextUtil.strEqualIgnoreCase(rs.outputTimeZoneAbbr, 
				outputTimeZoneAbbr))
			return false;
//System.out.println("equals: checking presentationGroupName");
//System.out.println("equals: presentationGroupName: '" + rs.presentationGroupName + "' '" + presentationGroupName + "'");
//System.out.println("equals: Sinces: '" + rs.sinceTime + "' '" + sinceTime + "'");
//System.out.println("equals: Untils: '" + rs.untilTime + "' '" + untilTime + "'");
//System.out.println("equals: consumerType: '" + rs.consumerType + "' '" + consumerType + "'");
//System.out.println("equals: consumerArg: '" + rs.consumerArg + "' '" + consumerArg + "'");
//System.out.println("equals: isProduction: '" + rs.isProduction + "' '" + isProduction + "'");

		if (!TextUtil.strEqualIgnoreCase(rs.presentationGroupName, 
				presentationGroupName)
		 || !TextUtil.strEqualIgnoreCase(rs.sinceTime, sinceTime)
		 || !TextUtil.strEqualIgnoreCase(rs.untilTime, untilTime)
		 || !TextUtil.strEqualIgnoreCase(rs.consumerType, consumerType)
		 || !TextUtil.strEqualIgnoreCase(rs.consumerArg, consumerArg)
		 || rs.isProduction != isProduction)
		{
			return false;
		}
//System.out.println("equals: checking datasource");
		String dsn1 = dataSource != null ? dataSource.getName() : null;
		String dsn2 = rs.dataSource != null ? rs.dataSource.getName() : null;
		if (!TextUtil.strEqualIgnoreCase(dsn1, dsn2))
			return false;
		if (networkListNames.size() != rs.networkListNames.size())
			return false;
//System.out.println("equals: checking netlist names");
	nextName:
		for(Iterator<String> it=networkListNames.iterator(); it.hasNext(); )
		{
			String s = it.next();
			Iterator<String> it2 = rs.networkListNames.iterator();
			while(it2.hasNext())
			{
				String s2 = it2.next();
				if (s.equals(s2))
					continue nextName;
			}
			return false; // fell through
		}
//System.out.println("equals: checking properties");
		if (!PropertiesUtil.propertiesEqual(getProperties(), rs.getProperties()))
			return false;
		return true;
	}

	/** 
	  Adds a network list name to this routing spec. 
	  @param listname the list name to add.
	*/
	public void addNetworkListName(String listname)
	{
		if (!networkListNames.contains(listname))
			networkListNames.add(listname);
	}


	/** From DatabaseObject interface, */
	public void prepareForExec()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		// resolve network list names and get/load network lists.
		Iterator<String> it;
		for(it = networkListNames.iterator(); it.hasNext();)
		{
			String name = it.next();
			NetworkList nl = myDatabase.networkListList.find(name);
			if (nl == null)
			{
				// Not loaded yet? Try to load it.
				nl = new NetworkList(name);
				try { nl.read(); }
				catch(DatabaseException e)
				{
					throw new IncompleteDatabaseException(
						"Cannot load network list '" + nl.name + "': " + e);
				}
				myDatabase.networkListList.add(nl);
			}
			networkLists.add(nl);
			if (!nl.isPrepared())
				nl.prepareForExec();
		}
		// Resolve time zone name to an object:
		if (outputTimeZoneAbbr == null)
			outputTimeZone = java.util.TimeZone.getTimeZone("UTC");
		else
		{
			outputTimeZone = java.util.TimeZone.getTimeZone(outputTimeZoneAbbr);
			if (outputTimeZone == null)
				throw new IncompleteDatabaseException(
				"Invalid time zone abbreviation '" + outputTimeZoneAbbr + "'");
		}

		// If Equations are to be used, resolve which are to be used and
		// prepare them for execution.
		//if (enableEquations)
		//	? future

		_isPrepared = true;
	}

	/** From DatabaseObject interface, */
	public boolean isPrepared()
	{
		return _isPrepared;
	}

	/** From DatabaseObject interface, */
	public void validate()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
	}

	/** Reads this complete routing spec from the database. */
	public void read()
		throws DatabaseException
	{
		myDatabase.getDbIo().readRoutingSpec(this);
	}

	/** Writes this routing spec out to the database. */
	public void write()
		throws DatabaseException
	{
		lastModifyTime = new Date();
		myDatabase.getDbIo().writeRoutingSpec(this);
	}

	/**
	* Sets a routing spec property name/value.
	* @param name the property name.
	* @param value the property value.
	*/
	public void setProperty(String name, String value)
	{
		getProperties().setProperty(name, value);
	}

	/**
	 * Return the value for the specified name, or null if not found.
	 * @param name the property name.
	 * @return the value for the specified name, or null if not found.
	 */
	public String getProperty(String name)
	{
		return PropertiesUtil.getIgnoreCase(getProperties(), name);
	}

	public String getDisplayName()
	{
		return getName();
	}

	public String getName()
	{
		return name;
	}

	/**
	 * @return the properties
	 */
	public Properties getProperties()
	{
		return properties;
	}

	/**
	 * @param properties the properties to set
	 */
	public void setProperties(Properties properties)
	{
		this.properties = properties;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name)
	{
		this.name = name;
	}
}
