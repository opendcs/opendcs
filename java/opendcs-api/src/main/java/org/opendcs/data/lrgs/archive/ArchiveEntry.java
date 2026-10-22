package org.opendcs.data.lrgs.archive;

import java.time.ZonedDateTime;

import org.opendcs.data.lrgs.LrgsDataInterface;

/**
 * Minimum information required for any archive.
 * ArchiveEntry
 */
public interface ArchiveEntry
{
    /**
     * Interface from which this entry arrived
     * @return
     */
    LrgsDataInterface getInterface();
    /**
     * Data Entry address/identifier. Such as NESDIS GOES ID, SHEF ID, Iridium IMEI, etc
     * @return
     */
    String getIdentifier();

    /**
     * Raw data of the entry
     * @return
     */
    byte[] getRawData();
    /**
     * Data this entry was stored
     * @return
     */
    ZonedDateTime getCreatedAt();    
}
