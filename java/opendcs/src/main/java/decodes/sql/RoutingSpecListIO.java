/*
 * Copyright 2025 OpenDCS Consortium and its Contributors
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package decodes.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import decodes.db.RoutingExecStatus;
import decodes.db.RoutingStatus;
import decodes.db.ValueNotFoundException;
import decodes.tsdb.CompAppInfo;
import opendcs.dai.LoadingAppDAI;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

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
import opendcs.dao.DaoHelper;

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

        

        String q = "SELECT id, name, dataSourceId, enableEquations, " +
                   "usePerformanceMeasurements, outputFormat, outputTimeZone, " +
                   "presentationGroupName, sinceTime, untilTime, " +
                   "consumerType, consumerArg, lastModifyTime, isProduction " +
                   "FROM RoutingSpec";

        try (Statement stmt = createStatement(); 
             ResultSet resultSet = stmt.executeQuery( q );
        )
        {
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
        }
    }

    public List<RoutingStatus> readRoutingSpecStatus(LoadingAppDAI dai) throws DatabaseException
    {
        ArrayList<RoutingStatus> ret = new ArrayList<>();

        String q = "select rs.ID, rs.NAME from ROUTINGSPEC rs";
        try (Statement stmt = createStatement();
             ResultSet rs = stmt.executeQuery( q ))
        {
            while(rs.next())
            {
                RoutingStatus rstat = new RoutingStatus(DbKey.createDbKey(rs.getLong(1)));
                rstat.setRoutingSpecId(DbKey.createDbKey(rs.getLong(1)));
                rstat.setName(rs.getString(2));
                ret.add(rstat);
            }
            q = "select se.SCHEDULE_ENTRY_ID, se.LOADING_APPLICATION_ID, se.ROUTINGSPEC_ID, "
                    + "se.RUN_INTERVAL, se.ENABLED, se.NAME "
                    + "from SCHEDULE_ENTRY se";

           try (ResultSet resultSet = stmt.executeQuery( q ))
           {
               while(resultSet.next())
               {
                   long rsId = resultSet.getLong(3);
                   String seName = resultSet.getString(6);
                   RoutingStatus ars = null;
                   if(seName.endsWith("-manual"))
                   {
                       ars = new RoutingStatus(DbKey.createDbKey(rsId));
                       ars.setManual(true);
                       ret.add(ars);
                   }
                   else
                       for(RoutingStatus tars : ret)
                       {
                           if(rsId == tars.getRoutingSpecId().getValue())
                           {
                               ars = tars;
                               break;
                           }
                       }
                   if(ars != null)
                   {
                       ars.setName(seName);
                       ars.setScheduleEntryId(DbKey.createDbKey(resultSet.getLong(1)));
                       ars.setAppId(DbKey.createDbKey(resultSet.getLong(2)));
                       ars.setRunInterval(resultSet.getString(4));
                       ars.setEnabled(TextUtil.str2boolean(resultSet.getString(5)));
                   }
               }
           }
           q = "select se.SCHEDULE_ENTRY_ID, ses.LAST_MODIFIED, ses.NUM_MESSAGES, ses.NUM_DECODE_ERRORS, ses.LAST_MESSAGE_TIME"
                   + " from SCHEDULE_ENTRY se, SCHEDULE_ENTRY_STATUS ses"
                   + " where se.SCHEDULE_ENTRY_ID = ses.SCHEDULE_ENTRY_ID"
                   + " and ses.SCHEDULE_ENTRY_STATUS_ID = "
                   + "  (select max(SCHEDULE_ENTRY_STATUS_ID) from SCHEDULE_ENTRY_STATUS "
                   + "   where SCHEDULE_ENTRY_ID = se.SCHEDULE_ENTRY_ID)";
           try (ResultSet resultSet = stmt.executeQuery(q))
           {
               while(resultSet.next())
               {
                   Long seid = resultSet.getLong(1);
                   for(RoutingStatus ars : ret)
                   {
                       if(seid == ars.getScheduleEntryId().getValue())
                       {
                           if (super._dbio.isCwms())
                           {
                               Date x = resultSet.getDate(2);
                               if (!resultSet.wasNull())
                               {
                                   ars.setLastActivityTime(x);
                               }
                           }
                           else
                           {
                             Long x = resultSet.getLong(2);
                             if(!resultSet.wasNull())
                             {
                                  ars.setLastActivityTime(new Date(x));
                             }
                           }
                           ars.setNumMessages(resultSet.getInt(3));
                           ars.setNumDecodesErrors(resultSet.getInt(4));
                           if (super._dbio.isCwms())
                           {
                               Date x = resultSet.getDate(5);
                               if (!resultSet.wasNull())
                               {
                                   ars.setLastMessageTime(x);
                               }
                           }
                           else
                           {
                               Long x = resultSet.getLong(5);
                               if(!resultSet.wasNull())
                               {
                                   ars.setLastMessageTime(new Date(x));
                               }
                           }
                           break;
                       }
                   }
               }
           }


            try
            {
                List<CompAppInfo> appRefs = dai.listComputationApps(false);
                for(RoutingStatus ars : ret)
                    if(ars.getAppId() != null)
                        for(CompAppInfo appRef : appRefs)
                            if(ars.getAppId().equals(appRef.getAppId()))
                            {
                                ars.setAppName(appRef.getAppName());
                                break;
                            }
            }
            catch (DbIoException ex)
            {
                throw new DatabaseException("Cannot list computation apps", ex);
            }

            Comparator<RoutingStatus> routingStatusComparator = Comparator.comparing(RoutingStatus::getRoutingSpecId)
                    .thenComparing(RoutingStatus::getName);

            ret.sort(routingStatusComparator);

            return ret;
        }
        catch(SQLException ex)
        {
            String msg = "RoutingSpecListIO: Error in query '" + q + "': " + ex;
            throw new DatabaseException(msg, ex);
        }
    }

    public List<RoutingExecStatus> readRoutingExecStatus(DbKey scheduleEntryId) throws DatabaseException
    {
        List<RoutingExecStatus> ret = new ArrayList<>();

        String q = "select rs.ID, se.NAME, se.SCHEDULE_ENTRY_ID,"
                + " ses.SCHEDULE_ENTRY_STATUS_ID, ses.RUN_START_TIME,"
                + " ses.RUN_COMPLETE_TIME, ses.LAST_MESSAGE_TIME, ses.HOSTNAME,"
                + " ses.RUN_STATUS, ses.NUM_MESSAGES, ses.NUM_DECODE_ERRORS,"
                + " ses.NUM_PLATFORMS, ses.LAST_SOURCE, ses.LAST_CONSUMER,"
                + " ses.LAST_MODIFIED"
                + " from ROUTINGSPEC rs, SCHEDULE_ENTRY_STATUS ses, SCHEDULE_ENTRY se"
                + " where ses.SCHEDULE_ENTRY_ID = se.SCHEDULE_ENTRY_ID"
                + " and se.ROUTINGSPEC_ID = rs.ID"
                + " and se.SCHEDULE_ENTRY_ID = ?"
                + " order by ses.RUN_START_TIME desc";

        try (Connection conn = connection();
             PreparedStatement stmt = conn.prepareStatement(q))
        {
            stmt.setLong(1, scheduleEntryId.getValue());
            try (ResultSet rs = stmt.executeQuery())
            {
                while(rs.next())
                {
                    RoutingExecStatus res = new RoutingExecStatus();
                    res.setRoutingSpecId(rs.getLong(1));
                    res.setScheduleEntryId(rs.getLong(3));
                    res.setRoutingExecId(rs.getLong(4));
                    res.setRunStart(rs.getDate(5));
                    Date x = rs.getDate(6);
                    if(!rs.wasNull())
                        res.setRunStop(x);
                    x = rs.getDate(7);
                    if(!rs.wasNull())
                        res.setLastMsgTime(x);
                    res.setHostname(rs.getString(8));
                    res.setRunStatus(rs.getString(9));
                    res.setNumMessages(rs.getInt(10));
                    res.setNumErrors(rs.getInt(11));
                    res.setNumPlatforms(rs.getInt(12));
                    res.setLastInput(rs.getString(13));
                    res.setLastOutput(rs.getString(14));
                    res.setLastActivity(rs.getDate(15));
                    ret.add(res);
                }
            }
        }
        catch(SQLException ex)
        {
            String msg = "Error in query '" + q + "': " + ex;
            throw new DatabaseException(msg, ex);
        }

        return ret;
    }

    /**
      Reads an existing routing spec into memory.
      The passed object is filled-in.
    */
    public void readRoutingSpec(RoutingSpec routingSpec) throws DatabaseException
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
             ResultSet resultSet = stmt.executeQuery(q))
        {
            String msg = String.format("No RoutingSpec found with id %d", routingSpec.getId().getValue());
            if (resultSet == null)
            {
                Throwable thr = new ValueNotFoundException(msg);
                throw new DatabaseException(msg, thr);
            }

            // There will be only one row in the result set.
            resultSet.next();
            if (resultSet.getRow() == 0)
            {
                Throwable thr = new ValueNotFoundException(msg);
                throw new DatabaseException(msg, thr);
            }
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
                try(PropertiesDAI propsDao2 = _dbio.makePropertiesDAO())
                {
                    propsDao2.inTransactionOf(dao);
                    dao.doModify(q, new Object[0]);
                    propsDao2.writeProperties("RoutingSpecProperty", "RoutingSpecId",
                                            rs.getId(), rs.getProperties());
                    // Update the RoutingSpecNetworkLists
                    delete_RS_NL(dao, rs);
                    insert_RS_NL(dao, rs);
                }
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



        try (PropertiesDAI propsDao = _dbio.makePropertiesDAO())
        {
            propsDao.inTransaction(dao ->
            {
                try(PropertiesDAI propsDao2 = _dbio.makePropertiesDAO())
                {
                    propsDao2.inTransactionOf(dao);
                    dao.doModify(q, new Object[0]);
                    log.info("Routing Spec Saved!!!!!!!!!!!!!");
                    // Now insert the RoutingSpecNetworkList records
                    insert_RS_NL(dao, rs);
                    propsDao2.writeProperties("RoutingSpecProperty", "RoutingSpecId",
                                            rs.getId(), rs.getProperties());
                }
            });
        }
        catch (Exception ex)
        {
            throw new DatabaseException("Unable to insert routing spec", ex);
        }
    }

    /**
    * Insert the RoutingSpecNetworkList records corresponding to a new
    * RoutingSpec.
    */
    private void insert_RS_NL(DaoBase dao, RoutingSpec rs) throws DatabaseException, SQLException
    {
        final DbKey rsId = rs.getId();

        String q = "INSERT INTO RoutingSpecNetworkList VALUES (?,?)";
        dao.doModifyBatch(q, v ->
        {
            return new Object[]{rsId, v};
        }, rs.networkListNames, 200);
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
        final String q = "SELECT id FROM RoutingSpec where lower(name) = lower(?)";
        try (Connection conn = connection();
             DaoHelper dao = new DaoHelper(_dbio, "routingspeclist", conn);
            )
        {
            return name2id(dao, name);
        }
    }

    private DbKey name2id(DaoBase dao, String name)
        throws SQLException
    {
        final String q = "SELECT id FROM RoutingSpec where lower(name) = lower(?)";
        return dao.getSingleResultOr(q, rs ->DbKey.createDbKey(rs, "id"), Constants.undefinedId, name);
    }
    /**
      Returns the last-modify-time for this routing spec in the database.
    */
    public Date getLMT(RoutingSpec spec)
    {
        try (Connection conn = connection();
             DaoHelper dao = new DaoHelper(this._dbio, "routinspeclist", conn))
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

            String q ="SELECT lastModifyTime FROM RoutingSpec WHERE id = ?";
            return dao.getSingleResultOr(q, rs -> getTimeStamp(rs, 1, null), null, id);
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
