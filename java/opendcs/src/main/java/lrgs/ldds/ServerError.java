/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package lrgs.ldds;

import java.text.NumberFormat;
import java.text.ParsePosition;

import ilex.util.TextUtil;

import lrgs.common.LrgsErrorCode;

/**
  ServerError is used by the client interfaces to the LRGS Data Server.
  When the client receives an error message from the server in response
  to a request, it will package the error info into a ServerError
  exception object, and then throw it.
*/
public class ServerError extends Exception
{
	public int Derrno, Errno;
	public String msg;

	/**
	  Create a ServerError exception from a string in the form:
		?Derrno,Errno,text-message
	  ...where Derrno and Errno are integers, and text-message is
	  an explantatory string provided by the server.
	  @param msg the message
	*/
	public ServerError(String msg)
	{
		// Initialize super class Exception with complete string
		super(msg);
		set(msg);
	}

	public ServerError(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	/**
	  Create a ServerError exception where Derrno and Errno are
	  explicitely set.
	*/
	public ServerError(String msg, int Derrno, int Errno)
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
		if (TextUtil.skipWhitespace(s, pp) == false)
			return;

		Number n = nf.parse(s, pp);
		if (n == null)
			return;
		Derrno = n.intValue();

		// Skip comma and white space before/after it.
		if (TextUtil.skipWhitespace(s, pp) == false)
			return;
		i = pp.getIndex();
		if (s.charAt(i) != ',')
			return;
		pp.setIndex(i+1);
		if (TextUtil.skipWhitespace(s, pp) == false)
			return;

		n = nf.parse(s, pp);
		if (n == null)
			return;
		Errno = n.intValue();

		if (TextUtil.skipWhitespace(s, pp) == false)
			return;
		i = pp.getIndex();
		if (s.charAt(i) != ',')
			return;
		pp.setIndex(i+1);
		if (TextUtil.skipWhitespace(s, pp) == false)
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

}
