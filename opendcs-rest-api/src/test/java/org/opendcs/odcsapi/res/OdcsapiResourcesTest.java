package org.opendcs.odcsapi.res;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import decodes.datasource.RawMessage;
import decodes.decoder.DecodedMessage;
import decodes.decoder.TimeSeries;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Test;
import org.opendcs.odcsapi.beans.ApiConfigScript;
import org.opendcs.odcsapi.beans.ApiConfigScriptSensor;
import org.opendcs.odcsapi.beans.ApiConfigSensor;
import org.opendcs.odcsapi.beans.ApiDecodedMessage;
import org.opendcs.odcsapi.beans.ApiDecodesTSValue;
import org.opendcs.odcsapi.beans.ApiDecodesTimeSeries;
import org.opendcs.odcsapi.beans.ApiLogMessage;
import org.opendcs.odcsapi.beans.ApiRawMessage;
import org.opendcs.odcsapi.beans.ApiUnitConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opendcs.odcsapi.res.OdcsapiResource.map;

final class OdcsapiResourcesTest
{
	@Test
	void testDecodedMessageMap() throws Exception
	{
		ApiDecodedMessage message = new ApiDecodedMessage();
		message.setMessageTime(Date.from(Instant.parse("2021-07-01T00:00:00Z")));
		ArrayList<ApiLogMessage> logMessages = new ArrayList<>();
		ApiLogMessage logMessage = new ApiLogMessage();
		logMessage.setPriority("High");
		logMessage.setText("Error encountered while reading depth value");
		logMessage.setTimeStamp(Date.from(Instant.parse("2021-07-01T00:00:00Z")));
		logMessages.add(logMessage);
		message.setLogMessages(logMessages);
		ArrayList<ApiDecodesTimeSeries> timeSeries = new ArrayList<>();
		ApiDecodesTimeSeries decodesTimeSeries = new ApiDecodesTimeSeries();
		decodesTimeSeries.setSensorName("Depth Sensor");
		decodesTimeSeries.setUnits("m");
		decodesTimeSeries.setSensorNum(556);
		ArrayList<ApiDecodesTSValue> values = new ArrayList<>();
		ApiDecodesTSValue value = new ApiDecodesTSValue();
		value.setValue("1.0");
		value.setTime(Date.from(Instant.parse("2021-07-01T00:00:00Z")));
		values.add(value);
		decodesTimeSeries.setValues(values);
		message.setTimeSeries(timeSeries);
		ApiRawMessage rawMessage = new ApiRawMessage();
		rawMessage.setBase64(Base64.encodeBase64String("base64_encoded_text".getBytes()));
		rawMessage.setXmitTime(Date.from(Instant.parse("2021-07-01T00:00:00Z")));
		rawMessage.setPlatformId("126484");
		ApiPlatformConfig platformConfig = new ApiPlatformConfig();
		platformConfig.setConfigId(118106L);
		platformConfig.setName("Platform Configuration");
		platformConfig.setNumPlatforms(9);
		platformConfig.setDescription("Platform configuration for the platform");
		ArrayList<ApiConfigSensor> configSensors = new ArrayList<>();
		ApiConfigSensor configSensor = new ApiConfigSensor();
		configSensor.setSensorNumber(556);
		configSensor.setSensorName("Depth Sensor");
		configSensor.setAbsoluteMax(10.0);
		configSensor.setAbsoluteMin(0.0);
		Properties properties = new Properties();
		properties.setProperty("property1", "value1");
		configSensor.setProperties(properties);
		configSensors.add(configSensor);
		platformConfig.setConfigSensors(configSensors);
		ArrayList<ApiConfigScript> configScripts = new ArrayList<>();
		ApiConfigScript configScript = new ApiConfigScript();
		configScript.setName("Script1");
		ArrayList<ApiConfigScriptSensor> scriptSensors = new ArrayList<>();
		ApiConfigScriptSensor scriptSensor = new ApiConfigScriptSensor();
		scriptSensor.setSensorNumber(556);
		ApiUnitConverter converter = new ApiUnitConverter();
		converter.setFromAbbr("m");
		converter.setToAbbr("ft");
		converter.setAlgorithm("Linear");
		converter.setA(3.28084);
		scriptSensor.setUnitConverter(converter);
		scriptSensors.add(scriptSensor);
		configScript.setScriptSensors(scriptSensors);
		platformConfig.setScripts(configScripts);

		DecodedMessage decodedMessage = map(message, rawMessage);

		assertNotNull(decodedMessage);
		assertEquals(message.getMessageTime(), decodedMessage.getMessageTime());
		assertEquals(rawMessage.getBase64().getBytes().length, decodedMessage.getRawMessage().getMessageData().length);
		int i = 0;
		for(Byte b : rawMessage.getBase64().getBytes())
		{
			assertEquals(b, decodedMessage.getRawMessage().getMessageData()[i]);
			i++;
		}
		assertEquals(Long.parseLong(rawMessage.getPlatformId()),
				decodedMessage.getRawMessage().getPlatform().getId().getValue());
		assertEquals(rawMessage.getXmitTime(), decodedMessage.getRawMessage().getTimeStamp());
		for (ApiDecodesTimeSeries timeSeries1 : message.getTimeSeries())
		{
			TimeSeries timeSeries1Decoded = decodedMessage.getTimeSeries(timeSeries1.getSensorNum());
			assertNotNull(timeSeries1Decoded);
			assertEquals(timeSeries1.getSensorName(), timeSeries1Decoded.getSensorName());
			assertEquals(timeSeries1.getUnits(), timeSeries1Decoded.getUnits());
			int j = 0;
			for (ApiDecodesTSValue value1 : timeSeries1.getValues())
			{
				assertEquals(value1.getTime(), timeSeries1Decoded.getBeginTime());
				assertEquals(value1.getValue(), timeSeries1Decoded.sampleAt(j).valueString());
				j++;
			}
		}
	}

	@Test
	void testRawMessageMap() throws Exception
	{
		ApiRawMessage rawMessage = new ApiRawMessage();
		rawMessage.setBase64(Base64.encodeBase64String("base64_encoded_text".getBytes()));
		rawMessage.setXmitTime(Date.from(Instant.parse("2021-07-01T00:00:00Z")));
		rawMessage.setPlatformId("126484");

		RawMessage decodedMessage = map(rawMessage);
		assertNotNull(decodedMessage);
		assertEquals(rawMessage.getBase64().getBytes().length, decodedMessage.getMessageData().length);
		int i = 0;
		for(Byte b : rawMessage.getBase64().getBytes())
		{
			assertEquals(b, decodedMessage.getMessageData()[i]);
			i++;
		}
		assertEquals(Long.parseLong(rawMessage.getPlatformId()), decodedMessage.getPlatform().getId().getValue());
		assertEquals(rawMessage.getXmitTime(), decodedMessage.getTimeStamp());
	}

	@Test
	void testTimeSeriesMap() throws Exception
	{
		ArrayList<ApiDecodesTimeSeries> timeSeries = new ArrayList<>();
		ApiDecodesTimeSeries decodesTimeSeries = new ApiDecodesTimeSeries();
		decodesTimeSeries.setSensorName("Flow Sensor");
		decodesTimeSeries.setUnits("cms");
		decodesTimeSeries.setSensorNum(556);
		ArrayList<ApiDecodesTSValue> values = new ArrayList<>();
		ApiDecodesTSValue value = new ApiDecodesTSValue();
		value.setValue("1.0");
		value.setTime(Date.from(Instant.parse("2021-07-01T00:00:00Z")));
		values.add(value);
		decodesTimeSeries.setValues(values);
		timeSeries.add(decodesTimeSeries);
		ApiPlatformConfig platformConfig = new ApiPlatformConfig();
		platformConfig.setConfigId(118106L);
		platformConfig.setName("Platform Configuration");
		platformConfig.setNumPlatforms(9);
		platformConfig.setDescription("Platform configuration for the platform");
		ArrayList<ApiConfigSensor> configSensors = new ArrayList<>();
		ApiConfigSensor configSensor = new ApiConfigSensor();
		configSensor.setSensorNumber(556);
		configSensor.setSensorName("Depth Sensor");
		configSensor.setAbsoluteMax(10.0);
		configSensor.setAbsoluteMin(0.0);
		Properties properties = new Properties();
		properties.setProperty("property1", "value1");
		configSensor.setProperties(properties);
		configSensors.add(configSensor);
		platformConfig.setConfigSensors(configSensors);
		ArrayList<ApiConfigScript> configScripts = new ArrayList<>();
		ApiConfigScript configScript = new ApiConfigScript();
		configScript.setName("Script1");
		ArrayList<ApiConfigScriptSensor> scriptSensors = new ArrayList<>();
		ApiConfigScriptSensor scriptSensor = new ApiConfigScriptSensor();
		scriptSensor.setSensorNumber(556);
		ApiUnitConverter converter = new ApiUnitConverter();
		converter.setFromAbbr("m");
		converter.setToAbbr("ft");
		converter.setAlgorithm("Linear");
		converter.setA(3.28084);
		scriptSensor.setUnitConverter(converter);
		scriptSensors.add(scriptSensor);
		configScript.setScriptSensors(scriptSensors);
		platformConfig.setScripts(configScripts);

		List<TimeSeries> decodedTimeSeries = map(timeSeries);

		assertNotNull(decodedTimeSeries);
		assertEquals(timeSeries.size(), decodedTimeSeries.size());
		ApiDecodesTimeSeries apiTS = timeSeries.get(0);
		TimeSeries timeSeries1Decoded = decodedTimeSeries.get(0);
		assertNotNull(timeSeries1Decoded);
		assertNotNull(apiTS);
		assertEquals(apiTS.getSensorName(), timeSeries1Decoded.getSensorName());
		assertEquals(apiTS.getUnits(), timeSeries1Decoded.getUnits());
		int i = 0;
		for (ApiDecodesTSValue value1 : apiTS.getValues())
		{
			assertEquals(value1.getTime(), timeSeries1Decoded.getBeginTime());
			assertEquals(value1.getValue(), timeSeries1Decoded.sampleAt(i).valueString());
			i++;
		}
	}
}
