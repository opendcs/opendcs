/*
* $Id: CwmsGroupHelper.java,v 1.10 2019/12/11 14:36:19 mmaloney Exp $
*  This is open-source software written by Sutron Corporation, under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
* Open Source Software
* 
* $Log: CwmsGroupHelper.java,v $
* Revision 1.10  2019/12/11 14:36:19  mmaloney
* Don't explicitly require CwmsTimeSeriesDb. This module is also used with OpenTsdb.
*
* Revision 1.9  2017/12/04 18:58:38  mmaloney
* CWMS-10012 fixed CWMS problem that could sometimes result in circular dependencies
* for group computations when a new Time Series was created. When compdepends
* daemon evaluates the 'T' notification, it needs to prepare each CwmsGroupHelper for
* expansion so that the regular expressions exist.
*
* Revision 1.8  2017/08/22 19:29:49  mmaloney
* Improve comments
*
* Revision 1.7  2017/05/03 17:04:14  mmaloney
* Improved debugs.
*
* Revision 1.6  2017/04/27 21:01:55  mmaloney
* Combine full/base/sub location/param/version with logical OR.
*
* Revision 1.5  2017/04/19 19:23:35  mmaloney
* CWMS-10609 nested group evaluation in group editor bugfix.
*
* Revision 1.4  2017/01/10 21:14:43  mmaloney
* Enhanced wildcard processing for CWMS as per punchlist for comp-depends project
* for NWP.
*
* Revision 1.3  2016/11/03 18:59:41  mmaloney
* Implement wildcard evaluation for groups.
*
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
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;

public class CwmsGroupHelper
	extends GroupHelper
{
//	private boolean justPrimed = false;
	private static String regexSpecial = "<([{\\^-=$!|]})?+.>";
	private static String module = "CwmsGroupHelper";
	
	private ArrayList<Pattern> subLocPatterns = new ArrayList<Pattern>();
	private ArrayList<Pattern> subParamPatterns = new ArrayList<Pattern>();
	private ArrayList<Pattern> subVersionPatterns = new ArrayList<Pattern>();
	private ArrayList<Pattern> baseLocPatterns = new ArrayList<Pattern>();
	private ArrayList<Pattern> baseParamPatterns = new ArrayList<Pattern>();
	private ArrayList<Pattern> baseVersionPatterns = new ArrayList<Pattern>();
	private ArrayList<Pattern> fullLocPatterns = new ArrayList<Pattern>();
	private ArrayList<Pattern> fullParamPatterns = new ArrayList<Pattern>();
	private ArrayList<Pattern> fullVersionPatterns = new ArrayList<Pattern>();


	public CwmsGroupHelper(TimeSeriesDb tsdb)
	{
		super(tsdb);
		module = "CwmsGroupHelper";
	}
	
	@Override
	protected void prepareForExpand(TsGroup tsGroup) throws DbIoException
	{
		tsdb.debug2(module + ".prepareForExpand group " + tsGroup.getGroupName()
			+ ", num SubParam specs: " + tsGroup.getOtherMembers("SubParam").size());
//		justPrimed = true;
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
		
		baseLocPatterns.clear();
		ArrayList<String> baseLocs = tsGroup.getOtherMembers("BaseLocation");
		for(String baseLoc : baseLocs)
		{
			String pat = makePatternString("BaseLocation", baseLoc);
			try
			{
				baseLocPatterns.add(Pattern.compile(pat));
			}
			catch(PatternSyntaxException ex)
			{
				tsdb.warning(module + " cannot compile baseloc '" + baseLoc 
					+ "', pattern='" + pat + "': " + ex);
			}
		}
		
		fullLocPatterns.clear();
		ArrayList<String> fullLocs = tsGroup.getOtherMembers("Location");
		for(String fullLoc : fullLocs)
		{
			String pat = makePatternString("FullLocation", fullLoc);
			try
			{
				fullLocPatterns.add(Pattern.compile(pat));
			}
			catch(PatternSyntaxException ex)
			{
				tsdb.warning(module + " cannot compile fullloc '" + fullLoc 
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
				tsdb.debug2("   Added SubParam pattern '" + pat + "'");
			}
			catch(PatternSyntaxException ex)
			{
				tsdb.warning(module + " cannot compile subparam '" + subPar 
					+ "', pattern='" + pat + "': " + ex);
			}
		}

		baseParamPatterns.clear();
		ArrayList<String> basePars = tsGroup.getOtherMembers("BaseParam");
		for(String basePar : basePars)
		{
			String pat = makePatternString("BaseParam", basePar);
			try
			{
				baseParamPatterns.add(Pattern.compile(pat));
				tsdb.debug2("   Added BaseParam pattern '" + pat + "'");
			}
			catch(PatternSyntaxException ex)
			{
				tsdb.warning(module + " cannot compile baseparam '" + basePar 
					+ "', pattern='" + pat + "': " + ex);
			}
		}
		
		fullParamPatterns.clear();
		ArrayList<String> fullPars = tsGroup.getOtherMembers("Param");
		for(String fullPar : fullPars)
		{
			String pat = makePatternString("FullParam", fullPar);
			try
			{
				fullParamPatterns.add(Pattern.compile(pat));
				tsdb.debug2("   Added FullParam pattern '" + pat + "'");
			}
			catch(PatternSyntaxException ex)
			{
				tsdb.warning(module + " cannot compile fullparam '" + fullPar 
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
		
		baseVersionPatterns.clear();
		ArrayList<String> baseVers = tsGroup.getOtherMembers("BaseVersion");
		for(String baseVer : baseVers)
		{
			String pat = makePatternString("BaseVersion", baseVer);
			try
			{
				baseVersionPatterns.add(Pattern.compile(pat));
			}
			catch(PatternSyntaxException ex)
			{
				tsdb.warning(module + " cannot compile baseversion '" + baseVer 
					+ "', pattern='" + pat + "': " + ex);
			}
		}
		
		fullVersionPatterns.clear();
		ArrayList<String> fullVers = tsGroup.getOtherMembers("Version");
		for(String fullVer : fullVers)
		{
			String pat = makePatternString("FullVersion", fullVer);
			try
			{
				fullVersionPatterns.add(Pattern.compile(pat));
				tsdb.debug2("   Added FullVersion pattern '" + pat + "'");
			}
			catch(PatternSyntaxException ex)
			{
				tsdb.warning(module + " cannot compile fullversion '" + fullVer 
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
		
		return sb.toString();
	}

	@Override
	protected boolean passesParts(TsGroup tsGroup, TimeSeriesIdentifier tsid)
	{
		// In order to pass the 'parts', there must be at least one match.
		// I.e., an empty group definition contains nothing.
		int matches = 0;

		ArrayList<DbKey> siteIds = tsGroup.getSiteIdList();

		CwmsTsId ctsid = (CwmsTsId)tsid;
boolean testmode = ctsid.getVersion().equalsIgnoreCase("Combined-raw");
if (testmode) Logger.instance().debug2("TESTMODE for group=" 
+ tsGroup.getGroupName() + ", tsid=" + tsid.getUniqueString());

		// User could enter an actual site plus another site with wildcard, then
		// I would have one actual siteId and one pattern.
		if (siteIds.size() > 0             // Fully specified sites in the group (the old way).
		 || fullLocPatterns.size() > 0     // A full location spec with a wildcard
		 || baseLocPatterns.size() > 0     // A base location spec, possibly with wildcard
		 || subLocPatterns.size() > 0)     // A sub location spec, possibly with wildcard
		{
			boolean passedSite = false;
			
			// Check direct site (Location) links.
			if (siteIds.size() > 0 || fullLocPatterns.size() > 0)
			{
				for(DbKey siteId : siteIds)
					if (tsid.getSite() != null && siteId.equals(tsid.getSite().getId()))
					{
						passedSite = true;
						break;
					}
			}
			
			// If no match, check location with wildcard spec.
			if (!passedSite && fullLocPatterns.size() > 0)
			{
				String tsidfullLoc = ctsid.getPart("location");
				if (tsidfullLoc != null && tsidfullLoc.length() > 0)
				{
					tsidfullLoc = tsidfullLoc.toUpperCase();
					for(Pattern slp : fullLocPatterns)
					{
						Matcher m = slp.matcher(tsidfullLoc);
						if (m.matches())
						{
							passedSite = true;
							break;
						}
					}
				}
			}
			
			// Still no match, check base location specs, possibly with wildcards.
			if (!passedSite && baseLocPatterns.size() > 0)
			{			
				String tsidbaseLoc = ctsid.getBaseLoc();
				if (tsidbaseLoc != null && tsidbaseLoc.length() > 0)
				{
					tsidbaseLoc = tsidbaseLoc.toUpperCase();
					for(Pattern slp : baseLocPatterns)
					{
						Matcher m = slp.matcher(tsidbaseLoc);
						if (m.matches())
						{
							passedSite = true;
							break;
						}
					}
				}
			}
			
			// Still no match, check sub location specs.
			if (!passedSite && subLocPatterns.size() > 0)
			{			
				String tsidSubLoc = ctsid.getSubLoc();
				if (tsidSubLoc != null && tsidSubLoc.length() > 0)
				{
					tsidSubLoc = tsidSubLoc.toUpperCase();
					for(Pattern slp : subLocPatterns)
					{
						Matcher m = slp.matcher(tsidSubLoc);
						if (m.matches())
						{
							passedSite = true;
							break;
						}
					}
				}
			}
				
			if (!passedSite)
				return false; // None of the ways of referencing a location match.
			matches++;
		}
if (testmode) Logger.instance().debug2("...passed location filter.");

		ArrayList<DbKey> dtIds = tsGroup.getDataTypeIdList();
		if (dtIds.size() > 0                 // Full data type (CWMS Param) specified
		 || fullParamPatterns.size() > 0     // Full Param spec possibly with wildcard
		 || baseParamPatterns.size() > 0     // Base Param spec possibly with wildcard
		 || subParamPatterns.size() > 0)     // Sub Param spec possibly with wildcard
		{
			boolean passedDT = false;
			
			String tsidFullParam = ctsid.getPart("param");
			if (tsidFullParam == null || tsidFullParam.trim().length() == 0)
			{
				Logger.instance().warning(module + " invalid TSID '" + ctsid.getUniqueString() 
					+ "' has no param part.");
				return false;
			}

			// Do a text compare of the param to the data types in the list. We have
			// seen cases where multiple datatypes differ only in case but have different
			// surrogate keys.
			for(DbKey dtId : dtIds)
			{
				DataType dt = DataType.getDataType(dtId);
				if (dt != null && dt.getCode().equalsIgnoreCase(tsidFullParam))
				{
					passedDT = true;
					break;
				}
			}

			// No match for data type, check full param with wildcard specs.
			if (!passedDT && fullParamPatterns.size() > 0)
			{
				tsidFullParam = tsidFullParam.toUpperCase();
				for(Pattern sp : fullParamPatterns)
				{
					Matcher m = sp.matcher(tsidFullParam);
					if (m.matches())
					{
						passedDT = true;
						break;
					}
				}
			}
			
			// Still no match. Check base param with wildcard.
			if (!passedDT && baseParamPatterns.size() > 0)
			{			
				String tsidbase = ctsid.getBaseParam();
				if (tsidbase != null && tsidbase.length() > 0)
				{
					tsidbase = tsidbase.toUpperCase();
					for(Pattern sp : baseParamPatterns)
					{
						Matcher m = sp.matcher(tsidbase);
						if (m.matches())
						{
							passedDT = true;
							break;
						}
					}
				}
			}

			// Still no match. Check sub param with wildcard.
			if (!passedDT && subParamPatterns.size() > 0)
			{			
				String tsidSub = ctsid.getSubParam();
				if (tsidSub != null && tsidSub.length() > 0)
				{
					tsidSub = tsidSub.toUpperCase();
					for(Pattern sp : subParamPatterns)
					{
						Matcher m = sp.matcher(tsidSub);
						if (m.matches())
						{
							passedDT = true;
							break;
						}
					}
				}
			}

			// No match on any of the ways of specifying param.
			if (!passedDT)
				return false;
			matches++;
		}
		
if (testmode) Logger.instance().debug2("...passed param filter.");
		
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

if (testmode) Logger.instance().debug2("...passed paramtype filter.");

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
		
if (testmode) Logger.instance().debug2("...passed interval filter.");

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
		
if (testmode) Logger.instance().debug2("...passed duration filter.");

if (testmode) Logger.instance().debug2("...numFull=" + fullVersionPatterns.size()
	+ ", numsub=" + subVersionPatterns.size() + ", numbase=" + baseVersionPatterns.size());


		if (fullVersionPatterns.size() > 0
		 || subVersionPatterns.size() > 0
		 || baseVersionPatterns.size() > 0)
		{
			boolean versionPassed = false;

			// fullVersion uses regex to handle wildcards
			if (fullVersionPatterns.size() > 0)
			{			
				String tsidfull = ctsid.getVersion();
				if (tsidfull != null && tsidfull.length() > 0)
				{
					tsidfull = tsidfull.toUpperCase();
					for(Pattern sp : fullVersionPatterns)
					{
						Matcher m = sp.matcher(tsidfull);
						if (m.matches())
						{
							versionPassed = true;
if (testmode)
Logger.instance().debug2("TSID '" + ctsid.getUniqueString() + "' passes full version filter in group "
	+ tsGroup.getGroupName() + " regex='" + sp.toString() + "'");
							break;
						}
					}
				}
			}
			
			if (!versionPassed && subVersionPatterns.size() > 0)
			{			
				String tsidSub = ctsid.getSubVersion();
				if (tsidSub != null && tsidSub.length() > 0)
				{
					tsidSub = tsidSub.toUpperCase();
					for(Pattern sp : subVersionPatterns)
					{
						Matcher m = sp.matcher(tsidSub);
						if (m.matches())
						{
							versionPassed = true;
if (testmode)
	Logger.instance().debug2("TSID '" + ctsid.getUniqueString() + "' passes sub version filter in group "
		+ tsGroup.getGroupName() + " regex='" + sp.toString() + "'");

							break;
						}
					}
				}
			}
		
			if (!versionPassed && baseVersionPatterns.size() > 0)
			{			
				String tsidbase = ctsid.getBaseVersion();
				if (tsidbase != null && tsidbase.length() > 0)
				{
					tsidbase = tsidbase.toUpperCase();
					for(Pattern sp : baseVersionPatterns)
					{
						Matcher m = sp.matcher(tsidbase);
						if (m.matches())
						{
							versionPassed = true;
if (testmode)
	Logger.instance().debug2("TSID '" + ctsid.getUniqueString() + "' passes base version filter in group "
		+ tsGroup.getGroupName() + " regex='" + sp.toString() + "'");

							break;
						}
					}
				}
			}

if (testmode) Logger.instance().debug2("Result of version filter=" + versionPassed);

			if (!versionPassed)
				return false;
			matches++;
		}

		// Passed all the checks and at least one was specified
		return matches > 0;
	}

}
