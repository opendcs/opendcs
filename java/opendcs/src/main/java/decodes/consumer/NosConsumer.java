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

import java.io.File;
import java.util.Properties;
import java.util.TimeZone;


import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.decoder.DecodedMessage;

/**
 * This is the composit consumer which outputs the several NOS
 * files.
 * It instantiates several OutputFormatter/FileConsumer pairs
 * for the several files required by NOS.
 */
public class NosConsumer extends DataConsumer
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private File outputDir;
	private Properties rsProps;
	public static final String module = "NosConsumer";

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
		log.info("NosConsumer output dir is '{}'", outputDir.getPath());

		FileConsumer fc = null;
		rsProps.setProperty("file.overwrite", "false");
		// Append to XXX.DCP
		try
		{
			fc = new FileConsumer();
			fc.open(outputDir.getPath() + "/XXX.DCP", rsProps);
			fc.startMessage(msg);
			NosDcpFormatter nosDcpFmt = new NosDcpFormatter();
			nosDcpFmt.initFormatter("NosDcpFormatter", TimeZone.getTimeZone("UTC"),
				null, rsProps);
			nosDcpFmt.formatMessage(msg, fc);
			nosDcpFmt.shutdown();
		}
		catch (OutputFormatterException ex)
		{
			throw new DataConsumerException("Cannot write DCP format", ex);
		}
		finally
		{
			if (fc != null)
				fc.close();
			fc = null;
		}
		
		// Append to XXX.ANC
		try
		{
			fc = new FileConsumer();
			fc.open(outputDir.getPath() + "/XXX.ANC", rsProps);
			fc.startMessage(msg);
			NosAncFormatter nosAncFmt = new NosAncFormatter();
			nosAncFmt.initFormatter("NosAncFormatter", TimeZone.getTimeZone("UTC"),
				null, rsProps);
			nosAncFmt.formatMessage(msg, fc);
			nosAncFmt.shutdown();
		}
		catch (OutputFormatterException ex)
		{
			throw new DataConsumerException("Cannot write ANC format.", ex);
		}
		finally
		{
			if (fc != null)
				fc.close();
			fc = null;
		}
		
		// Append to XXX.NES
		try
		{
			fc = new FileConsumer();
			fc.open(outputDir.getPath() + "/XXX.NES", rsProps);
			fc.startMessage(msg);
			NosNesFormatter nosNesFmt = new NosNesFormatter();
			nosNesFmt.initFormatter("NosNesFormatter", TimeZone.getTimeZone("UTC"),
				null, rsProps);
			nosNesFmt.formatMessage(msg, fc);
			nosNesFmt.shutdown();
		}
		catch (OutputFormatterException ex)
		{
			throw new DataConsumerException("Cannot write NES format", ex);
		}
		finally
		{
			if (fc != null)
				fc.close();
			fc = null;
		}
		
		// Append to XXX.QC
		try
		{
			fc = new FileConsumer();
			fc.open(outputDir.getPath() + "/XXX.QC", rsProps);
			fc.startMessage(msg);
			NosQcFormatter nosQcFmt = new NosQcFormatter();
			nosQcFmt.initFormatter("NosQcFormatter", TimeZone.getTimeZone("UTC"),
				null, rsProps);
			nosQcFmt.formatMessage(msg, fc);
			nosQcFmt.shutdown();
		}
		catch (OutputFormatterException ex)
		{
			throw new DataConsumerException("Cannot write QC format", ex);
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
