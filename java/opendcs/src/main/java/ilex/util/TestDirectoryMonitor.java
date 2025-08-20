/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/08/30 15:44:02  mjmaloney
*  Removed import statements for classes within ilex.util.
*
*  Revision 1.2  2004/08/30 14:50:31  mjmaloney
*  Javadocs
*
*  Revision 1.1  2004/04/02 18:58:16  mjmaloney
*  Created.
*
*/
package ilex.util;

import java.io.*;

public class TestDirectoryMonitor extends DirectoryMonitorThread
{
	File doneDir;

	TestDirectoryMonitor( )
	{
		super();
		setSleepEveryCycle(true);
		setSleepInterval(5000);
		doneDir = new File("./done");
		if (!doneDir.isDirectory())
			doneDir.mkdirs();
	}

	/**
	* @param file
	*/
	protected void processFile( File file )
	{
		System.out.println(file);
		file.renameTo(new File(doneDir, file.getName()));
	}
	
	public void finishedScan( )
	{
		System.out.println("Completed Scan");
	}
	
	/**
	* @param args
	*/
	public static void main( String[] args )
	{
		TestDirectoryMonitor myMonitor = new TestDirectoryMonitor();

		File dir = new File("./dir1");
		if (!dir.isDirectory())
			dir.mkdirs();
		myMonitor.addDirectory(dir);

		dir = new File("./dir2");
		if (!dir.isDirectory())
			dir.mkdirs();
		myMonitor.addDirectory(dir);

		myMonitor.start();
	}

	@Override
	protected void cleanup()
	{
	}
}
