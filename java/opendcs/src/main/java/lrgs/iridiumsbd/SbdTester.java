/*
 * Open source software by Cove Software, LLC.
 * Prepared under contract to the U.S. Government.
 * Copyright 2014 United States Government, U.S. Geological Survey
 * 
 * $Id$
 * 
 * $Log$
*/
package lrgs.iridiumsbd;

import ilex.net.BasicClient;
import ilex.util.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;

public class SbdTester
{
	/**
	 * Usage: java ... lrgs.iridiumsbd.SbdTester dir host [port]
	 * If port not supplied, use default of 10800.
	 * The directory 'dir' must contain Iridium SBD capture files.
	 * For each file, it modifies the date to be current and then
	 * sends it to the specified host/port as an Iridium SBD message.
	 * It then sleeps for 5 seconds after each file.
	 * @param args
	 */
	public static void main(String[] args)
		throws Exception
	{
		File dir = new File(args[0]);
		if (!dir.isDirectory())
		{
			System.err.println("Directory '" + args[0] + "' doesn't exist.");
			System.exit(1);;
		}
		
		String host = args[1];
		int port = IridiumSbdInterface.SBD_LISTEN_PORT;
		if (args.length > 2)
			port = Integer.parseInt(args[2]);
		
		File capfiles[] = dir.listFiles();
		for(File capfile : capfiles)
		{
			process(capfile, host, port);
			Thread.sleep(5000L);
		}

	}

	private static void process(File capfile, String host, int port)
		throws Exception
	{
		BasicClient client = new BasicClient(host, port);
		client.connect();
		FileInputStream fis = new FileInputStream(capfile);
		byte filedata[] = new byte[(int)capfile.length()];
		for(int idx = 0; idx < filedata.length; idx++)
			filedata[idx] = (byte)fis.read();
		fis.close();
		
		
	}

}
