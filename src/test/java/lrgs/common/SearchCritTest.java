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
*  Revision 1.4  2005/03/07 21:35:25  mjmaloney
*  dev
*
*  Revision 1.3  2000/01/25 01:59:28  mike
*  Fixed julian date parsing and made it consistent with Java version.
*
*  Revision 1.2  1999/09/21 08:51:11  mike
*  CmdLineProg changed to CmdLineProcessor
*
*  Revision 1.1  1999/09/16 16:24:54  mike
*  created
*
*
*/
package lrgs.common;

import java.util.Date;
import java.io.File;
import java.io.IOException;
import java.io.EOFException;

import ilex.util.*;

public class SearchCritTest extends CmdLineProcessor
{
	SearchCriteria sc;

	public SearchCritTest()
	{
		super();

		// Define a command list and the individual command actions.
		addHelpAndQuitCommands();

		addCmd(
			new CmdLine("read", "<filename>  - read specified file.")
			{
				public void execute(String[] tokens) throws EOFException
				{
					if (tokens.length > 1)
					{
						try 
						{
							sc = new SearchCriteria(new File(tokens[1])); 
						}
						catch (Exception ioe)
						{
							errorMsg(ioe.toString());
						}
					}
				}
			});

		addCmd(
			new CmdLine("print", "- Print criteria in standard file format.")
			{
				public void execute(String[] tokens) throws EOFException
				{
					if (sc != null)
					{
						System.out.print(sc);
						System.out.println("");
					}
					else
						errorMsg("No search criteria defined!");
				}
			});

		addCmd(
			new CmdLine("eval", "- Evaluate search criteria & show results.")
			{
				public void execute(String[] tokens) throws EOFException
				{
					if (sc != null)
					{
						try
						{
							Date d = sc.evaluateLrgsSinceTime();
							System.out.println("LRGS_SINCE: " 
								+ sc.getLrgsSince());
							System.out.println("\ttime_t: " 
								+ (d.getTime() / 1000));
							System.out.println("\t  eval: " + 
									IDateFormat.toString(d, false));
	
							d = sc.evaluateLrgsUntilTime();
							System.out.println("LRGS_UNTIL: " 
								+ sc.getLrgsUntil());
							System.out.println("\ttime_t: " 
								+ (d.getTime() / 1000));
							System.out.println("\t  eval: " 
								+ IDateFormat.toString(d, false));
	
							d = sc.evaluateDapsSinceTime();
							System.out.println("DAPS_SINCE: " 
								+ sc.getDapsSince());
							System.out.println("\ttime_t: " 
								+ (d.getTime() / 1000));
							System.out.println("\t  eval: " + 
									IDateFormat.toString(d, false));
	
							d = sc.evaluateDapsUntilTime();
							System.out.println("DAPS_UNTIL: " 
								+ sc.getDapsUntil());
							System.out.println("\ttime_t: " 
								+ (d.getTime() / 1000));
							System.out.println("\t  eval: " + 
									IDateFormat.toString(d, false));
						}
						catch(Exception ex)
						{
							errorMsg(ex.toString());
						}
					}
					else
						errorMsg("No search criteria defined!");
				}
			});

	}

	public static void main(String args[])
	{
		SearchCritTest sct = new SearchCritTest();
		int n = 0;
		try
		{
			n = sct.processInput();
		}
		catch(IOException ioe)
		{}
		System.out.println(n + " commands executed");
	}

}
