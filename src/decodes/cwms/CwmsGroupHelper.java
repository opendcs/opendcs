/*
* $Id$
*  This is open-source software written by Sutron Corporation, under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
* Open Source Software
* 
* $Log$
* Revision 1.2  2016/04/22 14:46:21  mmaloney
* Fix subgroup evaluation. Make it true recursion.
*
* Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
* OPENDCS 6.0 Initial Checkin
*
* Revision 1.10  2013/04/23 13:25:23  mmaloney
* Office ID filtering put back into Java.
*
* Revision 1.9  2013/03/21 18:27:40  mmaloney
* DbKey Implementation
*
* Revision 1.8  2012/07/05 18:24:23  mmaloney
* tsKey is stored as a long.
*
* Revision 1.7  2011/04/27 16:12:38  mmaloney
* comment
*
* Revision 1.6  2011/04/19 19:14:10  gchen
* (1) Add a line to set Site.explicitList = true in cwmsTimeSeriesDb.java to fix the multiple location entries on Location Selector in TS Group GUI.
*
* (2) Fix a bug in getDataType(String standard, String code, int id) method in decodes.db.DataType.java because the data id wasn't set up previously.
*
* (3) Fix the null point exception in line 154 in cwmsGroupHelper.java.
*
* Revision 1.5  2011/02/04 21:30:48  mmaloney
* Intersect groups
*
* Revision 1.4  2011/02/03 20:00:23  mmaloney
* Time Series Group Editor Mods
*
* Revision 1.3  2011/01/27 19:07:11  mmaloney
* When expanding group, must compare UPPER for all path string components.
*
* Revision 1.2  2011/01/12 18:57:16  mmaloney
* dev
*
* Revision 1.1  2010/11/28 21:05:25  mmaloney
* Refactoring for CCP Time-Series Groups
*
*/
package decodes.cwms;

import ilex.util.Logger;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import decodes.db.DataType;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.tsdb.GroupHelper;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;

/**
This class is a helper to the TempestTsdb for reading & writing groups.
*/
public class CwmsGroupHelper
	extends GroupHelper
{
	private boolean justPrimed = false;
	private ArrayList<Pattern> subLocPatterns = new ArrayList<Pattern>();
	private static String regexSpecial = "<([{\\^-=$!|]})?+.>";
	private static String module = "CwmsGroupHelper";
	private ArrayList<Pattern> subParamPatterns = new ArrayList<Pattern>();
	private ArrayList<Pattern> subVersionPatterns = new ArrayList<Pattern>();


	public CwmsGroupHelper(CwmsTimeSeriesDb tsdb)
	{
		super(tsdb);
tsdb.debug1("CwmsGroupHelper ctor");
	}
	
	@Override
	protected void prepareForExpand(TsGroup tsGroup) throws DbIoException
	{
tsdb.debug1("CwmsGroupHelper.prepareForExpand group " + tsGroup.getGroupName());
		justPrimed = true;
		// Create and compile the regex objects for subloc, subparam, and subversion.
		subLocPatterns.clear();
		ArrayList<String> subLocs = tsGroup.getOtherMembers("SubLocation");
		for(String subLoc : subLocs)
		{
			String pat = makePatternString("SubLocation", subLoc);
			try
			{
				subLocPatterns.add(Pattern.compile(pat));
			}
			catch(PatternSyntaxException ex)
			{
				tsdb.warning(module + " cannot compile subloc '" + subLoc 
					+ "', pattern='" + pat + "': " + ex);
			}
		}
		
		subParamPatterns.clear();
		ArrayList<String> subPars = tsGroup.getOtherMembers("SubParam");
		for(String subPar : subPars)
		{
			String pat = makePatternString("SubParam", subPar);
			try
			{
				subParamPatterns.add(Pattern.compile(pat));
			}
			catch(PatternSyntaxException ex)
			{
				tsdb.warning(module + " cannot compile subparam '" + subPar 
					+ "', pattern='" + pat + "': " + ex);
			}
		}

		
		
		subVersionPatterns.clear();
		ArrayList<String> subVers = tsGroup.getOtherMembers("SubVersion");
		for(String subVer : subVers)
		{
			String pat = makePatternString("SubVersion", subVer);
			try
			{
				subVersionPatterns.add(Pattern.compile(pat));
			}
			catch(PatternSyntaxException ex)
			{
				tsdb.warning(module + " cannot compile subversion '" + subVer 
					+ "', pattern='" + pat + "': " + ex);
			}
		}
	}
	
	/**
	 * Convert the subpart string provided by the user into a string suitable for a
	 * Java Pattern class. This involves converting asterisk into the pattern
	 * "[^-]+", meaning one or more occurances of a non-hyphen character. Other
	 * special characters are escaped.
	 * Also, normal alpha chars are converted to upper case so we can do a case insensitive
	 * compare.
	 * @param subpart the subpart specified by the user
	 * @return the string suitable for a Java pattern matcher
	 */
	private String makePatternString(String label, String subpart)
	{
		StringBuilder sb = new StringBuilder("^");
		for(int idx = 0; idx < subpart.length(); idx++)
		{
			char c = subpart.charAt(idx);
			if (c == '*')
				sb.append("[^-]+");
			else if (regexSpecial.indexOf(c) >= 0)
				sb.append("\\" + c);
			else if (Character.isLowerCase(c))
				sb.append(Character.toUpperCase(c));
			else
				sb.append(c);
		}
		sb.append("$");
Logger.instance().debug1("\t" + label + " input='" + subpart + "' converted='" + sb.toString() + "'");
		
		return sb.toString();
	}

	@Override
	protected boolean passesParts(TsGroup tsGroup, TimeSeriesIdentifier tsid)
	{
		// In order to pass the 'parts', there must be at least one match.
		// I.e., an empty group definition contains nothing.
		int matches = 0;

		ArrayList<DbKey> siteIds = tsGroup.getSiteIdList();
		if (justPrimed)
		{
			tsdb.debug1("CwmsGroupHelper.passesParts: Group=" + tsGroup.getGroupName() 
				+ ", #sites=" + siteIds.size() + ", tsidSiteId=" 
				+ (tsid.getSite() == null ? "null" : 
					("ID=" + tsid.getSite().getId() + ", " + tsid.getSite().getPreferredName().getNameValue())));
for(DbKey sid : siteIds)
	tsdb.debug1("    siteID=" + sid);
			justPrimed = false;
		}
		if (siteIds.size() > 0)
		{
			boolean passedSite = false;
			for(DbKey siteId : siteIds)
				if (tsid.getSite() != null && siteId.equals(tsid.getSite().getId()))
				{
					passedSite = true;
					break;
				}
			if (!passedSite)
				return false;
			matches++;
		}
		
		CwmsTsId ctsid = (CwmsTsId)tsid;

		ArrayList<DbKey> dtIds = tsGroup.getDataTypeIdList();
		if (dtIds.size() > 0)
		{
			boolean passedDT = false;
			for(DbKey dtId : dtIds)
				if (tsid.getDataTypeId() != null && dtId.equals(tsid.getDataTypeId()))
				{
					passedDT = true;
					break;
				}
			// Extra check for Param: There may be multiple data types with the
			// same code for legacy reasons.
			if (!passedDT)
			{
				DataType tsidDt = ctsid.getDataType();
				if (tsidDt != null)
					for(DbKey dtId : dtIds)
					{
						DataType dt = DataType.getDataType(dtId);
						if (dt != null && dt.getCode().equalsIgnoreCase(tsidDt.getCode()))
						{
							passedDT = true;
						}
					}
			}
	
			if (!passedDT)
				return false;
			matches++;
		}
		
		
		ArrayList<String> paramTypes = tsGroup.getOtherMembers("ParamType");
		if (paramTypes.size() > 0)
		{
			boolean passedPT = false;
			for(String paramType : paramTypes)
				if (ctsid.getParamType() != null && paramType.equalsIgnoreCase(ctsid.getParamType()))
				{
					passedPT = true;
					break;
				}
			if (!passedPT)
				return false;
			matches++;
		}

		ArrayList<String> intervals = tsGroup.getOtherMembers("Interval");
		if (intervals.size() > 0)
		{
			boolean passedIntv = false;
			for(String interval : intervals)
				if (ctsid.getInterval() != null && interval.equalsIgnoreCase(ctsid.getInterval()))
				{
					passedIntv = true;
					break;
				}
			if (!passedIntv)
				return false;
			matches++;
		}

		ArrayList<String> durations = tsGroup.getOtherMembers("Duration");
		if (durations.size() > 0)
		{
			boolean passedDur = false;
			for(String dur : durations)
				if (ctsid.getDuration() != null && dur.equalsIgnoreCase(ctsid.getDuration()))
				{
					passedDur = true;
					break;
				}
			if (!passedDur)
				return false;
			matches++;
		}

		ArrayList<String> versions = tsGroup.getOtherMembers("Version");
		if (versions.size() > 0)
		{
			boolean passedVer = false;
			for(String ver : versions)
				if (ctsid.getVersion() != null && ver.equalsIgnoreCase(ctsid.getVersion()))
				{
					passedVer = true;
					break;
				}
			if (!passedVer)
				return false;
			matches++;
		}
		
		ArrayList<String> baseLocs = tsGroup.getOtherMembers("BaseLocation");
		if (baseLocs.size() > 0)
		{
			boolean passed = false;
			for(String bl : baseLocs)
				if (ctsid.getBaseLoc() != null && bl.equalsIgnoreCase(ctsid.getBaseLoc()))
				{
					passed = true;
					break;
				}
			if (!passed)
				return false;
			matches++;
		}

		// Subloc uses regex to handle wildcards
		if (subLocPatterns.size() > 0)
		{			
			boolean passed = false;
			String tsidSubLoc = ctsid.getSubLoc();
			if (tsidSubLoc != null && tsidSubLoc.length() > 0)
			{
				tsidSubLoc = tsidSubLoc.toUpperCase();
				for(Pattern slp : subLocPatterns)
				{
					Matcher m = slp.matcher(tsidSubLoc);
					if (m.matches())
					{
						passed = true;
						break;
					}
				}
			}
			if (!passed)
				return false;
			matches++;
		}
		
		ArrayList<String> baseParams = tsGroup.getOtherMembers("BaseParam");
		if (baseParams.size() > 0)
		{
			boolean passed = false;
			for(String bp : baseParams)
				if (ctsid.getBaseParam() != null && bp.equalsIgnoreCase(ctsid.getBaseParam()))
				{
					passed = true;
					break;
				}
			if (!passed)
				return false;
			matches++;
		}

		// SubParam uses regex to handle wildcards
		if (subParamPatterns.size() > 0)
		{			
			boolean passed = false;
			String tsidSub = ctsid.getSubParam();
			if (tsidSub != null && tsidSub.length() > 0)
			{
				tsidSub = tsidSub.toUpperCase();
				for(Pattern sp : subParamPatterns)
				{
					Matcher m = sp.matcher(tsidSub);
					if (m.matches())
					{
						passed = true;
						break;
					}
				}
			}
			if (!passed)
				return false;
			matches++;
		}

		ArrayList<String> baseVersions = tsGroup.getOtherMembers("BaseVersion");
		if (baseVersions.size() > 0)
		{
			boolean passed = false;
			for(String bv : baseVersions)
				if (ctsid.getBaseVersion() != null && bv.equalsIgnoreCase(ctsid.getBaseVersion()))
				{
					passed = true;
					break;
				}
			if (!passed)
				return false;
			matches++;
		}
		
		
		// SubVersion uses regex to handle wildcards
		if (subVersionPatterns.size() > 0)
		{			
			boolean passed = false;
			String tsidSub = ctsid.getSubVersion();
			if (tsidSub != null && tsidSub.length() > 0)
			{
				tsidSub = tsidSub.toUpperCase();
				for(Pattern sp : subVersionPatterns)
				{
					Matcher m = sp.matcher(tsidSub);
					if (m.matches())
					{
						passed = true;
						break;
					}
				}
			}
			if (!passed)
				return false;
			matches++;
		}


		// Passed all the checks.
		return matches > 0;
	}

}
