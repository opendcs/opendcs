/*
 * This software was written by Cove Software, LLC. under contract to the 
 * U.S. Government. This software is property of the U.S. Government and 
 * may be used by permission only.
 * 
 * No warranty is provided or implied other than specific contractual terms.
 * 
 * Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
 */
package decodes.tsdb;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;

import opendcs.dai.DacqEventDAI;
import opendcs.dai.DeviceStatusDAI;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.opentsdb.OpenTimeSeriesDAO;
import opendcs.opentsdb.OpenTsdb;
import opendcs.opentsdb.StorageTableSpec;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.CwmsTsId;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.Platform;
import decodes.db.Site;
import decodes.db.SiteList;
import decodes.db.SiteName;
import decodes.polling.DacqEvent;
import decodes.polling.DeviceStatus;
import decodes.sql.DbKey;
import decodes.sql.SqlDatabaseIO;
import decodes.util.DecodesSettings;
import ilex.util.CmdLine;
import ilex.util.CmdLineProcessor;
import ilex.util.Logger;
import ilex.util.TextUtil;

/**
 * General purpose database command-line utility
 * @author mmaloney
 *
 */
public class DbUtil extends TsdbAppTemplate
{
	private CmdLineProcessor cmdLineProc = new CmdLineProcessor();
	private CmdLine listSiteCmd = 
		new CmdLine("list-site", "[startsWith] List sites, optionally starting with a specified string, sorted name.")
		{
			public void execute(String[] tokens)
			{
				doListSite(tokens);
			}
		};
	private CmdLine deletePlatCmd =
		new CmdLine("delete-platform", 
			"[id|site] [platformId or SiteName] - delete platform by ID or site name")
		{
			public void execute(String[] tokens)
			{
				doDeletePlatform(tokens);
			}
		};
	private CmdLine deleteSiteCmd =
		new CmdLine("delete-site", 
			"[default-site-name] - delete site by its default site name")
		{
			public void execute(String[] tokens)
			{
				doDeleteSite(tokens);
			}
		};
	private CmdLine locAliasCmd =
		new CmdLine("loc-aliases", "List all location aliases")
		{
			public void execute(String[] tokens)
			{
				doLocAliases(tokens);
			}
		};
	private CmdLine tsAliasCmd =
		new CmdLine("ts-aliases", "List all time-series aliases")
		{
			public void execute(String[] tokens)
			{
				doTsAliases(tokens);
			}
		};
	private CmdLine tsListCmd =	
		new CmdLine("list-ts", "[contains] List Time Series, optionally with id containing specified string, sorted name.")
		{
			public void execute(String[] tokens)
			{
				doListTS(tokens);
			}
		};
	private CmdLine tsDeleteCmd =	
		new CmdLine("delete-ts", "[contains] List Time Series, optionally with id containing specified string, sorted name.")
		{
			public void execute(String[] tokens)
			{
				doDeleteTS(tokens);
			}
		};
	private CmdLine devListCmd =	
		new CmdLine("list-dev", "List Device Statuses")
		{
			public void execute(String[] tokens)
			{
				doListDev(tokens);
			}
		};
	private CmdLine devUpdateCmd =	
		new CmdLine("update-dev", "[devname] [procname] [mediumId] [status] List Device Statuses")
		{
			public void execute(String[] tokens)
			{
				doUpdateDev(tokens);
			}
		};
	private CmdLine devEventsContaining =	
		new CmdLine("events-containing", "[string] List events containing a specified string")
		{
			public void execute(String[] tokens)
			{
				devEventsContaining(tokens);
			}
		};
	private CmdLine genEventsCmd =	
		new CmdLine("event", "[priority (I,W,F)] [subsystem] [event text...]")
		{
			public void execute(String[] tokens)
			{
				genEventsCmd(tokens);
			}
		};
	private CmdLine genSchedEventsCmd =	
		new CmdLine("sched-event", "[priority (I,W,F)] schedStatusId platformId(or -1) subsystem [event text...]")
		{
			public void execute(String[] tokens)
			{
				genSchedEventsCmd(tokens);
			}
		};
	private CmdLine versionCmd =	
		new CmdLine("version", " -- show DECODES and tsdb database versions")
		{
			public void execute(String[] tokens)
			{
				versionCmd(tokens);
			}
		};
	private CmdLine bparamCmd = 
		new CmdLine("bparam", " -- show CWMS Base Param - Unit Associations")
		{
			public void execute(String[] tokens)
			{
				bparamCmd(tokens);
			}
		};
	private CmdLine selectCmd = 
		new CmdLine("select", " -- An arbitrary database SELECT statement.")
		{
			public void execute(String[] tokens)
			{
				selectCmd(tokens);
			}
		};
	private CmdLine alterCmd = 
		new CmdLine("alter", " -- An arbitrary database ALTER statement.")
		{
			public void execute(String[] tokens)
			{
				updateCmd(tokens);
			}
		};
	private CmdLine updateCmd = 
		new CmdLine("update", " -- An arbitrary database UPDATE statement.")
		{
			public void execute(String[] tokens)
			{
				updateCmd(tokens);
			}
		};

	private CmdLine hdbRatingCmd = 
		new CmdLine("hdbRating", " -- Install a test rating in HDB.")
		{
			public void execute(String[] tokens)
			{
				hdbRatingCmd(tokens);
			}
		};

	private CmdLine tsdbStatsCmd = 
		new CmdLine("tsdbStats", " -- Display statistics on OpenTSDB Storage Tables.")
		{
			public void execute(String[] tokens)
			{
				tsdbStats(tokens);
			}
		};
	private CmdLine parmMorphCmd = 
		new CmdLine("parmMorph", " <Location> <mask> -- apply mask to location and show result.")
		{
			public void execute(String[] tokens)
			{
				parmMorph(tokens);
			}
		};



	public DbUtil()
	{
		super("util.log");
	}

	protected void parmMorph(String[] tokens)
	{
		if (tokens.length < 3)
		{
			System.out.println("2 params required.");
			return;
		}
		String loc = tokens[1];
		String mask = tokens[2];
		String result = CwmsTimeSeriesDb.morph(loc, mask);
		System.out.println("loc='" + loc + "', mask='" + mask + "', result='" + result + "'");
	}

	
	
	protected void tsdbStats(String[] tokens)
	{
		if (!theDb.isOpenTSDB())
		{
			System.out.println("This command is only available on OpenTSDB.");
			return;
		}
		
		OpenTimeSeriesDAO tsdao = (OpenTimeSeriesDAO)theDb.makeTimeSeriesDAO();
		try
		{
			ArrayList<StorageTableSpec> specs = tsdao.getTableSpecs(OpenTsdb.TABLE_TYPE_NUMERIC);
			ArrayList<TimeSeriesIdentifier> tsids = tsdao.listTimeSeries();
			
			System.out.println("" + specs.size() + " storage tables found:");
			for(StorageTableSpec spec : specs)
			{
				String tableName = "TS_NUM_" + tsdao.suffixFmt.format(spec.getTableNum());
				String q = "select count(*) from " + tableName;
				ResultSet rs = theDb.doQuery(q);
				int totalValues = rs.next() ? rs.getInt(1) : 0;

				System.out.println("" + spec.getTableNum() + ": numTimeSeries=" 
					+ spec.getNumTsPresent() + ", estAnnualValues=" + spec.getEstAnnualValues()
					+ ", currentTotalValues=" + totalValues);
				for(TimeSeriesIdentifier tsid : tsids)
				{
					CwmsTsId ctsid = (CwmsTsId)tsid;
					if (ctsid.getStorageTable() == spec.getTableNum())
					{
						q = "select count(*) from " + tableName + " where TS_ID = " + ctsid.getKey();
						rs = theDb.doQuery(q);
						System.out.println("    key=" + tsid.getKey() + ", "
							+ tsid.getUniqueString() + ", numValues="
							+ (rs.next() ? rs.getInt(1) : 0)
							+ ", TSID last modified on " + ctsid.getLastModified());
					}
				}
			}
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			tsdao.close();
		}
		
		
	}

	protected void hdbRatingCmd(String[] tokens)
	{
		String q = "select a.site_datatype_id from hdb_site_datatype a, hdb_site b "
			+ "where a.site_id = b.site_id and b.site_common_name = 'TESTSITE1' "
			+ "and a.datatype_id = 65";
		Statement st = null;
		try
		{
			st = theDb.getConnection().createStatement();
			ResultSet rs = st.executeQuery(q);
			if (!rs.next())
			{
				System.out.println("Statement '" + q + "' did not return any results.");
				return;
			}
			DbKey sdi = DbKey.createDbKey(rs, 1);
			rs.close();
			q = "{ call RATINGS.create_site_rating(" + sdi + ", 'Stage Flow', null, null, 7, 'test rating') }";
			st.executeUpdate(q);
			q = "select rating_id from ref_site_rating where indep_site_datatype_id = " + sdi;
			rs = st.executeQuery(q);
			if (!rs.next())
			{
				System.out.println("Statement '" + q + "' did not return any results.");
				return;
			}
			DbKey ratingId = DbKey.createDbKey(rs, 1);
			rs.close();
			
			double indep[] = { .0001, 1.0, 2.0, 3.0, 4.0, 5.0 };
			double dep[]   = { 1., 10., 100., 1000., 10000., 100000. };
			for(int i = 0; i<indep.length; i++)
			{
				q = "{ call RATINGS.modify_rating_point(" + ratingId + ", " + indep[i] + ", " + dep[i] + ") }";
				st.executeUpdate(q);
			}
		}
		catch(Exception ex)
		{
			System.err.println("Execption in '" + q + "': " + ex);
			ex.printStackTrace();
		}
		finally
		{
			try
			{
				st.close();
			}
			catch(Exception ex) {}
		}
	}

	protected void selectCmd(String[] tokens)
	{
		StringBuilder sb = new StringBuilder();
		for(String t : tokens)
			sb.append(t + " ");
		String q = "";
		try
		{
			Statement st = theDb.getConnection().createStatement();
			q = sb.toString();
			System.out.println("Executing: " + q);
			ResultSet rs = st.executeQuery(q);
			ArrayList<String[]> rows = new ArrayList<String[]>();
			ResultSetMetaData rsmd = rs.getMetaData();
			int numCols = rsmd.getColumnCount();
			String[] header = new String[numCols];
			int[] colWidth = new int[numCols];
			for(int i=0; i<numCols; i++)
			{	
				header[i] = rsmd.getColumnName(i+1);
				colWidth[i] = header[i].length();
			}
			while(rs.next())
			{
				String[] row = new String[numCols];
				for(int c = 0; c<numCols; c++)
				{
					row[c] = rs.getString(c+1);
					if (row[c] == null)
						row[c] = "null";
					if (row[c].length() > colWidth[c])
						colWidth[c] = row[c].length();
				}
				rows.add(row);
			}
			System.out.println("Result has " + rows.size() + " rows.");
			System.out.print("| ");
			for(int c = 0; c<numCols; c++)
				System.out.print(" " + 
					TextUtil.setLengthLeftJustify(header[c], colWidth[c]) + " |");
			System.out.println("");
			for(String[] row : rows)
			{
				System.out.print("| ");
				for(int c = 0; c<numCols; c++)
					System.out.print(" " +
						TextUtil.setLengthLeftJustify(row[c], colWidth[c]) + " |");
				System.out.println("");
			}
		}
		catch (SQLException ex)
		{
			System.err.println("Error in '" + q + "': " + ex);
			ex.printStackTrace();
		}
	}
	
	protected void updateCmd(String[] tokens)
	{
		StringBuilder sb = new StringBuilder();
		for(String t : tokens)
			sb.append(t + " ");
		String q = "";
		try
		{
			Statement st = theDb.getConnection().createStatement();
			q = sb.toString();
			System.out.println("Executing: " + q);
			int rows = st.executeUpdate(q);
			System.out.println("" + rows + " rows update.");
		}
		catch (SQLException ex)
		{
			System.err.println("Error in '" + q + "': " + ex);
			ex.printStackTrace();
		}
	}

	protected void bparamCmd(String[] tokens)
	{
		if (!(theDb instanceof CwmsTimeSeriesDb))
		{
			System.out.println("This command is only available for CWMS databases.");
			return;
		}
		CwmsTimeSeriesDb cwmsdb = (CwmsTimeSeriesDb)theDb;
		cwmsdb.getBaseParam().print();
	}

	protected void versionCmd(String[] tokens)
	{
		System.out.println("DECODES Database Version: " + theDb.getDecodesDatabaseVersion());
		System.out.println("TSDB Version: " + theDb.getTsdbVersion());
	}

	protected void doTsAliases(String[] tokens)
	{
		String q = "select * from cwms_v_ts_id where aliased_item is not null";
		try
		{
			ResultSet rs = theDb.doQuery(q);
			printRS(rs, q);
		}
		catch (Exception ex)
		{
			System.out.println("Query '" + q + "' threw exception: " + ex);
		}
		q = "select * from cwms_v_ts_id2 where aliased_item is not null";
		try
		{
			ResultSet rs = theDb.doQuery(q);
			printRS(rs, q);
		}
		catch (Exception ex)
		{
			System.out.println("Query '" + q + "' threw exception: " + ex);
		}
	}

	protected void doLocAliases(String[] tokens)
	{
		String q = "select * from cwms_v_loc where aliased_item is not null";
		try
		{
			ResultSet rs = theDb.doQuery(q);
			printRS(rs, q);
		}
		catch (Exception ex)
		{
			System.out.println("Query '" + q + "' threw exception: " + ex);
		}
		q = "select * from cwms_v_loc2 where aliased_item is not null";
		try
		{
			ResultSet rs = theDb.doQuery(q);
			printRS(rs, q);
		}
		catch (Exception ex)
		{
			System.out.println("Query '" + q + "' threw exception: " + ex);
		}
	}

	private void printRS(ResultSet rs, String q)
		throws Exception
	{
		System.out.println("Results of '" + q + "'");
		ResultSetMetaData rsmd = rs.getMetaData();
		for(int idx = 1; idx <= rsmd.getColumnCount(); idx++)
			System.out.printf("%s, ", rsmd.getColumnName(idx));
		System.out.println();
		while(rs.next())
			for (int idx = 1; idx <= rsmd.getColumnCount(); idx++)
				System.out.printf("%s,", rs.getString(idx));
		   
	}

	@Override
	protected void runApp() throws Exception
	{
		cmdLineProc.addCmd(listSiteCmd);
		cmdLineProc.addCmd(deleteSiteCmd);
		cmdLineProc.addCmd(deletePlatCmd);
		cmdLineProc.addCmd(tsListCmd);
		cmdLineProc.addCmd(locAliasCmd);
		cmdLineProc.addCmd(tsAliasCmd);
		cmdLineProc.addCmd(tsDeleteCmd);
		cmdLineProc.addCmd(devListCmd);
		cmdLineProc.addCmd(devUpdateCmd);
		cmdLineProc.addCmd(devEventsContaining);
		cmdLineProc.addCmd(genEventsCmd);
		cmdLineProc.addCmd(genSchedEventsCmd);
		cmdLineProc.addCmd(versionCmd);
		cmdLineProc.addCmd(bparamCmd);
		cmdLineProc.addCmd(selectCmd);
		cmdLineProc.addCmd(alterCmd);
		cmdLineProc.addCmd(updateCmd);
		cmdLineProc.addCmd(hdbRatingCmd);
		cmdLineProc.addCmd(tsdbStatsCmd);
		cmdLineProc.addCmd(parmMorphCmd);
		
		cmdLineProc.addHelpAndQuitCommands();
		
		cmdLineProc.prompt = "cmd: ";
		cmdLineProc.processInput();
	}

	protected void doDeletePlatform(String[] tokens)
	{
		if (!deletePlatCmd.requireTokens(3, tokens))
			return;
		if (tokens[1].equalsIgnoreCase("id"))
		{
			try
			{
				long id = Long.parseLong(tokens[2]);
				Platform p = Database.getDb().platformList.getById(DbKey.createDbKey(id));
				if (p == null)
				{
					System.out.println("There is no platform with id=" + id);
					return;
				}
				Database.getDb().getDbIo().deletePlatform(p);
				Database.getDb().platformList.removePlatform(p);
				System.out.println("Platform with ID " + id + " deleted.");
			}
			catch(NumberFormatException ex)
			{
				System.out.println("Platform ID must be the numeric surrogate key!");
				deletePlatCmd.usage();
			}
			catch (DatabaseException ex)
			{
				System.out.println(ex);
				ex.printStackTrace();
			}
		}
		else if (tokens[1].equalsIgnoreCase("site"))
		{
			System.out.println("Not implemented");
			// get site name
			// retrieve site
			// get all platforms at that site
		}
		else
		{
			deletePlatCmd.usage();
			return;
		}
		
	}

	protected void doListSite(String[] tokens)
	{
		SiteDAI siteDAO = theDb.makeSiteDAO();
		SiteList siteList = new SiteList();
		try
		{
			siteDAO.read(siteList);
			ArrayList<Site> sa = new ArrayList<Site>();
			for(Iterator<Site> sit = siteList.iterator(); sit.hasNext(); )
				sa.add(sit.next());
			Collections.sort(sa, 
				new Comparator<Site>()
				{
					@Override
					public int compare(Site o1, Site o2)
					{
						SiteName sn1 = o1.getPreferredName();
						SiteName sn2 = o2.getPreferredName();
						return sn1.getNameValue().compareTo(sn2.getNameValue());
					}
				});
			for(Site s : sa)
				if (tokens.length == 1 
				 || s.getPreferredName().getNameValue().toUpperCase().startsWith(tokens[1].toUpperCase()))
					System.out.println(s.getKey() + ": " 
						+ s.getPreferredName().getNameValue() + " " + s.getDescription());
			
		}
		catch (DbIoException ex)
		{
			System.err.println(ex);
			ex.printStackTrace();
		}
		finally
		{
			siteDAO.close();
		}
	}
	
	protected void doDeleteSite(String[] tokens)
	{
		if (!deleteSiteCmd.requireTokens(2, tokens))
			return;
		SiteName sn = new SiteName(null,
			DecodesSettings.instance().siteNameTypePreference,
			tokens[1]);
		 
		SiteDAI siteDAO = theDb.makeSiteDAO();
		
		try
		{
			DbKey key = siteDAO.lookupSiteID(sn);
			if (key.isNull())
				System.out.println("No such site with name '" + sn + "'");
			else
			{
				System.out.print("Site ID=" + key + ", confirm delete (y/n):");
				String s = System.console().readLine();
				if (s.toLowerCase().startsWith("y"))
				{
					siteDAO.deleteSite(key);
					System.out.println("Deleted");
				}
			}
		}
		catch (DbIoException ex)
		{
			System.out.println("Error deleting site: " + ex);
			ex.printStackTrace();
		}
		finally
		{
			siteDAO.close();
		}
	}

	protected void doListTS(String[] tokens)
	{
		TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
		try
		{
			ArrayList<TimeSeriesIdentifier> tslist = timeSeriesDAO.listTimeSeries(true);
			Collections.sort(tslist, 
				new Comparator<TimeSeriesIdentifier>()
				{
					@Override
					public int compare(TimeSeriesIdentifier o1, TimeSeriesIdentifier o2)
					{
						return o1.getUniqueString().compareTo(o2.getUniqueString());
					}
				});
			for(TimeSeriesIdentifier tsid : tslist)
				if (tokens.length == 1 
				 || tsid.getUniqueString().toUpperCase().contains(tokens[1].toUpperCase()))
				{
					String m = tsid.getUniqueString() + ", units=" + tsid.getStorageUnits();
					if (theDb.isOpenTSDB())
					{
						CwmsTsId ctsid = (CwmsTsId)tsid;
						m = m + ", table=" + ctsid.getStorageTable() + ", active=" + ctsid.isActive()
							+ ", desc='" + ctsid.getDescription() + "'";
					}
					System.out.println(m);
				}
		}
		catch (DbIoException ex)
		{
			System.err.println(ex);
			ex.printStackTrace();
		}
		finally
		{
			timeSeriesDAO.close();
		}
	}

	protected void doDeleteTS(String[] tokens)
	{
		if (!tsDeleteCmd.requireTokens(2, tokens))
			return;
		TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
		try
		{
			TimeSeriesIdentifier tsid = timeSeriesDAO.getTimeSeriesIdentifier(tokens[1]);
			System.out.print("TS unique name='" + tsid.getUniqueString() 
				+ "', key=" + tsid.getKey() + ", confirm delete (y/n):");
			String s = System.console().readLine();
			if (s.toLowerCase().startsWith("y"))
			{
				timeSeriesDAO.deleteTimeSeries(tsid);
				System.out.println("Deleted");
			}
		}
		catch (DbIoException ex)
		{
			System.err.println(ex);
			ex.printStackTrace();
		}
		catch (NoSuchObjectException ex)
		{
			System.err.println(ex);
			ex.printStackTrace();
		}
		finally
		{
			timeSeriesDAO.close();
		}

	}

	protected void doListDev(String[] tokens)
	{
		DeviceStatusDAI devStatusDAO = theDb.makeDeviceStatusDAO();
		try
		{
			System.out.println("Device Statuses: ");
			for(DeviceStatus devstat : devStatusDAO.listDeviceStatuses())
				System.out.println(devstat.toString());
		}
		catch (DbIoException ex)
		{
			System.err.println("Error listing device statuses: " + ex);
			ex.printStackTrace(System.err);
		}
		finally
		{
			devStatusDAO.close();
		}
	}
	
	protected void doUpdateDev(String[] tokens)
	{
		if (!devUpdateCmd.requireTokens(5, tokens))
			return;
		DeviceStatusDAI devStatusDAO = theDb.makeDeviceStatusDAO();
		try
		{
			DeviceStatus devStat = devStatusDAO.getDeviceStatus(tokens[1]);
			if (devStat == null)
				devStat = new DeviceStatus(tokens[1]);
			devStat.setInUse(!devStat.isInUse());
			devStat.setLastUsedByProc(tokens[2]);
			devStat.setLastUsedByHost("localhost");
			devStat.setLastActivityTime(new Date());
			devStat.setLastReceiveTime(new Date());
			devStat.setLastMediumId(tokens[3]);
			devStat.setLastErrorTime(new Date());
			devStat.setPortStatus(tokens[4]);
			devStatusDAO.writeDeviceStatus(devStat);
		}
		catch (DbIoException ex)
		{
			System.err.println("Error writing device status: " + ex);
			ex.printStackTrace(System.err);
		}
		finally
		{
			devStatusDAO.close();
		}
		
	}

	ArrayList<DacqEvent> devEvents = new ArrayList<DacqEvent>();
	
	protected void devEventsContaining(String[] tokens)
	{
		if (!devUpdateCmd.requireTokens(2, tokens))
			return;
		DacqEventDAI dacqEventDAO = theDb.makeDacqEventDAO();
		try
		{
			int oldSize = devEvents.size();
			dacqEventDAO.readEventsContaining(tokens[1], devEvents);
			while(oldSize < devEvents.size())
				System.out.println(devEvents.get(oldSize++).toString());
		}
		catch (DbIoException ex)
		{
			System.err.println("Error reading device events: " + ex);
			ex.printStackTrace(System.err);
		}
		finally
		{
			dacqEventDAO.close();
		}
	}

	protected void genEventsCmd(String[] tokens)
	{
		if (!genEventsCmd.requireTokens(3, tokens))
			return;
System.out.println("t[0]='" + tokens[0] + "' t[1]='" + tokens[1] + "' t[3]='" + tokens[3] + "'");
		char c = tokens[1].toLowerCase().charAt(0);
		int priority = 
			c == '3' ? Logger.E_DEBUG3 :
			c == '2' ? Logger.E_DEBUG2 :
			c == '1' ? Logger.E_DEBUG1 :
			c == 'i' ? Logger.E_INFORMATION :
			c == 'w' ? Logger.E_WARNING : Logger.E_FAILURE;
System.out.println("priority = " + priority);
		String subsystem = tokens[2];
		String evtText = cmdLineProc.inputLine.trim();
		int space = evtText.indexOf(' ');
		if (space != -1)
		{
			evtText = evtText.substring(space).trim();
			space = evtText.indexOf(' ');
			if (space != -1)
			{
				evtText = evtText.substring(space).trim();
				space = evtText.indexOf(' ');
				if (space != -1)
					evtText = evtText.substring(space).trim();
			}
		}
			
		
		DacqEventDAI dacqEventDAO = theDb.makeDacqEventDAO();
		try
		{
			DacqEvent evt = new DacqEvent();
			evt.setEventPriority(priority);
			evt.setSubsystem(subsystem);
			evt.setEventText(evtText);
			dacqEventDAO.writeEvent(evt);
		}
		catch (DbIoException ex)
		{
			System.err.println("Error writing event: " + ex);
			ex.printStackTrace(System.err);
		}
		finally
		{
			dacqEventDAO.close();
		}
	}

	protected void genSchedEventsCmd(String[] tokens)
	{
//		new CmdLine("sched-event", "[priority (I,W,F)] schedStatusId platformId(or -1) subsystem [event text...]")
		if (!genEventsCmd.requireTokens(6, tokens))
			return;
		char c = tokens[1].toLowerCase().charAt(0);
		int priority = 
			c == '3' ? Logger.E_DEBUG3 :
			c == '2' ? Logger.E_DEBUG2 :
			c == '1' ? Logger.E_DEBUG1 :
			c == 'i' ? Logger.E_INFORMATION :
			c == 'w' ? Logger.E_WARNING : Logger.E_FAILURE;
		
		DbKey schedStatusId = DbKey.NullKey;
		try
		{
			long id = Long.parseLong(tokens[2]);
			if (id != -1)
				schedStatusId = DbKey.createDbKey(id);
		}
		catch(NumberFormatException ex)
		{
			System.err.println("Invalid schedStatusId '" + tokens[2] + "' -- must be number");
			return;
		}

		DbKey platformId = DbKey.NullKey;
		try
		{
			long id = Long.parseLong(tokens[3]);
			if (id != -1)
				platformId = DbKey.createDbKey(id);
		}
		catch(NumberFormatException ex)
		{
			System.err.println("Invalid platformId '" + tokens[3] + "' -- must be number");
			return;
		}
		
		String subsystem = tokens[4];
		
		
		String evtText = "";
		for(int i = 5; i<tokens.length; i++)
			evtText = evtText + tokens[i] + " ";
		
		DacqEventDAI dacqEventDAO = theDb.makeDacqEventDAO();
		try
		{
			DacqEvent evt = new DacqEvent();
			evt.setEventPriority(priority);
			evt.setScheduleEntryStatusId(schedStatusId);
			evt.setPlatformId(platformId);
			evt.setSubsystem(subsystem);
			evt.setEventText(evtText);
			dacqEventDAO.writeEvent(evt);
		}
		catch (DbIoException ex)
		{
			System.err.println("Error writing event: " + ex);
			ex.printStackTrace(System.err);
		}
		finally
		{
			dacqEventDAO.close();
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args)
		throws Exception
	{
		DbUtil dbUtil = new DbUtil();
		dbUtil.execute(args);
	}

}
