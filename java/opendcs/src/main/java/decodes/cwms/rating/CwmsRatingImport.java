/*
* Copyright 2014 Cove Software, LLC
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.cwms.rating;

import java.io.File;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.gui.DecodesInterface;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.FileUtil;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;
import decodes.cwms.CwmsTimeSeriesDb;

/**
Import ratings from an XML file
*/
public class CwmsRatingImport extends TsdbAppTemplate
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
		CwmsRatingDao crd = new CwmsRatingDao((CwmsTimeSeriesDb)theDb);
		crd.setUseReference(false);
		try
		{
			String oid = officeIdArg.getValue().trim();
			if (oid != null && oid.length() > 0)
				crd.setOfficeId(oid);

			String xml = FileUtil.getFileContents(new File(filenameArg.getValue()));

			crd.importXmlToDatabase(xml);
		}
		catch(Exception ex)
		{
			log.atError().setCause(ex).log("Unable to import rating.");
		}
		finally
		{
			crd.close();
		}
	}

	public static void main(String args[])
		throws Exception
	{
		CwmsRatingImport cdu = new CwmsRatingImport();
		cdu.execute(args);
	}
}
