package decodes.tsdb.algo;

import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.CmdLineArgs;
import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;

public class ShowAlgoProps
	extends TsdbAppTemplate
{
	private StringToken algoNameArg = new StringToken("", "Algorithm Class Name", "", 
		TokenOptions.optArgument, null);

	public ShowAlgoProps()
	{
		super(null);
	}

	/**
	 * Overrides to add test-specific arguments.
	 */
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(algoNameArg);
	}

	public static void main(String args[])
		throws Exception
	{
		TsdbAppTemplate tp = new ShowAlgoProps();
		tp.execute(args);
	}

	protected void runApp()
		throws Exception
	{
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		String clsName = algoNameArg.getValue();
		Class cls = cl.loadClass(clsName);
		AW_AlgorithmBase exec = (AW_AlgorithmBase)cls.newInstance();
	
		PropertySpec specs[] = exec.getSupportedProps();
		for(int i=0; i<specs.length; i++)
			System.out.println("name='" + specs[i].getName() + "' "
				+ "desc='" + specs[i].getDescription() + "' "
				+ "type='" + specs[i].getType() + "'");
	}
}
