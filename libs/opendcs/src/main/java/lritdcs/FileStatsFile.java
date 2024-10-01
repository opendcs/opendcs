/*
*  $Id$
*
*  This is open-source software under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between the contractor and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*
*  $Log$
*  Revision 1.1  2012/12/12 16:01:31  mmaloney
*  Several updates for 5.2
*
*/
package lritdcs;

import java.io.LineNumberReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import lrgs.lrgsmain.LrgsInputInterface;
import lrgs.statusxml.LrgsStatusSnapshotExt;

/**
This class contains methods for reading & writing the quality log file.
*/
public class FileStatsFile
{
	private static final String module = "FileStats";
	private File file;
	private SimpleDateFormat sdf;
	private PrintStream output;
	private static final int maxLength = 80*1440*10; // about 10 day's worth.

	public static final String LogFileName = "filestats.log";

	/**
	 * Constructor.
	 * @param filename the file name
	 */
	public FileStatsFile()
	{
		file = new File(EnvExpander.expand(LogFileName));
		sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		output = null;
	}

	/** @return last modify time of the quality log file. */
	public long lastModified() { return file.lastModified(); }

	private void openLog(boolean append)
	{
		try 
		{
			output = new PrintStream(new FileOutputStream(file, append), true);
			if (file.length() == 0L)
			{
				output.println("#priority,filename,numMsgs,earliestCarrierEnd,"
					+ "earliestLrgsRcvd,latestCarrierEnd,latestLrgsRecvd,"
					+ "latestLritRcvd,"
					+ "fileCreated,dom2AXfer,dom2ARename,"
					+ "dom2BXfer,dom2BRename,dom2CXfer,dom2CRename,"
					+ "xfersComplete,fileLatency,maxMsgLatency");
				output.println("# earliest/latest LrgsRcvd = time that messages was "
					+ "received by the LRGS (e.g. CDADATA) from the DAMS-NT.");
				output.println("# earliest/latest carrier end time stamp comes from "
					+ "DAMS-NT. LrgsRcvd should be very soon after carrier end.");
				output.println("# latestLritRcvd is the time that the LRITDCS"
					+ " application received the file from LRGS. It should be very "
					+ "soon after latestLrgsRcvd.");
				output.println("# fileCreated is the time the LRIT file was finished.");
				output.println("# dom2ARename is the time the file was completely "
					+ "transferred and renamed on the first Domain 2.");
				output.println("# xfersComplete is the time the file is transferred "
					+ "and renamed on all DOMAIN 2s");
				output.println("# fileLatency is (dom2ARename - fileCreated) i.e. "
					+ "the amount of time it took to transfer after finishing the file.");
				output.println("# maxMsgLatency is (dom2ARename - earliestCarrierEnd ");
			}
		}
		catch(FileNotFoundException ex)
		{
			Logger.instance().warning(module + " Cannot open filestats log '"
				+ file.getPath() + "': " + ex);
			output = null;
		}
	}

	/**
	 * Silently close the log file, if one is open.
	 */
	public void close()
	{
		try 
		{
			if (output != null) 
				output.close(); 
			output = null;
		}
		catch(Exception ex) {}
	}

	/**
	 * Appends an entry to the file.
	 * @param qle the entry.
	 */
	public void append(LritDcsFileStats qle)
	{
		if (output == null)
			openLog(true);
		else if (file.length() > maxLength)
		{
			close();
			file.renameTo(new File(file.getPath() + ".old"));
			openLog(false);
		}
		if (output == null)
			return;
		output.println(stats2String(qle));
	}


	private String stats2String(LritDcsFileStats qle)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(qle.getPriority() + ",");
		sb.append(qle.getFile().getName() + ",");
		sb.append("" + qle.getNumMessages() + ",");
		sb.append(formatTime(qle.getEarliestCarrierEndTime()) + ",");
		sb.append(formatTime(qle.getEarliestLocalRcvTime()) + ",");
		sb.append(formatTime(qle.getLatestCarrierEndTime()) + ",");
		sb.append(formatTime(qle.getLatestLocalRcvTime()) + ",");
		sb.append(formatTime(qle.getLatestAppRcvTime()) + ",");
		sb.append(formatTime(qle.getFileSaveTime()) + ",");
		sb.append(formatTime(qle.getDom2AXferCompleteTime()) + ",");
		sb.append(formatTime(qle.getDom2ARenameCompleteTime()) + ",");
		sb.append(formatTime(qle.getDom2BXferCompleteTime()) + ",");
		sb.append(formatTime(qle.getDom2BRenameCompleteTime()) + ",");
		sb.append(formatTime(qle.getDom2CXferCompleteTime()) + ",");
		sb.append(formatTime(qle.getDom2CRenameCompleteTime()) + ",");
		sb.append(formatTime(qle.getAllTransfersCompleteTime()) + ",");
		
		long d2complete = 
			qle.getDom2ARenameCompleteTime() != null 
				? qle.getDom2ARenameCompleteTime().getTime()
				: qle.getDom2BRenameCompleteTime() != null
				? qle.getDom2BRenameCompleteTime().getTime()
				: qle.getDom2CRenameCompleteTime() != null
				? qle.getDom2CRenameCompleteTime().getTime()
				: System.currentTimeMillis();

		// File latency (time from file creation to d2 transfer complete.
		sb.append(""
			+ ((double)(d2complete - qle.getFileSaveTime().getTime()) / 1000.0)
			+ ",");
		
		// Max msg latency (time from earliest msg carrier end to d2 transfer complete
		sb.append(""
			+ ((double)(d2complete - qle.getEarliestCarrierEndTime().getTime()) / 1000.0));
		return sb.toString();
	}
	
	private String formatTime(Date d)
	{
		if (d == null)
			return "N/A";
		else
			return sdf.format(d);
	}
}
