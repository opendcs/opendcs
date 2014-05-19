/*
*  $Id$
*  
*  $Log$
*  Revision 1.14  2013/03/28 17:29:09  mmaloney
*  Refactoring for user-customizable decodes properties.
*
*  Revision 1.13  2013/02/28 16:12:52  mmaloney
*  added maintainGoesPdt method.
*
*  Revision 1.12  2011/12/16 20:21:50  mmaloney
*  Added GUI flag.
*
*  Revision 1.11  2011/02/03 20:00:23  mmaloney
*  Time Series Group Editor Mods
*
*  Revision 1.10  2010/10/22 18:03:09  mmaloney
*  formatting
*
*/
package lrgs.gui;

import java.util.Iterator;
import java.util.Properties;
import java.io.IOException;
import java.io.FileInputStream;

import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.util.EnvExpander;

import decodes.db.*;
import decodes.util.*;
import decodes.datasource.LrgsDataSource;
import decodes.datasource.RawMessage;
import decodes.consumer.OutputFormatter;
import decodes.consumer.StringBufferConsumer;
import decodes.decoder.DecodedMessage;

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
	static boolean decodesInitialized = false;
	static boolean initializedForDecoding = false;
	static boolean initializedForEditing = false;
	public static boolean silent = false;
	private static boolean isGUI = false;

	LrgsDataSource dataSource;
	java.util.TimeZone timeZone;
	PresentationGroup presGrp;
	OutputFormatter formatter;
	StringBufferConsumer consumer;
	StringBuffer decodeBuf;

	/** 
	  default constructor.
	  Call initDecodes after construction.
	*/
	DecodesInterface()
	{
		dataSource = new LrgsDataSource();
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
		if (!silent)
		{
			System.out.print("Enum, ");
			System.out.flush();
		}
		db.enumList.read();
		ResourceFactory.instance().addBuiltInEnums();

		if (!silent)
		{
			System.out.print("DataType, ");
			System.out.flush();
		}
		db.dataTypeSet.read();
		// System.out.print("EU, "); System.out.flush();
		// db.engineeringUnitList.read();
		if (!silent)
		{
			System.out.print("Sources, ");
			System.out.flush();
		}
		db.dataSourceList.read();

		decodesInitialized = true;
		if (!silent)
			System.out.println();
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

		Logger.instance().log(Logger.E_DEBUG1,
			"Initializing DECODES database from prop file '"
			+ propFile + "'.");

		if (propFile == null)
			propFile = EnvExpander.expand(
				"$DECODES_INSTALL_DIR/decodes.properties");

		DecodesSettings settings = DecodesSettings.instance();
		Properties props = new Properties();
		try
		{
			FileInputStream fis = new FileInputStream(propFile);
			props.load(fis);
			fis.close();
		}
		catch(IOException e)
		{
			Logger.instance().log(Logger.E_FAILURE,
			"Cannot open DECODES Properties File '"+propFile+"': "+e);
		}
		if (!settings.isLoaded())
			settings.loadFromProperties(props);

		if (!silent)
		{	System.out.print("Init DECODES DB: "); System.out.flush(); }

		// Construct database and the interface specified by properties.
		Database db = new Database();
		Database.setDb(db);

		DatabaseIO dbio;
		dbio = DatabaseIO.makeDatabaseIO(settings.editDatabaseTypeCode,
			settings.editDatabaseLocation);
		db.setDbIo(dbio);

		if (!silent)
		{	System.out.print("EU, "); System.out.flush(); }
		
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

		if (!silent)
		{	System.out.print("Site, "); System.out.flush(); }
		Site.explicitList = true;
		db.siteList.read();
		siteListRead = true;
		
		if (!silent)
		{	System.out.print("Equip, "); System.out.flush(); }
		db.equipmentModelList.read();
		
		if (!silent)
		{	System.out.print("Config, "); System.out.flush(); }
		db.platformConfigList.read();
		if (!initializedForDecoding)
			initializeForDecoding();

		db.platformConfigList.countPlatformsUsing();
		
		if (!silent)
		{	System.out.print("Routing, "); System.out.flush(); }
		db.routingSpecList.read();

		initializedForEditing = true;
		if (!silent)
			System.out.println();
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
		if (!silent)
		{	System.out.print("Platform List, "); System.out.flush(); }
		db.platformList.read();
		if (!silent)
		{	System.out.print("Presentation Groups, "); System.out.flush(); }
		db.presentationGroupList.read();
		if (!silent)
		{	System.out.print("Network Lists, "); System.out.flush(); }
		db.networkListList.read();
		initializedForDecoding = true;
		if (!silent)
			System.out.println();
	}
	
	public static void readSiteList()
		throws DecodesException
	{
		if (!siteListRead)
		{
			if (!silent)
			{
				System.out.print("Site, ");
				System.out.flush();
			}
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
		for(Iterator it = pgl.iterator(); it.hasNext(); )
		{
			PresentationGroup pg = (PresentationGroup)it.next();
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
		for(Iterator it = denum.iterator(); it.hasNext(); )
		{
			EnumValue env = (EnumValue)it.next();
			ret[i++] = env.value;
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
					String msg = "Cannot prepare presentation group '"
						+ name + "': " + ex;
					System.err.println(msg);
					ex.printStackTrace(System.err);
					Logger.instance().failure(msg);
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
				props);
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
		RawMessage rm = dataSource.lrgsMsg2RawMessage(msg);
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

		DecodedMessage dm = ds.decodeMessage(rm);
		//Calculates the elevation using the preoffset, scale and offset
		dm.applyScaleAndOffset();

		// Use presentation group to convert units & format values
		if (presGrp != null)
			dm.formatSamples(presGrp);

		// Use the formatter & consumer to output the message.
		decodeBuf.delete(0, decodeBuf.length());
		formatter.writeMessage(dm, consumer);

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
			ChannelMap.useLockForDownload = true;
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

}
