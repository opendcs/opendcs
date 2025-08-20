/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:16  cvs
*  Added legacy code to repository
*
*  Revision 1.1  2003/08/06 23:29:24  mjmaloney
*  dev
*
*/
package lritdcs;

import java.io.*;
import java.util.*;

public class TestLritDcsConfig
	extends Thread
	implements Observer
{
	TestLritDcsConfig()
	{
		LritDcsConfig cfg = LritDcsConfig.instance();
		cfg.load();
		System.out.println("Initial config loaded");
	}

	public void run()
	{
		System.out.println("Starting to observe...");
		LritDcsConfig.instance().addObserver(this);
		long start = System.currentTimeMillis();
		while(System.currentTimeMillis() - start < 120000)
		{
			try { sleep(1000L); } catch(InterruptedException ex) {}
			LritDcsConfig.instance().checkConfigFile();
		}
	}
	
	public void update(Observable obs, Object arg)
	{
		System.out.println("========================================");
		System.out.println("Configuration was changed");
		System.out.println("========================================");
		try { LritDcsConfig.instance().save(System.out); }
		catch(IOException ex) {}
		System.out.println("");
	}

	public static void main(String args[])
		throws IOException
	{
		TestLritDcsConfig tldc = new TestLritDcsConfig();
		tldc.run();
	}
}
