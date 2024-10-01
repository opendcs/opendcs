/**
 * $Id$
 * 
 * $Log$
 * 
 */
package opendcs.opentsdb;

import ilex.cmdline.IntegerToken;
import ilex.cmdline.TokenOptions;
import ilex.util.EnvExpander;

import java.util.ArrayList;

import opendcs.dbupdate.SQLReader;
import decodes.cwms.CwmsTsId;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.CmdLineArgs;
import decodes.util.PropertySpec;

public class Rebalance
	extends TsdbAppTemplate

{
	private IntegerToken addNumericTableArg = new IntegerToken("N", "(default=0) Number of Numeric Tables to Add",
		"", TokenOptions.optSwitch, 0);
	private IntegerToken addStringTableArg = new IntegerToken("S", "(default=0) Number of String Tables to Add",
		"", TokenOptions.optSwitch, 0);

	private PropertySpec[] myProps =
	{
		new PropertySpec("monitor", PropertySpec.BOOLEAN,
			"Set to true to allow monitoring from the GUI."),
		new PropertySpec("EventPort", PropertySpec.INT,
			"Open listening socket on this port to serve out app events."),
		new PropertySpec("reclaimTasklistSec", PropertySpec.INT,
			"(default=0) if set to a positive # of seconds, then when the tasklist is "
			+ "empty and this # of seconds has elapsed, shrink the allocated space for the "
			+ "tasklist back to something reasonable (Oracle only).")
	};


	public Rebalance()
	{
		super("rebalance.log");
	}

	public static void main(String[] args)
		throws Exception
	{
		// TODO Warn user that all apps that write time series data must be shut down.
		System.out.println("IMPORTANT!!!!");
		System.out.println(
			"This utility will move time series values among the various storage tables.");
		System.out.println(
			"Do not run this utility if any applications are currently writing time series data!!!");
		System.out.println(
			"Shut down any processes (CP, DECODES, GUIs, etc.) that are writing time series data.");
		System.out.println("");
		System.out.print("Press y to continue, n to abort: ");
		System.out.flush();
		String s = System.console().readLine();
		if (s.charAt(0) != 'y' && s.charAt(0) != 'Y')
		{
			System.out.println("Aborting.");
			System.exit(0);
		}
		
		Rebalance rebalance = new Rebalance();
		rebalance.execute(args);
	}

	@Override
	protected void runApp() 
		throws Exception
	{
		/*
		 * Note: Currently this works on Numeric tables only. In the future it may need
		 * to be expanded to handle the String tables also.
		 */
		
		// Load the current storage table list
		OpenTimeSeriesDAO tsDAO = (OpenTimeSeriesDAO)theDb.makeTimeSeriesDAO();
		ArrayList<StorageTableSpec> numericTables = tsDAO.getTableSpecs(OpenTsdb.TABLE_TYPE_NUMERIC);
		
		// Determine Highest table num so far. Don't assume contiguous.
		int highestTableNum = 0;
		for (StorageTableSpec sts : numericTables)
			if (sts.getTableNum() > highestTableNum)
				highestTableNum = sts.getTableNum();
		
		System.out.println("Currently there are " + numericTables.size() + " numeric tables and the "
			+ "highest table num = " + highestTableNum);
		
		// Read the queries for creating new tables from the DDL in the schema directory.
		String schemaDir = EnvExpander.expand("$DCSTOOL_HOME/schema")
			+ (theDb.isOracle() ? "/opendcs-oracle" : "/opendcs-pg");
		SQLReader sqlReader = new SQLReader(schemaDir + "/ts_num_template.sql");
		ArrayList<String> addTableQueries = sqlReader.createQueries();

		// Create the specified number of tables (could be zero).
		System.out.println("Adding " + addNumericTableArg.getValue() + " numeric tables.");
		for(int i=0; i<addNumericTableArg.getValue(); i++)
		{
			StorageTableSpec sts = new StorageTableSpec(OpenTsdb.TABLE_TYPE_NUMERIC);
			sts.setTableNum(++highestTableNum);
			numericTables.add(sts);
			
			tsDAO.doModify("insert into "
				+ "storage_table_list(table_num, storage_type, num_ts_present, est_annual_values)"
				+ " values (" + sts.getTableNum()
				+ ", '" + OpenTsdb.TABLE_TYPE_NUMERIC + "'"
				+ ", " + sts.getNumTsPresent()
				+ ", " + sts.getEstAnnualValues() + ")");
			
			for(String q : addTableQueries)
				tsDAO.doModify(q.replaceAll("0000", tsDAO.suffixFmt.format(highestTableNum)));
		}
		
		// Read all of the TSIDs into an array. The CwmsTsId has current storage table for each.
		ArrayList<TimeSeriesIdentifier> tsids = tsDAO.listTimeSeries();

		// Make a data structure to hold the TSID and the NEW table assignment if it is changed.
		class TsidTableAssignment
		{
			CwmsTsId tsid;
			int tableNum;
			TsidTableAssignment(CwmsTsId tsid, int tableNum)
			{
				this.tsid = tsid;
				this.tableNum = tableNum;
			}
		};
		ArrayList<TsidTableAssignment> tsidtab = new ArrayList<TsidTableAssignment>();
		for(TimeSeriesIdentifier tsid : tsids)
		{
			CwmsTsId ctsid = (CwmsTsId)tsid;
			tsidtab.add(new TsidTableAssignment(ctsid, ctsid.getStorageTable()));
		}
		
		// Don't trust the est annual values in the storage tables. Recalculate them.
		for(StorageTableSpec spec : numericTables)
		{
			spec.setEstAnnualValues(0);
			spec.setNumTsPresent(0);
			for(TsidTableAssignment nta : tsidtab)
				if (nta.tableNum == spec.getTableNum())
				{
					spec.setEstAnnualValues(spec.getEstAnnualValues()
						+ OpenTimeSeriesDAO.interval2estAnnualValues(nta.tsid.getIntervalOb()));
					spec.setNumTsPresent(spec.getNumTsPresent() + 1);
				}
		}
		
		System.out.println("Redistributing " + tsids.size() + " Time Series among "
			+ numericTables.size() + " numeric tables.");
		// Continue until I've iterated numTimeSeries or until smallest table >= largest table * FACTOR,
		// where FACTOR is set here:
		double FACTOR = .75;
		// Find smallest TSID in the largest table and move it to the smallest table.
		for(int idx = 0; idx < tsids.size(); idx++)
		{
			StorageTableSpec smallestTab = null;
			StorageTableSpec largestTab = null;
			for(StorageTableSpec spec : numericTables)
			{
				if (smallestTab == null || spec.getEstAnnualValues() < smallestTab.getEstAnnualValues())
					smallestTab = spec;
				if (largestTab == null || spec.getEstAnnualValues() > largestTab.getEstAnnualValues())
					largestTab = spec;
			}
			if (largestTab.getNumTsPresent() <= 1
			 || smallestTab.getNumTsPresent() >= largestTab.getNumTsPresent() - 1
			 || smallestTab.getEstAnnualValues() >= largestTab.getEstAnnualValues() * FACTOR)
				break; // All done
			
			// Move the smallest time series in largest table to smallest table
			TsidTableAssignment smallestTsid = null;
			for(TsidTableAssignment nta : tsidtab)
			{
				if (nta.tableNum != largestTab.getTableNum())
					continue;
				if (smallestTsid == null
				 || OpenTimeSeriesDAO.interval2estAnnualValues(nta.tsid.getIntervalOb())
				 	< OpenTimeSeriesDAO.interval2estAnnualValues(smallestTsid.tsid.getIntervalOb()))
					smallestTsid = nta;
			}
			if (smallestTsid != null)
			{
				smallestTsid.tableNum = smallestTab.getTableNum();
				largestTab.setNumTsPresent(largestTab.getNumTsPresent()-1);
				largestTab.setEstAnnualValues(largestTab.getEstAnnualValues() 
					- OpenTimeSeriesDAO.interval2estAnnualValues(smallestTsid.tsid.getIntervalOb()));
				smallestTab.setNumTsPresent(smallestTab.getNumTsPresent() + 1);
				smallestTab.setEstAnnualValues(smallestTab.getEstAnnualValues()
					+ OpenTimeSeriesDAO.interval2estAnnualValues(smallestTsid.tsid.getIntervalOb()));
			}
		}
				
		// TsidTableAssignments now reflect what I want things to be. Move the time series.
		int nMoved = 0;
		for(TsidTableAssignment nta : tsidtab)
		{
			if (nta.tsid.getStorageTable() == nta.tableNum)
				continue; // this TSID is staying put.
			
			System.out.println("Moving '" + nta.tsid.getUniqueString() + "' from table "
				+ nta.tsid.getStorageTable() + " to table " + nta.tableNum);
			
			String q = "insert into ts_num_" + tsDAO.suffixFmt.format(nta.tableNum)
				+ " select * from ts_num_" + tsDAO.suffixFmt.format(nta.tsid.getStorageTable())
				+ " where ts_id = " + nta.tsid.getKey();
			tsDAO.doModify(q);
			q = "delete from ts_num_" + tsDAO.suffixFmt.format(nta.tsid.getStorageTable())
				+ " where ts_id = " + nta.tsid.getKey();
			tsDAO.doModify(q);
			q = "update ts_spec set storage_table = " + nta.tableNum
				+ " where ts_id = " + nta.tsid.getKey();
			nMoved++;
		}
		
		System.out.println("" + nMoved + " time series moved.");
		
		System.out.println("Updating storage table stats.");
		// Now also write out the updated storage table specs.
		for(StorageTableSpec spec : numericTables)
		{
			String q = "update storage_table_list "
				+ "set num_ts_present = " + spec.getNumTsPresent()
				+ ", est_annual_values = " + spec.getEstAnnualValues()
				+ " where table_num = " + spec.getTableNum();
			tsDAO.doModify(q);
		}
		tsDAO.close();
	}
	
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		appNameArg.setDefaultValue("utility");
		cmdLineArgs.addToken(addNumericTableArg);
		cmdLineArgs.addToken(addStringTableArg);
	}
	
	@Override
	protected void oneTimeInit()
	{
	}

	@Override
	public PropertySpec[] getSupportedProps()
	{
		return myProps;
	}


}
