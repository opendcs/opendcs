/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*
* The rate limiting (in getRawMessage and RequestDelay) is derived from:
* https://stackoverflow.com/a/1407228 
* and 
* https://krishnaprasadas.blogspot.com/2012/05/throttling-algorithm.html
*/
package decodes.datasource;

import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.DataSource;
import decodes.db.Database;
import decodes.db.Constants;
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
public abstract class DataSourceExec implements PropertiesOwner
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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

	protected Database db = null;


	protected int requestRateLimit = -1; // Rate limit of requests per minute

	private final DelayQueue<RequestDelay> rateQueue = new DelayQueue<>();

	/**
	 * Required constructor for any data source.
	 *
	 * If you have a DataSource that can be used without
	 * a constructor it is okay to call @code{super(null,null)}
	 * as this base class does not call them.
	 *
	 * However a constructor that takes as passes the variables
	 * to this constructor should be provided as the makeExecutive
	 * function that creates sources needs to call the highest level
	 * constructor that includes them.
	 *
	 * @param dataSource The DataSource that defines the parameters for this DataSourceExecutive
	 * @param decodesDatabase The Decodes database interface.
	 *
	 * @since 7.0.9 perviously this was a default no args constructor. Update your local implementations and avoid
	 * access to Database.getDb() as we will be removing it.
	 */
	protected DataSourceExec(DataSource dataSource, Database decodesDatabase)
	{
		dbDataSource = dataSource;
		this.db=decodesDatabase;
		allowNullPlatform = false;
		allowDapsStatusMessages = false;
	}

	public void setRoutingSpecThread(RoutingSpecThread rst)
	{
		routingSpecThread = rst;
	}
	
	@Deprecated
	public void log(int priority, String msg)
	{
		log.atInfo().addKeyValue("originalPriority", priority).log(msg);
	}

	
	/**
	  Sets the dbDataSource member.
	  @param ds the database data source record
	  @deprecated this not be called directly, the DataSource is now passed in the constructor.
	*/
	@Deprecated
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
		return dbDataSource == null ? "anon" : dbDataSource.getName();
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
	 * Retrieve raw message from the source, apply rate limiting if configured to do so
	 * @return the next RawMessage object from the data source, or null if none currently available.
	 *
	 * @throws DataSourceTimeoutException if the data source is still
	 * waiting for a message and the timeout (as defined in the properties
	 * when init was called) has expired.
	 * @throws DataSourceException if some other problem arises.
	 */
	public RawMessage getRawMessage() throws DataSourceException
	{
		if (requestRateLimit > 0 && rateQueue.size() == 0)
		{
			for (int i = 0; i < requestRateLimit; i++)
			{
				rateQueue.add(new RequestDelay(0, TimeUnit.SECONDS));
			}
		}

		if (requestRateLimit == -1) // no limiting just return
		{
			log.info("Not rate limited request.");
			return getSourceRawMessage();
		}

		try
		{
			log.info("Supposedly rate limited request.");
			RequestDelay take = rateQueue.take();
			log.info("Delay was {}, inserted was {}, current time millis is {}, getDelay {}", take.delay, take.inserted, System.currentTimeMillis(), take.getDelay(TimeUnit.MILLISECONDS));
			RequestDelay newDelay = new RequestDelay(1, TimeUnit.MINUTES);
			rateQueue.add(newDelay);
			log.info("Delay was {}, inserted was {}, current time millis is {}, getDelay {}, rateQueue size = {}", newDelay.delay, newDelay.inserted, System.currentTimeMillis(), newDelay.getDelay(TimeUnit.MILLISECONDS), rateQueue.size());

		}
		catch (InterruptedException ex)
		{
			log.atWarn().setCause(ex).log("Interrupted waiting for delay value.");
		}

		return getSourceRawMessage();
	}

	/**
	  Reads the next raw message from the data source and returns it.
	  This DataSource will fill in the message data and attempt to 
	  associate it with a TransportMedium object.

	  @return the next RawMessage object from the data source, or null if none currently available.

	  @throws DataSourceTimeoutException if the data source is still
	  waiting for a message and the timeout (as defined in the properties
	  when init was called) has expired.
	  @throws DataSourceException if some other problem arises.
	*/
	protected abstract RawMessage getSourceRawMessage()
		throws DataSourceException;

	/**
	  Find the matching transport medium for this platform.
	  This logic is a bit messy because GOES can have different TM
	  types (ST, RD, or simply 'GOES'), and because we want to support
	  the old logic of channels &lt; 100 being self-timed.
	  <p>
	  The logic is implemented here in the base class so that it can
	  be used consistently for all data sources that return GOES messages.
	  If channel is not -1, this indicates GOES message. This function
	  should work for non-goes-data also.
	  <p>
	  @param p the platform resolved from the message.
	  @param tmid the transport medium ID (e.g. DCP address, Site ID)
	  @param chan the channel number from the message or -1 if undefined.
	  @param oldChannelRanges deprecated flag indicating channels &lt; 100 are ST
	  @return TransportMedium matching the arguments.
	*/
	protected TransportMedium resolveTransportMedium(Platform p,
		String tmid, int chan, boolean oldChannelRanges)
	{
		for(TransportMedium tm : p.transportMedia) 
		{
			if (!tmid.equalsIgnoreCase(tm.getMediumId()))
				continue;

			// If this is a GOES msg, but not a GOES TM, skip it.
			if (chan != -1) // means GOES
			{
				if (!(tm.getMediumType().equalsIgnoreCase(Constants.medium_GoesST)
			       || tm.getMediumType().equalsIgnoreCase(Constants.medium_GoesRD)
			       || tm.getMediumType().equalsIgnoreCase(Constants.medium_Goes)))
				continue;
			}
			else
			{
				// For non-goes, if a medium type is specified in the data source
				// it needs to match the one in the TM.
				String mt = getMediumType();
				if (mt != null && !mt.equalsIgnoreCase(tm.getMediumType()))
					continue;
			}
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
		log.debug("DataSource '{}' allowNullPlatform set to {}", getName(), tf);
	}

	public boolean getAllowNullPlatform() { return allowNullPlatform; }

	/**
	  Sets the 'allow-daps-status-messages' flag.
	  @param tf flag
	*/
	public void setAllowDapsStatusMessages(boolean tf)
	{
		allowDapsStatusMessages = tf;
		log.debug("DataSource '{}' {} Allowing DAPS Status Messages.", getName(), (tf ? "" : "NOT "));
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

	public RoutingSpecThread getRoutingSpecThread()
	{
		return routingSpecThread;
	}
	
	public String getMediumType()
	{
		return null;
	}
	
	public boolean supportsTimeRanges()
	{
		return false;
	}

	private static class RequestDelay implements Delayed
	{

		private final long delay;
		private final long inserted;
		private final TimeUnit delayUnit; // always MILLISECONDS

		public RequestDelay(long providedDelay, TimeUnit providedDelayUnit)
		{
			this.delayUnit = TimeUnit.MILLISECONDS;
			this.delay = this.delayUnit.convert(providedDelay, providedDelayUnit);
			this.inserted = System.currentTimeMillis();
		}

		@Override
		public int compareTo(Delayed o)
		{
			return Long.compare(getDelay(delayUnit), o.getDelay(delayUnit));
		}

		@Override
		public long getDelay(TimeUnit unit)
		{
			return unit.convert((inserted - System.currentTimeMillis()) + delay, delayUnit);
		}
		
	}
}
