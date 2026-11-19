package org.opendcs.data.lrgs.archive;

import java.nio.ByteBuffer;
import java.time.ZonedDateTime;
import java.util.Map;

import org.opendcs.data.lrgs.LrgsDataInterface;
import org.opendcs.database.DatabaseKey;

/**
 *
 * ArchiveEntry
 * @param dataInterface from which interface this message arrived
 * @param dataInterfaceId unique identifier for that above interface. Some retrievals may set only this and leave
 *                        dataInterfadce null for improved performance as the number of ArchiveEntry elements should
 *                        far exceed the number of Data Interfaces
 * @param identifier DCP/interface/Logger/etc specific unique identifier. E.G. NESDIS ID for GOES, IMEI for Iridium
 * @param header if a given interface can extract header information that can be placed here. Otherwise it will be an empty map
 * @param rawData raw data of the message. use {@link String#getBytes} with a charset of UTF-8 to encode strings. Can
 *                be null. {@link ByteBuffer#asReadOnlyBuffer()} will be called so that the stored value is immutable.
 * @param storeTime time message was stored to this database.
 * @param dataTime time of the data message. Can be null if not practical for an interface to extract time during store.
 * @param quality status, if known, of this message. Entry doesn't concern itself with how the status is determined and
 *                trusts the various interfaces to establish it correctly. If null is provided during the creation of
 *                this record {@link Quality#UNKNOWN} is used.
 */
public record ArchiveEntry(LrgsDataInterface dataInterface, DatabaseKey dataInterfaceId, String identifier, Map<String, Object> header, ByteBuffer rawData,
                           ZonedDateTime storeTime, ZonedDateTime dataTime, Quality quality)
{
    public ArchiveEntry
    {
        header = (header == null) ? Map.of() : Map.copyOf(header);
        quality = (quality == null) ? Quality.UNKNOWN : quality;
        dataInterfaceId = (dataInterfaceId == null && dataInterface != null) ? dataInterface.id() : dataInterfaceId;
        rawData = (rawData == null) ? rawData : rawData.asReadOnlyBuffer();
    }

    public enum Quality
    {
        GOOD('G'),
        BAD('B'),
        UNKNOWN('U');

        public final char qualityChar;

        Quality(char qualityChar)
        {
            this.qualityChar = qualityChar;
        }

        @Override
        public String toString()
        {
            return "" + this.qualityChar;
        }

        public static Quality fromDbChar(char dbChar)
        {
            Quality ret = UNKNOWN;
            for (var qual: Quality.values())
            {
                if (qual.qualityChar == dbChar)
                {
                    ret = qual;
                }
            }
            return ret;
        }

        public static Quality fromDbChar(String dbChar)
        {
            if (dbChar.length() > 1)
            {
                return Quality.UNKNOWN;
            }
            else
            {
                return fromDbChar(dbChar.charAt(0));
            }
        }
    }
}
