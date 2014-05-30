package decodes.consumer;

import ilex.util.Logger;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;

import lrgs.common.DcpMsg;

import decodes.datasource.GoesPMParser;
import decodes.datasource.RawMessage;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.PresentationGroup;
import decodes.decoder.DecodedMessage;
import decodes.decoder.NosDecoder;
import decodes.decoder.TimeSeries;

/**
 * Generates the NOS XXX.ANC Format
 */
public class NosAncFormatter extends OutputFormatter
{
	private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd yyyy HH:mm");
	public static final String module = "NosAncFormatter";

	public NosAncFormatter()
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
		
		try
		{
			char fc = rawmsg.getPM(GoesPMParser.FAILURE_CODE).getCharValue();
			if (fc != 'G' && fc != '?')
				return;
		}
		catch (NoConversionException e2)
		{
			return;
		}

		StringBuilder sb = new StringBuilder();
		
		// Go through each sensor. If ancillary output a line.
	  nextSensor:
		for(Iterator<TimeSeries> tsit = msg.getAllTimeSeries(); tsit.hasNext(); )
		{
			TimeSeries ts = tsit.next();
			if (ts.size() == 0)
				continue;
			ts.sort();
			DataType dt = ts.getSensor().getDataType(Constants.datatype_NOS);
			if (dt == null)
				continue;
			if (!NosConsumer.isAncillary(dt.getCode()))
				continue;
			int sensorDcpNum = ts.getSensorNumber() / 100 + 1;
			for(int varidx = 0; varidx < ts.size(); varidx++)
			{
				TimedVariable tv = ts.sampleAt(varidx);
				sb.append(rawmsg.getPM(NosDecoder.PM_STATION_ID));
				sb.append("" + sensorDcpNum);
				sb.append(sdf.format(tv.getTime()));
				sb.append(dt.getCode());
				if (tv.isNumeric()) // single number value
				{
					try 
					{
						sb.append(String.format("%6d", tv.getIntValue()));
						sb.append("            ");
					}
					catch(Exception ex) 
					{
						Logger.instance().warning(module + " bad variable: " + ex);
						continue nextSensor;
					}
				}
				else
				{
					String s = tv.getStringValue();
					StringTokenizer st = new StringTokenizer(s,",");
					int tokidx = 0;
					while(st.hasMoreTokens())
					{
						String t = st.nextToken();
						sb.append(String.format("%6s", t));
						tokidx++;
					}
					for(; tokidx < 3; tokidx++)
						sb.append("      ");
				}
				sb.append("   S");
				consumer.println(sb.toString());
				sb.setLength(0);
			}
		}
	}
	
	@Override
	public boolean usesTZ() { return false; }

}
