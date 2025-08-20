/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.1  2007/11/20 16:32:47  mmaloney
*  dev
*
*  Revision 1.2  2004/08/30 14:50:27  mjmaloney
*  Javadocs
*
*  Revision 1.1  2004/03/31 14:28:03  mjmaloney
*  Added new test.
*
*/
package ilex.util;

import java.io.IOException;
import ilex.cmdline.*;


/**
Test class for file logger.
*/
public class SequenceLoggerTest
{
	/**
	* @param args
	*/
	public static void main( String[] args )
		throws Exception
	{
		String fn = "logtest";
		String procname = "logtest";
		SequenceFileLogger logger = new SequenceFileLogger(procname, fn);
		logger.setNumOldLogs(10);
		logger.setMaxLength(1000);
		logger.info("Test Logger Starting ====");
		for(int i = 0; i<1000; i++)
			logger.info("Test Message Number " + i);
		logger.info("Test Logger Done ! ====");
	}
}
