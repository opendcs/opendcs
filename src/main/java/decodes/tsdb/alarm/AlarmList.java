/*
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
 * Revision 1.2  2017/05/17 20:36:25  mmaloney
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
import ilex.util.Logger;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TsdbAppTemplate;

/**
 * Lists names of alarm groups in the database.
 * @author mmaloney
 *
 */
public class AlarmList
	extends TsdbAppTemplate
{
	public AlarmList()
	{
		super("util.log");
		DecodesInterface.silent = true;
	}
	
	public static void main(String[] args)
		throws Exception
	{
		AlarmList alarmList = new AlarmList();
		alarmList.execute(args);
	}

//	@Override
//	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
//	{
//	}


	@Override
	protected void runApp() 
	{
		AlarmDAI alarmDAO = new AlarmDAO(TsdbAppTemplate.theDb);
		AlarmConfig cfg = new AlarmConfig();
		
		try
		{
			alarmDAO.check(cfg);
			for(AlarmGroup grp : cfg.getGroups())
				System.out.println(grp.getName());
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
