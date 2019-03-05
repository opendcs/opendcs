/*
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
 * Revision 1.1  2017/03/21 12:17:10  mmaloney
 * First working XML and SQL I/O.
 *
 */
package decodes.tsdb.alarm;

import java.io.IOException;

import opendcs.dai.AlarmDAI;
import opendcs.dao.AlarmDAO;
import lrgs.gui.DecodesInterface;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.Logger;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TsdbAppTemplate;
import decodes.tsdb.alarm.xml.AlarmXio;
import decodes.util.CmdLineArgs;

/**
 * Export named alarm group to XML file. Writes to stdout.
 * @author mmaloney
 *
 */
public class AlarmExport
	extends TsdbAppTemplate
{
	private StringToken grpNameArg = new StringToken("", "Name of Alarm Group to Export",
		"", TokenOptions.optArgument | TokenOptions.optRequired, ""); 
	
	public AlarmExport()
	{
		super("util.log");
		DecodesInterface.silent = true;
	}
	
	public static void main(String[] args)
		throws Exception
	{
		AlarmExport alarmExport = new AlarmExport();
		alarmExport.execute(args);
	}

	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(grpNameArg);
	}


	@Override
	protected void runApp() 
	{
		String grpName = grpNameArg.getValue();
		if (grpName == null || grpName.trim().length() == 0)
		{
			System.err.println("Missing required arg -- group name to export.");
			System.exit(1);
		}
		
		AlarmDAI alarmDAO = new AlarmDAO(TsdbAppTemplate.theDb);
		AlarmXio alarmXio = new AlarmXio();
		AlarmConfig cfg = new AlarmConfig();
		
		try
		{
			alarmDAO.check(cfg);
			for(AlarmGroup grp : cfg.getGroups())
				if (grpName.equalsIgnoreCase(grp.getName()))
				{
					alarmXio.writeXML(grp, System.out);
					System.exit(0);
				}
			String msg = "No such alarm group '" + grpName + "'.";
			Logger.instance().failure(msg);
			System.err.println(msg);
		}
		catch(IOException ex)
		{
			String msg = "Cannot write XML group '" + grpName + "': " + ex;
			Logger.instance().failure(msg);
			System.err.println(msg);
		}
		catch(DbIoException ex)
		{
			String msg = "Cannot read groups from database: " + ex;
			Logger.instance().failure(msg);
			System.err.println(msg);
		}
		finally
		{
			alarmDAO.close();
		}
		System.exit(1);
	}

}
