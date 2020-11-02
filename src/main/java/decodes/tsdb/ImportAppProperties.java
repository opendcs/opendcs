/*
 * $Id$
 * 
 * $Log$
 */
package decodes.tsdb;

import java.io.File;
import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.Properties;

import opendcs.dai.LoadingAppDAI;
import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import decodes.util.CmdLineArgs;

/**
 * Imports a properties file into the application's properties.
 * 
 * @author mmaloney Mike Maloney, Cove Software LLC
 */
public class ImportAppProperties extends TsdbAppTemplate
{
	// Required arg -- name of properties file to import.
	private StringToken propFileArg = new StringToken("", "properties-file",
		"", TokenOptions.optArgument | TokenOptions.optRequired, "");
	
	// Set to true to delete any existing props before importing the file.
	private BooleanToken replaceAllArg = new BooleanToken("r", "replace all existing props",
		"", TokenOptions.optSwitch, false);

	/**
	 * Constructor called from main method after parsing arguments.
	 */
	public ImportAppProperties()
	{
		super("util.log");
		setSilent(true);
	}
	
	public void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(propFileArg);
		cmdLineArgs.addToken(replaceAllArg);
	}


	@Override
	protected void runApp()
		throws Exception
	{
		LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO();
		try
		{
			CompAppInfo appInfo = loadingAppDao.getComputationApp(appNameArg.getValue());
			if (replaceAllArg.getValue())
				appInfo.getProperties().clear();
			String fn = propFileArg.getValue();
			File propFile = new File(fn);
			Properties props = new Properties();
			props.load(new FileInputStream(propFile));
			for(Enumeration<?> keysit = props.propertyNames(); keysit.hasMoreElements(); )
			{
				String key = (String)keysit.nextElement();
				appInfo.setProperty(key, props.getProperty(key));
			}
			loadingAppDao.writeComputationApp(appInfo);
		}
		finally
		{
			loadingAppDao.close();
		}
	}

	
	/**
	 * The main method.
	 * @param args command line arguments.
	 */
	public static void main( String[] args )
		throws Exception
	{
		ImportAppProperties app = new ImportAppProperties();
		app.execute(args);
	}

}
