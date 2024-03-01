package decodes.tsdb;

import decodes.util.CmdLineArgs;
import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;

import java.util.ArrayList;
import java.util.List;

public class ImportCompApp extends TsdbAppTemplate
{
    private final boolean createTimeseries;
    private final boolean noOverwrite;
    private final List<String> files;
    public ImportCompApp(boolean createTimeseries, boolean noOverwrite, List<String> files)
    {
        super("compimport.log");
        setSilent(true);
        this.createTimeseries = createTimeseries;
        this.noOverwrite = noOverwrite;
        this.files = files;
    }

    @Override
    protected void runApp() throws Exception
    {
        ImportComp ic = new ImportComp(theDb, createTimeseries, noOverwrite, files);
        ic.runApp();
    }

    public static void main( String[] args )
        throws Exception
    {
        StringToken xmlFileArgs = new StringToken("", "xml-file",
                                                  "",
                                                  TokenOptions.optArgument | TokenOptions.optMultiple|
                                                  TokenOptions.optRequired, "");;

        BooleanToken createTimeSeries = new BooleanToken("C", "create parms as needed",
                                                         "",
                                                         TokenOptions.optSwitch, false);;

        BooleanToken noOverwriteArg = new BooleanToken("o", "Do not overwrite records with matching name.",
                                                       "", TokenOptions.optSwitch, false);
        CmdLineArgs cmdLineArgs = new CmdLineArgs(true, "compimport");
        cmdLineArgs.addToken(createTimeSeries);
        cmdLineArgs.addToken(noOverwriteArg);
        cmdLineArgs.addToken(xmlFileArgs);
        // Call run method directly. For multi threaded executive, we would
        // create a thread and start it.
        List<String> fileNames = new ArrayList<>();
        for (int i = 0; i < xmlFileArgs.NumberOfValues(); i++)
        {
            fileNames.add(xmlFileArgs.getValue(i));
        }
        ImportCompApp app = new ImportCompApp(createTimeSeries.getValue(), noOverwriteArg.getValue(), fileNames);
        app.execute(args);


    }
}
