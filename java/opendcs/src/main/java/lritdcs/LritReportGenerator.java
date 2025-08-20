/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2012/12/12 16:08:35  mmaloney
*  Mods for 5.2
*
*  Revision 1.6  2012/12/12 16:05:40  mmaloney
*  Several updates made for new version of LRITDCS 5.2
*
*  Revision 1.5  2009/11/11 18:06:27  shweta
*  LRIT update
*
*  Revision 1.4  2009/08/14 14:02:51  shweta
*  Changes done to incorporate backup LRIT status.
*
*  Revision 1.3  2005/12/12 19:26:44  mjmaloney
*  dev
*
*  Revision 1.2  2005/10/12 14:15:20  mjmaloney
*  dev
*
*  Revision 1.1  2005/10/10 18:03:44  mjmaloney
*  dev
*
*/
package lritdcs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Properties;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.StringPair;
import ilex.xml.XmlOutputStream;


/**
This class writes the detail LRIT report.
*/
public class LritReportGenerator
{
	/** Used to format dates in the HTML report header. */
	private SimpleDateFormat headerDF;

	/** Used to format dates in columns in the HTML report. */
	public static SimpleDateFormat columnDF
		= new SimpleDateFormat("MM/dd HH:mm:ss");  //  @jve:decl-index=0:
	static { columnDF.setTimeZone(TimeZone.getTimeZone("UTC")); }

	/** Local copy of XML output stream */
	private XmlOutputStream xos;  //  @jve:decl-index=0:

	/** Used to format percentages */
	private DecimalFormat pctFormat = new DecimalFormat("###.##");

	// convenient constants used within HTML:
	private static final String top = "vertical-align: top; ";  //  @jve:decl-index=0:
	private static final String right  = "text-align: right; ";  //  @jve:decl-index=0:
	private static final String left   = "text-align: left; ";
	private static final String center = "text-align: center; ";  //  @jve:decl-index=0:
	private static final String bold   = "font-weight: bold; ";
	private static final String ital   = "font-style: italic; ";

	/** This is the default header to use if no file is provided. */
	private String defaultHeader = 
       "<h2 style=\"" + center + "\">LRIT File Generator: $HOSTNAME</h2>\n"
     + "<div style=\"" + center + "\">\n"
     + "  UTC: $DATE(MMMM dd, yyyy HH:mm:ss) (Day $DATE(DDD))\n"
     + "</div>\n"
     + "<div style=\"" + center + "\">Module Status: $MODULESTAT</div>\n"
     + "<br>\n";

	/** HTML header to include in report. */
	private String header;
 
	/** Constructs a new DetailReportGenerator */
	public LritReportGenerator()
	{
		headerDF = new SimpleDateFormat(
			"MMMM dd, yyyy  HH:mm:ss '(Day ' DDD')'");
		headerDF.setTimeZone(TimeZone.getTimeZone("UTC"));
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
		LritDcsStatus status, int scanSeconds)
	{
		Logger.instance().debug1("Generating LRIT detail report for " + host);
		FileOutputStream fos = null;
		try
		{
			File tmp = new File(output.getPath() + ".tmp");
			fos = new FileOutputStream(tmp);
			XmlOutputStream xos = new XmlOutputStream(fos, "html");
			writeReport(xos, host, status, scanSeconds);
			fos.close();
			if (!tmp.renameTo(output))
			{
				// On windows, have to explicitely delete before rename.
				output.delete();
				tmp.renameTo(output);
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
		LritDcsStatus status, int scanSeconds)
		throws IOException
	{
		// Define the styles to be used in the output:

		// Reverse Video OK & Error values
		String revOK = 
		 "color: rgb(51,255,51); font-weight: bold; background-color: black; ";
		String revWARN = 
		 "color: rgb(255,255,51); font-weight: bold; background-color: black; ";
		
		String revERR = 
			"color: #FF0000; font-weight: bold; background-color: black; ";

		String thStyle = top + center + bold + ital;

		String thCol0Style = "vertical-align: top; text-align: right; "
			+ "font-weight: bold; font-style: italic;";


		this.xos = xos;

xos.startElement("html");

  // HTML Header
xos.startElement("head");
xos.writeElement("title", "LRIT:" + host);
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
            xos.startElement("td", "colspan", "6");
              xos.writeElement("h3","style",center,"LQM Connection Status");
            xos.endElement("td");
          xos.endElement("tr");

          xos.startElement("tr");
            //String labelStyle = right + "width: 20%;";
            String labelStyle = right;
            xos.writeElement("td", "style", labelStyle, "LQM Status:");
            xos.writeElement("td", "style", 
              left + revOK + "width: 15%", status.lqmStatus);
            xos.writeElement("td", "style", labelStyle, "    ");
            xos.writeElement("td", "style", labelStyle, "    ");
            xos.writeElement("td", "style", labelStyle, 
              " Last LQM Contact:");
            xos.writeElement("td", "style", left + revOK + "width: 16%",
              columnDF.format(new Date(status.lastLqmContact)));
          xos.endElement("tr");
        xos.endElement("table");
      xos.endElement("td");
    xos.endElement("tr");

	// Hourly & Daily Data Collection Statistics Table
    xos.startElement("tr");
      xos.startElement("td");
        sp4[0] = new StringPair("style", left + "width: 100%;");
        sp4[1] = new StringPair("border", "0");
        sp4[2] = new StringPair("cellspacing", "2");
        sp4[3] = new StringPair("cellpadding", "2");
        xos.startElement("table", sp4);
          xos.startElement("tr");
            xos.startElement("td", "colspan", "8");
              xos.writeElement("h3", "style", center,
                "Incoming DCP Messages");
            xos.endElement("td");
          xos.endElement("tr");
        
          // Row with DDS server status and last retrieval
          xos.startElement("tr");
            xos.writeElement("td", "style", labelStyle + right,
              "Current DDS Server:");
            xos.writeElement("td", "style", left+revOK, "colspan", "2",
				status.lastDataSource);
            xos.writeElement("td", "style", labelStyle + right, "colspan", "4",
              "Last Msg Retrieved:");
            xos.writeElement("td", "style", left+revOK, 
              status.lastRetrieval == 0L ? "Never" :
              columnDF.format(new Date(status.lastRetrieval)));
          xos.endElement("tr");
        
          // Row with period labels
          xos.startElement("tr");
            xos.writeElement("th", "style", labelStyle + center, "   ");
            xos.writeElement("th", "style", labelStyle + center, "This Hour");
			xos.writeElement("th", "   ");
            xos.writeElement("th", "style", labelStyle + center, "Last Hour");
			xos.writeElement("th", "   ");
            xos.writeElement("th", "style", labelStyle + center, "This Day");
			xos.writeElement("th", "   ");
            xos.writeElement("th", "style", labelStyle + center, "Last Day");
          xos.endElement("tr");
        
          // Row with Incoming Hi Pri Messages
          xos.startElement("tr");
            xos.writeElement("td", "style", labelStyle + right, "High:");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.msgsThisHourHigh);
			xos.writeElement("td", "   ");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.msgsLastHourHigh);
			xos.writeElement("td", "   ");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.msgsTodayHigh);
			xos.writeElement("td", "   ");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.msgsYesterdayHigh);
          xos.endElement("tr");
        
          // Row with Incoming Medium Pri Messages
          xos.startElement("tr");
            xos.writeElement("td", "style", labelStyle + right, "Medium:");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.msgsThisHourMedium);
			xos.writeElement("td", "   ");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.msgsLastHourMedium);
			xos.writeElement("td", "   ");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.msgsTodayMedium);
			xos.writeElement("td", "   ");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.msgsYesterdayMedium);
          xos.endElement("tr");
        
          // Row with Incoming Low Pri Messages
          xos.startElement("tr");
            xos.writeElement("td", "style", labelStyle + right, "Low:");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.msgsThisHourLow);
			xos.writeElement("td", "   ");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.msgsLastHourLow);
			xos.writeElement("td", "   ");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.msgsTodayLow);
			xos.writeElement("td", "   ");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.msgsYesterdayLow);
          xos.endElement("tr");

        xos.endElement("table");
      xos.endElement("td");
    xos.endElement("tr");   // End of section containing Incoming stats.

   
    
	// Hourly & Daily Outgoing Files Section
    xos.startElement("tr");
      xos.startElement("td");
        sp4[0] = new StringPair("style", left + "width: 100%;");
        sp4[1] = new StringPair("border", "0");
        sp4[2] = new StringPair("cellspacing", "2");
        sp4[3] = new StringPair("cellpadding", "2");
        xos.startElement("table", sp4);
          xos.startElement("tr");
            xos.startElement("td", "colspan", "14");
              xos.writeElement("h3", "style", center,
                "Outgoing LRIT DCS Files");
            xos.endElement("td");
          xos.endElement("tr");

          xos.startElement("tr");
          xos.writeElement("td", "style", labelStyle + right, "Domain 2A Host:");
          xos.writeElement("td", "style", left+revOK, "colspan", "2",status.domain2Ahost);
          xos.writeElement("td", "style", labelStyle + right, "colspan", "2",
              "Last File Sent at:");
          xos.writeElement("td", "style",
            	status.domain2AStatus.equalsIgnoreCase("Error")? left+revERR: left+revOK, 
                status.lastFileSendA == 0L ? "Never" :
                    columnDF.format(new Date(status.lastFileSendA)));
          xos.writeElement("td", "   ");
          xos.writeElement("td", "style", left+revOK, "colspan", "3", status.lastFileName);
          xos.endElement("tr");
            
          xos.startElement("tr");
          xos.writeElement("td", "style", labelStyle + right, "Domain 2B Host:");
          xos.writeElement("td", "style", left+revOK, "colspan", "2",status.domain2Bhost);
          xos.writeElement("td", "style", labelStyle + right, "colspan", "2",
            "Last File Sent at:");
          xos.writeElement("td", "style",
        	  status.domain2BStatus.equalsIgnoreCase("Error")? left+revERR: left+revOK, 
              status.lastFileSendB == 0L ? "Never" : columnDF.format(new Date(status.lastFileSendB)));
              xos.endElement("tr");
          
	      xos.startElement("tr");
	      xos.writeElement("td", "style", labelStyle + right, "Domain 2C Host:");
	      xos.writeElement("td", "style", left+revOK, "colspan", "2", status.domain2Chost);
	      xos.writeElement("td", "style", labelStyle + right, "colspan", "2",
	          "Last File Sent at:");
	      xos.writeElement("td", "style",
	      	status.domain2CStatus.equalsIgnoreCase("Error") ? left+revERR : left+revOK, 
	        status.lastFileSendC == 0L ? "Never" : columnDF.format(new Date(status.lastFileSendC)));
	      xos.endElement("tr");
 
//	      xos.startElement("tr");
//	      xos.writeElement("td", "style", labelStyle + right, "PTP Directory:");
//	      xos.writeElement("td", "style", left+revOK, "colspan", "2", status.ptpDir);
//	      xos.writeElement("td", "style", labelStyle + right, "colspan", "2", "Last File Saved:");
//	      xos.writeElement("td", "style",
//	    	  status.ptpLastSave == 0L ? left+revERR : left+revOK,
//	    	  status.ptpLastSave == 0L ? "Never" : columnDF.format(new Date(status.ptpLastSave)));
//	      xos.endElement("tr");
            
       /* 
        xos.writeElement("td", "style", left+revOK, 
          status.lastRetrieval == 0L ? "Never" :
          columnDF.format(new Date(status.lastFileSend)));
      xos.endElement("tr");*/
          
          
          xos.startElement("tr");
            xos.writeElement("th", "style", labelStyle + center, "         ");
            xos.writeElement("th", "style", labelStyle + center, 
				"Queued to Send");
			xos.writeElement("th", "   ");
            xos.writeElement("th", "style", labelStyle + center, 
				"Sent This Hour");
			xos.writeElement("th", "   ");
            xos.writeElement("th", "style", labelStyle + center, 
				"Sent Last Hour");
			xos.writeElement("th", "   ");
            xos.writeElement("th", "style", labelStyle + center, 
				"Sent Today");
			xos.writeElement("th", "   ");
            xos.writeElement("th", "style", labelStyle + center, 
				"Sent Yesterday");
          xos.endElement("tr");
        
          // Row with Hi Pri Files
          xos.startElement("tr");
            xos.writeElement("td", "style", labelStyle + right, "High:");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.filesQueuedHigh);
			xos.writeElement("td", "   ");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.filesSentThisHourHigh);
			xos.writeElement("td", "   ");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.filesSentLastHourHigh);
			xos.writeElement("td", "   ");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.filesSentTodayHigh);
			xos.writeElement("td", "   ");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.filesSentYesterdayHigh);
          xos.endElement("tr");

          // Row with Med Pri Files
          xos.startElement("tr");
            xos.writeElement("td", "style", labelStyle + right, "Medium:");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.filesQueuedMedium);
			xos.writeElement("td", "   ");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.filesSentThisHourMedium);
			xos.writeElement("td", "   ");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.filesSentLastHourMedium);
			xos.writeElement("td", "   ");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.filesSentTodayMedium);
			xos.writeElement("td", "   ");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.filesSentYesterdayMedium);
          xos.endElement("tr");
        
          // Row with Low Pri Files
          xos.startElement("tr");
            xos.writeElement("td", "style", labelStyle + right, "Low:");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.filesQueuedLow);
			xos.writeElement("td", "   ");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.filesSentThisHourLow);
			xos.writeElement("td", "   ");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.filesSentLastHourLow);
			xos.writeElement("td", "   ");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.filesSentTodayLow);
			xos.writeElement("td", "   ");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.filesSentYesterdayLow);
          xos.endElement("tr");

          // Rows with Pending, Auto Retrans, & Manual
          xos.startElement("tr");
            xos.writeElement("td", "style", labelStyle + right, "Pending:");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.filesPending);
            xos.writeElement("td", "style", labelStyle + left, "colspan", "6",
				"(awaiting LQM confirmation)");
          xos.endElement("tr");
          xos.startElement("tr");
            xos.writeElement("td", "style", labelStyle + right,"Auto Retrans:");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.filesQueuedAutoRetrans);
            xos.writeElement("td", "style", labelStyle + left, "colspan", "6",
				"(will be sent with original priority)");
          xos.endElement("tr");
          xos.startElement("tr");
            xos.writeElement("td", "style", labelStyle+right,"Manual Retrans:");
            xos.writeElement("td", "style", left+revOK, 
              "" + status.filesQueuedManualRetrans);
            xos.writeElement("td", "style", labelStyle + left, "colspan", "4",
				"(will be sent low priority)");
          xos.endElement("tr");

        xos.endElement("table");
      xos.endElement("td");
    xos.endElement("tr");   // End of section containing Incoming stats.

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

	private void writeReportHeader(XmlOutputStream xos, String host, 
		LritDcsStatus status)
		throws IOException
	{
		Properties p = new Properties(System.getProperties());
		p.setProperty("HOSTNAME", host);
		p.setProperty("MODULESTAT", status.status);
		p.setProperty("TZ", "UTC");
		String expHeader = 
			EnvExpander.expand(header,p,new Date(status.serverGMT));
		xos.writeLiteral(expHeader);
	}
}

