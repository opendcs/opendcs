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
*  Revision 1.4  2005/10/20 18:01:05  mmaloney
*  event nums
*
*  Revision 1.3  2005/09/19 21:31:50  mmaloney
*  dev
*
*  Revision 1.2  2005/06/24 15:57:28  mjmaloney
*  Java-Only-Archive implementation.
*
*  Revision 1.1  2005/06/06 21:15:26  mjmaloney
*  Added new Java-Only Archiving Package
*
*/
package lrgs.archive;

import java.io.IOException;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

import ilex.util.Logger;

/**
This class contains static methods for reading & writing a day file's
minute-index. The file is a binary array of 1440 MsgIndexMinute objects.
*/
public class MinuteFile
{
	/**
	 * Loads the array of MsgIndexMinute entries from the named file.
	 * @param fn the file name.
	 * @param mns the array of MsgIndexMinute structures to fill.
	 */
	public static void load( String fn, MsgIndexMinute[] mns )
		throws IOException
	{
		DataInputStream dis = null;
		try
		{
			dis = new DataInputStream(
					new BufferedInputStream(
						new FileInputStream(fn)));

			for(int i=0; i<mns.length; i++)
			{
				mns[i].startIndexNum = dis.readInt();
				mns[i].reserved = dis.readInt();
				mns[i].oldestDapsTime = dis.readInt();
			}
			dis.close();
		}
		catch(IOException ex)
		{
			long t = System.currentTimeMillis();
			if ((t % (24L * 3600000L)) > 60000L) // after 1st minute of day?
				Logger.instance().warning(
					MsgArchive.module + ":" + MsgArchive.EVT_BAD_MINUTE_FILE
					+ "- Error reading '" + fn + "': " + ex);
			if (dis != null)
			{
				try { dis.close(); }
				catch(IOException ignore) {}
			}
			throw ex;
		}
	}
	
	/**
	 * Saves the array of MsgIndexMinute entries to the named file.
	 * @param fn the file name.
	 * @param mns the array of MsgIndexMinute structures to save.
	 */
	public static void save( String fn, MsgIndexMinute[] mns )
		throws IOException
	{
		DataOutputStream dos = null;
		try
		{
			dos = new DataOutputStream(
					new BufferedOutputStream(
						new FileOutputStream(fn)));

			for(int i=0; i<mns.length; i++)
			{
				dos.writeInt(mns[i].startIndexNum);
				dos.writeInt(mns[i].reserved);
				dos.writeInt(mns[i].oldestDapsTime);
			}
			dos.close();
		}
		catch(IOException ex)
		{
			Logger.instance().warning(
				MsgArchive.module + ":" + MsgArchive.EVT_BAD_MINUTE_FILE
				+ "- Error writing '" + fn + "': " + ex);
			if (dos != null)
			{
				try { dos.close(); }
				catch(IOException ignore) {}
			}
			throw ex;
		}
	}
}
