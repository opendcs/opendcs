package decodes.tsdb;

import decodes.util.CmdLineArgs;
import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;

import java.util.ArrayList;
import java.util.List;

public class ImportCompApp extends TsdbAppTemplate
{
    StringToken xmlFileArgs = new StringToken("", "xml-file",
                                                  "",
                                                  TokenOptions.optArgument | TokenOptions.optMultiple|
                                                  TokenOptions.optRequired, "");;

    BooleanToken createTimeSeriesArg = new BooleanToken("C", "create parms as needed",
                                                        "",
                                                        TokenOptions.optSwitch, false);;

    BooleanToken noOverwriteArg = new BooleanToken("o", "Do not overwrite records with matching name.",
                                                       "", TokenOptions.optSwitch, false);

    public ImportCompApp()
    {
        super("compimport.log");
        setSilent(true);
    }

    @Override
    protected void runApp() throws Exception
    {
        List<String> fileNames = new ArrayList<>();
        for (int i = 0; i < xmlFileArgs.NumberOfValues(); i++)
        {
            fileNames.add(xmlFileArgs.getValue(i));
        }
        ImportComp ic = new ImportComp(theDb, createTimeSeriesArg.getValue(),
                                        noOverwriteArg.getValue(), fileNames);
        ic.runApp();
    }

    @Override
    public void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(createTimeSeriesArg);
		cmdLineArgs.addToken(noOverwriteArg);
		cmdLineArgs.addToken(xmlFileArgs);
	}

    public static void main( String[] args )
        throws Exception
    {
        ImportCompApp app = new ImportCompApp();
        app.execute(args);
    }
}
