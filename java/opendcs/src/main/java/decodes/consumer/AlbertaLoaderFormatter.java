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

package decodes.consumer;

import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import ilex.var.IFlags;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Properties;
import java.util.TimeZone;


import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.Constants;
import decodes.db.DataPresentation;
import decodes.db.DataType;
import decodes.db.PresentationGroup;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.decoder.DecodedMessage;
import decodes.decoder.Sensor;
import decodes.decoder.TimeSeries;
import decodes.util.PropertySpec;

/**
 * Output formatter for Alberta Loader
 * @author mmaloney Mike Maloney, Cove Software, LLC
 *
 * Loader format outputs each time series value on a separate line in the format:
 * NNNNNNNN YYYYMMDD HHMMVVVVVVVVSSSSR
 * where
 * - NNNNNNNN is the site name (local name is used unless property "SiteNameType" is present)
 * - YYYYMMDD HHMM is the sample timestamp in the specified timezone (default=MST)
 * - VVVVVVVV is the value with 3 fractional decimal digits, right justified in the field.
 *   thus the value range is from -999.999 to 9999.999.
 * - SSSS is the Newleaf sensor type code, left right justified in the field.
 *   This is type "nl-shef" or if not defined "shef-pe"
 * - R is the result code. It can be 'R' for a normal value, or 'D' for a value that was
 *   deleted because it was out of range.
 */
public class AlbertaLoaderFormatter extends OutputFormatter
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	public final static String module = "AlbertaLoaderFormatter";
	private TimeZone timeZone = TimeZone.getTimeZone("MST");
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd HHmm");
	private String siteNameType = "local";
	private PresentationGroup presGrp = null;
	private NumberFormat numberFormat = null;
	private boolean trailer = true;


	private static PropertySpec propSpecs[] = 
	{
		new PropertySpec(Constants.enum_SiteName, 
			PropertySpec.DECODES_ENUM + Constants.enum_SiteName, 
			"(default='local' specify site name type to use in output"),
		new PropertySpec("trailer", PropertySpec.BOOLEAN, 
			"(default=true) Add the 2-line trailer to the end of the file.")
	};
	
	public AlbertaLoaderFormatter()
	{
		ofPropSpecs = propSpecs;
		numberFormat = NumberFormat.getNumberInstance();
		numberFormat.setMaximumFractionDigits(3);
		numberFormat.setMinimumFractionDigits(3);
		numberFormat.setGroupingUsed(false);
	}

	@Override
	protected void initFormatter(String type, TimeZone tz, PresentationGroup presGrp, Properties rsProps)
		throws OutputFormatterException
	{
		if (tz != null)
			timeZone = tz;
		dateFormat.setTimeZone(timeZone);
		String p = PropertiesUtil.getIgnoreCase(rsProps, Constants.enum_SiteName);
		if (p != null)
			siteNameType = p.trim();
		this.presGrp = presGrp;
		p = PropertiesUtil.getIgnoreCase(rsProps, "trailer");
		if (p != null)
			trailer = TextUtil.str2boolean(p);
	}

	@Override
	public void shutdown()
	{
	}

	@Override
	public void formatMessage(DecodedMessage msg, DataConsumer consumer)
		throws DataConsumerException, OutputFormatterException
	{
		consumer.startMessage(msg);

		for(Iterator<TimeSeries> tsit = msg.getAllTimeSeries(); tsit.hasNext(); )
		{
			TimeSeries ts = tsit.next();
			if (ts.size() == 0)
				continue;

			Sensor sensor = ts.getSensor();
			Site site = sensor.getSite(); // Will return platform site if none defined for sensor.
			SiteName siteName = site.getName(siteNameType);
			if (siteName == null)
				siteName = site.getPreferredName();

			DataType dataType = sensor.getDataType("nl-shef");
			if (dataType == null)
				dataType = sensor.getDataType("shef-newleaf");
			if (dataType == null)
			{
				DataType shefpe = sensor.getDataType("w-shef");
				if (shefpe != null)
					dataType = shefpe.findEquivalent("nl-shef");
			}
			if (dataType == null)
			{
				DataType shefpe = sensor.getDataType(Constants.datatype_SHEF);
				if (shefpe != null)
				{
					if ((dataType = shefpe.findEquivalent("nl-shef")) == null)
						dataType = shefpe;
				}
			}
			if (dataType == null)
			{
				log.warn("Cannot convert datatype {} to nl-shef -- sensor skipped.", sensor.getDataType());
				continue;
			}

			if (presGrp != null)
			{
				DataPresentation dataPres = presGrp.findDataPresentation(dataType);
				if (dataPres == null)
				{
					log.warn("Site {} Skipping sensor '{}' because there is no PresentationGroup({}) element.",
							 siteName.getNameValue(), dataType, presGrp.groupName);
					continue;
				}
			}
			int sz = ts.size();
			StringBuilder line = new StringBuilder();
			for(int i=0; i<sz; i++)
			{
				TimedVariable tv = ts.sampleAt(i);
				if ((tv.getFlags() & (IFlags.IS_MISSING|IFlags.IS_ERROR)) != 0)
					continue;
				line.setLength(0);
				line.append(TextUtil.setLengthLeftJustify(siteName.getNameValue(), 8));
				line.append(" ");
				line.append(dateFormat.format(tv.getTime()));
				char resultCode = 'R';
				try
				{
					double v = tv.getDoubleValue();
					if (v < -999.999 || v > 9999.999)
					{
						v /= 1000.;
						resultCode = 'D';
						continue;
						// As per AESRD direction, don't output 'D' lines.
					}
					line.append(TextUtil.setLengthLeftJustify(
						numberFormat.format(v), 8));
				}
				catch (NoConversionException e)
				{
					log.warn("Site {} Skipping sensor {} at time {} cannot retrieve numeric value.",
							 siteName.getNameValue(), dataType, dateFormat.format(tv.getTime()));
					continue;
				}
				line.append(TextUtil.setLengthLeftJustify(dataType.getCode(), 4));
				line.append(resultCode);

				consumer.println(line.toString());
			}
		}
		if (trailer)
		{
			// Canned trailer
			consumer.println("ZZZ      00000000 00001234.678SSD__");
			consumer.println("ZZZ      00000000 000012345.78SSSSX");
		}
		consumer.endMessage();
	}
}
