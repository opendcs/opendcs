/*
*  $Id$
*/
package decodes.comp;

import decodes.comp.RatingTableReader;
import decodes.comp.RatingComputation;
import decodes.comp.ComputationParseException;
import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.util.EnvExpander;

import java.io.LineNumberReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * Reads a rating table from a USGS rating table RDB file.
 */
public class RdbRatingReader implements RatingTableReader
{
	/**
	 * The name of the file being read.
	 */
	private String filename;
	
	/**
	 * State for reading parameters.
	 */
	private static final int STATE_PARAMS = 0;
	
	/**
	 * State for reading column header.
	 */
	private static final int STATE_COL_HEAD = 1;
	
	/**
	 * State for reading column format line
	 */
	private static final int STATE_COL_FMT = 2;
	
	/**
	 * State for reading column data.
	 */
	private static final int STATE_COL_DATA = 3;
	
	/**
	 * Current parser state.
	 */
	private int state;
	
	/**
	 * True if the column data contains shift values.
	 */
	private boolean containsShifts;
	
	/**
	 * Used for reading the file.
	 */
	LineNumberReader rdr;

	/**
	 * Local copy of RatingComputation used during parse.
	 */
//	private RatingComputation rc;
	private HasLookupTable rc;

	/** Used to collaps shift values table by throwing out dups. */
	private double lastShiftValue;

	/** Used to parse begin/end times. */
	private static SimpleDateFormat bedf = new SimpleDateFormat(
		"yyyyMMddHHmmss");

	/**
	  Constructs new RdbRatingReader for a particular file name.
	  @param filename the name of the RDB file
	*/
	public RdbRatingReader( String filename )
	{
		this.filename = filename;
		rdr = null;
	}
	
	/**
	  Reads rating data from the file and populates the computation.
	  @param rc the rating computation object.
	  @throws ComputationParseException if parse error reading file.
	*/
	public synchronized void readRatingTable( HasLookupTable rc ) 
		throws ComputationParseException
	{
		this.rc = rc;
		state = STATE_PARAMS;
		lastShiftValue = -1.0;
		try
		{
			LineNumberReader rdr = new LineNumberReader(
				new FileReader(EnvExpander.expand(filename)));
			String line;
			while((line = rdr.readLine()) != null)
			{
				switch(state)
				{
				case STATE_PARAMS: processParams(line); break;
				case STATE_COL_HEAD: processColHead(line); break;
				case STATE_COL_FMT: processColFmt(line); break;
				case STATE_COL_DATA: processColData(line); break;
				}
			}
		}
		catch(IOException ex)
		{
			parseWarning("IO Error: " + ex + " -- aborting.");
		}
		finally
		{
			if (rdr != null)
			{
				try { rdr.close(); }
				catch(Exception ex) {}
			}
			rc = null;
		}
	}

	/**
	 * Process a single line when in the PARAMS state.
	 * @param line a line from the file
	 */
	private void processParams( String line ) throws ComputationParseException
	{
		if (!line.startsWith("# //"))
		{
			state = STATE_COL_HEAD;
			processColHead(line);
			return;
		}
		String v;
		if ((v = TextUtil.scanAssign(line, "DATABASE NUMBER", 1, true)) != null)
		{
			rc.setProperty("dbno", v);
			return;
		}
		if (line.startsWith("# //STATION")
		 && (v = TextUtil.scanAssign(line, "NUMBER", 1, true)) != null)
		{
			rc.setProperty("StationNumber", v.trim());
			return;
		}
		if ((v = TextUtil.scanAssign(line, "StationName", 1, true)) != null)
		{
			rc.setProperty("StationName", v);
			return;
		}
		if ((v = TextUtil.scanAssign(line, "DD NUMBER", 1, true)) != null)
		{
			rc.setProperty("ddno", v.trim());
			if ((v = TextUtil.scanAssign(line, "LABEL", 1, true)) != null)
			{
				rc.setProperty("DepDescription", v.trim());
			}
			return;
		}
		if ((v = TextUtil.scanAssign(line, "PARAMETER CODE", 1, true)) != null)
		{
			rc.setProperty("DepEpaCode", v.trim());
			return;
		}
		if ((v = TextUtil.scanAssign(line, "RATING ID", 1, true)) != null)
		{
			rc.setProperty("RatingID", v.trim());
			if ((v = TextUtil.scanAssign(line, "TYPE", 1, true)) != null)
			{
				rc.setProperty("RatingType", v.trim());
			}
			if ((v = TextUtil.scanAssign(line, "NAME", 1, true)) != null)
			{
				rc.setProperty("RatingName", v.trim());
			}
			return;
		}
		if ((v = TextUtil.scanAssign(line, "OFFSET1", 1, true)) != null)
		{
			rc.setProperty("Offset1", v.trim());
			try
			{
				double d = Double.parseDouble(v);
				rc.setXOffset(d);
			}
			catch(NumberFormatException ex)
			{
				parseWarning("Bad OFFSET1 value '" + v + "' -- ignored.");
			}
			if ((v = TextUtil.scanAssign(line, "OFFSET2", 1, true)) != null)
			{
				rc.setProperty("Offset2", v.trim());
			}
			return;
		}
		if (line.startsWith("# //RATING_INDEP")
		 && (v = TextUtil.scanAssign(line, "PARAMETER", 1, true)) != null)
		{
			// The string should be in the form "NAME IN Units"
			// Example "Discharge IN Feet".
			v = v.trim();
			int idx = v.indexOf(" IN ");
			if (idx == -1)
			{
				rc.setProperty("IndepName", v.trim());
				//Try to find this format "Discharge (cfs)".
				int lParesIdx = v.indexOf("(");//left
				int rParesIdx = v.indexOf(")");//right
				if (lParesIdx != -1 && rParesIdx != -1)
				{
					rc.setProperty("IndepName", 
						v.substring(0, lParesIdx).trim());
					rc.setProperty("IndepUnits",
						v.substring(lParesIdx+1, rParesIdx).trim());
				}
			}
			else
			{
				rc.setProperty("IndepName", v.substring(0, idx).trim());
				rc.setProperty("IndepUnits", v.substring(idx+4).trim());
			}
			return;
		}
		if (line.startsWith("# //RATING_DEP")
		 && (v = TextUtil.scanAssign(line, "PARAMETER", 1, true)) != null)
		{
			// The string should be in the form "NAME IN Units"
			// Example "Discharge IN Feet".
			v = v.trim();
			int idx = v.indexOf(" IN ");
			if (idx == -1)
			{
				rc.setProperty("DepName", v.trim());
				//Try to find this format "Discharge (cfs)".
				int lParesIdx = v.indexOf("(");//left
				int rParesIdx = v.indexOf(")");//right
				if (lParesIdx != -1 && rParesIdx != -1)
				{
					rc.setProperty("DepName", 
							v.substring(0, lParesIdx).trim());
					rc.setProperty("DepUnits",
							v.substring(lParesIdx+1, rParesIdx).trim());
				}
			}
			else
			{
				rc.setProperty("DepName", v.substring(0, idx).trim());
				rc.setProperty("DepUnits", v.substring(idx+4).trim());
			}
			return;
		}
		if (line.startsWith("# //RATING_DATETIME"))
		{
			String ts = TextUtil.scanAssign(line,"BEGIN", 1, true);
			if (ts != null)
			{
				String tzs = TextUtil.scanAssign(line,"BZONE", 1, true);
				TimeZone tz = (tzs != null ? TimeZone.getTimeZone(tzs) :
					TimeZone.getTimeZone("UTC"));
				if (tz != null)
					bedf.setTimeZone(tz);
				try { rc.setBeginTime(bedf.parse(ts)); }
				catch(ParseException ex)
				{
					parseWarning("Invalid begin time format '" + ts
						+ "' -- begin time ignored.");
				}
			}
			ts = TextUtil.scanAssign(line,"END", 1, true);
			if (ts != null)
			{
				String tzs = TextUtil.scanAssign(line,"EZONE", 1, true);
				TimeZone tz = (tzs != null ? TimeZone.getTimeZone(tzs) :
					TimeZone.getTimeZone("UTC"));
				if (tz != null)
					bedf.setTimeZone(tz);
				if (ts.startsWith("----"))
					rc.setEndTime(new Date(Long.MAX_VALUE));
				else
				{
					try { rc.setEndTime(bedf.parse(ts)); }
					catch(ParseException ex)
					{
						parseWarning("Invalid end time format '" + ts
							+ "' -- begin time ignored.");
					}
				}
			}
		}
	}

	/**
	* Logs a warning message about parsing this file.
	* @param msg the message
	*/
	private void parseWarning( String msg )
	{
		Logger.instance().warning("RDB File '" + filename + ":"
			+ (rdr != null ? rdr.getLineNumber() : -1)
			+ " " + msg);
	}

	/**
	* Process a single line when in the COL_HEAD state.
	* @param line the line
	*/
	private void processColHead( String line ) throws ComputationParseException
	{
		if (!line.startsWith("INDEP"))
		{
			parseWarning("Expected column header, got '" + line 
				+ "' -- ignored");
		}
		containsShifts = line.indexOf("SHIFT") != -1;
		state = STATE_COL_FMT;
	}
	
	
	/**
	* Processes a single line in the COL_FMT state.
	* @param line the line
	*/
	private void processColFmt( String line ) throws ComputationParseException
	{
		state = STATE_COL_DATA;
		if (line.indexOf('N') == -1
		 && line.indexOf('S') == -1)
		{
			parseWarning("Expected column format line, got '" + line
				+ "' -- will try to parse column data.");
			processColData(line);
		}
	}
	
	
	/**
	 * Processes a single line in the COL_DATA state.
	* @param line the line
	 */
	private void processColData( String line ) throws ComputationParseException
	{
		StringTokenizer st = new StringTokenizer(line);
		double n[] = new double[3];
		n[0] = n[1] = n[2] = 0.0;
		for(int i=0; i<3 && st.hasMoreTokens(); i++)
		{
			String s = st.nextToken();
			try { n[i] = Double.parseDouble(s); }
			catch(NumberFormatException ex)
			{
				parseWarning("Expected 3 numbers, got '" + line 
					+ "' -- ignored.");
				return;
			}
		}
		rc.addPoint(n[0], n[2]);
		if (containsShifts && n[1] != lastShiftValue)
		{
			rc.addShift(n[0], n[1]);
			lastShiftValue = n[1];
		}
		
	}
}
