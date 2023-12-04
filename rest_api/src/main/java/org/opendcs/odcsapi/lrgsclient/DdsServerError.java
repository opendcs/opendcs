/*
*  $Id: DdsServerError.java,v 1.1 2023/05/15 18:33:56 mmaloney Exp $
*
*  $Source: /home/cvs/repo/odcsapi/src/main/java/org/opendcs/odcsapi/lrgsclient/DdsServerError.java,v $
*
*  $State: Exp $
*
*  $Log: DdsServerError.java,v $
*  Revision 1.1  2023/05/15 18:33:56  mmaloney
*  First check-in of lrgsclient package, derived from OpenDCS lrgs.ldds classes but simplified for API.
*
*  Revision 1.1.1.1  2022/10/19 18:03:34  cvs
*  imported 7.0.1
*
*  Revision 1.1  2008/04/04 18:21:15  cvs
*  Added legacy code to repository
*
*  Revision 1.6  2004/08/30 14:51:49  mjmaloney
*  Javadocs
*
*  Revision 1.5  2000/09/08 19:49:56  mike
*  Release prep.
*
*  Revision 1.4  2000/09/08 17:12:20  mike
*  Truncate error message at first null byte.
*
*  Revision 1.3  2000/03/31 16:09:57  mike
*  Error codes are now in lrgs.common.LrgsErrorCode
*
*  Revision 1.2  2000/01/19 14:34:52  mike
*  Debug messages to detect garbage collection.
*
*  Revision 1.1  2000/01/07 19:10:00  mike
*  Generalizing client interface
*
*
*/
package org.opendcs.odcsapi.lrgsclient;

import java.text.NumberFormat;
import java.text.ParsePosition;

import org.opendcs.odcsapi.util.ApiTextUtil;

import lrgs.common.LrgsErrorCode;

/**
  ServerError is used by the client interfaces to the LRGS Data Server.
  When the client receives an error message from the server in response
  to a request, it will package the error info into a ServerError
  exception object, and then throw it.
*/
public class DdsServerError extends Exception
{
	public int Derrno;
	public int Errno;
	public String msg;

	/**
	  Create a ServerError exception from a string in the form:
		?Derrno,Errno,text-message
	  ...where Derrno and Errno are integers, and text-message is
	  an explantatory string provided by the server.
	  @param msg the message
	*/
	public DdsServerError(String msg)
	{
		// Initialize super class Exception with complete string
		super(msg);
		set(msg);
	}

	/**
	  Create a ServerError exception where Derrno and Errno are
	  explicitely set.
	*/
	public DdsServerError(String msg, int Derrno, int Errno)
	{
		this(msg);
		this.Derrno = Derrno;
		this.Errno = Errno;
	}

	public void set(String s)
	{
		// Initialize internal values.
		Derrno = Errno = 0;

		int i;
		for(i=0; i<s.length() && s.charAt(i) != (char)0; i++);
		msg = s.substring(0,i);   // If parsing fails, msg is the complete text.

		if (s.charAt(0) != '?')
			return;

		// Make a number formatter to parse the string.
		NumberFormat nf = NumberFormat.getNumberInstance();

		// no grouping, otherwise comma is not taken as a delimiter.
		nf.setGroupingUsed(false);

		// Skip '?' and white space before first number.
		ParsePosition pp = new ParsePosition(1);
		if (!ApiTextUtil.skipWhitespace(s, pp))
			return;

		Number n = nf.parse(s, pp);
		if (n == null)
			return;
		Derrno = n.intValue();

		// Skip comma and white space before/after it.
		if (ApiTextUtil.skipWhitespace(s, pp) == false)
			return;
		i = pp.getIndex();
		if (s.charAt(i) != ',')
			return;
		pp.setIndex(i+1);
		if (ApiTextUtil.skipWhitespace(s, pp) == false)
			return;

		n = nf.parse(s, pp);
		if (n == null)
			return;
		Errno = n.intValue();

		if (ApiTextUtil.skipWhitespace(s, pp) == false)
			return;
		i = pp.getIndex();
		if (s.charAt(i) != ',')
			return;
		pp.setIndex(i+1);
		if (ApiTextUtil.skipWhitespace(s, pp) == false)
			return;
		if (s.charAt(i) == ',')
			i++;

		msg = s.substring(i);
	}

	/** @return string representation of this exception. */
	public String toString()
	{
		StringBuffer r = new StringBuffer("Server Error: " + msg);
		if (Derrno != 0)
		{
			r.append(" (");
			r.append(LrgsErrorCode.code2string(Derrno));
			r.append("-" + Derrno);
			if (Errno != 0)
			{
				r.append(", Errno=");
				r.append(Errno);
			}
			r.append(") ");
			r.append(LrgsErrorCode.code2message(Derrno));
		}
		return new String(r);
	}

//	public static void main(String[] args)
//	{
//		ServerError se = new ServerError("?123,456,Big Problem, Yeah!");
//		System.out.println("se='" + se + "'");
//		se = new ServerError("? 123	,		456 ,Big Problem, Yeah!");
//		System.out.println("se='" + se + "'");
//		se = new ServerError("? 123	,		456 ,Big Problem -- Yeah!");
//		System.out.println("se='" + se + "'");
//	}
}

