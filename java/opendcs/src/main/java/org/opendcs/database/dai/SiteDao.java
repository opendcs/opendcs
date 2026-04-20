package org.opendcs.database.dai;

import decodes.db.Site;
import decodes.db.SiteName;
import decodes.sql.DbKey;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;

public interface SiteDao extends OpenDcsDao
{
    /**
     * Retrieve site by specific ID.
     * @param tx active transaction for this request.
     * @param id it is safe to pass null or {@see DbKey.NullKey}, Optional.empty will be returned.
     * @return
     * @throws OpenDcsDataException
     */
    Optional<Site> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * Retrieve a site by specific SiteName (type + name)
     * @param tx active transaction for this request.
     * @param siteName Single site name of any type, if NameType doesn't exist, empty will be returned.
     * @return
     * @throws OpenDcsDataException
     */
    Optional<Site> getBySiteName(DataTransaction tx, SiteName siteName) throws OpenDcsDataException;

    /**
     * Given a collection of NameTypes find by matching any one of them.
     * @param tx
     * @param siteNames
     * @return
     * @throws OpenDcsDataException
     */
    Optional<Site> getByAnySiteName(DataTransaction tx, Collection<SiteName> siteNames) throws OpenDcsDataException;

    /**
     * Save (insert or update) a site to the database.
     *
     * Implementations should fail if a required site name is not present. For example CWMS requires a "CWMS" SiteNameType to be present
     * as it is used as the CWMS Location ID.
     *
     * The OpenDCS-Postgres and OpenDCS-Oracle implementation also require a CWMS Site name.
     *
     * @param tx
     * @param site
     * @return
     * @throws OpenDcsDataException
     */
    Site save(DataTransaction tx, Site site) throws OpenDcsDataException;

    /**
     * Delete a site from the database, will fail if anything, such as a time series, still
     * depends on this site.
     * @param tx
     * @param id
     * @throws OpenDcsDataException
     */
    void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * Retrieve all sites with the given row and offset.
     *
     * Data is sorted by prefered site name type first, then any others. While there *SHOULD* always
     * be a prefered name entry existing database may not. This can lead to inconsistent shorting.
     *
     * @param tx
     * @param limit
     * @param offset
     * @return
     * @throws OpenDcsDataException
     */
    List<Site> getAll(DataTransaction tx, int limit, int offset) throws OpenDcsDataException;
}
