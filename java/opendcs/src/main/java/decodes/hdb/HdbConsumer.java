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
*/
package decodes.hdb;

import java.io.OutputStream;
import java.util.Iterator;
import java.util.Properties;

import org.opendcs.authentication.AuthSourceService;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import opendcs.dai.TimeSeriesDAI;
import ilex.util.AuthException;
import ilex.util.TextUtil;
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
import decodes.util.TSUtil;
import decodes.sql.DbKey;
import decodes.tsdb.BadConnectException;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;

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
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
		Properties credentials = null;
		String authFileName = DecodesSettings.instance().DbAuthFile;
		try
		{
			credentials = AuthSourceService.getFromString(authFileName)
											 .getCredentials();
		}
		catch(AuthException ex)
		{
			String msg = "Cannot read DB auth from settings '"
				+ authFileName+ "': ";
			throw new DataConsumerException(msg, ex);
		}

		// Get the Oracle Data Source & open a connection.
		try
		{
			hdbTsDb = new HdbTimeSeriesDb();
			hdbTsDb.connect("decodes", credentials);
			log.info("Connected to HDB Time Series Database as user {}", credentials.getProperty("username"));
			autoCreateTs = TextUtil.str2boolean(hdbTsDb.getProperty("autoCreateTs"));
			timeSeriesDAO = hdbTsDb.makeTimeSeriesDAO();
		}
		catch (BadConnectException ex)
		{
			String msg = "Cannot connect to HDB Time Series DB";
			throw new DataConsumerException(msg, ex);
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
			log.atWarn().setCause(ex).log("Skipping HDB ingest for data from unknown platform");
			return;
		}
		Site platformSite = platform.getSite();
		if (platformSite == null)
		{
			log.warn("Skipping HDB ingest for data from unknown site, DCP Address={}", mediumId);
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
					log.atWarn()
					   .setCause(ex)
					   .log("Skipping sensor {} '{}'", sensor.getNumber(), sensor.getName());
					continue;
				}

				CTimeSeries cts = TSUtil.convert2CTimeSeries(
					ts,                    // the DECODES Time Series
					hdbTsId.getSdi(),      // unique Time Series key
					hdbTsId.getPart(HdbTsId.TABSEL_PART), // "R_" or "M_"
					hdbTsId.getInterval(),
					true,                  // mustWrite flag (we want to write all values in the TS
					DbKey.NullKey);        // sourceId not used in HDB
				cts.setTimeSeriesIdentifier(hdbTsId);

				try
				{
					timeSeriesDAO.saveTimeSeries(cts);
				}
				catch (BadTimeSeriesException ex)
				{
					log.atError()
					   .setCause(ex)
					   .log("Cannot save time series for '{}'", hdbTsId.getUniqueString());
				}
			}
		}
		catch(DbIoException ex)
		{
			String emsg = "Error writing message.";
			throw new DataConsumerException(emsg, ex);
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
			log.warn("Sensor {} Cannot find HDB datatype or equivalent -- skipping.", sensor.getName());
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
		log.info("Looking up TSID for '{}'", uniqueStr);
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
				log.atWarn()
				   .setCause(ex2)
				   .log("Time Series for '{}' does not exist and cannot be created.", uniqueStr);
				throw ex2;
			}
			catch(DbIoException ex2)
			{
				log.atWarn().setCause(ex2).log("Error creating Time Series for '{}'", uniqueStr);
				throw ex2;
			}
			catch (BadTimeSeriesException ex2)
			{
				log.atWarn()
				   .setCause(ex2)
				   .log("Bad Time Series for '{}' -- does not exist and cannot be created: ", uniqueStr);
				new NoSuchObjectException("Bad Time Series '" + uniqueStr + "'", ex2);
			}
			dbtsid = tsid;
		}
		catch(DbIoException ex)
		{
			log.atError().setCause(ex).log("Error looking up Time Series for '{}'", uniqueStr);
			throw ex;
		}
		return dbtsid;
	}
}
