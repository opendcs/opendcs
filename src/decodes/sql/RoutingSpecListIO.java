/*
 * $Id$
 *
 * Open Source Software
 *
 * $Log$
 * Revision 1.3  2014/10/07 12:40:45  mmaloney
 * dev
 *
 * Revision 1.2  2014/09/15 14:04:56  mmaloney
 * Code cleanup.
 *
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
 * Revision 1.17  2007/07/30 19:50:49  mmaloney
 * fixed bug - removed unneeded line
 *
 * Revision 1.16  2007/07/30 19:40:45  mmaloney
 * Fixed bug when re-initializing a routing spec
 *
 * Revision 1.15  2007/04/20 14:31:33  ddschwit
 * Changed SELECT * to SELECT <columnlist>
 *
 * Revision 1.14  2006/05/11 13:32:34  mmaloney
 * DataTypes are now immutable! Modified all references. Modified SQL IO code.
 *
 * Revision 1.13  2004/02/05 21:50:23  mjmaloney
 * final release prep for 6.0
 *
 * Revision 1.12  2003/11/15 20:28:37  mjmaloney
 * Mods to transparently support either V5 or V6 database.
 *
 * Revision 1.11  2003/09/10 18:59:49  mjmaloney
 * Implemented method to read (or re-read) a single routing spec.
 *
 * Revision 1.10  2002/12/21 20:28:57  mjmaloney
 * Bug fixes.
 *
 * Revision 1.9  2002/11/25 13:32:52  mjmaloney
 * Bug fixes
 *
 * Revision 1.8  2002/10/06 14:23:58  mjmaloney
 * SQL Development.
 *
 * Revision 1.7  2002/10/04 13:32:12  mjmaloney
 * SQL dev.
 *
 * Revision 1.6  2002/09/24 13:17:48  mjmaloney
 * SQL dev.
 *
 * Revision 1.5  2002/09/20 12:59:07  mjmaloney
 * SQL Dev.
 *
 * Revision 1.4  2002/09/19 12:18:05  mjmaloney
 * SQL Updates
 *
 * Revision 1.3  2002/08/29 05:48:50  chris
 * Added RCS keyword headers.
 *
 *
 */

package decodes.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Date;

import opendcs.dai.PropertiesDAI;
import opendcs.dao.PropertiesSqlDao;

import ilex.util.Logger;
import ilex.util.TextUtil;

import decodes.db.Constants;
import decodes.db.DatabaseException;
import decodes.db.DataSource;
import decodes.db.RoutingSpec;
import decodes.db.RoutingSpecList;
import decodes.tsdb.DbIoException;

/**
 * This class handles reading and writing the RoutingSpecLists from/to
 * the SQL database.
 */

public class RoutingSpecListIO extends SqlDbObjIo
{
	/**
	* Transient storage for the RoutingSpecList that's being read or written.
	*/
	RoutingSpecList _rsList;

	/**
	* This is used to look up the ID numbers and names of PresentationGroups
	*/
	PresentationGroupListIO _pgListIO;

	/**
	* This is used to look up NetworkList objects that are associated with
	* a RoutingSpec.
	*/
	NetworkListListIO _nllIO;


	/** Constructor. */
	public RoutingSpecListIO(SqlDatabaseIO dbio,
							 PresentationGroupListIO pgListIO,
							 NetworkListListIO nllIO)
	{
		super(dbio);
		
		_pgListIO = pgListIO;
		_nllIO = nllIO;
	}

	/**
	* Reads in the RoutingSpecList from the SQL database.
	* For each RoutingSpec, this also reads the associated records in the
	* RoutingSpecNetworkList table and the RoutingSpecProperty table.
	*/
	public void read(RoutingSpecList rsList)
		throws SQLException, DatabaseException
	{
		Logger.instance().log(Logger.E_DEBUG1,"Reading RoutingSpecs...");

		_rsList = rsList;

		Statement stmt = createStatement();
		
		String q = "SELECT id, name, dataSourceId, enableEquations, " +
		           "usePerformanceMeasurements, outputFormat, outputTimeZone, " +
		           "presentationGroupName, sinceTime, untilTime, " +
		           "consumerType, consumerArg, lastModifyTime, isProduction " +
				   "FROM RoutingSpec";
		
		ResultSet resultSet = stmt.executeQuery( q );

		while (resultSet != null && resultSet.next()) 
		{
			DbKey id = DbKey.createDbKey(resultSet, 1);
			String name = resultSet.getString(2);

			RoutingSpec routingSpec = new RoutingSpec(name);

			routingSpec.setId(id);

			DbKey dataSourceId = DbKey.createDbKey(resultSet, 3);
			routingSpec.dataSource = null;
			try
			{
				DataSource ds = _dbio._dataSourceListIO.getDS(
					rsList, dataSourceId);
				routingSpec.dataSource = ds;
			}
			catch(DatabaseException ex)
			{
				Logger.instance().log(Logger.E_WARNING,
					"Invalid dataSourceId " + dataSourceId
					+ " in routing spec '" + name 
					+ "' -- valid data source must be assigned before this"
					+ " spec can be used: " + ex);
			}

			routingSpec.enableEquations = 
				TextUtil.str2boolean(resultSet.getString(4));
			routingSpec.usePerformanceMeasurements =
				TextUtil.str2boolean(resultSet.getString(5));

			routingSpec.outputFormat = resultSet.getString(6);

			routingSpec.outputTimeZoneAbbr = resultSet.getString(7);
			//System.out.println("outputTimeZoneAbbr = " +
			//	routingSpec.outputTimeZoneAbbr);

			routingSpec.presentationGroupName = resultSet.getString(8);

			routingSpec.sinceTime = resultSet.getString(9);
			routingSpec.untilTime = resultSet.getString(10);

			routingSpec.consumerType = resultSet.getString(11);

			routingSpec.consumerArg = resultSet.getString(12);
			if (routingSpec.consumerArg == null)
				routingSpec.consumerArg = "";

			routingSpec.lastModifyTime = getTimeStamp(resultSet, 13, 
				routingSpec.lastModifyTime);
			//Timestamp ts = resultSet.getTimestamp(13);
			//if (!resultSet.wasNull())
				//routingSpec.lastModifyTime = ts;

			//System.out.println("Last modify time is " +
			//	routingSpec.lastModifyTime);
			routingSpec.isProduction = 
				TextUtil.str2boolean(resultSet.getString(14));

			// Get the properties associated with this object.
			PropertiesDAI propsDao = _dbio.makePropertiesDAO();
			try { propsDao.readProperties("RoutingSpecProperty", "RoutingSpecId", routingSpec.getId(),
				routingSpec.getProperties()); }
			catch (DbIoException e)
			{
				throw new DatabaseException(e.getMessage());
			}
			finally
			{
				propsDao.close();
			}

			// Get the NetworkLits associated with this database.

			read_RS_NL(routingSpec);

			rsList.add(routingSpec);
		}
		stmt.close();
	}

	/**
	  Reads an existing routing spec into memory. 
	  The passed object is filled-in.
	*/
	public void readRoutingSpec(RoutingSpec routingSpec)
		throws DatabaseException
	{
		if (!routingSpec.idIsSet()
		 && routingSpec.getName() != null && routingSpec.getName().length() > 0)
		{
			try { routingSpec.setId(name2id(routingSpec.getName())); }
			catch(SQLException ex) { routingSpec.clearId(); }
		}
		if (!routingSpec.idIsSet())
			throw new DatabaseException(
				"Cannot retrieve RoutingSpec with no name or ID.");

		String q = "SELECT id, name, dataSourceId, enableEquations, " +
                   "usePerformanceMeasurements, outputFormat, outputTimeZone, " +
                   "presentationGroupName, sinceTime, untilTime, " +
                   "consumerType, consumerArg, lastModifyTime, isProduction " +
		           "FROM RoutingSpec WHERE id = " + routingSpec.getId();

		try
		{
			debug2("RoutingSpecListIO.readroutingSpec: " + q);
			Statement stmt = createStatement();
			ResultSet resultSet = stmt.executeQuery( q );
			if (resultSet == null)
				throw new DatabaseException("No RoutingSpec found with id "
					+ routingSpec.getId());

			// There will be only one row in the result set.
			resultSet.next();
			routingSpec.setName(resultSet.getString(2));

			DbKey dataSourceId = DbKey.createDbKey(resultSet, 3);
			routingSpec.dataSource = null;
			try
			{
				routingSpec.dataSource = _dbio._dataSourceListIO.getDS(
					routingSpec, dataSourceId);
			}
			catch(DatabaseException ex)
			{
				Logger.instance().log(Logger.E_WARNING,
					"Invalid dataSourceId " + dataSourceId
					+ " in routing spec '" + routingSpec.getName() 
					+ "' -- valid data source must be assigned before this"
					+ " spec can be used: " + ex);
			}

			routingSpec.enableEquations = 
				TextUtil.str2boolean(resultSet.getString(4));
			routingSpec.usePerformanceMeasurements =
				TextUtil.str2boolean(resultSet.getString(5));
			routingSpec.outputFormat = resultSet.getString(6);
			routingSpec.outputTimeZoneAbbr = resultSet.getString(7);
			routingSpec.presentationGroupName = resultSet.getString(8);
			routingSpec.sinceTime = resultSet.getString(9);
			routingSpec.untilTime = resultSet.getString(10);
			routingSpec.consumerType = resultSet.getString(11);
			routingSpec.consumerArg = resultSet.getString(12);
			if (routingSpec.consumerArg == null)
				routingSpec.consumerArg = "";
			routingSpec.lastModifyTime = getTimeStamp(resultSet, 13, 
				routingSpec.lastModifyTime);
			routingSpec.isProduction = 
				TextUtil.str2boolean(resultSet.getString(14));

			PropertiesDAI propsDao = _dbio.makePropertiesDAO();
			try { propsDao.readProperties("RoutingSpecProperty", "RoutingSpecId", routingSpec.getId(),
				routingSpec.getProperties()); }
			catch (DbIoException e)
			{
				throw new DatabaseException(e.getMessage());
			}
			finally
			{
				propsDao.close();
			}

			// Get the NetworkLists associated with this database.
			read_RS_NL(routingSpec);

			// Add this routing spec to the routing spec list.
			routingSpec.getDatabase().routingSpecList.add(routingSpec);
			stmt.close();
		}
		catch(SQLException ex)
		{
			throw new DatabaseException("Error on query '" + q + "': "+ex);
		}
	}

	/**
	* This reads all the records of the RoutingSpecNetworkList table
	* associated with a particular RoutingSpec.  It then adds the
	* NetworkList names to the list of such names in the RoutingSpec.
	*/
	public void read_RS_NL(RoutingSpec routingSpec)
		throws DatabaseException, SQLException
	{
		Statement stmt = createStatement();
		String q =
			"SELECT networkListName FROM RoutingSpecNetworkList " +
			"WHERE RoutingSpecId = " + routingSpec.getId() + " ";

		ResultSet resultSet = stmt.executeQuery(q);
		if (resultSet != null) {
			while (resultSet.next()) 
			{
				String nm = resultSet.getString(1);
				//Logger.instance().debug1("read_RS_NL: After select for RS " 
				// + routingSpec.getName() + "' adding list " + nm);
				routingSpec.addNetworkListName(nm);
			}
		}
		//Logger.instance().debug1("read_RS_NL: After select for RS " + routingSpec.getName() + "', there are " 
		//+ routingSpec.networkListNames.size() +" NL Names.");
		stmt.close();
	}

	/**
	* This writes a RoutingSpec out to the database.  It could either
	* be a new object or a pre-existing object that has changed.
	*/
	public void write(RoutingSpec rs)
		throws DatabaseException, SQLException
	{
		if (rs.idIsSet()) 
			update(rs);
		else
		{
			DbKey id = name2id(rs.getName());
			if (!id.isNull())
			{
				rs.setId(id);
				update(rs);
			}
			else
				insert(rs);
		}
	}

	/**
	* Update a pre-existing RoutingSpec in the SQL database.
	*/
	public void update(RoutingSpec rs)
		throws DatabaseException, SQLException
	{
		DbKey id = rs.getId();
		if (rs.consumerArg != null && rs.consumerArg.trim().length() == 0)
			rs.consumerArg = null;
		
		String q =
			"UPDATE RoutingSpec SET " +
			  "Name = " + sqlString(rs.getName()) + ", " +
			  "DataSourceId = " + rs.dataSource.getId() + ", " +
			  "EnableEquations = " + sqlString(rs.enableEquations) + ", " +
			  "UsePerformanceMeasurements = " +
				sqlString(rs.usePerformanceMeasurements) + ", " +
			  "OutputFormat = " + sqlOptString(rs.outputFormat) + ", " +
			  "OutputTimeZone = " + sqlOptString(rs.outputTimeZoneAbbr) + ", " +

			  "presentationGroupName = " 
			  + sqlOptString(rs.presentationGroupName) + ", " +
			  "SinceTime = " + sqlOptString(rs.sinceTime) + ", " +
			  "UntilTime = " + sqlOptString(rs.untilTime) + ", " +
			  "ConsumerType = " + sqlOptString(rs.consumerType) + ", " + 
			  "ConsumerArg = " + sqlString(rs.consumerArg) + ", " +
			  "LastModifyTime = " + sqlDate(rs.lastModifyTime) + ", " +
			  "IsProduction = " + sqlString(rs.isProduction) + " " +
			"WHERE ID = " + id;

		executeUpdate(q);

		PropertiesDAI propsDao = _dbio.makePropertiesDAO();
		try { propsDao.writeProperties("RoutingSpecProperty", "RoutingSpecId", rs.getId(),
			rs.getProperties()); }
		catch (DbIoException e)
		{
			throw new DatabaseException(e.getMessage());
		}
		finally
		{
			propsDao.close();
		}

		// Update the RoutingSpecNetworkLists
		delete_RS_NL(rs);
		insert_RS_NL(rs);
	}

	/**
	* Insert a new RoutingSpec into the SQL database.  This also inserts
	* records into the RoutingSpecProperty and RoutingSpecNetworkList
	* tables, as required.
	*/
	public void insert(RoutingSpec rs)
		throws DatabaseException, SQLException
	{
		DbKey id = getKey("RoutingSpec");
		rs.setId(id);

		String q =
			"INSERT INTO RoutingSpec(id, name, datasourceid, enableequations, "
			+ "useperformancemeasurements, outputformat, outputtimezone, "
			+ "presentationgroupname, sincetime, untiltime, consumertype, "
			+ "consumerarg, lastmodifytime, isproduction)"
			+ " VALUES (" +
			  id + ", " +
			  sqlString(rs.getName()) + ", " +
			  rs.dataSource.getId() + ", " +
			  sqlString(rs.enableEquations) + ", " +
			  sqlString(rs.usePerformanceMeasurements) + ", " +
			  sqlOptString(rs.outputFormat) + ", " +
			  sqlOptString(rs.outputTimeZoneAbbr) + ", " +

			  sqlOptString(rs.presentationGroupName) + ", " +
			  sqlOptString(rs.sinceTime) + ", " +
			  sqlOptString(rs.untilTime) + ", " +
			  sqlOptString(rs.consumerType) + ", " + 
			  sqlString(rs.consumerArg) + ", " +
			  sqlDate(rs.lastModifyTime) + ", " +
			  sqlString(rs.isProduction) +
			")";

		executeUpdate(q);

		// Now insert the RoutingSpecNetworkList records
		insert_RS_NL(rs);

		// Now the properties
		PropertiesDAI propsDao = _dbio.makePropertiesDAO();
		try { propsDao.writeProperties("RoutingSpecProperty", "RoutingSpecId", rs.getId(),
			rs.getProperties()); }
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
	* Insert the RoutingSpecNetworkList records corresponding to a new
	* RoutingSpec.
	*/
	public void insert_RS_NL(RoutingSpec rs)
		throws DatabaseException, SQLException
	{
		DbKey rsId = rs.getId();

		//System.out.println("		RoutingSpecListIO.insert_RS_NL");

		for(Iterator<String> it = rs.networkListNames.iterator(); it.hasNext(); )
		{
			String nm = it.next();
			if (nm == null)
				continue; // shouldn't happen.
			String q =
				"INSERT INTO RoutingSpecNetworkList VALUES (" +
				  rsId + ", " + sqlReqString(nm) +
				")";

			executeUpdate(q);
		}
	}

	/**
	* Deletes a RoutingSpec from the list.  The SQL database ID must be set.
	* To maintain referential integrity (and because it's the "right" thing
	* to do) this also:
	*   1.  deletes all the records from RoutingSpecNetworkList that refer
	*	   to this RoutingSpec, and
	*   2.  deletes all the records from RoutingSpecProperty that "belong"
	*	   to this RoutingSpec.
	*/
	public void delete(RoutingSpec rs)
		throws DatabaseException, SQLException
	{
		DbKey id = rs.getId();

		// Do the related tables first
		delete_RS_NL(rs);

		PropertiesDAI propsDao = _dbio.makePropertiesDAO();
		try { propsDao.deleteProperties("RoutingSpecProperty", "RoutingSpecId", id); }
		catch (DbIoException e)
		{
			throw new DatabaseException(e.getMessage());
		}
		finally
		{
			propsDao.close();
		}

		// Finally, do the main RoutingSpec table
		String q = "DELETE FROM RoutingSpec WHERE ID = " + id;
		executeUpdate(q);
	}

	/**
	* Deletes the RoutingSpecNetworkList records corresponding to a
	* RoutingSpec.
	*/
	public void delete_RS_NL(RoutingSpec rs)
		throws DatabaseException, SQLException
	{
		DbKey id = rs.getId();

		String q = "DELETE FROM RoutingSpecNetworkList " +
				   "WHERE RoutingSpecId = " + id;
		tryUpdate(q);
	}

	private DbKey name2id(String name)
		throws SQLException
	{
		Statement stmt = createStatement();
		ResultSet rs = stmt.executeQuery(
			"SELECT id FROM RoutingSpec where lower(name) = "
			+ sqlReqString(name.toLowerCase()));

		DbKey ret = Constants.undefinedId;
		if (rs != null && rs.next())
			ret = DbKey.createDbKey(rs, 1);

		stmt.close();
		return ret;
	}

	/**
	  Returns the last-modify-time for this routing spec in the database.
	*/
	public Date getLMT(RoutingSpec spec)
	{
		try
		{
			DbKey id = spec.getId();
			if (id.isNull())
			{
				id = name2id(spec.getName());    // will throw if unsuccessfull
				try { spec.setId(id); }
				catch(DatabaseException ex) {} // guaranteed not to happen.
			}

			Statement stmt = createStatement();
			String q = 
				"SELECT lastModifyTime FROM RoutingSpec WHERE id = " + id;
			ResultSet rs = stmt.executeQuery(q);

			// Should be only 1 record returned.
			if (rs == null || !rs.next())
			{
				Logger.instance().log(Logger.E_WARNING,
					"Cannot get SQL LMT for Routing Spec '"
					+ spec.getName() + "' id=" + spec.getId());
				return null;
			}

			Date ret = getTimeStamp(rs, 1, (Date)null);
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			Logger.instance().log(Logger.E_WARNING,
				"SQL Error reading LMT for RoutingSpec '"
				+ spec.getName() + "' ID=" + spec.getId() + ": " + ex);
			return null;
		}
	}
}

