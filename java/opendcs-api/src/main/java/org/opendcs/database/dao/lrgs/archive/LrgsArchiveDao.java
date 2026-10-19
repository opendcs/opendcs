package org.opendcs.database.dao.lrgs.archive;

import java.time.ZonedDateTime;
import java.util.List;

import org.opendcs.data.lrgs.LrgsDataInterfaceType;
import org.opendcs.data.lrgs.archive.ArchiveEntry;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.dao.BasicDao;

public interface LrgsArchiveDao<T extends ArchiveEntry> extends BasicDao<T>
{
    /**
     * Retreive archive entries by the given creteria
     * @param tx DataTransaction to use
     * @param limit limit for paging
     * @param offset offset for paging
     * @param criteria given search criteria. Can be null, in which case implementations
     * should return all data in {@link org.opendcs.data.lrgs.archive.ArchiveEntry#getCreatedAt()} order by default
     * @return
     * @throws OpenDcsDataException
     */
    List<T> getAllBy(DataTransaction tx, int limit, int offset, LrgsSearchCriteria criteria) throws OpenDcsDataException;

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
    }
}
