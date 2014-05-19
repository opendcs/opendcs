/*
*  $Id$
*/
package lrgs.lrgsmon;

//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
//import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
//import java.util.ResourceBundle;
import java.util.TimeZone;
//import java.util.Properties;
//
//import ilex.util.EnvExpander;
//import ilex.util.Logger;
import ilex.util.StringPair;
import ilex.xml.XmlOutputStream;

//import lrgs.rtstat.RtStat;
import lrgs.statusxml.LrgsStatusSnapshotExt;
//import lrgs.apistatus.AttachedProcess;
//import lrgs.apistatus.QualityMeasurement;
//import lrgs.lrgsmain.LrgsInputInterface;
import lrgs.networkdcp.NetworkDcpStatus;

/**
This class writes the detail report for a single LRGS.
*/
public class NetworkDcpReportGenerator
{
//	private static ResourceBundle labels = 
//		RtStat.getLabels();
//	private static ResourceBundle genericLabels = 
//		RtStat.getGenericLabels();
	/** Used to format dates in the HTML report header. */
	private SimpleDateFormat headerDF;

	/** Used to format dates in columns in the HTML report. */
	private SimpleDateFormat columnDF;

//	/** Local copy of XML output stream */
//	private XmlOutputStream xos;

//	/** Used to format percentages */
//	private DecimalFormat pctFormat = new DecimalFormat("###.##");
//
//	/** Name of image file to place in output. */
//	private String imageFile;

	// convenient constants used within HTML:
	private static final String top = "vertical-align: top; ";
//	private static final String right  = "text-align: right; ";
//	private static final String left   = "text-align: left; ";
	private static final String center = "text-align: center; ";
	private static final String bold   = "font-weight: bold; ";
	private static final String ital   = "font-style: italic; ";

//	private static final String redStatus
//		= "color: red; font-weight: bold; background-color: black; ";
//	private static final String yellowStatus
//		= "color: yellow; font-weight: bold; background-color: black; ";
	public boolean showNetworkDcpDetail = false;

	/** Constructs a new DetailReportGenerator */
	public NetworkDcpReportGenerator(String imageFile)
	{
		headerDF = new SimpleDateFormat(
			"MMMM dd, yyyy  HH:mm:ss '(Day ' DDD')'");
		headerDF.setTimeZone(TimeZone.getTimeZone("UTC"));
		columnDF = new SimpleDateFormat("MM/dd HH:mm:ss");
		columnDF.setTimeZone(TimeZone.getTimeZone("UTC"));
//		this.imageFile = imageFile;
	}

	public void writeReport(XmlOutputStream xos, String host,
	    LrgsStatusSnapshotExt status)
	    throws IOException
	{
		// Define the styles to be used in the output:

		// Reverse Video OK & Error values
//		String revOK = "color: rgb(51,255,51); font-weight: bold; background-color: black; ";
//		String revERR = "color: rgb(255,255,51); font-weight: bold; background-color: black; ";
//
//		String thStyle = top + center + bold + ital;
//
//		String thCol0Style = "vertical-align: top; text-align: right; "
//		    + "font-weight: bold; font-style: italic;";
//		this.xos = xos;

		// HTML Header
		xos.startElement("head");
		xos.writeElement("title", host + " Network DCP Status");
		xos.endElement("head");

		xos.startElement("body", "style", "background-color: white;");

		ArrayList<NetworkDcpStatus> statusList = status.networkDcpStatusList
		    .getStatusList();

		StringPair sp4[] = new StringPair[4];
		sp4[0] = new StringPair("cellpadding", "2");
		sp4[1] = new StringPair("cellspacing", "2");
		sp4[2] = new StringPair("border", "1");
		sp4[3] = new StringPair("style", "text-align: left; width: 100%;");
		xos.startElement("table", sp4);

		xos.startElement("tr");
		xos.startElement("td", "colspan", "8");
		xos.writeElement("h3", "style", center, "Network DCP Status");
		xos.endElement("td");
		xos.endElement("tr");

		xos.startElement("tr");
		String labelStyle = center;
		xos.writeElement("td", "style", labelStyle, "Name");
		xos.writeElement("td", "style", labelStyle, "Host:Port");
		xos.writeElement("td", "style", labelStyle, "Poll Min");
		xos.writeElement("td", "style", labelStyle, "Last Attempt");
		xos.writeElement("td", "style", labelStyle, "Last Contact");
		xos.writeElement("td", "style", labelStyle, "Good Polls");
		xos.writeElement("td", "style", labelStyle, "Failed Polls");
		xos.writeElement("td", "style", labelStyle, "Messages");
		xos.endElement("tr");
		for (NetworkDcpStatus nds : statusList)
		{
			String dispName = nds.getDisplayName();
			String dcphost = nds.getHost();
			int port = nds.getPort();
			int pollMinutes = nds.getPollingMinutes();
			Date lastAttempt = nds.getLastPollAttempt();
			Date lastContact = nds.getLastContact();
			long numGood = nds.getNumGoodPolls();
			long numMsgs = nds.getNumMessages();
			long numFailedPolls = nds.getNumFailedPolls();
			xos.startElement("tr");
			xos.writeElement("td", "style", labelStyle, dispName);
			xos.writeElement("td", "style", labelStyle, dcphost + ":" + port);
			xos.writeElement("td", "style", labelStyle, "" + pollMinutes);
			xos.writeElement("td", "style", labelStyle,
			    lastAttempt == null ? "never" : columnDF.format(lastAttempt));
			xos.writeElement("td", "style", labelStyle,
			    lastContact == null ? "never" : columnDF.format(lastContact));
			xos.writeElement("td", "style", labelStyle, "" + numGood);
			xos.writeElement("td", "style", labelStyle, "" + numFailedPolls);
			xos.writeElement("td", "style", labelStyle, "" + numMsgs);
			xos.endElement("tr");
		}
		xos.endElement("table");
		xos.endElement("body");
		xos.endElement("html");
	}

	private void br(XmlOutputStream xos)
		throws IOException
	{
		xos.writeLiteral("<br>");
    	// xos.writeElement("br", null);
	}
}

