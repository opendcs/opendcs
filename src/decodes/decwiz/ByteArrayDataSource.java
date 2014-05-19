/**
 * $Id$
 * 
 * Open Source Software
 * 
 * $Log$
 * Revision 1.4  2013/03/21 18:27:40  mmaloney
 * DbKey Implementation
 *
 */
package decodes.decwiz;

import java.util.Date;
import java.util.Properties;
import java.util.Vector;
import ilex.util.Logger;
import ilex.var.Variable;

import decodes.datasource.*;
import decodes.db.*;

public class ByteArrayDataSource
	extends DataSourceExec
{
	byte[] bytes;
	PMParser pmParser = null;
	RawMessage rawMessage = null;

	public ByteArrayDataSource(byte[] bytes)
	{
		super();
		this.bytes = bytes;
		pmParser = null;
		allowNullPlatform = false;
		allowDapsStatusMessages = false;
	}

	/** @return name of this data source, as defined in DECODES DB. */
	public String getName()
	{
		return "ByteArrayDataSource";
	}

	/** 
	  Return currently active source. Groups should override this to delegate
	  to the active member.
	  @return currently active source.
	*/
	public String getActiveSource()
	{
		return getName();
	}

	/**
	 * Does nothing.
	 */
	public void processDataSource() {}

	/**
	 * Does nothing.
	 */
	public void init(Properties routingSpecProps, String since, 
		String until, Vector networkLists)
	{
	}

	/**
	 * Does nothing.
	*/
	public void close() {}

	/**
	  Reads the next raw message from the data source and returns it.
	  This DataSource will fill in the message data and attempt to 
	  associate it with a TransportMedium object.

	  @return the next RawMessage object from the data source.

	  @throws DataSourceTimeoutException if the data source is still
	  waiting for a message and the timeout (as defined in the properties
	  when init was called) has expired.
	  @throws DataSourceException if some other problem arises.
	*/
	public RawMessage getRawMessage()
		throws DataSourceException
	{
		rawMessage = new RawMessage(bytes, bytes.length);
		return rawMessage;
	}
	

	public void associatePlatform()
		throws DataSourceException
	{
		Platform plat = null;
		TransportMedium tm = null;
		try
		{ tm = rawMessage.getTransportMedium();}
		catch ( UnknownPlatformException ue ) { tm = null; };
		try 
		{
			if ( tm == null ) { 	
				plat = Database.getDb().platformList.getPlatform(
					pmParser.getMediumType(), rawMessage.getMediumId(), 
					rawMessage.getTimeStamp());
			} else {
				plat = Database.getDb().platformList.getPlatform(
					tm.getMediumType(), tm.getMediumId(), 
					rawMessage.getTimeStamp());
			}
		}
		catch(DatabaseException ex)
		{
			String msg = "Cannot read platform for medium type '"
				+ pmParser.getMediumType() + "' with ID '"
				+ rawMessage.getMediumId() + "': " + ex;
			throw new DataSourceException(msg);
		}

		if (plat == null)
		{
			String msg = "Cannot determine platform for medium type '"
				+ pmParser.getMediumType() + "' with ID '"
				+ rawMessage.getMediumId() + "'.";
			throw new UnknownPlatformException(msg);
		}

		rawMessage.setPlatform(plat);
		tm = resolveTransportMedium(plat, 
			rawMessage.getMediumId(), -1, false);
		rawMessage.setTransportMedium(tm);
	}
			
	public void parseHeader()
		throws HeaderParseException
	{
		try
		{
			pmParser = PMParser.getPMParser(Constants.medium_EDL);
			pmParser.parsePerformanceMeasurements(rawMessage);
			return;
		}
		catch(HeaderParseException ex)
		{
			Logger.instance().warning("Cannot parse as USGS-EDL: " + ex);
		}

		try
		{
			pmParser = PMParser.getPMParser(Constants.medium_Goes);
			pmParser.parsePerformanceMeasurements(rawMessage);
			Variable datevar = rawMessage.getPM(GoesPMParser.MESSAGE_TIME);
			Date msgTime = datevar.getDateValue();
			rawMessage.setTimeStamp(msgTime);
		}
		catch(HeaderParseException ex)
		{
			String msg = "Cannot parse as either USGS-EDL or GOES msg - "
				+ ex;
			Logger.instance().failure(msg);
			pmParser = null;
			throw new HeaderParseException(msg);
		}
		catch(Exception ex)
		{
			String msg = "Unexpected error parsing header: " + ex;
			System.err.println(msg);
			ex.printStackTrace();
			throw new HeaderParseException(msg);
		}
	}
	
	public void parseHeader(String mediumType)
		throws HeaderParseException
	{
		try
		{
			pmParser = PMParser.getPMParser(mediumType);
			pmParser.parsePerformanceMeasurements(rawMessage);
			return;
		}
		catch(HeaderParseException ex)
		{
			Logger.instance().warning("Cannot parse as USGS-EDL types: " + ex);
		}

		try
		{
			pmParser = PMParser.getPMParser(Constants.medium_Goes);
			pmParser.parsePerformanceMeasurements(rawMessage);
			Variable datevar = rawMessage.getPM(GoesPMParser.MESSAGE_TIME);
			Date msgTime = datevar.getDateValue();
			rawMessage.setTimeStamp(msgTime);
		}
		catch(HeaderParseException ex)
		{
			String msg = "Cannot parse as either USGS-EDL or GOES msg - "
				+ ex;
			Logger.instance().failure(msg);
			pmParser = null;
			throw new HeaderParseException(msg);
		}
		catch(Exception ex)
		{
			String msg = "Unexpected error parsing header: " + ex;
			System.err.println(msg);
			ex.printStackTrace();
			throw new HeaderParseException(msg);
		}
	}
}
