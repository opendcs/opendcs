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

import ilex.util.EnvExpander;
import ilex.var.Variable;

import java.io.File;
import java.util.Properties;
import java.util.TimeZone;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.db.Constants;
import decodes.db.Platform;
import decodes.decoder.DecodedMessage;

/**
 * This is the composit consumer which outputs the several NOS
 * files.
 * It instantiates several OutputFormatter/FileConsumer pairs
 * for the several files required by NOS.
 */
public class NosMeterConsumer extends DataConsumer
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private File outputDir;
	private Properties rsProps;
	public static final String module = "NosMeterConsumer";

	@Override
	public void open(String consumerArg, Properties props)
			throws DataConsumerException
	{
		rsProps = props;
		// The consumer arg contains the directory name.
		String ed = EnvExpander.expand(consumerArg);
		outputDir = new File(ed);
		if (!outputDir.isDirectory()
		 && !outputDir.mkdirs())
			throw new DataConsumerException(
				"NosConsumer cannot create output dir '" + ed + "'");
	}

	@Override
	public void close()
	{
		// Do nothing. All work done in startMessage method.
	}

	/**
	 *
	 */
	@Override
	public void startMessage(DecodedMessage msg) throws DataConsumerException
	{
		try
		{
			RawMessage rm = msg.getRawMessage();
			Platform p = rm.getPlatform();
			if (p != null)
			{
				String n = p.getSiteName(false);
				if (n != null)
					rsProps.setProperty("SITENAME", n);
			}
		}
		catch (UnknownPlatformException ex)
		{
			log.atWarn().setCause(ex).log("Unable to retrieve platform.");
		}

		String msg1 = msg.getTimeSeries(1).sampleAt(0).getStringValue();

		if(msg1!=null)
		msg.getRawMessage().setPM("NOS_STATION_ID", new Variable(msg1.substring(0, 8)));
		msg.getRawMessage().setPM("NOS_DCP_NUM", new Variable(""));


		log.info("NosConsumer output dir is '{}'", outputDir.getPath());
		FileConsumer fc = null;
		String filenameTemplate ="";
		rsProps.setProperty("file.overwrite", "false");
		// Append to $Date.transmission
		try
		{
			fc = new FileConsumer();
			 filenameTemplate = outputDir.getPath() + "/$DATE(" + Constants.suffixDateFormat_fmt + ")"+".transmission";

			fc.open(filenameTemplate, rsProps);
			fc.startMessage(msg);
			NosDcpFormatter nosDcpFmt = new NosDcpFormatter();
			nosDcpFmt.initFormatter("NosDcpFormatter", TimeZone.getTimeZone("UTC"),
				null, rsProps);
			nosDcpFmt.formatMessage(msg, fc);
			nosDcpFmt.shutdown();
		}
		catch (OutputFormatterException ex)
		{
			throw new DataConsumerException("Cannot write DCP format.", ex);
		}
		finally
		{
			if (fc != null)
				fc.close();
			fc = null;
		}

		// Append to Site-date.k
		try
		{
			fc = new FileConsumer();
			 filenameTemplate = outputDir.getPath() + "/$SITENAME-$DATE(" + Constants.suffixDateFormat_fmt + ")"+".k";

			fc.open(filenameTemplate, rsProps);
			fc.startMessage(msg);
			CurrentMeterFormatter curMetFmt = new CurrentMeterFormatter();
			curMetFmt.initFormatter("CurrentMeterFormatter", TimeZone.getTimeZone("UTC"),
				null, rsProps);
			curMetFmt.formatMessage(msg, fc);
			curMetFmt.shutdown();
		}
		catch (OutputFormatterException ex)
		{
			throw new DataConsumerException("cannot write Current Meter format.", ex);
		}
		finally
		{
			if (fc != null)
				fc.close();
			fc = null;
		}
	}

	@Override
	public void println(String line)
	{
		// Do nothing. All work done in startMessage method.
	}

	@Override
	public void endMessage()
	{
		// Do nothing. All work done in startMessage method.
	}

	/**
	 * @return true if this data type code is concidered 'ancillary'.
	 */
	public static boolean isAncillary(String dt)
	{
		if (dt == null || dt.length() == 0)
			return false;
		switch(dt.charAt(0))
		{
		// The following list are all different types of water level
		case 'A':
		case 'B':
		case 'N':
		case 'P':
		case 'Q':
		case 'S':
		case 'T':
		case 'U':
		case 'V':
		case 'X':
		case 'Y':
		case 'Z':
			return false;
		default: // everything else is ancillary
			return true;
		}

	}

	public static boolean isWaterLevel(String dt)
	{
		return !isAncillary(dt);
	}
}
