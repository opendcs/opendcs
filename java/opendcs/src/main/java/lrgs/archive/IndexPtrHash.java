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

import java.io.File;
import java.io.IOException;
import java.io.EOFException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Date;
import java.util.TimeZone;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.common.DcpAddress;
import lrgs.common.DcpMsgFlag;
import decodes.util.Pdt;

public class IndexPtrHash
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	public static String module = "IndexHashIo";

	/** IndexPtr entries hashed by DCP address.  */
	private HashMap<DcpAddress, IndexPtr> theHash
		= new HashMap<DcpAddress, IndexPtr>();

	public IndexPtrHash()
	{

	}

	/**
	 * Saves the hash map of index pointers.
	 */
	public synchronized void saveIndexPtrHash(String filePath,
		MsgArchive msgArchive)
	{


		// First trim the hash. Otherwise over time it fills up with
		// invalid DCP addresses and entries where the last receive time
		// is older than the beginning of the archive.
		Pdt pdt = Pdt.instance();
		boolean pdtIsValid = pdt.isLoaded() && pdt.size() > 20000;
		MsgPeriodArchive mpa = msgArchive.getPeriodArchive(0, true);
		int msgTimeCutoff = mpa != null ? mpa.startTime : 0;
		ArrayList<DcpAddress> addrs = new ArrayList<DcpAddress>();
		addrs.addAll(theHash.keySet());
		for(DcpAddress addr : addrs)
		{
			IndexPtr ip = theHash.get(addr);
			if (ip != null
			 && (ip.flagBits & DcpMsgFlag.MSG_TYPE_MASK) == DcpMsgFlag.MSG_TYPE_GOES
			 && (ip.msgTime < msgTimeCutoff
			     || (pdtIsValid && pdt.find(addr) == null)))
			{
				theHash.remove(addr);
			}
		}
		File hashFile = new File(filePath);
		log.trace(" Saving '{}' msgTimeCutoff={} ({}) pdtIsValid={}",
				  hashFile.getPath(), msgTimeCutoff, msgTimeCutoff,
				  new Date(msgTimeCutoff*1000L), pdtIsValid);
		try (DataOutputStream dos =  new DataOutputStream(
				new BufferedOutputStream(
					new FileOutputStream(hashFile))))
		{
			for(Iterator<DcpAddress> it=theHash.keySet().iterator();it.hasNext();)
			{
				DcpAddress addr = it.next();
				IndexPtr ip = theHash.get(addr);
				dos.writeUTF(addr.toString());
				dos.writeInt(ip.indexFileStartTime);
				dos.writeInt(ip.indexNumber);
				dos.writeInt(ip.msgTime);
				int flag_len = ip.flagBits | (ip.msgLength << 16);
				dos.writeInt(flag_len);
			}
		}
		catch(IOException ex)
		{
			log.atWarn().setCause(ex).log("{} - Cannot save index pointer hash", MsgArchive.EVT_BAD_HASH);
		}
	}

	public synchronized void loadIndexPtrHash(File archiveDir, String fileName)
	{

		File hashFile = new File(archiveDir, fileName);
		theHash.clear();
		try (DataInputStream dis = new DataInputStream(
				new BufferedInputStream(
					new FileInputStream(hashFile))))
		{
			while(true)
			{
				String straddr = dis.readUTF();
				int start = dis.readInt();
				int num = dis.readInt();
				int time = dis.readInt();
				int flag_len = dis.readInt();
				int flagBits = flag_len & 0xffff;
				int len = (flag_len>>16) & 0xffff;
				theHash.put(new DcpAddress(straddr),
					new IndexPtr(start, num, time, flagBits, len));
			}
		}
		catch(EOFException ex)
		{
			/* expected */
		}
		catch(IOException ex)
		{
			log.warn("{}- Error loading index pointer hash ({} elements loaded): ",
					 MsgArchive.EVT_BAD_HASH, theHash.size());
		}
		log.info("loaded index pointer hash ({} elements loaded) from {}", theHash.size(), hashFile.getPath());
	}

	public synchronized IndexPtr get(DcpAddress addr)
	{
		return theHash.get(addr);
	}

	public synchronized void put(DcpAddress addr, IndexPtr lastPtr)
	{
		theHash.put(addr, lastPtr);
	}

	/**
	 * Test main program. Pass filename on command line, prints hash.
	 */
	public static void main(String args[])
	{
		IndexPtrHash lastMsgHash = new IndexPtrHash();
		File archiveDir = new File(".");
		lastMsgHash.loadIndexPtrHash(archiveDir, args[0]);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		sdf2.setTimeZone(TimeZone.getTimeZone("UTC"));
		for(Iterator<DcpAddress> it=lastMsgHash.theHash.keySet().iterator();
			it.hasNext();)
		{
			DcpAddress addr = it.next();
			IndexPtr ip = lastMsgHash.get(addr);
			Date fstart = new Date(ip.indexFileStartTime * 1000L);
			Date mtime = new Date(ip.msgTime * 1000L);

			System.out.println(addr.toString()
				+ " last@" + sdf.format(fstart) + " #" + ip.indexNumber
				+ " mtime=" + sdf2.format(mtime)
				+ " flags=0x" + Integer.toHexString(ip.flagBits)
				+ " len=" + ip.msgLength);
		}
	}
}
