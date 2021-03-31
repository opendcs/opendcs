/*
* $Id$
*
* $Log$
* Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
* OPENDCS 6.0 Initial Checkin
*
* Revision 1.2  2012/08/29 14:20:22  mmaloney
* refactor silent mode.
*
* Revision 1.1  2011/01/13 13:45:59  mmaloney
* Created 'DeleteTriggers' utility.
*
*/
package decodes.tsdb;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.TimeZone;

import lrgs.gui.DecodesInterface;
import opendcs.dai.DaiBase;
import opendcs.dao.DaoBase;
import ilex.cmdline.*;
import ilex.util.Logger;

import decodes.util.CmdLineArgs;

/**
Delete Time Series Data.

Main Command Line Args include:
	-a compprocname -- default = "compproc" triggers are deleted for this app only.

Following the options are any number of data IDs.
*/
public class DeleteTriggers
	extends TsdbAppTemplate
{
	public DeleteTriggers()
	{
		super("util.log");
		setSilent(true);
	}

	/** For cmdline version, adds argument specifications. */
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		appNameArg.setDefaultValue("compproc");
	}

	protected void runApp()
		throws Exception
	{
		String q = "delete from cp_comp_tasklist where loading_application_id = " + getAppId();
		DaiBase dao = new DaoBase(theDb,"DeleteTriggers");
		dao.doModify(q);
		dao.close();
	}


	public void finalize()
	{
		shutdown();
	}

	public void shutdown()
	{
	}

	public static void main(String args[])
		throws Exception
	{
		DeleteTriggers tp = new DeleteTriggers();
		tp.execute(args);
	}
}
