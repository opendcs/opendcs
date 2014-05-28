package decodes.consumer;

import ilex.util.Logger;
import ilex.var.NoConversionException;
import ilex.var.Variable;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import lrgs.common.DcpMsg;

import decodes.datasource.GoesPMParser;
import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.db.Constants;
import decodes.db.Platform;
import decodes.db.PresentationGroup;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.decoder.DecodedMessage;
import decodes.decoder.NosDecoder;

/**
 * Generates the NOS NES Format
 */
public class NosNesFormatter extends OutputFormatter
{
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
			Logger.instance().warning(module + " no raw message!");
			return;
		}

		StringBuilder sb = new StringBuilder();
		try
		{
			sb.append(rawmsg.getTransportMedium().getMediumId());
		}
		catch(Exception ex)
		{
			Logger.instance().warning(module + " Missing DCP Address!");
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
		catch (NoConversionException e2)
		{
			Logger.instance().warning("Unknown message type skipped!");
			return;
		}

		sb.append(' ');
		
		// This is a DAPS Status message, so there's no station ID and DCP#
		// in the message. The station ID should be the same as the NOS
		// site name. Assume DCP # is 1
//		sb.append(rawmsg.getPM(NosDecoder.PM_STATION_ID));
//		sb.append(rawmsg.getPM(NosDecoder.PM_DCP_NUM));
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
		catch (Exception e)
		{
			Logger.instance().warning(
				"NES output format cannot associate platform: " + e);
		}
		sb.append(stationId + "1");
		
		sb.append(sdf.format(new Date())); // DECODE time
		sb.append(new String(rawmsg.getMessageData()));
		sb.append(' ');
	
		consumer.printLine(sb.toString());
	}
	
	@Override
	public boolean usesTZ() { return false; }

}
