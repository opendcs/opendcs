/*
* Copyright 2007 Ilex Engineering, Inc. - All Rights Reserved.
* No part of this file may be duplicated in either hard-copy or electronic
* form without specific written permission.
*
 * 2014 Notice: Cove Software, LLC believes the above copyright notice to be
 * in error. This module was 100% funded by the U.S. Federal Government under
 * contracts requiring that it be Government-Owned. It has been delivered to
 * U.S. Bureau of Reclamation, U.S. Geological Survey, and U.S. Army Corps of
 * Engineers under contract.
*/
package decodes.tsdb;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import ilex.cmdline.*;

import decodes.db.DataType;
import decodes.db.Site;
import decodes.sql.DbKey;
import decodes.util.CmdLineArgs;


/**
Reads some test data from a time series in the database.
*/
public class ShowTsGroup extends TsdbAppTemplate
{
	private StringToken grpNameArg;

	public ShowTsGroup()
	{
		super(null);
	}

	/**
	 * Overrides to add test-specific arguments.
	 */
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		grpNameArg = new StringToken("", "Group Name", "", 
			TokenOptions.optArgument, null);
		cmdLineArgs.addToken(grpNameArg);
	}

	public static void main(String args[])
		throws Exception
	{
		TsdbAppTemplate tp = new ShowTsGroup();
		tp.execute(args);
	}

	protected void runApp()
		throws Exception
	{
		String grpName = null;
		if (grpNameArg.NumberOfValues() > 0)
			grpName = grpNameArg.getValue();
		if (grpName == null || grpName.length() == 0)
		{
			System.out.print("Enter Group Name: ");
			BufferedReader bis = new BufferedReader(
				new InputStreamReader(System.in));
			grpName = bis.readLine();
		}
		TsGroup grp = theDb.getTsGroupByName(grpName);
		if (grp == null)
		{
			System.out.println("No Such Time Series Group: " + grpName);
			System.exit(0);
		}
		System.out.println("Time Series Group: " + grpName);
		System.out.println("ID: " + grp.getGroupId());
		System.out.println("Type: " + grp.getGroupType());
		System.out.println("Desc: " + grp.getDescription());
		System.out.println("Included SubGroups:");
		for(TsGroup subgrp : grp.getIncludedSubGroups())
			System.out.println("\tid=" + subgrp.getGroupId() + ", name="
				+ subgrp.getGroupName());
		System.out.println("Excluded SubGroups:");
		for(TsGroup subgrp : grp.getExcludedSubGroups())
			System.out.println("\tid=" + subgrp.getGroupId() + ", name="
				+ subgrp.getGroupName());
		System.out.println("Explicitly included time series:");
		for(TimeSeriesIdentifier tsid : grp.getTsMemberList())
			System.out.println("\t ts key=" + tsid.getKey()
				+ ", id-str=" + tsid.getUniqueString());
		System.out.println("Sites:");
		for(DbKey I : grp.getSiteIdList())
		{
			Site site = theDb.getSiteById(I);
			System.out.println("\tid=" + I + " " + (site != null ? site.getDisplayName() : ""));
		}
		System.out.println("Data Types:");
		for(DbKey I : grp.getDataTypeIdList())
		{
			DataType dt = DataType.getDataType(I);
			System.out.println("\tid=" + I + ", " + dt.toString());
		}
		if (theDb.isCwms())
		{
			System.out.println("ParamTypes:");
			for(String s : grp.getParamTypeList())
				System.out.println("\t" + s);
		}
		System.out.println("Intervals:");
		for(String s : grp.getIntervalCodeList())
			System.out.println("\t" + s);
		System.out.println("Durations:");
		for(String s : grp.getDurationList())
			System.out.println("\t" + s);
		if (theDb.isCwms())
		{
			System.out.println("Versions:");
			for(String s : grp.getVersionList())
				System.out.println("\t" + s);
		}
		
		System.out.println("Expanded Time Series Identifiers:");
		for(TimeSeriesIdentifier tsid : theDb.expandTsGroup(grp))
			System.out.println("\tsid key=" + tsid.getKey() + ", str="
				+ tsid.getUniqueString());
	}
}
