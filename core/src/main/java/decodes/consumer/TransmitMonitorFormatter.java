/*
*  $Id$
*
*  $Log$
*  Revision 1.2  2014/05/28 13:09:29  mmaloney
*  dev
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.1  2008/04/04 18:20:59  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2008/02/10 20:17:32  mmaloney
*  dev
*
*  Revision 1.2  2008/02/10 19:59:02  cvs
*  dev
*
*  Revision 1.1.1.1  2008/01/28 22:06:03  cvs
*  Imported from open source.
*
*  Revision 1.4  2004/08/24 21:01:38  mjmaloney
*  added javadocs
*
*  Revision 1.3  2003/11/19 16:16:20  mjmaloney
*  Always format BV with 3 decimal places.
*
*  Revision 1.2  2003/03/06 18:49:47  mjmaloney
*  Fixed DR 113 TransmitMonitor formatter problems.
*
*  Revision 1.1  2002/10/31 18:53:52  mjmaloney
*  release prep
*
*/
package decodes.consumer;

import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.text.NumberFormat;

import ilex.var.Variable;
import ilex.var.NoConversionException;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import ilex.util.Logger;

import decodes.db.*;
import decodes.decoder.DecodedMessage;
import decodes.decoder.TimeSeries;
import decodes.decoder.Sensor;
import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.datasource.GoesPMParser;
import decodes.util.PropertySpec;

/**
Formats the GOES header into a simple space-delimited row-column format,
with one line per DCP message.
*/
public class TransmitMonitorFormatter extends OutputFormatter
{
	private String delimiter;
	private SimpleDateFormat dateFormat;
	private String columns;
	private String colarray[];
	private boolean justify;
	private int colwidths[];
	private NumberFormat bvFormat;
	
	private static PropertySpec propSpecs[] = 
	{
		new PropertySpec("delimiter", PropertySpec.STRING, 
			"(default=space) delimits columns in output"),
		new PropertySpec("columns", PropertySpec.STRING, 
			"Comma-separated list of columns to include. Default is date/time followed by GOES header columns."),
		new PropertySpec("colwidths", PropertySpec.STRING, 
			"Comma-separated list of column widths. Columns are padded with blanks to specified width."),
		new PropertySpec("justify", PropertySpec.BOOLEAN, 
			"(default=false) If true, justify data in specified column width. Negative width=left justify. Positive=right.")
		
	};

	/** default constructor */
	public TransmitMonitorFormatter()
	{
		super();
		delimiter = " ";
		dateFormat = new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss");
		columns =
			GoesPMParser.MESSAGE_TIME + " " +
			"id" + " " +
			"name" + " " +
			GoesPMParser.FAILURE_CODE + " " +
			GoesPMParser.SIGNAL_STRENGTH + " " +
			GoesPMParser.MESSAGE_LENGTH + " " +
			GoesPMParser.CHANNEL + " " +
			GoesPMParser.FREQ_OFFSET + " " +
			GoesPMParser.MOD_INDEX + " " +
			GoesPMParser.QUALITY + " " +
			"batt";

		colarray = null;
		colwidths = new int[] { 19, 8, 10, 1, 2, 5, 3, 2, 2, 2, 5 };
		justify = true;

		bvFormat = NumberFormat.getNumberInstance();
		bvFormat.setGroupingUsed(false);
		bvFormat.setMinimumFractionDigits(3);
		bvFormat.setMaximumFractionDigits(3);
		bvFormat.setGroupingUsed(false);
	}

	/**
	  Initializes the Formatter. This method is called from the static
	  makeOutputFormatter method in this class. The RoutingSpec does not
	  need to call it explicitly.
	  @param type the type of this output formatter.
	  @param tz the time zone as specified in the routing spec.
	  @param presGrp The presentation group to handle rounding &amp; EU conversions.
	  @param rsProps the routing-spec properties.
	*/
	protected void initFormatter(String type, java.util.TimeZone tz,
		PresentationGroup presGrp, Properties rsProps)
		throws OutputFormatterException
	{
		String d = PropertiesUtil.getIgnoreCase(rsProps, "delimiter");
		if (d != null)
			delimiter = d;
		Calendar cal = Calendar.getInstance(tz);
		dateFormat.setCalendar(cal);

		String s = PropertiesUtil.getIgnoreCase(rsProps, "columns");
		if (s != null)
			columns = s;

		StringTokenizer st = new StringTokenizer(columns, " ,\t:");
		colarray = new String[st.countTokens()];
		for(int i=0; st.hasMoreTokens(); i++)
			colarray[i] = st.nextToken();

		s = PropertiesUtil.getIgnoreCase(rsProps, "colwidths");
		if (s != null)
		{
			colwidths = new int[st.countTokens()];
			st = new StringTokenizer(columns, " ,\t:");
			for(int i=0; st.hasMoreTokens(); i++)
			{
				try { colwidths[i] = Integer.parseInt(st.nextToken()); }
				catch(NumberFormatException ex)
				{
					Logger.instance().log(Logger.E_WARNING,
						"Invalid width in position " + i + 
						", 'colwidths' property must be an array of numbers.");
					colwidths[i] = 10;
				}
			}
		}

		s = PropertiesUtil.getIgnoreCase(rsProps, "justify");
		if (s != null && 
			(s.equalsIgnoreCase("false") || s.equalsIgnoreCase("no")
			 || s.equalsIgnoreCase("off")))
			justify = false;
	}

	/** Does nothing. */
	public void shutdown()
	{
	}

	/**
	  Writes the passed DecodedMessage to the passed consumer, using
	  a concrete format.
	  @param msg The message to output.
	  @param consumer The DataConsumer to output to.
	  @throws OutputFormatterException if there was a problem formatting data.
	  @throws DataConsumerException, passed through from consumer methods.
	*/
	public void formatMessage(DecodedMessage msg, DataConsumer consumer)
		throws DataConsumerException, OutputFormatterException
	{
		consumer.startMessage(msg);

		StringBuffer sb = new StringBuffer();
		RawMessage rawmsg = msg.getRawMessage();

		String id = "??";
		String nm = "??";
		try
		{
			TransportMedium tm = rawmsg.getTransportMedium();
			id = tm.getMediumId();
			Platform p = rawmsg.getPlatform();
			nm = p.getSiteName(false);
		}
		catch(UnknownPlatformException e)
		{
			nm = id;
		}

		for(int i=0; i<colarray.length; i++)
		{
			String colval = "";
			if (colarray[i].equalsIgnoreCase("time"))
				colval = dateFormat.format(rawmsg.getTimeStamp());
			else if (colarray[i].equalsIgnoreCase("id"))
				colval = id;
			else if (colarray[i].equalsIgnoreCase("name"))
				colval = nm;
			else if (colarray[i].equalsIgnoreCase("batt"))
			{
				colval = "??";

				// Find sensor named 'batt' or with data type VB or equiv.
				TimeSeries batt_ts = null;
				for(Iterator it = msg.getAllTimeSeries(); 
					it != null && it.hasNext(); )
				{
					TimeSeries ts = (TimeSeries)it.next();
					Sensor sensor = ts.getSensor();

					if (sensor.getName().toLowerCase().startsWith("batt")
						 && ts.size() > 0)
					{
						batt_ts = ts;
						break;
					}
				}
				if (batt_ts == null)
				{
					// No match for sensor name starting with "batt".
					// Look for data type of SHEF VB
					DataType bv=
						DataType.getDataType(Constants.datatype_SHEF,"VB");
					for(Iterator it = msg.getAllTimeSeries(); 
						it != null && it.hasNext(); )
					{
						TimeSeries ts = (TimeSeries)it.next();
						Sensor sensor = ts.getSensor();

						if (bv.isEquivalent(sensor.getDataType())
						 && ts.size() > 0)
						{
							batt_ts = ts;
							break;
						}
					}
				}
				if (batt_ts != null)
				{
					// Take the most recent sample
					batt_ts.sort();
					try
					{
						double bv = 
							batt_ts.sampleAt(batt_ts.size()-1).getDoubleValue();
						colval = bvFormat.format(bv);
					}
					catch(NoConversionException ex)
					{
						colval="??";
					}
				}
			}
			else // try to get named performance measurement & print it.
			{
				Variable v = rawmsg.getPM(colarray[i]);
				if (v == null)
				{
					Logger.instance().log(Logger.E_WARNING,
						"Message from platform " + nm + "(" + id 
						+ ") does not have performance measurement '"
						+ colarray[i] + "'");
				}
				else
					colval = v.toString();
			}

			if (justify == false)
				sb.append(colval);
			else
			{
				int width = i < colwidths.length ? colwidths[i] : 10;
				if (width < 0)
					sb.append(TextUtil.setLengthLeftJustify(colval, -width));
				else
					sb.append(TextUtil.setLengthRightJustify(colval, width));
			}

			if (i < colarray.length-1)
				sb.append(delimiter);
		}

		consumer.println(sb.toString());

		consumer.endMessage();
	}

	/** Allow this format to work on non-decoded platforms. */
	public boolean requiresDecodedMessage() { return false; }

	/** All this format to work on DAPS status messages. */
	public boolean acceptRealDcpMessagesOnly() { return false; }
	
	@Override
	public PropertySpec[] getSupportedProps()
	{
		return propSpecs;
	}

}

