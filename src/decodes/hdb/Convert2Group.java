/*
*  $Id$
*
*  This is open-source software written by Cove Software LLC under
*  contract to Sutron and the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  This source code is provided completely without warranty.
*  
*  $Log$
*  Revision 1.4  2016/11/03 19:01:21  mmaloney
*  Refactoring for group evaluation to make HDB work the same way as CWMS.
*
*  Revision 1.3  2016/09/29 18:54:35  mmaloney
*  CWMS-8979 Allow Database Process Record to override decodes.properties and
*  user.properties setting. Command line arg -Dsettings=appName, where appName is the
*  name of a process record. Properties assigned to the app will override the file(s).
*
*  Revision 1.2  2014/08/22 17:23:09  mmaloney
*  6.1 Schema Mods and Initial DCP Monitor Implementation
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.16  2013/03/21 18:27:40  mmaloney
*  DbKey Implementation
*
*  Revision 1.15  2012/09/07 19:59:04  mmaloney
*  Bug fix, resolve first input then use THAT tsid to resolve outputs.
*
*  Revision 1.14  2012/08/27 18:04:23  mmaloney
*  If a matching single comp is disabled, it must be excluded from execution.
*
*  Revision 1.13  2012/08/22 19:27:58  mmaloney
*  Improvements to report format.
*
*  Revision 1.12  2012/08/22 13:31:33  mmaloney
*  Improvements to report format.
*
*  Revision 1.11  2012/08/22 13:19:02  mmaloney
*  Improvements to report format.
*
*  Revision 1.10  2012/08/22 13:01:46  mmaloney
*  Added report feature.
*
*  Revision 1.9  2012/08/21 22:05:49  mmaloney
*  dev
*
*  Revision 1.8  2012/08/21 22:01:43  mmaloney
*  dev
*
*  Revision 1.7  2012/08/21 12:48:32  mmaloney
*  dev
*
*  Revision 1.6  2012/08/21 12:30:30  mmaloney
*  dev
*
*  Revision 1.5  2012/08/21 12:29:40  mmaloney
*  dev
*
*  Revision 1.4  2012/08/20 20:27:28  mmaloney
*  dev
*
*  Revision 1.3  2012/08/20 20:23:50  mmaloney
*  dev
*
*  Revision 1.2  2012/08/20 20:20:34  mmaloney
*  dev
*
*  Revision 1.1  2012/08/20 20:09:12  mmaloney
*  Implement HDB Convert2Group utility.
*
*/
package decodes.hdb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import opendcs.dai.ComputationDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;
import lrgs.gui.DecodesInterface;
import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.TextUtil;
import decodes.sql.DbKey;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CompMetaData;
import decodes.tsdb.ConstraintException;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbComputation;
import decodes.tsdb.DbIoException;
import decodes.tsdb.GroupHelper;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;
import decodes.tsdb.TsdbAppTemplate;
import decodes.tsdb.algo.AW_AlgorithmBase;
import decodes.tsdb.xml.CompXio;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;
import decodes.db.Constants;


/**
This is the main class for the utility that converts non-group computations
to group computations.
*/
public class Convert2Group
	extends TsdbAppTemplate
{
	// Local caches for computations, groups, cp_comp_depends:
	private ArrayList<DbComputation> compCache = new ArrayList<DbComputation>();
	private GroupHelper groupHelper = null;
	private BooleanToken testMode = new BooleanToken("T", "Report Only - no DB changes.",
		"", TokenOptions.optSwitch, false);
	private StringToken disposeOption = new StringToken("X", "'delete' or 'disable'",
		"", TokenOptions.optSwitch, "disable");
	private boolean deleteSingle = false; // set to true by -Ddelete
	private StringToken disposedSingleArg = new StringToken("S", "Filename to save disposed single comps",
		"", TokenOptions.optSwitch, "disposed-comps.xml");
	private StringToken reportFileArg = new StringToken("R", "Report File Output",
		"", TokenOptions.optSwitch, "convert2group-report.txt");
	private StringToken compIdsArg = new StringToken("", "Computation-IDs",
		"", TokenOptions.optArgument | TokenOptions.optRequired, "");
	private File disposedSingleFile = null;
	
	private String compName = "";
	private PrintStream reportPS = null;
	private int reportIndent = 0;

	/**
	 * Constructor called from main method after parsing arguments.
	 */
	public Convert2Group()
	{
		super("convert2group.log");
	}

	/** Sets default app name (and log file) to compdepends */
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		appNameArg.setDefaultValue("compedit");
		cmdLineArgs.addToken(testMode);
		cmdLineArgs.addToken(disposeOption);
		cmdLineArgs.addToken(disposedSingleArg);
		cmdLineArgs.addToken(reportFileArg);
		cmdLineArgs.addToken(compIdsArg);
	}

	/**
	 * The application run method. Called after all initialization methods
	 * by the base class.
	 * @throws LockBusyException if another process has the lock
	 * @throws DbIoException on failure to access the database
	 * @throws NoSuchObjectException if the application is invalid.
	 */
	public void runApp( )
		throws DbIoException
	{
		if (!theDb.isHdb())
		{
			System.err.println("This utility is for HDB only. Cannot run on other database types.");
			System.exit(1);
		}
		
		String reportName = EnvExpander.expand(reportFileArg.getValue());
		try
		{
			reportPS = new PrintStream(new File(reportName));
		}
		catch (FileNotFoundException ex)
		{
			System.err.println("Cannot open report file '" + reportName + "': " + ex);
			System.exit(1);
		}
		
		report("============== Convert2Group Starting " + (new Date())
			+ " =============");
		
		groupHelper = theDb.makeGroupHelper();
		groupHelper.setGroupCacheDumpDir(null); // Do not dump groups tofile.

		String s = disposeOption.getValue();
		if (s.equalsIgnoreCase("delete"))
		{
			deleteSingle = true;
		}
		report("Single computations that are converted to group will be "
			+ (deleteSingle ? "deleted." : "disabled."));
		
		s = disposedSingleArg.getValue();
		if (s != null && s.length() > 0)
		{
			String exp = EnvExpander.expand(s);
			disposedSingleFile = new File(exp);
		}
		report("Single computations that have been thusly disposed will be saved in "
			+ disposedSingleFile.getPath());

		refreshCaches();
		for(int argIdx = 0; argIdx < compIdsArg.NumberOfValues(); argIdx++)
		{
			String arg = compIdsArg.getValue(argIdx);
			DbKey compId = Constants.undefinedId;
			try { compId = DbKey.createDbKey(Long.parseLong(arg)); }
			catch(NumberFormatException ex)
			{
				warning("Bad compID[" + argIdx + "] '" + arg + "'. Must be integer. Skipped.");
				continue;
			}
			processComp(compId);
		}
		try { reportPS.close(); } catch(Exception ex) {}
	}

	public void initDecodes()
		throws DecodesException
	{
		DecodesInterface.silent = true;
		if (DecodesInterface.isInitialized())
			return;
		DecodesInterface.initDecodesMinimal(cmdLineArgs.getPropertiesFile());
	}

	/**
	 * The main method.
	 * @param args command line arguments.
	 */
	public static void main( String[] args )
		throws Exception
	{
		Convert2Group app = new Convert2Group();
		app.execute(args);
	}

	public void warning(String msg)
	{
		Logger.instance().warning(compName + " " + msg);
		for(int i=0; i<reportIndent; i++)
			reportPS.print("    ");
		reportPS.println(msg);
	}
	private void report(String msg)
	{
		if (msg.length() > 0)
			info(msg);
		for(int i=0; i<reportIndent; i++)
			reportPS.print("    ");
		reportPS.println(msg);
	}
	public void info(String x)
	{
		Logger.instance().info(compName + " " + x);
	}
	private void debug(String x)
	{
		Logger.instance().debug3(compName + " " + x);
	}

	private void refreshCaches()
	{
		LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO();
		ComputationDAI computationDAO = theDb.makeComputationDAO();
		TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
		TsGroupDAI tsGroupDAO = theDb.makeTsGroupDAO();
		try
		{
			info("Refreshing TSID Cache...");
			timeSeriesDAO.reloadTsIdCache();
			
			info("Refreshing Computation Cache...");
			compCache.clear();
			
			// Note: This daemon processes comps for a mix of app IDs.
			List<String> compNames = loadingAppDao.listComputationsByApplicationId(
				Constants.undefinedId, false);
			for(String nm : compNames)
			{
				try
				{
					DbComputation comp = computationDAO.getComputationByName(nm);
					expandComputationInputs(comp);
					compCache.add(comp);
				}
				catch (NoSuchObjectException ex)
				{
					warning("Computation '" + nm 
						+ "' could not be read: " + ex);
				}
			}
			info("After loading, " + compCache.size()
				+ " computations in cache.");

			info("Refreshing Group Cache...");
			String q = "SELECT GROUP_ID FROM TSDB_GROUP";
			ArrayList<DbKey> grpIds = new ArrayList<DbKey>();
			ResultSet rs = theDb.doQuery(q);
			while(rs != null && rs.next())
				grpIds.add(DbKey.createDbKey(rs, 1));
				
			tsGroupDAO.fillCache();

			info("Expanding Groups in Cache...");
			groupHelper.evalAll();
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
			tsGroupDAO.close();
			timeSeriesDAO.close();
			loadingAppDao.close();
			computationDAO.close();
		}
	}
	

	/**
	 * Processes a group Computation from the command line.
	 * @param compId the computation ID
	 */
	private void processComp(DbKey compId)
		throws DbIoException
	{
		ComputationDAI computationDAO = theDb.makeComputationDAO();
		TsGroupDAI tsGroupDAO = theDb.makeTsGroupDAO();
		
		// Fetch the computation from my cache.
		DbComputation groupComp = getCompFromCache(compId);
		if (groupComp == null)
		{
			warning("No computation with ID " + compId + " -- skipped.");
			return;
		}
		compName = "Computation-" + compId + " (" + groupComp.getName() + ")";
		report("");
		report("Processing " + compName);
		report("");
		reportIndent = 1;
		
		// If not a group comp, issue warning and skip.
		if (groupComp.getGroupId() == Constants.undefinedId)
		{
			report(compName + " is not a group computation -- skipped.");
			return;
		}

		// Get list of TSIDs from the expanded group for this comp.
		// Use same code in resolver to create a list of concrete clones of this comp.
		ArrayList<DbComputation> clones = makeConcreteClones(groupComp);
		if (clones == null)
			return;
		report("When expanded by group, there are " + clones.size() + " computations.");
		if (clones.size() == 0)
			return;
		report("Looking for single computations that match the expanded group computation.");

		ArrayList<DbComputation> toDispose = new ArrayList<DbComputation>();
		ArrayList<TimeSeriesIdentifier> mustExclude = new ArrayList<TimeSeriesIdentifier>();
		reportIndent++;
		for(DbComputation concreteComp : clones)
		{
			// find matching single (non-group) computations.
			// use matchesInputsAndAlgo - this means the comp WILL be triggered
			for(DbComputation singleComp : compCache)
			{
				if (singleComp.getGroupId() != Constants.undefinedId)
					continue;
				if (matchesParmsAndAlgo(concreteComp, singleComp))
				{
					report("Comp-" + singleComp.getId() + " (" + singleComp.getName() 
						+ ") has matching algorithm and parameters.");
					reportIndent++;
					if (matchesProps(concreteComp, singleComp)
					 && singleComp.isEnabled())
					{
						// The clone from the group matches this single
						// computation. Put it in the list to be disposed.
						toDispose.add(singleComp);
						report("Properties also match. This computation will be disposed.");
					}
					else
					{
						// The clone from the group does not match the single
						// computation. To prevent it from being triggered. Add an
						// entry to the exclude list for the TS that triggered this
						// instance of the group comp.
						mustExclude.add(concreteComp.triggeringTsid);
						report("Properties do NOT match. ");
						report ("This single computation will remain and will be excluded "
							+ "from the group.");
					}
					reportIndent--;
				}
			}
		}
		reportIndent--;
		
		if (toDispose.size() == 0)
		{
			report("Group computation matched no single computation. No changes will be made.");
			return;
		}
		
		// 'mustExclude' contains TSIDs that ARE members of the group but must be prevented
		// from triggering this group computation. To do this, we have to create an exclusion
		// group with these TSID, create a copy of the original group just for this computation,
		// and then exclude the exclusion group.
		if (mustExclude.size() > 0)
		{
			// Create a separate subgroup containing the mustExclude TSIDs "comp-" + compId + "-excluded"
			TsGroup toExclude = new TsGroup();
			toExclude.setGroupName("comp-" + compId + "-excluded");
			toExclude.setGroupType("comp-select");
			String desc = "These Time Series Identifiers are excluded from the execution of "
				+ "computation(" + compId + ") " + groupComp.getName();
			toExclude.setDescription(desc);
			report("Creating exclusion group: " + toExclude.getGroupName());
			report("These time series identifiers will be excluded:");
			reportIndent++;
			for(TimeSeriesIdentifier tsid : mustExclude)
			{
				report(tsid.getUniqueString());
				toExclude.addTsMember(tsid);
			}
			reportIndent--;
			if (!testMode.getValue())
				theDb.writeTsGroup(toExclude);
			report("");
			
			// Create a new TsGroup just for this computation "comp-" + compId + "-group"
			TsGroup compGroup = new TsGroup();
			compGroup.setGroupName("comp-" + compId + "-group");
			compGroup.setGroupType("comp-select");
			compGroup.setDescription("Special group for " + compName);
			compGroup.addSubGroup(tsGroupDAO.getTsGroupById(groupComp.getGroupId()), 'A');	
			compGroup.addSubGroup(toExclude, 'S');
			report("Creating new group '" + compGroup.getGroupName() + "' just for this computation.");
			report("The new group will include the original group and exclude the above TSIDs");
			if (!testMode.getValue())
				theDb.writeTsGroup(compGroup);
			
			// Change the group computation to use the new group.
			groupComp.setGroup(compGroup);
			report("Comp-" + compId + " will be modified with group assignment set to '" 
				+ compGroup.getGroupName() + "'");
		}

		// Create an XML file with all the single comps that are going to be disposed.
		try
		{
			saveDisposedCompsAndAlgos(toDispose);
		}
		catch (IOException ex)
		{
			warning("Cannot save '" + disposedSingleFile.getPath() + "': " + ex);
			warning("Aborting!");
			return;
		}
		
		report("");
		report((deleteSingle ? "Deleting " : "Disabling ") + "the following computations:");
		reportIndent++;
		for(DbComputation disposeComp : toDispose)
		{
			String id = "Computation(" + disposeComp.getId() + ") '" + disposeComp.getName() + "'";
			report(id);
	
			if (!testMode.getValue())
			{
				if (deleteSingle)
				{
					// attempt to delete the computation
					try
					{
						if (!testMode.getValue())
							computationDAO.deleteComputation(disposeComp.getId());
						continue;
					}
					catch (ConstraintException ex)
					{
						warning("Cannot delete " + id + ": " + ex + " -- will disable.");
						// fall through
					}
				}
			
				// fell through, either !deleteSingle or unable to delete.
				// Mark the computation as disabled and save it.
				if (!testMode.getValue())
				{
					disposeComp.setEnabled(false);
					computationDAO.writeComputation(disposeComp);
				}
			}
		}
		reportIndent--;
		
		// Enable the group (if not testmode)
		report("Enabling " + compName);
		groupComp.setEnabled(true);
		if (!testMode.getValue())
			computationDAO.writeComputation(groupComp);
		
		tsGroupDAO.close();
		computationDAO.close();
	}

	/**
	 * Called with a group computation, attempt to make a concrete (non-group)
	 * computation by applying each group member to the input parms.
	 * If all input parms exist then this computation could be executed.
	 * Add it to the return list.
	 * @return list of concrete computations expanded from the group.
	 */
	private ArrayList<DbComputation> makeConcreteClones(DbComputation groupComp)
		throws DbIoException
	{
		info("Expanding inputs from group.");
		ArrayList<DbComputation> ret = new ArrayList<DbComputation>();
		
		// If not a group comp just add the completely-specified parms.
		TsGroupDAI tsGroupDAO = theDb.makeTsGroupDAO();
		TsGroup grp = tsGroupDAO.getTsGroupById(groupComp.getGroupId());
		tsGroupDAO.close();
		if (grp == null)
		{
			warning("Invalid group ID + " + groupComp.getGroupId() + ": no matching group -- skipped.");
			return null;
		}
		report("Computation IS a group computation.");
		report("Group-" + grp.getGroupId() + " (" + grp.getGroupName() + ") has "
			+ grp.getExpandedList().size() + " members.");

		// For each time series in the expanded list
		for(TimeSeriesIdentifier tsid : grp.getExpandedList())
		{
			DbComputation tcomp = makeConcreteClone(groupComp, tsid);
			if (tcomp == null)
				continue;
			addIfNotAlreadyPresent(ret, tcomp);
		}
		return ret;
	}
	
	private DbComputation makeConcreteClone(DbComputation comp, TimeSeriesIdentifier tsid)
		throws DbIoException
	{
		DbComputation tcomp = comp.copyNoId();
		tcomp.setId(comp.getId());

		info("Checking ts=" + tsid.getUniqueString());
		
		TimeSeriesIdentifier firstInputTsid = null;

		// Process each input parm first to determine if this TS is a potential trigger.
		for(Iterator<DbCompParm> parmit = tcomp.getParms(); parmit.hasNext(); )
		{
			DbCompParm parm = parmit.next();
			if (!parm.isInput())
				continue;

			// Transform the group TSID by the parm
			info("Checking input parm " + parm.getRoleName()
				+ " sdi=" + parm.getSiteDataTypeId() + " intv=" + parm.getInterval()
				+ " tabsel=" + parm.getTableSelector() + " modelId=" + parm.getModelId()
				+ " dt=" + parm.getDataType() + " siteId=" + parm.getSiteId()
				+ " siteName=" + parm.getSiteName());
			
			try
			{
				TimeSeriesIdentifier ttsid = theDb.transformTsidByCompParm(
					tsid,  // TSID from group (transform will not modify it.
					parm,  // The input parm.
					false, // do NOT create TS if it doesn't exist
					true,  // YES fill in the identifying fields in parm
					null); // ts display name (not important here)
				if (firstInputTsid == null)
					firstInputTsid = ttsid;
			}
			catch(BadTimeSeriesException ex)
			{ /* Won't happen because createTS flag is false */ }
			catch(NoSuchObjectException ex)
			{
				// The TS, after transformation by the parm, doesn't exist in the DB.
				// Therefore it is not a candidate for this param.
				debug("TS " + tsid.getUniqueString() +" not a candidate for comp.");
				return null;
			}
		}
		if (firstInputTsid == null)
		{
			warning("No input parms!");
			return null;
		}
		
		// All input params are transformed and they all exist in the DB.
		// Now transform the output params from the first input.
		for(Iterator<DbCompParm> parmit = tcomp.getParms(); parmit.hasNext(); )
		{
			DbCompParm parm = parmit.next();
			if (!parm.isOutput())
				continue;

			info("Checking output parm " + parm.getRoleName()
				+ " sdi=" + parm.getSiteDataTypeId() + " intv=" + parm.getInterval()
				+ " tabsel=" + parm.getTableSelector() + " modelId=" + parm.getModelId()
				+ " dt=" + parm.getDataType() + " siteId=" + parm.getSiteId()
				+ " siteName=" + parm.getSiteName());
			
			try
			{
				theDb.transformTsidByCompParm(
					firstInputTsid,  // TSID as modified above.
					parm,  // The output parm.
					true,  // YES, DO create TS if it doesn't exist
					true,  // YES fill in the identifying fields in parm
					null); // ts display name (not important here)
			}
			catch(BadTimeSeriesException ex)
			{ 
				warning("Cannot create output time series: " + ex);
				return null;
			}
			catch(NoSuchObjectException ex)
			{ /* Won't happen because createTS flag is true */ }
		}
		
		tcomp.triggeringTsid = tsid;
		info("Successfully created concrete clone of group comp.");
		return tcomp;
	}

	private DbCompParm getFirstInput(DbComputation comp)
	{
		for(Iterator<DbCompParm> parmit = comp.getParms(); parmit.hasNext();)
		{
			DbCompParm dcp = parmit.next();
			if (dcp.isInput() && !dcp.getSiteDataTypeId().isNull())
				return dcp;
		}
		// Shouldn't happen, the parm will have at least one input defined.
		return null;
	}
	
	/**
	 * Ensure that only one copy of a given cloned computation exists in
	 * each evaluation. Example: Adding A and B; both A and B are present in the
	 * input data. Clone will be created when we evaluate A. We don't want to
	 * create a separate clone when we evaluate B.
	 * This method adds the computation to the result set only if it is not
	 * already present
	 * @param results the result set
	 * @param comp the concrete cloned computation
	 * @return the computation in the list if exact match is found
	 */
	private void 
		addIfNotAlreadyPresent(ArrayList<DbComputation> results, DbComputation comp)
	{
		DbCompParm parm2Test = getFirstInput(comp);
		if (parm2Test == null)
			return; // Shouldn't happen. Every algo has at least one input.
		String role = parm2Test.getRoleName();

		for(DbComputation inList : results)
		{
			DbCompParm firstParmInList = inList.getParm(role);
			if (firstParmInList == null)
				return; // Shouldn't happen. Same algo will have same roles.
			
			// If the 1st parms in both comps are equal, then they are the same.
			if (firstParmInList.equals(parm2Test))
			{
				debug("Resolver: Duplicate comp in cycle: "
					+ comp.getName() + ", 1st input: " + firstParmInList.getSiteDataTypeId());
				return;
			}
		}
		results.add(comp);
	}


	/**
	 * Compare the algorithm and time-series parameters for the passed computations
	 * @param grpComp the computation as derived from the group
	 * @param singleComp the single (non-group) computation
	 * @return tru if the parameters and algorithm are the same, false otherwise.
	 */
	private boolean matchesParmsAndAlgo(DbComputation grpComp, DbComputation singleComp)
	{
		debug("matchesParmsAndAlgo Comparing comps " + grpComp.getId() + " and " + singleComp.getId());
		if (grpComp.getAlgorithmId() != singleComp.getAlgorithmId())
			return false;
		debug("    algos are the same");
		
		ArrayList<DbCompParm> grpParms = grpComp.getParmList();
		ArrayList<DbCompParm> singleParms = singleComp.getParmList();
		if (grpParms.size() != singleParms.size())
			return false;
		debug("    same number of parms");
		
		// For every grpParm there must be a matching singleParm
	nextGrpParm:
		for(DbCompParm grpParm : grpParms)
		{
			for(DbCompParm singleParm : singleParms)
			{
				debug("   comparing grp:" + grpParm + ", single:" + singleParm);
				if (grpParm.equals(singleParm))
				{
					debug("    parm '" + grpParm.getRoleName() + "' is the same.");
					continue nextGrpParm;
				}
				else debug("Not EQUAL");
			}
			// Fell through. There is no match for this grpParm.
			return false;
		}
		debug("    matchesParmsAndAlgo returning true!");
		return true;
	}
	
	/**
	 * Compare the fully-evaluated properties used for the passed computations.
	 * @param grpComp the computation as derived from the group
	 * @param singleComp the single (non-group) computation
	 * @return true if they are the same, false otherwise.
	 */
	private boolean matchesProps(DbComputation grpComp, DbComputation singleComp)
		throws DbIoException
	{
		String[] builtInPropNames = { "aggUpperBoundClosed", "aggLowerBoundClosed",
			"aggregateTimeZone", "noAggregateFill", "aggPeriodInterval",
			"interpDeltas", "maxInterpIntervals" };
		
		try
		{
			DbAlgorithmExecutive grpExec = grpComp.getExecutive();
			if (grpExec == null)
			{
				grpComp.prepareForExec(theDb);
				grpExec = grpComp.getExecutive();
			}
			if (!(grpExec instanceof AW_AlgorithmBase))
			{
				warning(grpExec.getClass().getName() + " is not a subclass of AW_AlgorithmBase");
				return false;
			}
			AW_AlgorithmBase grpBase = (AW_AlgorithmBase)grpExec;
			
			DbAlgorithmExecutive singleExec = singleComp.getExecutive();
			if (singleExec == null)
			{
				singleComp.prepareForExec(theDb);
				singleExec = singleComp.getExecutive();
			}
			if (!(singleExec instanceof AW_AlgorithmBase))
			{
				warning(singleExec.getClass().getName() + " is not a subclass of AW_AlgorithmBase");
				return false;
			}
			AW_AlgorithmBase singleBase = (AW_AlgorithmBase)singleExec;
			
			String propNames[] = grpBase.getPropertyNames();
			for(String propName : propNames)
				if (!TextUtil.strEqualIgnoreCase(grpBase.getEvaluatedProperty(propName),
					singleBase.getEvaluatedProperty(propName)))
					return false;
			for(String propName : builtInPropNames)
				if (!TextUtil.strEqualIgnoreCase(grpBase.getEvaluatedProperty(propName),
					singleBase.getEvaluatedProperty(propName)))
					return false;

			// All tests above pass.
			return true;
		}
		catch (DbCompException ex)
		{
			warning("Cannot initialize computation: " + ex);
			return false;
		}
		catch (NoSuchObjectException ex)
		{
			warning("Comp references unknown algorithm: " + ex);
			return false;
		}
	}

	/**
	 * @return computation from cache or null if no matching compId.
	 */
	private DbComputation getCompFromCache(DbKey compId)
	{
		for(DbComputation comp : compCache)
			if (compId.equals(comp.getId()))
				return comp;
		return null;
	}
	
	private void expandComputationInputs(DbComputation comp)
		throws DbIoException
	{
		// Input parameters must have the SDI's expanded
		for(Iterator<DbCompParm> parmit = comp.getParms();
			parmit.hasNext(); )
		{
			DbCompParm parm = parmit.next();
			if (parm.isInput() && parm.getSiteId() == Constants.undefinedId)
			{
				info("Expanding input parm '" + parm.getRoleName() + "' in comp '" + comp.getName() + "'");
				try { theDb.expandSDI(parm); }
				catch(NoSuchObjectException ex)
				{
					// Do nothing, it may be a group parm with no SDI specified.
				}
				info("After expanding, siteId=" + parm.getSiteId() + ", sitename='" + parm.getSiteName() + "'");
			}
		}
	}
	
	
	private void saveDisposedCompsAndAlgos(ArrayList<DbComputation> toDispose)
		throws DbIoException, IOException
	{
		ArrayList<CompMetaData> metadata = new ArrayList<CompMetaData>();
		HashMap<String, DbCompAlgorithm> algos = new HashMap<String, DbCompAlgorithm>();
		for(DbComputation comp : toDispose)
		{
			DbCompAlgorithm algo = comp.getAlgorithm();
			algos.put(algo.getName(), algo);
		}
		for(DbCompAlgorithm algo : algos.values())
			metadata.add(algo);
		for(DbComputation comp : toDispose)
			metadata.add(comp);

		CompXio cx = new CompXio("Convert2Group", theDb);
		report("");
		report("Saving disposed computations (and algorithms) to " + disposedSingleFile.getPath());
		cx.writeFile(metadata, disposedSingleFile.getPath());
	}

}

