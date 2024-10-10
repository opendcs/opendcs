/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:16  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2005/12/30 19:41:00  mmaloney
*  dev
*
*  Revision 1.3  2003/12/20 01:45:15  mjmaloney
*  dev
*
*  Revision 1.2  2003/08/11 15:59:19  mjmaloney
*  dev
*
*  Revision 1.1  2003/08/11 01:33:58  mjmaloney
*  dev
*
*/
package lritdcs;

import java.io.*;

public class ScrubberThread
	extends LritDcsThread
{
	File directories[];
	File mediumDir, mediumSentDir;
	File lowDir, lowSentDir;
	int scrubHours;

	public ScrubberThread()
	{
		super("ScrubberThread");
		scrubHours = 48;
	}

	public void run()
	{
		debug1("Starting");
		while(!shutdownFlag)
		{
			try { sleep(60000L); }
			catch(InterruptedException ex) {}

			long now = System.currentTimeMillis();
			for(int i=0; i<directories.length; i++)
			{
				File file[] = directories[i].listFiles();
				for(int j=0; file != null && j<file.length; j++)
				{
					long lmt = file[j].lastModified();
					if ((now - lmt) > (scrubHours * 60L * 60L * 1000L))
					{
						debug1("Deleting file '" + file[j].getPath() + "'");
						try { file[j].delete(); }
						catch(Exception ex)
						{
							warning(Constants.EVT_FILE_DELETE_ERR, 
								"- Unable to delete file '" 
								+ file[j].getPath() + "': " + ex);
						}
					}
				}
			}
		}
	}

	public void init()
		throws InitFailedException
	{
		// Create the directory objects. Make sure they all exist and are
		// directories.
		directories = new File[6];

		int i=0;
		try
		{
			String home = LritDcsConfig.instance().getLritDcsHome();
			directories[0] = new File(home + File.separator + "high");
			directories[1] = new File(home + File.separator + "high.sent");
			directories[2] = new File(home + File.separator + "medium");
			directories[3] = new File(home + File.separator + "medium.sent");
			directories[4] = new File(home + File.separator + "low");
			directories[5] = new File(home + File.separator + "low.sent");
			for(i=0; i<directories.length; i++)
			{
				if (!directories[i].exists())
					directories[i].mkdirs();
			}
		}
		catch(Exception ex)
		{
			throw new InitFailedException(
				"Can't create directory '" + directories[i].getPath() + "': "
				+ ex);
		}

		getConfigValues(LritDcsConfig.instance());
		registerForConfigUpdates();
	}

	protected void getConfigValues(LritDcsConfig cfg)
	{
		scrubHours = cfg.getScrubHours();
	}
}

