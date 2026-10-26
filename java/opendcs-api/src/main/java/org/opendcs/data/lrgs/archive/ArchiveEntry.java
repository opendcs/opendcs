package org.opendcs.data.lrgs.archive;

import java.time.ZonedDateTime;
import java.util.Map;

import org.opendcs.data.lrgs.LrgsDataInterface;

/**
 *
 * ArchiveEntry
 * @param dataInterface from which interface this message arrived
 * @param identifier DCP/interface/Logger/etc specific unique identifier. E.G. NESDIS ID for GOES, IMEI for Iridium
 * @param header if a given interface can extract header information that can be placed here. Otherwise it will be an empty map
 * @param rawData raw data of the message. use {@link String#getBytes} with a charset of UTF-8 to encode strings. Can
 *                be null.
 * @param storeTime time message was stored to this database.
 * @param dataTime time of the data message. Can be null if not practical for an interface to extract time during store.
 * @param quality status, if known, of this message. Entry doesn't concern itself with how the status is determined and
 *                trusts the various interfaces to establish it correctly. If null is provided during the creation of
 *                this record {@link Quality#UNKNOWN} is used.
 */
public record ArchiveEntry(LrgsDataInterface dataInterface, String identifier, Map<String, Object> header, byte[] rawData,
                           ZonedDateTime storeTime, ZonedDateTime dataTime, Quality quality)
{
    public ArchiveEntry
    {
        header = (header == null) ? Map.of() : Map.copyOf(header);
        quality = (quality == null) ? Quality.UNKNOWN : quality;
    }

    public enum Quality
    {
        GOOD("G"),
        BAD("B"),
        UNKNOWN("U");

        private final String qualityChar;

        Quality(String qualityChar)
        {
            this.qualityChar = qualityChar;
        }

        @Override
        public String toString()
        {
            return this.qualityChar;
        }
    }
}
