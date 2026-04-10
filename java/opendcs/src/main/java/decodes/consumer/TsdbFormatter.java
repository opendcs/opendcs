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
import ilex.var.IFlags;
import ilex.var.TimedVariable;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Properties;
import java.util.TimeZone;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.PresentationGroup;
import decodes.decoder.DecodedMessage;
import decodes.decoder.TimeSeries;
import decodes.util.DecodesSensorCnvt;

/**
 * TsdbFormatter outputs data in the format described for the TsImport program.
 * See the header for decodes.consumer.TsImport for details.
 */
public class TsdbFormatter extends OutputFormatter
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private String module = "TsdbFormatter";
	private String timeFormat = "yyyy/MM/dd-HH:mm:ss";
	private TimeZone tz = null;
	private SimpleDateFormat sdf;
	
	@Override
	protected void initFormatter(String type, TimeZone tz, PresentationGroup presGrp, 
		Properties rsProps)
		throws OutputFormatterException
	{
		this.tz = tz;
		String s = PropertiesUtil.getIgnoreCase(rsProps, "timeFormat");
		if (s != null)
			timeFormat = s;
		sdf = new SimpleDateFormat(timeFormat);
		sdf.setTimeZone(tz);
	}

	@Override
	public void shutdown()
	{
	}

	@Override
	public void formatMessage(DecodedMessage msg, DataConsumer consumer)
		throws DataConsumerException,
		OutputFormatterException
	{
		consumer.startMessage(msg);
		
		consumer.println("SET:TZ=" + tz.getID());
		for(Iterator<TimeSeries> tsit = msg.getAllTimeSeries(); tsit.hasNext(); )
		{
			TimeSeries ts = tsit.next();
			if (ts.size() == 0)
				continue;
			
			ts.sort();
			
			String tsid=null;
			try
			{
				tsid = ((DecodesSensorCnvt)ts.getSensor()).getDbTsId();
			}
			catch(ClassCastException ex)
			{
				log.atWarn().setCause(ex).log(" can only be used for extracting data from database.");
				continue;
			}
			
			consumer.println("SET:UNITS=" + ts.getEU().abbr);
			consumer.println("TSID:" + tsid);
			
			for(int idx = 0; idx < ts.size(); idx++)
			{
				TimedVariable tv = ts.sampleAt(idx);
				if ((tv.getFlags() & (IFlags.IS_ERROR|IFlags.IS_MISSING)) != 0)
					continue;
				
				consumer.println(sdf.format(tv.getTime()) + "," +
					ts.formattedSampleAt(idx) + ",0x" + 
					Integer.toHexString(tv.getFlags()));
			}
		}
		
		consumer.endMessage();
	}
}
