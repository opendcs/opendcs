/*
*  $Id$
*/
package decodes.decoder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;
import java.util.Date;
import java.util.TimeZone;

import ilex.util.Logger;
import ilex.util.ArrayUtil;
import ilex.util.ByteUtil;
import ilex.util.IDateFormat;
import ilex.var.TimedVariable;
import ilex.var.Variable;
import ilex.var.IFlags;

import decodes.db.*;

/**
FieldOperation is a DecodesOperation that extracts and parses a field 
from the data stream. There are several kinds of fields. See the sub-chapter
on fields in the DECODES User Guide for details.
*/
class FieldOperation extends DecodesOperation
{
	/** field-type is the 1st arg inside the parenthesis for the field op */
	private String field_type;

	/** data-type is the 2nd arg inside the parenthesis for the field op */
	private char data_type;

	/** length is the 3rd argument inside the parenthesis for the field op */
	private int field_length;

//	/** length may be optionally folled by a delimiter */
//	private byte field_delimiter;

	/** Any character in this string can serve as a delimiter */
	private String delimiter;

	/** used if necessary to parse numbers */
	private NumberParser numberParser;

	/** the script that this op belongs to */
	private DecodesScript decodesScript;

	/** Used if field is specified as string */
	private String sensorName;

	/** Number of sensor. */
	private int sensorNumber;

	/** Signifies that time is an event. */
	private boolean event;

	/** Set by the 'x' suffix to the sensor number. */
	private boolean supressOutput = false;

	/** Signifies that time is an AM time. */
	private boolean AMSet = false;

	/** Signifies that time is a  PM time. */
	private boolean PMSet = false;

	/** current field **/
	private String currentField;
	
	private SimpleDateFormat loggerDateFmt = 
		new SimpleDateFormat("yyyy MMM/dd HH:mm:ss");
	
	private boolean isBinary = false;
	
	private String literalData = null;

	/** 3-char month abbreviations */
	static final String mn[] = 
	{ "---", "jan", "feb", "mar", "apr", "may", "jun",
	         "jul", "aug", "sep", "oct", "nov", "dec"
	};

	/** @return code for field type operations. */
	public char getType() { return 'f'; }

	/**
	  Constructor.
	  @param  repetitions number of times to repeat this operation
	  @param  args complete string inside the parens
	  @param  ds the DecodesScript that this operation belongs to
	  @throws ScriptFormatException if syntax error detected in arguments
	*/
	public FieldOperation(int repetitions, String args, DecodesScript ds)
		throws ScriptFormatException
	{
		super(repetitions);
		loggerDateFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		currentField = "f("+args+")";
		decodesScript = ds;

		// set defaults
		data_type = 'a';
		field_length = 0;
		sensorNumber = -1;
		delimiter = null;
		event = false;

		FieldArgsTokenizer fat = new FieldArgsTokenizer(args);

		String s = fat.nextToken();
		if ( s == null || s.length() == 0 ) {
			throw new ScriptFormatException(
				"Field operation with no field_type");
		}
		field_type = s.toLowerCase();

		s = fat.nextToken();
		if (s == null || s.length() == 0)
			throw new ScriptFormatException(
				"Field operation with no data-type");
		if (fat.wasDoubleQuoted)
		{
			data_type = NumberParser.ASCII_FMT;
			literalData = s;
		}
		else if (s.length() == 1)
			data_type = s.charAt(0);
		else if (s.equalsIgnoreCase("bc"))
			data_type = NumberParser.CAMPBELL_BINARY_FMT;
		else if (s.equalsIgnoreCase("bt") || s.equalsIgnoreCase("bd"))
			data_type = NumberParser.SIGNBIT_BINARY_FMT;
		else if (s.equalsIgnoreCase("bin"))
		{
			data_type = NumberParser.BIN_SIGNED_MSB;
			isBinary = true;
		}
		else if (s.equalsIgnoreCase("ubin"))
		{
			data_type = NumberParser.BIN_UNSIGNED_MSB;
			isBinary = true;
		}
		else if (s.equalsIgnoreCase("binl"))
		{
			data_type = NumberParser.BIN_SIGNED_LSB;
			isBinary = true;
		}
		else if (s.equalsIgnoreCase("ubinl"))
		{
			data_type = NumberParser.BIN_UNSIGNED_LSB;
			isBinary = true;
		}
		else
			data_type = s.charAt(0);

		s = fat.nextToken();
		if (literalData != null)
			field_length = literalData.length();
		else
		{
			if (s == null)
				throw new ScriptFormatException(
					"Field operation with no length");
			int pos = -1;
			for(pos=0; pos<s.length(); pos++)
			{
				char c = s.charAt(pos);
				if (c == 'd' || c == 'D')
					break;
			}
			if ( pos == -1 )
				pos = s.length();
			try { field_length = Integer.parseInt(s.substring(0,pos)); }
			catch (NumberFormatException nfe)
			{
				throw new ScriptFormatException("Field length must be a number");
			}
	
			if ( pos < s.length() )     // There was a 'd' in the length field
			{
				if ( pos+1 == s.length() ) {
					delimiter = ",";
					s = fat.nextToken();
				} else {
					delimiter =  s.substring(pos+1);
				}
			}
		}
		if ((s = fat.nextToken()) != null)
		{
			sensorName = s;

			int idxX = s.indexOf("x");
			if (idxX == -1) idxX = s.indexOf("X");
			if (idxX != -1)
			{
				s = s.substring(0, idxX);
				supressOutput = true;
			}
			try 
			{
				sensorNumber = Integer.parseInt(s);
				if (sensorNumber < 0)
				{
					// The code uses -1 to mean an error condition,
					// so set the supress flag and make the number positive.
					supressOutput = true;
					sensorNumber = -sensorNumber;
				}
			}
			catch ( NumberFormatException nfe ) 
			{
				sensorNumber = -1;
			}
			if ( field_type.equals("t") )
				event = true;
		}

		if (!field_type.equals("f") && !field_type.equals("r"))
		{
			numberParser = new NumberParser();
			numberParser.setDataType(data_type);
		}
		else
			numberParser = null;

		/* Special processing for delimiters */

		if ( delimiter != null && delimiter.length() == 2 && delimiter.charAt(0) == '\\' ) {
			delimiter = delimiter.substring(1);
			delimiter = delimiter.replace("t","\t");
			delimiter = delimiter.replace("n","\n");
			delimiter = delimiter.replace("r","\r");
		}	
		// If delimiter contains either sign, add the other one.
		if (delimiter != null)
		{
			boolean hasMinus = (delimiter.indexOf('-') >= 0);
			boolean hasPlus  = (delimiter.indexOf('+') >= 0);
			if (hasMinus && !hasPlus)
				delimiter = delimiter + "+";
			else if (hasPlus && !hasMinus)
				delimiter = delimiter + "-";
		}
		// If delimiter is a single 'S' or 's', it means either sign.
		if (delimiter != null
		 && (delimiter.equals("S") || delimiter.equals("s")))
			delimiter = "+=";

		// If delimiter is a hex representation
		if (delimiter != null && delimiter.length() == 3
			&& (delimiter.equals("X") || delimiter.equals("x"))
			&& ByteUtil.isHexChar((byte)delimiter.charAt(1))
			&& ByteUtil.isHexChar((byte)delimiter.charAt(2)))
		{
			int x = Integer.parseInt(delimiter.substring(1,3), 16);
			delimiter = "" + ((char)x);
		}
	}


	/**
	  Executes this operation using the context provided.
	  @param dd holds the raw data and context.
	  @param msg store decoded values here.
	  @throws DecoderException or subclass if error detected.
	*/
	public void execute(DataOperations dd, DecodedMessage msg) 
		throws DecoderException
	{
		if (sensorNumber == -1 && (field_type.equals("s") || event))
			throw new ScriptException("Invalid sensor number in field "+currentField);
//			throw new ScriptException("No time-series for sensor " 
//				+ sensorNumber);

//		Logger.instance().log(Logger.E_DEBUG3,
//			"Executing FieldOperation, type=" + field_type
//			+ ", sensor=" + sensorNumber
//			+ ", repetitions=" + repetitions
//			+ ", length=" + field_length 
//		 	+ ", delim='" + (field_delimiter==0 ? 'N' : (char)field_delimiter) 
//			+ "'");

		boolean isString = (data_type=='S'||data_type=='s');
		for (int n=0; n < repetitions; n++) 
		{
			int fieldStart = dd.getBytePos();
			int lineNum = dd.getCurrentLine();
			byte field[];
			boolean isZformat=false;
			if (literalData != null)
				field = literalData.getBytes();
			
			else if (data_type=='Z'||data_type=='z')
			{
				isZformat=true;
				field = dd.getField( field_length, delimiter, isBinary, isZformat);			
			}
			else if(data_type=='R'|| data_type=='r')
			{
				field = dd.getField(msg.getRawMessage().getData().length+2, delimiter, isBinary, isString);
			}
			else if(data_type=='N'|| data_type=='n')
			{
				field = dd.getField(msg.getRawMessage().getData().length+2, delimiter, isBinary, isString);
			}
			else if(data_type=='K' || data_type == 'k')
			{
				field = dd.getField(msg.getRawMessage().getData().length, delimiter, isBinary, isString);
			}
			else
				field = dd.getField(field_length, delimiter, isBinary, isString);


			if (field != null && numberParser != null)
				Logger.instance().log(Logger.E_DEBUG3,
				"Field Parse: data='" + new String(field) + "', fieldType="
				+ field_type + ", dataType=" + numberParser.getDataType());

			
			if ( is_blank(field) && !isString && !isZformat )
				continue;
			
			if (field_type.equals("s")) // Sensor Field
			{
				Variable v;
				String s = new String(field).trim();

				try
				{
					if (isMissingSymbol(s))
					{
						v = new Variable("m");
						v.setFlags(v.getFlags() | IFlags.IS_MISSING);
						Logger.instance().debug3("found missing symbol '" + s + "'");
					}
					else
					{
						v = numberParser.parseDataValue(field);
						Logger.instance().debug3("field parsed to '" + v + "'");
					}
				}
				catch(FieldParseException ex)
				{
					if(isZformat && s!=null && s.equalsIgnoreCase(""))
						s = "//";
					Logger.instance().debug1("Field Parse Exception: "
						+ ex.getMessage());
					v = new Variable("e");
					v.setFlags(v.getFlags() | IFlags.IS_ERROR);
				}
				if (!supressOutput)
				{
					TimedVariable tv = msg.addSample(sensorNumber, v, lineNum);
					if (tv != null && DecodesScript.trackDecoding)
					{
						DecodedSample ds = new DecodedSample(this, 
							fieldStart, dd.getBytePos(),
							tv, msg.getTimeSeries(sensorNumber));
						formatStatement.getDecodesScript().addDecodedSample(ds);
					}
				}
				RecordedTimeStamp rts = msg.getTimer();
				rts.dayJustSet = false;
			}
			else if (field_type.equals("f")) // Format Label Field
			{
				String label = new String(field);
				Logger.instance().log(Logger.E_DEBUG3,
					"Searching for format label '" + label + "'");
				FormatStatement newFormat = 
					decodesScript.getFormatStatement(label);
				if (newFormat == null)
				{
					// Try to find an error handler
					newFormat = decodesScript.getFormatStatement("ERROR");
					if (newFormat == null)
						throw new FieldParseException("No such format label '"
							+ label + "', and no ERROR statement.");
				}
				throw new SwitchFormatException(newFormat);
			}
			else if (field_type.equals("jdy") || field_type.equals("jdy+")) 
			{
				boolean increment = field_type.equals("jdy+");
				int t = numberParser.parseIntValue(field);
				if (t < 1 || t > 366)
					throw new FieldParseException("Invalid Julian Date: " + t);
				int stat = msg.getTimer().getStatus();
				RecordedTimeStamp rts = msg.getTimer();

				/*
				  The increment flag means that this is the day just ending.
				  Conversely, (!increment) means that this is the day just starting.
				  Therefore, before fixing-up partial dates, decrement day
				  if !increment. Then set it back.
				*/
				if ( t == 366 ) {
					int curYear = rts.getYear();
					GregorianCalendar gc = new GregorianCalendar();
					if ( curYear != 1970 && !gc.isLeapYear(curYear) ) {
						throw new FieldParseException("Found day 366 for non-leap year "+curYear+" - value ignored.");
					}
				}
				rts.setDayOfYear(t);
				if (!increment)
					rts.decrementDay();
				if (stat != rts.getStatus())
					msg.upgradeStoredTimes();
				rts.incrementDay();
				rts.setHour(0);
				rts.setMinute(0);
				rts.setSecond(0);
				rts.dayJustSet = true;
				msg.justGotNonYearField();
			}
			else if ( field_type.equals("dy") )  // Day of Month
			{
				int t = numberParser.parseIntValue(field);
				RecordedTimeStamp rts = msg.getTimer();
				int stat = rts.getStatus();
				if (stat != rts.setDayOfMonth(t))
				{
					// stat can now be SecOfYear or Complete.
					// A new day is starting. Assume already-retrieved times
					// were for yesterday.
					rts.decrementDay();
					msg.upgradeStoredTimes();
					rts.incrementDay();
				}
				rts.dayJustSet = true;
				msg.justGotNonYearField();
			}
			else if ( field_type.equals("yr") ) 
			{
				for(int i=0; i<field.length; i++)
					if (!Character.isDigit((char)field[i]))
						throw new FieldParseException("Bad year value");
				int t = numberParser.parseIntValue(field);
				int stat = msg.getTimer().getStatus();
				if (stat != msg.getTimer().setYear(t))
				{
					// for 'yr' field, 
					// stat can only change from SEC_OF_YEAR to COMPLETE.
					// Assume a new day is starting. Assume already-retrieved
					// Times were for yesterday.
					msg.upgradeStoredTimes();
				}
				msg.setJustGotFullDateTime(true);
			}
			else if (field_type.equals("d") || field_type.equals("d+")) // Date
			{
				parseDate(field, msg, field_type.equals("d+"));
				RecordedTimeStamp rts = msg.getTimer();
				rts.setHour(0);
				rts.setMinute(0);
				rts.setSecond(0);
				rts.dayJustSet = true;
				msg.justGotNonYearField();
				// Note - parseDate does its own upgradeToStoredTimes.
			}
			else if ( field_type.equals("mn") )  // Month Field
			{
				int t = 0;
				if (field.length <= 2)
					t = numberParser.parseIntValue(field);
				else if (field.length >= 3)
					t = indexOfMonth(field);
				int stat = msg.getTimer().getStatus();
				if (stat != msg.getTimer().setMonth(t))
				{
					// stat can no be SecOfYear or Complete.
					// Assume a new day is starting. Assume already-retrieved
					// Times were for yesterday.
					msg.getTimer().decrementDay();
					msg.upgradeStoredTimes();
					msg.getTimer().incrementDay();
				}
				msg.justGotNonYearField();
			}
			else if ( field_type.equals("hr") ) 
			{
				int t = numberParser.parseIntValue(field);
				int stat = msg.getTimer().getStatus();
				if (stat != msg.getTimer().setHour(t))
					msg.upgradeStoredTimes();
				msg.justGotNonYearField();
			}
			else if ( field_type.equals("min") ) 
			{
				int t = numberParser.parseIntValue(field);
				int stat = msg.getTimer().getStatus();
				if (stat != msg.getTimer().setMinute(t))
					msg.upgradeStoredTimes();
				msg.justGotNonYearField();
			}
			else if (field_type.equals("sec") ) 
			{
				int t = numberParser.parseIntValue(field);
				int stat = msg.getTimer().getStatus();
				if (stat != msg.getTimer().setSecond(t))
					msg.upgradeStoredTimes();
				msg.justGotNonYearField();
			}
			else if (field_type.equals("t") ) 
			{
				RecordedTimeStamp rts = msg.getTimer();
				int stat = rts.getStatus();
				long msec = rts.getMsec();
				parseTime(field, rts);
				msg.justGotNonYearField();

				if ( PMSet ) 
					msg.getTimer().setPM(true);
				else if ( AMSet )
					msg.getTimer().setPM(false);
				// Special EDL case - just have time, supposed to be ascending,
				// and time jumps backward. Assume we bump day by one.
				if (stat == RecordedTimeStamp.COMPLETE
				 && msec > rts.getMsec()
				 && decodesScript.getDataOrder()==Constants.dataOrderAscending
				 && !rts.dayJustSet)
					rts.incrementDay();
				rts.dayJustSet = false;

				if (stat != msg.getTimer().getStatus())
					msg.upgradeStoredTimes();

				if (sensorNumber != -1 && event)
					msg.addSample(sensorNumber, new Variable(1.0), lineNum);
				msg.justGotNonYearField();
			}
			else if (field_type.equals("ti") ) 
			{
				if (sensorNumber == -1)
					throw new ScriptFormatException(
						"Time-Interval field must have sensor number");
				try
				{
					int sod = IDateFormat.getSecondOfDay(new String(field));
					msg.setTimeInterval(sensorNumber, sod);
				}
				catch(IllegalArgumentException ex)
				{
					throw new FieldParseException("Bad time interval value");
				}
			}
 			// Minute Interval (positive or negative)
			else if (field_type.equals("mint") 
			      || field_type.equals("mint-") )
			{
				if (sensorNumber == -1)
					throw new ScriptFormatException(
						"Minute-Interval field must have sensor number");
				try
				{
					int m = numberParser.parseIntValue(field);
					if (field_type.equals("mint-") 
					 && decodesScript.getDataOrder() == Constants.dataOrderAscending)
						m = -m;
Logger.instance().log(Logger.E_DEBUG3,
"Setting interval for sensor " + sensorNumber + " to " + (m*60) + " seconds.");
					msg.setTimeInterval(sensorNumber, m*60);
				}
				catch(IllegalArgumentException ex)
				{
					throw new FieldParseException("Bad minute interval value");
				}
			}
			else if (field_type.equals("moff") )  // Minute Offset
			{
				try
				{
					int m = numberParser.parseIntValue(field);

					// Reset 'current' time to 'message' time minus offset.
					Date msgTime = msg.getRawMessage().getTimeStamp();
					if (msgTime == null) msgTime = new Date();
					long msec = msgTime.getTime();
					// moff implies that we truncate to minute boundary
					msec = (msec / 60000L) * 60000L;
					msec -= (m * 60000L);
					Date timeStamp = new Date(msec);
					msg.getTimer().setComplete(timeStamp);
					Logger.instance().log(Logger.E_DEBUG3,
						"Set Minute OFFset to " + m + ", current timer="
						+ loggerDateFmt.format(timeStamp));
				}
				catch(IllegalArgumentException ex)
				{
					throw new FieldParseException("Bad minute interval value");
				}
			}
			else if (field_type.equals("to") ) 
			{
			}
			else if (field_type.equals("a") ) 
			{
				char c = (char)field[0];
				if (c == 'A' || c == 'a')  // AM
					msg.getTimer().setPM(false);
				else if (c == 'P' || c == 'p')  // PM
					msg.getTimer().setPM(true);
			}
		}
	}


	private boolean is_blank(byte f[])
	{
		int i;
		for(i=0; i < f.length; i++ ) 
		{
			if ( f[i] != (byte)' ' )
				return(false);
		}
		return(true);
	}

	/** Debug method dumps field data to stdout. */
  	void dump_field(byte[] field)
  	{
          int i;

          for (i=0; i < field.length; i++ )
            System.out.print((char)field[i]);
          System.out.println("");
          System.out.println("size = " + field.length);
          System.out.println("");
	}

	private void parseDate(byte[] field, DecodedMessage msg, boolean increment)
		throws DecoderException
	{
		int y, m, d;

		// Get the timer & save the status before any updates.
		RecordedTimeStamp rts = msg.getTimer();
		int stat = msg.getTimer().getStatus();

		// Note sensorNumber doubles as field_id for date ops
		switch(sensorNumber)
		{
		case 1:  // YYMMDD or YY/MM/DD or YYYY/MM/DD
			if (field_length == 6)
			{
				y = numberParser.parseIntValue(ArrayUtil.getField(field, 0, 2));
				m = numberParser.parseIntValue(ArrayUtil.getField(field, 2, 2));
				d = numberParser.parseIntValue(ArrayUtil.getField(field, 4, 2));
			}
			else if (field_length == 8)
			{
				y = numberParser.parseIntValue(ArrayUtil.getField(field, 0, 2));
				m = numberParser.parseIntValue(ArrayUtil.getField(field, 3, 2));
				d = numberParser.parseIntValue(ArrayUtil.getField(field, 6, 2));
			}
			else if (field_length == 10)
			{
				y = numberParser.parseIntValue(ArrayUtil.getField(field, 0, 4));
				m = numberParser.parseIntValue(ArrayUtil.getField(field, 5, 2));
				d = numberParser.parseIntValue(ArrayUtil.getField(field, 8, 2));
			}
			else
				throw new FieldParseException(
					"Date format 1 bad date format '" + field + "'");

			rts.setYear(y);
			msg.setJustGotFullDateTime(true);
			rts.setMonth(m);
			rts.setDayOfMonth(d);
			if (!increment)
				rts.decrementDay();
			if (stat != msg.getTimer().getStatus())
				msg.upgradeStoredTimes();
			rts.incrementDay();
			break;

		case 2: // ddd, yyddd, yy/ddd, or yyyyddd
			y = -1;
			if (field_length <= 4)
			{
				d = numberParser.parseIntValue(field);
			}
			else if (field_length == 5)
			{
				y = numberParser.parseIntValue(ArrayUtil.getField(field, 0, 2));
				d = numberParser.parseIntValue(ArrayUtil.getField(field, 2, 3));
			}
			else if (field_length == 6)
			{
				y = numberParser.parseIntValue(ArrayUtil.getField(field, 0, 2));
				d = numberParser.parseIntValue(ArrayUtil.getField(field, 3, 3));
			}
			else if (field_length == 7)
			{
				y = numberParser.parseIntValue(ArrayUtil.getField(field, 0, 4));
				d = numberParser.parseIntValue(ArrayUtil.getField(field, 4, 3));
			}
			else
				throw new FieldParseException(
					"Date format 2 bad data '" + field + "'");

			if (y != -1)
			{
				rts.setYear(y);
				msg.setJustGotFullDateTime(true);
			}
			else
				msg.justGotNonYearField();

			if ( d == 366 ) 
			{
				int curYear = rts.getYear();
				GregorianCalendar gc = new GregorianCalendar();
				if ( curYear != 1970 && !gc.isLeapYear(curYear) )
				{
					throw new FieldParseException("Found day 366 for non-leap year "+curYear+".");
				}
			}
			rts.setDayOfYear(d);
			if (!increment)  // see comment above for jdy field type.
				rts.decrementDay();
			if (stat != msg.getTimer().getStatus())
				msg.upgradeStoredTimes();
			rts.incrementDay();
			break;

		case 3: // mmdd or mm/dd
			if (field_length == 4)
			{
				m = numberParser.parseIntValue(ArrayUtil.getField(field, 0, 2));
				d = numberParser.parseIntValue(ArrayUtil.getField(field, 2, 2));
			}
			else if (field_length == 5)
			{
				m = numberParser.parseIntValue(ArrayUtil.getField(field, 0, 2));
				d = numberParser.parseIntValue(ArrayUtil.getField(field, 3, 2));
			}
			else
				throw new FieldParseException(
					"Date format 3 bad data '" + field + "'");

			rts.setMonth(m);
			rts.setDayOfMonth(d);
			if ( !increment )
				rts.decrementDay();
			if (stat != msg.getTimer().getStatus())
				msg.upgradeStoredTimes();
			rts.incrementDay();
Logger.instance().debug3("After M field with month=" + m + ", day=" + d + ", dayOfYear=" + rts.getDayOfYear() + ", incr=" + increment);
			msg.justGotNonYearField();
			break;

		case 4: // mmddyy, mm/dd/yy
			m = numberParser.parseIntValue(ArrayUtil.getField(field, 0, 2));
			if (field_length == 6)
			{
				d = numberParser.parseIntValue(ArrayUtil.getField(field, 2, 2));
				y = numberParser.parseIntValue(ArrayUtil.getField(field, 4, 2));
			}
			else if (field_length == 8 || field_length == 10)
			{
				d = numberParser.parseIntValue(ArrayUtil.getField(field, 3, 2));
				y = numberParser.parseIntValue(ArrayUtil.getField(field, 6, 
					field_length == 8 ? 2 : 4));
			}
			else
				throw new FieldParseException(
					"Date format 4 bad data '" + field + "'");

			rts.setYear(y);
			rts.setMonth(m);
			rts.setDayOfMonth(d);
			if (!increment)
				rts.decrementDay();
			if (stat != msg.getTimer().getStatus())
				msg.upgradeStoredTimes();
			rts.incrementDay();
			msg.setJustGotFullDateTime(true);
			break;

		default:
			throw new ScriptException("Unknown date format " + sensorNumber);
		}
	}

	private int indexOfMonth(byte [] field)
		throws FieldParseException
	{
		String name = new String(field);
		if (name.length() > 3)
			name = name.substring(0, 3);
		name = name.toLowerCase();

		for(int i=1; i<mn.length; i++)
			if (name.equals(mn[i]))
				return i;

		throw new FieldParseException("No such month '" + new String(field) + "'");
	}

	private void parseTime(byte[] field, RecordedTimeStamp rts)
		throws DecoderException
	{
		int h=0, m=0, s=0;

//	Set standard time delimiters
		String delimiters = "[\\s:\\x2e-]"; // Time delimiters: ':',' ','.','-' 

//	Convert time field to String
		String tf = new String (ArrayUtil.getField(field,0, field.length));

//	Check for am/pm designators
		int ampmIndex = tf.toLowerCase().indexOf("am");
		if ( ampmIndex > 0 ) {
			AMSet = true;
			PMSet = false;
			tf = tf.substring(0, ampmIndex);
		} else {
			ampmIndex = tf.toLowerCase().indexOf("pm");
			if ( ampmIndex > 0 ) {
				PMSet = true;
				AMSet = false;
				tf = tf.substring(0, ampmIndex);
			}
		}

//	Eliminate trailing spaces

		int spaceIndex = tf.lastIndexOf(' ');
		while ( spaceIndex == tf.length()-1 )
		{
			tf = tf.substring(0,spaceIndex);
			spaceIndex = tf.lastIndexOf(' ');
		}

//	See if there are standard delimiters

    String tm[] = tf.split(delimiters);
		if ( tm.length == 1 ) {

//		No delimiters - analyze length to determine field format

			field = tf.getBytes();
			if (field.length <= 2) // m or mm
			{
				m = numberParser.parseIntValue(ArrayUtil.getField(field, 0,
					field.length));
			}
			else if (field.length == 3) // HMM
			{
				h = numberParser.parseIntValue(ArrayUtil.getField(field, 0, 1));
				m = numberParser.parseIntValue(ArrayUtil.getField(field, 1, 2));
			}
			else // some combo of HH MM and optionally SS.
			{
				if ( field.length > 0 && field.length <= 8 ) {
					h = numberParser.parseIntValue(ArrayUtil.getField(field, 0, 2));
					if (field.length == 4) // hhmm
						m = numberParser.parseIntValue(ArrayUtil.getField(field, 2, 2));
					else if (field.length == 5) // hh:mm
						m = numberParser.parseIntValue(ArrayUtil.getField(field, 3, 2));
					else if (field.length == 6) // hhmmss
					{
						m = numberParser.parseIntValue(ArrayUtil.getField(field, 2, 2));
						s = numberParser.parseIntValue(ArrayUtil.getField(field, 4, 2));
					}
					else if (field.length == 8) // hh:mm:ss
					{
						m = numberParser.parseIntValue(ArrayUtil.getField(field, 3, 2));
						s = numberParser.parseIntValue(ArrayUtil.getField(field, 6, 2));
					}
				}
			}
		} else if ( tm.length > 1 ) {
//		Has delimeter(:,.,-,or space)
			if ( tm.length > 2 ) 		// Has a second
				s = numberParser.parseIntValue(tm[2].getBytes());
			if ( tm.length > 1 ) 	 //  Has a minute
				m = numberParser.parseIntValue(tm[1].getBytes());
			h = numberParser.parseIntValue(tm[0].getBytes());
		}
		if ( h < 0 || h > 24 || m < 0 || m > 59 || s < 0 || s >59 ) 
					throw new FieldParseException("Bad time format '" + 
						new String(field) + "'");
		rts.setHour(h);
		rts.setMinute(m);
		rts.setSecond(s);
	}
	
	/**
	 * @param s the field data
	 * @return true if the field data represents a placeholder for a missing value.
	 */
	private boolean isMissingSymbol(String s)
	{
		return s.startsWith("//") 
		 || s.startsWith("??")
		 || s.startsWith("---")
		 || (s.equalsIgnoreCase("M") && data_type == NumberParser.ASCII_FMT)
		 || decodesScript.isMissingSymbol(s);
	}

	public static void main(String args[])
	{
		FieldArgsTokenizer fat = new FieldArgsTokenizer(args[0]);
		String tok;
		int i=0;
		while((tok = fat.nextToken()) != null)
			System.out.println("Token[" + (i++) + "] = '" + tok + "'");
	}
}

class FieldArgsTokenizer
{
	String str;
	int pos;
	int length;
	boolean quoted;
	boolean doubleQuoted;
	boolean wasDoubleQuoted;

	public FieldArgsTokenizer(String inputstr)
	{
		str = inputstr.trim();
		pos = 0;
		length = str.length();
	}

	public String nextToken()
	{
		int start = pos;
		quoted = false;
		doubleQuoted = false;
		wasDoubleQuoted = false;
		StringBuffer ret = new StringBuffer();
		while(pos < length)
		{
			char c = str.charAt(pos++);
			if (quoted)
			{
				if (c == '\'')
				{
					quoted = false;
					continue;
				}
			}
			else if (doubleQuoted)
			{
				if (c == '"')
				{
					doubleQuoted = false;
					continue;
				}
			}
			else if (c == ',')
				break;
			else if (c == '\'')
			{
				quoted = true;
				continue;
			}
			else if (c == '"')
			{
				doubleQuoted = true;
				wasDoubleQuoted = true;
				continue;
			}
			else if (c == ' ')  // delete unquoted spaces.
				continue;
			ret.append(c);
		}
		if (pos > start)
			return ret.toString();
		else
			return null;
	}
}

