package decodes.snotel;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.StringTokenizer;

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
			//MJM 20201110 new format is: stationId|stationName|dcpAddr|formatFlag
			line = line.trim();
			if (line.startsWith("#") || line.length() == 0)
				continue;
			String delim = ",";
			if (line.contains("|"))
				delim = "|";
//System.out.println("Read line '" + line + "', delim=" + delim);

			String fields[] = new String[4];
			StringTokenizer st = new StringTokenizer(line, delim);
			int nt = 0;
			while(nt < 4 && st.hasMoreTokens())
				fields[nt++] = st.nextToken();
			
//			String fields[] = line.split(delim);
//			if (fields.length < 4)
			if (nt < 4)
			{
				logger.warning("Line " + lnr.getLineNumber() + " '" + line
					+ "' incorrect number of pipe-separated fields. 4 required. -- Skipped.");
				continue;
			}
//for(int idx=0;idx<fields.length;idx++)System.out.println("fld[" + idx + "]=" + fields[idx]);
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
			
			if (fields[3].length() == 0)
			{
				logger.warning("Line " + lnr.getLineNumber() + " '" + line
					+ "' missing data format in last field, should be A or B. -- Skipped.");
				continue;
			}
			char formatFlag = fields[3].charAt(0);
			
			SnotelPlatformSpec spec = new SnotelPlatformSpec(stationId, stationName, 
				dcpAddress, formatFlag);

//System.out.println("Adding spec '" + spec + "'");
			platformSpecs.put(dcpAddress, spec);
		}
		
		lnr.close();
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
		Logger logger = new StderrLogger("test-snotel");
		spsl.loadFile(new File(args[0]), logger);
		for(SnotelPlatformSpec spec : spsl.getPlatformSpecs())
			System.out.println(spec.toString());
	}
}
