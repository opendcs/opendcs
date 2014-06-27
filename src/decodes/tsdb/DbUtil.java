package decodes.tsdb;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.Platform;
import decodes.db.Site;
import decodes.db.SiteList;
import decodes.db.SiteName;
import decodes.sql.DbKey;
import decodes.util.DecodesSettings;
import ilex.util.CmdLine;
import ilex.util.CmdLineProcessor;

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



	public DbUtil()
	{
		super("util.log");
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
		catch (DbIoException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
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
			ArrayList<TimeSeriesIdentifier> tslist = timeSeriesDAO.listTimeSeries();
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
					System.out.println(tsid.getUniqueString());
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
		catch (NoSuchObjectException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			timeSeriesDAO.close();
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
