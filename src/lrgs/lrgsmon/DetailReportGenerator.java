/*
*  $Id$
*/
package lrgs.lrgsmon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.Properties;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.StringPair;
import ilex.xml.XmlOutputStream;

import lrgs.rtstat.RtStat;
import lrgs.statusxml.LrgsStatusSnapshotExt;
import lrgs.apistatus.AttachedProcess;
import lrgs.apistatus.QualityMeasurement;
import lrgs.lrgsmain.LrgsInputInterface;

/**
This class writes the detail report for a single LRGS.
*/
public class DetailReportGenerator
{
	private static ResourceBundle labels = 
		RtStat.getLabels();
	private static ResourceBundle genericLabels = 
		RtStat.getGenericLabels();
	/** Used to format dates in the HTML report header. */
	private SimpleDateFormat headerDF;

	/** Used to format dates in columns in the HTML report. */
	private SimpleDateFormat columnDF;

	/** Local copy of XML output stream */
	private XmlOutputStream xos;

	/** Used to format percentages */
	private DecimalFormat pctFormat = new DecimalFormat("###.##");

	/** Name of image file to place in output. */
	private String imageFile;

	// convenient constants used within HTML:
	private static final String top = "vertical-align: top; ";
	private static final String right  = "text-align: right; ";
	private static final String left   = "text-align: left; ";
	private static final String center = "text-align: center; ";
	private static final String bold   = "font-weight: bold; ";
	private static final String ital   = "font-style: italic; ";

	private static final String redStatus
		= "color: red; font-weight: bold; background-color: black; ";
	private static final String yellowStatus
		= "color: yellow; font-weight: bold; background-color: black; ";
	public boolean showNetworkDcpDetail = false;

	/** This is the default header to use if no file is provided. */
	private String defaultHeader = 
       "<h2 style=\"" + center + "\">LRGS: $HOSTNAME</h2>\n"
     + "<div style=\"" + center + "\">\n"
     + "  UTC: $DATE(MMMM dd, yyyy HH:mm:ss) (Day $DATE(DDD))\n"
     + "</div>\n"
     + "<div style=\"" + center + "\">" + labels.getString(
     		"DetailReportGenerator.timeReportedLrgs") +"</div>\n"
     + "<div style=\"$STATSTYLE\">System Status: $SYSTEMSTAT</div>\n"
     + "<div style=\"" + center + "\">LRGS Version: $LRGSVERSION</div>\n"
     + "<br>\n";

	/** HTML header to include in report. */
	private String header;
 
	/** Constructs a new DetailReportGenerator */
	public DetailReportGenerator(String imageFile)
	{
		headerDF = new SimpleDateFormat(
			"MMMM dd, yyyy  HH:mm:ss '(Day ' DDD')'");
		headerDF.setTimeZone(TimeZone.getTimeZone("UTC"));
		columnDF = new SimpleDateFormat("MM/dd HH:mm:ss");
		columnDF.setTimeZone(TimeZone.getTimeZone("UTC"));
		this.imageFile = imageFile;
		this.header = defaultHeader;
	}

	/**
	 * Sets the header to the contents of a named file.
	 * @param filename the header file name.
	 * @throws IOException if file can't be read.
	 */
	public void setHeader(String filename)
		throws IOException
	{
		if (filename == null)
		{
			header = defaultHeader;
			return;
		}
		File f = new File(filename);
		FileReader fr = new FileReader(f);
		BufferedReader br = new BufferedReader(fr);
		StringBuffer sb = new StringBuffer((int)f.length());
		String line;
		while( (line = br.readLine()) != null)
			sb.append(line + "\n");
		br.close();
		fr.close();
		header = sb.toString();
	}

	/** Writes the detail report. */
	public void write(File output, String host, 
		LrgsStatusSnapshotExt status, int scanSeconds)
	{
//		Logger.instance().debug1("Generating detail report for " + host);
		FileOutputStream fos = null;
		try
		{
			File tmp = new File(output.getPath() + ".tmp");
			fos = new FileOutputStream(tmp);
			XmlOutputStream xos = new XmlOutputStream(fos, "html");
			writeReport(xos, host, status, scanSeconds);
			fos.close();
			fos = null;
			if (!tmp.renameTo(output))
			{
				// On windows, have to explicitely delete before rename.
				output.delete();
				if (!tmp.renameTo(output))
				{
					Logger.instance().warning(
						"LRGS Status report move failed. This can happen on a "
						+ "Windows LRGS if the LRGSHOME directory is being "
						+ "dynamically scanned by your AV. Recommend removing "
						+ "the LRGSHOME directory from the AV scan.");
				}
			}
		}
		catch(IOException ex)
		{
			Logger.instance().warning("Cannot write " + output.getPath()
				+ ": " + ex);
		}
		finally
		{
			if (fos != null)
			{
				try { fos.close(); }
				catch(IOException ex){}
			}
		}
	}

	public void writeReport(XmlOutputStream xos, String host, 
		LrgsStatusSnapshotExt status, int scanSeconds)
		throws IOException
	{
		// Define the styles to be used in the output:

		// Reverse Video OK & Error values
		String revOK = 
		 "color: rgb(51,255,51); font-weight: bold; background-color: black; ";
		String revERR = 
		 "color: rgb(255,255,51); font-weight: bold; background-color: black; ";

		String thStyle = top + center + bold + ital;

		String thCol0Style = "vertical-align: top; text-align: right; "
			+ "font-weight: bold; font-style: italic;";


		this.xos = xos;

		xos.startElement("html");

		// HTML Header
		xos.startElement("head");
		xos.writeElement("title", host);
		if (scanSeconds > 0)
			xos.writeElement("meta", "http-equiv", "refresh", "CONTENT", 
				"" + scanSeconds, null);
		xos.endElement("head");

		xos.startElement("body", "style", "background-color: white;");

		// First part of body is the report header, formatted in a table.
		writeReportHeader(xos, host, status);

	StringPair sp4[] = new StringPair[4];
    sp4[0] = new StringPair("cellpadding", "2");
    sp4[1] = new StringPair("cellspacing", "2");
    sp4[2] = new StringPair("border", "1");
    sp4[3] = new StringPair("style", "text-align: left; width: 100%;");
    xos.startElement("table", sp4);
        xos.startElement("tr");
          xos.startElement("td");
            sp4[0] = new StringPair("cellpadding", "2");
            sp4[1] = new StringPair("cellspacing", "2");
            sp4[2] = new StringPair("border", "0");
            sp4[3] = new StringPair("style", "width: 100%;");
            xos.startElement("table", sp4);

              xos.startElement("tr");
                xos.startElement("td", "colspan", "7");
                  xos.writeElement("h3", "style",center,
                		  labels.getString(
                			"DetailReportGenerator.archiveStatistics"));
                xos.endElement("td");
              xos.endElement("tr");

              xos.startElement("tr");
                String labelStyle = right + "width: 20%;";
                xos.writeElement("td", "style", labelStyle, 
					labels.getString("DetailReportGenerator.messagesStorage"));
                xos.writeElement("td", "style", 
					left + revOK + "width: 10%", ""+status.lss.arcStats.dirSize);
                xos.writeElement("td", "style", labelStyle,labels.getString( 
						"DetailReportGenerator.oldestMsgTime"));
                xos.writeElement("td", "style", left + revOK + "width: 16%",
					columnDF.format(
						new Date(status.lss.arcStats.oldestMsgTime * 1000L)));

                xos.writeElement("td", "style", labelStyle,labels.getString( 
                		"DetailReportGenerator.nextIdx"));
                xos.writeElement("td", "style", 
					left + revOK + "width: 10%", ""+status.lss.arcStats.dirNext);
              xos.endElement("tr");
            xos.endElement("table");
          xos.endElement("td");
        xos.endElement("tr");

	// Hourly Data Collection Statistics Table
        xos.startElement("tr");
          xos.startElement("td");
            sp4[0] = new StringPair("style", left + "width: 100%;");
            sp4[1] = new StringPair("border", "0");
            sp4[2] = new StringPair("cellspacing", "2");
            sp4[3] = new StringPair("cellpadding", "2");
            xos.startElement("table", sp4);
                xos.startElement("tr");
                  xos.startElement("td", "colspan", "9");
                    xos.writeElement("h3", "style", center,labels.getString(
						"DetailReportGenerator.hourlyDCStat"));
                  xos.endElement("td");
                xos.endElement("tr");

				// Row with hour names
                xos.startElement("tr");
                  xos.writeElement("th", "style", thStyle + right, 
                		  labels.getString("DetailReportGenerator.hour"));
                  int hr = (status.lss.currentHour + 17) % 24;
				  for(int i=0; i<8; i++)
                  {
                    String hrString = "" + hr + "-" + ((hr+1)%24);
                    xos.writeElement("th", "style", thStyle, hrString);
                    hr = (hr + 1) % 24;
                  }
                xos.endElement("tr");

			  QualityMeasurement qual[] = status.getDownlinkQualityHistory(
				LrgsInputInterface.DL_DOMSAT);
			  if (qual != null)
			  {
				if (status.majorVersion < 5)
					qual = status.lss.qualMeas;
                xos.startElement("tr");
                  xos.writeElement("td", "style", right, 
					"DOMSAT (Good/"
					+ (status.majorVersion < 5 ? "Dropped):" : "ParErr):"));
                  hr = (status.lss.currentHour + 17) % 24;
				  for(int i=0; i<8; i++)
                  {
	                int g = qual[hr] != null ? qual[hr].numGood : 0;
                    String v = "" + g;
					int e = qual[hr] != null ? qual[hr].numDropped : 0;
					if (e != 0)
						v = v + " / " + e;
                    xos.writeElement("td", "style", revOK + center, v);
                    hr = (hr + 1) % 24;
                  }
                xos.endElement("tr");
			  }

			  qual = status.getDownlinkQualityHistory(
				LrgsInputInterface.DL_NETBAK);
			  if (qual != null)
			  {
				qual = status.lss.qualMeas;
                xos.startElement("tr");
                  xos.writeElement("td", "style", right, 
					"Old NetBack (Recovered):");
                  hr = (status.lss.currentHour + 17) % 24;
				  for(int i=0; i<8; i++)
                  {
                    String v = "" 
						+ (qual[hr] != null ? qual[hr].numRecovered : 0);
                    xos.writeElement("td", "style", revOK + center, v);
                    hr = (hr + 1) % 24;
                  }
                xos.endElement("tr");
			  }

			  if ((status.majorVersion > 5
			       || (status.majorVersion == 5 && status.minorVersion >= 3))
			   && status.getDownlinkQualityHistory(
					LrgsInputInterface.DL_DOMSAT) != null
			   && status.domsatDropped != null)
			  {
                xos.startElement("tr");
                  xos.writeElement("td", "style", right, 
					"DOMSAT Dropped:");
                  hr = (status.lss.currentHour + 17) % 24;
				  for(int i=0; i<8; i++)
                  {
                    String v = "" + status.domsatDropped[hr];
                    xos.writeElement("td", "style", revOK + center, v);
                    hr = (hr + 1) % 24;
                  }
                xos.endElement("tr");
			  }
			
			

				// If there is a DRGS connection, print an extra row for it.
				qual = 
				  status.getDownlinkQualityHistory(LrgsInputInterface.DL_DRGS);
				if (qual != null
				 && status.getDownlinkQualityHistory(
					LrgsInputInterface.DL_DRGSCON) != null)
				{
	                xos.startElement("tr");
	                  xos.writeElement("td", "style", right, 
						"GOES DRGS"
						+ (status.majorVersion < 5 ? ":" : " (Good/ParErr):"));
	                  hr = (status.lss.currentHour + 17) % 24;
					  for(int i=0; i<8; i++)
	                  {
	                    int g = qual[hr] != null ? qual[hr].numGood : 0;
                    	String v = "" + g;
						if (status.majorVersion >= 5)
						{
							int e = qual[hr] != null ? qual[hr].numDropped : 0;
					    	if (e != 0)
								v = v + " / " + e;
						}
                    	xos.writeElement("td", "style", revOK + center, v);
	                    hr = (hr + 1) % 24;
	                  }
	                xos.endElement("tr");
				}

				// If there is a GR3110 connection, print an extra row for it.
				qual = 
				  status.getDownlinkQualityHistory(LrgsInputInterface.DL_GR3110);
				if (qual != null)
				{
	                xos.startElement("tr");
	                  xos.writeElement("td", "style", right, "GR3110");
	                  hr = (status.lss.currentHour + 17) % 24;
					  for(int i=0; i<8; i++)
	                  {
	                    int g = qual[hr] != null ? qual[hr].numGood : 0;
                    	String v = "" + g;
						int e = qual[hr] != null ? qual[hr].numDropped : 0;
					    if (e != 0)
							v = v + " / " + e;
                    	xos.writeElement("td", "style", revOK + center, v);
	                    hr = (hr + 1) % 24;
	                  }
	                xos.endElement("tr");
				}

				// If there is a DDS Recv connection, print an extra row for it.
				qual = 
					status.getDownlinkQualityHistory(LrgsInputInterface.DL_DDS);
				if (qual != null)
				{
	                xos.startElement("tr");
	                  xos.writeElement("td", "style", right, 
						"DDS Recv (Good/ParErr):");
	                  hr = (status.lss.currentHour + 17) % 24;
					  for(int i=0; i<8; i++)
	                  {
						  
	                    int g = qual[hr] != null ? qual[hr].numGood : 0;	                  
                    	String v = "" + g;                    	
						int e = qual[hr] != null ? qual[hr].numDropped : 0;
					    if (e != 0)
							v = v + " / " + e;
                    	xos.writeElement("td", "style", revOK + center, v);
	                    hr = (hr + 1) % 24;
	                  }
	                xos.endElement("tr");
				}

				// If there is a Secondary DDS Recv connection, print an extra row for it.
				qual = 
					status.getDownlinkQualityHistory(LrgsInputInterface.DL_DDS_SECONDRAY);
				if (qual != null)
				{
					int j = 0;
					String strArr[] = new String[8];
					hr = (status.lss.currentHour + 17) % 24;
					for(int i=0; i<8; i++)
	                  {
						int g = qual[hr] != null ? qual[hr].numGood : 0;	                   
						String v = "" + g;                    
						int e = qual[hr] != null ? qual[hr].numDropped : 0;
					    if (e != 0)
							v = v + " / " + e;					    
	                    hr = (hr + 1) % 24;
	                   strArr[i]=v;
	                    if(v.equalsIgnoreCase("0"))
	                    	j++;
	                  }
					
					if(j!=8)
					{
	                xos.startElement("tr");
	                  xos.writeElement("td", "style", right, 
						"DDS Recv:Secondary (Good/ParErr):");
	                  for(int i=0;i<8;i++)
	                  {
	                	  xos.writeElement("td", "style", revOK + center, strArr[i]);  
	                  }	                 
	                xos.endElement("tr");
					}
				}
				
				// If there is an LRIT Interface, print an extra row for it.
				qual = status.getDownlinkQualityHistory(
					LrgsInputInterface.DL_LRIT);
				if (qual != null)
				{
	                xos.startElement("tr");
	                  xos.writeElement("td", "style", right, 
						"LRIT (Good/ParErr):");
	                  hr = (status.lss.currentHour + 17) % 24;
					  for(int i=0; i<8; i++)
	                  {
	                    int g = qual[hr] != null ? qual[hr].numGood
						      : 0;
                    	String v = "" + g;
						int e = qual[hr] != null ? qual[hr].numDropped : 0;
					    if (e != 0)
							v = v + " / " + e;
                    	xos.writeElement("td", "style", revOK + center, v);
	                    hr = (hr + 1) % 24;
	                  }
	                xos.endElement("tr");
				}

				// If there is a NOAAPORT Interface, print an extra row for it.
				qual = status.getDownlinkQualityHistory(
					LrgsInputInterface.DL_NOAAPORT);
				if (qual != null)
				{
	                xos.startElement("tr");
	                  xos.writeElement("td", "style", right, 
						"NOAAPORT (Good/ParErr):");
	                  hr = (status.lss.currentHour + 17) % 24;
					  for(int i=0; i<8; i++)
	                  {
	                    int g = qual[hr] != null ? qual[hr].numGood : 0;
                    	String v = "" + g;
						int e = qual[hr] != null ? qual[hr].numDropped : 0;
					    if (e != 0)
							v = v + " / " + e;
                    	xos.writeElement("td", "style", revOK + center, v);
	                    hr = (hr + 1) % 24;
	                  }
	                xos.endElement("tr");
				}

				// If there is a Network DCP main, print an extra row for it.
				qual = status.getDownlinkQualityHistory(
					  LrgsInputInterface.DL_NETWORKDCP);
				if (qual != null)
				{
	                xos.startElement("tr");
	                  xos.writeElement("td", "style", right, 
						"Network DCP (Good/ParErr):");
	                  hr = (status.lss.currentHour + 17) % 24;
					  for(int i=0; i<8; i++)
	                  {
	                    int g = qual[hr] != null ? qual[hr].numGood : 0;
                    	String v = "" + g;
						if (status.majorVersion >= 5)
						{
							int e = qual[hr] != null ? qual[hr].numDropped : 0;
					    	if (e != 0)
								v = v + " / " + e;
						}
                    	xos.writeElement("td", "style", revOK + center, v);
	                    hr = (hr + 1) % 24;
	                  }
	                xos.endElement("tr");
				}
				
				// If there is a Iridium, print an extra row for it.
				qual = status.getDownlinkQualityHistory(
					  LrgsInputInterface.DL_IRIDIUM);
				if (qual != null)
				{
	                xos.startElement("tr");
	                  xos.writeElement("td", "style", right, 
						"Iridium (Good/Has Errors):");
	                  hr = (status.lss.currentHour + 17) % 24;
					  for(int i=0; i<8; i++)
	                  {
	                    int g = qual[hr] != null ? qual[hr].numGood : 0;
                    	String v = "" + g;
						if (status.majorVersion >= 5)
						{
							int e = qual[hr] != null ? qual[hr].numDropped : 0;
					    	if (e != 0)
								v = v + " / " + e;
						}
                    	xos.writeElement("td", "style", revOK + center, v);
	                    hr = (hr + 1) % 24;
	                  }
	                xos.endElement("tr");
				}
				
				// If there is a DCP_SESSION_MGR, print an extra row for it.
				qual = status.getDownlinkQualityHistory(
					  LrgsInputInterface.DL_SESSIONMGR);
				if (qual != null)
				{
	                xos.startElement("tr");
	                  xos.writeElement("td", "style", right, 
						"DCP Polling Sessions:");
	                  hr = (status.lss.currentHour + 17) % 24;
					  for(int i=0; i<8; i++)
	                  {
	                    int g = qual[hr] != null ? qual[hr].numGood : 0;
                    	String v = "" + g;
						if (status.majorVersion >= 5)
						{
							int e = qual[hr] != null ? qual[hr].numDropped : 0;
					    	if (e != 0)
								v = v + " / " + e;
						}
                    	xos.writeElement("td", "style", revOK + center, v);
	                    hr = (hr + 1) % 24;
	                  }
	                xos.endElement("tr");
				}

				// If there is a EDL_INPUT, print an extra row for it.
				qual = status.getDownlinkQualityHistory(LrgsInputInterface.DL_EDL);
				if (qual != null)
				{
	                xos.startElement("tr");
	                  xos.writeElement("td", "style", right, 
						"EDL Ingest:");
	                  hr = (status.lss.currentHour + 17) % 24;
					  for(int i=0; i<8; i++)
	                  {
	                    int g = qual[hr] != null ? qual[hr].numGood : 0;
                    	String v = "" + g;
						if (status.majorVersion >= 5)
						{
							int e = qual[hr] != null ? qual[hr].numDropped : 0;
					    	if (e != 0)
								v = v + " / " + e;
						}
                    	xos.writeElement("td", "style", revOK + center, v);
	                    hr = (hr + 1) % 24;
	                  }
	                xos.endElement("tr");
				}

				
				// Print row with number of messages ARCHIVED
				qual = status.lss.qualMeas;
	            xos.startElement("tr");
	            xos.writeElement("td", "style", right, 
					status.majorVersion < 5 ? "Archived:" :
						"Archived (Good/ParErr):");
	            hr = (status.lss.currentHour + 17) % 24;
				for(int i=0; i<8; i++)
	            {
	                int g = qual[hr] != null ? qual[hr].numGood : 0;
                    String v = "" + g;
					if (status.majorVersion >= 5)
					{
						int e = qual[hr] != null ? qual[hr].numDropped : 0;
						if (e != 0)
							v = v + " / " + e;
					}
                   	xos.writeElement("td", "style", revOK + center, v);
	                hr = (hr + 1) % 24;
	            }
	            xos.endElement("tr");
            xos.endElement("table");
          xos.endElement("td");
        xos.endElement("tr");

	// Downlink Statistics Table
        xos.startElement("tr");
          xos.startElement("td");
            sp4[0] = new StringPair("style", left + "width: 100%;");
            sp4[1] = new StringPair("border", "0");
            sp4[2] = new StringPair("cellspacing", "2");
            sp4[3] = new StringPair("cellpadding", "2");
            xos.startElement("table", sp4);

                xos.startElement("tr");
                  xos.startElement("td", "colspan", "6");
                    xos.writeElement("h3", "style", center,
						labels.getString(
								"DetailReportGenerator.downlinkStats"));
                  xos.endElement("td");
                xos.endElement("tr");

                xos.startElement("tr");
                  xos.writeElement("th", "style", thStyle, labels.getString(
                		  "DetailReportGenerator.downlinkName"));
                  xos.writeElement("th", "style", thStyle, labels.getString(
                		  "DetailReportGenerator.lastMsgRecvT"));
//                  xos.writeElement("th", "style", thStyle, labels.getString(
//                		  "DetailReportGenerator.linkType"));
                  xos.writeElement("th", "style", thStyle, labels.getString(
                		  "DetailReportGenerator.lastSeqNum"));
                  xos.writeElement("th", "style", thStyle, labels.getString(
                		  "DetailReportGenerator.linkStatus"));
                  xos.writeElement("th", "style", thStyle, labels.getString(
                		  "DetailReportGenerator.linkParams"));
                xos.endElement("tr");

			if (status.lss.downLinks != null)
			{
                for(int i=0; i<status.lss.downLinks.length; i++)
                {
                  if (status.lss.downLinks[i] == null
				   || status.lss.downLinks[i].type == 
						LrgsInputInterface.DL_UNUSED
				   || status.lss.downLinks[i].type == 
						LrgsInputInterface.DL_DRGS
                   || status.lss.downLinks[i].type ==
                	   LrgsInputInterface.DL_NETDCPCONT
                   || status.lss.downLinks[i].type ==
                	   LrgsInputInterface.DL_DDS
                   || status.lss.downLinks[i].type ==
                    	   LrgsInputInterface.DL_DDS_SECONDRAY)
                	  
                  {
                    continue;
                  }
                   
                  xos.startElement("tr");
                    String nm = status.lss.downLinks[i].name;
                    if (status.lss.downLinks[i].type == 
                    	LrgsInputInterface.DL_NETWORKDCP
                     && showNetworkDcpDetail)
                    {
                    	xos.writeElement("a", "href", 
                    		"http://local/showNetworkDcps", nm);
                    }
                    else
                    	xos.writeElement("td", nm);
					int lmrt = status.lss.downLinks[i].lastMsgRecvTime;
					String lmrts = lmrt == 0 ? "(none)" : 
						columnDF.format(new Date(lmrt * 1000L));
                    xos.writeElement("td", "style", center, lmrts);
                    int t = status.lss.downLinks[i].type;

                    xos.writeElement("td", "style", center,
						""+status.lss.downLinks[i].lastSeqNum);

                    String statstr = status.lss.downLinks[i].statusString;
                    if (statstr == null || statstr.trim().length() == 0)
	                    switch(status.lss.downLinks[i].statusCode)
	                    {
	                    case 0: statstr = "Disabled"; break;
	                    case 1: statstr = "Initializing"; break;
	                    case 2: statstr = "Active"; break;
	                    case 3: statstr = "Timeout"; break;
	                    case 4: statstr = "Error"; break;
	                    }
                    xos.writeElement("td", "style", center, statstr);
                   
                    //display connection group under Link Params on page.                                 
                    xos.writeElement("td", "style", center,status.lss.downLinks[i].group);
                    xos.endElement("tr");
                 
                   /* String v = status.lss.downLinks[i].BER;
                    xos.writeElement("td", "style", left,
						(v != null && v.length() > 0) ? "BER="+v : "");
                  xos.endElement("tr");*/ 

                }
			}

            xos.endElement("table");
          xos.endElement("td");
        xos.endElement("tr");

        xos.startElement("tr");
          xos.startElement("td");
            sp4[0] = new StringPair("style", left + "width: 100%;");
            sp4[1] = new StringPair("border", "0");
            sp4[2] = new StringPair("cellspacing", "2");
            sp4[3] = new StringPair("cellpadding", "2");
            xos.startElement("table", sp4);

                xos.startElement("tr");
                  xos.startElement("td", "colspan", "7");
                    xos.writeElement("h3", "style", center,
                    	labels.getString("DetailReportGenerator.clientStats"));
                  xos.endElement("td");
                xos.endElement("tr");

                xos.startElement("tr");
                  xos.writeElement("th", "style", thStyle, 
               		  labels.getString("DetailReportGenerator.slot"));
                  xos.writeElement("th", "style", thStyle, 
               		  labels.getString("DetailReportGenerator.hostName"));
//                  xos.writeElement("th", "style", thStyle, 
//               		  labels.getString("DetailReportGenerator.clientType"));
                  xos.writeElement("th", "style", thStyle, 
               		  labels.getString("DetailReportGenerator.user"));
                  xos.writeElement("th", "style", thStyle, 
               		  labels.getString("DetailReportGenerator.msgCount"));
                  xos.writeElement("th", "style", thStyle, 
               		  labels.getString("DetailReportGenerator.lastActivityTime"));
                  xos.writeElement("th", "style", thStyle, 
               		  labels.getString("DetailReportGenerator.lastMsgTime"));
                  xos.writeElement("th", "style", thStyle, 
               		  labels.getString("DetailReportGenerator.status"));
                xos.endElement("tr");

			if (status.lss.attProcs != null)
			{
                for(int i=0; i<status.lss.attProcs.length; i++)
                {
                  AttachedProcess ap = status.lss.attProcs[i];
                  if (ap == null || ap.pid == -1)
                  {
                    continue;
                  }
                   
                  xos.startElement("tr");
                    xos.writeElement("td", "style", center, "" + i);
                    xos.writeElement("td", "style", center, ap.getName());
//                    xos.writeElement("td", "style", center, ap.type);
                    xos.writeElement("td", "style", center, ap.user);
                    xos.writeElement("td", "style", center, "" + ap.lastSeqNum);
                    xos.writeElement("td","style", center, fmtColTime(ap.lastPollTime));
                    xos.writeElement("td","style", center, fmtColTime(ap.lastMsgTime));
                    xos.writeElement("td", "style", center, ap.status);
                  xos.endElement("tr");
                }
			}

            xos.endElement("table");
          xos.endElement("td");
        xos.endElement("tr");
    xos.endElement("table");
  xos.endElement("body");
xos.endElement("html");
	}

	private void br()
		throws IOException
	{
		xos.writeLiteral("<br>");
    	//xos.writeElement("br", null);
	}
	
	private String fmtColTime(int timet)
	{
		if (timet <= 0)
			return "-";
		return columnDF.format(new Date(timet*1000L));
	}

	private void writeReportHeader(XmlOutputStream xos, String host, 
		LrgsStatusSnapshotExt status)
		throws IOException
	{
		Properties p = new Properties(System.getProperties());
		p.setProperty("IMAGEFILE", imageFile);
		p.setProperty("HOSTNAME", host);

		if (status.fullVersion == null)
			p.setProperty("LRGSVERSION", "" + status.majorVersion + "."
				+ status.minorVersion);
		else
			p.setProperty("LRGSVERSION", status.fullVersion);

		p.setProperty("TZ", "UTC");
		String s = status.systemStatus;
		if (s.startsWith("R:"))
		{
			p.setProperty("STATSTYLE", redStatus + center);
			p.setProperty("SYSTEMSTAT", s.substring(2));
		}
		else if (s.startsWith("Y:"))
		{
			p.setProperty("STATSTYLE", yellowStatus + center);
			p.setProperty("SYSTEMSTAT", s.substring(2));
		}
		else
		{
			p.setProperty("STATSTYLE", center);
			p.setProperty("SYSTEMSTAT", s);
		}
		String expHeader = 
			EnvExpander.expand(header,p,new Date(status.lss.lrgsTime * 1000L));
		xos.writeLiteral(expHeader);
	}
}

