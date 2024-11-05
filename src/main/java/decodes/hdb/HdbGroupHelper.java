/*
*  $Id$
*
*  This is open-source software written under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*  
*  $Log$
*  Revision 1.2  2018/02/05 15:50:54  mmaloney
*  Added ObjectType capability.
*
*  Revision 1.1  2016/11/03 19:00:36  mmaloney
*  Refactoring for group evaluation to make HDB work the same way as CWMS.
*
*  Revision 1.2  2016/06/27 15:30:11  mmaloney
*  Removed nuisance INFO and DEBUGs
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.5  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*/
package decodes.hdb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.tsdb.GroupHelper;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;
import decodes.tsdb.TsGroupMember;

/**
 * Implement a cache of TS Groups used in GUIs and for evaluation.
 * @author mmaloney
 */
public class HdbGroupHelper
	extends GroupHelper
{
	public HdbGroupHelper(TimeSeriesDb tsdb)
	{
		super(tsdb);
	}

	@Override
	protected boolean passesParts(TsGroup grp, TimeSeriesIdentifier tsid)
	{
		// In order to pass the 'parts', there must be at least one match.
		// I.e., an empty group definition contains nothing.
		int matches = 0;

		// Is this tsid's site in the site list?
		ArrayList<DbKey> siteIds = grp.getSiteIdList();
		boolean siteFound = false;
		if (siteIds.size() > 0)
		{
			DbKey tsSiteId = tsid.getSite().getId();
			for(DbKey siteId : siteIds)
				if (siteId.equals(tsSiteId))
				{
					siteFound = true;
					matches++;
					break;
				}
		}
		
		// MJM 2/2/2018 check for ObjectType match
		boolean objectTypeSpecified = false;
		boolean objectTypeFound = false;
		String tsidObjectType = tsid.getSite() == null ? null : tsid.getSite().getLocationType();

		if (tsidObjectType != null && tsidObjectType.trim().length() > 0)
		{
			ArrayList<TsGroupMember> otherParts = grp.getOtherMembers();
			for(TsGroupMember tgm : otherParts)
				if (tgm.getMemberType().equalsIgnoreCase("ObjectType"))
				{
					objectTypeSpecified = true;
					if (tsidObjectType.equalsIgnoreCase(tgm.getMemberValue()))
					{
						objectTypeFound = true;
						matches++;
						break;
					}
				}
		}
		
		if ((siteIds.size() > 0 || objectTypeSpecified)   // Either sites or object types specified
		 && !siteFound && !objectTypeFound)               // Neither was found
			return false;

		// Data Types are specified in this group?
		ArrayList<DbKey> dataTypeIds = grp.getDataTypeIdList();
		if (dataTypeIds.size() > 0) 
		{
			boolean found = false;
			for(DbKey dtId : dataTypeIds)
				if (dtId.equals(tsid.getDataTypeId()))
				{
					found = true;
					matches++;
					break;
				}
			if (!found)
				return false;
		}
			
		String parts[] = tsid.getParts();
		// The first two parts are always site (aka location) and datatype (aka param).
		// So skip those and check all the subsequent parts.
		for (int pi = 2; pi < parts.length; pi++)
		{
			ArrayList<TsGroupMember> otherParts = grp.getOtherMembers();
			boolean found = false;
			boolean thisPartSpecified = false;
			for(TsGroupMember tgm : otherParts)
			{
				if (tgm.getMemberType().equalsIgnoreCase(parts[pi]))
				{
					thisPartSpecified = true;
					String tsidVal = tsid.getPart(parts[pi]);
					if (tsidVal != null && tgm.getMemberValue().equalsIgnoreCase(tsidVal))
					{
						found = true;
						matches++;
						break;
					}
				}
			}
			if (thisPartSpecified && !found)
				return false;
		}
		
		
		// If at least one part matched, then this tsid passes.
		return matches > 0;
	}

	@Override
	protected void prepareForExpand(TsGroup grp) throws DbIoException
	{
	}
}
