package decodes.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import decodes.db.Database;
import opendcs.dai.DacqEventDAI;
import opendcs.dai.PlatformStatusDAI;
import opendcs.dai.PropertiesDAI;
import opendcs.dai.SiteDAI;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import decodes.db.Constants;
import decodes.db.DatabaseException;
import decodes.db.Platform;
import decodes.db.PlatformConfig;
import decodes.db.PlatformList;
import decodes.db.PlatformSensor;
import decodes.db.SiteName;
import decodes.db.TransportMedium;
import decodes.db.Site;
import decodes.tsdb.DbIoException;
import decodes.util.DecodesSettings;

import opendcs.dao.DaoHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PlatformListIO handles the I/O of the PlatformList object and some of its
 * kids to/from the SQL database.
 * When reading, this reads the Platform and TransportMedium tables.
 * Note, though, that only the MediumType and MediumId fields of the
 * TransportMedium table are used.
 */
public class PlatformListIO extends SqlDbObjIo
{
    private final static Logger log = LoggerFactory.getLogger(PlatformListIO.class);

    /** Transient reference to the PlatformList that we're working on. */
    protected PlatformList _pList;

    /**
    * This is a reference to the ConfigListIO object for this SQL database.
    * This is passed in from the SqlDatabaseIO object when this PlatformListIO
    * object is created.  It's used to retrieve the
    * PlatformConfig objects corresponding to particular ID numbers.
    */
    protected ConfigListIO _configListIO;

    /**
    * This is a reference to the EquipmentModelListIO object for this
    * database.  This is used to retrieve EquipmentModel objects by their
    * ID numbers.
    */
    protected EquipmentModelListIO _equipmentModelListIO;

    private String coltpl;
    private String tmColumns = null;

    /** Set by the dbimport program to facilitate autoDeleteOnImport at MVR. */
    public static boolean isDbImport = false;

    protected String getTmColumns()
    {
        if (tmColumns == null)
        {
            tmColumns =
                "PLATFORMID, MEDIUMTYPE, MEDIUMID, "
                    + "SCRIPTNAME, CHANNELNUM, ASSIGNEDTIME, TRANSMITWINDOW, "
                    + "TRANSMITINTERVAL, EQUIPMENTID";
            if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_6)
                tmColumns = tmColumns + ", TIMEADJUSTMENT, PREAMBLE";
            if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_7)
                tmColumns = tmColumns + ", TIMEZONE";
            if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_11)
                tmColumns = tmColumns + ", LOGGERTYPE, BAUD, STOPBITS, PARITY, "
                    + "DATABITS, DOLOGIN, USERNAME, PASSWORD";
            log.debug("PlatformListIO: databaseversion={}, tmColumns='{}'",
                    getDatabaseVersion(), tmColumns);
        }
        return tmColumns;
    }

    /**
    * Constructor.
    * @param dbio the SqlDatabaseIO to which this IO object belongs
    * @param configListIO used to access platform configs
    * @param emlIO used to access EquipmentModel records
    */
    public PlatformListIO(SqlDatabaseIO dbio,
                          ConfigListIO configListIO,
                          EquipmentModelListIO emlIO)
    {
        super(dbio);

        _configListIO = configListIO;
        _equipmentModelListIO = emlIO;
    }

    private DaoHelper getDaoHelper()
    {
        DaoHelper h = new DaoHelper(_dbio,"sql.platformListIO",connection());
        return h;
    }

    @Override
    public void setConnection(Connection conn)
    {
        super.setConnection(conn);

        // For efficiency, have subordinate dbio's use my connection.
        _configListIO.setConnection(conn);
        _equipmentModelListIO.setConnection(conn);
    }

    private String getColTpl()
    {
        if (coltpl == null)
        {
            // column template
            coltpl = "id, agency, isProduction, siteId, configId"
                + ", description, lastModifyTime, expiration";
            if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_7)
                coltpl = coltpl + ", platformDesignator";
        }
        return coltpl;
    }

    /**
    * Read the PlatformList.
    * This reads partial data from the Platform and TransportMedium tables.
    * This corresponds to reading the platform/PlatformList.xml file of
    * the XML database.
    * The partial data of the Platform table are the following fields:
    * <ul>
    *   <li>platformId</li>
    *   <li>description</li>
    *   <li>agency</li>
    *   <li>expiration</li>
    *   <li>configName</li>
    *   <li>site</li>
    *   <li>transportMedia - partial data</li>
    *   <li>isReadComplete - should be false</li>
    * </ul>
    * The partial data of the TransportMedium table are the MediumType and
    * MediumId fields.
    * @param platformList the PlatformList object to populate
    */
    public void read(PlatformList platformList)
        throws SQLException, DatabaseException
    {
        log.debug("Reading PlatformList...");

        _pList = platformList;

        try (Statement stmt = createStatement();)
        {
            String q =
                (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_7) ?
                    ("SELECT ID, Agency, IsProduction, " +
                    "SiteId, ConfigId, Description, " +
                    "LastModifyTime, Expiration, platformDesignator " +
                    "FROM Platform")
                :
                    ("SELECT ID, Agency, IsProduction, " +
                    "SiteId, ConfigId, Description, " +
                    "LastModifyTime, Expiration " +
                    "FROM Platform");

            log.debug("Executing query '{}'", q );
            try (ResultSet rs = stmt.executeQuery(q))
            {
                if (rs != null)
                {
                    while (rs.next())
                    {
                        DbKey platformId = DbKey.createDbKey(rs, 1);

                        // MJM 20041027 Check to see if this ID is already in the
                        // cached platform list and ignore if so. That way, I can
                        // periodically refresh the platform list to get any newly
                        // created platforms after the start of the routing spec.
                        // Refreshing will not affect previously read/used platforms.
                        Platform p = _pList.getById(platformId);
                        if (p != null)
                            continue;

                        p = new Platform(platformId);
                        _pList.add(p);

                        p.agency = rs.getString(2);

                        DbKey siteId = DbKey.createDbKey(rs, 4);
                        if (!rs.wasNull()) {
                            p.setSite(p.getDatabase().siteList.getSiteById(siteId));
                        }

                        DbKey configId = DbKey.createDbKey(rs, 5);
                        if (!rs.wasNull())
                        {
                            PlatformConfig pc =
                                platformList.getDatabase().platformConfigList.getById(
                                    configId);
                            if (pc == null)
                                pc = _configListIO.getConfig(configId);
                            p.setConfigName(pc.configName);
                            p.setConfig(pc);
                        }

                        String desc = rs.getString(6);
                        if (!rs.wasNull())
                            p.setDescription(desc);

                        p.lastModifyTime = getTimeStamp(rs, 7, null);

                        p.expiration = getTimeStamp(rs, 8, p.expiration);

                        if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_7)
                            p.setPlatformDesignator(rs.getString(9));
                    }
                }
            }
        }
        readAllTransportMedia(platformList);
    }

    protected void readAllTransportMedia(PlatformList platformList)
        throws SQLException, DatabaseException
    {
        String q = "select " + getTmColumns() + " from TransportMedium ";
        log.debug("Executing query '{}'", q);
        getDaoHelper().doQuery(q, rs ->
        {
            DbKey platId = DbKey.createDbKey(rs, 1);
            Platform p = platformList.getById(platId);
            if (p == null)
            {
                log.debug("TM for non-existent platform id={} TM.type={}, TM.mediumId={}",
                        platId, rs.getString(2), rs.getString(3));
            }
            else
            {
                try
                {
                    rs2tm(rs, p);
                } catch (Exception e)
                {
                    log.error("Error reading TransportMedium", e);
                }
            }
        });
    }

    /**
    * Read the information from the TransportMedium table about the
    * TransportMedium objects associated with a particular platform.
    * This then instantiates the TransportMedium object and adds it to
    * the appropriate containers.
    * This only reads partial data from the table:
    * MediumType and MediumId.  This corresponds to the data in the
    * platform/PlatformList.xml file in the XML database.
    * @param p the Platform
    */
    private void readTransportMedia(Platform p)
        throws SQLException
    {
        p.transportMedia.clear();

        String q = "select " + getTmColumns() + " from TransportMedium "
            + "WHERE PlatformId = ? ";

        getDaoHelper().doQuery(q, rs ->
        {
            try
            {
                rs2tm(rs, p);
            }catch(DatabaseException e)
            {
                throw new SQLException(e);
            }
        },p.getId());

    }

    /**
     * Builds a TransportMedium from a Platform and ResultSet
     * The TransportMedium is added to the Platform
     *
     * Passed a result set where next==true. Assume columns of tmColumns.
     * If passed platform != null, the tm is added to the platforms list of media.
     * @param rs the result set
     * @param platform the Platform that owns this TM
     * @return the TransportMedium
     */
    private TransportMedium rs2tm(ResultSet rs, Platform platform)
        throws SQLException, DatabaseException
    {
        String mediumType = rs.getString(2);
        String mediumId = rs.getString(3);

        TransportMedium tm = new TransportMedium(platform, mediumType, mediumId);

        tm.scriptName = rs.getString(4);
        int channelNum = rs.getInt(5);
        if (rs.wasNull())
            channelNum = Constants.undefinedIntKey;
        tm.channelNum = channelNum;

        int assignedTime = rs.getInt(6);
        if (!rs.wasNull()) tm.assignedTime = assignedTime;
        int transmitWindow = rs.getInt(7);
        if (!rs.wasNull()) tm.transmitWindow = transmitWindow;
        int transmitInterval = rs.getInt(8);
        if (!rs.wasNull()) tm.transmitInterval = transmitInterval;

        DbKey equipmentId = DbKey.createDbKey(rs, 9);
        if (!equipmentId.isNull())
            tm.equipmentModel =
                _equipmentModelListIO.getEquipmentModel(equipmentId,
                    tm.getDatabase());

        if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_6)
        {
            int ta = rs.getInt(10);
            if (rs.wasNull())
                ta = 0;
            tm.setTimeAdjustment(ta);
            String str = rs.getString(11);
            if (!rs.wasNull() && str != null && str.length() > 0)
                tm.setPreamble(Character.toUpperCase(str.charAt(0)));
            if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_7)
            {
                str = rs.getString(12);
                if (rs.wasNull() || str.trim().length() == 0)
                    str = null;
                tm.setTimeZone(str);
                if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_11)
                {
                    tm.setLoggerType(rs.getString(13));
                    int x = rs.getInt(14);
                    if (!rs.wasNull()) tm.setBaud(x);
                    x = rs.getInt(15);
                    if (!rs.wasNull()) tm.setStopBits(x);
                    String s = rs.getString(16);
                    if (!rs.wasNull() && s.length() > 0)
                        tm.setParity(s.charAt(0));
                    x = rs.getInt(17);
                    if (!rs.wasNull()) tm.setDataBits(x);
                    tm.setDoLogin(TextUtil.str2boolean(rs.getString(18)));
                    tm.setUsername(rs.getString(19));
                    tm.setPassword(rs.getString(20));
                }
            }
        }

        if (platform != null)
            platform.transportMedia.add(tm);
        return tm;
    }

    /**
    * This reads the "complete" data for the given Platform.
    * @param p the Platform
    */
    public void readPlatform(Platform p)
        throws SQLException, DatabaseException
    {

            String q = "SELECT " + getColTpl() + " FROM Platform WHERE ID = ?";
            log.debug("readPlatform() Executing '{}'", q );
            getDaoHelper().doQuery(q, rs ->
            {
                // Can only be 1 platform with a given ID.
                {
                    p.setAgency(rs.getString(2));

                    p.isProduction = TextUtil.str2boolean(rs.getString(3));

                    DbKey siteId = DbKey.createDbKey(rs, 4);
                    if (!rs.wasNull())
                    {
                        // If site was previously loaded, use it.
                        p.setSite(p.getDatabase().siteList.getSiteById(siteId));

                        try(SiteDAI siteDAO = _dbio.makeSiteDAO())
                        {
                            siteDAO.setManualConnection(connection);
                            if (p.getSite() == null)
                            {
                                if (!DbKey.isNull(siteId))
                                {
                                    try
                                    {
                                        p.setSite(siteDAO.getSiteById(siteId));
                                        p.getDatabase().siteList.addSite(p.getSite());
                                    }
                                    catch(Exception ex)
                                    {
                                        log.warn("Platform {} id={} has invalid siteID {}",
                                                p.getDisplayName(), p.getKey(), siteId);
                                        p.setSite(null);
                                    }
                                }
                            }
                            else
                            {
                                try
                                {
                                    siteDAO.readSite(p.getSite());
                                }
                                catch (Exception e)
                                {
                                    log.error("An error occurred reading site {}",
                                            p.getSite(),e);
                                }
                            }
                        }
                    }

                    DbKey configId = DbKey.createDbKey(rs, 5);
                    if (!rs.wasNull())
                    {
                        // Get config out of database's list.
                        PlatformConfig pc =
                            p.getDatabase().platformConfigList.getById(configId);
                        boolean commitAfterSelect = _dbio.commitAfterSelect;
                        try
                        {
                            // Caller will commit after THIS method returns, so don't
                            // have it commit after we read the site.
                            _dbio.commitAfterSelect = false;

                            if (pc == null)
                            {
                                // Not in database's list yet? Add it.
                                pc = _configListIO.readConfig(configId);
                                if (pc != null)
                                    p.getDatabase().platformConfigList.add(pc);
                            }
                            // Already in list. Check to see if it's current.
                            else
                                _configListIO.readConfig(pc.getId());
                        }catch(DatabaseException e)
                        {
                            throw new SQLException(e);
                        }
                        finally
                        {
                            _dbio.commitAfterSelect = commitAfterSelect;
                        }

                        if (pc != null)
                        {
                            p.setConfigName(pc.configName);
                            p.setConfig(pc);
                        }
                    }

                    p.setDescription(rs.getString(6));
                    p.lastModifyTime = getTimeStamp(rs, 7, p.lastModifyTime);
                    p.expiration = getTimeStamp(rs, 8, p.expiration);
                    if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_7)
                        p.setPlatformDesignator(rs.getString(9));

                    readTransportMedia(p);

                    if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_6)
                    {
                        PropertiesDAI propsDao = _dbio.makePropertiesDAO();
                        propsDao.setManualConnection(connection);
                        try {
                            propsDao.readProperties("PlatformProperty", "platformID", p.getId(),
                            p.getProperties());
                        }
                        catch (DbIoException e)
                        {
                            throw new SQLException(e);
                        }
                        finally
                        {
                            propsDao.close();
                        }
                    }
                }
            },p.getId());

        readPlatformSensors(p);
    }

    /**
    * This reads the PlatformSensor and PlatformSensorProperty records
    * associated with this Platform.
    * The Platform argument must have had its ID set.
    * @param p the Platform
    */
    private void readPlatformSensors(Platform p)
        throws DatabaseException, SQLException
    {

        StringBuilder q = new StringBuilder(120);
        q.append("SELECT platformId, sensorNumber, siteId");
        if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_7)
        {
            q.append(", dd_nu");
        }
        q.append(" FROM PlatformSensor WHERE PlatformId = ? ");
        q.append(" ORDER BY sensorNumber");

        getDaoHelper().doQuery(q.toString(), rs ->
        {
            if (rs == null)
            {
                return;
            }
            int sn = rs.getInt(2);
            PlatformSensor ps = new PlatformSensor(p, sn);
            p.addPlatformSensor(ps);

            DbKey siteId = DbKey.createDbKey(rs, 3);
            if (!rs.wasNull())
            {
                // If site was previously loaded, use it.
                ps.site = p.getDatabase().siteList.getSiteById(siteId);

                boolean commitAfterSelect = _dbio.commitAfterSelect;
                try
                {
                    // Caller will commit after THIS method returns, so don't
                    // have it commit after we read the site.
                    _dbio.commitAfterSelect = false;

                    // Else attempt to read site from database.
                    if (ps.site == null)
                    {
                        if (siteId != Constants.undefinedId)
                        {
                            Site site = new Site();
                            try
                            {
                                site.setId(siteId);
                                site.read();
                                p.getDatabase().siteList.addSite(site);
                                ps.site = site;
                            } catch (DatabaseException ex)
                            {
                                log.warn("Platform Sensor with invalid site ID={}, site record left blank.", siteId);
                            }
                        }
                    } else
                    {
                        try
                        {
                            ps.site.read();
                        } catch (DatabaseException e)
                        {
                            throw new SQLException(e);
                        }
                    }
                } finally
                {
                    _dbio.commitAfterSelect = commitAfterSelect;
                }
            }
            if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_7)
            {
                int dd_nu = rs.getInt(4);
                ps.setUsgsDdno(!rs.wasNull() ? dd_nu : 0);
            }

            try
            {
                readPSProps(ps, p.getId());
            } catch (DatabaseException e)
            {
                throw new SQLException(e);
            }

        }, p.getId());


    }

    /**
    * Read the PlatformSensorProperty's associated with a PlatformSensor.
    * @param ps the PlatformSensor in which to place the properties
    * @param pid the platform ID
    */
    private void readPSProps(PlatformSensor ps, DbKey pid)
        throws DatabaseException, SQLException
    {
        PropertiesDAI propsDao = _dbio.makePropertiesDAO();
        propsDao.setManualConnection(connection);
        try { propsDao.readProperties("PlatformSensorProperty", "platformID", "SensorNumber",
            ps.platform.getId(), ps.sensorNumber, ps.getProperties()); }
        catch (DbIoException e)
        {
            throw new DatabaseException(e.getMessage());
        }
        finally
        {
            propsDao.close();
        }

        String ddno = PropertiesUtil.getIgnoreCase(ps.getProperties(), "DDNO");
        if (ddno != null)
            try
            {
                ps.setUsgsDdno(Integer.parseInt(ddno));
                PropertiesUtil.rmIgnoreCase(ps.getProperties(), "DDNO");
            }
            catch(NumberFormatException ex) { }
    }


    /**
    * Writes a complete Platform back out to the database.  If the Platform's
    * ID is -1, then we know that this is a new Platform.
    * @param p the Platform
    */
    public void writePlatform(Platform p)
        throws SQLException, DatabaseException
    {
        //System.out.println("    PlatformListIO.writePlatform(p)");

        if (p.idIsSet())
            update(p);
        else
        {
// MJM 20030116 - Don't want to do this. Multiple platforms are allowed to
// have the same transport ID & type. Example: Historical versions.
            // Use transport media to see if this platform already exists in
            // the database.
//            int id = getIdByTransportMedia(p);
//            if (id != Constants.undefinedId)
//            {
//                p.setId(id);
//                update(p);
//            }
//            else
                insert(p);
        }

        //System.out.println("    num PlatformSensors is " +
        //    p.platformSensors.size());
    }


    /**
    * Writes the changed  data for a pre-existing Platform back out to the
    * database, using an UPDATE query.
    * @param p the Platform
    */
    private void update(Platform p)
        throws DatabaseException, SQLException
    {
        String q =
            "UPDATE Platform SET " +
              "Agency = " + sqlString(p.agency) + ", " +
              "IsProduction = " + sqlString(p.isProduction) + ", " +
              "SiteId = " + sqlOptHasId(p.getSite()) + ", " +
              "ConfigId = " + sqlOptHasId(p.getConfig()) + ", " +
              "Description = " + sqlOptString(p.description) + ", " +
              "LastModifyTime = " + sqlOptDate(p.lastModifyTime) + ", " +
              "Expiration = " + sqlOptDate(p.expiration);
        if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_7)
            q = q + ", platformDesignator = " +
                sqlOptString(p.getPlatformDesignator());

        q = q + " WHERE ID = " + p.getId();

        try { executeUpdate(q); }
        catch(Exception ex)
        {
            if (ex.toString().toLowerCase().contains("unique"))
            {
                // This means a different platform exists with the (siteid,designator)
                // in the above update.
                if (isDbImport && DecodesSettings.instance().autoDeleteOnImport)
                {
                    q = "select id from platform where siteid = " +
                        sqlOptHasId(p.getSite()) + " and platformdesignator "
                        + (p.getPlatformDesignator() == null || p.getPlatformDesignator().length()==0
                          ? "is null" : ("= " + sqlString(p.getPlatformDesignator())));
                    try (Statement stmt = createStatement();)
                    {
                        try (ResultSet rs = stmt.executeQuery(q);)
                        {
                            DbKey pid = Constants.undefinedId;
                            if (rs != null && rs.next())
                            {
                                log.info("Imported platform clashes with old platform ID={}"
                                     +" -- deleting old platform.",pid);
                                pid = DbKey.createDbKey(rs, 1);
                                Platform oldPlat = new Platform(pid);
                                delete(oldPlat);
                                // Now recursively (but only 1 level) try again.
                                isDbImport = false;
                                update(p);
                                isDbImport = true;
                            }
                            else
                            {
                                log.warn("Cannot update platform because of unknown clash: ", ex);
                            }
                        }
                    }
                }
                else
                {
                    log.warn("Cannot update platform {}", p.getDisplayName(), ex);
                }
                return;
            }
        }

        // Now update the PlatformSensor and PlatformSensorProperty
        // records.
        // We'll take the easy road, which is to first delete them all
        // and then re-insert them.

        deletePlatformSensors(p);
        insertPlatformSensors(p);

        // Update the TransportMedia.  We'll take the easy route and
        // first delete them all, and then re-insert them.

        deleteTransportMedia(p);
        insertTransportMedia(p);

        if (getDatabaseVersion() >= 6)
        {
            PropertiesDAI propsDao = _dbio.makePropertiesDAO();
            propsDao.setManualConnection(connection);

            try { propsDao.writeProperties("PlatformProperty", "platformId", p.getId(),
                p.getProperties()); }
            catch (DbIoException e)
            {
                throw new DatabaseException(e.getMessage());
            }
            finally
            {
                propsDao.close();
            }
        }
    }

    /**
    * Writes a brand-new Platform into the SQL database, using an
    * INSERT query.
    * @param p the Platform
    */
    private void insert(Platform p)
        throws SQLException, DatabaseException
    {
        DbKey id = getKey("Platform");
        p.setId(id);
        p.getDatabase().platformList.add(p);  // adds (or re-adds) with new ID.

        String q =
            "INSERT INTO Platform (" + getColTpl() + ")"
            + " VALUES (" +
              id + ", " +
              sqlString(p.agency) + ", " +
              sqlString(p.isProduction) + ", " +
              sqlOptHasId(p.getSite()) + ", " +
              sqlOptHasId(p.getConfig()) + ", " +
              sqlOptString(p.description) + ", " +
              sqlOptDate(p.lastModifyTime) + ", " +
              sqlOptDate(p.expiration);

        if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_7)
            q = q + ", " + sqlOptString(p.getPlatformDesignator(), 24);

        q += ")";

        try { executeUpdate(q); }
        catch(Exception ex)
        {
            if (ex.toString().toLowerCase().contains("unique"))
            {
                log.warn("Cannot write platform {} to database: ", p.getDisplayName() , ex);
                return;
            }
        }

        // Write the PlatformSensor and PlatformSensorProperty entities
        insertPlatformSensors(p);

        // Now insert all the TransportMediums that are owned by this
        // Platform.  Since the Platform is new, we are guaranteed that
        // each TransportMedium is also new.
        insertTransportMedia(p);

        if (getDatabaseVersion() >= 6)
        {
            PropertiesDAI propsDao = _dbio.makePropertiesDAO();
            propsDao.setManualConnection(connection);

            try { propsDao.writeProperties("PlatformProperty", "platformId", p.getId(),
                p.getProperties()); }
            catch (DbIoException e)
            {
                throw new DatabaseException(e.getMessage());
            }
            finally
            {
                propsDao.close();
            }
        }
    }

    /**
    * This inserts all the TransportMedia associated with this Platform.
    * The Platform must have already had its SQL database ID set.
    * @param p the Platform
    */
    private void insertTransportMedia(Platform p)
        throws SQLException, DatabaseException
    {
        DbKey id = p.getId();
        Vector<TransportMedium> v = p.transportMedia;
        for (int i = 0; i < v.size(); ++i)
        {
            TransportMedium tm = v.get(i);
            insertTransportMedium(id, tm);
        }
    }

    /**
    * This writes the PlatformSensor and PlatformSensorProperty records for
    * a particular Platform.  Note that records are only written if the
    * PlatformSensor object is not empty.
    * @param p the Platform
    */
    private void insertPlatformSensors(Platform p)
        throws SQLException, DatabaseException
    {
        Iterator<PlatformSensor> i = p.getPlatformSensors();
        while (i.hasNext())
        {
            PlatformSensor ps = i.next();
            insert(ps);
        }
    }

    /**
    * This inserts a PlatformSensor and its associated
    * PlatformSensorProperty's into the database, if the PlatformSensor
    * is not empty.
    * @param ps the PlatformSensor
    */
    private void insert(PlatformSensor ps)
        throws SQLException, DatabaseException
    {
        if (ps.isEmpty()) return;

        DbKey pid = ps.platform.getId();
        int sn = ps.sensorNumber;

        int usgsDdno = ps.getUsgsDdno();
        if (usgsDdno <= 0)
        {
            String s = PropertiesUtil.rmIgnoreCase(ps.getProperties(), "DDNO");
            if (s != null)
            {
                try { usgsDdno = Integer.parseInt(s.trim()); }
                catch(NumberFormatException ex)
                {
                    usgsDdno = 0;
                    ps.getProperties().setProperty("DDNO", s);
                }
            }
        }

        String q = "INSERT INTO PlatformSensor VALUES (" +
                     pid + ", " +
                     sn + ", " +
                     sqlOptHasId(ps.site);
        if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_7)
        {
            if (usgsDdno <= 0)
                q = q + ", NULL";
            else
                q = q + ", " + usgsDdno;
        }
        else if (usgsDdno > 0)
            ps.getProperties().setProperty("DDNO", "" + usgsDdno);
        q += ")";

        executeUpdate(q);

        PropertiesDAI propsDao = _dbio.makePropertiesDAO();
        propsDao.setManualConnection(connection);
        try { propsDao.writeProperties("PlatformSensorProperty", "platformID", "SensorNumber",
            ps.platform.getId(), ps.sensorNumber, ps.getProperties()); }
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
    * Insert a new TransportMedium into the database.
    * @param platformId the database ID of the platform
    * @param tm the TransportMedium
    */
    private void insertTransportMedium(DbKey platformId, TransportMedium tm)
        throws SQLException, DatabaseException
    {
        String medType = tm.getMediumType();
        String medId = tm.getMediumId();
        if (medType == null || medType.trim().length() == 0
         || medId == null || medId.trim().length() == 0)
        {
            log.warn("Skipping null transport medium");
            return;
        }

        medId = medId.trim();

        String q = "insert into transportmedium(" + getTmColumns() + ") values (";
        q = q +
              platformId + ", " +
              sqlReqString(tm.getMediumType()) + ", " +
              sqlReqString(tm.getMediumId()) + ", " +
              sqlOptString(tm.scriptName) + ", " +
              sqlOptInt(tm.channelNum) + ", " +
              sqlOptInt(tm.assignedTime) + ", " +
              sqlOptInt(tm.transmitWindow) + ", " +
              sqlOptInt(tm.transmitInterval) + ", " +
              sqlOptHasId(tm.equipmentModel);
        if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_6)
            q = q + ", "
                  + Integer.toString(tm.getTimeAdjustment()) + ", " +
                "'" + tm.getPreamble() + "'";
        if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_7)
            q = q + ", "
            + sqlOptString(tm.getTimeZone());
        if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_11)
            q = q + ", " +
                sqlOptString(tm.getLoggerType()) + ", " +
                sqlOptInt(tm.getBaud()) + ", " +
                sqlOptInt(tm.getStopBits()) + ", " +
                "'" + tm.getParity() + "', " +
                sqlOptInt(tm.getDataBits()) + ", " +
                (tm.isDoLogin() ? "'TRUE'" : "'FALSE'") + ", " +
                sqlOptString(tm.getUsername()) + ", " +
                sqlOptString(tm.getPassword());
        q = q + ")";
        try { executeUpdate(q); }
        catch(SQLException ex)
        {
            log.warn("Error on query '{}'", q , ex);
        }
    }

    /**
    * Deletes a platform from the database, including its transport
    * media, it's PlatformSensors and PlatformSensorProperties.
    * It's configuration is NOT deleted.
    * The Platform object has it's ID unset, and it is removed from
    * its RecordList.
    * @param p the Platform
    */
    public void delete(Platform p)
        throws SQLException, DatabaseException
    {
        PlatformStatusDAI platformStatusDAO = _dbio.makePlatformStatusDAO();
        if (platformStatusDAO != null)
        {
            try { platformStatusDAO.deletePlatformStatus(p.getId()); }
            catch(DbIoException ex)
            {
                throw new DatabaseException("Cannot delete platform status for platform with id="
                    + p.getId() + ": " + ex);
            }
            finally { platformStatusDAO.close(); }
        }
        deletePlatformSensors(p);
        deleteTransportMedia(p);
        if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_6)
        {
            PropertiesDAI propsDao = _dbio.makePropertiesDAO();
            propsDao.setManualConnection(connection);

            try { propsDao.deleteProperties("PlatformProperty", "platformId", p.getId()); }
            catch (DbIoException e)
            {
                throw new DatabaseException(e.getMessage());
            }
            finally
            {
                propsDao.close();
            }
        }

        if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_10)
        {
            DacqEventDAI dacqEventDAO = _dbio.makeDacqEventDAO();
            try { dacqEventDAO.deleteEventsForPlatform(p.getId()); }
            catch (DbIoException ex)
            {
                log.warn("Error deleting DACQ_EVENT records for platform {}",
                         p.getDisplayName(), ex);
            }
            finally { dacqEventDAO.close(); }
        }

        String q = "DELETE FROM Platform WHERE ID = " + p.getId();
        executeUpdate(q);

        // NOTE: it's up to the caller to remove from Database collection(s).
    }

    /**
    * Delete all the PlatformSensor and PlatformSensorProperty records
    * associated with a Platform.  The Platform must have its SQL
    * database ID set.
    * @param p the Platform
    */
    private void deletePlatformSensors(Platform p)
        throws DatabaseException, SQLException
    {
        PropertiesDAI propsDao = _dbio.makePropertiesDAO();
        propsDao.setManualConnection(connection);
        try { propsDao.deleteProperties("PlatformSensorProperty", "platformID", p.getId()); }
        catch (DbIoException e)
        {
            throw new DatabaseException(e.getMessage());
        }
        finally
        {
            propsDao.close();
        }
        String q = "DELETE FROM PlatformSensor WHERE PlatformId = " + p.getId();
        tryUpdate(q);
    }

    /**
    * This deletes all the TransportMedium's belonging to this Platform.
    * The Platform must have already had its SQL database ID set.
    * @param p the Platform
    */
    private void deleteTransportMedia(Platform p)
        throws DatabaseException, SQLException
    {
        String q = "DELETE FROM TransportMedium WHERE PlatformId = " + p.getId();
        tryUpdate(q);
    }

    /**
     * Find the platform ID with the matching medium type/id.
     * If timestamp is provided, find the platform with the earliest expiration
     * date after the specified timestamp.
     * expiration date
     * @param mediumType
     * @param mediumId
     * @param timeStamp
     * @return
     */
    public DbKey lookupPlatformId(String mediumType, String mediumId,
        Date timeStamp)
    {

        String q = "SELECT PlatformId, expiration "
            + "FROM TransportMedium, Platform WHERE ";
        mediumType = mediumType.toLowerCase();
        if (mediumType.startsWith("goes"))
        {
            q = q + "(lower(mediumType) = "
                + sqlReqString(Constants.medium_Goes.toLowerCase())
                + " OR lower(mediumType) = "
                + sqlReqString(Constants.medium_GoesST.toLowerCase())
                + " OR lower(mediumType) = "
                + sqlReqString(Constants.medium_GoesRD.toLowerCase())
                + ")";
        }
        else
            q = q + "lower(mediumType) = " + sqlReqString(mediumType);
        q = q + " AND upper(mediumId) = " + sqlReqString(mediumId.toUpperCase());
        q = q + " AND TransportMedium.platformId = Platform.id";
        if (timeStamp == null)
            timeStamp = new Date();
        try (Statement stmt = createStatement();)
        {
            try (ResultSet rs = stmt.executeQuery(q);)
            {
                DbKey pid = Constants.undefinedId;
                DbKey bestId = Constants.undefinedId;
                Date bestExp = null;
                while (rs != null && rs.next())
                {
                    pid = DbKey.createDbKey(rs, 1);
                    if (rs.wasNull())
                        continue;
                    Date expiration = getTimeStamp(rs, 2, null);
                    if (expiration != null
                    && timeStamp.compareTo(expiration) > 0)
                        // this timestamp is after expiration, skip it.
                        continue;

                    if (expiration == null) // 'current' version
                    {
                        if (bestId == Constants.undefinedId)
                            bestId = pid;
                    }
                    else if (bestExp == null || bestExp.compareTo(expiration) > 0)
                    {
                        bestId = pid;
                        bestExp = expiration;
                    }
                }
                return bestId;
            }
        }
        catch(SQLException ex)
        {
            log.warn("SQL Execution on '{}'", q ,ex);
            return Constants.undefinedId;
        }
    }

    public DbKey lookupCurrentPlatformId(SiteName sn,
        String designator, boolean useDesignator)
        throws DatabaseException
    {
        String q = "SELECT platform.id FROM "
            + "platform, sitename "
            + "WHERE platform.siteid = sitename.siteid "
            + "AND lower(sitename.nametype) = "
                + sqlReqString(sn.getNameType().toLowerCase())
            + " AND lower(sitename.sitename) = "
                + sqlReqString(sn.getNameValue().toLowerCase())
            + " AND platform.expiration is null";
        if (useDesignator && designator != null)
            q = q + " AND lower(platform.platformdesignator) = "
                + sqlReqString(designator.toLowerCase());
        try (Statement stmt = createStatement();)
        {
            try (ResultSet rs = stmt.executeQuery(q);)
            {
                DbKey pid = Constants.undefinedId;
                if (rs != null && rs.next())
                    pid = DbKey.createDbKey(rs, 1);
                return pid;
            }
        }
        catch(SQLException ex)
        {
            log.warn("SQL Execution on '{}'", q , ex);
            return Constants.undefinedId;
        }
    }


    /**
    * Returns the last-modify-time for this platform in the database.
    * When the editor modifies a config, equipment-model, or site, it also
    * updatess the platform record.
    * @param p the Platform
    */
    public Date getLMT(Platform p)
        throws DatabaseException
    {
        try (Statement stmt = createStatement();)
        {
            String q = "SELECT lastModifyTime FROM Platform WHERE id = " + p.getId();
            try (ResultSet rs = stmt.executeQuery(q);)
            {
                // Should be only 1 record returned.
                if (rs == null || !rs.next())
                {
                    log.warn("Cannot get SQL LMT for platform ID {}" , p.getId());
                    return null;
                }

                Date ret = getTimeStamp(rs, 1, (Date)null);
                return ret;
            }
        }
        catch(SQLException ex)
        {
            log.warn("SQL Error reading LMT for platform ID {}",
                    p.getId(), ex);
            return null;
        }
    }

    public Date getListLMT()
    {
        String q = "SELECT MAX(lastModifyTime) from Platform";
        try (Statement stmt = createStatement();)
        {
            try (ResultSet rs = stmt.executeQuery(q);)
            {
                // Should be only 1 record returned.
                if (rs == null || !rs.next())
                {
                    log.warn("Cannot get SQL LMT for platform list.");
                    return new Date(0L);
                }

                Date ret = getTimeStamp(rs, 1, new Date(0L));
                return ret;
            }
        }
        catch(SQLException ex)
        {
            log.warn("SQL Error reading LMT for platform list: ", ex);
            return new Date(0L);
        }
    }


    public ArrayList<String> readNetworKListName(String transportId)
    {
        ArrayList<String> networkListArray =new ArrayList<String>();
        String q = "SELECT networklist.name FROM networklist, networklistentry WHERE networklistentry.transportid= '"+transportId+"' AND networklistentry.networklistid=networklist.id";
        try (Statement stmt = createStatement();)
        {
            try (ResultSet rs = stmt.executeQuery(q);)
            {
                // Should be only 1 record returned.
                while(rs.next())
                {
                    networkListArray.add(rs.getString(1));
                }
                return networkListArray;
            }
        }
        catch(SQLException ex)
        {
            log.warn("SQL Error reading networkList: " + ex);
            return null;
        }
    }
}
