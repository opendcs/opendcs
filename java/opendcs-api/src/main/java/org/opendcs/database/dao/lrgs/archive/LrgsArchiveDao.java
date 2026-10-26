package org.opendcs.database.dao.lrgs.archive;

import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;

import org.opendcs.data.lrgs.LrgsDataInterfaceType;
import org.opendcs.data.lrgs.archive.ArchiveEntry;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.exceptions.data.OpenDcsConstraintException;
import org.opendcs.util.Result;

/**
 * Defines an interface for pulling archived messages out of the database or putting them in.
 * LrgsArchiveDao
 */
public interface LrgsArchiveDao extends OpenDcsDao
{
    /**
     * Store a message to this archive. Returns a new instance of ArchiveEntry with
     * the storeTime set by the implementation.
     *
     * Implementations may handle message merge conflicts however they determine is best. If the message was not
     * saved, return the result as an OpenDCsConstraintException.
     *
     * @param tx DataTransaction to use.
     * @param entry entry to store.
     * @return Saved message, with stored_time field updated. Or the reason a message could not be saved.
     *         Usages should determine if a failure to save is exceptional or not. E.g. a "merge filter" not saving
     *         a bad message over a good message is not exceptional
     * @throws OpenDcsDataException
     */
    Result<ArchiveEntry, OpenDcsConstraintException> archive(DataTransaction tx, ArchiveEntry entry) throws OpenDcsDataException;

    /**
     * Retrieve archive entries by the given criteria
     * @param tx DataTransaction to use
     * @param limit limit for paging
     * @param offset offset for paging
     * @param criteria given search criteria. Can be null, in which case implementations
     * should return all data in {@link OrderBy#STORE_TIME} ascending order by default
     * @return
     * @throws OpenDcsDataException
     */
    default List<ArchiveEntry> getAllBy(DataTransaction tx, int limit, int offset, LrgsSearchCriteria criteria) throws OpenDcsDataException
    {
        return getAllBy(tx, limit, offset, criteria, OrderBy.STORE_TIME);
    }


    /**
     * Retrieve archive entries by the given criteria
     * @param tx DataTransaction to use
     * @param limit limit for paging
     * @param offset offset for paging
     * @param criteria given search criteria. Can be null, in which case implementations
     * should return all data in {@link org.opendcs.data.lrgs.archive.ArchiveEntry#getCreatedAt()} order by default
     * @return
     * @throws OpenDcsDataException
     */
    List<ArchiveEntry> getAllBy(DataTransaction tx, int limit, int offset, LrgsSearchCriteria criteria, OrderBy order) throws OpenDcsDataException;

    /**
     * Remove matching entries from the database.
     * Implementation of this method
     * @param tx DataTransaction to use
     * @param criteria criteria used to match which entries to delete.
     * @return number of entries deleted. {@see Integer#MIN_VALUE} is the default return, indicating
     *         this implementation does not allow deleted. This is used over throwing an exception as this not
     *         excpetional behavior, though varius interfaces should be informed.
     * @throws OpenDcsDataException
     */
    default int deleteBy(DataTransaction tx, LrgsSearchCriteria criteria) throws OpenDcsDataException
    {
        /* intentionally do nothing by default */
        return Integer.MIN_VALUE;
    }

    /**
     *
     * SearchCriteria
     * Archive Implementations are free to add additional criteria as required.
     *
     * NOTE: I may be over thinking this a bit, but the original SearchCriteria is rather convoluted given
     * the changes over time starting from all GOES to GOES + other thing and the various DOMSAT etc things
     * that no longer exists.
     */
    public interface LrgsSearchCriteria
    {
        List<String> getIdentifiers();
        List<LrgsDataInterfaceType> getTypes();

        /**
         * Start time of the data itself.
         * @return
         */
        ZonedDateTime getDataStartTime();

        /**
         * end time of the data
         * @return
         */
        ZonedDateTime getDataEndTime();

        /**
         * start time of search based on store data to this LRGS archive
         * @return
         */
        ZonedDateTime getStoredStartTime();

        /**
         * end time of search based on store data to this LRGS archive
         * @return
         */
        ZonedDateTime getStoredEndTime();

        /**
         * Which qualities to retrieve.
         * @return
         */
        EnumSet<ArchiveEntry.Quality> getQualities();
    }

    public enum OrderBy
    {
        STORE_TIME,
        DATA_TIME;
    }
}
