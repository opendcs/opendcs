/*
* $Id$
*
* $Log$
* Revision 1.2  2017/08/22 19:56:39  mmaloney
* Refactor
*
* Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
* OPENDCS 6.0 Initial Checkin
*
* Revision 1.1  2012/10/06 12:21:21  mmaloney
* Created.
*
*
*/
package decodes.tsdb;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import opendcs.dai.ComputationDAI;
import opendcs.dai.LoadingAppDAI;

import lrgs.gui.DecodesInterface;

import ilex.cmdline.*;
import ilex.util.Logger;

import decodes.util.CmdLineArgs;

/**
Disable all computations for a given loading application.
This is used by regression tests so that a single test can be
run at a time.
In HDB a simple SQL statement suffices for this. For CWMS 
it must be done here from Java so that the CP_COMP_DEPENDS
gets updated properly.
This program also clears the cp_comp_tasklist of any outstanding records
for the specified application ID.
Main Command Line Args include:
	-a compprocname (required) no default is provided in the code.
*/
public class DisableComps
	extends TsdbAppTemplate
{
	public DisableComps()
	{
		super("util.log");
		setSilent(true);
	}

	/** For cmdline version, adds argument specifications. */
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		appNameArg.setDefaultValue("");
	}

	protected void runApp()
		throws Exception
	{
		LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO();
		ComputationDAI computationDAO = theDb.makeComputationDAO();
		
		List<String> compNames = loadingAppDao.listComputationsByApplicationId(getAppId(), true);
		loadingAppDao.close();
		for(String compName : compNames)
		{
			DbComputation comp = computationDAO.getComputationByName(compName);
			if (comp != null)
			{
				comp.setEnabled(false);
				computationDAO.writeComputation(comp);
			}
		}
		// Just to make sure ...
		String q = "update cp_computation set enabled = 'N' "
			+ "where loading_application_id = " + getAppId();
		theDb.doModify(q);

		// And just to be thorough ...
		q = "delete from cp_comp_depends where computation_id in ("
			+ "select computation_id from cp_computation where loading_application_id = "
			+ getAppId() + ")";
		theDb.doModify(q);

		// Now delete any stray tasklist entries.
		q = "delete from cp_comp_tasklist where loading_application_id = " + getAppId();
		theDb.doModify(q);
		computationDAO.close();

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
		DisableComps tp = new DisableComps();
		tp.execute(args);
	}
}
