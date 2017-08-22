/*
 * $Id$
 * 
 * $Log$
 * 
 * Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
*/
package decodes.tsdb;

import java.sql.ResultSet;
import ilex.cmdline.TokenOptions;
import ilex.cmdline.StringToken;
import decodes.db.Database;
import decodes.db.Site;
import decodes.sql.DbKey;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;
import lrgs.gui.DecodesInterface;


/**
Print list of computation dependencies.
*/
public class CompDependsList 
	extends TsdbAppTemplate
{
	/** Application name - used to determine LOADING_APPLICATION_ID. */
	protected StringToken appFilterArg  = new StringToken("f", "Application-Name (for filter)", "",
		TokenOptions.optSwitch, "");;

	/** No args ctor */
	public CompDependsList()
	{
		super("util.log");
		DecodesInterface.silent = true;
	}

	public static void main(String args[])
		throws Exception
	{
		new CompDependsList().execute(args);
	}

	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(appFilterArg);
	}

	@Override
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

}
