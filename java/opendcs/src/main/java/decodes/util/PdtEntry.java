/*
 *  $Id$
 */
package decodes.util;

import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import ilex.util.TextUtil;
import ilex.util.IDateFormat;
import lrgs.common.DcpAddress;

/**
 * This class contains a PDT entry.
 */
public class PdtEntry
{
	public static final int fieldWidths[] = { 6, 8, 1, 3, 1, 3, 6, 6, 4, 4, 1,
	    2, 1, 1, 31, 7, 8, 1, 14, 16, 1, 1, 6, 24, 20, 20, 30, 11, 1, 1 };
	public static final SimpleDateFormat sdf = new SimpleDateFormat(
	    "yyyyDDDHHmm");
	public static int lineLength = 0;
	static
	{
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		for(int i=0; i<fieldWidths.length; i++)
			lineLength += fieldWidths[i];
	}
	public static final Date zeroDate = new Date(0L);

	/** NOAA User Name for Agency */
	public String agency = null;
	
	/** DCP Address */
	public DcpAddress dcpAddress = null;

	/** Self-timed channel or 0 if non specified. */
	public int st_channel = 0;
	
	/** Random channel or 0 if non specified. */
	public int rd_channel = 0;
	
	/** Self-timed message slot: first transmit second-of-day */
	public int st_first_xmit_sod = 0;
	
	/** Self-timed message interval between transmissions in seconds */
	public int st_xmit_interval = 0;
	
	/** Self-timed message window length in seconds */
	public int st_xmit_window = 0;
	public int baud = 0;
	public char data_format = 'x';
	public String state_abbr = null;
	public char location_code = 'x';
	public String description = null;
	public double latitude = 0.0;
	public double longitude = 0.0;
	public char location_type = 'x';
	public String manufacturer = null;
	public String model = null;
	public char unknown_flag1 = 'x';
	public char nmc_flag = 'x';
	public String nmc_descriptor = null;
	public String maintainer = null;
	public String telnum1 = null;
	public String telnum2 = null;
	public String shefcodes = null;
	public Date lastmodified = zeroDate;
	public int num_failures = 0;
	
	/** MJM - this doesn't mean active!!! */
	public char active_flag = 'Y';
	
	private NwsXrefEntry nwsXrefEntry = null;
	private boolean xrefed = false;

	private enum FillerStyle
	{
		LEADING_WS, LEADINGZEROS, TRAILING_WS
	}

	/**
	 * Constructs a new empty PdtEntry. values must be then set manually.
	 */
	public PdtEntry()
	{
	}

	/**
	 * Constructs a new PDT entry from a line from the the pdt flat file.
	 */
	public PdtEntry(String pdtFileLine) throws BadPdtEntryException
	{
		this();
		assignFromLine(pdtFileLine);
//if (dcpAddress.toString().equalsIgnoreCase("CE677A96"))
//{
//	System.out.println("Orig:   " + pdtFileLine);
//	System.out.println("Parsed: " + this.printToLine());
//}
	}

	public String printToLine()
	{
		StringBuilder myLine = new StringBuilder(lineLength);
		for(int i=0; i<lineLength; i++)
			myLine.append(' ');
		
		printField(myLine, agency==null ? "" : agency, 0, FillerStyle.LEADING_WS);

		printField(myLine, dcpAddress==null ? "" : dcpAddress.toString(), 1,
			    FillerStyle.LEADING_WS);

		if (st_channel != 0)
		{
			printField(myLine, "S", 2, FillerStyle.LEADING_WS);
			printField(myLine, String.valueOf(st_channel), 3,
			    FillerStyle.LEADINGZEROS);

			if (rd_channel != 0)
			{
				printField(myLine, "R", 4, FillerStyle.LEADING_WS);
				printField(myLine, String.valueOf(rd_channel), 5,
				    FillerStyle.LEADINGZEROS);
			}
			else
			{
				printField(myLine, "", 4, FillerStyle.LEADING_WS);
				printField(myLine, "", 5, FillerStyle.LEADINGZEROS);
			}
		}
		else
		{
			printField(myLine, "R", 2, FillerStyle.LEADING_WS);
			printField(myLine, String.valueOf(rd_channel), 3,
			    FillerStyle.LEADINGZEROS);

			printField(myLine, "", 4, FillerStyle.LEADING_WS);
			printField(myLine, "", 5, FillerStyle.LEADINGZEROS);
		}

	
		printField(myLine, hhmmss(st_first_xmit_sod), 6, FillerStyle.LEADINGZEROS);
		printField(myLine, hhmmss(st_xmit_interval), 7, FillerStyle.LEADINGZEROS);
		printField(myLine, mmss(st_xmit_window), 8, FillerStyle.LEADINGZEROS);

		printField(myLine, String.valueOf(baud), 9, FillerStyle.LEADINGZEROS);

		printField(myLine, String.valueOf(data_format), 10, FillerStyle.LEADING_WS);

		printField(myLine, state_abbr == null ? "" : state_abbr, 11, 
			FillerStyle.LEADING_WS);

		printField(myLine, "", 12, FillerStyle.LEADING_WS);

		printField(myLine, 
			location_code == 'x' ? "" : String.valueOf(location_code), 13,
			FillerStyle.LEADING_WS);

		printField(myLine, description==null?"":description, 14, 
			FillerStyle.TRAILING_WS);

		printField(myLine, latitude2str(latitude), 15, 
			FillerStyle.LEADINGZEROS);

		printField(myLine, longitude2str(longitude), 16,
			    FillerStyle.LEADINGZEROS);

		printField(myLine, 
			location_type == 'x' ? "" : String.valueOf(location_type), 17,
			FillerStyle.LEADING_WS);

		printField(myLine, 
			manufacturer == null ? "" : String.valueOf(manufacturer), 18,
			    FillerStyle.TRAILING_WS);

		printField(myLine, 
			model == null ? "" : String.valueOf(model), 19, 
			FillerStyle.TRAILING_WS);

		printField(myLine, 
			unknown_flag1 == 'x' ? "" : String.valueOf(unknown_flag1), 20,
			FillerStyle.LEADING_WS);

		printField(myLine, 
			nmc_flag == 'x' ? "" : String.valueOf(nmc_flag), 21,
			FillerStyle.LEADING_WS);

		printField(myLine, nmc_descriptor == null ? "" : nmc_descriptor, 22,
			FillerStyle.LEADING_WS);

		printField(myLine, maintainer == null ? "" : maintainer, 23, 
			FillerStyle.TRAILING_WS);

		printField(myLine, telnum1 == null ? "" : telnum1, 24, 
			FillerStyle.TRAILING_WS);

		printField(myLine, telnum2 == null ? "" : telnum2, 25, 
			FillerStyle.TRAILING_WS);

		printField(myLine, shefcodes == null ? "" : shefcodes, 26, 
			FillerStyle.TRAILING_WS);

		printField(myLine, 
			lastmodified == null ? "" : sdf.format(lastmodified), 27,
			FillerStyle.LEADING_WS);

		printField(myLine, 
			num_failures == 0 ? "" : (""+num_failures), 28,
			FillerStyle.LEADING_WS);

		printField(myLine, String.valueOf(active_flag), 29, 
			FillerStyle.LEADING_WS);

		return myLine.toString();
	}

	private void printField(StringBuilder line, String insert, int index,
	    FillerStyle fillerStyle)
	{
		int fieldWidth = fieldWidths[index];
		int position = 0;
		for(int i=0; i<index; i++)
			position += fieldWidths[i];
		int insertLen = insert.length();
		int lenDiff = fieldWidth - insertLen; // + means fill, - means trim

		if (fillerStyle == FillerStyle.LEADING_WS)
		{
			if (lenDiff < 0)
			{
				// Take the left-most 'fieldWidth' chars
				insert = insert.substring(0, fieldWidth);
				lenDiff = 0;
			}
			while(lenDiff-- > 0)
				line.setCharAt(position++, ' ');
			for(int i=0; i<insertLen; i++)
				line.setCharAt(position++, insert.charAt(i));
		}
		else if (fillerStyle == FillerStyle.LEADINGZEROS)
		{
			if (lenDiff < 0)
			{
				// Take the right-most 'fieldWidth' chars
				insert = insert.substring(-lenDiff);
				lenDiff = 0;
			}
			while(lenDiff-- > 0)
				line.setCharAt(position++, '0');
			for(int i=0; i<insertLen; i++)
				line.setCharAt(position++, insert.charAt(i));
		}
		else if (fillerStyle == FillerStyle.TRAILING_WS)
		{
			if (lenDiff < 0)
			{
				// Take the left-most 'fieldWidth' chars
				insert = insert.substring(0, fieldWidth);
				lenDiff = 0;
			}
			for(int i=0; i<insertLen; i++)
				line.setCharAt(position++, insert.charAt(i));
			while(lenDiff-- > 0)
				line.setCharAt(position++, ' ');
		}
	}

//	private String printWhiteSpace(String line, String insert, int index)
//	{
//		if (fieldWidths[index] <= insert.length())
//		{
//			return line;
//		}
//		for (int pos = 0; pos < fieldWidths[index] - insert.length(); pos++)
//		{
//			line = line + " ";
//		}
//		return line;
//	}

	public void assignFromLine(String line)
	    throws BadPdtEntryException
	{
		String[] f = TextUtil.getFixedFields(line, fieldWidths);
		int len = f.length;
		if (len < 11)
			throw new BadPdtEntryException("Only " + len + " fields in line.");

		agency = new String(f[0].trim());
		try
		{
			dcpAddress = new DcpAddress(new String(f[1]));
		}
		catch (NumberFormatException ex)
		{
			throw new BadPdtEntryException("Bad DCP address '" + f[1] + "'");
		}

		char c = f[2].charAt(0);
		if (c == 'S' || c == 'D')
		{
			try
			{
				st_channel = Integer.parseInt(f[3].trim());
				c = f[4].charAt(0);
				if (c == 'R')
					rd_channel = Integer.parseInt(f[5].trim());

				st_first_xmit_sod = IDateFormat.getSecondOfDay(f[6]);
				st_xmit_interval = IDateFormat.getSecondOfDay(f[7]);
				st_xmit_window = IDateFormat.getSecondOfDay(f[8]) / 60;
			}
			catch (NumberFormatException ex)
			{
				throw new BadPdtEntryException("Bad chan params");
			}
			catch (IllegalArgumentException ex)
			{
				throw new BadPdtEntryException("time params");
			}
		}
		else if (c == 'R')
		{
			try
			{
				rd_channel = Integer.parseInt(f[3].trim());
			}
			catch (NumberFormatException ex)
			{
				throw new BadPdtEntryException("Bad chan Params");
			}
		}

		try
		{
			baud = Integer.parseInt(f[9].trim());
		}
		catch (NumberFormatException ex)
		{
			throw new BadPdtEntryException("Bad baud");
		}

		data_format = f[10].charAt(0);

		if (len < 12)
			return;
		state_abbr = new String(f[11]);

		if (len < 14)
			return;
		location_code = f[13].charAt(0);

		if (len < 15)
			return;
		description = new String(f[14].trim());

		try
		{
			if (len < 16)
				return;
			latitude = cvtloc(f[15]);

			if (len < 17)
				return;
			longitude = cvtloc(f[16]);
		}
		catch (NumberFormatException ex)
		{
		}

		if (len < 18)
			return;
		location_type = f[17].charAt(0);

		if (len < 19)
			return;
		manufacturer = new String(f[18].trim());

		if (len < 20)
			return;
		model = new String(f[19].trim());

		if (len < 21)
			return;
		unknown_flag1 = f[20].charAt(0);

		if (len < 22)
			return;
		nmc_flag = f[21].charAt(0);

		if (len < 23)
			return;
		nmc_descriptor = new String(f[22].trim());

		if (len < 24)
			return;
		maintainer = new String(f[23].trim());

		if (len < 25)
			return;
		telnum1 = new String(f[24].trim());

		if (len < 26)
			return;
		telnum2 = new String(f[25].trim());

		if (len < 27)
			return;
		shefcodes = new String(f[26].trim());

		if (len < 28)
			return;
		try
		{
			lastmodified = sdf.parse(f[27]);
		}
		catch (ParseException ex)
		{
			throw new BadPdtEntryException("Bad LMT");
		}

		if (len < 29)
			return;
		c = f[28].charAt(0);
		if (Character.isDigit(c))
			num_failures = (int) c - (int) '0';

		if (len < 30)
			return;
		
// No: this is not an active flag.		
//		active_flag = f[29].charAt(0);
	}

	private double cvtloc(String s)
	    throws NumberFormatException
	{
		int sign = 1;
		if (s.charAt(0) == '-')
			sign = -1;
		int deg = 0, min = 0, sec = 0;
		if (s.length() == 8)
		{
			deg = Integer.parseInt(s.substring(1, 4));
			min = Integer.parseInt(s.substring(4, 6));
			sec = Integer.parseInt(s.substring(6));
		}
		else if (s.length() == 7)
		{
			deg = Integer.parseInt(s.substring(1, 3));
			min = Integer.parseInt(s.substring(3, 5));
			sec = Integer.parseInt(s.substring(5));
		}
		return ((double) deg + (double) min / 60. + (double) sec / 3600.)
		    * sign;
	}
	
	public static String hhmmss(int sod) 
	{
		int h = sod / (60*60);
		sod -= (h*60*60);
		int m = sod / 60;
		int s = sod % 60;

		StringBuilder sb = new StringBuilder();
		if (h < 10) sb.append('0');
		sb.append(h);
		if (m < 10) sb.append('0');
		sb.append(m);
		if (s < 10) sb.append('0');
		sb.append(s);
		return sb.toString();
	}
	public static String mmss(int sod) 
	{
		int h = sod / (60*60);
		sod -= (h*60*60);
		int m = sod / 60;
		int s = sod % 60;

		StringBuilder sb = new StringBuilder();
//		if (h < 10) sb.append('0');
//		sb.append(h);
		if (m < 10) sb.append('0');
		sb.append(m);
		if (s < 10) sb.append('0');
		sb.append(s);
		return sb.toString();
	}
	
	public static String latitude2str(double value)
	{
		byte sb[] = new byte[7];
		if (value < 0)
		{
			sb[0] = (byte)'-';
			value = -value;
		}
		else
			sb[0] = (byte)' ';
		
		int deg = (int)value;
		sb[1] = (byte)((int)'0' + (deg/10));
		sb[2] = (byte)((int)'0' + (deg%10));
		
		value = (value - (double)deg) * 60;
		int min = (int)value;
		sb[3] = (byte)((int)'0' + (min/10));
		sb[4] = (byte)((int)'0' + (min%10));
		
		value = (value - (double)min) * 60;
		int sec = (int)(value+.5);
		sb[5] = (byte)((int)'0' + (sec/10));
		sb[6] = (byte)((int)'0' + (sec%10));
	
		return new String(sb);
	}
	
	public static String longitude2str(double value)
	{
		byte sb[] = new byte[8];
		if (value < 0)
		{
			sb[0] = (byte)'-';
			value = -value;
		}
		else
			sb[0] = (byte)' ';
		
		int deg = (int)value;
		sb[1] = (byte)((int)'0' + (deg/100));
		sb[2] = (byte)((int)'0' + ((deg%100)/10));
		sb[3] = (byte)((int)'0' + (deg%10));
		
		value = (value - (double)deg) * 60;
		int min = (int)value;
		sb[4] = (byte)((int)'0' + (min/10));
		sb[5] = (byte)((int)'0' + (min%10));
		
		value = (value - (double)min) * 60;
		int sec = (int)(value+.5);
		sb[6] = (byte)((int)'0' + (sec/10));
		sb[7] = (byte)((int)'0' + (sec%10));
	
		return new String(sb);
	}
		
	public String toString()
	{
		return agency + ":" + dcpAddress + ":" + st_channel + ":"
		    + rd_channel + ":" + st_first_xmit_sod + ":" + st_xmit_interval
		    + ":" + st_xmit_window + ":" + baud + ":" + data_format + ":"
		    + state_abbr + ":" + location_code + ":" + description + ":"
		    + latitude + ":" + longitude + ":" + location_type + ":"
		    + manufacturer + ":" + model + ":" + unknown_flag1 + ":" + nmc_flag
		    + ":" + nmc_descriptor + ":" + maintainer + ":" + telnum1 + ":"
		    + telnum2 + ":" + shefcodes + ":" + lastmodified + ":"
		    + num_failures + ":" + active_flag;
	}
	
	public String getDescription()
	{
		return nwsXrefEntry != null ? nwsXrefEntry.getLocationName() : description;
	}

	public NwsXrefEntry getNwsXrefEntry()
	{
		if (!xrefed)
		{
			NwsXref nx = NwsXref.instance();
			if (nx.isLoaded())
				nwsXrefEntry = nx.getByAddr(dcpAddress);
			xrefed = true;
		}
		return nwsXrefEntry;
	}
}
