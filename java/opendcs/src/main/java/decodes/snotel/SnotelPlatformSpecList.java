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
package decodes.snotel;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.StderrLogger;
import lrgs.common.DcpAddress;

/**
 * This class maintains a collection of SnotelPlatformSpec that
 * are read from a designated file.
 */
public class SnotelPlatformSpecList
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private HashMap<DcpAddress, SnotelPlatformSpec> platformSpecs
		= new HashMap<DcpAddress, SnotelPlatformSpec>();
	private long lastLoadTime = 0L;
	
	public SnotelPlatformSpecList()
	{
	}
	
	/**
	 * Return the platform spec corresponding to the GOES address or null if
	 * there is none.
	 * @param addr the GOES DCP Address
	 * @return platform spec or null
	 */
	public SnotelPlatformSpec getPlatformSpec(DcpAddress addr)
	{
		return platformSpecs.get(addr);
	}
	
	public Collection<SnotelPlatformSpec> getPlatformSpecs()
	{
		return platformSpecs.values();
	}

	public void loadFile(File specFile)
		throws IOException
	{
		platformSpecs.clear();
		
		// open filename with line number reader
		LineNumberReader lnr = new LineNumberReader(new FileReader(specFile));
		String line;
		while ((line = lnr.readLine()) != null)
		{
			//MJM 20201110 new format is: stationId|stationName|dcpAddr|formatFlag
			line = line.trim();
			if (line.startsWith("#") || line.length() == 0)
				continue;
			String delim = ",";
			if (line.contains("|"))
				delim = "|";

			String fields[] = new String[4];
			StringTokenizer st = new StringTokenizer(line, delim);
			int nt = 0;
			while(nt < 4 && st.hasMoreTokens())
				fields[nt++] = st.nextToken();
			
			if (nt < 4)
			{
				log.warn("Line {} '{}'incorrect number of pipe-separated fields. 4 required. -- Skipped.",
						 lnr.getLineNumber(), line);
				continue;
			}
			int stationId = 0;
			try { stationId = Integer.parseInt(fields[0]); }
			catch(Exception ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("Line {} '{}' bad site number in first field. Must be integer. -- Skipped.",
				   		lnr.getLineNumber(), line);
				continue;
				
			}
			String stationName = fields[1];
			
			DcpAddress dcpAddress = new DcpAddress(fields[2].toUpperCase());
			
			if (fields[3].length() == 0)
			{
				log.warn("Line {} '{}' missing data format in last field, should be A or B. -- Skipped.",
						 lnr.getLineNumber(), line);
				continue;
			}
			char formatFlag = fields[3].charAt(0);
			
			SnotelPlatformSpec spec = new SnotelPlatformSpec(stationId, stationName, 
				dcpAddress, formatFlag);

			platformSpecs.put(dcpAddress, spec);
		}
		
		lnr.close();
		lastLoadTime = System.currentTimeMillis();
	}
	
	public void addHistoryRetrieval(HistoryRetrieval hr)
	{
		platformSpecs.put(hr.getSpec().getDcpAddress(), hr.getSpec());
	}
	
	/**
	 * Test main - pass file name on commandline. It reads the file and spits it back out
	 * to stdout. Any errors are printed to stderr.
	 * @param args the one arg should be the filename to read.
	 */
	public static void main(String args[])
		throws Exception
	{
		SnotelPlatformSpecList spsl = new SnotelPlatformSpecList();
		spsl.loadFile(new File(args[0]));
		for(SnotelPlatformSpec spec : spsl.getPlatformSpecs())
			System.out.println(spec.toString());
	}

	public long getLastLoadTime()
	{
		return lastLoadTime;
	}
}
