/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package opendcs.dao;

import ilex.util.TextUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import opendcs.dai.PropertiesDAI;
import opendcs.dai.SiteDAI;
import opendcs.util.sql.WrappedConnection;
import decodes.db.Constants;
import decodes.db.DatabaseException;
import decodes.db.Site;
import decodes.db.SiteList;
import decodes.db.SiteName;
import decodes.sql.DbKey;
import decodes.sql.DecodesDatabaseVersion;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;

public class SiteDAO extends DaoBase implements SiteDAI
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    // MJM Increased from 30 min to 3 hours for 6.4 RC08
    public static final long CACHE_MAX_AGE = 3 * 60 * 60 * 1000L;
    protected static DbObjectCache<Site> cache = new DbObjectCache<Site>(CACHE_MAX_AGE, false);

    public String siteAttributes =
        "id, latitude, longitude, nearestCity, state, "
        + "region, timezone, country, elevation, elevUnitAbbr, description";
    public String siteNameAttributes;
    public String siteTableName = "Site";
    protected String siteTableKeyColumn = "id";

    protected PropertiesDAI propsDao = null;
    protected long lastCacheFillMsec = 0L;

    public SiteDAO(DatabaseConnectionOwner tsdb)
    {
        super(tsdb, "SiteDao");
        propsDao = new PropertiesSqlDao(tsdb);
        siteNameAttributes =
            (tsdb.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_7) ?
                "siteid, nameType, SiteName, dbNum, agency_cd" :
                "siteid, nameType, SiteName";
        // MJM 2016/05/23 Do not add the new fields for HDB.
        if (tsdb.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_10)
            siteAttributes = siteAttributes +
                ", active_flag, location_type, modify_time, public_name";
    }

    @Override
    public void setManualConnection(Connection conn)
    {
        super.setManualConnection(conn);
        propsDao.setManualConnection(conn);
    }

    @Override
    public Connection getConnection()
    {
        Connection conn = super.getConnection();
        propsDao.setManualConnection(conn);
        return new WrappedConnection(conn, c -> {});
    }


    @Override
    public synchronized Site getSiteById(DbKey id)
        throws DbIoException, NoSuchObjectException
    {
        if (DbKey.isNull(id))
            return null;

        Site ret = cache.getByKey(id);
        if (ret != null)
            return ret;

        Site site = new Site();
        site.forceSetId(id);
        readSite(site);
        cache.put(site);
        return site;
    }

    protected void readNames(Site site)
        throws DbIoException, SQLException
    {
        // Next read all Names and then assign them to sites.
        String q = "SELECT " + siteNameAttributes
            + " FROM SiteName WHERE siteId = ?";

        doQuery(q, rs -> {
            SiteName sn = new SiteName(site, rs.getString(2),
                rs.getString(3));
            sn.setUsgsDbno(rs.getString(4));
            sn.setAgencyCode(rs.getString(5));
            site.addName(sn);
        }, site.getId());
    }

    /**
      Extract values from result set and put them in the passed Site.
      @param site the Site object
      @param rsSite the JDBC Result Set
      @returns Site - the site object that was passed in.
    */
    protected Site resultSet2Site(Site site, ResultSet rsSite)
        throws SQLException
    {
        site.forceSetId(DbKey.createDbKey(rsSite, 1));
        site.latitude = rsSite.getString(2);
        site.longitude = rsSite.getString(3);
        site.nearestCity = rsSite.getString(4);
        site.state = rsSite.getString(5);
        site.region = rsSite.getString(6);
        site.timeZoneAbbr = rsSite.getString(7);
        site.country = rsSite.getString(8);

        double d = rsSite.getDouble(9);
        if (!rsSite.wasNull())
            site.setElevation(d);
        site.setElevationUnits(rsSite.getString(10));
        site.setDescription(rsSite.getString(11));
        if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_10)
        {
            site.setActive(TextUtil.str2boolean(rsSite.getString(12)));
            site.setLocationType(rsSite.getString(13));
            site.setLastModifyTime(db.getFullDate(rsSite, 14));
            site.setPublicName(rsSite.getString(15));
        }
        return site;
    }

    @Override
    public synchronized DbKey lookupSiteID(final String nameValue)
        throws DbIoException
    {
        // The 'uniqueName' in the cache will be the preferred site name type
        Site site = cache.getByUniqueName(nameValue);
        if (site != null)
            return site.getKey();

        // If not found, search the cache for any name match
        site = cache.search(
            new Comparable<Site>()
            {
                @Override
                public int compareTo(Site ob)
                {
                    for(SiteName sn : ob.getNameArray())
                        if (sn.getNameValue().equalsIgnoreCase(nameValue))
                            return 0;
                    // Note: DbObjectCache.search does a linear search, not a binary search.
                    // So always returning -1 meaning 'no match' is okay.
                    return -1;
                }
            });
        if (site != null)
            return site.getKey();


        // Finally search the database for a SiteName with matching value.
        String q = "select siteid from SiteName "
            + " where lower(siteName) = lower(?)";
        try
        {
            DbKey key = getSingleResult(q, rs -> DbKey.createDbKey(rs, 1),nameValue);
            if (key == null)
            {
                key = Constants.undefinedId;
            }
            return key;
        }
        catch(SQLException ex)
        {
            String msg = "lookupSiteId(str) - Error in query '" + q + "'";
            throw new DbIoException(msg, ex);
        }
    }

    @Override
    public synchronized DbKey lookupSiteID( final SiteName siteName )
        throws DbIoException
    {
        Site site = cache.search(
            new Comparable<Site>()
            {
                @Override
                public int compareTo(Site ob)
                {
                    SiteName obsn = ob.getName(siteName.getNameType());
                    if (obsn == null)
                        return -1;

                    return siteName.getNameValue().toLowerCase().compareTo(
                        obsn.getNameValue().toLowerCase());
                }
            });
        if (site != null)
            return site.getKey();

        String q = "select siteid from SiteName "
            + " where lower(nameType) = lower(?)"
            + " and lower(siteName) = lower(?)";
        try
        {
            DbKey key = getSingleResult(q, rs -> DbKey.createDbKey(rs,1), siteName.getNameType(),siteName.getNameValue());
            if (key == null)
            {
                key = Constants.undefinedId;
            }
            return key;
        }
        catch(SQLException ex)
        {
            String msg = "lookupSiteId - Error in query '" + q + "'";
            throw new DbIoException(msg, ex);
        }
    }

    @Override
    public void fillCache()
        throws DbIoException
    {
        log.trace("(Generic)SiteDAO.fillCache()");

        final HashMap<DbKey, Site> siteHash = new HashMap<DbKey, Site>();
        String q = "SELECT " + siteAttributes + " FROM " + siteTableName;
        int nNames[] = new int[1];
        nNames[0] = 0;

        try
        {
            doQuery(q, rs ->
            {
                Site site = new Site();
                resultSet2Site(site, rs);
                siteHash.put(site.getKey(), site);
            });

            q = "SELECT " + siteNameAttributes + " FROM SiteName order by nameType, siteName";
            final SiteName prevName = new SiteName(null, "","");
            DbKey prevId[] = new DbKey[1];
            prevId[0] = DbKey.NullKey;

            doQuery(q, rs ->
            {
                DbKey key = DbKey.createDbKey(rs, 1);
                Site site = siteHash.get(key);

                if (site == null)
                {
                    if (!db.isHdb()) // For some crazy reason, HDB has lots of orphan site names.
                    {
                        log.warn("SiteName for id={}, but no matching site.", key);
                    }
                    return;
                }

                // There is an issue in HDB with multiple identical site names pointing to different sites.
                // The HDB site name query orders results by type,value.
                final String nameType = rs.getString(2);
                final String nameValue = rs.getString(3);
                if (prevName.getNameType().equalsIgnoreCase(nameType) && prevName.getNameValue().equalsIgnoreCase(nameValue))
                {
                    log.warn("SiteName for id={} with nametype={} and nameValue={} " +
                             "is a duplicate to a name to a different site with id={}. Discarding the name for {}",
                             key ,nameType, nameValue, prevId[0], key);
                }
                else
                {
                    prevName.setNameType(nameType);
                    prevName.setNameValue(nameValue);
                    prevId[0] = key;
                }

                SiteName sn = new SiteName(site, nameType, nameValue);
                sn.setUsgsDbno(rs.getString(4));
                sn.setAgencyCode(rs.getString(5));
                site.addName(sn);
                nNames[0]++;
            });
        }
        catch(SQLException ex)
        {
            String msg = "fillCache - Error in query '" + q + "'";
            throw new DbIoException(msg, ex);
        }

        for(Site site : siteHash.values())
            cache.put(site);
        int nProps = 0;
        if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_8)
            nProps = propsDao.readPropertiesIntoCache("site_property", cache);
        log.debug("Site Cache Filled: {} sites, {} names, {} properties.",
                  cache.size(), nNames[0], nProps);
        lastCacheFillMsec = System.currentTimeMillis();
    }

    @Override
    public void read(SiteList siteList) throws DbIoException
    {
        if (cache.size() == 0)
            fillCache();
        for(DbObjectCache<Site>.CacheIterator cit = cache.iterator(); cit.hasNext(); )
            siteList.addSite(cit.next());
    }

    @Override
    public void readSite(Site site)
        throws DbIoException, NoSuchObjectException
    {
        DbKey id = site.getId();
        try
        {
            Site site2 = getSingleResult("select * from site where id = ?",
                                        rs -> resultSet2Site(site, rs), id );
            if (site2 == null)
            {
                throw new NoSuchObjectException("No such site with ID=" + id);
            }

            readNames(site);
            if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_8)
                propsDao.readProperties("site_property", "site_id", id, site.getProperties());
        }
        catch(SQLException ex)
        {
            String err = "Error in readSite(" + siteTableKeyColumn + "=" + id + ")";
            throw new DbIoException(err, ex);
        }
    }

    @Override
    public Site getSiteBySiteName(SiteName sn)
        throws DbIoException
    {
        DbKey id = lookupSiteID(sn);
        if (id == null || id.isNull())
            return null;

        try { return getSiteById(id); }
        catch (NoSuchObjectException e) { return null; }
    }

    @Override
    public void writeSite(Site newSite)
        throws DbIoException
    {
        if (!newSite.idIsSet())
        {
            for (Iterator<SiteName> snit = newSite.getNames(); snit.hasNext(); )
            {
                DbKey id = lookupSiteID(snit.next());
                if (id != null && !id.isNull())
                {
                    newSite.forceSetId(id);
                    break;
                }
            }
        }
        newSite.setLastModifyTime(new Date());
        if (newSite.idIsSet())
        {
            try
            {
                Site dbSite = new Site();
                dbSite.forceSetId(newSite.getId());
                readSite(dbSite);
                cache.remove(dbSite.getId());
                update(newSite, dbSite);
                cache.put(newSite);
            }
            catch(NoSuchObjectException ex)
            {
                newSite.clearId();
            }
        }
        if (!newSite.idIsSet())
        {
            insert(newSite);
            cache.put(newSite);
        }
        if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_8)
            propsDao.writeProperties("site_property", "site_id",
                newSite.getKey(), newSite.getProperties());
    }


    /**
    * Updates an already-existing Site in the database.
      @param newSite the new Site object to write to the database
      @param dbSite the site as it currently exists in the database
    */
    protected void update(Site newSite, Site dbSite)
        throws DbIoException, NoSuchObjectException
    {
        DbKey id = newSite.getId();

        String desc = newSite.getDescription();
        if (desc == null)
            desc = "";
        if (desc.length() > 800)
            desc = desc.substring(0,799);
        final Map<String,Object> fields = new LinkedHashMap<>(); // LinkedHashMap to preserve order

        fields.put("latitude",newSite.latitude);
        fields.put("longitude", newSite.longitude);
        fields.put("nearestCity", newSite.nearestCity);
        fields.put("state", newSite.state);
        fields.put("region", newSite.region);
        fields.put("timezone", newSite.timeZoneAbbr);
        fields.put("country", newSite.country);

        if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_6)
        {
            fields.put("elevation", newSite.getElevation());
            fields.put("elevunitabbr", newSite.getElevationUnits());
            fields.put("description",desc);
        }

        if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_10)
        {
            fields.put("active_flag",newSite.isActive() ? "TRUE" : "FALSE");
            fields.put("location_type",newSite.getLocationType());
            fields.put("modify_time", newSite.getLastModifyTime());
            fields.put("public_name", newSite.getPublicName());
        }
        StringBuilder q = new StringBuilder("UPDATE site SET ");
        Iterator<String> columnSet = fields.keySet().iterator();
        while(columnSet.hasNext())
        {
            q.append(columnSet.next()).append("=?");
            if(columnSet.hasNext())
            {
                q.append(",");
            }
            q.append(" ");
        }
        q.append( " WHERE " + siteTableKeyColumn + " = ?");
        log.trace("Running '{}'", q);
        try
        {
            List<Object> parameters =
                     fields.entrySet()
                           .stream()
                           .map(e -> e.getValue())
                           .collect(Collectors.toList());
            parameters.add(id);
            doModify(q.toString(), parameters.toArray(new Object[0]));
        }
        catch (SQLException ex)
        {
            throw new DbIoException("Unable to update site entry", ex);
        }

        updateAllSiteNames(newSite, dbSite);
        // Now delete & re-insert the SiteNames.
        //deleteSiteNames(s);
        //insertAllSiteNames(s, id);
    }

    /**
     * Update SiteName records for the given site.
     *
     * @param newSite Site object with new information.
     * @param dbSite Site object as it was from the database.
     * @throws DatabaseException
     * @throws SQLException
     */
    protected void updateAllSiteNames(Site newSite, Site dbSite)
        throws DbIoException, NoSuchObjectException
    {
        //Go through the site names to determine records to insert or update
        for (Iterator<SiteName> newIt = newSite.getNames(); newIt.hasNext(); )
        {
            SiteName newSn = newIt.next();
            SiteName dbSn = dbSite.getName(newSn.getNameType());
            if (dbSn == null)
                insertSiteName(newSite.getKey(), newSn);
            else
            {
                if (!dbSn.equals(newSn))
                    updateSiteName(newSite.getKey(), newSn);
                dbSite.removeName(newSn.getNameType());
            }
        }

        // Any names left in dbSite don't exist in newSite and should be removed
        for (Iterator<SiteName> dbSnIt = dbSite.getNames(); dbSnIt.hasNext(); )
        {
            SiteName sn = dbSnIt.next();
            try
            {
                doModify("delete from sitename where siteid = ? and lower(nametype) = lower(?)",
                         newSite.getKey(),sn.getNameType());
            }
            catch (SQLException ex)
            {
                throw new DbIoException("Unable to remove SiteName " + sn, ex);
            }
        }
    }

    /**
     * Update a SiteName record
     *
     * @param siteId
     * @param sn
     * @throws DatabaseException
     * @throws SQLException
     */
    protected void updateSiteName(DbKey siteId, SiteName sn)
        throws DbIoException
    {
        final Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("sitename",sn.getNameValue());

        if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_7)
        {
            fields.put("dbNum",sn.getUsgsDbno());
            fields.put("agency_cd",sn.getAgencyCode());
        }
        StringBuilder q = new StringBuilder("UPDATE SiteName SET ");
        Iterator<String> columnSet = fields.keySet().iterator();
        while(columnSet.hasNext())
        {
            q.append(columnSet.next()).append("=?");
            if(columnSet.hasNext())
            {
                q.append(",");
            }
            q.append(" ");
        }
        q.append(" WHERE siteid = ? and lower(nametype) = lower(?)");
        try
        {
            List<Object> parameters =
                     fields.entrySet()
                           .stream()
                           .map(e -> e.getValue())
                           .collect(Collectors.toList());
            parameters.add(siteId);
            parameters.add(sn.getNameType());
            doModify(q.toString(),parameters.toArray(new Object[0]));
        }
        catch (SQLException ex)
        {
            throw new DbIoException("Unable to update SiteName " + sn, ex);
        }
    }

    /**
    * Inserts a new Site into the database.  The Site must not have
    * an SQL Database ID value set already.
    * @param s the Site object
    */
    protected void insert(Site s)
        throws DbIoException
    {
        // Assign new ID & re-add with ID to the database's collection of sites.
        DbKey id = getKey("Site");
        s.forceSetId(id);
        s.getDatabase().siteList.addSite(s);

        String desc = s.getDescription();
        if (desc != null && desc.length() > 800)
            desc = desc.substring(0,799);
        ArrayList<String> columns = new ArrayList<>();
        ArrayList<Object> parameters = new ArrayList<>();
        StringBuilder q = new StringBuilder("INSERT INTO Site(");

        columns.add("id"); parameters.add(id);
        columns.add("latitude"); parameters.add(db.isOpenTSDB() && db.isOracle() ? ("" + s.latitude) : s.latitude);
        columns.add("longitude"); parameters.add(db.isOpenTSDB() && db.isOracle() ? ("" + s.longitude) : s.longitude);
        columns.add("nearestCity"); parameters.add(s.nearestCity);
        columns.add("state"); parameters.add(s.state);
        columns.add("region"); parameters.add(s.region);
        columns.add("timezone"); parameters.add(s.timeZoneAbbr);
        columns.add("country"); parameters.add(s.country);
        int dbVersion = db.getDecodesDatabaseVersion();

        if (dbVersion >= DecodesDatabaseVersion.DECODES_DB_6)
        {
            columns.add("elevation"); parameters.add(s.getElevation());
            columns.add("elevUnitAbbr"); parameters.add(s.getElevationUnits());
            columns.add("description"); parameters.add(desc);
        }
        if (dbVersion >= DecodesDatabaseVersion.DECODES_DB_10) // version 10 or higher
        {
            columns.add("active_flag"); parameters.add(s.isActive() ? "TRUE" : "FALSE");
            columns.add("location_type"); parameters.add(s.getLocationType());
            Date modifyTime = s.getLastModifyTime();
            columns.add("modify_time"); parameters.add(modifyTime != null ? modifyTime : new Date());
            columns.add("public_name"); parameters.add(s.getPublicName());
        }
        q.append(String.join(",",columns));
        q.append(") values(");
        q.append(parameters.stream().map(f -> "?").collect(Collectors.joining(",")));
        q.append(")");
        try
        {
            doModify(q.toString(),parameters.toArray(new Object[0]));
        }
        catch (SQLException ex)
        {
            throw new DbIoException("Unable to insert Site.", ex);
        }

        for(Iterator<SiteName> snit = s.getNames(); snit.hasNext(); )
            insertSiteName(s.getKey(), snit.next());
    }

    /**
    * Insert a new SiteName into the database.
    * @param siteId the Site SQL ID
    * @param sn the SiteName object
    */
    protected void insertSiteName(DbKey siteId, SiteName sn)
        throws DbIoException
    {
        // Ignore null or missing name values. Oracle can't store them.
        if (sn.getNameValue() == null || sn.getNameValue().trim().length() == 0)
        {
            return;
        }

        ArrayList<String> columns = new ArrayList<>();
        ArrayList<Object> parameters = new ArrayList<>();
        columns.add("siteid"); parameters.add(siteId);
        columns.add("nametype"); parameters.add(sn.getNameType());
        columns.add("sitename"); parameters.add(sn.getNameValue());

        if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_7)
        {
            columns.add("dbnum"); parameters.add(sn.getUsgsDbno());
            columns.add("agency_cd"); parameters.add(sn.getAgencyCode());
        }
        StringBuilder q = new StringBuilder("INSERT INTO SiteName(");
        q.append(String.join(",",columns));
        q.append(") values (")
         .append(parameters.stream().map(f -> "?").collect(Collectors.joining(",")))
         .append(")");
        try
        {
            log.trace("Executing '{}'", q.toString());
            doModify(q.toString(),parameters.toArray(new Object[0]));
        }
        catch (SQLException ex)
        {
            throw new DbIoException("Unable to store SiteName -> " + sn, ex);
        }
    }

    @Override
    public void deleteSite(DbKey key) throws DbIoException
    {
        // it would be good to do this as a single transaction but I'm
        // not sure of a good way to handle that given the current design of the
        // systems.
        try
        {
            doModify("delete from sitename where siteid = ?", key);
            doModify("delete from site_property where site_id = ?", key);
            doModify("delete from site where " + siteTableKeyColumn + " = ?", key);
            cache.remove(key);
        }
        catch (SQLException ex)
        {
            throw new DbIoException("Unable to delete site or portions of site.", ex);
        }
    }

    public void close()
    {
        super.close();
        propsDao.close();
    }

    public long getLastCacheFillMsec()
    {
        return lastCacheFillMsec;
    }
}
