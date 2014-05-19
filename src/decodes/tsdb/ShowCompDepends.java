/*
 * Open source software by Cove Software, LLC
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
import decodes.sql.DbKey;
import decodes.util.CmdLineArgs;


/**
Troubleshooting utility to print the current CP_COMP_DEPENDS table
in 6 comma-delimited columns the following format:
TS_Key(int)   "TS_ID(Unique String)"  COMPUTATION_ID(int) "comp name" Loading_App_Id(int) "Loading App Name"
*/
public class ShowCompDepends extends TsdbAppTemplate
{
	// Local caches for computations, groups, cp_comp_depends:
	private ArrayList<DbComputation> compList = new ArrayList<DbComputation>();
	private ArrayList<CpCompDependsRecord> cpCompDependsTable = new ArrayList<CpCompDependsRecord>();
	ArrayList<CompAppInfo> loadingApps = new ArrayList<CompAppInfo>();


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
		fillLists();
		
		TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
		for(CpCompDependsRecord ccdr : cpCompDependsTable)
		{
			DbKey tsKey = ccdr.getTsKey();
			TimeSeriesIdentifier tsid = timeSeriesDAO.getTimeSeriesIdentifier(tsKey);
			DbKey compId = ccdr.getCompId();
			DbComputation comp = getComp(compId);
			String compName = "Inaccessible";
			DbKey appId = Constants.undefinedId;
			String appName = "Inaccessible";
			if (comp != null)
			{
				compName = comp.getName();
				appId = comp.getAppId();
				CompAppInfo cai = getApp(appId);
				if (cai != null)
					appName = cai.getAppName();
			}
			
			System.out.println("" + tsKey + ",    "
				+ (tsid == null ? "null" : ("\""+tsid.getUniqueString()+"\"")) + ",   "
				+ compId + ",  " + "\"" + compName + "\"" + ",  "
				+ appId + ",  " + "\"" + appName + "\"");
		}
		timeSeriesDAO.close();
	}
	
	private void fillLists()
	{
		LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO();
		ComputationDAI computationDAO = theDb.makeComputationDAO();
		TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();

		try
		{
			info("filling TSID Cache...");
			timeSeriesDAO.reloadTsIdCache();
			
			info("filling Computation Cache...");
			
			List<String> compNames = loadingAppDao.listComputationsByApplicationId(
				Constants.undefinedId, false);
			for(String nm : compNames)
			{
				try
				{
					DbComputation comp = computationDAO.getComputationByName(nm);
					compList.add(comp);
				}
				catch (NoSuchObjectException ex)
				{
					warning("Computation '" + nm 
						+ "' could not be read: " + ex);
				}
			}
			info("After loading, " + compList.size()
				+ " computations in cache.");

			info("Reloading CP_COMP_DEPENDS Cache...");
			loadCpCompDependsCache();
			loadingApps = loadingAppDao.listComputationApps(false);

		}
		catch (Exception ex)
		{
			String msg = "Error refreshing caches: " + ex;
			Logger.instance().failure(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
		finally
		{
			timeSeriesDAO.close();
			loadingAppDao.close();
			computationDAO.close();
		}
	}
	
	private void warning(String x)
	{
		Logger.instance().warning("ShowCompDepends: " + x);
	}
	private void info(String x)
	{
		Logger.instance().info("ShowCompDepends: " + x);
	}
	private void debug(String x)
	{
		Logger.instance().debug3("ShowCompDepends: " + x);
	}

	/**
	 * Flush the cache and then load all the CP_COMP_DEPENDS records
	 * for my appId.
	 */
	private void loadCpCompDependsCache()
	{
		cpCompDependsTable.clear();
		
		CompDependsDAI compDependsDAO = theDb.makeCompDependsDAO();
	
		
		String tsIdCol = 
			theDb.isHdb() || theDb.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_9 
			? "TS_ID" : "SITE_DATATYPE_ID";
		String q = "SELECT " + tsIdCol + ", COMPUTATION_ID FROM CP_COMP_DEPENDS ORDER BY "
				+ tsIdCol;
		
		try
		{
			ResultSet rs = theDb.doQuery(q);
			while (rs != null && rs.next())
			{
				CpCompDependsRecord rec = new CpCompDependsRecord(
					DbKey.createDbKey(rs, 1), DbKey.createDbKey(rs, 2));
				cpCompDependsTable.add(rec);
			}
		}
		catch (Exception ex)
		{
			warning("Error in query '" + q + "': " + ex);
			return;
		}
	}
	
	private DbComputation getComp(DbKey id)
	{
		for(DbComputation dc : compList)
			if (id.equals(dc.getId()))
				return dc;
		return null;
	}
	
	private CompAppInfo getApp(DbKey id)
	{
		for(CompAppInfo cai : loadingApps)
			if (id.equals(cai.getAppId()))
				return cai;
		return null;
	}

}
