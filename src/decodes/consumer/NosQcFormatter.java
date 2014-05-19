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
 * Generates the NOS XXX.QC Format
 */
public class NosQcFormatter extends OutputFormatter
{
	private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd yyyy HH:mm");
	public static final String module = "NosQcFormatter";

	public NosQcFormatter()
	{
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	@Override
	protected void init(String type, TimeZone tz, PresentationGroup presGrp,
			Properties rsProps) throws OutputFormatterException
	{
	}

	@Override
	public void shutdown()
	{
	}

	@Override
	public void writeMessage(DecodedMessage msg, DataConsumer consumer)
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
		
		// Sudha says that airtemps persist, and should be output for redundant
		// and tsunami sensors.
		int saved_airtemp1      = 999999;
		int saved_airtemp2      = 999999;

		// Go through each sensor. If ancillary output a line.
		for(Iterator<TimeSeries> tsit = msg.getAllTimeSeries(); tsit.hasNext(); )
		{
			TimeSeries ts = tsit.next();
			if (ts.size() == 0)
				continue;
			// MJM Don't sort, use the order in which data was decoded.
			ts.sort(true); // sort in descending order
			DataType dt = ts.getSensor().getDataType(Constants.datatype_NOS);
			if (dt == null)
				continue;
			if (!NosConsumer.isWaterLevel(dt.getCode()))
				continue;
			int sensorDcpNum = ts.getSensorNumber() / 100 + 1;

			for(int varidx = 0; varidx < ts.size(); varidx++)
			{
				TimedVariable tv = ts.sampleAt(varidx);
				sb.append(rawmsg.getPM(NosDecoder.PM_STATION_ID));
				sb.append("" + sensorDcpNum);
				sb.append("  ");
				sb.append(sdf.format(tv.getTime()).toUpperCase());
				sb.append(" ");
				sb.append(dt.getCode());
				sb.append(" S "); // this is all satellite data
				sb.append((tv.getFlags() & NosDecoder.FLAG_REDUNDANT) == 0
					? "P" : "R");

				int pressure      = 999999;
				int prim_wl_value = 999999;
				int prim_wl_sigma = 999999;
				int prim_wl_outli = 999999;
				int back_wl_value = 999999;
				int back_wl_sigma = 999999;
				int back_wl_outli = 999999;
				int back_wl_temp  = 999999;
				int airtemp1      = 999999;
				int airtemp2      = 999999;
				int datum_offset  = 999999;
				int sensor_offset = 999999;
				int back_wl_gain  = 999999;
				int back_wl_offset= 999999;
				try
				{
// Sudha says that even B sensors go into the PWL columns
//					if (dt.getCode().charAt(0) != 'B') // Not backup = primary
//					{
						if (!tv.isNumeric())
						{
							// should be value,sigma,outliers[,t1,t2]
							// parse and put data in the prim_wl fields
							// If aquatrack(A) or airgap(Q), then the
							// two temperature fields are also present.
							StringTokenizer strtok = 
								new StringTokenizer(tv.getStringValue(),",");
							if (strtok.hasMoreTokens())
								prim_wl_value = Integer.parseInt(strtok.nextToken());
							if (strtok.hasMoreTokens())
								prim_wl_sigma = Integer.parseInt(strtok.nextToken());
							if (strtok.hasMoreTokens())
								prim_wl_outli = Integer.parseInt(strtok.nextToken());
							if (strtok.hasMoreTokens())
								saved_airtemp1 = airtemp1 = Integer.parseInt(strtok.nextToken());
							if (strtok.hasMoreTokens())
								saved_airtemp2 = airtemp2 = Integer.parseInt(strtok.nextToken());
						}
						else // is numeric -- means redundant: just prim_wl_value
						{
							prim_wl_value = tv.getIntValue();
						}
//					}
//					else
//					{
//						if (!tv.isNumeric())
//						{
//							// should be value,sigma,outliers[,t1,t2]
//							// parse and put data in the back_wl fields
//							// If aquatrack(A) or airgap(Q), then the
//							// two temperature fields are also present.
//							StringTokenizer strtok = 
//								new StringTokenizer(tv.getStringValue(),",");
//							if (strtok.hasMoreTokens())
//								back_wl_value = Integer.parseInt(strtok.nextToken());
//							if (strtok.hasMoreTokens())
//								back_wl_sigma = Integer.parseInt(strtok.nextToken());
//							if (strtok.hasMoreTokens())
//								back_wl_outli = Integer.parseInt(strtok.nextToken());
//							if (strtok.hasMoreTokens())
//								airtemp1 = Integer.parseInt(strtok.nextToken());
//							if (strtok.hasMoreTokens())
//								airtemp2 = Integer.parseInt(strtok.nextToken());
//						}
//						else // is numeric -- means redundant.
//						{
//							back_wl_value = tv.getIntValue();
//						}
//					}
					// Datum Offset
					datum_offset = 
						msg.getRawMessage().getPM(
							NosDecoder.PM_DATUM_OFFSET).getIntValue();
					sensor_offset = 
						msg.getRawMessage().getPM(
							NosDecoder.PM_SENSOR_OFFSET).getIntValue();
				}
				catch(Exception ex)
				{
					Logger.instance().warning("NosQcFormatter bad value '"
						+ tv.getStringValue() + "' -- cannot parse.");
					continue;
				}
				
				sb.append(" " + String.format("%6d", pressure));
				sb.append(" " + String.format("%6d", prim_wl_value));
				sb.append(" " + String.format("%6d", prim_wl_sigma));
				sb.append(" " + String.format("%6d", prim_wl_outli));
				sb.append(" " + String.format("%6d", back_wl_value));
				sb.append(" " + String.format("%6d", back_wl_sigma));
				sb.append(" " + String.format("%6d", back_wl_outli));
				sb.append(" " + String.format("%6d", back_wl_temp));

				// Sudha says to copy airtemps for redundant and tsunami records:
				if (airtemp1 == 999999
				 && dt.getCode().charAt(0) != 'B'
				 && (dt.getCode().charAt(0) == 'U'
					 || (tv.getFlags() & NosDecoder.FLAG_REDUNDANT) != 0))
				{
					sb.append(" " + String.format("%6d", saved_airtemp1));
					sb.append(" " + String.format("%6d", saved_airtemp2));
				}
				else
				{
					sb.append(" " + String.format("%6d", airtemp1));
					sb.append(" " + String.format("%6d", airtemp2));
				}

				sb.append(" " + String.format("%6d", datum_offset));
				sb.append(" " + String.format("%6d", sensor_offset));
				sb.append(" " + String.format("%6d", back_wl_gain));
				sb.append(" " + String.format("%6d", back_wl_offset));
				consumer.println(sb.toString());
				sb.setLength(0);
			}
		}
	}
	
	@Override
	public boolean usesTimeZone() { return false; }

}
