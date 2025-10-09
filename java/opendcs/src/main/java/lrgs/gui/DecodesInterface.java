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
package lrgs.gui;

import java.io.File;
import java.util.Iterator;
import java.util.Properties;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.EnvExpander;
import ilex.util.TextUtil;
import decodes.datasource.LrgsDataSource;
import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.db.Database;
import decodes.db.DatabaseIO;
import decodes.db.DbEnum;
import decodes.db.DecodesScript;
import decodes.db.EnumValue;
import decodes.db.InvalidDatabaseException;
import decodes.db.Platform;
import decodes.db.PresentationGroup;
import decodes.db.PresentationGroupList;
import decodes.db.Site;
import decodes.db.TransportMedium;
import decodes.consumer.OutputFormatter;
import decodes.consumer.StringBufferConsumer;
import decodes.decoder.DecodedMessage;
import decodes.util.ChannelMap;
import decodes.util.DecodesException;
import decodes.util.DecodesSettings;
import decodes.util.NwsXref;
import decodes.util.Pdt;
import decodes.util.ResourceFactory;
import lrgs.common.DcpMsg;


/**
This class provides an interface to DECODES for the LRGS Msg Browser GUI.
<p>
The purpose is to encapsulate all references to DECODES classes here.
That way, the browser can still run on systems where DECODES is not installed
and the DECODES classes are not available.
*/
public class DecodesInterface
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	static boolean decodesInitialized = false;
	static boolean initializedForDecoding = false;
	static boolean initializedForEditing = false;
	public static boolean silent = false;
	private static boolean isGUI = false;
	// No setter, used by IntegrationTests which will use reflection.
	// More work required to find a better way.
	private static boolean preventDecodesShutdown = false;

	LrgsDataSource dataSource;
	java.util.TimeZone timeZone;
	PresentationGroup presGrp;
	OutputFormatter formatter;
	StringBufferConsumer consumer;
	StringBuffer decodeBuf;

	/**
	   Call initDecodes before construction.
	   This is currently only used by the Message Browser, usage else where
	   is discouraged.
	*/
	DecodesInterface(Database db)
	{
		dataSource = new LrgsDataSource(null,db);
		decodeBuf = new StringBuffer();
		consumer = new StringBufferConsumer(decodeBuf);
		timeZone = null;
	}

	//===========================================================
	// Static Methods
	//===========================================================


	/**
	  Initialize interface to the DECODES database.

	  This method does the minimal initialization that will be neede by
	  most (all?) applications. After calling this method you probably
	  also want to call initializeForDecoding() or initializeForEditing().

	  <p>
	  NOTE: If DECODES is not installed, a NoClassDefFoundError will be
	  thrown when you attempt to call this method.

	  @return true if success, false if failure,
	*/
	public static void initDecodes(String propFile)
			throws DecodesException
	{
		if (decodesInitialized)
			return;

		initDecodesMinimal(propFile);
		Database db = Database.getDb();

		// Initialize minimal collections:

		log.trace("Enum, ");
		db.enumList.read();

		log.trace("DataType, ");
		db.dataTypeSet.read();

		log.trace("Sources, ");

		db.dataSourceList.read();

		decodesInitialized = true;

	}

	/**
	 * Connects to DECODES database, initializes the singleton and the DBIO
	 * class, but does not read any of the lists.
	 * @param propFile name of DECODES properties file
	 * @throws DecodesException if any connect-error occurs.
	 */
	public static void initDecodesMinimal(String propFile)
		throws DecodesException
	{
		if (decodesInitialized)
			return;

		ResourceFactory.instance();
		DecodesSettings settings = DecodesSettings.instance();

		log.trace("Init DECODES DB: ");

		// Construct database and the interface specified by properties.
		Database db = Database.getDb();
		if (db == null)
		{
			db = new Database();
			Database.setDb(db);
		}

		DatabaseIO dbio;
		dbio = DatabaseIO.makeDatabaseIO(settings.editDatabaseTypeCode,
			settings.editDatabaseLocation);
		db.setDbIo(dbio);

		log.trace("EU, ");

		db.engineeringUnitList.read();
	}

	private static boolean siteListRead = false;

	/**
	  Initializes the database instance appropriately for applications that
	  intend to edit the database.
	*/
	public static void initializeForEditing()
		throws DecodesException
	{
		if (!decodesInitialized)
			throw new DecodesException("Must initialize DECODES before calling "
				+ "initializeForEditing()");
		if (initializedForEditing)
			return;

		Database db = Database.getDb();

		log.trace("Site, ");
		Site.explicitList = true;
		db.siteList.read();
		siteListRead = true;

		log.trace("Equip, ");
		db.equipmentModelList.read();

		log.trace("Config, ");
		db.platformConfigList.read();
		if (!initializedForDecoding)
			initializeForDecoding();

		db.platformConfigList.countPlatformsUsing();

		log.trace("Routing, ");
		db.routingSpecList.read();

		initializedForEditing = true;
	}


	/**
	  Initializes the database instance appropriately for applications that
	  intend to use the database to decode a stream of data.
	*/
	public static void initializeForDecoding()
		throws DecodesException
	{
		if (!decodesInitialized)
			throw new DecodesException("Must initialize DECODES before calling "
				+ "initializeForDecoding()");
		if (initializedForDecoding)
			return;
		Database db = Database.getDb();
		readSiteList();
		log.trace("Platform Configs, ");
		db.platformConfigList.read();
		log.trace("Platforms, ");
		db.platformList.read();
		log.trace("Presentation Groups, ");
		db.presentationGroupList.read();
		log.trace("Network Lists, ");
		db.networkListList.read();
		initializedForDecoding = true;
	}

	public static void readSiteList()
		throws DecodesException
	{
		if (!siteListRead)
		{
			log.trace("Site, ");
			Database db = Database.getDb();
			db.siteList.read();
			siteListRead = true;
		}
	}

	/**
	  @returns a String array of presentation group names.
	*/
	public static String[] getPresentationGroups()
	{
		PresentationGroupList pgl = Database.getDb().getPresentationGroupList();
		String ret[] = new String[pgl.size()];
		int i=0;
		for(Iterator<PresentationGroup> it = pgl.iterator(); it.hasNext(); )
		{
			PresentationGroup pg = it.next();
			ret[i++] = pg.groupName;
		}
		return ret;
	}

	/**
	  @returns a String array of known output formats.
	*/
	public static String[] getOutputFormats()
	{
		DbEnum denum = Database.getDb().getDbEnum("OutputFormat");
		if (denum == null)
			return null;
		String ret[] = new String[denum.values().size()];
		int i=0;
		for(Iterator<EnumValue> it = denum.iterator(); it.hasNext(); )
		{
			EnumValue env = it.next();
			ret[i++] = env.getValue();
		}
		return ret;
	}

	//===========================================================
	// Instance Methods
	//===========================================================

	/**
	  Sets the presentation group to be used.
	  @param name name of the PG.
	*/
	public void setPresentation(String name)
	{
		if (presGrp == null
		 || !presGrp.groupName.equalsIgnoreCase(name))
		{
			presGrp = null;
			if (!TextUtil.isAllWhitespace(name))
				presGrp = Database.getDb().presentationGroupList.find(name);
			if (presGrp != null && !presGrp.isPrepared())
			{
				try { presGrp.prepareForExec(); }
				catch(InvalidDatabaseException ex)
				{
					log.atError().setCause(ex).log("Cannot prepare presentation group '{}'", name);
				}
			}
		}
	}

	/**
	  Sets the timezone to be used.
	  @param tzname the timezone name (one of the many understood by the
	  java.util.TimeZone class.
	*/
	public void setTimeZone(String tzname)
		throws Exception
	{
		if (tzname == null || TextUtil.isAllWhitespace(tzname))
			timeZone = java.util.TimeZone.getTimeZone("UTC");
		else
			timeZone = java.util.TimeZone.getTimeZone(tzname);
	}

	/**
	  Sets and configures the DECODES output formatter to be used.
	  @param name name of output formatter
	  @param props properties passed to the formatter for initialization
	*/
	public void setFormatter(String name, Properties props)
		throws Exception
	{
		formatter = OutputFormatter.makeOutputFormatter(name, timeZone, presGrp,
				props, null);
	}

	/**
	 * @return the output formatter currently in use.
	 */
	public OutputFormatter getFormatter() { return formatter; }

	/**
	  Decodes the message according to pre-set formatter, timezone, and
	  presentation group. Returns a long string containing the formatted,
	  decoded data.
	  @param msg the raw DCP Message data
	  @return String decoded formatted message data
	*/
	public String decodeMessage(DcpMsg msg)
		throws Exception
	{
//TODO handle case where formatter does the decoding, like snotel.
// In that case there won't be a platform, tm, or script. DecodedMessage is just a wrapper.
		dataSource.setAllowNullPlatform(!formatter.requiresDecodedMessage());
		RawMessage rm = dataSource.lrgsMsg2RawMessage(msg);
		DecodedMessage dm = null;
		try
		{
			Platform p = rm.getPlatform();
			TransportMedium tm = rm.getTransportMedium();
			if (!p.isPrepared())
				p.prepareForExec();
			if (!tm.isPrepared())
				tm.prepareForExec();

			// Get decodes script & use it to decode message.
			DecodesScript ds = tm.getDecodesScript();
			if (ds == null)
				throw new Exception(
					"Transport medium does not have a DecodesScript");

			dm = ds.decodeMessage(rm);
			//Calculates the elevation using the preoffset, scale and offset
			dm.applyScaleAndOffset();

			// Use presentation group to convert units & format values
			if (presGrp != null)
				dm.formatSamples(presGrp);
		}
		catch(UnknownPlatformException ex)
		{
			if (formatter.requiresDecodedMessage())
				throw ex;
			dm = new DecodedMessage(rm, false);
		}

		// Use the formatter & consumer to output the message.
		decodeBuf.delete(0, decodeBuf.length());
		formatter.formatMessage(dm, consumer);

		return decodeBuf.toString();
	}

	public static boolean isInitialized() { return decodesInitialized; }

	public static boolean isGUI()
	{
		return isGUI;
	}

	public static void setGUI(boolean isGUI)
	{
		DecodesInterface.isGUI = isGUI;
	}

	public static void maintainGoesPdt()
	{
		DecodesSettings settings = DecodesSettings.instance();
		if (settings.pdtLocalFile != null && settings.pdtLocalFile.trim().length() > 0)
		{
			// Get PDT singleton instance.
			Pdt pdt = Pdt.instance();
			Pdt.downloadIntervalMsec = 24 * 3600 * 1000L; // 24 hrs
			Pdt.useLockForDownload = true;
			// Note thread will not do download if url is null, blank or "-",
			// so this is safe to do.
			pdt.startMaintenanceThread(settings.pdtUrl, settings.pdtLocalFile);
		}
		if (settings.cdtLocalFile != null && settings.cdtLocalFile.trim().length() > 0)
		{
			// Get CDT singleton instance.
			ChannelMap cdt = ChannelMap.instance();
			Pdt.downloadIntervalMsec = 24 * 3600 * 1000L; // 24 hrs
			// Note thread will not do download if url is null, blank or "-",
			// so this is safe to do.
			cdt.startMaintenanceThread(settings.cdtUrl, settings.cdtLocalFile);
		}
		if (settings.nwsXrefLocalFile != null && settings.nwsXrefLocalFile.trim().length() > 0)
		{
			// Get NwsXref singleton instance.
			NwsXref xref = NwsXref.instance();
			Pdt.downloadIntervalMsec = 24 * 3600 * 1000L; // 24 hrs
			Pdt.useLockForDownload = true;
			// Note thread will not do download if url is null, blank or "-",
			// so this is safe to do.
			xref.startMaintenanceThread(settings.nwsXrefUrl, settings.nwsXrefLocalFile);
		}
	}

	/**
	 * Called by daemons when they detect that the database has gone down.
	 * This method closes database connections and discards the cached database
	 * objects so that a clean restart can be attempted later.
	 */
	public static void shutdownDecodes()
	{
		if (!preventDecodesShutdown)
		{
			log.info("Shutting down Decodes Connection.");
			Database db = Database.getDb();
			if (db != null)
			{
				DatabaseIO dbio = db.getDbIo();
				if (dbio != null)
				{
					dbio.close();
				}
				db.setDbIo(null);
			}
			decodesInitialized = initializedForDecoding = initializedForEditing = false;
		}
	}
}