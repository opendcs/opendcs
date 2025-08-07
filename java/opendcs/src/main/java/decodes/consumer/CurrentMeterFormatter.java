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

import ilex.util.AsciiUtil;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import ilex.var.IFlags;
import ilex.var.TimedVariable;

import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.db.Constants;
import decodes.db.Platform;
import decodes.db.PresentationGroup;
import decodes.decoder.DecodedMessage;
import decodes.decoder.TimeSeries;

/**
 * 
 * This class outputs the decoded ensemble for real time current meters. Developed for NOS. 
 * @author shweta
 *
 */
public class CurrentMeterFormatter extends OutputFormatter
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	private Column columns[];
	private String delim;	
	private String missing = "";
	private boolean includeGoesHeader = false;

	public CurrentMeterFormatter()
	{
		super();
		delim = "\n";
		columns = null;	
	}

	protected void initFormatter(String type, java.util.TimeZone tz,
		PresentationGroup presGrp, Properties rsProps)
		throws OutputFormatterException
	{
		log.debug("Initializing {}, props='{}'", module, PropertiesUtil.props2string(rsProps));
		
		String s = PropertiesUtil.getIgnoreCase(rsProps, "missing");
		if (s != null)
			missing = s;
		
		s = PropertiesUtil.getIgnoreCase(rsProps, "delim");
		if (s == null)
			s = PropertiesUtil.getIgnoreCase(rsProps, "delimiter");
		if (s != null)
		{
			delim = new String(AsciiUtil.ascii2bin(s));
			log.debug("Delimiter changed to '{}'", delim);
		}

		s = PropertiesUtil.getIgnoreCase(rsProps, "includeGoesHeader");
		if (s != null)
		{
			includeGoesHeader = TextUtil.str2boolean(s);
			log.debug("includeGoesHeader = {}", includeGoesHeader);
		}
	}

	/**
	 * Closes any resources used by this formatter.
	*/
	public void shutdown()
	{
	}

	/**
	* Writes the passed DecodedMessage to the passed consumer, using
	* a concrete format.
	* @throws OutputFormatterException if there was a problem formatting data.
	* @throws DataConsumerException, passed through from consumer methods.
	*/
	public void formatMessage(DecodedMessage msg, DataConsumer consumer)
		throws DataConsumerException, OutputFormatterException
	{
		consumer.startMessage(msg);

		StringBuffer sb = new StringBuffer();
		RawMessage rawmsg = msg.getRawMessage();


		Platform platform;
		try
		{
			platform = rawmsg.getPlatform();
		}
		catch(UnknownPlatformException ex)
		{
			throw new OutputFormatterException("Unable to retrieve platform",ex);
		}

		// Construct column array, not counting the ID & time columns.
		int numColumns = 0;
		
			for(Iterator it = msg.getAllTimeSeries(); it.hasNext(); it.next())
				numColumns++;
			
			if (numColumns == 0)
				throw new OutputFormatterException(
					"Empty time series for platform '" 
					+ platform.makeFileName() + "' -- skipped.");

			columns = new Column[1];

			TimeSeries ts = msg.getTimeSeries(1);
			columns[0] = new Column(ts);
		
		
		Date d;
		while((d = findNextDate()) != null)
		{
			sb.setLength(0);
			
			sb.append(columns[0].nextSample());
			sb.append("\n");		
			consumer.println(sb.toString());
		}
		consumer.endMessage();
	}

	private Date findNextDate()
	{
		Date ret = null;

		for(int i=0; i<columns.length; i++)
		{
			Date d = columns[i].nextSampleTime();
			if (d != null
			 && (ret == null || d.compareTo(ret) < 0))
				ret = d;
		}
		return ret;
	}

	class Column
	{
		TimeSeries timeSeries;
		int curSampleNum;

		Column(TimeSeries ts)
		{
			timeSeries = ts;
			curSampleNum = 0;

			// sort time series into ascending order.
			if (timeSeries != null)
			{
				timeSeries.setDataOrder(Constants.dataOrderAscending);
				timeSeries.sort();
			}
		}
		

		// Return date of next sample in series or null if at end of series.
		Date nextSampleTime()
		{
			if (timeSeries == null)
				return null;
			if (curSampleNum < timeSeries.size())
				return timeSeries.timeAt(curSampleNum);
			return null;
		}

		String nextSample()
		{
			if (timeSeries == null)
				return "";
			if (curSampleNum >= timeSeries.size())
				return "";
			int idx = curSampleNum++;
			TimedVariable tv = timeSeries.sampleAt(idx);
			if ((tv.getFlags() & IFlags.IS_MISSING) != 0
			 || (tv.getFlags() & IFlags.IS_ERROR) != 0)
			{
				return missing;
			}
			return timeSeries.formattedSampleAt(idx).trim();
		}
	}
}

