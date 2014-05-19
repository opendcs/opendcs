/*
 * Open source software by Cove Software, LLC
*/
package decodes.comp;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.math.BigDecimal;
import java.util.StringTokenizer;
import java.text.NumberFormat;

import ilex.util.EnvExpander;
import ilex.util.Logger;

/**
Reads a rating table from a file that has columns for each decimal point.
The left most column is the input (independant), e.g. Stage.
Then 10 more columns representing the discharge for that stage plus some
decimal fraction like .1 or .01.
A 'header line' should contain the decimal fractions.
A property called 'headerLineStart' specifies a word that tells us this
is the header line. headerLineStart defaults to 'ght'.
*/
public class DecColumnRatingReader implements RatingTableReader
{
	public static final String module = "DecColumnRatingReader";

	/** The name of the file being read. */
	private String filename;
	
	/** Used for reading the file. */
	private LineNumberReader rdr;

	/** A word to destinguish the start of the header line. */
	private String headerLineStart = "ght";

	/** Default to tenths of a unit for the columns. */
	private double adders[] = {.0,.1,.2,.3,.4,.5,.6,.7,.8,.9};

	private NumberFormat numFmt = NumberFormat.getInstance();

	/**
	 * Constructs new DecColumnRatingReader for a particular file name.
	 */
	public DecColumnRatingReader( String filename )
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
			boolean headerSeen = false;
		  nextLine:
			while((line = rdr.readLine()) != null)
			{
				//Need to skip the header lines, the last 2 lines of the file
				StringTokenizer st = new StringTokenizer(line);
				int numTokens = st.countTokens();

				// A line must have at least 2 tokens for one indep and one 
				// dep. We must be able to handle partial lines at end of file.
				if (numTokens < 2)
					continue;

				String tok0 = st.nextToken();

				if (numTokens >= 11 && tok0.equalsIgnoreCase(headerLineStart))
				{
					// This is the header line, anything parsed before this is garbage.
					if (!headerSeen)
						rc.clearTable();
					headerSeen = true;
					int preCount = 0;
					double[] newAdders = new double[10];
					for(int i=0; i<10 && st.hasMoreTokens(); i++)
					{
						String s = st.nextToken();
						char c = s.charAt(s.length() - 1);
						if (!Character.isDigit(c) && c != ',' && c != '+' && c != '-' && c != '.')
							continue;
						try { newAdders[i] = Double.parseDouble(s); }
						catch(NumberFormatException ex)
						{
							parseWarning("Bad adder in header line '" + s
								+ "' -- will default to tenths.");
							continue nextLine;
						}
					}
					adders = newAdders;
					continue;
				}

				double baseIndep = 0.0;
				try { baseIndep = numFmt.parse(tok0).doubleValue(); }
				catch(Exception ex)
				{
				//	parseWarning("invalid base independent '" + tok0
				//		+ "' -- line skipped.");
					continue;
				}

				for(int i=0; i<10 && st.hasMoreTokens(); i++)
				{
					String s = st.nextToken();

					// Get rid of * (indicates a measured point)
					int idx = s.indexOf("*");
					if (idx != -1)
						s = s.substring(0, idx);

					char c = s.charAt(s.length() - 1);
					if (!Character.isDigit(c) && c != ',' && c != '+' && c != '-' && c != '.')
						continue;

					try 
					{
						double dep = numFmt.parse(s).doubleValue(); 
						rc.addPoint(baseIndep + adders[i], dep);
					}
					catch(Exception ex)
					{
//						parseWarning("Bad dep-value in data line '" + s
//								+ "' -- value skipped.");
						continue nextLine;
					}
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
		Logger.instance().warning(module + " " + filename + ":"
			+ (rdr != null ? rdr.getLineNumber() : -1)
			+ " " + msg);
	}

	/** sets a word to destinguish the start of the header line. */
	public void setHeaderLineStart(String hls) { headerLineStart = hls; }

	/** Test main to read a file & print results. */
	public static void main(String args[])
		throws Exception
	{
		DecColumnRatingReader rrr = new DecColumnRatingReader(args[0]);
		RatingComputation rc = new RatingComputation(rrr);
		rc.read();
		rc.dump();
	}
}
