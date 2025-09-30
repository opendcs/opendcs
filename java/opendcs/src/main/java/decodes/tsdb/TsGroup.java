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

import java.util.ArrayList;
import java.util.Iterator;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import opendcs.dao.CachableDbObject;

import decodes.db.Constants;
import decodes.db.SiteName;
import decodes.sql.DbKey;
import decodes.tsdb.xml.CompXioTags;

/**
 *
 * This class encapsulates information about TimeSeries Groups.
 *
 */
public class TsGroup implements CompMetaData, CachableDbObject
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private DbKey groupId = Constants.undefinedId;
	private String groupName = null;
	private String groupType = null;
	private String description = "";

	/** Sites that are members */
	private ArrayList<DbKey> siteIdList = new ArrayList<DbKey>();

	/** Data Types that are members */
	private ArrayList<DbKey> dataTypeIdList = new ArrayList<DbKey>();

	/** Explicit time series that are members */
	private ArrayList<TimeSeriesIdentifier> tsMemberList =
		new ArrayList<TimeSeriesIdentifier>();

	/** explicit time series members as strings, used by xml import/export programs only */
	private ArrayList<String> tsMemberIDList = new ArrayList<String>();

	/** Other member types (statcode, interval, paramtype, version) */
	private ArrayList<TsGroupMember> otherMembers = new ArrayList<TsGroupMember>();

	/** Sub-groups to include */
	private ArrayList<TsGroup> includeGroups = new ArrayList<TsGroup>();

	/** Sub-groups to exclude */
	private ArrayList<TsGroup> excludeGroups = new ArrayList<TsGroup>();

	/** Sub-groups to intersect with this one */
	private ArrayList<TsGroup> intersectedGroups = new ArrayList<TsGroup>();

	/**Array list of site names in 'Nametype-Name' format . eg  'local-HNKM2'  **/
	private ArrayList<String> siteNameList = new ArrayList<String>();
	private boolean _isExpanded = false;
	private boolean _isTransient = false;
	private ArrayList<TimeSeriesIdentifier> expandedList =
		new ArrayList<TimeSeriesIdentifier>();

	/**
	 * Default Constructor makes an empty group.
	 */
	public TsGroup()
	{
	}

	/**
	 * Makes a copy of this group, with a new name and a -1 ID.
	 * @param newName the name to give the new group.
	 * @return a copy of this group, with a new name and a -1 ID.
	 */
	@SuppressWarnings("unchecked")
	public TsGroup copy(String newName)
	{
		TsGroup g = new TsGroup();
		g.groupId = Constants.undefinedId;
		g.groupName = newName;
		g.groupType = this.groupType;
		g.description = this.description;

		g.expandedList = new ArrayList<TimeSeriesIdentifier>();
		for(TimeSeriesIdentifier tsid : this.expandedList)
			g.expandedList.add(tsid);

		g.includeGroups = (ArrayList<TsGroup>)includeGroups.clone();
		g.dataTypeIdList = (ArrayList<DbKey>)dataTypeIdList.clone();
		g.siteIdList = (ArrayList<DbKey>)siteIdList.clone();
		g._isExpanded = this._isExpanded;
		g._isTransient = this._isTransient;
		g.siteNameList = (ArrayList<String>)this.siteNameList.clone();

		g.tsMemberList = (ArrayList<TimeSeriesIdentifier>)tsMemberList.clone();
		g.otherMembers = (ArrayList<TsGroupMember>)otherMembers.clone();
		g.excludeGroups = (ArrayList<TsGroup>)excludeGroups.clone();
		g.intersectedGroups = (ArrayList<TsGroup>)intersectedGroups.clone();

		g.siteNameList = (ArrayList<String>)siteNameList.clone();
		g._isExpanded = false;

		return g;
	}

	/**
	 * Clears all entries from this group.
	 * Called before (re)reading from database.
	 */
	public void clear()
	{
		expandedList.clear();
		includeGroups.clear();
		dataTypeIdList.clear();
		siteIdList.clear();
		tsMemberList.clear();
		otherMembers.clear();
		siteNameList.clear();
		_isExpanded = false;
		excludeGroups.clear();
		tsMemberIDList.clear();
	}

	/**
	 * @return surrogate database key for this group, or Constants.undefinedId
	 * if this group is not yet saved to database.
	 */
	public DbKey getGroupId()
	{
		return groupId;
	}

	/**
	 * Set surrogate key for this group.
	 */
	public void setGroupId(DbKey groupId)
	{
		this.groupId = groupId;
	}


	/** @return group name */
	public String getGroupName()
	{
		return groupName;
	}

	/** Sets group name */
	public void setGroupName(String groupName)
	{
		this.groupName = groupName;
	}


	/**
	 * @return the string representing the type of this group
	 */
	public String getGroupType()
	{
		return groupType;
	}

	/**
	 * Set the group type
	 */
	public void setGroupType(String groupType)
	{
		this.groupType = groupType;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		if (description == null)
			description = "";
		this.description = description;
	}

	/** Adds a site ID to this group */
	public void addSiteId(DbKey id)
	{
		siteIdList.add(id);
	}

	/** @return the list of site ids */
	public ArrayList<DbKey> getSiteIdList()
	{
		return siteIdList;
	}

	public ArrayList<String> getSiteNameList()
	{
		return siteNameList;
	}

	public void addDataTypeId(DbKey id)
	{
		dataTypeIdList.add(id);
	}

	public ArrayList<DbKey> getDataTypeIdList()
	{
		return dataTypeIdList;
	}

	/**
	 * Add an 'other' member. Type should match one of the time-series
	 * identifier parts return by db.getTsIdParts.
	 * @param type the type of this member (e.g. 'interval', 'version')
	 * @param value The value of the member
	 */
	public void addOtherMember(String type, String value)
	{
		rmOtherMember(type, value);
		otherMembers.add(new TsGroupMember(type, value));
	}

	/**
	 * @return the entire list of other members
	 */
	public ArrayList<TsGroupMember> getOtherMembers()
	{
		return otherMembers;
	}

	/**
	 * Returns a new (non persistent) array of other members of a given
	 * type. For example, to get a list of all 'interval' memebers call
	 * with type="interval"
	 * @param type the desired type
	 * @return a new non-persistent array of members of the specified type
	 */
	public ArrayList<String> getOtherMembers(String type)
	{
		ArrayList<String> ret = new ArrayList<String>();
		for(TsGroupMember tgm : otherMembers)
			if (tgm.getMemberType().equalsIgnoreCase(type))
				ret.add(tgm.getMemberValue());
		return ret;
	}


	/** @return the list of data descriptor objects */
	public ArrayList<TimeSeriesIdentifier> getExpandedList()
	{
		return expandedList;
	}

	/** Clears the expanded list */
	public void clearExpandedList()
	{
		expandedList.clear();
	}


	/**
	 * Removes the matching other member, returns true if found and removed.
	 * @param type the member type to remove
	 * @param value the member value to remove
	 * @return true if member found and removed, false if no match
	 */
	public boolean rmOtherMember(String type, String value)
	{
		for(Iterator<TsGroupMember> tgmit = otherMembers.iterator();
			tgmit.hasNext(); )
		{
			TsGroupMember tgm = tgmit.next();
			if (tgm.getMemberType().equalsIgnoreCase(type)
			 && tgm.getMemberValue().equalsIgnoreCase(value))
			{
				tgmit.remove();
				return true;
			}
		}
		return false;
	}

	/**
	 * Return a non-persistent set of interval codes in this group.
	 * Changes to the returned collection are not saved!
	 * @return a non-persistent set of interval codes in this group.
	 */
	public ArrayList<String> getIntervalCodeList()
	{
		return getOtherMembers(TsGroupMemberType.Interval.toString());
	}

	/**
	 * Return a non-persistent set of ParamType codes in this group.
	 * Changes to the returned collection are not saved!
	 * @return a non-persistent set of ParamType codes in this group.
	 */
	public ArrayList<String> getParamTypeList()
	{
		return getOtherMembers(TsGroupMemberType.ParamType.toString());
	}

	/**
	 * Return a non-persistent set of Duration codes in this group.
	 * Changes to the returned collection are not saved!
	 * @return a non-persistent set of Duration codes in this group.
	 */
	public ArrayList<String> getDurationList()
	{
		return getOtherMembers(TsGroupMemberType.Duration.toString());
	}

	/**
	 * Return a non-persistent set of Version codes in this group.
	 * Changes to the returned collection are not saved!
	 * @return a non-persistent set of Version codes in this group.
	 */
	public ArrayList<String> getVersionList()
	{
		return getOtherMembers(TsGroupMemberType.Version.toString());
	}

	/**
	 * Adds a sub-group to this group. If 'include' is true then the members
	 * of the sub-group are included to this group. If false, then the
	 * members of the sub-groups are excluded.
	 * @param subGroupMember the sub-group
	 * @param combine one of 'A'=add, 'S'=subtract, 'I'=intersect
	 */
	public void addSubGroup(TsGroup subGroupMember, char combine)
	{
		combine = Character.toUpperCase(combine);
		log.trace("TsGroup.addSubGroup({},{}), this group={}",
				  subGroupMember.getGroupName(), combine, this.groupName);
		if (combine == 'S' || combine == 'F')
			excludeGroups.add(subGroupMember);
		else if (combine == 'I')
			intersectedGroups.add(subGroupMember);
		else
			includeGroups.add(subGroupMember);
	}

	/**
	 * @return list of sub groups that are included in this group.
	 */
	public ArrayList<TsGroup> getIncludedSubGroups()
	{
		return includeGroups;
	}

	/**
	 * @return list of sub groups that are excluded from this group.
	 */
	public ArrayList<TsGroup> getExcludedSubGroups()
	{
		return excludeGroups;
	}

	/**
	 * Adds an explicit time-series member
	 * @param tsid the time series identifier
	 */
	public void addTsMember(TimeSeriesIdentifier tsid)
	{
		tsMemberList.add(tsid);
	}

	public void addTsMemberID(String tsidStr)
	{
		tsMemberIDList.add(tsidStr);
	}

	/**
	 * @return the list of explicit time-series members
	 */
	public ArrayList<TimeSeriesIdentifier> getTsMemberList()
	{
		return tsMemberList;
	}

	public ArrayList<String> getTsMemberIDList()
	{
		return tsMemberIDList;
	}


	/**
	 * Adds a time series to the expanded list.
	 * Before use, the list is evaluated by the database code and all time
	 * series that are determined to be members are added to the 'expanded'
	 * list using this method.
	 */
	public void addToExpandedList(TimeSeriesIdentifier tsid)
	{
		rmFromExpandedList(tsid);
		expandedList.add(tsid);
	}

	public void rmFromExpandedList(TimeSeriesIdentifier tsid)
	{
		for(Iterator<TimeSeriesIdentifier> tsidit = expandedList.iterator();
			tsidit.hasNext(); )
		{
			TimeSeriesIdentifier tt = tsidit.next();
			if (tt.getKey() == tsid.getKey())
				tsidit.remove();
		}
	}

	/**
	 * Return true if the expanded list contains the passed TSID.
	 * @param tsid the TSID to check
	 * @return true if the expanded list contains the passed TSID
	 */
	public boolean expandedListContains(TimeSeriesIdentifier tsid)
	{
		for(TimeSeriesIdentifier exTsid : expandedList)
			if (exTsid.getKey() == tsid.getKey()
			 || exTsid.getUniqueString().equalsIgnoreCase(tsid.getUniqueString()))
				return true;
		return false;
	}

	/**
	 * @return true if this list has been expanded.
	 */
	public boolean getIsExpanded()
	{
		return _isExpanded;
	}

	/** Sets the isExpanded flag */
	public void setIsExpanded(boolean tf)
	{
		_isExpanded = tf;
	}

	/**
	 * A 'transient' group is one that is built programmatically and not
	 * stored in the database.
	 */
	public void setTransient() { _isTransient = true; }

	/** @return true if this group is transient (i.e. not stored in DB) */
	public boolean isTransient() { return _isTransient; }



	/**
	 * Called after group expanded and then made more restrictive.
	 * Go through current list of DataDescriptors and delete the ones that
	 * are no longer in the group.
	 */
	public void refilter(TimeSeriesDb tsdb)
	{
		for(int i = 0; i < expandedList.size(); )
		{
			TimeSeriesIdentifier tsid = expandedList.get(i);
			if (dataTypeIdList.size() > 0)
			{
				boolean dtOK = false;
				DbKey dtid = tsid.getDataTypeId();
				for(DbKey I : dataTypeIdList)
					if (dtid.equals(I))
					{
						dtOK = true;
						break;
					}
				if (!dtOK)
				{
					expandedList.remove(i);
					continue;
				}
			}

			if (siteNameList.size() > 0)
			{
				boolean siteNameOK = false;
				for(String s : siteNameList)
				{
					int hyphen = s.indexOf("-");
					if (hyphen <= 0)
						continue;
					String nameType = s.substring(0,hyphen);
					if (nameType == null || nameType.length()==0)
						continue;
					String nameValue = s.substring(hyphen+1);
					if (nameValue == null || nameValue.length()==0)
						continue;

					//retrieve name of site
					SiteName sn = tsid.getSite().getName(nameType);
					if (sn != null
					 && sn.getNameValue().equalsIgnoreCase(nameValue))
					{
						siteNameOK = true;
						break;
					}
				}
				if (!siteNameOK)
				{
					expandedList.remove(i);
					continue;
				}
			}

			String parts[] = tsdb.getTsIdParts();
			for(String part : parts)
			{
				if (part.equalsIgnoreCase("Site")
				 || part.equalsIgnoreCase("Location")
				 || part.equalsIgnoreCase("DataType")
				 || part.equalsIgnoreCase("Param"))
					continue;
				ArrayList<String> partMembers = getOtherMembers(part);
				if (partMembers.size() > 0)
				{
					boolean partOK = false;
					for(String partMember : partMembers)
					{
						String tsPart = tsid.getPart(part);
						if (partMember.equalsIgnoreCase(tsPart))
						{
							partOK = true;
							break;
						}
					}
					if (!partOK)
					{
						expandedList.remove(i);
						continue;
					}
				}
			}

			// Fell through all the checks, go on to next TSID
			i++;
		}
	}

	public void addSiteName(String sitename)
	{
		siteNameList.add(sitename);
	}

	/* (non-Javadoc)
	 * @see decodes.tsdb.CompMetaData#getObjectType()
	 */
	@Override
	public String getObjectType()
	{
		return CompXioTags.tsGroup;
	}

	/* (non-Javadoc)
	 * @see decodes.tsdb.CompMetaData#getObjectName()
	 */
	@Override
	public String getObjectName()
	{
		return groupName;
	}

	/**
	 * Determine if theGroup exists in the subgroup list
	 *
	 * @param theGroup
	 * @return true if exist; otherwise, false.
	 */
	public boolean hasSubgroup(TsGroup theGroup)
	{
		for (TsGroup g: includeGroups)
			if (g != null && g.getGroupName().equals(theGroup.getGroupName()))
				return true;
		for (TsGroup g: excludeGroups)
			if (g != null && g.getGroupName().equals(theGroup.getGroupName()))
				return true;
		for (TsGroup g: intersectedGroups)
			if (g != null && g.getGroupName().equals(theGroup.getGroupName()))
				return true;

		return false;
	}

	public ArrayList<TsGroup> getIntersectedGroups()
	{
		return intersectedGroups;
	}

	public void setIntersectedGroups(ArrayList<TsGroup> intersectedGroups)
	{
		this.intersectedGroups = intersectedGroups;
	}

	public boolean isInExpandedList(DbKey tsKey)
	{
		for(TimeSeriesIdentifier tsid : expandedList)
			if (tsid.getKey().equals(tsKey))
				return true;
		return false;
	}

	@Override
	public DbKey getKey()
	{
		return groupId;
	}

	@Override
	public String getUniqueName()
	{
		return groupName;
	}

}