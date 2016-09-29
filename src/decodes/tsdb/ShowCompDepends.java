/*
 * $Id$
 * 
 * $Log$
 * Revision 1.6  2016/09/23 15:56:20  mmaloney
 * Add -f appname argument.
 *
 * Revision 1.5  2016/06/07 22:00:51  mmaloney
 * Refactoring for efficiency, particularly with HDB.
 *
 * Revision 1.4  2014/10/07 13:03:35  mmaloney
 * dev
 *
 * 
 * Copyright 2007 Ilex Engineering, Inc. - All Rights Reserved.
 * No part of this file may be duplicated in either hard-copy or electronic
 * form without specific written permission.
*/
package decodes.tsdb;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import opendcs.dai.CompDependsDAI;
import opendcs.dai.ComputationDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.TimeSeriesDAI;
import lrgs.gui.DecodesInterface;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.Logger;
import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.Site;
import decodes.sql.DbKey;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;


/**
Troubleshooting utility to print the current CP_COMP_DEPENDS table
in 6 comma-delimited columns the following format:
TS_Key(int)   "TS_ID(Unique String)"  COMPUTATION_ID(int) "comp name" Loading_App_Id(int) "Loading App Name"
*/
public class ShowCompDepends extends TsdbAppTemplate
{
	/** Application name - used to determine LOADING_APPLICATION_ID. */
	protected StringToken appFilterArg  = new StringToken("f", "Application-Name (for filter)", "",
		TokenOptions.optSwitch, "");;

	public ShowCompDepends()
	{
		super(null);
	}

	/**
	 * Overrides to add test-specific arguments.
	 */
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(appFilterArg);
	}

	public static void main(String args[])
		throws Exception
	{
		TsdbAppTemplate tp = new ShowCompDepends();
		DecodesInterface.silent = true;
		tp.execute(args);
	}

	protected void runApp()
		throws Exception
	{
		String tables = "CP_COMP_DEPENDS a, CP_COMPUTATION b, HDB_LOADING_APPLICATION c";
		String columns = "a.TS_ID, a.computation_id, b.computation_name, b.loading_application_id, "
			+ " c.loading_application_name";
		String joins = "a.computation_id = b.computation_id"
			+ " and b.loading_application_id = c.loading_application_id";
		
		
		if (theDb.isHdb())
		{
			tables = tables + ", cp_ts_id d, hdb_site_datatype e";
			joins = joins + " and a.ts_id = d.ts_id and d.site_datatype_id = e.site_datatype_id";
			columns = columns + ", e.site_id, e.datatype_id, d.interval, d.table_selector, d.model_id";
		}
		else if (theDb.isCwms())
		{
			tables = tables + ", cwms_v_ts_id d";
			joins = joins + " and a.ts_id = d.ts_code";
			columns = columns + ", d.cwms_ts_id";
		}
		
		if (appFilterArg.getValue().length() > 0)
			joins = joins + " and lower(c.loading_application_name) = '"
				+ appFilterArg.getValue().toLowerCase() + "'";
		
		String q = "select " + columns + " from " + tables + " where " + joins;
		ResultSet rs = theDb.doQuery(q);
		while(rs != null && rs.next())
		{
			DbKey tsKey = DbKey.createDbKey(rs, 1);
			DbKey compId = DbKey.createDbKey(rs, 2);
			String compName = rs.getString(3);
			DbKey appId = DbKey.createDbKey(rs, 4);
			String appName = rs.getString(5);
			
			String tsid = "";
			if (theDb.isHdb())
			{
				DbKey siteId = DbKey.createDbKey(rs, 6);
				Site site = Database.getDb().siteList.getSiteById(siteId);
				String sn = site == null ? "nullsite" : site.getPreferredName().getNameValue();
				DbKey datatypeId = DbKey.createDbKey(rs, 7);
				String interval = rs.getString(8);
				String tabsel = rs.getString(9);
				DbKey modelId = DbKey.createDbKey(rs, 10);
				tsid = sn + "." + datatypeId + "." + interval + "." + tabsel;
				if (modelId != null && modelId.getValue() != -1L)
					tsid = tsid + "." + modelId;
			}
			else if (theDb.isCwms())
				tsid = rs.getString(6);
			
			System.out.println("" + tsKey + ",    "
				+ "\""+tsid+"\"" + ",   "
				+ compId + ",  " + "\"" + compName + "\"" + ",  "
				+ appId + ",  " + "\"" + appName + "\"");
		}

	}

	@Override
	public void initDecodes()
		throws DecodesException
	{
		DecodesInterface.initDecodesMinimal(cmdLineArgs.getPropertiesFile());
		Database db = Database.getDb();
		db.siteList.read();
	}

	public void info(String x)
	{
		Logger.instance().info("ShowCompDepends: " + x);
	}
	private void debug(String x)
	{
		Logger.instance().debug3("ShowCompDepends: " + x);
	}
}
