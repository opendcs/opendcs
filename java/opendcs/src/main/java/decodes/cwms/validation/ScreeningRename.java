package decodes.cwms.validation;

import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.validation.dao.ScreeningDAI;
import decodes.sql.DbKey;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.CmdLineArgs;

public class ScreeningRename extends TsdbAppTemplate
{
	public static final String module = "ScreeningRename";
	
	private StringToken screeningIdArg = new StringToken("", "screening ID",
		"", TokenOptions.optArgument | TokenOptions.optMultiple | TokenOptions.optRequired, "");

	private ScreeningRename()
	{
		super("screening.log");
	}
	
	@Override
	protected void runApp() throws Exception
	{
		if (screeningIdArg.NumberOfValues() != 2)
		{
			System.err.println("Usage: ScreeningRename oldName newName");
			System.exit(1);
		}
		CwmsTimeSeriesDb cwmsTsdb = (CwmsTimeSeriesDb)theDb;
		ScreeningDAI screeningDAO = cwmsTsdb.makeScreeningDAO();
		
		String oldName = screeningIdArg.getValue(0);
		String newName = screeningIdArg.getValue(1);
		screeningDAO.renameScreening(oldName, newName);
	}

	public static void main(String[] args)
		throws Exception
	{
		ScreeningRename me = new ScreeningRename();
		me.execute(args);
	}

	@Override
	public void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(screeningIdArg);
	}

}
