/*
*  $Id$
*/
package lrgs.ldds;

import ilex.util.ArrayUtil;

/**
This class represents a complete DDS protocol message. It is used on both
the client and server sides.
*/
public class LddsMessage
{
    public static final int ValidHdrLength = 10;
    public static final String ValidSync = "FAF0";
    public static final int ValidMaxDataLength = 99000;

    public static final String ValidIds = "abcdefghijklmnopqrstu";
    public static final char IdHello         = 'a';
    public static final char IdGoodbye       = 'b';
    public static final char IdStatus        = 'c';
    public static final char IdStart         = 'd';
    public static final char IdStop          = 'e';
    public static final char IdDcp           = 'f';
    public static final char IdCriteria      = 'g';
    public static final char IdGetOutages    = 'h';
    public static final char IdIdle          = 'i';
    public static final char IdPutNetlist    = 'j';
    public static final char IdGetNetlist    = 'k';
    public static final char IdAssertOutages = 'l';
    public static final char IdAuthHello     = 'm';
    public static final char IdDcpBlock      = 'n';
    public static final char IdEvents        = 'o';
    public static final char IdRetConfig     = 'p';
    public static final char IdInstConfig    = 'q';
    public static final char IdDcpBlockExt   = 'r';
    public static final char IdUnused_6      = 's';
    public static final char IdUnused_7      = 't';
    public static final char IdUser          = 'u';

    // Instance data:
    public char MsgId;     // Identifies type of message
    public int MsgLength;  // Length of data field following header
    public byte MsgData[]; // Data following header as a string

    /**
      This constructor is used on the receiving end after ValidHdrLength bytes
      have been read from the socket. It will throw ProtocolError
      if the header is invalid. It builds a message containing only the
      header field.
      @param hdr the DDS message header.
    */
    public LddsMessage(byte hdr[]) throws ProtocolError
    {
        if (hdr.length < ValidHdrLength)
            throw new ProtocolError(
                "Invalid LDDS message header - length=" + hdr.length);
        String sync = new String(ArrayUtil.getField(hdr, 0, 4));

        if (sync.compareTo(ValidSync) != 0)
            throw new ProtocolError(
                "Invalid LDDS message header - bad sync '" + sync + "'");

        MsgId = (char)hdr[4];
        if (ValidIds.indexOf((int)MsgId) < 0)
            throw new ProtocolError(
                "Invalid LDDS message header - ID = '" + MsgId + "'");

        // DRS server may have leading spaces in the length field.
        byte lenbytes[] = ArrayUtil.getField(hdr, 5, 5);
        for(int i = 0; i < 5; i++)
            if (lenbytes[i] == (byte)' ')
                lenbytes[i] = (byte)'0';

        String lenstr = new String(lenbytes);
        try { MsgLength = Integer.parseInt(lenstr); }
        catch (NumberFormatException nfe)
        {
            throw new ProtocolError(
                "Invalid LDDS message header - bad length field = '" +
                    lenstr+ "'");
        }
    }

    /**
      Use this constructor on the sending end. Construct the object and then
      call getBytes() to get the data to send over the socket.

      Throws ProtocolError if ID is invalid or if length is
      negative or greater than maximum allowable length.

      @param MsgId the ID to place in the header
      @param StrData the message data as a string
    */
    public LddsMessage(char MsgId, String StrData)
    {
        this.MsgId = MsgId;

        MsgLength = StrData != null ? StrData.length() : 0;

// Experimentally DON'T add the null byte at the end.
//        MsgLength = StrData != null ? StrData.length()+1 : 0;

        // Using resize will add a null byte at the end. Legacy requires this.
        MsgData = MsgLength > 0 ?
            ArrayUtil.resize(StrData.getBytes(), MsgLength) : null;
    }

    /**
      @return byte array representation of this header for sending over
      a socket.
    */
    public byte[] getBytes()
    {
        byte ret[] = new byte[ValidHdrLength + MsgLength];
        for(int i = 0; i < 4; i++)
            ret[i] = (byte)ValidSync.charAt(i);
        ret[4] = (byte)MsgId;

        // Note: 48 == ASCII '0'
        ret[5] = (byte)(48 +  MsgLength        / 10000);
        ret[6] = (byte)(48 + (MsgLength%10000) /  1000);
        ret[7] = (byte)(48 + (MsgLength% 1000) /   100);
        ret[8] = (byte)(48 + (MsgLength%  100) /    10);
        ret[9] = (byte)(48 + (MsgLength%   10)        );

        if (MsgLength > 0 && MsgData != null)
            for(int i = 0; i < MsgLength; i++)
                ret[10+i] = MsgData[i];
        return ret;
    }
}
