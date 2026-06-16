package org.opendcs.database.dai;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;

import decodes.db.Platform;
import decodes.db.Site;
import decodes.sql.DbKey;

public interface PlatformDao extends OpenDcsDao
{
    /**
     * Retrieve a specific platform by the given DbKey.
     * @param tx
     * @param id
     * @return
     * @throws OpenDcsDataException
     */
    Optional<Platform> getById(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * Retreive a specific Platform by medium type and medium id
     * @param tx
     * @param mediumType
     * @param mediumId
     * @return
     * @throws OpenDcsDataException
     */
    default Optional<Platform> getByMediumId(DataTransaction tx, String mediumType, String mediumId) throws OpenDcsDataException
    {
        return getByMediumId(tx, mediumType, mediumId, null);
    }

    /**
     * Retrieve a specific Platform by medium type, medium id, and the date/time desired.
     * @param tx
     * @param mediumType
     * @param mediumId
     * @param effectiveFor
     * @return
     * @throws OpenDcsDataException
     */
    Optional<Platform> getByMediumId(DataTransaction tx, String mediumType, String mediumId, ZonedDateTime effectiveFor) throws OpenDcsDataException;

    /**
     * Find the first platform that matches the given site
     * @param tx
     * @param site
     * @return
     * @throws OpenDcsDataException
     */
    default Optional<Platform> findPlatformFor(DataTransaction tx, Site site) throws OpenDcsDataException
    {
        return findPlatformFor(tx, site, null);
    }

    /**
     * Find platform that matches the site and designator
     * @param tx
     * @param site
     * @param designator
     * @return
     * @throws OpenDcsDataException
     */
    default Optional<Platform> findPlatformFor(DataTransaction tx, Site site, String designator) throws OpenDcsDataException
    {
        var platforms = findPlatformsFor(tx, site, designator);
        return platforms.isEmpty() ? Optional.empty() : Optional.of(platforms.getFirst());
    }

    /**
     * Retrieve all platform configured for a given site.
     * @param tx
     * @param site
     * @param designator if not null will always return at most one platform
     * @return
     * @throws OpenDcsDataException
     */
    List<Platform> findPlatformsFor(DataTransaction tx, Site site, String designator) throws OpenDcsDataException;

    /**
     * Save a platform to the database.
     * 
     * <b>MUST</b> throw Exception if PlatformConfig has not already been saved.
     * @param tx
     * @param platform
     * @return new Platform instances of the platform that was just saved will all detailed, such as generated key, fill out
     * @throws OpenDcsDataException
     */
    Platform save(DataTransaction tx, Platform platform) throws OpenDcsDataException;

    /**
     * Remove platform from the database. Will fail if platform is assigned to any netlists or routing specs.
     *
     * @param tx
     * @param id
     * @throws OpenDcsDataException
     */
    void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    /**
     * Retrieve List of platforms from the database. Optionally retrieving config and equipment information.
     *
     * @param tx
     * @param limit
     * @param offset
     * @param fillAll whether or not to fill out all data.
     * @return
     * @throws OpenDcsDataException
     */
    default List<Platform> getAll(DataTransaction tx, int limit, int offset, boolean fillAll) throws OpenDcsDataException
    {
        return getAll(tx, limit, offset, fillAll, null);
    }

    /**
     * Retrieve list of platforms fro mthe database of the provided medium type.
     * Optionally retrieving all additional data, such as the Platform Config and Equipment information.
     * @param tx
     * @param limit
     * @param offset
     * @param fillAll
     * @param mediumType specifies which group of platforms we wish to retrieve.
     * @return
     * @throws OpenDcsDataException
     */
    List<Platform> getAll(DataTransaction tx, int limit, int offset, boolean fillAll, String mediumType) throws OpenDcsDataException;
}
