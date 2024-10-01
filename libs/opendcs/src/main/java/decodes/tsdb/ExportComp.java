/*
*  $Id$
*  
*  $Log$
*  Revision 1.4  2017/04/27 21:07:32  mmaloney
*  Update to use DAOs rather than the obsolete methods in TimeSeriesDb.
*
*  Revision 1.3  2016/09/23 15:54:58  mmaloney
*  Remove stderr debugs.
*
*  Revision 1.2  2015/03/19 18:05:07  mmaloney
*  Set DecodesInterface.silent
*
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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Iterator;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;

import lrgs.gui.DecodesInterface;
import opendcs.dai.AlgorithmDAI;
import opendcs.dai.ComputationDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;
import ilex.cmdline.*;
import ilex.util.Logger;
import decodes.db.Constants;
import decodes.hdb.HdbTimeSeriesDb;
import decodes.util.CmdLineArgs;
import decodes.tsdb.xml.*;

/**
This is the Export program to write an xml file of comp meta data.
*/
public class ExportComp
	extends TsdbAppTemplate
{
	private StringToken xmlFileArg;
	private StringToken controlFileArg;
//	private String outputFile = null;;
	
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
		TimeSeriesDAI tsdao = theDb.makeTimeSeriesDAO();
		try
		{
			tsdao.reloadTsIdCache();
			if (theDb instanceof HdbTimeSeriesDb)
				((HdbTimeSeriesDb)theDb).fillHdbSdiCache();
		}
		catch (DbIoException ex)
		{
			String msg = "Cannot load TSIDs: " + ex;
			Logger.instance().fatal(msg);
			System.err.println(msg);
			return;
		}
		finally
		{
			tsdao.close();
		}
		
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
				Logger.instance().fatal(msg);
				System.err.println(msg);
				return;
			}
		}
	
		LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO();
		AlgorithmDAI algorithmDao = theDb.makeAlgorithmDAO();
		ComputationDAI computationDAO = theDb.makeComputationDAO();
		TsGroupDAI groupDAO = theDb.makeTsGroupDAO();

		try
		{
			ArrayList<CompMetaData> metadata = new ArrayList<CompMetaData>();
	
			// now go get the applications data
			List<CompAppInfo> applist = loadingAppDao.listComputationApps(false);
			for(CompAppInfo cai : applist)
			{
				if (haveControlFile && !ctrlFileProc.contains(cai.getAppName().toLowerCase()))
					continue;
				metadata.add(cai);
			}
	
			// now get the algorithms  data
			ArrayList<DbCompAlgorithm> algolist = algorithmDao.listAlgorithms();
			for(DbCompAlgorithm algo : algolist)
			{
				if (haveControlFile && !ctrlFileAlgo.contains(algo.getName().toLowerCase()))
					continue;
				metadata.add(algo);
			}

			// now get the computations
			ArrayList<DbComputation> compList = computationDAO.listCompsForGUI(new CompFilter());
			
			//Load the computations into the metadata
			ArrayList<TsGroup> tsGrpList = new ArrayList<TsGroup>(); 
			for(DbComputation comp : compList)
			{
				if (haveControlFile && !ctrlFileComp.contains(comp.getName().toLowerCase()))
					continue;

				// Each parm has an SDI. Prior to export, I need to expand
				// this into DataType objects.
				for(Iterator<DbCompParm> pit = comp.getParms(); pit.hasNext();)
				{
					DbCompParm dcp = pit.next();
					// NOTE: expandSDI even if SDI is null site and dt may be specified independently
					// for group computations.
//					if (dcp.getSiteDataTypeId() != Constants.undefinedId)
//					{
						try { theDb.expandSDI(dcp); }
						catch(NoSuchObjectException ex)
						{
							Logger.instance().warning("Time Series with id "
								+ dcp.getSiteDataTypeId()
								+ " has been removed, setting comp parm to undefined.");
							dcp.setSiteDataTypeId(Constants.undefinedId);
						}
//					}
				}
				
				//Get the TS group name for each computation and
				//add all TS groups into the TSGroupList with expanding their TS subgroups
				String tsGrpName = null;
				if ((tsGrpName = comp.getGroupName()) != null)
				{
					TsGroup tsGrp;
					if ((tsGrp = groupDAO.getTsGroupByName(tsGrpName)) != null)
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
			ArrayList<TsGroup> tmpGrps = groupDAO.getTsGroupList(null);
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
				tmpGrp = groupDAO.getTsGroupById(tmpGrp.getGroupId());
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
			
			CompXio cx = new CompXio("ExportComp", theDb);
			String fn = xmlFileArg.getValue(0);
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
			groupDAO.close();
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
