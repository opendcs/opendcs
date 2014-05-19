/*
* $Id$
*
* $Log$
* Revision 1.4  2012/11/09 16:22:13  mmaloney
* Rating GUI Delete Feature
* Rating Import Merge Feature
*
* Revision 1.3  2012/11/08 21:46:23  mmaloney
* Print 'cause' when Rating API throws exception.
*
* Revision 1.2  2012/11/06 21:12:53  mmaloney
* Minimal, silent decodes init.
*
* Revision 1.1  2012/11/06 20:40:38  mmaloney
* Created.
*
*
* Open source software 
*/
package decodes.cwms.rating;

import java.io.File;

import lrgs.gui.DecodesInterface;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.FileUtil;
import ilex.util.Logger;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;
import decodes.cwms.CwmsTimeSeriesDb;

/**
Import ratings from an XML file
*/
public class CwmsRatingImport
	extends TsdbAppTemplate
{
	private StringToken officeIdArg = null;
	private StringToken filenameArg = null;
	
	public CwmsRatingImport()
	{
		super("util.log");
		setSilent(true);
	}

	/** For cmdline version, adds argument specifications. */
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		officeIdArg = new StringToken("O", "CWMS office ID", "", 
				TokenOptions.optSwitch, "");
		cmdLineArgs.addToken(officeIdArg);
		filenameArg = new StringToken("", "XML Rating File", "", 
			TokenOptions.optArgument|TokenOptions.optRequired, "");
		cmdLineArgs.addToken(filenameArg);
	}

	public void initDecodes()
		throws DecodesException
	{
		DecodesInterface.silent = true;
		DecodesInterface.initDecodesMinimal(cmdLineArgs.getPropertiesFile());
	}

	protected void runApp()
		throws Exception
	{
		try
		{
			CwmsRatingDao crd = new CwmsRatingDao((CwmsTimeSeriesDb)theDb);
			String oid = officeIdArg.getValue().trim();
			if (oid != null && oid.length() > 0)
				crd.setOfficeId(oid);
			
			String xml = FileUtil.getFileContents(new File(filenameArg.getValue()));
			
			crd.importXmlToDatabase(xml);
		} 
		catch(Exception ex)
		{
			Logger.instance().failure(ex.toString());
			System.err.println(ex.toString());
			ex.printStackTrace(System.err);
//			Throwable cause = ex.getCause();
//			if (cause != null)
//			{
//				Logger.instance().failure("Cause: " + cause);
//				System.err.println("Cause: " + cause);
//				cause.printStackTrace(System.err);
//			}
		}
	}
	
	public static void main(String args[])
		throws Exception
	{
		CwmsRatingImport cdu = new CwmsRatingImport();
		cdu.execute(args);
	}
}
