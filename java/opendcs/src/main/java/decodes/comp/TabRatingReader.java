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

import ilex.util.EnvExpander;

import java.io.LineNumberReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

/**
 * Reads a rating table from a USGS rating table RDB file.
 */
public class TabRatingReader implements RatingTableReader
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
			log.atError().setCause(ex).log("IO Error -- aborting.");
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

}
