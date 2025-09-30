/*
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
package decodes.tsdb;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.gui.DecodesInterface;
import opendcs.dai.AlgorithmDAI;
import opendcs.dai.ComputationDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;
import ilex.cmdline.*;
import decodes.db.Constants;
import decodes.hdb.HdbTimeSeriesDb;
import decodes.util.CmdLineArgs;
import decodes.tsdb.xml.*;

/**
This is the Export program to write an xml file of comp meta data.
*/
public class ExportComp extends TsdbAppTemplate
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private StringToken xmlFileArg;
	private StringToken controlFileArg;

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
			log.atError().setCause(ex).log("Cannot load TSIDs.");
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
				log.atError().setCause(ex).log("Cannot read control file '{}'", controlFileArg.getValue());
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

					try { theDb.expandSDI(dcp); }
					catch(NoSuchObjectException ex)
					{
						log.atWarn()
						   .setCause(ex)
						   .log("Time Series with id {} has been removed, setting comp parm to undefined.",
						   		dcp.getSiteDataTypeId());
						dcp.setSiteDataTypeId(Constants.undefinedId);
					}
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
						log.warn("TS Group {} doesn't exist in the database.", tsGrpName);
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
			log.atError().setCause(ex).log("Error in XML export.");
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
				log.warn("{}:{} bad syntax -- no colon, expected type:name", fname, br.getLineNumber());
				continue;
			}
			if (colon == 0)
			{
				log.warn("{}:{} bad syntax -- no type, expected type:name", fname, br.getLineNumber());
				continue;
			}
			if (line.length() <= colon+1)
			{
				log.warn("{}:{} bad syntax -- no name, expected type:name", fname, br.getLineNumber());
				continue;
			}
			String value = line.substring(colon+1).trim();
			if (value.length() == 0)
			{
				log.warn("{}:{} bad syntax -- no name, expected type:name", fname, br.getLineNumber());
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
				log.warn("{}:{} bad type, expected comp, algo, proc, or group", fname, br.getLineNumber());
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