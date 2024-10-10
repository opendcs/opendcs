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
*  Revision 1.1  2008/04/04 18:21:11  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2006/05/23 19:29:35  mmaloney
*  dev
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
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.EOFException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class IndexPtrDump
{
	public static void main(String args[])
		throws Exception
	{
		File f = new File(args[0]);
		DataInputStream dis = new DataInputStream(
                    new BufferedInputStream(
                        new FileInputStream(f)));
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy MM/dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		int i=0;
		try
		{
			while(true)
			{
				String straddr = dis.readUTF();
				int start = dis.readInt();
				int num = dis.readInt();
				int time = dis.readInt();
				int flag_len = dis.readInt();
				int flagBits = flag_len & 0xffff;
				int len = (flag_len >> 16) & 0xffff;
				System.out.println("" + (i++) + ": addr='" + straddr 
					+ "',  start=" + 
					sdf.format(new Date((long)start * 1000L))
					+ "  indexNum=" + num 
					+ "  msgTime=" + sdf.format(new Date((long)time * 1000L))
					+ "  flags=0x" + Integer.toHexString(flagBits)
					+ "  len=" + len);
			}
		}
		catch(EOFException ex) {}
	}
}
