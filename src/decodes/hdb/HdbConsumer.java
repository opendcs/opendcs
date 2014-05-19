/*
 *  $Id$
 *
 *  This is open-source software written by ILEX Engineering, Inc., under
 *  contract to the federal government. You are free to copy and use this
 *  source code for your own purposes, except that no part of this source
 *  code may be claimed to be proprietary.
 *
 *  Except for specific contractual terms between ILEX and the federal 
 *  government, this source code is provided completely without warranty.
 *  For more information contact: info@ilexeng.com
 *
 *  $Log$
 *  Revision 1.8  2013/03/21 18:27:40  mmaloney
 *  DbKey Implementation
 *
 *  Revision 1.7  2012/09/06 23:40:12  mmaloney
 *  Skip sensor cleanly if no time series and can't create.
 *
 *  Revision 1.6  2012/07/25 15:47:04  mmaloney
 *  Major rewrite. Use HdbTsDb methods rather than duplicating all the code
 *  here for writing to HDB. This follows the model of the Cwms Consumer.
 *
 *  Revision 1.5  2012/06/06 19:04:37  mmaloney
 *  dev
 *
 *  Revision 1.4  2012/06/06 18:54:19  mmaloney
 *  dev
 *
 *  Revision 1.3  2012/06/06 18:42:36  mmaloney
 *  Added autoCreateTs feature.
 *
 *  Revision 1.2  2008/11/29 21:07:57  mjmaloney
 *  merge with opensrc
 *
 *  Revision 1.1  2008/11/15 01:04:00  mmaloney
 *  Moved from separate trees to common parent
 *
 *  Revision 1.8  2007/12/11 01:05:18  mmaloney
 *  javadoc cleanup
 *
 *  Revision 1.7  2007/08/30 21:04:44  mmaloney
 *  dev
 *
 *  Revision 1.6  2006/05/24 19:05:04  mmaloney
 *  dev
 *
 *  Revision 1.5  2006/05/22 14:05:39  mmaloney
 *  dev
 *
 *  Revision 1.4  2006/05/11 13:32:33  mmaloney
 *  DataTypes are now immutable! Modified all references. Modified SQL IO code.
 *
 *  Revision 1.3  2006/04/04 13:09:52  mmaloney
 *  completed.
 *
 *  Revision 1.1  2005/06/22 15:44:47  mjmaloney
 *  created.
 *
 */
package decodes.hdb;

import java.io.OutputStream;
import java.util.Iterator;
import java.util.Properties;

import opendcs.dai.TimeSeriesDAI;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.util.UserAuthFile;

import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.decoder.DecodedMessage;
import decodes.decoder.Sensor;
import decodes.decoder.TimeSeries;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.Platform;
import decodes.db.Site;
import decodes.db.TransportMedium;
import decodes.consumer.DataConsumer;
import decodes.consumer.DataConsumerException;
import decodes.util.DecodesSettings;
import decodes.tsdb.BadConnectException;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesHelper;

/**
 * HdbConsumer writes data to the USBR Hydrologic Database.
 * 
 * <p>
 * Properties used by HdbConsumer include:
 * </p>
 * <ul>
 * <li>list property names, meaning, & default values here.</li>
 * </ul>
 */
public class HdbConsumer extends DataConsumer
{
	private String module = "HdbConsumer";
	
	/** Connection to the database */
	private HdbTimeSeriesDb hdbTsDb = null;
	
	private String mediumId = null; 
	private boolean autoCreateTs = false;
	private TimeSeriesDAI timeSeriesDAO = null;
	
	/** default constructor */
	public HdbConsumer()
	{
		super();
	}

	/**
	 * Opens and initializes the consumer. This method is called once, at the
	 * start of the routing spec.
	 * 
	 * @param consumerArg
	 *            file name template.
	 * @param props
	 *            routing spec properties.
	 * @throws DataConsumerException
	 *             if the consumer could not be initialized.
	 */
	public void open(String consumerArg, Properties props)
		throws DataConsumerException
	{
		// Get username & password from Auth file
		Properties credentials = new Properties();
		String authFileName = 
			EnvExpander.expand(DecodesSettings.instance().DbAuthFile);
		UserAuthFile authFile = null;
		try 
		{
			authFile = new UserAuthFile(authFileName);
			authFile.read();
			credentials.setProperty("username", authFile.getUsername());
			credentials.setProperty("password", authFile.getPassword());
		}
		catch(Exception ex)
		{
			String msg = "Cannot read DB auth from file '" 
				+ authFileName+ "': " + ex;
			warning(msg);
		}
		
		// Get the Oracle Data Source & open a connection.
		try
		{
			hdbTsDb = new HdbTimeSeriesDb();
			hdbTsDb.connect("decodes", credentials);
			info("Connected to HDB Time Series Database as user " 
				+ authFile.getUsername());
			autoCreateTs = TextUtil.str2boolean(hdbTsDb.getProperty("autoCreateTs"));
			timeSeriesDAO = hdbTsDb.makeTimeSeriesDAO();
		}
		catch (BadConnectException ex)
		{
			String msg = "Cannot connect to HDB Time Series DB: " + ex;
			failure(msg);
			throw new DataConsumerException(msg);
		}
	}

	/**
	 * Closes the data consumer. This method is called by the routing
	 * specification when the data consumer is no longer needed.
	 */
	public void close()
	{
		hdbTsDb.closeConnection();
		hdbTsDb = null;
		if (timeSeriesDAO != null)
			timeSeriesDAO.close();
	}

	/**
	 * This method called at the beginning of each decoded message. We do all
	 * the IO work here: the println method does nothing. Use a NullFormatter
	 * when using HdbConsumer.
	 * 
	 * @param msg
	 *            The message to be written.
	 * @throws DataConsumerException
	 *             if an error occurs.
	 */
	public void startMessage(DecodedMessage msg) 
		throws DataConsumerException
	{
		Platform platform;

		try
		{
			RawMessage rawmsg = msg.getRawMessage();
			TransportMedium tm = rawmsg.getTransportMedium();
			platform = rawmsg.getPlatform();
			mediumId = tm.getMediumId();
		}
		catch (UnknownPlatformException ex)
		{
			Logger.instance().warning(
				"Skipping HDB ingest for data from " + "unknown platform: "
					+ ex);
			return;
		}
		Site platformSite = platform.getSite();
		if (platformSite == null)
		{
			warning(
				"Skipping HDB ingest for data from "
				+ "unknown site, DCP Address=" + mediumId);
			return;
		}

		TimeSeriesDAI timeSeriesDAO = hdbTsDb.makeTimeSeriesDAO();
		try
		{
			for (Iterator<TimeSeries> it = msg.getAllTimeSeries(); it.hasNext();)
			{
				TimeSeries ts = it.next();
	
				// Only process time series that have data.
				if (ts.size() == 0)
					continue;
	
				Sensor sensor = ts.getSensor();
				
				HdbTsId hdbTsId = null;
				
				try { hdbTsId = resolveTsId(sensor, platform); }
				catch (NoSuchObjectException ex)
				{
					warning("Skipping sensor " + sensor.getNumber() + " '"
						+ sensor.getName() + "': " + ex);
					continue;
				}
				
				CTimeSeries cts = TimeSeriesHelper.convert2CTimeSeries(
					ts,                    // the DECODES Time Series
					hdbTsId.getSdi(),      // unique Time Series key
					hdbTsId.getPart(HdbTsId.TABSEL_PART), // "R_" or "M_"
					hdbTsId.getInterval(), 
					true,                  // mustWrite flag (we want to write all values in the TS
					Constants.undefinedIntKey);// sourceId not used in HDB
				cts.setTimeSeriesIdentifier(hdbTsId);
	
				try
				{
					timeSeriesDAO.saveTimeSeries(cts);
				}
				catch (BadTimeSeriesException ex)
				{
					Logger.instance().failure(module + " Cannot save time series for '"
						+ hdbTsId.getUniqueString() + "': " + ex);
				}
			}
		}
		catch(DbIoException ex)
		{
			String emsg = "Error writing message: " + ex;
			failure(emsg);
			throw new DataConsumerException(emsg);
		}
		finally
		{
			timeSeriesDAO.close();
		}
	}

	/*
	 * Does nothing.
	 * 
	 * @param line the line to be written.
	 */
	public void println(String line)
	{
	}

	/**
	 * Called when a message is complete. Do nothing.
	 */
	public void endMessage()
	{
	}

	/**
	 * For status gathering, this method returns some symbolic name about the
	 * consumer. For a file consumer this is the file name.
	 * 
	 * @return symbolic output name
	 */
	public String getActiveOutput()
	{
		return "hdb";
	}

	/**
	 * Returns null, this consumer cannot do streaming output.
	 */
	public OutputStream getOutputStream()
	{
		return null;
	}

	/**
	 * Given a sensor with its meta-data and properties, construct and lookup
	 * an HdbTsId object in the time-series database. If the autoCreateTs property
	 * is set in the TSDB_PROPERTY table then attempt to create the time-series
	 * if it doesn't exist.
	 * @param sensor The DECODES Sensor object
	 * @return the HdbTsId read from, or created in, the database
	 * @throws DbIoException if SQL exception
	 * @throws NoSuchObjectException If HdbTsId doesn't exist and autoCreateTs=false
	 */
	private HdbTsId resolveTsId(Sensor sensor, Platform platform)
		throws DbIoException, NoSuchObjectException
	{
		HdbTsId tsid = new HdbTsId();

		// First look for a data type with standard "hdb".
		// If none, go through all datatypes and try to find an equivalent.
		DataType hdbDt = sensor.getDataType(Constants.datatype_HDB);
		if (hdbDt == null)
			for (Iterator<DataType> dtit = sensor.getAllDataTypes(); 
				dtit.hasNext();)
			{
				DataType tdt = dtit.next();
				if ((hdbDt = tdt.findEquivalent(Constants.datatype_HDB)) != null)
					break;
			}
		if (hdbDt == null)
		{
			warning("Sensor " + sensor.getName()
				+ " Cannot find HDB datatype or equivalent -- skipping.");
			throw new NoSuchObjectException("No Data Type");
		}
		tsid.setDataType(hdbDt);

		Site site = sensor.getSensorSite();
		if (site == null)
			site = platform.getSite();
		tsid.setSite(site);
		// There has to be an HDB name or we wouldn't get here.
		tsid.setSiteName(site.getName("hdb").getNameValue());

		// Assume that the interval is 'instant' unless there
		// is a sensor property that says otherwise.
		String interval = sensor.getProperty("interval");
		if (interval == null)
			interval = "instant";
		tsid.setInterval(interval);
		
		// Assume real data unless there's a property "modeled=true"
		String tabsel = "R_"; 
		if (TextUtil.str2boolean(sensor.getProperty("modeled")))
			tabsel = "M_";
		tsid.setTableSelector(tabsel);
		
		// If there is a property "modelID", get it's value.
		String modelIdS = sensor.getProperty("modelId");
		if (modelIdS != null)
			tsid.setPart(HdbTsId.MODELID_PART, modelIdS.trim());
		
		String uniqueStr = tsid.getUniqueString();
		info("Looking up TSID for '" + uniqueStr + "'");
		HdbTsId dbtsid = null;
		try
		{
			dbtsid = (HdbTsId)timeSeriesDAO.getTimeSeriesIdentifier(uniqueStr);
		}
		catch(NoSuchObjectException ex)
		{
			if (!autoCreateTs)
				throw ex;
			try { timeSeriesDAO.createTimeSeries(tsid); }
			catch(NoSuchObjectException ex2)
			{
				warning("Time Series for '" + uniqueStr 
					+ "' does not exist and cannot be created: " + ex2);
				throw ex2; 
			}
			catch(DbIoException ex2)
			{
				warning("Error creating Time Series for '" + uniqueStr 
					+ "': " + ex2);
				throw ex2;
			}
			catch (BadTimeSeriesException ex2)
			{
				warning("Bad Time Series for '" + uniqueStr 
					+ "' -- does not exist and cannot be created: " + ex2);
				new NoSuchObjectException("Bad Time Series '" + uniqueStr + "'"); 
			}
			dbtsid = tsid;
		}
		catch(DbIoException ex)
		{
			warning("Error looking up Time Series for '" + uniqueStr 
				+ "': " + ex);
			throw ex;
		}
		return dbtsid;
	}

	private void failure(String msg)
	{
		String s = module + " " + (mediumId == null ? "" : mediumId+" ") + msg;
		Logger.instance().failure(s);
	}
	private void warning(String msg)
	{
		String s = module + " " + (mediumId == null ? "" : mediumId+" ") + msg;
		Logger.instance().warning(s);
	}
	private void info(String msg)
	{
		String s = module + " " + (mediumId == null ? "" : mediumId+" ") + msg;
		Logger.instance().info(s);
	}
}
