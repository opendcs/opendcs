/*
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
 * Revision 1.2  2017/05/17 20:36:26  mmaloney
 * First working version.
 *
 * Revision 1.1  2017/03/21 12:17:10  mmaloney
 * First working XML and SQL I/O.
 *
 */
package decodes.tsdb.alarm;

import opendcs.dai.AlarmDAI;
import opendcs.dao.AlarmDAO;
import lrgs.gui.DecodesInterface;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.Logger;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TsdbAppTemplate;
import decodes.tsdb.alarm.xml.AlarmXio;
import decodes.tsdb.xml.DbXmlException;
import decodes.util.CmdLineArgs;

public class AlarmImport
	extends TsdbAppTemplate
{
	private StringToken grpFileArg = new StringToken("", "Alarm Group XML File(s)",
		"", TokenOptions.optArgument | TokenOptions.optMultiple | TokenOptions.optRequired, ""); 
	
	public AlarmImport()
	{
		super("util.log");
		DecodesInterface.silent = true;
	}
	
	public static void main(String[] args)
		throws Exception
	{
		AlarmImport alarmImport = new AlarmImport();
		alarmImport.execute(args);
	}

	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(grpFileArg);
	}


	@Override
	protected void runApp() 
	{
		AlarmDAI alarmDAO = new AlarmDAO(TsdbAppTemplate.theDb);
		AlarmXio alarmXio = new AlarmXio();
		
		for(int i=0; i<grpFileArg.NumberOfValues(); i++)
		{
			String fn = grpFileArg.getValue(i);
			AlarmGroup grp = null;
			try
			{
				grp = alarmXio.readFile(fn);
				alarmDAO.write(grp);
			}
			catch(DbXmlException ex)
			{
				String msg = "Cannot read '" + fn + "': " + ex;
				Logger.instance().failure(msg);
				System.err.println(msg);
			}
			catch(DbIoException ex)
			{
				String msg = "Cannot write group '" + grp.getName() + "' to database: " + ex;
				Logger.instance().failure(msg);
				System.err.println(msg);
			}
			finally
			{
				alarmDAO.close();
			}
		}
	}

}
