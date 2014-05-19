/*
* $Id$
*
* $Log$
* Revision 1.2  2012/11/06 21:12:53  mmaloney
* Minimal, silent decodes init.
*
* Revision 1.1  2012/11/06 20:41:53  mmaloney
* Created.
*
*
* Open source software 
*/
package decodes.cwms.rating;

import java.util.List;

import lrgs.gui.DecodesInterface;
import ilex.cmdline.*;
import ilex.util.Logger;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;
import decodes.cwms.CwmsTimeSeriesDb;

/**
Export a rating to stdout
*/
public class CwmsRatingExport
	extends TsdbAppTemplate
{
	private StringToken officeIdArg = null;
	private StringToken locationIdsArg = null;
	
	public CwmsRatingExport()
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
		locationIdsArg = new StringToken("", "Location IDs", "", 
			TokenOptions.optArgument|TokenOptions.optRequired, "");
		cmdLineArgs.addToken(locationIdsArg);
		
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
			
			List<CwmsRatingRef> crrl = crd.listRatings(locationIdsArg.getValue());
			for(CwmsRatingRef crr : crrl)
				System.out.println(crd.toXmlString(crr, true));
		} 
		catch(Exception ex)
		{
			Logger.instance().failure(ex.toString());
			System.err.println(ex.toString());
			ex.printStackTrace(System.err);
		}
	}
	
	public static void main(String args[])
		throws Exception
	{
		CwmsRatingExport cdu = new CwmsRatingExport();
		cdu.execute(args);
	}
}
