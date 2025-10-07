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
package lrgs.archive;

import java.io.LineNumberReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.text.ParseException;

import ilex.util.EnvExpander;
import lrgs.lrgsmain.LrgsInputInterface;
import lrgs.statusxml.LrgsStatusSnapshotExt;

/**
This class contains methods for reading & writing the quality log file.
*/
public class QualLogFile
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private static final String module = "QualLog";
	private File file;
	private SimpleDateFormat sdf;
	private PrintStream output;
	private static final int maxLength = 80*1440*10; // about 10 day's worth.

	public static final String LogFileName = "quality.log";

	/**
	 * Constructor.
	 * @param filename the file name
	 */
	public QualLogFile(String filename)
	{
		file = new File(EnvExpander.expand(filename));
		sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm");
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
				output.println("# date/time domsatGood domsatPar aveBER maxBER"
					+ " drgsGood drgsPar ddsGood ddsPar noaaportGood "
					+ "noaaportPar lritGood lritErr nbGood nbPar "
					+ "arcGood arcPar domsatDropped gr3110 iridium edl");
			}
		}
		catch(FileNotFoundException ex)
		{
			log.atWarn().setCause(ex).log("Cannot open quality log '{}'", file.getPath());
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
	public void append(QualLogEntry qle)
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
		output.println(qle2String(qle));
	}

	/**
	 * Reads back 24 hours to initialize the quality entries in the
	 * status structure.
	 * @param lsse the status snapshot.
	 */
	public void initQualityStatus(LrgsStatusSnapshotExt lsse)
	{
		int drgsIdx = -1;
		int domsatIdx = -1;
		int ddsIdx = -1;
		int noaaportIdx = -1;
		int lritIdx = -1;
		int netbackIdx = -1;
		int gr3110Idx = -1;
		int iridiumIdx = -1;
		int edlIdx = -1;

		for(int i=0; i < lsse.lss.downLinks.length; i++)
		{
			if (lsse.lss.downLinks[i].type == LrgsInputInterface.DL_DRGS)
				drgsIdx = i;
			else if (lsse.lss.downLinks[i].type == LrgsInputInterface.DL_DOMSAT)
				domsatIdx = i;
			else if (lsse.lss.downLinks[i].type == LrgsInputInterface.DL_NETBAK)
				netbackIdx = i;
			else if (lsse.lss.downLinks[i].type == LrgsInputInterface.DL_DDS)
				ddsIdx = i;
			else if (lsse.lss.downLinks[i].type==LrgsInputInterface.DL_NOAAPORT)
				noaaportIdx = i;
			else if (lsse.lss.downLinks[i].type == LrgsInputInterface.DL_LRIT)
				lritIdx = i;
			else if (lsse.lss.downLinks[i].type == LrgsInputInterface.DL_GR3110)
				gr3110Idx = i;
			else if (lsse.lss.downLinks[i].type == LrgsInputInterface.DL_IRIDIUM)
				iridiumIdx = i;
			else if (lsse.lss.downLinks[i].type == LrgsInputInterface.DL_EDL)
				edlIdx = i;
		}

		log.info("Reading quality log, domsatIdx={}, ddsIdx={}, drgsIdx={}, edlIdx={}",
				 domsatIdx, ddsIdx, drgsIdx, edlIdx);

		// We only are interested in last 24 hours.
		long cutoff = (System.currentTimeMillis() / 3600000L) * 3600000L;
		cutoff -= (3600000L * 23L);

		LineNumberReader lnr = null;
		try
		{
			lnr = new LineNumberReader(new FileReader(file));
			String line;
			while((line = lnr.readLine()) != null)
			{
				line = line.trim();
				if (line.startsWith("#"))
					continue;
				QualLogEntry qle = parse(line, file, lnr.getLineNumber());
				if (qle == null)
					continue;
				long t = qle.timeStamp.getTime();
				if (t < cutoff)
					continue;
				int h = (int)(t / 3600000L) % 24;

				lsse.lss.qualMeas[h].containsData = true;
				lsse.lss.qualMeas[h].numGood += qle.archivedGood;
				lsse.lss.qualMeas[h].numDropped += qle.archivedErr;

				if (drgsIdx != -1)
				{
					lsse.downlinkQMs[drgsIdx].dl_qual[h].containsData = true;
					lsse.downlinkQMs[drgsIdx].dl_qual[h].numGood
						+= qle.drgsGood;
					lsse.downlinkQMs[drgsIdx].dl_qual[h].numDropped
						+= qle.drgsErr;
					if (qle.drgsGood > 0)
						lsse.lss.downLinks[drgsIdx].lastMsgRecvTime
							= (int)(t / 1000L);
				}
				if (domsatIdx != -1)
				{
					lsse.downlinkQMs[domsatIdx].dl_qual[h].containsData = true;
					lsse.downlinkQMs[domsatIdx].dl_qual[h].numGood +=
						qle.domsatGood;
					lsse.downlinkQMs[domsatIdx].dl_qual[h].numDropped +=
						qle.domsatErr;
					lsse.domsatDropped[h] += qle.domsatDropped;
					if (qle.domsatGood > 0)
						lsse.lss.downLinks[domsatIdx].lastMsgRecvTime
							= (int)(t / 1000L);
				}
				if (ddsIdx != -1)
				{
					lsse.downlinkQMs[ddsIdx].dl_qual[h].containsData = true;
					lsse.downlinkQMs[ddsIdx].dl_qual[h].numGood += qle.ddsGood;
					lsse.downlinkQMs[ddsIdx].dl_qual[h].numDropped+=qle.ddsErr;
					if (qle.ddsGood > 0)
						lsse.lss.downLinks[ddsIdx].lastMsgRecvTime
							= (int)(t / 1000L);
				}

				if (lritIdx != -1)
				{
					lsse.downlinkQMs[lritIdx].dl_qual[h].containsData = true;
					lsse.downlinkQMs[lritIdx].dl_qual[h].numGood +=
						qle.lritGood;
					lsse.downlinkQMs[lritIdx].dl_qual[h].numDropped +=
						qle.lritErr;
					if (qle.lritGood > 0)
						lsse.lss.downLinks[lritIdx].lastMsgRecvTime
							= (int)(t / 1000L);
				}
				if (noaaportIdx != -1)
				{
					lsse.downlinkQMs[noaaportIdx].dl_qual[h].containsData=true;
					lsse.downlinkQMs[noaaportIdx].dl_qual[h].numGood +=
						qle.noaaportGood;
					lsse.downlinkQMs[noaaportIdx].dl_qual[h].numDropped +=
						qle.noaaportErr;
					if (qle.noaaportGood > 0)
						lsse.lss.downLinks[noaaportIdx].lastMsgRecvTime
							= (int)(t / 1000L);
				}
				if (netbackIdx != -1)
				{
					lsse.downlinkQMs[netbackIdx].dl_qual[h].containsData = true;
					lsse.downlinkQMs[netbackIdx].dl_qual[h].numGood +=
						qle.netbackGood;
					lsse.downlinkQMs[netbackIdx].dl_qual[h].numDropped +=
						qle.netbackErr;
				}

				if (gr3110Idx != -1)
				{
					lsse.downlinkQMs[gr3110Idx].dl_qual[h].containsData = true;
					lsse.downlinkQMs[gr3110Idx].dl_qual[h].numGood +=
						qle.gr3110Count;
				}
				if (iridiumIdx != -1)
				{
					lsse.downlinkQMs[iridiumIdx].dl_qual[h].containsData = true;
					lsse.downlinkQMs[iridiumIdx].dl_qual[h].numGood +=
						qle.iridiumCount;
				}
				if (edlIdx != -1)
				{
					lsse.downlinkQMs[edlIdx].dl_qual[h].containsData = true;
					lsse.downlinkQMs[edlIdx].dl_qual[h].numGood += qle.edlCount;
				}
			}
		}
		catch(IOException ex)
		{
			log.atWarn().setCause(ex).log("Cannot initialize quality measurements.");
		}
		try { if (lnr != null) lnr.close(); }
		catch(Exception ex) {}
	}

	/**
	 * Parses a line from the log file into a qual log entry.
	 * @param line the line of text.
	 * @param file the File object.
	 * @param linenum the line number in the file.
	 * @return entry or null if parse error.
	 */
	public QualLogEntry parse(String line, File file, int linenum)
	{
		StringTokenizer st = new StringTokenizer(line);
		int n = st.countTokens();
		if (n < 17)
			return null;

		QualLogEntry qle = new QualLogEntry();
		try
		{
			qle.timeStamp = sdf.parse(st.nextToken());
		}
		catch(ParseException ex)
		{
			log.atWarn().setCause(ex).log("Invalid date in '{}({})'", file.getName(), linenum);
			return null;
		}
		try
		{
			qle.domsatGood = Integer.parseInt(st.nextToken());
			qle.domsatErr = Integer.parseInt(st.nextToken());
			qle.aveDomsatBER = st.nextToken();
			qle.maxDomsatBER = st.nextToken();
			qle.drgsGood = Integer.parseInt(st.nextToken());
			qle.drgsErr = Integer.parseInt(st.nextToken());
			qle.ddsGood = Integer.parseInt(st.nextToken());
			qle.ddsErr = Integer.parseInt(st.nextToken());
			qle.noaaportGood = Integer.parseInt(st.nextToken());
			qle.noaaportErr = Integer.parseInt(st.nextToken());
			qle.lritGood = Integer.parseInt(st.nextToken());
			qle.lritErr = Integer.parseInt(st.nextToken());
			qle.netbackGood = Integer.parseInt(st.nextToken());
			qle.netbackErr = Integer.parseInt(st.nextToken());
			qle.archivedGood = Integer.parseInt(st.nextToken());
			qle.archivedErr = Integer.parseInt(st.nextToken());
			if (st.hasMoreTokens())
			{
				qle.domsatDropped = Integer.parseInt(st.nextToken());
				if (st.hasMoreTokens())
				{
					qle.gr3110Count = Integer.parseInt(st.nextToken());
					if (st.hasMoreTokens())
					{
						qle.iridiumCount = Integer.parseInt(st.nextToken());
						if (st.hasMoreTokens())
							qle.edlCount = Integer.parseInt(st.nextToken());
					}
				}
			}
		}
		catch(Exception ex)
		{
			log.atWarn().setCause(ex).log("Parse error in '{}({})'", file.getName(), linenum);
			return null;
		}
		return qle;
	}

	private String qle2String(QualLogEntry qle)
	{
		return sdf.format(qle.timeStamp)
			+ " " + qle.domsatGood
			+ " " + qle.domsatErr
			+ " " + qle.aveDomsatBER
			+ " " + qle.maxDomsatBER
			+ " " + qle.drgsGood
			+ " " + qle.drgsErr
			+ " " + qle.ddsGood
			+ " " + qle.ddsErr
			+ " " + qle.noaaportGood
			+ " " + qle.noaaportErr
			+ " " + qle.lritGood
			+ " " + qle.lritErr
			+ " " + qle.netbackGood
			+ " " + qle.netbackErr
			+ " " + qle.archivedGood
			+ " " + qle.archivedErr
			+ " " + qle.domsatDropped
			+ " " + qle.gr3110Count
			+ " " + qle.iridiumCount
			+ " " + qle.edlCount;
	}
}
