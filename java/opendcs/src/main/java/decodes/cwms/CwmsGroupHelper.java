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
* $Id: CwmsGroupHelper.java,v 1.10 2019/12/11 14:36:19 mmaloney Exp $
*  This is open-source software written by Sutron Corporation, under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*/
package decodes.cwms;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.DataType;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.tsdb.GroupHelper;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;

public class CwmsGroupHelper extends GroupHelper
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

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
		log.trace("PrepareForExpand group {}, num SubParam specs: {}",
				  tsGroup.getGroupName(), tsGroup.getOtherMembers("SubParam").size());

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
				log.atWarn().setCause(ex).log("Cannot compile subloc '{}', pattern='{}'", subLoc, pat);
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
				log.atWarn().setCause(ex).log("Cannot compile baseloc '{}', patttern='{}'", baseLoc, pat);
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
				log.atWarn().setCause(ex).log("Cannot compile fullloc '{}', pattern='{}'", fullLoc, pat);
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
				log.trace("   Added SubParam pattern '{}'", pat);
			}
			catch(PatternSyntaxException ex)
			{
				log.atWarn().setCause(ex).log("Cannot compile subparam '{}', pattern='{}'", subPar, pat);
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
				log.trace("   Added BaseParam pattern '{}'", pat);
			}
			catch(PatternSyntaxException ex)
			{
				log.atWarn().setCause(ex).log("Cannot compile baseparam '{}', pattern='{}'", basePar, pat);
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
				log.trace("   Added FullParam pattern '{}'", pat);
			}
			catch(PatternSyntaxException ex)
			{
				log.atWarn().setCause(ex).log("Cannot compile fullparam '{}', pattern='{}'", fullPar, pat);
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
				log.atWarn().setCause(ex).log("Cannot compile subversion '{}', pattern='{}'", subVer, pat);
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
				log.atWarn().setCause(ex).log("Cannot compile baseversion '{}', pattern='{}'", baseVer, pat);
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
				log.trace("   Added FullVersion pattern '{}'", pat);
			}
			catch(PatternSyntaxException ex)
			{
				log.atWarn().setCause(ex).log("Cannot compile fullversion '{}', pattern='{}'", fullVer, pat);
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
				log.warn("Invalid TSID '{}' has no param part.", ctsid.getUniqueString());
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
							break;
						}
					}
				}
			}

			if (!versionPassed)
				return false;
			matches++;
		}

		// Passed all the checks and at least one was specified
		return matches > 0;
	}

}
