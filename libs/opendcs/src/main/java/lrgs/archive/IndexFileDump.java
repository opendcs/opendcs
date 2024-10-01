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
*  Revision 1.2  2008/09/05 13:03:34  mjmaloney
*  LRGS 7 dev
*
*  Revision 1.1  2008/04/04 18:21:11  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2007/01/22 00:35:33  mmaloney
*  Dams NT 8.1 implementation
*
*  Revision 1.4  2006/08/21 21:41:34  mmaloney
*  dev
*
*  Revision 1.3  2005/11/21 19:13:40  mmaloney
*  LRGS 5.4 prep
*
*  Revision 1.2  2005/06/30 15:15:27  mjmaloney
*  Java Archive Development.
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
import lrgs.common.DcpMsgIndex;
import lrgs.common.DcpAddress;

public class IndexFileDump
{
	public static void main(String args[])
		throws Exception
	{
		IndexFile indexFile = new IndexFile(args[0], false);
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		long loc = 0L;
		boolean done = false;
		DcpMsgIndex[] indexes = new DcpMsgIndex[100];
		for(int i=0; i<100; i++)
			indexes[i] = new DcpMsgIndex();

		int start = 0;
		int end = -1;
		if (args.length > 1)
		{
			if (!args[1].equals("+"))
				start = Integer.parseInt(args[1]);
			if (args.length > 2)
			{
				if (!args[2].equals("+"))
					end = Integer.parseInt(args[2]);
			}
		}
		System.out.println("Dumping from index " + start + " to " + end);

		for(int i=start; !done; )
		{
			if (end != -1 && i >= end)
				done = true;
			try
			{
				int n = indexFile.readIndexes(i, 100, indexes);
				System.out.println("======================================");
				for(int j=0; j<n; j++)
				{
					if (end != -1 && i+j > end)
					{
						done = true;
						break;
					}
					DcpMsgIndex idx = indexes[j];
					System.out.println("Index[" + (i+j) + "]:");
					System.out.println("\tRecvTime=" 
						+ sdf.format(idx.getLocalRecvTime())
						+ "     DapsTime="
						+ sdf.format(idx.getXmitTime()));
					DcpAddress da = idx.getDcpAddress();
					System.out.println("\tOffset=" + idx.getOffset()
						+ "     addr=" + da.toString());
					System.out.println("\tSequenceNum=" 
						+ ((long)idx.getSequenceNum() & 0xffff)
						+ "     Channel = " + idx.getChannel()
						+ "     Flag = " + Integer.toHexString(idx.getFlagbits())
						+ "    FailureCode=" + idx.getFailureCode());
					System.out.println("\tmergFilterCode=" 
						+ (int)idx.getMergeFilterCode());
					System.out.println("\tPrev File Start="
						+ sdf.format(new Date((long)idx.getPrevFileThisDcp() *1000L))
						+ "     Prev Idx Num this DCP ="+idx.getPrevIdxNumThisDcp());
					System.out.println("\tData Source ID=" + idx.getDataSourceId()
						+ "     MsgLength=" + idx.getMsgLength());
				}
				i += n;
				if (n==0)
					done = true;
			}
			catch(EOFException ex) 
			{
				if (args.length > 1 && args[args.length-1].equals("+"))
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

