/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/

package decodes.comp;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

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
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
							}
						}
						catch(NumberFormatException ex)
						{
							log.atWarn().setCause(ex)
							        .log("file: '{}':{} AreaRatingReader - Not a parsable precision line. -- ignored",filename ,lineNumber());
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
								log.atWarn().setCause(ex)
							        .log("file: '{}':{} AreaRatingReader - Not a parsable line. -- ignored",filename ,lineNumber());
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
			log.atWarn().setCause(ex)
			.log("file: '{}':{} IO Error: -- aborting.",filename ,lineNumber());
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
	
	private int lineNumber()
	{
		return (rdr != null ? rdr.getLineNumber() : -1);
	}

}
