/*
*  $Id$
*/
package decodes.comp;

import decodes.comp.RatingTableReader;
import decodes.comp.RatingComputation;
import decodes.comp.ComputationParseException;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.TextUtil;

import java.io.LineNumberReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 * Reads a rating table from a USGS rating table RDB file.
 */
public class TabRatingReader implements RatingTableReader
{
	/**
	 * The name of the file being read.
	 */
	private String filename;
	
	/**
	 * Used for reading the file.
	 */
	LineNumberReader rdr;

	/**
	 * Constructs new TabRatingReader for a particular file name.
	 */
	public TabRatingReader( String filename )
	{
		this.filename = filename;
		rdr = null;
	}
	
	/**
	 * Reads rating data from the file and populates the computation.
	 * @param rc the computation.
	 * @throws ComputationParseException if error reading file.
	 */
	public synchronized void readRatingTable( HasLookupTable rc ) 
		throws ComputationParseException
	{
		try
		{
			LineNumberReader rdr = new LineNumberReader(
				new FileReader(EnvExpander.expand(filename)));
			String line;
			while((line = rdr.readLine()) != null)
			{
				line = line.trim();
				if (line.length() == 0 || line.charAt(0) == '#')
					continue;

				StringTokenizer st = new StringTokenizer(line, " \t,");
				if (st.countTokens() < 2)
				{
					//parseWarning("Skipped line -- need 2 column values.");
					continue;
				}
				try
				{
					double indep = Double.parseDouble(st.nextToken());
					double dep = Double.parseDouble(st.nextToken());
					rc.addPoint(indep, dep);
				}
				catch(NumberFormatException ex)
				{
					//parseWarning("Skipped line -- column data not a number.");
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
	* Logs a warning message about parsing this file.
	* @param msg the message
	*/
	private void parseWarning( String msg )
	{
		Logger.instance().warning("Table File '" + filename + ":"
			+ (rdr != null ? rdr.getLineNumber() : -1)
			+ " " + msg);
	}
}
