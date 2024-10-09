package decodes.consumer;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.var.Variable;

import java.io.File;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

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
		try {
			RawMessage rm = msg.getRawMessage();
			Platform p = rm.getPlatform();
			if (p != null)
			{
				String n = p.getSiteName(false);
				if (n != null)
					rsProps.setProperty("SITENAME", n);
			}
		} catch (UnknownPlatformException e1) {
			Logger.instance().warning(e1.getMessage());
		}
		
		String msg1 = msg.getTimeSeries(1).sampleAt(0).getStringValue();
		
		//String id = msg1.substring(0, 8);
		if(msg1!=null)
		msg.getRawMessage().setPM("NOS_STATION_ID", new Variable(msg1.substring(0, 8)));
		msg.getRawMessage().setPM("NOS_DCP_NUM", new Variable(""));
		
	
		Logger.instance().info("NosConsumer output dir is '" + outputDir.getPath() + "'");
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
		catch (OutputFormatterException e)
		{
			Logger.instance().failure(module + " Cannot write DCP format: " + e);
			e.printStackTrace();
			throw new DataConsumerException(e.toString());
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
		catch (OutputFormatterException e)
		{
			Logger.instance().failure(module + " Cannot write Current Meter format: " + e);
			e.printStackTrace();
			throw new DataConsumerException(e.toString());
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
