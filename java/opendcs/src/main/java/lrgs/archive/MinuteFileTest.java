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
*  Revision 1.2  2005/06/24 15:57:28  mjmaloney
*  Java-Only-Archive implementation.
*
*  Revision 1.1  2005/06/06 21:15:26  mjmaloney
*  Added new Java-Only Archiving Package
*
*/
package lrgs.archive;

public class MinuteFileTest
{
	public static void main(String args[])
		throws Exception
	{
		if (args.length < 2)
		{
			System.err.println("Usage java com.ilexeng.dcstool.archive [r|w]");
			System.exit(1);
		}
		if (args.length > 0 && args[0].equals("w"))
			writeTest(args[1]);
		else if (args.length > 0 && args[0].equals("r"))
			readTest(args[1]);
		else
			System.err.println("Usage java com.ilexeng.dcstool.archive [r|w]");
	}

	public static void writeTest(String fn)
		throws Exception
	{
		MsgIndexMinute mns[] = new MsgIndexMinute[1440];
		for(int i=0; i<1440; i++)
		{
			mns[i] = new MsgIndexMinute();
			mns[i].startIndexNum = i;
			mns[i].reserved = i;
			mns[i].oldestDapsTime = i;
		}
		MinuteFile.save(fn, mns);
	}

	public static void readTest(String fn)
		throws Exception
	{
		MsgIndexMinute mns[] = new MsgIndexMinute[1440];
		for(int i=0; i<1440; i++)
			mns[i] = new MsgIndexMinute();
		MinuteFile.load(fn, mns);
		for(int i=0; i<1440; i++)
		{
			System.out.println("mns[" + i 
				+ "] start=" + mns[i].startIndexNum
				+ ", end=" + mns[i].reserved
				+ ", oldest=" + mns[i].oldestDapsTime);
		}
	}
}
