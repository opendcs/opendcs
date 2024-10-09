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
*  Revision 1.1  2005/06/30 15:15:27  mjmaloney
*  Java Archive Development.
*
*/
package lrgs.archive;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
This class dumps the name message index minute hash file.
Each day file has one of these files.
*/
public class MsgIndexMinuteDump
{
	public static void main(String args[])
		throws Exception
	{
		MsgIndexMinute mim[] = new MsgIndexMinute[60*24];
		for(int i=0; i<60*24; i++)
			mim[i] = new MsgIndexMinute();
		MinuteFile.load(args[0], mim);
		DecimalFormat df = new DecimalFormat("00");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy MM/dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		System.out.println("Min.   start  end   oldest-msg-time");
		for(int i=0; i<60*24; i++)
		{
			System.out.println(
				df.format(i/60) + ":" + df.format(i%60)
				+ "   start=" + mim[i].startIndexNum
				+ "   reserved=" + mim[i].reserved
				+ "   oldest=" + sdf.format(
					new Date((long)mim[i].oldestDapsTime*1000L)));
		}
	}
}
