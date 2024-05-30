/*
*  $Id: CwmsConsumer.java,v 1.13 2020/01/31 19:30:23 mmaloney Exp $
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
*  $Log: CwmsConsumer.java,v $
*  Revision 1.13  2020/01/31 19:30:23  mmaloney
*  Improve debugs
*
*  Revision 1.12  2018/12/18 15:21:02  mmaloney
*  Updates for jOOQ
*
*  Revision 1.11  2018/05/01 17:35:26  mmaloney
*  sourceId is now a DbKey
*
*  Revision 1.10  2017/08/22 19:29:15  mmaloney
*  Refactor
*
*  Revision 1.9  2017/02/20 19:41:51  mmaloney
*  The TsdbCompLock code has been moved to RoutingSpecThread.main and run.
*
*  Revision 1.8  2015/05/14 13:52:19  mmaloney
*  RC08 prep
*
*  Revision 1.7  2015/04/02 18:08:24  mmaloney
*  Use DbURI and jdbcOracleDriver if they are defined in CwmsDbConfig.
*  This (re)allows the use of an XML decodes database writing to CWMS.
*
*  Revision 1.6  2014/10/28 18:37:34  mmaloney
*  Use Platform Sensor Site for location if one is defined.
*
*  Revision 1.5  2014/08/22 17:23:11  mmaloney
*  6.1 Schema Mods and Initial DCP Monitor Implementation
*
*  Revision 1.4  2014/06/27 20:00:44  mmaloney
*  getSiteName fix: It was using the wrong constant.
*
*  Revision 1.3  2014/05/30 13:15:35  mmaloney
*  dev
*
*  Revision 1.2  2014/05/28 13:09:31  mmaloney
*  dev
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.23  2013/04/23 13:25:23  mmaloney
*  Office ID filtering put back into Java.
*
*  Revision 1.22  2013/03/25 19:03:29  mmaloney
*  Allow clients to monitor events.
*
*  Revision 1.21  2013/03/21 18:27:40  mmaloney
*  DbKey Implementation
*
*/
package decodes.cwms;

import ilex.util.AuthException;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;

import org.opendcs.authentication.AuthSourceService;
import org.opendcs.database.DatabaseService;

import opendcs.dai.LoadingAppDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.util.logging.JulUtils;
import decodes.consumer.DataConsumer;
import decodes.consumer.DataConsumerException;
import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.DatabaseIO;
import decodes.db.EngineeringUnit;
import decodes.db.Platform;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.db.TransportMedium;
import decodes.decoder.DecodedMessage;
import decodes.decoder.Sensor;
import decodes.decoder.TimeSeries;
import decodes.sql.DbKey;
import decodes.tsdb.BadConnectException;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbIoException;
import decodes.tsdb.LockBusyException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsdbCompLock;
import decodes.util.DecodesSettings;
import decodes.util.PropertySpec;
import decodes.util.TSUtil;

/**
 CwmsConsumer writes data to the CWMS Oracle Database.
 Notice: This Consumer uses Oracle classes from Jar file: 
 ojdbc14.jar. In addition, depending on which oracle driver
 you use native code will be required. If the oracle thin 
 driver is used you don't need native code, but if the oracle 
 oci driver is used you need native code.
 
 <p>Properties used by CwmsConsumer include:</p>
 <p>Properties from decodes-cwms.conf (located on $DECODES_INSTALL_DIR)</p>
 <ul>  
  <li>Timezone=GMT	(GMT is the default time zone)</li>
  <li>dbUri=host:portnumber:SID	
			(this is the DB connection information, oracle tnsName)</li>
  <li>cwmsVersion=raw	(raw is the default value for CWMS version)</li>
  <li>cwmsofficeid	(This is a required field for the store_ts procedure, 
  			the user will have to set it, no default value provided)</li>
 </ul>
 <p>Properties from cwmsdb.auth (located on $DECODES_INSTALL_DIR)</p>
 <ul>  
  <li>Username	(The username to connect to the CWMS Database)</li>
  <li>Password	(The password to connect to the CWMS Database)</li>
 </ul>
 <p>Properties from shefCwmsParam.prop file 
 								(located on $DECODES_INSTALL_DIR):</p>
 <ul>  
  <li>All the SHEF code to CWMS code mapping, Example: PC=Precip</li>
 </ul>
 <p>Properties from routing spec dialog window (optional):</p>
 <ul>  
  <li>cwmsofficeid	(This value will override the cwmsofficeid 
  												configuration value)</li>
  <li>storerule	(valid values: Delete Insert, Replace All, 
  Replace With Non Missing, Replace Missing Values Only, Do Not Replace -
  This value will override the hardcoded value for store
  rule field)</li>
  <li>overrideprot	(This is the Data protection checking rule flag rule. 
	Indicates if data will be override or not. Refer to CWMS manual for 
	more information. 1 means rule not enforced, 0 means rule is enforced)</li>
  <li>versiondate	(Do not use with current CWMS DB version v2)
  					(optional parameter for store_ts, format mm/dd/yyyy 
					default to null)</li>
 </ul>
 <p>Properties from sensor dialog window (optional):</p>
 <ul>  
  <li>CwmsDuration (This value will override the CwmsDuration 
  												value set by the code)</li>
  <li>CwmsVersion	(This value will override the cwmsversion 
  												configuration value)</li>
  <li>CwmsParamType (This value will override the CwmsParamType 
  												value set by the code)</li>
 </ul>
 <p>To create DB username/password authentication file:</p>
 <ul>  
  <li>Use setCwmsUser script (on linux system)</li>
  <li>Use setCwmsUser.bat (on windows system)</li>
 </ul> 
*/
public class CwmsConsumer extends DataConsumer
{
	private String module = "CwmsConsumer";
	/** Store the properties of the shefCwmsParam.prop file */
	private Properties shefCwmsProps;
	/** The Cwms version used for the time series descriptor */
	private String cwmsVersion;

	CwmsDbConfig cwmsCfg = CwmsDbConfig.instance();
	/** Connection to the database */
	
	private CwmsTimeSeriesDb cwmsTsdb = null;
	private TsdbCompLock myLock = null;
	private int msgsProcessed = 0;
	private boolean lockDeleted = false;
	
	private PropertySpec[] myspecs = new PropertySpec[]
	{
		new PropertySpec("cwmsOfficeId", PropertySpec.BOOLEAN, 
			"Optional override Office ID to use when connecting to CWMS."),
		new PropertySpec("cwmsVersion", PropertySpec.STRING, 
			"Optional VERSION to use when creating new time series.")
	};

	/** Default constructor */
	public CwmsConsumer()
	{
		super();
		cwmsVersion = CwmsConstants.DEFAULT_VERSION_VALUE;
	}

	/**
	  Opens and initializes the consumer.
	  This method is called once, at the start of the routing spec. It will
	  read the configuration file and establish the connection to the 
	  CWMS Oracle Database. In addition, it will set all the required
	  properties.
	  
	  @param consumerArg file name template.
	  @param props routing spec properties.
	  @throws DataConsumerException if the consumer could not be initialized.
	*/
	public void open(String consumerArg, Properties props)
		throws DataConsumerException
	{		
		lockDeleted=false;
		
		// Read the CWMS configuration file.
		initCwmsConfig(props);

		// Get username & password from Auth file
		Properties credentials = new Properties();
		String authFileName = 
			EnvExpander.expand(cwmsCfg.getDbAuthFile());
		try 
		{
			credentials = AuthSourceService.getFromString(authFileName)
										   .getCredentials();
		}
		catch(AuthException ex)
		{
			String msg = module + " Cannot read DB credentials '" 
				+ authFileName + "'";			
			throw new DataConsumerException(msg,ex);
		}
		
		// Get the Oracle Data Source & open a connection.
		try
		{
			DecodesSettings settings = DecodesSettings.instance().asCopy();
			settings.editDatabaseTypeCode = DecodesSettings.DB_CWMS;
			settings.editDatabaseType = "CWMS";
			settings.editDatabaseLocation = cwmsCfg.getDbAuthFile();
			settings.DbAuthFile = cwmsCfg.DbAuthFile;
			settings.CwmsOfficeId = cwmsCfg.cwmsOfficeId;
			settings.editTimeZone = cwmsCfg.timeZone;
			cwmsTsdb = (CwmsTimeSeriesDb)DatabaseService.getDatabaseFor("decodes", settings, credentials);
			cwmsTsdb.requireCcpTables = false;
			Logger.instance().info(module + " Connected to CWMS database");
		}
		catch (DatabaseException ex)
		{
			String msg = module + " " + ex;
			Logger.instance().fatal(msg);
			throw new DataConsumerException(msg);
		}

		// Open and load the SHEF to CWMS Param properties file. This file
		// contains all the mapping needed to convert from Shef codes to 
		// Cwms values. If we can not open this properties file, we'll use
		// the hard coded Property Hash Map.
		loadShefCwmsParamMapping(cwmsCfg.getShefCwmsParamFile());
	}

	/**
	 * Unconditionally and silently close everything.
	*/
	public void close()
	{
		if (myLock != null)
		{
			LoadingAppDAI loadingAppDAO = cwmsTsdb.makeLoadingAppDAO();
			try
			{
				loadingAppDAO.releaseCompProcLock(myLock);
				myLock = null;
			}
			catch(DbIoException ex)
			{
				Logger.instance().warning("DbIoException releasing lock: " + ex);
				myLock = null;
			}
			finally
			{
				loadingAppDAO.close();
			}
		}

		cwmsTsdb = null;
	}

	/**
	  This method is called at the beginning of each decoded message. We do all
	  the IO work here: the println method does nothing.
	  Use a NullFormatter when using CwmsConsumer.

	  @param msg The message to be written.
	  @throws DataConsumerException if an error occurs.
	*/
	public synchronized void startMessage(DecodedMessage msg)
		throws DataConsumerException
	{
		Platform platform;
		TransportMedium tm;
		RawMessage rawmsg;
		try
		{
			rawmsg = msg.getRawMessage();
			tm = rawmsg.getTransportMedium();
			platform = rawmsg.getPlatform();
		}
		catch(UnknownPlatformException ex)
		{
			Logger.instance().warning(module + 
				" Skipping CWMS ingest for data from "
				+ "unknown platform: " + ex);
			return;
		}
		Site platformSite = platform.getSite();
		if (platformSite == null)
		{
			Logger.instance().warning(module + 
					" Skipping CWMS ingest for data from "
					+ "unknown site, DCP Address = " + tm.getMediumId());
			return;
		}
		msgsProcessed++;

		try
		{
			// Process the time series data and call the store_ts procedure
			for(Iterator<TimeSeries> it = msg.getAllTimeSeries(); it.hasNext(); )
			{
				TimeSeries ts = it.next();
				// Only process time series that have data.
				if (ts.size() == 0)
					continue;
				// Need to fill out the following fields for the store_ts
				// procedure: p_office_id, p_timeseries_desc, p_units, 
				// (array with timestamp, value, quality), p_store_rule
				// p_override_prot, p_versiondate
				Sensor sensor = ts.getSensor();
				if (sensor == null)
				{
					Logger.instance().warning(module 
									+ " Platform DCP " + tm.getMediumId() 
									+ " has no sensor configured -- skipping.");
					continue;
				}
				// Office ID from routing spec properties or config value.
				String timeSeriesDesc = createTimeSeriesDesc(ts, platformSite);
				if (timeSeriesDesc == null)
				{	// Could not create the right time descriptor, skipping this 
					// msg.
					Logger.instance().warning(module  
							+ " Platform Site Name " + platform.getSiteName()
							+ ", Platform Agency " + platform.getAgency()
							+ ", DCP Address " + tm.getMediumId() 
							+ ", sensor " + sensor.getName()
							+ " Cannot find CWMS or SHEF datatype -- skipping.");
					continue;
				}
				String units = ts.getEU().abbr;			
				processAndStoreData(ts, timeSeriesDesc, units,
					platform, tm.getMediumId());
			}
		}
		catch(Exception ex)
		{
			String emsg = module + " Error storing TS data: " + ex;
			Logger.instance().warning(emsg);

			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			ex.printStackTrace(pw);
			Logger.instance().warning(sw.toString());
			// It might be a business rule exception, like improper units.
			// So don't kill the whole routing spec, just go on.
//			close();
//			throw new DataConsumerException(emsg);
		}
		finally
		{
		}
	}
	
	/**
	 * This method gets all sensor values (timestamp and actual readings),
	 * and call the insertInStoreTs to insert the data in the CWMS Database.
	 * 
	 * @param ts the time series object containing current data
	 * @param timeSeriesDesc CWMS time series descriptor
	 * @param unit the units of the data set
	 * @param Platform current platform in process
	 * @param DCPAddress current DCP in process
	 * @throws DataConsumerException if it fails to insert data
	 */
	private void processAndStoreData(TimeSeries ts, 
		String timeSeriesDesc, String units, 
		Platform platform, String DCPAddress) 
		throws DbIoException
	{
		TimeSeriesIdentifier tsid = null;

		try(Connection conn = cwmsTsdb.getConnection();
			TimeSeriesDAI timeSeriesDAO = cwmsTsdb.makeTimeSeriesDAO();)
		{
			timeSeriesDAO.setManualConnection(conn);
			try
			{
				tsid = timeSeriesDAO.getTimeSeriesIdentifier(timeSeriesDesc);
			}
			catch(NoSuchObjectException ex)
			{
				Logger.instance().info(module + " No time series for '" + timeSeriesDesc
					+ "' -- will attempt to create.");
				tsid = new CwmsTsId();
				try { tsid.setUniqueString(timeSeriesDesc); }
				catch(BadTimeSeriesException ex2)
				{
					Logger.instance().warning(module + " Cannot create time series -- bad path '"
						+ timeSeriesDesc + ": " + ex2);
					return;
				}
				try { timeSeriesDAO.createTimeSeries(tsid); }
				catch(NoSuchObjectException ex2)
				{
					Logger.instance().warning(module + " Cannot create time series for path '"
						+ timeSeriesDesc + ": " + ex2);
					return;
				}
				catch(BadTimeSeriesException ex3)
				{
					Logger.instance().warning(module + " Cannot create time series for path '"
						+ timeSeriesDesc + ": " + ex3);
					return;
				}
			}
			
			String tabSel = tsid.getPart("ParamType") + "."
				+ tsid.getPart("Duration") + "."
				+ tsid.getPart("Version");
	
	
			CTimeSeries cts = TSUtil.convert2CTimeSeries(
				ts,                    // the DECODES Time Series
				tsid.getKey(),         // ts_code
				tabSel,                // CWMS tabse is paramtype.duration.version
				tsid.getInterval(), 
				true,                  // mustWrite flag (we want to write all values in the TS
				DbKey.NullKey);        // sourceId not used in CWMS
	
			try
			{
				timeSeriesDAO.saveTimeSeries(cts);
			}
			catch (BadTimeSeriesException ex)
			{
				Logger.instance().failure(module + " Cannot save time series for '"
					+ timeSeriesDesc + "': " + ex);
			}
		}
		catch(SQLException ex)
		{
			throw new DbIoException("Unable to acquire connection.", ex);
		}
	}
	
	/*
	  Does nothing.
	  @param line the line to be written.
	*/
	public void println(String line)
	{
	}

	/**
	  Called when a message is complete. Do nothing.
	*/
	public void endMessage()
	{
	}

	/** 
	  For status gathering, this method returns some symbolic name about the
	  consumer. For a file consumer this is the file name.

	  @return symbolic output name
	*/
	public synchronized String getActiveOutput()
	{
		if (myLock != null)
		{
			LoadingAppDAI loadingAppDAO = cwmsTsdb.makeLoadingAppDAO();
			try
			{
				myLock.setStatus("msgs:" + msgsProcessed);
				loadingAppDAO.checkCompProcLock(myLock);
			}
			catch(DbIoException ex)
			{
				Logger.instance().warning("DbIoException checking lock: " + ex);
				myLock = null;
			}
			catch (LockBusyException ex)
			{
				Logger.instance().warning("LockBusyException: " + ex);
				lockDeleted = true;
				if (routingSpecThread != null)
				{
					routingSpecThread.setCurrentStatus("TSDB Lock Removed");
					routingSpecThread.shutdown();
				}
			}
			finally
			{
				loadingAppDAO.close();
			}
		}
		return "cwms";
	}

	/**
	  Returns null, this consumer cannot do streaming output.
	*/
	public OutputStream getOutputStream()
	{
		return null;
	}

	/**
	 * This method gets properties from the Routing Spec.
	 * 
	 * @param props the properties from rounting spec
	 */
	private void getPropertyValues(Properties props)
	{
		// Get Office ID from Routing Spec properties
		String s = 
			PropertiesUtil.getIgnoreCase(props,CwmsConstants.CWMS_OFFICE_ID);
		if (s != null)
			cwmsCfg.cwmsOfficeId = s;

		// Get the "cwmsVersion" property, which overrides the config file.
		s = PropertiesUtil.getIgnoreCase(props, CwmsConstants.CWMS_VERSION);
		if (s != null && s.trim().length() > 0)
		{
			cwmsVersion = s;
			Logger.instance().debug3("Using rs property version '" + cwmsVersion + "'");
		}
	}
	
	/**
	 * This method builds the Cwms time series descriptor. The descriptor
	 * has  six parts: location.param.paramtype.interval.duration.version
	 * 
	 * @param ts the time series object containing current data
	 * @param platformSite current site
	 * @return String time series descriptor or null if can not build the
	 * 			param part
	 */
	public String createTimeSeriesDesc(TimeSeries ts, Site platformSite)
	{
		StringBuffer timeSeriesDescriptor = new StringBuffer("");
		
		Site site = ts.getSensor().getSite();
		if (site == null)
			site = platformSite;
		
		// Find the location. Use the CWMS site name or default site name type
		SiteName sn = site.getName(Constants.snt_CWMS);
		if (sn == null)
			sn = site.getPreferredName();
		String location = sn.getNameValue();
		
		timeSeriesDescriptor.append(location);
		timeSeriesDescriptor.append(".");
		
		// Find the Param value.
		String param = getParamValue(ts);
		if (param == null)
		{	// If not cwms code, not shef code, skip this message
			// The startMessage method will log a warning message
			return null;
		}
		timeSeriesDescriptor.append(param);
		timeSeriesDescriptor.append(".");
		
		// Find the Param Type value, if user assigned sensor property 
		// CwmsParamType use this value else set ParamType to Inst.
		String paramType = 
					ts.getSensor().getProperty(CwmsConstants.CWMS_PARAM_TYPE);
		if (paramType == null)
			paramType = CwmsConstants.PARAM_TYPE_INST; // Inst
		timeSeriesDescriptor.append(paramType);
		timeSeriesDescriptor.append(".");
		
		// Find Interval, if recording mode is Fixed: 
		// convert them from seconds to minutes or
		// hours or 1day or week or 1month or 1year or 1decade
		String intervalStr = ts.getSensor().getProperty("cwmsInterval");
		if (intervalStr == null)
		{
			if (ts.getSensor().getRecordingMode() == 
												Constants.recordingModeVariable)
			{ // If recording mode is Variable (V), set intervalStr to 0;
				intervalStr = "0";
			}
			else
			{ // Recording mode is Fixed.
				intervalStr = 
					getIntervalValue(ts.getSensor().getRecordingInterval());
			}
		}
		timeSeriesDescriptor.append(intervalStr);
		timeSeriesDescriptor.append(".");
		
		// Find Duration. If it is setup in the sensor property use it.
		String duration = 
					ts.getSensor().getProperty(CwmsConstants.CWMS_DURATION);
		if (duration == null)
		{
			if (paramType.equalsIgnoreCase(CwmsConstants.PARAM_TYPE_INST))
			{	// if paramType value is Inst set duration to 0
				duration = "0";
			}
			else
			{	// else set duration to interval value, (see above)
				duration = intervalStr;
			}
		}
		timeSeriesDescriptor.append(duration);
		timeSeriesDescriptor.append(".");
		
		// Find Version. "raw" is the default value
		String tempVersion = 
			ts.getSensor().getProperty(CwmsConstants.CWMS_VERSION);
		if (tempVersion != null)
		{	// if user set a CwmsVersion property on a sensor, 
			// use it and override cwmsVersion value
			timeSeriesDescriptor.append(tempVersion);
		}
		else
		{
			timeSeriesDescriptor.append(cwmsVersion);
Logger.instance().debug3("Using default version '" + cwmsVersion + "'");
		}
		
		return timeSeriesDescriptor.toString();
	}
	
	/**
	 * This method fills out the Param "Parameter Element" value 
	 * of the time series descriptor. It uses the data type code 
	 * from Decodes Database editor. If the data type is CWMS it 
	 * will use the code directly, but if the data type is SHEF it
	 * will convert from SHEF to CWMS codes using an external text file
	 * call shefCwmsParam.prop located on DECODES_INSTALL_DIR.
	 *
	 * @param TimeSeries the time series obj containing current data
	 * @return the cwms data type code or null if no cwms neither shef
	 * 			codes are found
	 */
	private String getParamValue(TimeSeries ts)
	{
		String param;
		DataType dt = ts.getSensor().getDataType(CwmsConstants.CWMS_DATA_TYPE);
		if (dt != null)
		{	// If a CWMS data type is specified by the user, use it directly
			param = dt.getCode().trim();
		}
		else
		{
			// If SHEF code is provided, convert from shef to cwms using
			// the values from the text file shefCwmsParam.prop
			dt = ts.getSensor().getDataType(Constants.datatype_SHEF);
			if (dt != null)
			{
				param = PropertiesUtil.getIgnoreCase(shefCwmsProps, 
					dt.getCode().trim());
				EngineeringUnit eu = ts.getEU();
				if (eu.getAbbr().equalsIgnoreCase("v"))
					ts.setEU(EngineeringUnit.getEngineeringUnit("volt"));
			}
			else
			{	// If not cwms code, not shef code, skip this message
				// Return null, a warning message will be displayed on
				// the startMessage method when a null is returned.
				param = null;
			}
		}
		return param;
	}
	
	/**
	 * This method will convert from seconds to:
	 * 1Minute, 2Minutes, (3,4,5,6,10,12,15,20,30) Minutes
	 * 1Hour, 2Hours, 3Hours, 4Hours, 6Hours, 8Hours, 12Hours
	 * 1Day
	 * Week
	 * 1Month
	 * 1Year
	 * 1Decade 
	 * It gets the closes value to the second passed in. For example
	 * if intervalInSeconds passed in was 122, the return value will be
	 * 2Minutes.
	 * 
	 * @param intervalInSeconds the recording interval from time series obj
	 * @return the formatted string according to seconds given
	 */
	private String getIntervalValue(int intervalInSeconds)
	{	// All seconds for the names listed on intervalNames array.
		// The numbers of the int intervals array matches the names
		// on the intervalNames, ex. 60=1Minute, 604800=Week, etc.
		int intervals[] = 
						{60,120,180,240,300,360,600,720,900,1200,1800,
						3600,7200,10800,14400,21600,28800,43200,
						86400,
						604800,
						2592000,
						31556926,
						315569260};
		String intervalNames[] = 
				{"1Minute","2Minutes","3Minutes","4Minutes","5Minutes",
				"6Minutes","10Minutes","12Minutes","15Minutes","20Minutes",
				"30Minutes",
				"1Hour","2Hours","3Hours","4Hours","6Hours","8Hours","12Hours",
				"1Day","Week","1Month","1Year","1Decade"};
		int closestValue = -1;
		for (int i = 0;i<intervals.length;i++)
		{
			if (closestValue == -1 || 
					(Math.abs(intervalInSeconds - intervals[i]) < 
						Math.abs(intervalInSeconds - intervals[closestValue])))
			{
				closestValue = i;
			}
		}		
		return intervalNames[closestValue];
	}

	/**
	 * This method initializes the CWMS configuration from three places:
	 * <ul>
	 *   <li>If this is a CWMS DECODES Database, get parms from decodes.properties</li>
	 *   <li>Try to open the properties file $DECODES_INSTALL_DIR/decodes-cwms.conf.</li>
	 *   <li>Override with properties from the routing spec.</li>
	 * </ul> 
	 */
	public void initCwmsConfig(Properties props)
	{
		String configFileName = CwmsConstants.CONFIG_FILE_NAME;
		DatabaseIO dbio = Database.getDb().getDbIo();
		if (dbio instanceof CwmsSqlDatabaseIO)
		{
			cwmsCfg.initFromDecodesDb((CwmsSqlDatabaseIO)dbio);
		}
		try { cwmsCfg.loadFromProperties(configFileName); }
		catch(IOException ex)
		{
			if (!(dbio instanceof CwmsSqlDatabaseIO))
			{
				String msg = module + " Cannot load configuration from '"
					+ configFileName + "': " + ex;
				Logger.instance().failure(msg);
			}
		}

		// If user set Cwms version on config file, use it
		String s = cwmsCfg.getCwmsVersion();
		if (s != null)
		{
			cwmsVersion = s;
		}
		
		// Get properties from Routing Spec 
		getPropertyValues(props);
	}

	
	/**
	 * This method reads all the Shef/Cwms codes mapping found
	 * on the DECODES_INSTALL_DIR/shefCwmsParam.prop file. It 
	 * stores all the properties on the shefCwmsProps Properties 
	 * object class. These properties will be used when creating 
	 * the Cwms Param part time series descriptor. If it cannot read
	 * this properties file, this method will fill out the shefCwmsProps
	 * Properties object with some hard coded shef-cwms mapping values.
	 * 
	 * @param shefCwmsFilePath the file of the shefCwmsParam.prop file
	 */
	public void loadShefCwmsParamMapping(String shefCwmsFilePath)
	{
		//CwmsConstants.SHEF_CWMS_PARAM_FILEPATH
		shefCwmsProps = new Properties();
		fillInShefCwmsProps();
		String shefCwmsMap = "";
		try 
		{ 
			shefCwmsMap = EnvExpander.expand(shefCwmsFilePath);
			FileInputStream is = new FileInputStream(new File(shefCwmsMap));
			shefCwmsProps.load(is);
			is.close();	
		}
		catch(IOException ex)
		{
			String msg = module + " Cannot read properties file '" + 
					shefCwmsMap + "': " + ex + " -- will use defaults only.";
			Logger.instance().info(msg);
		}
	}
	
	/**
	 * Fill out the shefCwmsProps Properties Object with hard coded values.
	 *
	 */
	private void fillInShefCwmsProps()
	{
		shefCwmsProps.setProperty(CwmsConstants.PC, CwmsConstants.PRECIP);
		shefCwmsProps.setProperty(CwmsConstants.HG, CwmsConstants.STAGE);
		shefCwmsProps.setProperty(CwmsConstants.HP, CwmsConstants.STAGE_POOL);
		shefCwmsProps.setProperty(CwmsConstants.HT, CwmsConstants.STAGE_TAIL);
		shefCwmsProps.setProperty(CwmsConstants.VB, CwmsConstants.VOLT);
		shefCwmsProps.setProperty(CwmsConstants.BV, CwmsConstants.VOLT);
		shefCwmsProps.setProperty(CwmsConstants.HR, CwmsConstants.ELEV);
		shefCwmsProps.setProperty(CwmsConstants.LF, CwmsConstants.STOR);
		shefCwmsProps.setProperty(CwmsConstants.QI, CwmsConstants.FLOW_IN);
		shefCwmsProps.setProperty(CwmsConstants.QR, CwmsConstants.FLOW);
		shefCwmsProps.setProperty(CwmsConstants.TA, CwmsConstants.TEMP_AIR);
		shefCwmsProps.setProperty(CwmsConstants.TW, CwmsConstants.TEMP_WATER);
		shefCwmsProps.setProperty(CwmsConstants.US, CwmsConstants.SPEED_WIND);
		shefCwmsProps.setProperty(CwmsConstants.UP, CwmsConstants.SPEED_WIND);
		shefCwmsProps.setProperty(CwmsConstants.UD, CwmsConstants.DIR_WIND);
	}
	
	@Override
	public PropertySpec[] getSupportedProps()
	{
		return myspecs;
	}
}
