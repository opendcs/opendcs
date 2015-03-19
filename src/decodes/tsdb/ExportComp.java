/*
*  $Id$
*  
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.15  2012/08/20 20:08:05  mmaloney
*  Implement HDB Convert2Group utility.
*
*  Revision 1.14  2012/01/17 17:45:41  mmaloney
*  Catch NoSuchObject exception if a TS is deleted, rendering a computation invalid.
*
*  Revision 1.13  2011/02/21 21:29:02  mmaloney
*  Also export apps
*
*  Revision 1.12  2011/02/21 21:23:13  mmaloney
*  Allow spaces after colon in control file.
*
*  Revision 1.11  2011/02/21 20:57:05  mmaloney
*  Implement control file feature.
*
*  Revision 1.10  2011/02/04 21:30:16  mmaloney
*  Intersect groups
*
*  Revision 1.9  2011/01/28 20:07:02  gchen
*  *** empty log message ***
*
*  Revision 1.8  2011/01/16 22:05:00  gchen
*  Modify the sortTsGroupList method to make sure to load the subgroups first and to handle the subgroup recursion.
*
*  Add the command line switch -A to allow to export all TS groups from the DB
*
*  Revision 1.7  2011/01/11 19:29:42  mmaloney
*  dev
*
*/
package decodes.tsdb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Iterator;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import lrgs.gui.DecodesInterface;
import opendcs.dai.AlgorithmDAI;
import opendcs.dai.ComputationDAI;
import opendcs.dai.LoadingAppDAI;
import ilex.cmdline.*;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.StderrLogger;
import ilex.util.UserAuthFile;
import decodes.db.Constants;
import decodes.util.AppMessages;
import decodes.util.CmdLineArgs;
import decodes.tsdb.xml.*;
import decodes.tsdb.test.TestProg;

/**
This is the Export program to write an xml file of comp meta data.
*/
public class ExportComp
	extends TsdbAppTemplate
{
	private StringToken xmlFileArg;
	private StringToken controlFileArg;
	private String outputFile = null;;
	
	private ArrayList<String> ctrlFileAlgo = new ArrayList<String>();
	private ArrayList<String> ctrlFileComp = new ArrayList<String>();
	private ArrayList<String> ctrlFileProc = new ArrayList<String>();
	private ArrayList<String> ctrlFileGroup = new ArrayList<String>();

	/**
	 * Constructor called from main method after parsing arguments.
	 */
	public ExportComp(String logfile)
	{
		super(logfile);
	}

	public void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		controlFileArg = new StringToken("C", "control-file",
			"", TokenOptions.optSwitch, ""); 
		xmlFileArg = new StringToken("", "xml-file",
			"", TokenOptions.optArgument | TokenOptions.optRequired, ""); 
		cmdLineArgs.addToken(controlFileArg);
		cmdLineArgs.addToken(xmlFileArg);
		DecodesInterface.silent = true;
	}

	/**
	 * The run method.
	 */
	public void runApp( )
	{
		Properties props = cmdLineArgs.getCmdLineProps();
		boolean haveControlFile = false;
		if (controlFileArg.getValue() != null
		 && controlFileArg.getValue().length() > 0)
		{
			try
			{
				readControlFile(controlFileArg.getValue());
				haveControlFile = true;
			}
			catch(IOException ex)
			{
				String msg = "Cannot read control file '" 
					+ controlFileArg.getValue() + "': " + ex;
				Logger.instance().warning(msg);
				System.err.println(msg);
				return;
			}
		}
	
		LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO();
		AlgorithmDAI algorithmDao = theDb.makeAlgorithmDAO();
		ComputationDAI computationDAO = theDb.makeComputationDAO();

		try
		{
			ArrayList<CompMetaData> metadata = new ArrayList<CompMetaData>();
	
			// now go get the applications data
			String inList = null;
			inList = props.getProperty("apps");
			List<CompAppInfo> applist = null; 
			if (inList == null || inList.length() == 0)
			{
				System.err.println("Creating FULL apps lists.");
				applist = loadingAppDao.listComputationApps(false);
			}
			else
			{
				System.err.println("Creating limited list apps.");
				applist = loadingAppDao.ComputationAppsIn(inList);
			}
			for(CompAppInfo cai : applist)
			{
				if (haveControlFile
				 && !ctrlFileProc.contains(cai.getAppName().toLowerCase()))
					continue;
				metadata.add(cai);
			}
	
			// now get the algorithms  data
			inList = props.getProperty("algs");
			List<String> algolist = null; 
			if (inList == null || inList.length() == 0)
			{
				System.err.println("Creating FULL algo list.");
				algolist = theDb.listAlgorithms();
			}
			else
			{
				System.err.println("Creating limited algos.");
				algolist = theDb.AlgorithmsIn(inList);
			}

			for(String algoname : algolist)
			{
				if (haveControlFile 
				 && !ctrlFileAlgo.contains(algoname.toLowerCase()))
					continue;
				DbCompAlgorithm algo = algorithmDao.getAlgorithm(algoname);
				metadata.add(algo);
			}

			// now get the computations
			inList = props.getProperty("comps");
			//System.out.println("the inlist: " + inList); 
			List<String> compList = null;
			if (inList == null || inList.length() == 0)
			{
				System.err.println("Creating FULL comp list.");
				compList = theDb.listComputations();
			}
			else if (inList.indexOf("|") > -1) // had multilist entry
			{
				System.err.println("Creating multi-limited comps.");
				StringTokenizer st = new StringTokenizer(inList,"|"); 
				String tstring = null;
				while (st.hasMoreTokens())
				{
					List<String> tmpList = null;
					tstring = st.nextToken();
				//System.err.println("TSTRING: "+ tstring);
					tmpList = theDb.ComputationsIn(tstring);
					if (compList == null) compList = tmpList;
					else for(String tname : tmpList) compList.add(tname);
				}
			}
			else //just had one multilist entry
			{
				System.err.println("Creating limited comps.");
				compList = theDb.ComputationsIn(inList);
			}
			//System.err.println("Final List:  " + compList);
			
			//Load the computations into the metadata
			ArrayList<TsGroup> tsGrpList = new ArrayList<TsGroup>(); 
			for(String compname : compList)
			{
				if (haveControlFile 
				 && !ctrlFileComp.contains(compname.toLowerCase()))
					continue;

				DbComputation comp = computationDAO.getComputationByName(compname);

				// Each parm has an SDI. Prior to export, I need to expand
				// this into DataType objects.
				for(Iterator<DbCompParm> pit = comp.getParms(); pit.hasNext();)
				{
					DbCompParm dcp = pit.next();
					if (dcp.getSiteDataTypeId() != Constants.undefinedId)
					{
						try { theDb.expandSDI(dcp); }
						catch(NoSuchObjectException ex)
						{
							Logger.instance().warning("Time Series with id "
								+ dcp.getSiteDataTypeId()
								+ " has been removed, setting comp parm to undefined.");
							dcp.setSiteDataTypeId(Constants.undefinedId);
						}
					}
				}
				
				//Get the TS group name for each computation and
				//add all TS groups into the TSGroupList with expanding their TS subgroups
				String tsGrpName = null;
				if ((tsGrpName = comp.getGroupName()) != null)
				{
					TsGroup tsGrp;
					if ((tsGrp = theDb.getTsGroupByName(tsGrpName)) != null)
					{
						tsGrpList.add(tsGrp);
						for (TsGroup g : tsGrp.getIncludedSubGroups())
							tsGrpList.add(g);
						for (TsGroup g : tsGrp.getExcludedSubGroups())
							tsGrpList.add(g);
						for (TsGroup g : tsGrp.getIntersectedGroups())
							tsGrpList.add(g);
					}
					else
					{
						String msg = "TS Group " + tsGrpName
								+ " doesn't exist in the database.";
						System.err.println(msg);
						Logger.instance().warning(msg);
					}
				}

				//Add the computation into the metadata
				metadata.add(comp);
			}

			// Get the TS groups from the DB
			ArrayList<TsGroup> tmpGrps = theDb.getTsGroupList(null);
		nextTmpGrp:
			for (TsGroup tmpGrp : tmpGrps)
			{
				if (haveControlFile
				 && !ctrlFileGroup.contains(tmpGrp.getGroupName().toLowerCase()))
					continue;
				for(TsGroup alreadyInList : tsGrpList)
					if (alreadyInList.getGroupName().equalsIgnoreCase(
						tmpGrp.getGroupName()))
						continue nextTmpGrp;
				tmpGrp = theDb.getTsGroupById(tmpGrp.getGroupId());
				tsGrpList.add(tmpGrp);
			nextIncSub:
				for (TsGroup g : tmpGrp.getIncludedSubGroups())
				{
					for(TsGroup alreadyInList : tsGrpList)
						if (alreadyInList.getGroupName().equalsIgnoreCase(
							g.getGroupName()))
							continue nextIncSub;
					tsGrpList.add(g);
				}
			nextExcSub:
				for (TsGroup g : tmpGrp.getExcludedSubGroups())
				{
					for(TsGroup alreadyInList : tsGrpList)
						if (alreadyInList.getGroupName().equalsIgnoreCase(
							g.getGroupName()))
							continue nextExcSub;
					tsGrpList.add(g);
				}
			nextIntSub:
				for (TsGroup g : tmpGrp.getIntersectedGroups())
				{
					for(TsGroup alreadyInList : tsGrpList)
						if (alreadyInList.getGroupName().equalsIgnoreCase(
							g.getGroupName()))
							continue nextIntSub;
					tsGrpList.add(g);
				}
			}
			
			//Reorder the tmpTsGrpsList and
			//put all subgroups of a TS group in front of it
			ArrayList<TsGroup> tsGrpsList = sortTsGroupList(tsGrpList);
			
			//Load each TS group into the metadata
			if (tsGrpsList != null) {
				for(TsGroup tsGrp: tsGrpsList)
				  metadata.add(tsGrp);
			}
			
System.err.println("Making CompXio object...");
			CompXio cx = new CompXio("ExportComp", theDb);
			String fn = xmlFileArg.getValue(0);
System.err.println("Calling write file to " + fn);
			cx.writeFile(metadata, fn);
		}
		catch(Exception ex)
		{
			String msg = "Error in XML export: " + ex;
			Logger.instance().fatal(msg);
			System.err.println(msg);
			ex.printStackTrace();
		}
		finally
		{
			computationDAO.close();
			algorithmDao.close();
			loadingAppDao.close();
		}
	}

	/**
	 * Sort the TS Group List with searching subgroups for each TS group and 
	 * putting them in the front of each referring TS group.
	 * 
	 * @param tsGrpsList: a TsGroup array list for sorting
	 * @return ArrayList<TsGroup>: sorted TsGroup array list 
	 */
	protected ArrayList<TsGroup> sortTsGroupList(ArrayList<TsGroup> theTsGrpList)
	{
		if ((theTsGrpList == null) || (theTsGrpList.size() == 0))
			return null;
		
		ArrayList<TsGroup> retTsGrpList = new ArrayList<TsGroup>();
		ArrayList<TsGroup> searchTsGrpList = new ArrayList<TsGroup>();
		
		for (TsGroup tsGrp: theTsGrpList) {
			searchTsGrpList.clear();
			addTheTSGroup(tsGrp, theTsGrpList, retTsGrpList, searchTsGrpList);
		}
		
		theTsGrpList.clear();
		searchTsGrpList.clear();
		return retTsGrpList;
	}

	 /**
	  * Add a TS group object with its subgroups into retTsGrpList
	  * 
	  * @param tsGrp
	  * @param theTsGrpList
	  * @param retTsGrpList
	  * @param searchTsGrpList
	  */
	private void addTheTSGroup(TsGroup tsGrp, ArrayList<TsGroup> theTsGrpList,
			ArrayList<TsGroup> retTsGrpList, ArrayList<TsGroup> searchTsGrpList)
	{
		//tsGrp is null
		if (tsGrp == null)
			return;
		
		//tsGrp is found in retTsGrpList, so no need to add this object.
		if (findTsGroup(tsGrp, retTsGrpList) != null)
			return;

		TsGroup theFoundTsGrp = findTsGroup(tsGrp, theTsGrpList);
		//tsGrp is not found in theTsGrpList
		if (theFoundTsGrp == null)
			return;
		
		//If tsGrp appears in the searchTsGrpList, stop recursion
		if (searchTsGrpList != null) {
			if (findTsGroup(theFoundTsGrp, searchTsGrpList) != null)
				return;
			else
				searchTsGrpList.add(theFoundTsGrp);
		}
		
		//tsGrp is found in theTsGrpList, so do the following
		//Add theFoundTsGrp with its included subgroups into retTsGrpList
		for (TsGroup g: theFoundTsGrp.getIncludedSubGroups())
			addTheTSGroup(g, theTsGrpList, retTsGrpList, searchTsGrpList);

		//Add theFoundTsGrp with its excluded subgroups into retTsGrpList
		for (TsGroup g: theFoundTsGrp.getExcludedSubGroups())
			addTheTSGroup(g, theTsGrpList, retTsGrpList, searchTsGrpList);

		//Add theFoundTsGrp with its intersected subgroups into retTsGrpList
		for (TsGroup g: theFoundTsGrp.getIntersectedGroups())
			addTheTSGroup(g, theTsGrpList, retTsGrpList, searchTsGrpList);
		
		retTsGrpList.add(theFoundTsGrp);
	}

	private TsGroup findTsGroup(TsGroup tsGrp, ArrayList<TsGroup> theTsGrpList)
	{
		if ((tsGrp == null) || (theTsGrpList == null) || (theTsGrpList.size() == 0))
			return null;
		
		for (TsGroup g: theTsGrpList)
			if (g.getGroupName().equals(tsGrp.getGroupName()))
				return g;

		return null;
	}

	private void readControlFile(String fname)
		throws IOException
	{
		File ctrlFile = new File(fname);
		LineNumberReader br = new LineNumberReader(
			new FileReader(ctrlFile));
		String line = null;
		while((line = br.readLine()) != null)
		{
			line = line.trim().toLowerCase();
			if (line.length() == 0 || line.charAt(0) == '#')
				continue;
			int colon = line.indexOf(':');
			if (colon == -1)
			{
				Logger.instance().warning(fname + ":" + br.getLineNumber()
					+ " bad syntax -- no colon, expected type:name");
				continue;
			}
			if (colon == 0)
			{
				Logger.instance().warning(fname + ":" + br.getLineNumber()
					+ " bad syntax -- no type, expected type:name");
				continue;
			}
			if (line.length() <= colon+1)
			{
				Logger.instance().warning(fname + ":" + br.getLineNumber()
					+ " bad syntax -- no name, expected type:name");
				continue;
			}
			String value = line.substring(colon+1).trim();
			if (value.length() == 0)
			{
				Logger.instance().warning(fname + ":" + br.getLineNumber()
					+ " bad syntax -- no name, expected type:name");
				continue;
			}
				
			switch(line.charAt(0))
			{
			case 'c': // computation
				ctrlFileComp.add(value);
				break;
			case 'a': // algorithm
				ctrlFileAlgo.add(value);
				break;
			case 'p': // process
				ctrlFileProc.add(value);
				break;
			case 'g': // group
				ctrlFileGroup.add(value);
				break;
			default:
				Logger.instance().warning(fname + ":" + br.getLineNumber()
					+ " bad type, expected comp, algo, proc, or group");
			}
		}
		br.close();
	}

	public void setOutputFile(String fn)
	{
		xmlFileArg.setDefaultValue(fn);
	}
	
	/**
	 * The main method.
	 * @param args command line arguments.
	 */
	public static void main( String[] args )
		throws Exception
	{
		// Call run method directly. For multi threaded executive, we would
		// create a thread and start it.
		ExportComp app = new ExportComp("util.log");
		app.execute(args);
	}
}
