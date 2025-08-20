/*
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
 * Revision 1.1  2019/03/05 14:53:00  mmaloney
 * Checked in partial implementation of Alarm classes.
 *
 * Revision 1.1  2017/03/30 20:55:20  mmaloney
 * Alarm and Event monitoring capabilities for 6.4 added.
 *
 */
package decodes.tsdb.alarm;


import opendcs.dai.AlarmDAI;
import opendcs.dao.AlarmDAO;
import lrgs.gui.DecodesInterface;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import decodes.sql.DbKey;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.CmdLineArgs;

/**
 * Lists names of alarm groups in the database.
 * @author mmaloney
 *
 */
public class AlarmDelete
	extends TsdbAppTemplate
{
	private StringToken grpNameArg = new StringToken("", "Name of Alarm Group to Delete",
		"", TokenOptions.optArgument | TokenOptions.optRequired, ""); 

	public AlarmDelete()
	{
		super("util.log");
		DecodesInterface.silent = true;
	}
	
	public static void main(String[] args)
		throws Exception
	{
		AlarmDelete alarmDelete = new AlarmDelete();
		alarmDelete.execute(args);
	}

	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(grpNameArg);
	}

	@Override
	protected void runApp()
		throws Exception
	{
		String grpName = grpNameArg.getValue();
		if (grpName == null || grpName.trim().length() == 0)
		{
			System.err.println("Missing required arg -- group name to delete.");
			System.exit(1);
		}
		
		AlarmDAI alarmDAO = new AlarmDAO(TsdbAppTemplate.theDb);
		try
		{
			DbKey alarmId = alarmDAO.groupName2id(grpName);
			if (DbKey.isNull(alarmId))
				System.err.println("No such group named '" + grpName + "'");
			else
				alarmDAO.deleteAlarmGroup(alarmId);
		}
		finally
		{
			alarmDAO.close();
		}
	}
}
