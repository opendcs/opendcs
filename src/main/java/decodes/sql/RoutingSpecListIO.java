package decodes.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;

import org.slf4j.LoggerFactory;

import java.util.Date;

import opendcs.dai.PropertiesDAI;
import opendcs.dai.ScheduleEntryDAI;
import ilex.util.TextUtil;
import decodes.db.Constants;
import decodes.db.DatabaseException;
import decodes.db.DataSource;
import decodes.db.RoutingSpec;
import decodes.db.RoutingSpecList;
import decodes.db.ScheduleEntry;
import decodes.tsdb.DbIoException;
import opendcs.dao.DaoBase;

/**
 * This class handles reading and writing the RoutingSpecLists from/to
 * the SQL database.
 */

public class RoutingSpecListIO extends SqlDbObjIo
{
    private final org.slf4j.Logger log = LoggerFactory.getLogger(RoutingSpecListIO.class);
    /**
    * This is used to look up the ID numbers and names of PresentationGroups
    */
    private PresentationGroupListIO _pgListIO;

    /**
    * This is used to look up NetworkList objects that are associated with
    * a RoutingSpec.
    */
    private NetworkListListIO _nllIO;

    private DataSourceListIO _dslIO;

    private PropertiesDAI propsDao = null;


    /** Constructor. */
    public RoutingSpecListIO(SqlDatabaseIO dbio,
                             PresentationGroupListIO pgListIO,
                             NetworkListListIO nllIO,
                             DataSourceListIO dslIO)
    {
        super(dbio);

        _pgListIO = pgListIO;
        _nllIO = nllIO;
        _dslIO = dslIO;
    }

    @Override
    public void setConnection(Connection conn)
    {
        super.setConnection(conn);
        // Have subordinates use my connection.
        _pgListIO.setConnection(conn);
        _nllIO.setConnection(conn);
        _dslIO.setConnection(conn);

        if (conn != null) // Opening
        {
            propsDao = _dbio.makePropertiesDAO();
            ((DaoBase)propsDao).setManualConnection(conn);
        }
        else // conn is null: closing
        {
            if (propsDao != null)
                propsDao.close();
            propsDao = null;
        }
    }

    /**
    * Reads in the RoutingSpecList from the SQL database.
    * For each RoutingSpec, this also reads the associated records in the
    * RoutingSpecNetworkList table and the RoutingSpecProperty table.
    */
    public void read(RoutingSpecList rsList)
        throws SQLException, DatabaseException
    {
        log.debug("Reading RoutingSpecs...");

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
                DataSource ds = _dbio._dataSourceListIO.getDS(rsList, dataSourceId);
                routingSpec.dataSource = ds;
            }
            catch(DatabaseException ex)
            {
                log.atWarn()
                   .setCause(ex)
                   .log(
                      "Invalid dataSourceId {} in routing spec '{}'"
                    + " -- valid data source must be assigned before this"
                    + " spec can be used: ",dataSourceId, name );
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
            {
                routingSpec.consumerArg = "";
            }

            routingSpec.lastModifyTime = getTimeStamp(resultSet, 13, routingSpec.lastModifyTime);

            routingSpec.isProduction = TextUtil.str2boolean(resultSet.getString(14));

            // Get the properties associated with this object.
            try
            {
                propsDao.readProperties(
                    "RoutingSpecProperty", "RoutingSpecId",
                    routingSpec.getId(), routingSpec.getProperties());
            }
            catch (DbIoException ex)
            {
                throw new DatabaseException("Unable to read routing spec.", ex);
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
        {
            throw new DatabaseException(
                "Cannot retrieve RoutingSpec with no name or ID.");
        }

        String q = "SELECT id, name, dataSourceId, enableEquations, " +
                   "usePerformanceMeasurements, outputFormat, outputTimeZone, " +
                   "presentationGroupName, sinceTime, untilTime, " +
                   "consumerType, consumerArg, lastModifyTime, isProduction " +
                   "FROM RoutingSpec WHERE id = " + routingSpec.getId();

        log.trace("RoutingSpecListIO.readroutingSpec: " + q);
        try (Statement stmt = createStatement();
             ResultSet resultSet = stmt.executeQuery(q);)
        {
            if (resultSet == null)
            {
                throw new DatabaseException("No RoutingSpec found with id "    + routingSpec.getId());
            }

            // There will be only one row in the result set.
            resultSet.next();
            routingSpec.setName(resultSet.getString(2));

            DbKey dataSourceId = DbKey.createDbKey(resultSet, 3);
            routingSpec.dataSource = null;
            try
            {
                routingSpec.dataSource = _dbio._dataSourceListIO.readDS(dataSourceId);
            }
            catch(DatabaseException ex)
            {
                log.atWarn()
                   .setCause(ex)
                   .log(
                      "Invalid dataSourceId {} in routing spec '{}'"
                    + " -- valid data source must be assigned before this"
                    + " spec can be used: ", dataSourceId, routingSpec.getName() );
            }

            routingSpec.enableEquations = TextUtil.str2boolean(resultSet.getString(4));
            routingSpec.usePerformanceMeasurements = TextUtil.str2boolean(resultSet.getString(5));
            routingSpec.outputFormat = resultSet.getString(6);
            routingSpec.outputTimeZoneAbbr = resultSet.getString(7);
            routingSpec.presentationGroupName = resultSet.getString(8);
            routingSpec.sinceTime = resultSet.getString(9);
            routingSpec.untilTime = resultSet.getString(10);
            routingSpec.consumerType = resultSet.getString(11);
            routingSpec.consumerArg = resultSet.getString(12);
            if (routingSpec.consumerArg == null)
            {
                routingSpec.consumerArg = "";
            }
            routingSpec.lastModifyTime = getTimeStamp(resultSet, 13, routingSpec.lastModifyTime);
            routingSpec.isProduction = TextUtil.str2boolean(resultSet.getString(14));

            routingSpec.getProperties().clear();
            try
            {
                 propsDao.readProperties("RoutingSpecProperty", "RoutingSpecId",
                                          routingSpec.getId(), routingSpec.getProperties());
            }
            catch (DbIoException ex)
            {
                throw new DatabaseException("Unable to read routing spec.", ex);
            }

            // Get the NetworkLists associated with this database.
            read_RS_NL(routingSpec);

            // Add this routing spec to the routing spec list.
            routingSpec.getDatabase().routingSpecList.add(routingSpec);
            stmt.close();
        }
        catch(SQLException ex)
        {
            throw new DatabaseException("Error on query '" + q + "'", ex);
        }
    }

    /**
    * This reads all the records of the RoutingSpecNetworkList table
    * associated with a particular RoutingSpec.  It then adds the
    * NetworkList names to the list of such names in the RoutingSpec.
    */
    private void read_RS_NL(RoutingSpec routingSpec) throws DatabaseException, SQLException
    {
        String q =
            "SELECT networkListName FROM RoutingSpecNetworkList " +
            "WHERE RoutingSpecId = " + routingSpec.getId() + " ";
        try (Statement stmt = createStatement();
             ResultSet resultSet = stmt.executeQuery(q);)
        {
            if (resultSet != null)
            {
                while (resultSet.next())
                {
                    String nm = resultSet.getString(1);
                    routingSpec.addNetworkListName(nm);
                }
            }
        }
    }

    /**
    * This writes a RoutingSpec out to the database.  It could either
    * be a new object or a pre-existing object that has changed.
    */
    public void write(RoutingSpec rs) throws DatabaseException, SQLException
    {
        if (rs.idIsSet())
        {
            update(rs);
        }
        else
        {
            DbKey id = name2id(rs.getName());
            if (!id.isNull())
            {
                rs.setId(id);
                update(rs);
            }
            else
            {
                insert(rs);
            }
        }
    }

    /**
    * Update a pre-existing RoutingSpec in the SQL database.
    */
    private void update(RoutingSpec rs)
        throws DatabaseException, SQLException
    {
        DbKey id = rs.getId();
        if (rs.consumerArg != null && rs.consumerArg.trim().length() == 0)
        {
            rs.consumerArg = null;
        }

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

        

        try (PropertiesDAI propsDao = _dbio.makePropertiesDAO())
        {
            propsDao.inTransaction(dao ->
            {
                dao.doModify(q, new Object[0]);
                propsDao.writeProperties("RoutingSpecProperty", "RoutingSpecId",
                                         rs.getId(), rs.getProperties());
                // Update the RoutingSpecNetworkLists
                delete_RS_NL(dao, rs);
                insert_RS_NL(rs);
            });
        }
        catch (Exception ex)
        {
            throw new DatabaseException("Unable to update routing spec.", ex);
        }

        
    }

    /**
    * Insert a new RoutingSpec into the SQL database.  This also inserts
    * records into the RoutingSpecProperty and RoutingSpecNetworkList
    * tables, as required.
    */
    private void insert(RoutingSpec rs)
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

        try
        {
            propsDao.writeProperties("RoutingSpecProperty", "RoutingSpecId",
                                     rs.getId(), rs.getProperties());
        }
        catch (DbIoException ex)
        {
            throw new DatabaseException("Unable to insert routing spec", ex);
        }
    }

    /**
    * Insert the RoutingSpecNetworkList records corresponding to a new
    * RoutingSpec.
    */
    private void insert_RS_NL(RoutingSpec rs)
        throws DatabaseException, SQLException
    {
        DbKey rsId = rs.getId();

        for(Iterator<String> it = rs.networkListNames.iterator(); it.hasNext(); )
        {
            String nm = it.next();
            if (nm == null)
            {
                continue; // shouldn't happen.
            }
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
    *       to this RoutingSpec, and
    *   2.  deletes all the records from RoutingSpecProperty that "belong"
    *       to this RoutingSpec.
    */
    public void delete(RoutingSpec rs)
        throws DatabaseException, SQLException
    {
        DbKey id = rs.getId();

        try (PropertiesDAI propsDAO = _dbio.makePropertiesDAO();
             ScheduleEntryDAI seDAO = _dbio.makeScheduleEntryDAO();)
        {
            propsDAO.inTransaction(dao -> 
            {
                try(PropertiesDAI props2 = _dbio.makePropertiesDAO())
                {
                    props2.inTransactionOf(dao);
                    // Do the related tables first
                    delete_RS_NL(dao, rs);
                    props2.deleteProperties("RoutingSpecProperty", "RoutingSpecId", id);
                    if (seDAO != null)
                    {
                        seDAO.inTransactionOf(dao);
                        ArrayList<ScheduleEntry> seList = seDAO.listScheduleEntries(null);
                        for(ScheduleEntry se : seList)
                        {
                            if (se.getRoutingSpecId().equals(rs.getId()))
                            {
                                seDAO.deleteScheduleEntry(se);
                            }
                        }
                    }

                    // Finally, do the main RoutingSpec table
                    String q = "DELETE FROM RoutingSpec WHERE ID = ?";
                    dao.doModify(q, id);
                }
            });
        }
        catch (Exception ex)
        {
            throw new DatabaseException("Unable to delete routing spec entries", ex);
        }
    }

    /**
    * Deletes the RoutingSpecNetworkList records corresponding to a
    * RoutingSpec.
    */
    private void delete_RS_NL(DaoBase dao, RoutingSpec rs)
        throws DatabaseException, SQLException
    {
        DbKey id = rs.getId();

        String q = "DELETE FROM RoutingSpecNetworkList " +
                   "WHERE RoutingSpecId = ?";
        dao.doModify(q, id);
    }

    private DbKey name2id(String name)
        throws SQLException
    {
        try (Statement stmt = createStatement();
             ResultSet rs = stmt.executeQuery(
                "SELECT id FROM RoutingSpec where lower(name) = "
                + sqlReqString(name.toLowerCase()));)
        {
            DbKey ret = Constants.undefinedId;
            if (rs != null && rs.next())
            {
                ret = DbKey.createDbKey(rs, 1);
            }

            return ret;
        }
    }

    /**
      Returns the last-modify-time for this routing spec in the database.
    */
    public Date getLMT(RoutingSpec spec)
    {
        try (Statement stmt = createStatement();)
        {
            DbKey id = spec.getId();
            if (id.isNull())
            {
                id = name2id(spec.getName());    // will throw if unsuccessful
                try
                {
                    spec.setId(id);
                }
                catch(DatabaseException ex) {} // guaranteed not to happen.
            }

            String q =
                "SELECT lastModifyTime FROM RoutingSpec WHERE id = " + id;
            try(ResultSet rs = stmt.executeQuery(q);)
            {
                // Should be only 1 record returned.
                if (rs == null || !rs.next())
                {
                    log.warn("Cannot get SQL LMT for Routing Spec '{}', id={}",spec.getName(), spec.getId());
                    return null;
                }

                Date ret = getTimeStamp(rs, 1, (Date)null);
                return ret;
            }
        }
        catch(SQLException ex)
        {
            log.atWarn()
               .setCause(ex)
               .log("SQL Error reading LMT for RoutingSpec '{}' id={}", spec.getName(), ex);
            return null;
        }
    }
}
