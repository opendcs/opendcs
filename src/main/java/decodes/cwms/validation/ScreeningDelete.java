/**
 * $Id$
 * 
 * Copyright 2015 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * 
 * $Log$
 */
package decodes.cwms.validation;

import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.validation.dao.ScreeningDAI;
import decodes.sql.DbKey;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.CmdLineArgs;

public class ScreeningDelete extends TsdbAppTemplate
{
	public static final String module = "ScreeningDelete";
	
	private BooleanToken yesArg = new BooleanToken("y", "Yes, I'm sure. Don't ask for confirmation.",
		"", TokenOptions.optSwitch, false);
	private StringToken screeningIdArg = new StringToken("", "screening ID",
		"", TokenOptions.optArgument | TokenOptions.optMultiple | TokenOptions.optRequired, "");

	private ScreeningDelete()
	{
		super("screening.log");
	}
	
	@Override
	protected void runApp() throws Exception
	{
		CwmsTimeSeriesDb cwmsTsdb = (CwmsTimeSeriesDb)theDb;
		ScreeningDAI screeningDAO = cwmsTsdb.makeScreeningDAO();
		
		for(int idx = 0; idx < screeningIdArg.NumberOfValues(); idx++)
		{
			String screeningId = screeningIdArg.getValue(idx);
			DbKey key = screeningDAO.getKeyForId(screeningId);
			if (DbKey.isNull(key))
			{
				System.out.println("Screening '" + screeningId + "' does not exist.");
				continue;
			}
			Screening screening = screeningDAO.getByKey(key);
			
			if (!yesArg.getValue())
			{
				System.out.print("Confirm delete of screening '" + screeningId + "' (y/n)? ");
				System.out.flush();
				String a = System.console().readLine();
				if (a == null)
					return;
				else if (!a.trim().toUpperCase().startsWith("Y"))
				{
					System.out.println("Screening '" + screeningId + "' NOT deleted.");
					continue;
				}
			}
			System.out.println("Deleting Screening '" + screeningId + "' and all of its time series associations.");
			screeningDAO.deleteScreening(screening);
		}

	}

	public static void main(String[] args)
		throws Exception
	{
		ScreeningDelete me = new ScreeningDelete();
		me.execute(args);
	}

	@Override
	public void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(screeningIdArg);
		cmdLineArgs.addToken(yesArg);
	}

}
