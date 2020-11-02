package decodes.snotel;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Collection;
import java.util.HashMap;

import ilex.util.Logger;
import ilex.util.StderrLogger;
import lrgs.common.DcpAddress;

/**
 * This class maintains a collection of SnotelPlatformSpec that
 * are read from a designated file.
 */
public class SnotelPlatformSpecList
{
	private HashMap<DcpAddress, SnotelPlatformSpec> platformSpecs
		= new HashMap<DcpAddress, SnotelPlatformSpec>();
	
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

	public void loadFile(File specFile, Logger logger)
		throws IOException
	{
		platformSpecs.clear();
		
		// open filename with line number reader
		LineNumberReader lnr = new LineNumberReader(new FileReader(specFile));
		String line;
		while ((line = lnr.readLine()) != null)
		{
			line = line.trim();
			if (line.startsWith("#") || line.length() == 0)
				continue;
			
			String fields[] = line.split(",");
			if (fields.length != 6)
			{
				logger.warning("Line " + lnr.getLineNumber() + " '" + line
					+ "' incorrect number of comma-separated fields. 6 required. -- Skipped.");
				continue;
			}
			int stationId = 0;
			try { stationId = Integer.parseInt(fields[0]); }
			catch(Exception ex)
			{
				logger.warning("Line " + lnr.getLineNumber() + " '" + line
					+ "' bad site number in first field. Must be integer. -- Skipped.");
				continue;
				
			}
			String stationName = fields[1];
			DcpAddress dcpAddress = new DcpAddress(fields[2].toUpperCase());
			
			int numChannels = 0;
			try { numChannels = Integer.parseInt(fields[3]); }
			catch(Exception ex)
			{
				logger.warning("Line " + lnr.getLineNumber() + " '" + line
					+ "' bad number of channels '" + fields[3] 
					+ "'. Must be integer. -- Skipped.");
				continue;
			}
		
			int numHours = 0;
			try { numHours = Integer.parseInt(fields[4]); }
			catch(Exception ex)
			{
				logger.warning("Line " + lnr.getLineNumber() + " '" + line
					+ "' bad number of hours '" + fields[4] 
					+ "'. Must be integer. -- Skipped.");
				continue;
			}
			
			if (fields[5].length() == 0)
			{
				logger.warning("Line " + lnr.getLineNumber() + " '" + line
					+ "' missing data format in last field, should be B or A. -- Skipped.");
				continue;
			}
			
			SnotelPlatformSpec spec = new SnotelPlatformSpec(stationId, 
				stationName, dcpAddress, numChannels, numHours, fields[5].charAt(0));

			platformSpecs.put(dcpAddress, spec);
		}
		
		lnr.close();
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
		Logger logger = new StderrLogger("test-snotel");
		spsl.loadFile(new File(args[0]), logger);
		for(SnotelPlatformSpec spec : spsl.getPlatformSpecs())
			System.out.println(spec.toString());
	}
}
