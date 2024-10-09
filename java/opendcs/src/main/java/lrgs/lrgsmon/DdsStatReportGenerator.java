/*
*
*/
package lrgs.lrgsmon;

import ilex.util.Logger;
import ilex.util.StringPair;
import ilex.xml.XmlOutputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import lrgs.db.DdsConnectionStats;
import lrgs.db.DdsPeriodStats;
import lrgs.db.LrgsConstants;

/**
This class writes the detail report for LRGS User Utilization Reports.
*/
public class DdsStatReportGenerator
{
	/** Used to format dates in columns in the HTML report. */
	private SimpleDateFormat columnHourDF;

	/** Used to format dates in Hour Start column of DDS Connection 
	  	HTML report. */
	private SimpleDateFormat hourStartDF;
	
	/** Used to format dates for the dds connection file name. */
	private SimpleDateFormat fileDF;
	
	/** Used to format avg */
	private static DecimalFormat avgFormat = new DecimalFormat("###.00");
	
	/** convenient constants used within HTML: */
	private static final String top = "vertical-align: top; ";
	private static final String right  = "text-align: right; ";
	private static final String left   = "text-align: left; ";
	private static final String center = "text-align: center; ";
	private static final String bold   = "font-weight: bold; ";
	private static final String ital   = "font-style: italic; ";
	private static final String verticalCenter = "vertical-align: center; ";

	/**
	 * Constructs a new DdsStatReportGenerator. 
	 * Initialize all Date formats used when generating the HTML report.
	 * 
	 * @param timeZone the timezone to use when formatting dates
	 */
	public DdsStatReportGenerator(String timeZone)
	{
		TimeZone tz = TimeZone.getTimeZone(timeZone);
		hourStartDF = new SimpleDateFormat("MM/dd/yyyy-HH:mm");
		hourStartDF.setTimeZone(tz);
		
		columnHourDF = new SimpleDateFormat("MM/dd-HH:mm:ss");
		columnHourDF.setTimeZone(tz);

		fileDF = new SimpleDateFormat("yyyyMMddHH");
		fileDF.setTimeZone(tz);		
	}
	
	/**
	 * This method generates the DDS Connection Statistics Report.
	 * It will loop from the given start time to end time to create
	 * the main hourly usage summary report plus individual html files
	 * for each hour between start time and end time.
	 * 
	 * @param startTime the user given start time
	 * @param endTime the user given end time
	 * @param hourlyFileName the file name to be used when creating html file
	 * @param outputDir the directory on the file system where all html files
	 * will be placed
	 * @param ddsConnectionList the list containing dds connections from
	 * start time to end time
	 * @param ddsPeriodStatsList the list containing dds period stat info from
	 * start time to end time
	 * @param lrgsName the current lrgs in used
	 */
	public void generateDdsStatReport(Date startTime, Date endTime, 
									String hourlyFileName, File outputDir,
									List<DdsConnectionStats> ddsConnectionList,
									List<DdsPeriodStats> ddsPeriodStatsList,
									String lrgsName)
	{		
		// Create the file that will hold the DDS Connection Statistics
		FileOutputStream mainFos = null;
		File mainHourlyFile = null;
		try
		{	// This is the main file that will be created for all the hours
			mainHourlyFile = new File(outputDir.getPath() +
					File.separator + hourlyFileName + ".html");
			//Logger.instance().info("Main Hourly File Name = "+
			//										mainHourlyFile.toString());
			mainFos = new FileOutputStream(mainHourlyFile);
			XmlOutputStream mainXos = 
							new XmlOutputStream(mainFos, "html");
			createDDSPeriodStatisticTableHeader(mainXos, startTime, endTime,
												lrgsName);
		
			long startT = startTime.getTime();
			long endT = endTime.getTime();
			HashSet<String> distinctUserHash = new HashSet<String>();
			// Loop from startTime to endTime, one hour at a time
			for (long hour = startT; hour <= endT; hour+=3600000L)
			{
				// For every hour, get # of distinct User, # of Admins done
				distinctUserHash.clear();
				int adminsDone = 0;
				// Loop dds connection array to get all connections 
				//of a single hr
				Iterator connectionsIterator = ddsConnectionList.iterator();
			
				if (!connectionsIterator.hasNext())
				{
					Logger.instance().info("No records found for DDS " +
											"Connection table.");
				}
				FileOutputStream fos = null;
				String hrHtmlFileName = "";
				File tmp = null;
				Date currentHourlyTime = new Date(hour);
				try
				{	// This is the file that will be created per hour
					hrHtmlFileName = hourlyFileName + //"DDSConnections"
						fileDF.format(currentHourlyTime) + ".html"; 
					tmp = new File(outputDir.getPath() +
							File.separator + hrHtmlFileName);
					//Logger.instance().info("Per hour file= "+tmp.toString());
					fos = new FileOutputStream(tmp);
					XmlOutputStream xos = 
									new XmlOutputStream(fos, "html");
					// Create table header with ID, Start, End, From, User,
					// Disposition, #Msgs, Admin?
					createDDSConnectionTableHeader(xos, currentHourlyTime,
																lrgsName);
					while (connectionsIterator.hasNext())
					{
						DdsConnectionStats 
						connectionStats = 
								(DdsConnectionStats)connectionsIterator.next();
						if (connectionStats != null)
						{
							// Verify that this record belongs to the 
							// current hour
							Date tempStart = connectionStats.getStartTime();
							Date tempEnd = connectionStats.getEndTime();
							// if endTime is null means still active
							if (tempStart != null)
							{
								if ((tempStart.getTime() < hour+3600000L) &&
									(tempEnd == null || 
										(tempEnd.getTime() >= hour)))
								{
									// Create/add the html row to the output 
									//file.
									createHourlyDataRow(xos,
											connectionStats.getConnectionId(),
											tempStart,
											tempEnd,
											connectionStats.getFromIpAddr(),
											connectionStats.getSuccessCode(),
											connectionStats.getUserName(),
											connectionStats.getMsgsReceived(),
											connectionStats.isAdmin_done());
									//Calculate # of Admin done
									if (connectionStats.isAdmin_done())
										adminsDone++;
									//Calculate # of distinct users
									distinctUserHash.add(
												connectionStats.getUserName());
								}	
							}
						}
					} // End while connectionsIterator.hasNext()
					// Close the table for this html hour file
					xos.endElement("table");
					xos.endElement("body");
					xos.endElement("html");
					fos.close();
				}
				catch(IOException ex)
				{
					Logger.instance().warning("Cannot write " +
										tmp.toString() + ": " + ex);
				}
				finally
				{
					if (fos != null)
					{
						try { fos.close(); }
						catch(IOException ex){}
					}
				}
	
				// Loop dds period stats array to extract data for a single 
				// hour
				Iterator periodStatIterator = ddsPeriodStatsList.iterator();
				
				if (!periodStatIterator.hasNext())
				{
					Logger.instance().info("No records found for DDS " +
											"Period Stats table.");
				}
				while (periodStatIterator.hasNext())
				{
					DdsPeriodStats periodStats = 
									(DdsPeriodStats)periodStatIterator.next();
					if (periodStats != null)
					{
						// Verify that this record belongs to the 
						// current hour   1 hour = 3600000L milliseconds
						//This is in case the startTime is not in the 0 minutes
						if (periodStats.getStartTime() != null)
						{
							long pedStartT = 
										periodStats.getStartTime().getTime();
							pedStartT = (pedStartT/3600000L) * 3600000L;
							if (hour == pedStartT)
							{
								// Create/Add a row to Hourly DDS Connection
								createMainHourlyDataRow(mainXos,hrHtmlFileName,
										periodStats.getStartTime(),
										periodStats.getPeriodDuration(),
										periodStats.getNumAuth(),
										periodStats.getNumUnAuth(),
										periodStats.getBadPasswords(),
										periodStats.getBadUsernames(),
										periodStats.getMaxClients(),
										periodStats.getMinClients(),
										periodStats.getAveClients(),
										periodStats.getMsgsDelivered(),
										distinctUserHash.size(), adminsDone);
								break; // Get out of this inner loop.
							}	
						}
					}
				} // End while periodStatIterator.hasNext()
				
			} // End for loop hour
			// Close the Hourly DDS Connection Statistics table (main file)
			mainXos.endElement("table");
			mainXos.endElement("body");
			mainXos.endElement("html");
			mainFos.close();
		}
		catch(IOException ex)
		{
			Logger.instance().warning("Cannot write " + 
					mainHourlyFile.toString() + ": " + ex);
		}
		finally
		{
			if (mainFos != null)
			{
				try { mainFos.close(); }
				catch(IOException ex){}
			}
		}
	}
	
	/**
	 * This method generates the table header for the main html hourly
	 * report file.
	 * 
	 * @param mainXos XmlOutputStream obj used to generate the html output
	 * in XMl format
	 * @param startTime the initial time given by the user
	 * @param endTime the end time given by the user
	 * @param lrgsName the current lrgs in used
	 * @throws IOException thrown if fails to create html
	 */
	private void createDDSPeriodStatisticTableHeader(XmlOutputStream mainXos,
								Date startTime, Date endTime, String lrgsName) 
															throws IOException
	{
		String thStyle = verticalCenter + center + bold + ital;
		mainXos.startElement("html");
		// HTML Header
		mainXos.startElement("head");
		mainXos.writeElement("title", 
								"LRGS User Utilization Report");
		mainXos.endElement("head");
		mainXos.startElement("body", "style", "background-color: white;");
		// Create the Hourly DDS Connection Statistics header
		mainXos.writeLiteral(setDDSPeriodStatisticHeader(startTime,endTime,
																	lrgsName));
		// Create the table header 
		// Hour Start, Connects, Failures, Users, Msgs, Admins, 
		// Under Concurrent Connections - Min, Max, Avg
		StringPair sp4[] = new StringPair[4];
		sp4[0] = new StringPair("cellpadding", "2");
		sp4[1] = new StringPair("cellspacing", "2");
		sp4[2] = new StringPair("border", "1");
		sp4[3] = new StringPair("style", "text-align: left; width: 100%;");
		mainXos.startElement("table", sp4);
	    
		mainXos.startElement("tr");
		mainXos.writeElement("th", "style", thStyle, "Hour Start");
		mainXos.writeElement("th", "style", thStyle, "Connects");
		mainXos.writeElement("th", "style", thStyle, "Failures");
		mainXos.writeElement("th", "style", thStyle, "Users");
		mainXos.writeElement("th", "style", thStyle, "Msgs");
		mainXos.writeElement("th", "style", thStyle, "Admins");
		mainXos.startElement("th", "style", thStyle,"colspan","3");
			mainXos.startElement("table", sp4);
			mainXos.startElement("tr");
				mainXos.writeElement("th", "style", thStyle,"colspan","3",
												"Concurrent Connections");
			mainXos.endElement("tr");
			mainXos.startElement("tr");
				mainXos.writeElement("th", "style", thStyle, "Min");
				mainXos.writeElement("th", "style", thStyle, "Max");
				mainXos.writeElement("th", "style", thStyle, "Avg");
				mainXos.endElement("tr");
				mainXos.endElement("table");
		mainXos.endElement("th");		    
		mainXos.endElement("tr");
	}

	/**
	 * This method creates the HTML header for the main hourly
	 * statistics report.
	 * 
	 * @param startTime begin time for the dds connections
	 * @param endTime end time for the dds connections
	 * @param lrgsName the name of the current lrgs in used
	 * @return String html header for main hourly report
	 */
	private String setDDSPeriodStatisticHeader(Date startTime, Date endTime,
												String lrgsName)
	{
		// HTML header to include in report Main hourly report file
		String header = 
		"<h2 style=\"" + center + "\">LRGS: " + lrgsName + "</h2>\n"
		+ "<h2 style=\"" + center + "\">Hourly DDS Connection Statistics"
		+ " (" + hourStartDF.format(startTime) + " - "
		+ hourStartDF.format(endTime) + ")"
		+ "</h2>\n"
		+ "<br>\n";
		
		return header;
	}
	
	/**
	 * This method generates the table header for the html hourly
	 * report file.
	 * 
	 * @param xos XmlOutputStream obj used to generate the html output
	 * in XMl format
	 * @param currentTime is the current hour in process
	 * @param lrgsName the current lrgs in used
	 * @throws IOException thrown if fails to create html
	 */
	private void createDDSConnectionTableHeader(XmlOutputStream xos,
											Date currentTime, String lrgsName) 
															throws IOException
	{
		String thStyle = verticalCenter + center + bold + ital;
		xos.startElement("html");
		// HTML Header
		xos.startElement("head");
		xos.writeElement("title", 
								"LRGS User Hourly Utilization Report");
		xos.endElement("head");
		xos.startElement("body", "style", "background-color: white;");
		// Create the Hourly DDS Connection header
		xos.writeLiteral(setDDSConnectionHeader(currentTime, lrgsName));
		// Create the table header 
		// ID, Start, End, From, User, Disposition, #Msgs, Admin?
		StringPair sp4[] = new StringPair[4];
		sp4[0] = new StringPair("cellpadding", "2");
		sp4[1] = new StringPair("cellspacing", "2");
		sp4[2] = new StringPair("border", "1");
		sp4[3] = new StringPair("style", "text-align: left; width: 100%;");
		xos.startElement("table", sp4);
	    
		xos.startElement("tr");
		
		xos.writeElement("th", "style", thStyle, "ID");
		xos.writeElement("th", "style", thStyle, "Start");
		xos.writeElement("th", "style", thStyle, "End");
		xos.writeElement("th", "style", thStyle, "From");
		xos.writeElement("th", "style", thStyle, "User");
		xos.writeElement("th", "style", thStyle, "Disposition");
		xos.writeElement("th", "style", thStyle, "# Msgs");
		xos.writeElement("th", "style", thStyle, "Admin?");
				    
		xos.endElement("tr");
	}
	
	/**
	 * This method creates the HTML header for the hourly
	 * dds connection report.
	 * 
	 * @param currentTime current hour in process
	 * @param lrgsName current lrgs in used
	 * @return String html header for hourly report
	 */
	private String setDDSConnectionHeader(Date currentTime, String lrgsName)
	{
		// HTML header to include in hourly report file
		String header = 
		"<h2 style=\"" + center + "\">LRGS: " + lrgsName + "</h2>\n"
		+ "<h2 style=\"" + center + "\">DDS Usage Connection Report for: "
		+ hourStartDF.format(currentTime)
		+ "</h2>\n"
		+ "<br>\n";
		
		return header;
	}
	
	/**
	 * This method generates an HTML row of data for the
	 * hourly report file. This information is coming from
	 * the LRGS DB dds_connection table.
	 * 
	 * @param xos XmlOutputStream obj used to generate the html output
	 * in XMl format
	 * @param connectionId value from Lrgs DB dds_connection table
	 * @param startTime value from Lrgs DB dds_connection table
	 * @param endTime value from Lrgs DB dds_connection table
	 * @param fromIpAddr value from Lrgs DB dds_connection table
	 * @param successCode value from Lrgs DB dds_connection table
	 * @param userName value from Lrgs DB dds_connection table
	 * @param msgsReceived value from Lrgs DB dds_connection table
	 * @param adminDone value from Lrgs DB dds_connection table
	 * @throws IOException thrown if fails to create html
	 */
	private void createHourlyDataRow(XmlOutputStream xos,
									int connectionId,
									Date startTime,
									Date endTime,
									String fromIpAddr,
									char successCode,
									String userName,
									int msgsReceived,
									boolean adminDone) throws IOException
	{
		xos.startElement("tr");
		xos.writeElement("td", "style", center, "" + connectionId);
		xos.writeElement("td", "style", center,columnHourDF.format(startTime));
		if (endTime != null)
			xos.writeElement("td", "style", center,
												columnHourDF.format(endTime));
		else
			xos.writeElement("td", "style", center,"-");
		xos.writeElement("td", "style", center, fromIpAddr);
		xos.writeElement("td", "style", center, userName);
		xos.writeElement("td","style", center,
				LrgsConstants.successCodeName(successCode));  
		xos.writeElement("td", "style", center, "" + msgsReceived);
		xos.writeElement("td", "style", center, adminDone ? "Y" : "N");
		xos.endElement("tr");
	}

	/**
	 * This method generates the main html page report. It creates the 
	 * table with the information from the dds_period_stats table of
	 * the LRGS Database. 
	 * 
	 * @param mainXos XmlOutputStream obj used to generate the html output
	 * in XMl format
	 * @param linkToHourlyFile contains the hyper link to hourly html file 
	 * @param StartTime value from Lrgs DB dds_period_stats table 
	 * @param periodDuration value from Lrgs DB dds_period_stats table
	 * @param numAuth value from Lrgs DB dds_period_stats table
	 * @param numUnAuth value from Lrgs DB dds_period_stats table
	 * @param badPasswords value from Lrgs DB dds_period_stats table
	 * @param badUsernames value from Lrgs DB dds_period_stats table
	 * @param maxClients value from Lrgs DB dds_period_stats table
	 * @param minClients value from Lrgs DB dds_period_stats table
	 * @param aveClients value from Lrgs DB dds_period_stats table
	 * @param msgsDelivered value from Lrgs DB dds_period_stats table
	 * @param distinctUsers found within the current hour
	 * @param adminsDone # of admin done within the current hour 
	 * @throws IOException thrown if fails to create html
	 */
	private void createMainHourlyDataRow(XmlOutputStream mainXos,
						String linkToHourlyFile, Date startTime,
						char periodDuration, int numAuth, int numUnAuth,
						int badPasswords, int badUsernames,	int maxClients,
						int minClients,	double aveClients, int msgsDelivered,
						int distinctUsers, int adminsDone) throws IOException
	{
		mainXos.startElement("tr");
		mainXos.startElement("td", "style", center);
		// Create a hyper-link that will be opened on a new window
		mainXos.writeElement("a", "href", linkToHourlyFile, 
							"target", "_blank", 
							hourStartDF.format(startTime));
		mainXos.endElement("td");
		mainXos.writeElement("td", "style", center, "" + (numAuth+numUnAuth));
		mainXos.writeElement("td", "style", center, "" + 
												(badPasswords+badUsernames));
		mainXos.writeElement("td", "style", center, "" + distinctUsers);
		mainXos.writeElement("td", "style", center, "" + msgsDelivered);
		mainXos.writeElement("td", "style", center, "" + adminsDone);

		mainXos.startElement("td", "style", center,"colspan","3");
		StringPair sp4[] = new StringPair[5];
		sp4[0] = new StringPair("cellpadding", "2");
		sp4[1] = new StringPair("cellspacing", "2");
		sp4[2] = new StringPair("border", "1");
		sp4[3] = new StringPair("style", "text-align: left; width: 100%;");
		sp4[4] = new StringPair("frame", "vsides");

		mainXos.startElement("table", sp4);
		mainXos.startElement("tr");
			mainXos.writeElement("td", "style", center, "width","33%",
															"" + minClients);
			mainXos.writeElement("td", "style", center, "width","33%",
															"" + maxClients);
			if (aveClients == 0)
				mainXos.writeElement("td", "style", center, "width","33%",
																	"0.00");
			else if (aveClients > 0 && aveClients < 1)
				mainXos.writeElement("td", "style", center,"width","33%", 
										"0" + avgFormat.format(aveClients));
			else
				mainXos.writeElement("td", "style", center,"width","33%", 
												avgFormat.format(aveClients));
			mainXos.endElement("tr");
			mainXos.endElement("table");
		mainXos.endElement("td");

		mainXos.endElement("tr");
	}	
}
