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
*  Revision 1.2  2009/10/09 18:14:35  mjmaloney
*  GenLritDcsFile abandoned
*
*  Revision 1.1  2008/04/04 18:21:16  cvs
*  Added legacy code to repository
*
*  Revision 1.1  2006/03/23 15:17:03  mmaloney
*  created.
*
*/
package lritdcs;

import java.io.File;
import java.util.TimeZone;
import java.util.Date;
import java.text.SimpleDateFormat;

public class GenLritDcsFile
{
	public static void main(String args[])
		throws Exception
	{
		if (args.length != 2)
		{
			System.err.println("Arguments: DCP-Addr  Fail-Code");
			System.exit(0);
		}
		LritDcsFile ldf = new LritDcsFile(Constants.HighPri, 
			new File("."), Constants.SC_Both);

		StringBuffer sb = new StringBuffer(
			"AAAAAAAAyydddhhmmssFssoonn177EII00010abcdefghij");
		for(int i=0; i<args[0].length() && i < 8; i++)
			sb.setCharAt(i, args[0].charAt(i));
		sb.setCharAt(19, args[1].charAt(0));

		Date d = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyDDDHHmmss");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		String s = sdf.format(d);
		for(int i=0; i<s.length(); i++)
			sb.setCharAt(i+8, s.charAt(i));

//		ldf.addMessage(sb.toString().getBytes());
		File f = ldf.saveFile();
		System.out.println("Data saved to " + f.getName());
	}
}
