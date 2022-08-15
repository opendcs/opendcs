package opendcs.opentsdb.hydrojson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import decodes.datasource.DataSourceException;
import decodes.datasource.DataSourceExec;
import decodes.datasource.GoesPMParser;
import decodes.datasource.HeaderParseException;
import decodes.datasource.PMParser;
import decodes.datasource.RawMessage;
import decodes.db.Constants;
import decodes.db.DatabaseException;
import decodes.db.DecodesScript;
import decodes.db.IncompleteDatabaseException;
import decodes.db.InvalidDatabaseException;
import decodes.db.Platform;
import decodes.db.PlatformConfig;
import decodes.db.ScriptSensor;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.db.TransportMedium;
import decodes.dbeditor.TraceLogger;
import decodes.decoder.DecodedMessage;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.util.DecodesSettings;
import ilex.util.Logger;
import ilex.util.TextUtil;
import opendcs.opentsdb.hydrojson.beans.DataSourceRef;
import opendcs.opentsdb.hydrojson.beans.ApiDecodedMessage;
import opendcs.opentsdb.hydrojson.beans.ApiLogMessage;
import opendcs.opentsdb.hydrojson.beans.ApiPlatformConfig;
import opendcs.opentsdb.hydrojson.beans.DecodesRouting;
import opendcs.opentsdb.hydrojson.beans.ApiRawMessage;
import opendcs.opentsdb.hydrojson.beans.RoutingRef;
import opendcs.opentsdb.hydrojson.dao.DataSourceDAO;
import opendcs.opentsdb.hydrojson.dao.RoutingDAO;
import opendcs.opentsdb.hydrojson.dao.TsDAO;
import opendcs.opentsdb.hydrojson.errorhandling.WebAppException;

/**
 * This class runs DECODES routing spec in a sub-program for retrieving raw
 * messages and doing test decoding.
 */
public class DecodesRunner
{
	/**
	 * Synchronize static to prevent multiple clients using the apiRawMsgRs routing spec at the
	 * same time.
	 */
	public synchronized static ApiRawMessage getRawMessage(String tmtype, String tmid, TsdbInterface dbi)
		throws DbIoException, WebAppException
	{
		try(RoutingDAO dao = new RoutingDAO(dbi.getTheDb());
			TsDAO tsDao = new TsDAO(dbi.getTheDb());
			DataSourceDAO dsDao = new DataSourceDAO(dbi.getTheDb()))
		{
			ArrayList<RoutingRef> routingRefs = dao.getRoutingRefs();
			DecodesRouting apiRawMsgRs = null;
			for(RoutingRef rr : routingRefs)
				if (rr.getName().equalsIgnoreCase("apiRawMsgRs"))
				{
					try
					{
						apiRawMsgRs = dao.getRouting(rr.getRoutingId());
					}
					catch (WebAppException ex)
					{
						// This will mean NO Such Object
					}
					break;
				}
			if (apiRawMsgRs == null)
			{
				System.out.println("No 'apiRawMsgRs' routing spec. Will create.");
				apiRawMsgRs = new DecodesRouting();
				apiRawMsgRs.setName("apiRawMsgRs");
			}		

			apiRawMsgRs.setDestinationType("pipe");
			apiRawMsgRs.setOutputFormat("raw");
			apiRawMsgRs.setSince("now - 24 hours");
			apiRawMsgRs.setUntil("now");
			apiRawMsgRs.getPlatformIds().clear();
			apiRawMsgRs.getPlatformIds().add(tmid);
			
			apiRawMsgRs.setGoesSelfTimed(false);
			apiRawMsgRs.setGoesRandom(false);
			apiRawMsgRs.setNetworkDCP(false);
			apiRawMsgRs.setIridium(false);
			if (tmtype != null && tmtype.toLowerCase().contains("self"))
				apiRawMsgRs.setGoesSelfTimed(true);
			else if (tmtype != null && tmtype.toLowerCase().contains("random"))
				apiRawMsgRs.setGoesRandom(true);
			else if (tmtype != null && tmtype.toLowerCase().contains("dcp"))
				apiRawMsgRs.setNetworkDCP(true);
			else if (tmtype != null && tmtype.toLowerCase().contains("iridium"))
				apiRawMsgRs.setIridium(true);
			
			Properties tsdbProps = tsDao.getTsdbProperties();
			String dsName = tsdbProps.getProperty("api.datasource");
			ArrayList<DataSourceRef> dataSourceRefs = dsDao.readDataSourceRefs();
			Long dsId = null;
			for(DataSourceRef dsr : dataSourceRefs)
				if ((dsName == null && dsr.getType().toLowerCase().equals("lrgs"))
				 || (dsName != null && dsName.equalsIgnoreCase(dsr.getName())))
				{
					dsId = dsr.getDataSourceId();
					break;
				}
			
			if (dsId == null)
			{
				throw new WebAppException(ErrorCodes.BAD_CONFIG,
					"No usable LRGS datasource: Define 'api.datasource' in TSDB properties.");
			}
			apiRawMsgRs.setDataSourceId(dsId);
			
			dao.writeRouting(apiRawMsgRs);
			
			String dcstoolHome = tsdbProps.getProperty("api.dcstool_home");
			if (dcstoolHome == null)
				throw new WebAppException(ErrorCodes.BAD_CONFIG,
					"Cannot run DECODES because 'api.dcstool_home' is undefined in TSDB properties.");
			String cmd = dcstoolHome + "/bin/rs apiRawMsgRs";
			try
			{
				String cmdRet = runCmd(cmd);
				// The command may return multiple messages in reverse time order. Get only
				// the first one.
				if (cmdRet.length() > 38)
				{
					String lenField = cmdRet.substring(32, 37);
					try
					{
						int dataLen = Integer.parseInt(lenField);
						cmdRet = cmdRet.substring(0, 37 + dataLen);
					}
					catch(NumberFormatException ex)
					{
						Logger.instance().warning("Cannot parse length field '" + lenField
							+ "' -- will return entire cmd results.");
					}
				}
				ApiRawMessage ret = new ApiRawMessage();
				ret.setBase64(new String(Base64.getEncoder().encodeToString(cmdRet.getBytes())));
				return ret;
			}
			catch (Exception ex)
			{
				throw new WebAppException(ErrorCodes.IO_ERROR,
					"Cannot execute command '" + cmd + "': " + ex);
			}
			
		}
	}
	
	/**
	* Run a command and return its standard output as a string.
	*/
	private static String runCmd(String cmd)
		throws IOException
	{
		Process proc = Runtime.getRuntime().exec(cmd);

		// Start a separate thread to read the input stream.
		final InputStream is = proc.getInputStream();
		final ByteArrayOutputStream cmdOut = new ByteArrayOutputStream();
		Thread isr = 
			new Thread()
			{
				public void run()
				{
					try
					{
						int n;
						byte buf[] = new byte[1024];
						while((n = is.read(buf)) > 0)
							cmdOut.write(buf, 0, n);
					}
					catch(IOException ex) {}
				}
			};
		isr.start();

		// Likewise for the stderr stream
		final InputStream es = proc.getErrorStream();
		Thread esr =
			new Thread()
			{
				public void run()
				{
					try
					{
						byte buf[] = new byte[1024];
						int n = es.read(buf);
						if (n > 0)
							Logger.instance().warning(
								"cmd(" + cmd + ") stderr returned(" + n + ") '"
								+ new String(buf, 0, n) + "'");
					}
					catch(IOException ex) {}
				}
			};
		esr.start();
 
		// Finally, wait for process and catch its exit code.
		try
		{
            int exitStatus = proc.waitFor();
            // Race-condition, after process ends, wait a sec for
            // reads in isr & esr above to finish.
            Thread.sleep(1000L);
			if (exitStatus != 0)
				Logger.instance().warning("cmd(" + cmd + ") exit status "
					+ exitStatus);
        }
        catch(InterruptedException ex)
        {
        }
		
		return cmdOut.toString();
	}

	public synchronized static ApiRawMessage getRawMessage2(String tmtype, 
		String tmid, TsdbInterface dbi)
		throws DbIoException, WebAppException
	{
		DataSourceExec dsExec = null;
		try(TsDAO tsDao = new TsDAO(dbi.getTheDb());
			DataSourceDAO dsDao = new DataSourceDAO(dbi.getTheDb()))
		{
			Properties tsdbProps = tsDao.getTsdbProperties();
			String dsName = tsdbProps.getProperty("api.datasource");
			
			ArrayList<DataSourceRef> dataSourceRefs = dsDao.readDataSourceRefs();
			Long dsId = null;
			// If api.datasource specified, find it's ID.
			// Else find the first LRGS data source in the list.
			for(DataSourceRef dsr : dataSourceRefs)
				if ((dsName == null && dsr.getType().toLowerCase().equals("lrgs"))
				 || (dsName != null && dsName.equalsIgnoreCase(dsr.getName())))
				{
					dsId = dsr.getDataSourceId();
					break;
				}
			if (dsId == null)
				throw new WebAppException(ErrorCodes.BAD_CONFIG,
					"No usable LRGS datasource: Define 'api.datasource' in TSDB properties.");

			decodes.db.DataSource ds = new decodes.db.DataSource();
			ds.forceSetId(DbKey.createDbKey(dsId));
			dbi.getSqlDbIo().readDataSource(ds);
System.out.println("Read data source id=" + dsId + ", name='" + ds.getName() + "' type='" + ds.dataSourceType + "'");
			dsExec = ds.makeDelegate();
			Properties rsProps = new Properties();
			rsProps.setProperty("sc:DCP_ADDRESS", tmid);
			rsProps.setProperty("single", "true");
			if (tmtype != null)
			{
				String t = tmtype.toLowerCase();
				if (t.contains("self"))
					rsProps.setProperty("sc:SOURCE", "GOES_SELFTIMED");
				else if (t.contains("rand"))
					rsProps.setProperty("sc:SOURCE", "GOES_RANDOM");
				else if (t.contains("irid"))
					rsProps.setProperty("sc:SOURCE", "IRIDIUM");
				else if (t.contains("netdcp"))
					rsProps.setProperty("sc:SOURCE",  "NETDCP");
			}
			dsExec.setAllowNullPlatform(true);
			dsExec.setAllowDapsStatusMessages(false);


			dsExec.init(rsProps, "now - 64 hours", "now", new Vector<decodes.db.NetworkList>());
			RawMessage rawMsg = dsExec.getRawMessage();
			if (rawMsg == null)
				throw new DataSourceException("dsExec.getRawMessage returned null!");
			ApiRawMessage ret = new ApiRawMessage();
			ret.setBase64(new String(Base64.getEncoder().encodeToString(rawMsg.getData())));
			return ret;
		}
		catch (DataSourceException ex)
		{
			throw new WebAppException(ErrorCodes.IO_ERROR, "Failed to retrieve message: " + ex);
		}
		catch (DatabaseException ex)
		{
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Cannot read data source: " + ex);
		}
		finally
		{
			if (dsExec != null)
				dsExec.close();
		}
	}

	/**
	 * Do a test decode of the passed message data using the named script within the
	 * passed configuration.
	 * @param msgData
	 * @param cfg
	 * @param scriptName
	 * @return
	 */
	public static ApiDecodedMessage decodeMessage(ApiRawMessage msgData, 
		ApiPlatformConfig cfg, String scriptName)
		throws DbIoException, WebAppException
	{
		// Convert base64 back to plaintext & create DECODES RawMessage
		byte[] rawdata = Base64.getDecoder().decode(msgData.getBase64());
		RawMessage rawMessage = new RawMessage(rawdata, rawdata.length);

		// Convert webapp bean to DECODES platform config
		PlatformConfig platformConfig = ApiPlatformConfig.toDecodes(cfg);
		
		// Setup dummy platform to do decoding.
		// Set up dummy platform to do decoding.
		Platform tmpPlatform = new Platform();
		tmpPlatform.setSite(new Site());
		tmpPlatform.getSite().addName(new SiteName(tmpPlatform.getSite(), "USGS", "dummy"));
		tmpPlatform.setConfig(platformConfig);
		tmpPlatform.setConfigName(platformConfig.configName);
		
		// If a script is specified, use it. Else take the first in the config.
		DecodesScript script = null;
		if (scriptName != null)
			script = platformConfig.getScript(scriptName);
		else if (platformConfig.getNumScripts() > 0)
			script = platformConfig.decodesScripts.get(0);
		if (script == null)
			throw new WebAppException(ErrorCodes.BAD_CONFIG, "No such script '" + scriptName
				+ "' within the passed configuration.");

		String mediumType = script.getHeaderType();
System.out.println("script.getHeaderType() returned '" + mediumType + "', scriptType='" + script.scriptType + "'");
		if (mediumType == null)
			mediumType = isGoes(rawdata) ? Constants.medium_Goes :
				isIridium(rawdata) ? Constants.medium_IRIDIUM : Constants.medium_EDL;
System.out.println("Determined mediumType '" + mediumType + "'");
		TransportMedium tmpMedium = new TransportMedium(tmpPlatform, mediumType, "11111111");

		tmpMedium.scriptName = scriptName;
		tmpMedium.setDecodesScript(script);
		tmpPlatform.transportMedia.add(tmpMedium);
		
		ApiDecodedMessage ret = new ApiDecodedMessage();
		TraceLogger traceLogger = new TraceLogger("DecodeTest")
			{
				public void doLog(int priority, String text)
				{
					System.out.println(standardMessage(priority, text));
					ret.getLogMessages().add(
						new ApiLogMessage(new Date(), priorityName[priority], text));
				}
	
			};
		traceLogger.setMinLogPriority(Logger.E_DEBUG3);

		rawMessage.setPlatform(tmpPlatform);
		rawMessage.setTransportMedium(tmpMedium);
		try
		{
			PMParser pmParser = PMParser.getPMParser(mediumType);
			pmParser.parsePerformanceMeasurements(rawMessage);
			traceLogger.info("Header type '" + mediumType 
				+ "' length=" + pmParser.getHeaderLength());
			for(Iterator<String> pmnit = rawMessage.getPMNames(); pmnit.hasNext(); )
			{
				String pmn = pmnit.next();
				traceLogger.info("  PM:" + pmn + "=" + rawMessage.getPM(pmn));
			}
		}
		catch (HeaderParseException ex)
		{
			String tz = DecodesSettings.instance().editTimeZone;
			if (tz == null) tz = "UTC";
			tmpMedium.setTimeZone(tz);
			tmpMedium.setMediumType(Constants.medium_EDL);
			// Set dummy medium id -- rawMessage must have a medium id set
			// to avoid
			// an error in the parser. It doesn't actually need one because
			// the platform and
			// script id is known by context. (SED - 06/11/2008)
			rawMessage.setMediumId("11111111");
			try
			{
				PMParser edlPMParser = PMParser.getPMParser("edl");
				edlPMParser.parsePerformanceMeasurements(rawMessage);
				traceLogger.info("" + ex + " -- will process as EDL file with no header.");
			}
			catch (HeaderParseException ex2)
			{
				throw new WebAppException(ErrorCodes.MISSING_ID, 
					"Cannot parse message header as " + mediumType + " or edl: " + ex2);
			}
		}
		Date timeStamp;
		try
		{
			timeStamp = rawMessage.getPM(GoesPMParser.MESSAGE_TIME).getDateValue();
		}
		catch (Exception ex)
		{
			timeStamp = new Date();
		}
		rawMessage.setTimeStamp(timeStamp);

		try
		{
			script.prepareForExec();
System.out.println("After script.prepare, there are " + script.scriptSensors.size() + " script sensors:");
for(ScriptSensor ss : script.scriptSensors)
{
System.out.println("sensor[" + ss.sensorNumber + "] rawConverter=" + ss.rawConverter + ", execConverter=" + ss.execConverter);
}
			tmpMedium.prepareForExec();
			DecodesScript.trackDecoding = true;
			DecodedMessage dm = script.decodeMessage(rawMessage);
			traceLogger.debug1("After decoding there are " 
				+ script.getDecodedSamples().size() + " decoded samples.");
			ret.fillFromDecodes(dm, script.getDecodedSamples());
		}
		catch (Exception ex)
		{
			throw new WebAppException(ErrorCodes.BAD_CONFIG, "Decoding failed: " + ex);
		}
		
		return ret;
	}
	
	public static boolean isGoes(byte[] msgData)
	{
System.out.println("isGoes(" + new String(msgData) + ")");

		if (msgData.length < 37)
			return false;
		if (!TextUtil.isHexString(new String(msgData, 0, 8)))
			return false;
		for(int i=8; i<8+11; i++)
			if (!Character.isDigit((char)msgData[i]))
				return false;
		return true;
	}
	
	public static boolean isIridium(byte[] msgData)
	{
		return (new String(msgData,0,3)).startsWith("ID=");
	}

}
