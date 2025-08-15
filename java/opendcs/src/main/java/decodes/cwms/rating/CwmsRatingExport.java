/*
 * This software was written by Cove Software, LLC ("COVE") under contract 
 * to the United States Government. 
 * 
 * No warranty is provided or implied other than specific contractual terms
 * between COVE and the U.S. Government
 * 
 * U.S. Army Corps of Engineers, Hydrologic Engineering Center.
*/
package decodes.cwms.rating;

import java.util.List;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.gui.DecodesInterface;
import ilex.cmdline.*;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;
import decodes.cwms.CwmsTimeSeriesDb;

/**
Export a rating to stdout
*/
public class CwmsRatingExport extends TsdbAppTemplate
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
		try (CwmsRatingDao crd = new CwmsRatingDao((CwmsTimeSeriesDb)theDb))
		{
			crd.setUseReference(false);
			String oid = officeIdArg.getValue().trim();
			if (oid != null && oid.length() > 0)
				crd.setOfficeId(oid);
			
			List<CwmsRatingRef> crrl = crd.listRatings(locationIdsArg.getValue());
			for(CwmsRatingRef crr : crrl)
				System.out.println(crd.toXmlString(crr, true));
		} 
		catch(Exception ex)
		{
			log.atError().setCause(ex).log("Unable to extract ratings.");
		}
	}
	
	public static void main(String args[])
		throws Exception
	{
		CwmsRatingExport cdu = new CwmsRatingExport();
		cdu.execute(args);
	}
}
