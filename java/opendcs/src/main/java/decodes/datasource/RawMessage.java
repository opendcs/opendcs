/*
*  $Id$
*
*  $Log$
*  Revision 1.4  2008/11/20 18:49:18  mjmaloney
*  merge from usgs mods
*
*/
package decodes.datasource;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import ilex.var.Variable;
import ilex.util.ArrayUtil;

import decodes.util.*;
import decodes.db.*;
import lrgs.common.DcpMsg;

/**
A RawMessage object holds the data received from the data source along with
references to the platform's meta data in the database.
*/
public class RawMessage
{
	private Platform platform;
	private TransportMedium transportMedium;
	private Date timeStamp;
	byte[] data;
	private HashMap<String, Variable> performanceMeasurements
		= new HashMap<String, Variable>();
	private int headerLength;

	/// String parsed from header to be used for platform association.
	private String mediumId;

	/** The source that created this raw message */
	public String dataSourceName = "unknown";
	
	/** LRGS Data Source sets this. DCP Monitor uses it. */
	private DcpMsg origDcpMsg = null;
	
	/** default constructor */
	public RawMessage()
	{
		platform = null;
		transportMedium = null;
		data = null;
		timeStamp = null;
		headerLength = 0;
		mediumId = null;
	}

	/**
	  Constructs a RawMessage from the passed data buffer and length.
	  The data will be copied into this object so it is safe to call
	  with a static buffer that subsequently gets used by a different
	  thread.
	  @param data the data
	  @param len the length of the data
	*/
	public RawMessage(byte[] data, int len)
	{
		this();
		this.data = ilex.util.ArrayUtil.getField(data, 0, len);
	}

	public RawMessage(byte[] data)
	{
		this(data, data.length);
	}

	/**
	  Sets the platform.
	  @param p the platform
	*/
	public void setPlatform(Platform p) { platform = p; }

	/** 
	  @return the Platform.
	  @throws UnknownPlatformException if not set.
	*/
	public Platform getPlatform()
		throws UnknownPlatformException
	{
		if (platform == null)
			throw new UnknownPlatformException(
				"No Platform defined for message '"
				+ new String(data, 0, 
					(data.length > headerLength ? headerLength : data.length))
				+ "'");
		return platform;
	}
	
	public Platform getPlatformOrNull() { return platform; }

	/**
	  The TransportMedium object is resolved by the DataSource and
	  set explicitely here.
	  @param tm the TransportMedium
	*/
	public void setTransportMedium(TransportMedium tm)
	{
		this.transportMedium = tm;
		if ( tm != null )
			setMediumId(tm.getMediumId());
	}

	/**
	  @return the TransportMedium for this message.
	  @throws DecodesException if there is none defined.
	*/
	public TransportMedium getTransportMedium()
		throws UnknownPlatformException
	{
		if (transportMedium == null)
			throw new UnknownPlatformException(
				"No TransportMedium defined for message '"
				+ new String(data, 0, 
					(data.length > headerLength ? headerLength : data.length))
				+ "'");
		return transportMedium;
	}

	/**
	  Sets the time-stamp associated with this message.
	  @param ts the time stamp
	*/
	public void setTimeStamp(Date ts)
	{
		timeStamp = ts;
	}

	/** @return this message's time stamp. */
	public Date getTimeStamp()
	{
		return timeStamp;
	}

	/**
	  The PMParser calls this method to set the header length depending on
	  the kind of message this is (GOES DOMSAT headers are all 37 bytes).
	  @param len the length
	*/
	public void setHeaderLength(int len)
	{
		headerLength = len;
	}

	/** @return the header length */
	public int getHeaderLength() { return headerLength; }

	/** @return the header portion of the message.  */
	public byte[] getHeader()
	{
		return ArrayUtil.getField(data, 0, headerLength);
	}
		
	/**
	  @return the complete data array including header.
	*/
	public byte[] getData()
	{
		return data;
	}

	/**
	  @return only the message data with the header removed.
	*/
	public byte[] getMessageData()
	{
		if (data.length <= headerLength)
			return new byte[0];
		return ilex.util.ArrayUtil.getField(data, headerLength, 
			data.length - headerLength);
	}

	/**
	 * @return the line number at the start of the data.
	 */
	public int getStartLineNum()
	{
		int ln = 0;
		for(int i=0; i<data.length && i < headerLength; i++)
			if (data[i] == (byte)'\n')
				ln++;
		return ln;
	}

	/**
	 * Returns the performance measurement with a given name, if one exists.
	 * Returns null if not.
	 */
	public Variable getPM(String name)
	{
		return performanceMeasurements.get(name);
	}

	/**
	  Sets or replaces the value of the name in the performance measurements.
	  @param name the name of the performance measurement.
	  @param value the value of the performance measurement.
	 */
	public void setPM(String name, Variable value)
	{
		performanceMeasurements.put(name, value);
	}

	/**
	 * Deletes the performance measurement with the given name.
	  @param name the name of the performance measurement.
	 */
	public void removePM(String name)
	{
		performanceMeasurements.remove(name);
	}

	public Iterator<String> getPMNames()
	{
		return performanceMeasurements.keySet().iterator();
	}

	/** @return message data as a string. */
	public String toString()
	{
		return new String(data);
	}

	/** @return the medium ID in this message */
	public String getMediumId() { return mediumId; }

	/**
	  Sets the medium ID for this message.
	  @param id the id
	*/
	public void setMediumId(String id) { mediumId = id; }

	/** Retrieve original DcpMsg or null if none was set. */
	public DcpMsg getOrigDcpMsg() { return origDcpMsg; }

	/** LRGS Data Source will store original DcpMsg object in raw msg */
	public void setOrigDcpMsg(DcpMsg dm) { origDcpMsg = dm; }

}

