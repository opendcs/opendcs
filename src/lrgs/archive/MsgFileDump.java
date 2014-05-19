/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of this source
*  code may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*
*  $Log$
*  Revision 1.6  2010/08/04 20:48:05  mmaloney
*  dev
*
*  Revision 1.5  2010/08/04 20:35:17  mmaloney
*  dev
*
*  Revision 1.4  2010/08/04 20:14:24  mmaloney
*  dev
*
*  Revision 1.3  2010/08/04 20:12:30  mmaloney
*  null-pointer in dump
*
*  Revision 1.2  2008/09/05 13:03:34  mjmaloney
*  LRGS 7 dev
*
*  Revision 1.1  2008/04/04 18:21:11  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2007/02/22 00:36:54  mmaloney
*  Release preparation for LRGS 6.0
*
*  Revision 1.4  2007/01/22 00:35:34  mmaloney
*  Dams NT 8.1 implementation
*
*  Revision 1.3  2007/01/12 21:45:18  mmaloney
*  Archive file changes for LRGS Version 6.
*
*  Revision 1.2  2005/11/21 19:13:40  mmaloney
*  LRGS 5.4 prep
*
*  Revision 1.1  2005/06/23 15:47:16  mjmaloney
*  Java archive search algorithms.
*
*/
package lrgs.archive;

import java.io.File;
import java.io.EOFException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import lrgs.common.DcpMsg;

public class MsgFileDump
{
	public static void main(String args[])
		throws Exception
	{
		MsgFile msgFile = new MsgFile(new File(args[0]), false);
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		SimpleDateFormat edf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	
		long loc = 0L;
		if (args.length > 1 && Character.isDigit(args[1].charAt(0)))
		{
			try { loc = Long.parseLong(args[1]); }
			catch(NumberFormatException ex)
			{
				System.err.println("Bad start offset " + args[1]);
				System.exit(1);
			}
		}
		
		boolean done = false;
		for(int i=1; !done; i++)
		{
			try
			{
				DcpMsg dcpMsg = msgFile.readMsg(loc);
				if (dcpMsg == null)
				{
					System.out.println("Abnormal null response from msgFile.readMsg(" + loc + ")");
					break;
				}
				System.out.println("======================================");
				System.out.println("DCP Message " + i + "   location=" + loc);
				Date rt = dcpMsg.getLocalReceiveTime();
				System.out.println("RecvTime=" 
					+ (rt != null ? sdf.format(rt) : "null")
					+ "     SequenceNum=" + dcpMsg.getSequenceNum()
					+ "     Flags=0x" + Integer.toHexString(dcpMsg.flagbits));
				
				System.out.println("MergeFilterCode=" + dcpMsg.mergeFilterCode
					+ "     Baud=" + dcpMsg.getBaud()
					+ "     DataSourceId=" + dcpMsg.getDataSourceId());
				Date cs = dcpMsg.getCarrierStart();
				Date ce = dcpMsg.getCarrierStop();
				if (cs != null && ce != null)
					System.out.println("CarrierStart=" + edf.format(cs)
						+ "     CarrierEnd=" + edf.format(ce));

				Date dt = dcpMsg.getDomsatTime();
				if (dt != null)
					System.out.println("DomsatTime=" + edf.format(dt));
				dt = dcpMsg.getXmitTime();
				if (dt != null)
					System.out.println("XmitTime=" + edf.format(dt));

				System.out.println(new String(dcpMsg.getData()));
				loc = msgFile.getLocation();
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
	}
}

