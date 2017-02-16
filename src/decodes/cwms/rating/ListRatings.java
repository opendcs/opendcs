/*
 * $Id$
 * 
 * $Log$
 * Revision 1.2  2016/09/29 18:54:36  mmaloney
 * CWMS-8979 Allow Database Process Record to override decodes.properties and
 * user.properties setting. Command line arg -Dsettings=appName, where appName is the
 * name of a process record. Properties assigned to the app will override the file(s).
 *
 * 
 * This software was written by Cove Software, LLC ("COVE") under contract 
 * to the United States Government. 
 * 
 * No warranty is provided or implied other than specific contractual terms
 * between COVE and the U.S. Government
 * 
 * Copyright 2016 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
 */
package decodes.cwms.rating;

import java.util.List;

import lrgs.gui.DecodesInterface;

import decodes.cwms.CwmsTimeSeriesDb;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.DecodesException;

public class ListRatings extends TsdbAppTemplate
{

	public ListRatings(String logname)
	{
		super(logname);
	}

	@Override
	protected void runApp()
		throws Exception
	{
		CwmsRatingDao crd = new CwmsRatingDao((CwmsTimeSeriesDb)theDb);
		try
		{
			List<CwmsRatingRef> ratings = crd.listRatings(null);
		}
		finally
		{
			crd.close();
		}
		for(CwmsRatingRef crr : ratings)
			System.out.println(crr.toString());
	}

	public void initDecodes()
		throws DecodesException
	{
		DecodesInterface.silent = true;
		DecodesInterface.initDecodesMinimal(cmdLineArgs.getPropertiesFile());
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
		throws Exception
	{
		ListRatings app = new ListRatings("util.log");
		app.execute(args);
	}

}
