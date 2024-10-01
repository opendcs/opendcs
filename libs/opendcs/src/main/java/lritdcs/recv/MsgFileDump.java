package lritdcs.recv;

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
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		long loc = 0L;
		boolean done = false;
		for(int i=1; !done; i++)
		{
			try
			{
				DcpMsg dcpMsg = msgFile.readMsg(loc);
				System.out.println("======================================");
				System.out.println("DCP Message " + i + "   location=" + loc);
				System.out.println("RecvTime=" 
					+ sdf.format(dcpMsg.getLocalReceiveTime())
					+ "     SequenceNum=" + dcpMsg.getSequenceNum()
					+ "     Flags=0x" + Integer.toHexString(dcpMsg.flagbits));
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

