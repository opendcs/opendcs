package org.opendcs.odcsapi.opendcs_dep;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;

import javax.ws.rs.WebApplicationException;

import org.opendcs.odcsapi.beans.ApiDecodedMessage;
import org.opendcs.odcsapi.beans.ApiDecodesTSValue;
import org.opendcs.odcsapi.beans.ApiDecodesTimeSeries;
import org.opendcs.odcsapi.beans.ApiPlatformConfig;
import org.opendcs.odcsapi.beans.ApiConfigSensor;
import org.opendcs.odcsapi.beans.ApiConfigScript;
import org.opendcs.odcsapi.beans.ApiConfigScriptSensor;
import org.opendcs.odcsapi.beans.ApiRawMessage;
import org.opendcs.odcsapi.beans.ApiScriptFormatStatement;
import org.opendcs.odcsapi.beans.ApiTokenPosition;
import org.opendcs.odcsapi.beans.ApiUnitConverter;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.util.ApiPropertiesUtil;
import org.opendcs.odcsapi.util.ApiTextUtil;

import decodes.datasource.GoesPMParser;
import decodes.datasource.HeaderParseException;
import decodes.datasource.PMParser;
import decodes.datasource.RawMessage;
import decodes.db.ConfigSensor;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.DecodesScript;
import decodes.db.DecodesScriptException;
import decodes.db.FormatStatement;
import decodes.db.Platform;
import decodes.db.PlatformConfig;
import decodes.db.ScriptSensor;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.db.TransportMedium;
import decodes.db.UnitConverterDb;
import decodes.decoder.DecodedMessage;
import decodes.decoder.DecodedSample;
import decodes.sql.DbKey;
import ilex.util.Logger;
import ilex.var.TimedVariable;

public class TestDecoder
{
	/**
	 * Do a test decode of the passed message data using the named script within the
	 * passed configuration.
	 * @param msgData
	 * @param cfg
	 * @param scriptName
	 * @param dbi 
	 * @return
	 */
	public static ApiDecodedMessage decodeMessage(ApiRawMessage msgData, 
		ApiPlatformConfig cfg, String scriptName, DbInterface dbi)
		throws DbException, WebAppException
	{
		final ApiDecodedMessage ret = new ApiDecodedMessage();
		
		// Capture the OpenDCS log messages to return to client.
		Logger origLogger = Logger.instance();
		Logger traceLogger = new TraceLogger(ret.getLogMessages());
		traceLogger.setMinLogPriority(Logger.E_DEBUG3);
		Logger.setLogger(traceLogger);

		// Convert base64 back to plaintext & create DECODES RawMessage
		byte[] rawdata = Base64.getDecoder().decode(msgData.getBase64());
		RawMessage rawMessage = new RawMessage(rawdata, rawdata.length);

		// Convert webapp bean to DECODES platform config
		PlatformConfig platformConfig = apiConfig2decodesConfig(cfg);

		// Setup dummy platform to do decoding.
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
		traceLogger.debug1("script.getHeaderType() returned '" + mediumType + "', scriptType='" 
			+ script.scriptType + "'");
		if (mediumType == null)
			mediumType = isGoes(rawdata) ? Constants.medium_Goes :
				isIridium(rawdata) ? Constants.medium_IRIDIUM : Constants.medium_EDL;
		traceLogger.debug1("Determined mediumType '" + mediumType + "'");
		TransportMedium tmpMedium = new TransportMedium(tmpPlatform, mediumType, "11111111");

		tmpMedium.scriptName = scriptName;
		tmpMedium.setDecodesScript(script);
		tmpPlatform.transportMedia.add(tmpMedium);

		rawMessage.setPlatform(tmpPlatform);
		rawMessage.setTransportMedium(tmpMedium);
		try
		{
			try
			{
				CompRunner.initDecodes(TsdbManager.makeTsdb(dbi));
				PMParser pmParser = PMParser.getPMParser(mediumType);
				if (pmParser == null)
				{
					System.out.println("Cannot get pmParser for mediumType '" + mediumType + "'");
					throw new WebAppException(ErrorCodes.BAD_CONFIG, 
						"Cannot get pmParser for mediumType '" + mediumType + "'");
				}
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
				String tz = ApiPropertiesUtil.getIgnoreCase(DbInterface.decodesProperties, "editTimeZone");
				if (tz == null) 
					tz = "UTC";
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
				traceLogger.debug1("After script.prepare, there are " 
					+ script.scriptSensors.size() + " script sensors:");
				for(ScriptSensor ss : script.scriptSensors)
					traceLogger.debug1("sensor[" + ss.sensorNumber + "] rawConverter=" 
						+ ss.rawConverter + ", execConverter=" + ss.execConverter);
				tmpMedium.prepareForExec();
				DecodesScript.trackDecoding = true;
				DecodedMessage dm = script.decodeMessage(rawMessage);
				traceLogger.debug1("After decoding there are " 
					+ script.getDecodedSamples().size() + " decoded samples.");
				decodedMsg2api(ret, dm, script.getDecodedSamples());
			}
			catch (Exception ex)
			{
				throw new WebAppException(ErrorCodes.BAD_CONFIG, "Decoding failed: " + ex);
			}
		}
		finally
		{
			Logger.setLogger(origLogger);
		}
		return ret;
	}
	
	public static boolean isGoes(byte[] msgData)
	{
		Logger.instance().debug1("isGoes(" + new String(msgData) + ")");

		if (msgData.length < 37)
			return false;
		if (!ApiTextUtil.isHexString(new String(msgData, 0, 8)))
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
	
	private static PlatformConfig apiConfig2decodesConfig(ApiPlatformConfig apiCfg)
	{
		PlatformConfig ret = new PlatformConfig();
		ret.forceSetId(DbKey.createDbKey(apiCfg.getConfigId()));
		ret.configName = apiCfg.getName();
		ret.description = apiCfg.getDescription();
		ret.numPlatformsUsing = apiCfg.getNumPlatforms();
		
		for(ApiConfigSensor acs : apiCfg.getConfigSensors())
		{
			ConfigSensor cs = new ConfigSensor(ret, acs.getSensorNumber());
			cs.sensorName = acs.getSensorName();
			cs.recordingMode = acs.getRecordingMode();
			cs.recordingInterval = acs.getRecordingInterval();
			cs.timeOfFirstSample = acs.getTimeOfFirstSample();
			if (acs.getAbsoluteMax() != null)
				cs.absoluteMax = acs.getAbsoluteMax();
			if (acs.getAbsoluteMin() != null)
				cs.absoluteMin = acs.getAbsoluteMin();
			ApiPropertiesUtil.copyProps(cs.getProperties(), acs.getProperties());
			for(String std : acs.getDataTypes().keySet())
				cs.getDataTypeVec().add(DataType.getDataType(std, acs.getDataTypes().get(std)));
			cs.setUsgsStatCode(acs.getUsgsStatCode());
			ret.addSensor(cs);		
		}

		for(ApiConfigScript apiScript : apiCfg.getScripts())
		{
//			DecodesScript ds = new DecodesScript(ret, apiScript.getName());
			// 7/18/2023 MJM modified for compatibility with latest opendcs.jar
			DecodesScript ds = null;
			try
			{
				ds = DecodesScript.empty()
					.scriptName(apiScript.getName())
					.platformConfig(ret)
					.build();
			}
			catch (IOException | DecodesScriptException ex)
			{
				throw new WebApplicationException("Unable to process script", ex, 500);
			}

			ds.setDataOrder(apiScript.getDataOrder());
			ds.scriptType = Constants.scriptTypeDecodes;
			if (apiScript.getHeaderType() != null)
				ds.scriptType = ds.scriptType + ":" + apiScript.getHeaderType();
			for(ApiConfigScriptSensor apiScriptSensor : apiScript.getScriptSensors())
			{
				ScriptSensor ss = new ScriptSensor(ds, apiScriptSensor.getSensorNumber());
				if (apiScriptSensor.getUnitConverter() != null)
				{
					ApiUnitConverter apiUC = apiScriptSensor.getUnitConverter();
					ss.rawConverter = new UnitConverterDb("raw", apiUC.getToAbbr());
					ss.rawConverter.algorithm = apiUC.getAlgorithm();
					if (apiUC.getA() != null) ss.rawConverter.coefficients[0] = apiUC.getA();
					if (apiUC.getB() != null) ss.rawConverter.coefficients[1] = apiUC.getB();
					if (apiUC.getC() != null) ss.rawConverter.coefficients[2] = apiUC.getC();
					if (apiUC.getD() != null) ss.rawConverter.coefficients[3] = apiUC.getD();
					if (apiUC.getE() != null) ss.rawConverter.coefficients[4] = apiUC.getE();
					if (apiUC.getF() != null) ss.rawConverter.coefficients[5] = apiUC.getF();
				}
				ds.addScriptSensor(ss);
			}
			
			for(ApiScriptFormatStatement apiScriptFmt : apiScript.getFormatStatements())
			{
				FormatStatement fs = new FormatStatement(ds, apiScriptFmt.getSequenceNum());
				fs.label = apiScriptFmt.getLabel();
				fs.format = apiScriptFmt.getFormat();
				ds.getFormatStatements().add(fs);
			}
			
			ret.addScript(ds);
		}

		return ret;
	}
	
	private static void decodedMsg2api(ApiDecodedMessage adm, DecodedMessage dm, ArrayList<DecodedSample> decodedSamples)
	{
		adm.setMessageTime(dm.getMessageTime());
		for (decodes.decoder.TimeSeries ts : dm.getTimeSeriesArray())
		{
			ApiDecodesTimeSeries adts = new ApiDecodesTimeSeries();
			adts.setSensorNum(ts.getSensorNumber());
			adts.setSensorName(ts.getSensorName());
			adts.setUnits(ts.getUnits());
			for(int i=0; i<ts.size(); i++)
			{
				TimedVariable tv = ts.sampleAt(i);
				ApiDecodesTSValue adtsv = new ApiDecodesTSValue();
				adtsv.setTime(tv.getTime());
				adtsv.setValue(tv.getStringValue());
				adts.getValues().add(adtsv);
				for(Iterator<DecodedSample> dsit = decodedSamples.iterator(); dsit.hasNext(); )
				{
					DecodedSample ds = dsit.next();
					if (ds.getTimeSeries() == ts && ds.getSample() == tv)
					{
						adtsv.setRawDataPosition(
							new ApiTokenPosition(ds.getRawDataPosition().getStart(), ds.getRawDataPosition().getEnd()));
						dsit.remove();
						break;
					}
				}
			}
			adm.getTimeSeries().add(adts);
		}
	}


}
