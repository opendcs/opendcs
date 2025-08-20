/*
 * $Id$
 * 
 * $Log$
 * 
 * Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
*/
package decodes.tsdb;

import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import opendcs.dai.TsGroupDAI;
import decodes.db.DataType;
import decodes.db.Site;
import decodes.sql.DbKey;
import decodes.util.CmdLineArgs;

/** Display group meta data and expand to ts list.*/
public class TsGroupDisplay 
	extends TsdbAppTemplate
{
	private StringToken groupNameArg = 
		new StringToken("", "Group-Name", "", TokenOptions.optArgument, null);
	private String groupName = null;
	private TsGroup theGroup = null;

	public TsGroupDisplay()
	{
		super("util.log");
	}

	public static void main(String args[])
		throws Exception
	{
		TsdbAppTemplate tp = new TsGroupDisplay();
		tp.execute(args);
	}
	
	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(groupNameArg);
	}

	@Override
	protected void runApp()
		throws Exception
	{
		groupName = groupNameArg.getValue();
		if (groupName == null)
		{
			System.err.println("Usage: ... groupName");
			System.exit(1);;
		}
		TsGroupDAI groupDAO = theDb.makeTsGroupDAO();
		theGroup = groupDAO.getTsGroupByName(groupName);
		if (theGroup == null)
		{
			System.err.println("No Such Group: " + groupName);
			System.exit(1);
		}
		else
			displayGroup();
		groupDAO.close();
	}
	
	private void displayGroup() 
		throws DbIoException, NoSuchObjectException
	{
		System.out.println("Key: " 
			+ theGroup.getGroupId());
		System.out.println("Time Series Group: " 
			+ groupName);
		System.out.println("Type: " 
			+ theGroup.getGroupType());
		System.out.println("Desc: " 
			+ theGroup.getDescription());
		
		System.out.println("Included SubGroups:");
		for(TsGroup subGroup : theGroup.getIncludedSubGroups())
			System.out.println("    key=" + subGroup.getGroupId() + ", name=" + subGroup.getGroupName());
		
		System.out.println("Excluded SubGroups:");
		for(TsGroup subGroup : theGroup.getExcludedSubGroups())
			System.out.println("    key=" + subGroup.getGroupId() + ", name=" + subGroup.getGroupName());

		System.out.println("Intersected SubGroups:");
		for(TsGroup subGroup : theGroup.getIntersectedGroups())
			System.out.println("    key=" + subGroup.getGroupId() + ", name=" + subGroup.getGroupName());

		System.out.println("Explicite time series members:");
		for(TimeSeriesIdentifier tid : theGroup.getTsMemberList())
			System.out.println("    key=" + tid.getKey() + ", id=" + tid.getUniqueString());
		
		System.out.println("Locations:");
		for(DbKey key : theGroup.getSiteIdList())
		{
			Site site = theDb.getSiteById(key);
			System.out.println("    key=" + key + " " + 
				(site != null ? site.getDisplayName() : ""));
		}
		
		System.out.println("DataTypes:");
		for(DbKey key : theGroup.getDataTypeIdList())
		{
			DataType dt = DataType.getDataType(key);
			System.out.println("    key=" + key + ", " + dt.toString());
		}
		
		for(TsGroupMember member : theGroup.getOtherMembers())
			System.out.println("    " + member.getMemberType() + "=" + member.getMemberValue());
		
		System.out.println("Expanded TSIDs:");
		for(TimeSeriesIdentifier tsid : theDb.expandTsGroup(theGroup))
			System.out.println("   key=" + tsid.getKey() + ", id="	+ tsid.getUniqueString());
	}
}
