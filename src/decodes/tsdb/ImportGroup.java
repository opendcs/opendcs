/**
 *  $Id:
 *  
 *  $Log:
 */
package decodes.tsdb;

import java.util.ArrayList;

import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import decodes.db.Constants;
import decodes.sql.DbKey;
import decodes.tsdb.xml.CompXio;
import decodes.tsdb.xml.DbXmlException;
import decodes.util.AppMessages;
import decodes.util.CmdLineArgs;
import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.Logger;

/**
 * Create on Dec 22, 2010
 * 
 * @author gchen
 * 
 *
 */
public class ImportGroup extends TsdbAppTemplate
{
	private StringToken xmlFileArgs;
	private BooleanToken createTimeSeries;
	private String appModule = "ImpTSGrp";
	private SiteDAI siteDAO = null;
	
	private enum LookupObjectType {TsUniqStr, SiteId, InclSubgrp, ExclSubgrp};

	/**
	 * @param logname
	 */
	public ImportGroup()
	{
		super("util.log");
		setSilent(true);
	}

	public void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		createTimeSeries = new BooleanToken("C", "create parms as needed",
			"", TokenOptions.optSwitch, false);
		cmdLineArgs.addToken(createTimeSeries);

		xmlFileArgs = new StringToken("", "xml-file",
			"", TokenOptions.optArgument | TokenOptions.optMultiple|
			TokenOptions.optRequired, ""); 
		cmdLineArgs.addToken(xmlFileArgs);
	}
	
	/**
	 * Sort the TS Group List with searching subgroups for each TS group and 
	 * putting them in the front of each referring TS group.
	 * 
	 * @param tsGrpsList: a TsGroup array list for sorting
	 * @return ArrayList<TsGroup>: sorted TsGroup array list 
	 */
	protected ArrayList<TsGroup> sortTsGroupList(ArrayList<TsGroup> tsGrpsList)
	{
		if ((tsGrpsList == null) || (tsGrpsList.size() == 0))
			return null;
		
		ArrayList<TsGroup> retTsGrpsList = new ArrayList<TsGroup>();
		for(TsGroup tsGrp: tsGrpsList) {
			for(TsGroup tsSubGrp: tsGrp.getIncludedSubGroups())
				addTheTSGroup(tsSubGrp, tsGrpsList, retTsGrpsList);
			for(TsGroup tsSubGrp: tsGrp.getExcludedSubGroups())
				addTheTSGroup(tsSubGrp, tsGrpsList, retTsGrpsList);
			
			addTheTSGroup(tsGrp, tsGrpsList, retTsGrpsList);
		}
		tsGrpsList.clear();
		return retTsGrpsList;
	}

	/**
	 * Add a TS group object found from the fromTsGroupList into
	 * the toTsGroupList which does not contain the TS group object.
	 *  
	 * @param tsGrp
	 * @param fromTSGrpList
	 * @param toTSGrpList
	 */
	private void addTheTSGroup(TsGroup tsGrp, 
			ArrayList<TsGroup> fromTsGrpList, ArrayList<TsGroup> toTsGrpList)
	{
		if (toTsGrpList == null)
			return;
		
		if (toTsGrpList.size() == 0) {
			//Add the tsGrp object found from the fromTsGrpList into the toTsGrpList
			if (fromTsGrpList != null)
				for(TsGroup g: fromTsGrpList)
					if (g.getGroupName().equals(tsGrp.getGroupName())) {
						toTsGrpList.add(g);
						break;
					}
			return;
		}
		
		//Search if the tsGrp already exists in the toTsGrpList
		boolean hasTheTsGrp = false;
		for(TsGroup g: toTsGrpList) {
			if (g.getGroupName().equals(tsGrp.getGroupName())) {
				hasTheTsGrp = true;
				break;
			}
		}
		//Add the tsGrp object found from the fromTsGrpList into the toTsGrpList
		if (!hasTheTsGrp && (fromTsGrpList != null))
			for(TsGroup g: fromTsGrpList)
				if (g.getGroupName().equals(tsGrp.getGroupName())) {
					toTsGrpList.add(g);
					break;
				}
	}
	
	/**
	 * Lookup if a certain object exists in the DB. If not, ignore it within the imported TS group 
	 * 
	 * @param tsGrp: TS group needs to be expanded for certain object 
	 * @param lookupObjType: a certain object type, TsUniqStr  - time series unique string;
	 *                                              SiteId     - site ID;
	 *                                              InclSubgrp - included subgroup
	 *                                              ExclSubgrp - excluded subgroup
	 */
	protected void lookupObject(TsGroup tsGrp, LookupObjectType lookupObjType)
	{
		ArrayList<Object> objList = new ArrayList<Object>();
		switch (lookupObjType) {
			case TsUniqStr: {
				for(String strObj: tsGrp.getTsMemberIDList())
					objList.add(strObj);
				break;
			}
			case SiteId: {
				for(String strObj: tsGrp.getSiteNameList())
					objList.add(strObj);
				break;
			}
			case InclSubgrp: {
				for(TsGroup subGrp: tsGrp.getIncludedSubGroups())
					objList.add(subGrp);
				break;
			}
			case ExclSubgrp: {
				for(TsGroup subGrp: tsGrp.getExcludedSubGroups())
					objList.add(subGrp);
				break;
			}
		}
		
		String msgStr;
		for(Object obj: objList) 
		{
			TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
			try 
			{
				switch (lookupObjType) 
				{
					case TsUniqStr: 
					{
						msgStr = " time series unique string does not exist.";
						TimeSeriesIdentifier objId = 
							timeSeriesDAO.getTimeSeriesIdentifier((String)obj);
						if (objId != null)
							tsGrp.addTsMember(objId);
						else
							System.out.println((String)obj + msgStr);
						break;
					}
					case SiteId: {
						msgStr = "  site does not exist.";
						DbKey objId = siteDAO.lookupSiteID((String)obj);
						if (objId != Constants.undefinedId)
							tsGrp.addSiteId(objId);
						else
							System.out.println((String)obj + msgStr);
					  break;
					}
					case InclSubgrp: {
						msgStr = " subgroup does not exist.";
						TsGroup objId = theDb.getTsGroupByName(((TsGroup)obj).getGroupName());
						if (objId != null)
							((TsGroup)obj).setGroupId(objId.getGroupId());
						else
							System.out.println(((TsGroup)obj).getGroupName() + msgStr);
						break;
					}
					case ExclSubgrp: {
						msgStr = " subgroup does not exist.";
						TsGroup objId = theDb.getTsGroupByName(((TsGroup)obj).getGroupName());
						if (objId != null)
							((TsGroup)obj).setGroupId(objId.getGroupId());
						else
							System.out.println(((TsGroup)obj).getGroupName() + msgStr);
						break;
					}
				}
			}
			catch (Exception E) 
			{
				System.out.println(E.toString());
			}
			finally
			{
				if (timeSeriesDAO != null)
					timeSeriesDAO.close();
			}
		}
	}

	protected void displayException(String exMsg)
	{
		displayMessage(AppMessages.sys_ExceptionMessage, 
				AppMessages.ShowMode._exception, exMsg, this.appModule);
	}

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
		siteDAO = theDb.makeSiteDAO();
		CompXio cx = new CompXio("ImportGroup", theDb);
		for(int i=0; i<xmlFileArgs.NumberOfValues(); i++) {
      //Read the metadata from XML file
			String fn = xmlFileArgs.getValue(i);
			displayMessage(AppMessages.util_ReadMetadataFromXML, fn);
			ArrayList<CompMetaData> metadata;
			try { 
				metadata = cx.readFile(fn);
				displayMessage(AppMessages.util_SuccessReadMetadataFromXML, fn);
			}
			catch(DbXmlException ex) {
				displayMessage(AppMessages.util_FailReadMetadataFromXML, fn);
				continue;
			}

			//Write the TS groups from the metadata into the tmpTsGrpsList
			displayMessage(AppMessages.util_LoadTSGroupList);
			ArrayList<TsGroup> tmpTsGrpsList = new ArrayList<TsGroup>();
			for(CompMetaData mdobj: metadata) {
				if (mdobj instanceof TsGroup)
					tmpTsGrpsList.add((TsGroup)mdobj);
			}				

			//Reorder the tmpTsGrpsList and
			//put all subgroups of a TS group in front of it
			displayMessage(AppMessages.util_SortTSGroupList);
			ArrayList<TsGroup> tsGrpsList = sortTsGroupList(tmpTsGrpsList);

			//Write the TS groups into the DB
			boolean incompleteFlag = false;
			displayMessage(AppMessages.util_WriteTSGroupsToDB, "");
			try {
				for(TsGroup g: tsGrpsList) {
				  //Lookup the time series unique string
					lookupObject(g, LookupObjectType.TsUniqStr);
					
					//Lookup the site ID
					lookupObject(g, LookupObjectType.SiteId);
	
					//Lookup the subgroup ID
					lookupObject(g, LookupObjectType.InclSubgrp);
					lookupObject(g, LookupObjectType.ExclSubgrp);
	
					//Write each TS group into the DB 
					try {
						theDb.writeTsGroup(g);
					}
					catch(DbIoException E) {
						incompleteFlag = true;
						displayMessage(AppMessages.util_FailWriteTSGroupToDB, g.getGroupName(), AppMessages.ShowMode._error);
						displayException(E.toString());
					}
			  }  //The end of for(TsGroup g: tsGrpsList) loop
				if (incompleteFlag)
					displayMessage(AppMessages.util_IncompleteWriteMetadataToDB);
				else
					displayMessage(AppMessages.util_SuccessWriteMetadataToDB);
			}
			catch (Exception E) {
				displayMessage(AppMessages.util_FailImpTSGroup, E.toString(), AppMessages.ShowMode._error);
			}
		}
		siteDAO.close();
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) 
	  throws Exception
	{
		ImportGroup app = new ImportGroup();
		app.execute(args);
		app.closeDb();
	}

}
