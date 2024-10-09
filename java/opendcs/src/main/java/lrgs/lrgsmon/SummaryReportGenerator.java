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
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.Vector;
import java.util.Iterator;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.StringPair;
import ilex.util.TextUtil;
import ilex.xml.XmlOutputStream;

import lrgs.rtstat.RtStat;
import lrgs.statusxml.LrgsStatusSnapshotExt;
import lrgs.apistatus.AttachedProcess;
import lrgs.apistatus.DownLink;
import lrgs.apistatus.QualityMeasurement;
import lrgs.lrgsmain.LrgsInputInterface;

/**
This class writes the detail report for a single LRGS.
*/
public class SummaryReportGenerator
{
	private static ResourceBundle labels = 
		RtStat.getLabels();
	private static ResourceBundle genericLabels = 
		RtStat.getGenericLabels();
	/// Used to format dates in the HTML report header.
	private SimpleDateFormat headerDF;

	/// Used to format dates in columns in the HTML report.
	private SimpleDateFormat columnDF;

	/// Local copy of XML output stream
	private XmlOutputStream xos;

	/// HTML style string to align to top
	private static final String vTop = "vertical-align: top; ";

	/// Holds SummaryInfo objects for each LRGS.
	ArrayList<SummaryInfo> lrgsSummaries;

	/// Name of image file to place in output.
	private String imageFile;

	/** HTML header to include in report. */
	private String header;

	/// Constructs a new SummaryReportGenerator
	public SummaryReportGenerator(String imageFile)
	{
		headerDF = new SimpleDateFormat("MMMM dd, yyyy  HH:mm:ss '(Day ' DDD')'");
		headerDF.setTimeZone(TimeZone.getTimeZone("UTC"));
		columnDF = new SimpleDateFormat("MM/dd HH:mm:ss");
		columnDF.setTimeZone(TimeZone.getTimeZone("UTC"));
		lrgsSummaries = new ArrayList<SummaryInfo>();
		this.imageFile = imageFile;
		this.header = setDefaultHeader();//defaultHeader;
	}

	private String setDefaultHeader()
	{
		String defaultHeader =
			"<table cellpadding=\"2\" cellspacing=\"2\" border=\"0\" " +
								"style=\"text-align: left; width: 100%;\">\n" +
		      "<tbody>\n" +
		        "<tr>\n" +
		          "<td style=\"vertical-align: top; \">\n" +
		          	"<img src=\"" + imageFile +"\" alt=\"SatDish\"></td>\n" +
		          "<td style=\"vertical-align: top; text-align: center;\">\n" +
		            "<h2>" + 
		            	labels.getString("SummaryReportGenerator.title")+
		            "</h2>\n" +
		            "<div style=\"text-align: center;\">\n" +
		              "UTC: $DATE(MMMM dd, yyyy HH:mm:ss) (Day $DATE(DDD))\n"+
		            "</div>\n" + 
		          "</td>\n" +
		        "</tr>\n" + 
		      "</tbody>\n" +
		    "</table>\n";
		return defaultHeader;
	}
	
	/**
	  Updates this host's summary info in internal tables, does not write report.
	*/
	public synchronized void update(String host, LrgsStatusSnapshotExt status,
		String extHost)
	{
		get(host).update(status, extHost);
	}

	/// Called to initiate monitoring on a particular host.
	public synchronized void monitor(String host)
	{
		get(host);
	}

	/// Clears the list of hosts being monitored.
	public synchronized void clear()
	{
		lrgsSummaries.clear();
	}

	/// Gets the summary struct by host, creates new if necessary.
	private SummaryInfo get(String host)
	{
		for(SummaryInfo si : lrgsSummaries)
			if (si.host.equalsIgnoreCase(host))
				return si;

		SummaryInfo si = new SummaryInfo(host);
		lrgsSummaries.add(si);
		return si;
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
			header = setDefaultHeader();//defaultHeader;
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
	
	/** Writes the detail report containing accumulated summary info */
	public synchronized void write(File output, int scanSeconds)
	{
		Logger.instance().debug1("Generating summary report.");
		FileOutputStream fos = null;
		try
		{
			File tmp = new File(output.getPath() + ".tmp");
			fos = new FileOutputStream(tmp);
			XmlOutputStream xos = new XmlOutputStream(fos, "html");
			writeReport(xos, scanSeconds);
			fos.close();
			if (!tmp.renameTo(output))
			{
				// on windows, have to delete before rename
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

	private void writeReport(XmlOutputStream xos, int scanSeconds)
		throws IOException
	{
		this.xos = xos;
		xos.xmlDtdUri = "-//W3C//DTD HTML 4.01 Transitional//EN";
		xos.xmlDtdScope = "PUBLIC";
//		xos.writeXmlHeader();
xos.startElement("html");

// HTML Header
  xos.startElement("head");
    xos.writeElement("meta", "http-equiv", "content-type",
		"content", "text/html; charset=ISO-8859-1", null);
    xos.writeElement("meta", "http-equiv", "refresh", "CONTENT", "" + scanSeconds, null);
    xos.writeElement("title", labels.getString(
    						"SummaryReportGenerator.reportTitle"));
    StringPair sp3[] = new StringPair[3];
//    sp3[0] = new StringPair("rel", "stylesheet");
//    sp3[1] = new StringPair("type", "text/css");
//    sp3[2] = new StringPair("href", "lrgsmon.css");
//    xos.writeElement("link", sp3, null);
	xos.writeLiteral(
		"<link rel=\"stylesheet\" type=\"text/css\" href=\"lrgsmon.css\">");

  xos.endElement("head");

// First part of body is the report header, formatted in a table.
  xos.startElement("body");
    StringPair sp4[] = new StringPair[4];
    sp4[0] = new StringPair("cellpadding", "2");
    sp4[1] = new StringPair("cellspacing", "2");
    sp4[2] = new StringPair("border", "0");
    sp4[3] = new StringPair("style", "text-align: left; width: 100%;");

    //First part of body is the report header, formatted in a table.
	writeReportHeader(xos);
// replace from here
//    xos.startElement("table", sp4);
//      xos.startElement("tbody");
//        xos.startElement("tr");
//		if (imageFile.length() > 0 && !imageFile.equals("-"))
//		{
//          xos.startElement("td", "style", vTop);
//			xos.writeLiteral("<img src=\"" + imageFile + "\" alt=\"SatDish\">");
//          xos.endElement("td");
//		}
//
//          xos.startElement("td", "style", vTop + "text-align: center;");
//            xos.writeElement("h2", "LRGS Summary Status");
//            xos.writeElement("div", "style", "text-align: center;", 
//              "UTC: " + headerDF.format(new Date()));
//          xos.endElement("td");
//        xos.endElement("tr");
//      xos.endElement("tbody");
//    xos.endElement("table");
// ... to here
    br();

    sp4[0] = new StringPair("style", "text-align: left; width: 100%;");
    sp4[1] = new StringPair("border", "1");
    sp4[2] = new StringPair("cellspacing", "2");
    sp4[3] = new StringPair("cellpadding", "2");
    xos.startElement("table", sp4);
      xos.startElement("tbody");
        xos.startElement("tr");
          String style = "width: 26%; ";
          xos.writeElement("th", "style", style, labels.getString(
        		  "SummaryReportGenerator.hostName"));
          style = "width: 14%; ";
          xos.writeElement("th", "style", style, labels.getString(
        		  "SummaryReportGenerator.statusTime"));
          style = "width: 10%; ";
          xos.writeElement("th", "style", style, labels.getString(
        		  "SummaryReportGenerator.LRGSStatus"));
          style = "width: 10%; ";
          xos.startElement("th", "style", style);
            xos.writePCDATA(labels.getString(
            		"SummaryReportGenerator.column1A")); br();
            xos.writePCDATA(labels.getString(
            		"SummaryReportGenerator.column1B")); br();
            xos.writePCDATA(labels.getString(
    				"SummaryReportGenerator.column1C"));
          xos.endElement("th");
          style = "width: 8%; ";
          xos.startElement("th", "style", style);
            xos.writePCDATA(labels.getString(
    			"SummaryReportGenerator.column2A")); br();
            xos.writePCDATA(labels.getString(
    			"SummaryReportGenerator.column2B")); br();
            xos.writePCDATA(labels.getString(
    			"SummaryReportGenerator.column2C"));
          xos.endElement("th");
          style = "width: 8%; ";
          xos.startElement("th", "style", style);
            xos.writePCDATA(labels.getString(
			"SummaryReportGenerator.column3A")); br();
            xos.writePCDATA(labels.getString(
			"SummaryReportGenerator.column3B")); br();
            xos.writePCDATA(labels.getString(
			"SummaryReportGenerator.column3C"));
          xos.endElement("th");
          style = "width: 8%; ";
          xos.startElement("th", "style", style);
            xos.writePCDATA(labels.getString(
			"SummaryReportGenerator.column4A")); br();
            xos.writePCDATA(labels.getString(
			"SummaryReportGenerator.column4B")); br();
            xos.writePCDATA(labels.getString(
			"SummaryReportGenerator.column4C"));
          xos.endElement("th");
          style = "width: 8%; ";
          xos.startElement("th", "style", style);
            xos.writePCDATA(labels.getString(
			"SummaryReportGenerator.column5A")); br();
            xos.writePCDATA(labels.getString(
			"SummaryReportGenerator.column5B")); br();
            xos.writePCDATA(labels.getString(
			"SummaryReportGenerator.column5C"));
          xos.endElement("th");
          style = "width: 8%; ";
          xos.startElement("th", "style", style);
          	xos.writePCDATA(labels.getString(
			"SummaryReportGenerator.column6A")); br();
            xos.writePCDATA(labels.getString(
			"SummaryReportGenerator.column6B")); br();
            xos.writePCDATA(labels.getString(
			"SummaryReportGenerator.column6C")); br();
            xos.writePCDATA("");
          xos.endElement("th");
        xos.endElement("tr");

        for(Iterator it = lrgsSummaries.iterator(); it.hasNext(); )
        {
          SummaryInfo si = (SummaryInfo)it.next();

          String stat = "OK";
          String tdclass="OK";
          if (System.currentTimeMillis() - si.lastContact > 600000L)
          {
            stat = "No Response";
            tdclass = "ERR";
          }
          else if (!si.isUsable)
          {
            stat = "Not Usable";
            tdclass = "ERR";
          }

          xos.startElement("tr");
            xos.startElement("td", "class", tdclass, "style", "width: 21%; ");
		      String linkTarget = si.host+".html";
		      if (si.extHost != null)
				linkTarget = linkTarget + "?" + si.extHost;
              xos.writeElement("a", "href", linkTarget, si.host);
            xos.endElement("td");

            xos.writeElement("td", "class", tdclass, "style", "width: 14%; ", 
 			  si.statusTime != 0L ? 
				columnDF.format(new Date(si.statusTime*1000L)) : "N/A");
            xos.writeElement("td", "class", tdclass, "style", "width: 10%; ", stat);
            xos.writeElement("td", "class", tdclass, "style", "width: 15%; ", 
				si.primaryDLName + ":" + si.primaryDLStat);
            xos.writeElement("td", "class", tdclass, "style", "width: 8%; ", si.primaryQual);
            xos.writeElement("td", "class", tdclass, "style", "width: 8%; ", si.totalQual);

            xos.writeElement("td", "class", tdclass, "style", "width: 8%; ", "" + si.msgsThisHour);
			
            xos.writeElement("td", "class", tdclass, "style", "width: 8%; ", "" + si.numDDSClients);
            xos.writeElement("td", "class", tdclass, "style", "width: 8%; ", si.lrgsVersion);
          xos.endElement("tr");
        }
      xos.endElement("tbody");
    xos.endElement("table");
  xos.endElement("body");
xos.endElement("html");
	}

	private void br()
		throws IOException
	{
    	xos.writeLiteral("<br>");
	}
	
	private void writeReportHeader(XmlOutputStream xos) throws IOException
	{
		Properties props = System.getProperties();
		props.setProperty("TZ", "UTC");
		String expHeader = EnvExpander.expand(header, props);
		xos.writeLiteral(expHeader);
	}
}

class SummaryInfo
{
	String host;
	long lastContact;
	long statusTime;
	boolean isUsable;
	String primaryDLName;
	String primaryDLStat;
	String primaryQual;
	String totalQual;
	int msgsThisHour;
	int numDDSClients;
//	boolean netbackAvail;
//	int numNetbackClients;
	String lrgsVersion;
	String extHost;

	/// Used to format percentages
	private static DecimalFormat pctFormat = new DecimalFormat("###.##");


	/// Construct new SummaryInfo struct for a single LRGS.
	public SummaryInfo(String host)
	{
		this.host = host;
		lastContact = 0L;
		statusTime = 0L;
		isUsable = false;
		primaryDLStat = "(none)";
		primaryQual = "0%";
		totalQual = "0%";
		msgsThisHour = 0;
		numDDSClients = 0;
//		netbackAvail = false;
//		numNetbackClients = 0;
		extHost = null;
		lrgsVersion = "?";
	}

	/// Updates internal summary variables from LRGS status snapshot.
	public void update(LrgsStatusSnapshotExt status, String extHost)
	{
		this.extHost = extHost;
		lastContact = System.currentTimeMillis();
		statusTime = status.lss.lrgsTime;
		isUsable = status.isUsable;
		getPrimary(status);
		QualityMeasurement qm = status.lss.qualMeas[status.lss.currentHour];
		int p = qm.numGood;
		int t = qm.numGood + qm.numDropped;
		int r = qm.numRecovered;
		primaryQual = pctFormat.format(t == 0 ? 0.0
			: ((double)p / (double)t) * 100.0) + "%";
        int g = qm.numGood + qm.numRecovered;
		totalQual = pctFormat.format(t <= 0 ? 100.0
			: ((double)g / (double)t) * 100.0) + "%";
		msgsThisHour = qm.numGood + qm.numRecovered;

		numDDSClients = 0;
//		numNetbackClients = 0;
		lrgsVersion = "" + status.majorVersion + "." + status.minorVersion;

//		netbackAvail = false;
		for(int i=0; i<status.lss.attProcs.length; i++)
		{
			AttachedProcess ap = status.lss.attProcs[i];
			if (ap == null)
				continue;
			if (TextUtil.startsWithIgnoreCase(ap.type, "DDS-Cli"))
				numDDSClients++;
//			else if (ap.type.startsWith("Net"))
//				numNetbackClients++;
//			else if (ap.type.startsWith("NB-Server"))
//				netbackAvail = true;
		}
	}
	private void getPrimary(LrgsStatusSnapshotExt status)
	{
		int domsatIdx = -1;
		int drgsIdx = -1;
		int lritIdx = -1;
		int noaaportIdx = -1;
		int ddsIdx = -1;
		int netbakIdx = -1;
		for(int i=0; i<status.lss.downLinks.length; i++)
		{
			DownLink dl = status.lss.downLinks[i];
			if (dl == null)
				continue;
			if (dl.type == LrgsInputInterface.DL_DOMSAT)
				domsatIdx = i;
			else if (dl.type == LrgsInputInterface.DL_NETBAK)
				netbakIdx = i;
			else if (dl.type == LrgsInputInterface.DL_DRGS)
				drgsIdx = i;
			else if (dl.type == LrgsInputInterface.DL_LRIT)
				lritIdx = i;
			else if (dl.type == LrgsInputInterface.DL_NOAAPORT)
				noaaportIdx = i;
			else if (dl.type == LrgsInputInterface.DL_DDS)
				ddsIdx = i;
		}
		if (domsatIdx != -1)
		{
			primaryDLName = "DOMSAT";
			primaryDLStat = status.lss.downLinks[domsatIdx].statusString;
		}
		else if (drgsIdx != -1
			&& status.getDownlinkQualityHistory(LrgsInputInterface.DL_DRGSCON)
			   != null)
		{
			primaryDLName = "DRGS";
			primaryDLStat = status.lss.downLinks[drgsIdx].statusString;
		}
		else if (ddsIdx != -1)
		{
			primaryDLName = "DDS";
			primaryDLStat = status.lss.downLinks[ddsIdx].statusString;
		}
		else if (lritIdx != -1)
		{
			primaryDLName = "LRIT";
			primaryDLStat = status.lss.downLinks[lritIdx].statusString;
		}
		else if (noaaportIdx != -1)
		{
			primaryDLName = "NOAAPORT";
			primaryDLStat = status.lss.downLinks[noaaportIdx].statusString;
		}
		else if (netbakIdx != -1)
		{
			primaryDLName = "NETBACK";
			primaryDLStat = status.lss.downLinks[netbakIdx].statusString;
		}
		else
		{
			primaryDLName = "(none)";
			primaryDLStat = "";
		}
	}
}
