/*
 * $Id: AlarmExport.java,v 1.4 2020/02/27 22:09:38 mmaloney Exp $
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log: AlarmExport.java,v $
 * Revision 1.4  2020/02/27 22:09:38  mmaloney
 * Bug fix: No args should export everything.
 *
 * Revision 1.3  2019/10/21 14:06:36  mmaloney
 * Fix incorrect dependency on hec library.
 *
 * Revision 1.2  2019/05/10 18:35:26  mmaloney
 * dev
 *
 * Revision 1.1  2019/03/05 14:53:01  mmaloney
 * Checked in partial implementation of Alarm classes.
 *
 * Revision 1.1  2017/03/21 12:17:10  mmaloney
 * First working XML and SQL I/O.
 *
 */
package decodes.tsdb.alarm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;

import opendcs.dai.AlarmDAI;
import opendcs.dao.AlarmDAO;
import lrgs.gui.DecodesInterface;
import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.TextUtil;
import decodes.db.DataType;
import decodes.db.Site;
import decodes.sql.DbKey;
import decodes.tsdb.TsdbAppTemplate;
import decodes.tsdb.alarm.xml.AlarmXio;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesSettings;

/**
 * Export named alarm group to XML file. Writes to stdout.
 * @author mmaloney
 *
 */
public class AlarmExport
	extends TsdbAppTemplate
{
	private BooleanToken currentOnly = new BooleanToken("C", "Current Alarms Only",
		"", TokenOptions.optSwitch, false);
	private StringToken datatypeArg = new StringToken("T", "DataType",
			"", TokenOptions.optSwitch | TokenOptions.optMultiple, ""); 
	private BooleanToken inclFileProcArg = new BooleanToken("F", "Include File and Process Alarms",
			"", TokenOptions.optSwitch, false);
	private StringToken grpNameArg = new StringToken("G", "Alarm Group Name",
			"", TokenOptions.optSwitch | TokenOptions.optMultiple, ""); 
	private StringToken siteNameArg = new StringToken("S", "Site Name",
			"", TokenOptions.optSwitch | TokenOptions.optMultiple, ""); 
	
	public AlarmExport()
	{
		super("util.log");
		DecodesInterface.silent = true;
	}
	
	public static void main(String[] args)
		throws Exception
	{
		AlarmExport alarmExport = new AlarmExport();
		alarmExport.execute(args);
	}

	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(currentOnly);
		cmdLineArgs.addToken(datatypeArg);
		cmdLineArgs.addToken(inclFileProcArg);
		cmdLineArgs.addToken(grpNameArg);
		cmdLineArgs.addToken(siteNameArg);
	}

	@Override
	protected void runApp() 
		throws Exception
	{
//		String grpName = grpNameArg.getValue();
//		if (grpName == null || grpName.trim().length() == 0)
//		{
//			System.err.println("Missing required arg -- group name to export.");
//			System.exit(1);
//		}
		
		AlarmDAI alarmDAO = new AlarmDAO(TsdbAppTemplate.theDb);
		AlarmXio alarmXio = new AlarmXio();
		AlarmConfig cfg = new AlarmConfig();
		
		alarmDAO.check(cfg);
		ArrayList<AlarmScreening> screenings = alarmDAO.getAllScreenings();
		ArrayList<AlarmGroup> groups = new ArrayList<AlarmGroup>();

		// Filter groups to export and also build a list of group IDs to filter screenings.
		ArrayList<DbKey> groupIds = new ArrayList<DbKey>();
		for(AlarmGroup grp : cfg.getGroups())
		{
			if (grpNameArg.NumberOfValues() == 0
			 || (grpNameArg.NumberOfValues() == 1 && grpNameArg.getValue(0).trim().length() == 0))
			{
				groups.add(grp);
//				groupIds.add(grp.getAlarmGroupId());
				continue;
			}
			
			boolean found = false;
			for(int idx = 0; idx < grpNameArg.NumberOfValues(); idx++)
				if (TextUtil.strEqualIgnoreCase(grp.getName(), grpNameArg.getValue(idx)))
				{
					found = true;
					break;
				}
			if (found)
			{
				groups.add(grp);
				groupIds.add(grp.getAlarmGroupId());
			}
		}
			
		// Sort screenings by data type, site, reverse start date
		Collections.sort(screenings,
			new Comparator<AlarmScreening>()
			{
				@Override
				public int compare(AlarmScreening s1, AlarmScreening s2)
				{
					long x = s1.getDatatypeId().getValue() - s2.getDatatypeId().getValue();
					if (x != 0)
						return x > 0 ? 1 : -1;

					x = s1.getSiteId().getValue() - s2.getSiteId().getValue();
					if (x != 0)
						return x > 0 ? 1 : -1;

					Date d1 = s1.getStartDateTime();
					Date d2 = s2.getStartDateTime();
					
					long m1 = d1 == null ? 0L : d1.getTime();
					long m2 = d2 == null ? 0L : d2.getTime();
					
					return m1 > m2 ? -1 : m1 < m2 ? 1 : 0;
				}
			});
			
		// Get list of datatype IDs for filter
		ArrayList<DbKey> dtids = new ArrayList<DbKey>();
		for(int idx = 0; idx < datatypeArg.NumberOfValues(); idx++)
		{
			String dts = datatypeArg.getValue(idx);
			if (dts == null || dts.trim().length() == 0)
				continue;
			
			int colon = dts.indexOf(':');
			String std = colon == -1 ? DecodesSettings.instance().dataTypeStdPreference :
				dts.substring(0, colon);
			String code = colon == -1 ? dts : dts.substring(colon + 1);
			DataType dt = decodes.db.Database.getDb().dataTypeSet.get(std, code);
			if (dt == null || DbKey.isNull(dt.getId()))
			{
				System.err.println("Invalid datatype arg '" + dts + "' -- ignored.");
				continue;
			}
			dtids.add(dt.getId());
		}
			
		// Get list of Site Ids for filter
		ArrayList<DbKey> siteIds = new ArrayList<DbKey>();
		for(int idx = 0; idx < siteNameArg.NumberOfValues(); idx++)
		{
			String sns = siteNameArg.getValue(idx);
			if (sns == null || sns.trim().length() == 0)
				continue;
			
			int colon = sns.indexOf(':');
			String nameType = colon == -1 ? DecodesSettings.instance().siteNameTypePreference :
				sns.substring(0, colon);
			String nameValue = colon == -1 ? sns : sns.substring(colon + 1);
			
			Site site = decodes.db.Database.getDb().siteList.getSite(nameType, nameValue);
			if (site != null)
				siteIds.add(site.getId());
			else
				System.err.println("Invalid site name arg '" + sns + "' -- ignored.");
		}
			
		// Now go through the screenings and apply the filters.
		AlarmScreening as = null;
		AlarmScreening lastScreening = null;

		for(Iterator<AlarmScreening> scrit = screenings.iterator() ; scrit.hasNext(); 
			lastScreening = as)
		{
			as = scrit.next();
			
			// Filter by data type
			if (dtids.size() > 0)
			{
				boolean found = false;
				for (DbKey dtid : dtids)
					if (dtid.equals(as.getDatatypeId()))
					{
						found = true;
						break;
					}
				if (!found)
				{
					scrit.remove();
					continue;
				}
			}
			
			// Filter by alarm group
			if (groupIds.size() > 0)
			{
				boolean found = false;
				for(DbKey gid : groupIds)
					if (gid.equals(as.getAlarmGroupId()))
					{
						found = true;
						continue;
					}
				if (!found)
				{
					scrit.remove();
					continue;
				}
			}

			// Filter by site id
			if (siteIds.size() > 0)
			{
				boolean found = false;
				for (DbKey sid : siteIds)
					if (sid.equals(as.getSiteId()))
					{
						found = true;
						break;
					}
				if (!found)
				{
					scrit.remove();
					continue;
				}
			}

			// If current-only, only take first matching site/dt because reverse sorted above
			if (currentOnly.getValue()
			 && lastScreening != null
			 && as.getSiteId().equals(lastScreening.getSiteId())
			 && as.getDatatypeId().equals(lastScreening.getDatatypeId()))
			{
				scrit.remove();
				continue;
			}
		}
		
		if (!inclFileProcArg.getValue())
			groups.clear();
		
		alarmXio.writeXML(screenings, groups, System.out);
		
		alarmDAO.close();
	}

}
