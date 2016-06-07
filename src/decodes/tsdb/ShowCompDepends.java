/*
 * $Id$
 * 
 * $Log$
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
	// Local caches for computations, groups, cp_comp_depends:
//	private ArrayList<DbComputation> compList = new ArrayList<DbComputation>();
//	private ArrayList<CpCompDependsRecord> cpCompDependsTable = new ArrayList<CpCompDependsRecord>();
//	ArrayList<CompAppInfo> loadingApps = new ArrayList<CompAppInfo>();


//	private StringToken grpNameArg;

	public ShowCompDepends()
	{
		super(null);
	}

	/**
	 * Overrides to add test-specific arguments.
	 */
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
//		grpNameArg = new StringToken("", "Group Name", "", 
//			TokenOptions.optArgument, null);
//		cmdLineArgs.addToken(grpNameArg);
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

//		fillLists();
//		
//		TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
//		for(CpCompDependsRecord ccdr : cpCompDependsTable)
//		{
//			DbKey tsKey = ccdr.getTsKey();
//			TimeSeriesIdentifier tsid = timeSeriesDAO.getTimeSeriesIdentifier(tsKey);
//			DbKey compId = ccdr.getCompId();
//			DbComputation comp = getComp(compId);
//			String compName = "Inaccessible";
//			DbKey appId = Constants.undefinedId;
//			String appName = "Inaccessible";
//			if (comp != null)
//			{
//				compName = comp.getName();
//				appId = comp.getAppId();
//				CompAppInfo cai = getApp(appId);
//				if (cai != null)
//					appName = cai.getAppName();
//			}
//			
//			System.out.println("" + tsKey + ",    "
//				+ (tsid == null ? "null" : ("\""+tsid.getUniqueString()+"\"")) + ",   "
//				+ compId + ",  " + "\"" + compName + "\"" + ",  "
//				+ appId + ",  " + "\"" + appName + "\"");
//		}
//		timeSeriesDAO.close();
	}

	@Override
	public void initDecodes()
		throws DecodesException
	{
		DecodesInterface.initDecodesMinimal(cmdLineArgs.getPropertiesFile());
		Database db = Database.getDb();
		db.siteList.read();
	}

	
	
	
//	private void fillLists()
//	{
//		LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO();
//		ComputationDAI computationDAO = theDb.makeComputationDAO();
//		TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
//
//		try
//		{
//			info("filling TSID Cache...");
//			timeSeriesDAO.reloadTsIdCache();
//			
//			info("filling Computation Cache...");
//			
//			
//			
//			List<String> compNames = loadingAppDao.listComputationsByApplicationId(
//				Constants.undefinedId, false);
//			for(String nm : compNames)
//			{
//				try
//				{
//					DbComputation comp = computationDAO.getComputationByName(nm);
//					compList.add(comp);
//				}
//				catch (NoSuchObjectException ex)
//				{
//					warning("Computation '" + nm 
//						+ "' could not be read: " + ex);
//				}
//			}
//			info("After loading, " + compList.size()
//				+ " computations in cache.");
//
//			info("Reloading CP_COMP_DEPENDS Cache...");
//			loadCpCompDependsCache();
//			loadingApps = loadingAppDao.listComputationApps(false);
//
//		}
//		catch (Exception ex)
//		{
//			String msg = "Error refreshing caches: " + ex;
//			Logger.instance().failure(msg);
//			System.err.println(msg);
//			ex.printStackTrace(System.err);
//		}
//		finally
//		{
//			timeSeriesDAO.close();
//			loadingAppDao.close();
//			computationDAO.close();
//		}
//	}
	
	private void info(String x)
	{
		Logger.instance().info("ShowCompDepends: " + x);
	}
	private void debug(String x)
	{
		Logger.instance().debug3("ShowCompDepends: " + x);
	}

//	/**
//	 * Flush the cache and then load all the CP_COMP_DEPENDS records
//	 * for my appId.
//	 */
//	private void loadCpCompDependsCache()
//	{
//		cpCompDependsTable.clear();
//		
//		CompDependsDAI compDependsDAO = theDb.makeCompDependsDAO();
//	
//		
//		String tsIdCol = 
//			theDb.isHdb() || theDb.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_9 
//			? "TS_ID" : "SITE_DATATYPE_ID";
//		String q = "SELECT " + tsIdCol + ", COMPUTATION_ID FROM CP_COMP_DEPENDS ORDER BY "
//				+ tsIdCol;
//		
//		try
//		{
//			ResultSet rs = theDb.doQuery(q);
//			while (rs != null && rs.next())
//			{
//				CpCompDependsRecord rec = new CpCompDependsRecord(
//					DbKey.createDbKey(rs, 1), DbKey.createDbKey(rs, 2));
//				cpCompDependsTable.add(rec);
//			}
//		}
//		catch (Exception ex)
//		{
//			warning("Error in query '" + q + "': " + ex);
//			return;
//		}
//	}
//	
//	private DbComputation getComp(DbKey id)
//	{
//		for(DbComputation dc : compList)
//			if (id.equals(dc.getId()))
//				return dc;
//		return null;
//	}
//	
//	private CompAppInfo getApp(DbKey id)
//	{
//		for(CompAppInfo cai : loadingApps)
//			if (id.equals(cai.getAppId()))
//				return cai;
//		return null;
//	}

}
