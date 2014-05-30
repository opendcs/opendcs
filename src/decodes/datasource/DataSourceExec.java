/*
*  $Id$
*
*  Open Source Software
*
*  $Log$
*  Revision 1.2  2014/05/28 13:09:29  mmaloney
*  dev
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.5  2013/03/21 18:27:40  mmaloney
*  DbKey Implementation
*
*  Revision 1.4  2012/07/05 13:26:57  mmaloney
*  RoutingSpecThread log method made public so that constituent classes can use it to log a message with the RS name in it.
*  DataSourceExec.log method created.
*  LrgsDataSource modified to use the RS Thread log method.
*
*  Revision 1.3  2009/02/27 00:15:17  mjmaloney
*  Iridium SBD Support
*
*  Revision 1.2  2008/09/26 14:56:53  mjmaloney
*  Added <all> and <production> network lists
*
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.14  2007/12/11 01:05:15  mmaloney
*  javadoc cleanup
*
*  Revision 1.13  2006/12/04 17:35:04  mmaloney
*  dev
*
*  Revision 1.12  2006/07/24 21:39:20  mmaloney
*  dev
*
*  Revision 1.11  2005/06/21 14:00:51  mjmaloney
*  Better responsiveness on DDS links for timeout & hangup conditions.
*
*  Revision 1.10  2005/06/04 16:49:27  mjmaloney
*  dev
*
*  Revision 1.9  2004/08/24 23:52:43  mjmaloney
*  Added javadocs.
*
*  Revision 1.8  2004/04/15 19:48:20  mjmaloney
*  Added status methods to support routing status monitor web app.
*
*  Revision 1.7  2003/11/15 20:16:32  mjmaloney
*  Use accessor methods for TransportMedium type.
*  For GOES, don't need to explicitely look for GOES, RD, and ST. The tmKey
*  in the Platform set will be the same for all three.
*
*  Revision 1.6  2003/08/12 19:19:56  mjmaloney
*  dev
*
*  Revision 1.5  2003/06/06 18:35:12  mjmaloney
*  Added allowDapsStatusMessages for status monitor routing specs, etc.
*
*  Revision 1.4  2003/06/06 14:03:41  mjmaloney
*  Added code to allow null platform linkage in special circumstances.
*
*  Revision 1.3  2003/06/06 13:50:35  mjmaloney
*  Added boolean, set, & get methods to allow unknown platform.
*
*  Revision 1.2  2003/03/05 18:13:34  mjmaloney
*  Fix DR 122 - Base class method in DataSourceExec now makes association to TM.
*
*  Revision 1.1  2001/07/08 21:16:35  mike
*  Replaced DataSourceInterface with abstract base class DataSourceExec.
*  The base class contains a link back to the Database DataSource object.
*
*/
package decodes.datasource;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import lrgs.common.SearchCriteria;

import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;

import decodes.db.DataSource;
import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.InvalidDatabaseException;
import decodes.db.NetworkList;
import decodes.db.TransportMedium;
import decodes.db.Platform;
import decodes.routing.RoutingSpecThread;
import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;


/**
  This is the base class for all DECODES data sources. It is 
  implemented by the database 'DataSource' object, which delegates
  to a real data source to read files, LRGS connections, directories,
  etc.
*/
public abstract class DataSourceExec
	implements PropertiesOwner
{
	/** The data source record from the DECODES database. */
	protected DataSource dbDataSource;

	/** 
	  If this is true, then the data source should not throw
	  UnknownPlatformException if it is not able to resolve a linkage
	  to transport medium, and platform. It should simply return the
	  RawMessage object with these items set to null.
	  Some clients may not care about the linkage and just want raw data.
	*/
	protected boolean allowNullPlatform;

	/**
	  Normally DAPS status messages are skipped by DECODES. To allow them
	  to be returned set allowDapsStatusMessages; to true.
	*/
	protected boolean allowDapsStatusMessages;
	
	protected RoutingSpecThread routingSpecThread = null;

	/** default constructor */
	protected DataSourceExec()
	{
		dbDataSource = null;
		allowNullPlatform = false;
		allowDapsStatusMessages = false;
	}

	public void setRoutingSpecThread(RoutingSpecThread rst)
	{
		routingSpecThread = rst;
	}
	
	public void log(int priority, String msg)
	{
		if (routingSpecThread == null)
			Logger.instance().log(priority, msg);
		else
			routingSpecThread.log(priority, msg);
	}

	
	/**
	  Sets the dbDataSource member.
	  @param ds the database data source record
	*/
	public void setDataSource(DataSource ds)
		throws InvalidDatabaseException
	{
		dbDataSource = ds;
		processDataSource();
	}
	
	/**
	 * @return the DataSource object from the database.
	 */
	public DataSource getDataSource() { return dbDataSource; }

	/** @return name of this data source, as defined in DECODES DB. */
	public String getName()
	{
		return dbDataSource.getName();
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
	  Called once prior to using this data source, the concrete sub-class 
	  should evaluate what information is
	  needed from of the data source object (name, argument, group
	  members, etc.) The base class (this) will store a reference to
	  the dbDataSource.
	  @throws InvalidDatabaseException if required information is 
	  missing or in an improper format.
	*/
	public abstract void processDataSource()
		throws InvalidDatabaseException;

	/**
	  Initializes the data source.
	  The name and argument of the data source were passed implicitly
	  by the 'setDataSource' call. This method is called by the routing
	  spec to initialize (or reinitialize) the data source for retrieving
	  messages.  The properties from the routing specification are
	  passed. The concrete data source uses all available information to make
	  connections, open files, etc.
	  <p>
	  @param routingSpecProps the routing spec properties
	  @param since the 'since' time from the routing spec
	  @param until the 'until' time from the routing spec
	  @param networkLists contains NetworkList objects
	  @throws DataSourceException if the source could not be initialized.
	*/
	public abstract void init(Properties routingSpecProps, String since, 
		String until, Vector<NetworkList> networkLists)
		throws DataSourceException;

	/**
	  Closes the data source.
	  This method is called by the routing specification when the data
	  source is no longer needed.
	*/
	public abstract void close();

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
	public abstract RawMessage getRawMessage()
		throws DataSourceException;

	/**
	  Find the matching transport medium for this platform.
	  This logic is a bit messy because GOES can have different TM
	  types (ST, RD, or simply 'GOES'), and because we want to support
	  the old logic of channels < 100 being self-timed.
	  <p>
	  The logic is implemented here in the base class so that it can
	  be used consistently for all data sources that return GOES messages.
	  If channel is not -1, this indicates GOES message. This function
	  should work for non-goes-data also.
	  <p>
	  @param p the platform resolved from the message.
	  @param tmid the transport medium ID (e.g. DCP address, Site ID)
	  @param chan the channel number from the message or -1 if undefined.
	  @param oldChannelRanges deprecated flag indicating channels < 100 are ST
	  @return TransportMedium matching the arguments.
	*/
	protected TransportMedium resolveTransportMedium(Platform p,
		String tmid, int chan, boolean oldChannelRanges)
	{
		for(TransportMedium tm : p.transportMedia) 
		{
  			// If this is a GOES msg, but not a GOES TM, skip it.
			if (chan != -1
			 && !(tm.getMediumType().equalsIgnoreCase(Constants.medium_GoesST)
			     || tm.getMediumType().equalsIgnoreCase(Constants.medium_GoesRD)
			     || tm.getMediumType().equalsIgnoreCase(Constants.medium_Goes)))
				continue;

			if (!tmid.equalsIgnoreCase(tm.getMediumId()))
				continue;

			/*
			  IF
				no channel to check,
			  	or channel matches,
			  	or using old ranges && this channel falls into ST or RD range,
			  THEN
				we have a match!
			*/
			if (chan == -1
			 || chan == tm.channelNum
		     || (oldChannelRanges 
				 && tm.getMediumType() == Constants.medium_GoesST 
				 && chan < 100)
		     || (oldChannelRanges 
				 && tm.getMediumType() == Constants.medium_GoesRD
				 && chan > 100))
			{
				return tm;
			}
		}
		return null;
	}

	/**
	  Sets the 'allow-null-platform' flag.
	  @param tf flag
	*/
	public void setAllowNullPlatform(boolean tf) 
	{
		allowNullPlatform = tf;
		Logger.instance().debug1(
			"DataSource '" + getName() + "' "
			+ " allowNullPlatform set to " + tf);
	}

	public boolean getAllowNullPlatform() { return allowNullPlatform; }

	/**
	  Sets the 'allow-daps-status-messages' flag.
	  @param tf flag
	*/
	public void setAllowDapsStatusMessages(boolean tf)
	{
		allowDapsStatusMessages = tf;
		Logger.instance().debug1(
			"DataSource '" + getName() + "' "
			+ (tf ? "" : "NOT ") + "Allowing DAPS Status Messages.");
	}

	/**
	  @return the 'allow-daps-status-messages' flag.
	*/
	public boolean getAllowDapsStatusMessages() 
	{
		return allowDapsStatusMessages;
	}

	/**
	 * Some data sources may hang or loop waiting for responses from servers,
	 * etc. This method tells the data source to stop what it's doing and
	 * abort the routing spec.
	 * The default implementation here does nothing.
	 */
	public void abortGetRawMessage()
	{
	}

	/**
	 * Base class returns an empty array for backward compatibility.
	 */
	@Override
	public PropertySpec[] getSupportedProps()
	{
		return new PropertySpec[0];
	}

	/**
	 * Base class return true for backward compatibility.
	 */
	@Override
	public boolean additionalPropsAllowed()
	{
		return true;
	}
}

