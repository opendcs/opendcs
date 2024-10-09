/**
 *  $Id$
 *  
 *  $Log$
 *  Revision 1.6  2011/02/04 21:30:16  mmaloney
 *  Intersect groups
 *
 *  Revision 1.5  2011/01/28 20:07:02  gchen
 *  *** empty log message ***
 *
 *  Revision 1.4  2011/01/18 18:04:50  gchen
 *  *** empty log message ***
 *
 *  Revision 1.3  2011/01/16 22:10:50  gchen
 *  Modify the sortTsGroupList method to make sure to load the subgroups first and to handle the subgroup recursion.
 *
 *  In addition, the application can export a certain group type TS groups from the DB according to the command line switch -Dgrptypes=value.
 *
 *  Revision 1.2  2011/01/11 19:29:42  mmaloney
 *  dev
 *
 */
package decodes.tsdb;

import java.util.ArrayList;
import java.util.StringTokenizer;

import decodes.tsdb.xml.CompXio;
import decodes.util.AppMessages;
import decodes.util.CmdLineArgs;

import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.Logger;
import opendcs.dai.TsGroupDAI;

/**
 * Create on Dec 22, 2010
 * 
 * @author gchen
 * 
 * This is the utility to export TS groups from the DB to XML file.
 * 
 * User can export TS groups with using the following options
 * (1) no switch				Export all TS groups;
 * (2) -T grpTypes			Export those TS groups under GROUP TYPE grpTypes,
 *                      where grpTypes is <grp type1>[|<grp type2>[|...]];
 * (3) -N grpNames			Export those TS groups under GROUP NAME grpNames,
 *                      where grpNames is <grp name1>[|<grp name2>[|...]];
 *  
 */
public class ExportGroup extends TsdbAppTemplate
{
	private StringToken xmlFileArg;					//Exported XML file argument
	private StringToken tsGrpTypesArg;			//Group types switch argument -T grpTypes
	private StringToken tsGrpNamesArg;			//Group names switch argument -N grpNames
	
	private String appModule = "ExpTSGrp";

	/**
	 * Constructor called from main method after parsing arguments.
	 */
	public ExportGroup()
	{
		super("util.log");
	}

	public void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		xmlFileArg = new StringToken("", "xml-file", "",
				TokenOptions.optArgument | TokenOptions.optRequired, ""); 
		tsGrpTypesArg = new StringToken("T", "TS Group Type(s)", "",
				TokenOptions.optSwitch, "");
		tsGrpNamesArg = new StringToken("N", "TS Group Name(s)", "",
				TokenOptions.optSwitch, "");
		cmdLineArgs.addToken(tsGrpTypesArg);
		cmdLineArgs.addToken(tsGrpNamesArg);
		cmdLineArgs.addToken(xmlFileArg);
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
		for (TsGroup g: theFoundTsGrp.getExcludedSubGroups())
			addTheTSGroup(g, theTsGrpList, retTsGrpList, searchTsGrpList);
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

	/**
	 * Display the exception message from Exception object
	 * 
	 * @param exMsg
	 */
	protected void displayException(String exMsg)
	{
		displayMessage(AppMessages.sys_ExceptionMessage, 
				AppMessages.ShowMode._exception, exMsg, this.appModule);
	}
	
	/**
	 * Display the application message from a certain app
	 * 
	 * @param msgItem
	 */
	protected void displayMessage(AppMessages msgItem)
	{
		displayMessage(msgItem, null, null, this.appModule);
	}

	protected void displayMessage(AppMessages msgItem, String otherMsg)
	{
		displayMessage(msgItem, null, otherMsg, this.appModule);
	}

	protected void displayMessage(AppMessages msgItem, AppMessages.ShowMode showMode)
	{
		displayMessage(msgItem, showMode, null, this.appModule);
	}
	
	protected void displayMessage(AppMessages msgItem, String otherMsg, AppMessages.ShowMode showMode)
	{
		displayMessage(msgItem, showMode, otherMsg, this.appModule);
	}

	protected void displayMessage(AppMessages msg, AppMessages.ShowMode showMode,
			String otherMsg, String appModule)
	{
		if (msg == null)
			msg = AppMessages.sys_Unknown;
		if (showMode == null)
			showMode = AppMessages.ShowMode._displaying;
		if (otherMsg == null)
			otherMsg = "";
		if (appModule == null)
			appModule = "";
		
		String msgStr = String.format(appModule+": "+msg.showMessage(showMode), otherMsg);
		System.out.printf(msgStr + "%n");
		switch (showMode) {
			case _displaying: {
				Logger.instance().info(msgStr);
				break;
			}
			case _warning   : {
				Logger.instance().warning(msgStr);
				break;
			}
			case _error     : {
				Logger.instance().failure(msgStr);
				break;
			}
			case _exception : {
				Logger.instance().warning(msgStr);
				break;
			}
			default         : {
				Logger.instance().info(msgStr);
				break;
			}
		}
	}
		
	/* (non-Javadoc)
	 * @see decodes.tsdb.TsdbAppTemplate#runApp()
	 */
	@Override
	protected void runApp() throws Exception
	{
		TsGroupDAI groupDAO = theDb.makeTsGroupDAO();
		try 
		{
			ArrayList<CompMetaData> metadata = new ArrayList<CompMetaData>();
	
			//Get the TS groups
			ArrayList<TsGroup> tmpTsGrpsList = new ArrayList<TsGroup>();
			//Get those TS groups within the specified group types
			String grpTypes = tsGrpTypesArg.getValue();
			if (grpTypes != null && grpTypes.length() != 0)
			{
				displayMessage(AppMessages.util_CreateLimitTSGroupList);
				StringTokenizer st = new StringTokenizer(grpTypes, "|"); 
				String aGrpType = null;
				while (st.hasMoreTokens())
				{
					ArrayList<TsGroup> aTsGrpsList = null;
					aGrpType = st.nextToken();
					try
					{
 						if (aGrpType == null || aGrpType.length() == 0) continue; 
 						aTsGrpsList = groupDAO.getTsGroupList(aGrpType);
 						if (aTsGrpsList == null) continue;
 						for(TsGroup g: aTsGrpsList) tmpTsGrpsList.add(g);
					}
					catch (Exception E)
					{
						displayException(E.toString());
					}
				}
			}
			//Get those TS groups within the specified group names
			String grpNames = tsGrpNamesArg.getValue();
			if (grpNames != null && grpNames.length() != 0)
			{
				displayMessage(AppMessages.util_CreateLimitTSGroupList);
				StringTokenizer st = new StringTokenizer(grpNames, "|"); 
				String aGrpName = null;
				while (st.hasMoreTokens())
				{
					TsGroup aTsGrp = null;
					aGrpName = st.nextToken();
					try
					{
						if (aGrpName == null || aGrpName.length() == 0) continue;
						aTsGrp = groupDAO.getTsGroupByName(aGrpName);
 						if (aTsGrp == null) continue;
 						tmpTsGrpsList.add(aTsGrp);
					}
					catch (Exception E)
					{
						displayException(E.toString());
					}
				}
			}
			//Get ALL TS groups if no specified group type or name is defined
			if ((grpTypes == null || grpTypes.length() == 0) &&
					(grpNames == null || grpNames.length() == 0))
			{
				displayMessage(AppMessages.util_CreateFullTSGroupList);
				try
				{
					tmpTsGrpsList = groupDAO.getTsGroupList(null);
				}
				catch (Exception E)
				{
					displayException(E.toString());
				}
			}
			
			//Return if the tmpTsGrpsList is empty
			if (tmpTsGrpsList == null || tmpTsGrpsList.size() == 0)
			{
				displayMessage(AppMessages.util_EmptyTSGroupList, AppMessages.ShowMode._warning);
				return;
			}
			
			//Expand all TS groups with their subgroups
			for(TsGroup tsGrp: tmpTsGrpsList) {
				if (tsGrp != null) {
					for(TsGroup g: tsGrp.getIncludedSubGroups())
						tmpTsGrpsList.add(g);
					for(TsGroup g: tsGrp.getExcludedSubGroups())
						tmpTsGrpsList.add(g);
					for(TsGroup g: tsGrp.getIntersectedGroups())
						tmpTsGrpsList.add(g);
				}
			}

			//Reorder the tmpTsGrpsList and
			//put all subgroups of a TS group in front of it without duplication
			displayMessage(AppMessages.util_SortTSGroupList);
			ArrayList<TsGroup> tsGrpsList = sortTsGroupList(tmpTsGrpsList);
			
			//Return if the tsGrpsList is empty
			if (tsGrpsList == null || tsGrpsList.size() == 0)
			{
				displayMessage(AppMessages.util_EmptyTSGroupList, AppMessages.ShowMode._warning);
				return;
			}

			//Load each TS group into the metadata
			displayMessage(AppMessages.util_LoadTSGroupList);
			for(TsGroup tsGrp: tsGrpsList)
			  metadata.add(tsGrp);
	
			//Write the metadata into the XML file
			displayMessage(AppMessages.util_MakeCompXioObj);
			CompXio cx = new CompXio("ExportGroup", theDb);
			String fn = xmlFileArg.getValue(0);
			displayMessage(AppMessages.util_WriteMetadataToXML, fn);
			cx.writeFile(metadata, fn);
			displayMessage(AppMessages.util_SuccessWriteMetadataToXML, fn);
		}
		catch(Exception ex) {
			displayMessage(AppMessages.util_FailExpTSGroup, AppMessages.ShowMode._error);
			displayException(ex.toString());
			ex.printStackTrace();
		}
		finally
		{
			groupDAO.close();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
		throws Exception
	{
		ExportGroup app = new ExportGroup();
		app.execute(args);
		app.closeDb();
	}

}
