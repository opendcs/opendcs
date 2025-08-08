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

import ilex.var.NoConversionException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.datasource.GoesPMParser;
import decodes.datasource.RawMessage;
import decodes.db.Constants;
import decodes.db.Platform;
import decodes.db.PresentationGroup;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.decoder.DecodedMessage;

/**
 * Generates the NOS NES Format
 */
public class NosNesFormatter extends OutputFormatter
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd yyyy HH:mm:ss");
	public static final String module = "NosNesFormatter";

	public NosNesFormatter()
	{
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	@Override
	protected void initFormatter(String type, TimeZone tz, PresentationGroup presGrp,
			Properties rsProps) throws OutputFormatterException
	{
	}

	@Override
	public void shutdown()
	{
	}

	@Override
	public void formatMessage(DecodedMessage msg, DataConsumer consumer)
			throws DataConsumerException, OutputFormatterException
	{
		RawMessage rawmsg = msg.getRawMessage();
		if (rawmsg == null)
		{
			log.warn(" no raw message!");
			return;
		}

		StringBuilder sb = new StringBuilder();
		try
		{
			sb.append(rawmsg.getTransportMedium().getMediumId());
		}
		catch(Exception ex)
		{
			log.atWarn().setCause(ex).log("Missing DCP Address!");
			sb.append("        ");
		}
		
		try
		{
			sb.append(rawmsg.getPM(GoesPMParser.MESSAGE_TIME).getDateValue());
			char fc = rawmsg.getPM(GoesPMParser.FAILURE_CODE).getCharValue();
			if (fc == 'G' || fc == '?')
				return;
			sb.append(fc);
		}
		catch (NoConversionException ex)
		{
			log.atWarn().setCause(ex).log("Unknown message type skipped!");
			return;
		}

		sb.append(' ');
		
		// This is a DAPS Status message, so there's no station ID and DCP#
		// in the message. The station ID should be the same as the NOS
		// site name. Assume DCP # is 1
		String stationId = "xxxxxxx";
		try
		{
			Platform p = msg.getRawMessage().getPlatform();
			if (p != null)
			{
				Site site = p.getSite();
				if (site != null)
				{
					SiteName nsn = site.getName(Constants.snt_NOS);
					if (nsn != null)
						stationId = nsn.getNameValue();
				}
			}
		}
		catch (Exception ex)
		{
			log.atWarn()
			   .setCause(ex)
			   .log("NES output format cannot associate platform.");
		}
		sb.append(stationId + "1");
		
		sb.append(sdf.format(new Date())); // DECODE time
		sb.append(new String(rawmsg.getMessageData()));
		sb.append(' ');
	
		consumer.println(sb.toString());
	}
	
	@Override
	public boolean usesTZ() { return false; }

}
