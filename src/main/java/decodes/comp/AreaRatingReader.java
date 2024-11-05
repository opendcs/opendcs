package decodes.comp;

import ilex.util.Logger;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.math.BigDecimal;
import java.util.StringTokenizer;

/**
 * Reads a rating table from a USGS rating table Area file.
 */
public class AreaRatingReader implements RatingTableReader
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
	 * Constructs new AreaRatingReader for a particular file name.
	 */
	public AreaRatingReader( String filename )
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
				new FileReader(filename));
			String line;
			double samples[] = {.0,.1,.2,.3,.4,.5,.6,.7,.8,.9};
			while((line = rdr.readLine()) != null)
			{
				//Need to skip the header lines, the last 2 lines of the file
				StringTokenizer st1 = new StringTokenizer(line);
				int numItems = st1.countTokens();//needs to be 12 numbers or
												//2 numbers which is the last 
												//line of the last table
				//If line start with feet - find out the STANDARD PRECISION
				String trimLine = line.trim();
				if (trimLine.startsWith("feet") && numItems > 11)
				{
					int preCount = 0;
					samples = new double[10];
					while(st1.hasMoreTokens())
					{
						String s = st1.nextToken();
						if (s != null && s.equalsIgnoreCase("feet"))
							continue;
						
						try 
						{
							if (preCount < 10)
							{
								samples[preCount] = Double.parseDouble(s);
								//System.out.println("prec = " + 
								//samples[preCount]);	
							}
						}
						catch(NumberFormatException ex)
						{
							parseWarning("AreaRatingReader " +
									"Not a parseable precision line '" + line 
									+ "' -- ignored.");
						}
						preCount++;
					}
					continue;
				}

				if (numItems == 12 || numItems ==2)
				{
					double[] data = new double[numItems];
					
					boolean dataPoints = true;//indicates a good line
					//Verify that all numbers are double parseable
					int x = 0;
					while(st1.hasMoreTokens())
					{
						String s = st1.nextToken();
						try 
						{ 
							//Ged rid of * if there is one
							int idx = s.indexOf("*");
							if (idx != -1)
							{	//remove the * from the number
								s = s.substring(0, idx);
							}
							double temp = Double.parseDouble(s);
							if (x < numItems)
								data[x] = temp;
							x++;
						}
						catch(NumberFormatException ex)
						{
							dataPoints = false;
							if (numItems != 2)
							{	//we have a lot of lines with 2 items
								parseWarning("AreaRatingReader " +
								"Not a parseable line '" + line 
								+ "' -- ignored.");
							}
							break;
						}
					}
					//If dataPoints is true
					if (dataPoints)
					{//This is a line to process
//						double samples[] = {.00,.01,.02,.03,.04,
//											.05,.06,.07,.08,.09};
//						double samples[] = {.0,.1,.2,.3,.4,
//								.5,.6,.7,.8,.9};
						double indep = data[0];//First num in line
						int numberOfItems = data.length - 1;
						if (numItems == 2)
							numberOfItems = data.length;
						
						for (int y = 1; y < numberOfItems; y++)
						{	//Get the next 10 numbers after the first one or
							//just one
							double dep = data[y];
							int decimalPlace = 2;
						    BigDecimal bd = 
						    	new BigDecimal(indep + samples[y-1]);
						    bd = bd.setScale(
						    		decimalPlace,BigDecimal.ROUND_HALF_EVEN);
						    rc.addPoint(bd.doubleValue(), dep);
							//rc.addPoint(indep + samples[y-1], dep);
							//rc.addPoint(indep, dep);
							//indep = indep + .01;
						}
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
		Logger.instance().warning("Table File '" + filename + ":"
			+ (rdr != null ? rdr.getLineNumber() : -1)
			+ " " + msg);
	}

}
