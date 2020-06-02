/*
*  $Id: OpenTsdbConsumer.java,v 1.5 2020/02/20 15:32:16 mmaloney Exp $
*  
*  $Log: OpenTsdbConsumer.java,v $
*  Revision 1.5  2020/02/20 15:32:16  mmaloney
*  Use sensor properties, which if this is in ExportTimeSeries, will be delegated to
*  the TSID, to build the TSID parts.
*
*  Revision 1.4  2020/01/31 19:43:18  mmaloney
*  Several enhancements to complete OpenTSDB.
*
*  Revision 1.3  2019/12/11 14:44:20  mmaloney
*  Partial implementation of OpenTSDB computations
*
*  Revision 1.2  2018/05/31 14:14:41  mmaloney
*  Set storage units when creating new TSID.
*
*  Revision 1.1  2018/05/01 17:49:45  mmaloney
*  First working OpenTSDB Consumer
*
*/
package opendcs.opentsdb;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import ilex.util.UserAuthFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Properties;

import opendcs.dai.TimeSeriesDAI;
import decodes.consumer.DataConsumer;
import decodes.consumer.DataConsumerException;
import decodes.cwms.CwmsConstants;
import decodes.cwms.CwmsTsId;
import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.db.Constants;
import decodes.db.DataType;
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
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.util.DecodesSettings;
import decodes.util.PropertySpec;
import decodes.util.TSUtil;

/**
 OpenTsdbConsumer writes data to the OpenTSDB Database.
 
 <p>Properties from shefCwmsParam.prop file 
 								(located on $DECODES_INSTALL_DIR):</p>
 <ul>  
  <li>All the SHEF code to CWMS code mapping, Example: PC=Precip</li>
 </ul>
 <p>Properties from routing spec dialog window (optional):</p>
 <ul>  
   <li>storerule	(valid values: Delete Insert, Replace All, 
  Replace With Non Missing, Replace Missing Values Only, Do Not Replace -
  This value will override the hardcoded value for store
  rule field)</li>
  <li>overrideprot	(This is the Data protection checking rule flag rule. 
	Indicates if data will be override or not. Refer to CWMS manual for 
	more information. 1 means rule not enforced, 0 means rule is enforced)</li>
 </ul>
 <p>Properties from sensor dialog window (optional):</p>
 <ul>  
  <li>CwmsDuration (This value will override the CwmsDuration 
  												value set by the code)</li>
  <li>CwmsParamType (This value will override the CwmsParamType 
  												value set by the code)</li>
 </ul>
 <p>To create DB username/password authentication file:</p>
 <ul>  
  <li>Use setCwmsUser script (on linux system)</li>
  <li>Use setCwmsUser.bat (on windows system)</li>
 </ul> 
*/
public class OpenTsdbConsumer extends DataConsumer
{
	private String module = "OpenTsdbConsumer";
	
	private String dbAuthFile = null;
	private String appName = "decodes";
	private DbKey appId = DbKey.NullKey;
	private String shefParamMapping = null;
	private String dataTypeStandard = Constants.datatype_CWMS;
	private boolean canCreateTs = true;
	
	/** Store the properties of the shefCwmsParam.prop file */
	private Properties shefCwmsProps;
	
	/** The Cwms version used for the time series descriptor */
	private String cwmsVersion = "raw";

	private OpenTsdb openTsdb = null;
	
	private PropertySpec propSpecs[] =
	{
		new PropertySpec("databaseLocation", PropertySpec.STRING, 
			"(default=null, meaning use the same database as DECODES) This property allows"
			+ " you to write to a different database from the one hosting DECODES metadata."),
		new PropertySpec("dbAuthFile", PropertySpec.FILENAME,
			"(deafult=null) If the database is different from the DECODES database, this file "
			+ "can be used to specify the encrypted file containing database username and password."),
		new PropertySpec("jdbcOracleDriver", PropertySpec.STRING, 
			"(default=null) If the database is different from the DECODES database, this specifies"
			+ " the JDBC driver to use."),
		new PropertySpec("appName", PropertySpec.STRING, 
			"(default=null) The application name to connect to the database as. If not supplied,"
			+ " use the application name assigned to the running routing spec. If none, use 'decodes'."),
		new PropertySpec("dataTypeStandard", PropertySpec.DECODES_ENUM + "DataTypeStandard", 
			"(default=CWMS) Specifies which data type is used from the sensor to build the TSID."),
		new PropertySpec("shefParamMapping", PropertySpec.FILENAME,
			"If mapping from SHEF to the Param part of the TSID is required, supply a mapping file."),
		new PropertySpec("canCreateTS", PropertySpec.BOOLEAN, 
			"(default=true) Allows the consumer to create new time series if one doesn't exist."),
		new PropertySpec("tsidDuration", PropertySpec.STRING, 
			"(default=0, meaning instantaneous reading) The default duration part of the time series ID. "
			+ "For compatibility, this can also be specified as 'cwmsDuration'. Sensor properties with "
			+ "the same name will override the default set here."),
		new PropertySpec("tsidVersion", PropertySpec.STRING, 
			"(default='raw') The default version part of the time series ID. For compatibility,"
			+ " this can also be specified as 'cwmsVersion'. Sensor properties with the same name"
			+ " will override the default set here."),
	};

	/** Default constructor */
	public OpenTsdbConsumer()
	{
		super();
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
		Logger.instance().info(module + " initializing.");
		// Get username & password from Auth file
		Properties credentials = new Properties();
		dbAuthFile = PropertiesUtil.getIgnoreCase(props, "dbAuthFile");
		if (dbAuthFile == null)
			dbAuthFile = DecodesSettings.instance().DbAuthFile;
		String authPath = EnvExpander.expand(dbAuthFile);
		try 
		{
			UserAuthFile authFile = new UserAuthFile(authPath);
			authFile.read();
			credentials.setProperty("username", authFile.getUsername());
			credentials.setProperty("password", authFile.getPassword());
		}
		catch(Exception ex)
		{
			String msg = module + " Cannot read DB auth from file '" 
				+ authPath + "': " + ex;
			Logger.instance().warning(module + " " + msg);
		}
		
		// Get the Oracle Data Source & open a connection.
		try
		{
			openTsdb = new OpenTsdb();
			String s = PropertiesUtil.getIgnoreCase(props, "databaseLocation");
			if (s != null)
				openTsdb.setDatabaseLocation(s);
			else
				openTsdb.setDatabaseLocation(DecodesSettings.instance().editDatabaseLocation);
			s = PropertiesUtil.getIgnoreCase(props, "jdbcOracleDriver");
			if (s != null)
				openTsdb.setJdbcOracleDriver(s);
			else
				openTsdb.setJdbcOracleDriver(DecodesSettings.instance().jdbcDriverClass);
			
			s = PropertiesUtil.getIgnoreCase(props, "appName");
			if (s != null)
				appName = s; 
			          
			appId = openTsdb.connect(appName, credentials);
		}
		catch (BadConnectException ex)
		{
			String msg = module + " " + ex;
			Logger.instance().fatal(msg);
			throw new DataConsumerException(msg);
		}

		// Open and load the SHEF to CWMS Param properties file. This file
		// contains all the mapping needed to convert from Shef codes to 
		// Cwms values. If we can not open this properties file, we'll use
		// the hard coded Property Hash Map.
		shefParamMapping = PropertiesUtil.getIgnoreCase(props, "shefParamMapping");
		if (shefParamMapping != null)
			loadShefCwmsParamMapping(shefParamMapping);
		
		String s = PropertiesUtil.getIgnoreCase(props, "dataTypeStandard");
		if (s != null && s.trim().length() > 0)
			dataTypeStandard = s;
		
		// Get the "cwmsVersion" property, which overrides the config file.
		s = PropertiesUtil.getIgnoreCase(props, CwmsConstants.CWMS_VERSION);
		if (s != null && s.trim().length() > 0)
			cwmsVersion = s;
		
		s = PropertiesUtil.getIgnoreCase(props, "canCreateTS");
		if (s != null && s.trim().length() > 0)
			canCreateTs = TextUtil.str2boolean(s);

	}

	/**
	 * Unconditionally and silently close everything.
	*/
	public void close()
	{
		Logger.instance().info(module + " closing database connection with appID=" + appId);
		openTsdb.closeConnection();
		openTsdb = null;
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
				String tsid = buildTSID(ts, platformSite);
				if (tsid == null)
				{	// Could not create the right time descriptor, skipping this 
					// msg.
					Logger.instance().warning(module  
							+ " Platform Site Name " + platform.getSiteName()
							+ ", Platform Agency " + platform.getAgency()
							+ ", DCP Address " + tm.getMediumId() 
							+ ", sensor " + sensor.getName()
							+ " Cannot determine " + dataTypeStandard + " datatype -- skipping.");
					continue;
				}
				String units = ts.getEU().abbr;			
				processAndStoreData(ts, tsid, units, platform, tm.getMediumId());
			}
		}
		catch(Exception ex)
		{
			String emsg = module + " Error storing TS data: " + ex;
			Logger.instance().warning(emsg);
			PrintStream ps = Logger.instance().getLogOutput();
			if (ps != null)
				ex.printStackTrace(ps);
			else
				ex.printStackTrace(System.err);
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
	 * @param timeseriesDesc CWMS time series descriptor
	 * @param unit the units of the data set
	 * @param Platform current platform in process
	 * @param DCPAddress current DCP in process
	 * @throws DataConsumerException if it fails to insert data
	 */
	private void processAndStoreData(TimeSeries ts, 
		String tsidStr, String units, 
		Platform platform, String DCPAddress) 
		throws DbIoException
	{
		TimeSeriesIdentifier tsid = null;

		TimeSeriesDAI timeSeriesDAO = openTsdb.makeTimeSeriesDAO();
		timeSeriesDAO.setAppModule(routingSpecThread.getRoutingSpec().getName());
		TsDataSource ds = ((OpenTimeSeriesDAO)timeSeriesDAO).getTsDataSource();
		DbKey sourceId = ds.getSourceId();
		
		try
		{
			try
			{
				tsid = timeSeriesDAO.getTimeSeriesIdentifier(tsidStr);
			}
			catch(NoSuchObjectException ex)
			{
				if (!canCreateTs)
				{
					Logger.instance().info(module + " Cannot create time series for path '"
							+ tsidStr + "' -- canCreateTS is false.");
					return;
				}
				Logger.instance().info(module + " No time series for '" + tsidStr
					+ "' -- will attempt to create.");
				tsid = new CwmsTsId();
				try 
				{
					tsid.setUniqueString(tsidStr);
					tsid.setStorageUnits(ts.getUnits());
					tsid.setDescription(tsid.getSiteName() + " - " + tsid.getPart("Param") + " (created by DECODES)");
				}
				catch(BadTimeSeriesException ex2)
				{
					Logger.instance().warning(module + " Cannot create time series -- bad path '"
						+ tsidStr + ": " + ex2);
					return;
				}
				try { timeSeriesDAO.createTimeSeries(tsid); }
				catch(NoSuchObjectException ex2)
				{
					Logger.instance().warning(module + " Cannot create time series for path '"
						+ tsidStr + ": " + ex2);
					return;
				}
				catch(BadTimeSeriesException ex3)
				{
					Logger.instance().warning(module + " Cannot create time series for invalid path '"
						+ tsidStr + ": " + ex3);
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
				sourceId);             // sourceId required for OpenTSDB
			cts.setTimeSeriesIdentifier(tsid);
	
			try
			{
				timeSeriesDAO.saveTimeSeries(cts);
			}
			catch (BadTimeSeriesException ex)
			{
				Logger.instance().failure(module + " Cannot save time series for '"
					+ tsidStr + "': " + ex);
				PrintStream ps = Logger.instance().getLogOutput();
				if (ps != null)
					ex.printStackTrace(ps);
				else
					ex.printStackTrace(System.err);
			}
		}
		finally
		{
			timeSeriesDAO.close();
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
		return "OpenTSDB";
	}

	/**
	  Returns null, this consumer cannot do streaming output.
	*/
	public OutputStream getOutputStream()
	{
		return null;
	}


	/**
	 * This method builds the Cwms Time Series ID descriptor. It
	 * has  six parts: location.param.paramtype.interval.duration.version
	 * 
	 * @param TimeSeries the time series object containing current data
	 * @param Site current site
	 * @return String timeseries descriptor or null if can not build the
	 * 			param part
	 */
	public String buildTSID(TimeSeries ts, Site platformSite)
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
		String paramType = ts.getSensor().getProperty("statcode");
		if (paramType == null)
		{
			paramType = ts.getSensor().getProperty(CwmsConstants.CWMS_PARAM_TYPE);
			if (paramType == null)
				paramType = ts.getSensor().getProperty("paramtype");
		}
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
			intervalStr = ts.getSensor().getProperty("interval");
			if (intervalStr == null)
			{
				if (ts.getSensor().getRecordingMode() == Constants.recordingModeVariable)
				{ // If recording mode is Variable (V), set intervalStr to 0;
					intervalStr = "0";
				}
				else
				{ // Recording mode is Fixed.
					intervalStr = getIntervalValue(ts.getSensor().getRecordingInterval());
				}
			}
		}
		timeSeriesDescriptor.append(intervalStr);
		timeSeriesDescriptor.append(".");
		
		// Find Duration. If it is setup in the sensor property use it.
		String duration = ts.getSensor().getProperty(CwmsConstants.CWMS_DURATION);
		if (duration == null)
		{
			duration = ts.getSensor().getProperty("duration");
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
		}
		timeSeriesDescriptor.append(duration);
		timeSeriesDescriptor.append(".");
		
		// Find Version. "raw" is the default value
		String tempVersion = ts.getSensor().getProperty(CwmsConstants.CWMS_VERSION);
		if (tempVersion == null)
		{
			tempVersion = ts.getSensor().getProperty("version");
			if (tempVersion == null)
				tempVersion = cwmsVersion;
		}
		timeSeriesDescriptor.append(tempVersion);
		
		return timeSeriesDescriptor.toString();
	}
	
	/**
	 * This method fills out the Param "Parameter Element" value 
	 * of the timeseries ID.
	 * @param TimeSeries the time series obj containing current data
	 * @return the cwms data type code or null if no cwms neither shef
	 * 			codes are found
	 */
	private String getParamValue(TimeSeries ts)
	{
		String param = null;
		DataType dt = ts.getSensor().getDataType(dataTypeStandard);
		if (dt != null)
			// Proper type is present, use it directly.
			param = dt.getCode().trim();
		else if (shefCwmsProps != null)
		{
			// If SHEF code is provided, convert from shef to cwms using
			// the values from the text file shefCwmsParam.prop
			dt = ts.getSensor().getDataType(Constants.datatype_SHEF);
			if (dt != null)
				param = PropertiesUtil.getIgnoreCase(shefCwmsProps, dt.getCode().trim());
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
	 * @param intervalInSeconds the recording interval from timeseries obj
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
	 * This method reads all the Shef/Cwms codes mapping found
	 * on the DECODES_INSTALL_DIR/shefCwmsParam.prop file. It 
	 * stores all the properties on the shefCwmsProps Properties 
	 * object class. These properties will be used when creating 
	 * the Cwms Param part timeseries descriptor. If it cannot read
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
		return propSpecs;
	}
	
	@Override
	public String getArgLabel()
	{
		return "DB URI (if external DB):";
	}

}
