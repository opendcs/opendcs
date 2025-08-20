/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:12  cvs
*  Added legacy code to repository
*
*  Revision 1.2  1999/09/16 16:23:49  mike
*  9/16/1999
*
*  Revision 1.1  1999/09/03 15:34:52  mike
*  Initial checkin.
*
*
*/

package lrgs.common;

import java.io.*;
import java.util.Iterator;

import ilex.util.FileExceptionList;

/**
NetlistReader is a test class for reading network lists.
*/
public class NetlistReader
{
	public static void main(String args[])
	{
		if (args.length < 1)
		{
			System.err.println("usage: NetlistReader filename");
			return;
		}

		NetworkList nl;
		try
		{
			nl = new NetworkList(new File(args[0]));
		}
		catch(IOException e)
		{
			System.out.println("Cannot read file " + args[0]);
			return;
		}
		int n = nl.size();
		
		System.out.println("Read " + n + " DCPs from " + args[0]);
		for(int i=0; i < n; ++i)
		{
			NetworkListItem nli = (NetworkListItem)nl.elementAt(i);
			System.out.println(i + ": " + nli);
		}
		
		if (nl.getNumExceptions() > 0)
		{
			System.out.println("There are " + nl.getNumExceptions() + 
				" Exceptions");
			FileExceptionList fel = nl.getWarnings();
			Iterator it = fel.iterator();
			while(it.hasNext())
				System.out.println(fel.file + " line " + it.next());
		}
	}
}
		
		
