/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of this source
*  code may be claimed to be proprietary.
*
* $Log$
* Revision 1.6  2013/05/07 20:52:35  mmaloney
* dev
*
* Revision 1.5  2013/05/07 19:36:35  mmaloney
* dev
*
* Revision 1.4  2013/05/07 19:35:14  mmaloney
* dev
*
* Revision 1.3  2013/05/07 19:33:56  mmaloney
* dev
*
* Revision 1.2  2013/05/07 19:28:01  mmaloney
* dev
*
* Revision 1.1  2013/05/07 18:34:29  mmaloney
* Created.
*
*/
package lrgs.archive;

import java.io.File;
import java.io.EOFException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;

public class MsgFileRebuild
{
	static SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	static { sdf.setTimeZone(TimeZone.getTimeZone("UTC")); }
	
	public static void main(String args[])
		throws Exception
	{
		MsgFile msgFileIN = new MsgFile(new File(args[0]), false);
		
		File outFile = new File(args[0] + "-reb");
		if (outFile.exists())
			outFile.delete();
		MsgFile msgFileOUT = new MsgFile(outFile, true);
		HashMap<String, DcpMsg> msgMap = new HashMap<String, DcpMsg>();

		long loc = 0L;
		
		boolean done = false;
		int goes, deleted, dups;
		goes = deleted = dups = 0;
		for(int i=1; !done; i++)
		{
			try
			{
				DcpMsg dcpMsg = msgFileIN.readMsg(loc);
				if (dcpMsg == null)
				{
					System.out.println("Abnormal null response from msgFile.readMsg(" + loc + ")");
					break;
				}
				int f = dcpMsg.getFlagbits();
				System.out.printf("0x%X", f);
				if ((f & DcpMsgFlag.MSG_DELETED) != 0)
				{
					System.out.println("-d");
					deleted++;
				}
				else if (dcpMsg.isGoesMessage())
				{
					System.out.println("-g");
					goes++;
				}
				else
				{
					String key = makeKey(dcpMsg);
					if (msgMap.put(key, dcpMsg) == null)
						System.out.println(" - " + key);
					else
						dups++;
				}

				loc = msgFileIN.getLocation();
			}
			catch(EOFException ex) 
			{
				if (args.length == 2 && args[1].equals("+"))
				{
					try { Thread.sleep(1000L); } 
					catch(InterruptedException ie) {}
				}
				else
					done = true;
			}
		}
		for(DcpMsg dcpMsg : msgMap.values())
			msgFileOUT.writeMsg(dcpMsg);
		System.out.println("goes=" + goes + ", deleted=" + deleted + ", written=" + msgMap.values().size()
			+ ", dups=" + dups);
	}
	
	private static String makeKey(DcpMsg msg)
	{
		return msg.getDcpAddress() + ":" + sdf.format(msg.getXmitTime());
	}
}

