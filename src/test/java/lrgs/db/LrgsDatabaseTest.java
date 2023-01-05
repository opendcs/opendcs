package lrgs.db;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import lrgs.lrgsmain.LrgsConfig;

/**
 * Test class for the LrgsDatabase class. This program 
 * will test the LrgsDatabase class.
 * To run this program make sure that you create the lrgs 
 * database first.
 * 
 */
public class LrgsDatabaseTest
{
	private static String dateFormatFromConfig;
	private static String timeZoneFromConfig;
	private static Date startTime1;
	private static Date endTime1;
	
	private static Date startTime2;
	private static Date endTime2;
	
	private static Date startTime3;
	private static Date endTime3;
	
	private static Date startTime4;
	private static Date endTime4;
	
	/**
	 * This is the main program for the LrgsDatabase test class. It 
	 * takes two parameters. The first one is a file path which is the 
	 * physical location of the lrgs config file in your filesystem.
	 * The second parameter is the action to be performed. Values are:
	 * CLEANDB -  indicates that it is a clean Database with the 
	 * exception of the lrgs_database table. It will tests all 
	 * LrgsDatabase SQL statements.  -OR-
	 * READDB - will read all data from all lrgs database tables
	 * and display the output to the console.
	 * 
	 * @param args the lrgs config file path and the action to be performed
	 */
	public static void main(String[] args)
	{

		// Verify that we have the right parameters.
		//      java LrgsDatabaseTest FILEPATH ACTION
		//java -cp /home/jperez/work/decodes/dist/decodes.jar:
		//         /home/jperez/work/decodes/3rd-party/postgresql.jar 
		//      lrgs.db.LrgsDatabaseTest /home/jperez/work/lrgs/lrgs.conf READDB
		// FILEPATH: location of lrgs.conf file in the filesystem
		// ACTION: CLEANDB -or- READDB 
		//		   CLEANDB - indicates that it is a clean Database with the 
		//         exception of the lrgs_database table. It will tests all 
		//         LrgsDatabase SQL statements.
		// 		   READDB - will read all data from all lrgs database tables
		//         and display the output to the console.			
		if (args.length < 2)
		{
			// Error.
			StringBuffer errorMsg = new StringBuffer();
			errorMsg.append("LrgsDabaseTest Usage: java LrgsDatabaseTest FILEPATH ACTION\n");
			errorMsg.append("\t\twhere FILEPATH is the location of lrgs.conf file in the filesystem\n");
			errorMsg.append("\t\tand ACTION is CLEANDB -or- READDB\n");
			errorMsg.append("\t\tCLEANDB - indicates that it is a clean Database with the\n");
			errorMsg.append("\t\texception of the lrgs_database table. It will tests all\n");
			errorMsg.append("\t\tLrgsDatabase SQL statements.\n");
			errorMsg.append("\t\tREADDB - will read all data from all lrgs database tables\n");
			errorMsg.append("\t\tand display the output to the console.\n");
			System.out.println(errorMsg.toString());
			System.exit(1);
		}
		else
		{
			final String FILEPATH = args[0];
			final String ACTION = args[1];
			if (FILEPATH == null)
			{
				System.out.println("File Path cannot be null.");
				System.exit(1);
			}
			if (ACTION == null)
			{
				System.out.println("Action cannot be null.");
				System.exit(1);
			}
			System.out.println("Given File Path = " + FILEPATH);
			System.out.println("Given Action = " + ACTION);

			try 
			{
				// Test the connect to Database method.
				LrgsDatabase lrgsDB = null;
				lrgsDB = testConnect(lrgsDB, FILEPATH);
				// Set time formats and different datetimes to be used for the
				// rest of the database table test.
				SimpleDateFormat dateFmt;
				dateFmt = new SimpleDateFormat(dateFormatFromConfig);
				TimeZone tz = TimeZone.getTimeZone(timeZoneFromConfig);
				dateFmt.setTimeZone(tz);
				fillOutDateTimes(dateFmt);
				if (ACTION.equalsIgnoreCase("CLEANDB"))
				{
					testAllSQLs(lrgsDB, dateFmt);
				}
				else if (ACTION.equalsIgnoreCase("READDB"))
				{
					readAllData(lrgsDB, dateFmt);
				}
				// Another Action that INSERT data
			}
			catch (LrgsDatabaseException e)
			{   // Catch exception and print it to the console.
				e.printStackTrace();
			}
		}
	}

	private static void testAllSQLs(LrgsDatabase lrgsDB, SimpleDateFormat dateFmt) throws LrgsDatabaseException
	{
		// NOTICE: these tests need to be run in the following order ONLY.
		// Clean the Database first: Run the sql script deleteLrgsData.sh 
		// found on /work/lrgs/lrgs-distro/bin
				
		// Test reading from the lrgs_database table.
		testGetLrgsDatabase(lrgsDB);
				
		// Test saving a Data Source
		testSaveDataSource(lrgsDB);

		// Test Data Source read method. Will test the insert and update.
		testGetDataSource(lrgsDB);
		// Read a Data Soure that does not exists in the DB.
		testGetDataSource2(lrgsDB);
				
		// Test reading a collection of Data Sources.
		testGetDataSources(lrgsDB);
				
		// Test delete Data Source method (delete one record)
		testDeleteDataSource(lrgsDB);
				
		// Test deleting the rest of Data Sources (delete all records left)
		testDeleteDataSources(lrgsDB);
				
		// Test reading a collection of Data Sources after deleting the table.
		testGetDataSources(lrgsDB);
				
		// Test saving DdsConnectionStats object, also test the update part.
		testSaveDdsConnectionStats(lrgsDB, dateFmt);
				
		// Test reading a collection of DdsConnectionStats objects
		testGetDdsConnectionStats(lrgsDB, dateFmt);
		testGetDdsConnectionStats2(lrgsDB, dateFmt);
				
		// Test deleting DdsConnectionStats
		testDeleteDdsStats(lrgsDB, dateFmt);
				
		// Test saving DdsPeriodStats and update
		testSaveDdsPeriodStats(lrgsDB, dateFmt);
				
		// Test reading DdsPeriodStats
		testGetDdsPeriodStats(lrgsDB, dateFmt);
			
		// Test deleting DdsConnectionStats and DdsPeriodStats
		testDeleteDdsStats(lrgsDB, dateFmt);
				
		// Test saving the 3 different outages and update
		// System Outage, Domsat Gap, Damsnt Outage
		testSaveOutages(lrgsDB, dateFmt);
				
		// Test reading the 3 different outages
		testGetOutages(lrgsDB, dateFmt);
				
		// Test deleting Outages
		testDeleteOutages(lrgsDB, dateFmt);
		// Verify that all Outages were deleted
		testGetOutages(lrgsDB, dateFmt);	
	}
	
	private static void readAllData(LrgsDatabase lrgsDB, SimpleDateFormat dateFmt) throws LrgsDatabaseException
	{
		// Read all tables
		testGetLrgsDatabase(lrgsDB);
		testGetDataSources(lrgsDB);
		testGetAllDdsConnectionStats(lrgsDB, dateFmt);
		testGetAllDdsPeriodStats(lrgsDB, dateFmt);
		testGetAllOutages(lrgsDB, dateFmt);		
	}
	
	/*
	 * Test method for 'lrgs.db.LrgsDatabase.connect(Properties)'
	 */
	private static LrgsDatabase testConnect(LrgsDatabase lrgsDB, String FILEPATH) throws LrgsDatabaseException
	{
		System.out.println("============================================================");
		System.out.println("Testing the LrgsDatabase Connect method.");
		Properties credentials = new Properties();
		credentials.setProperty("username","lrgs_adm");
		credentials.setProperty("password", "");
		
		// Load the Configuration file.
		LrgsConfig cfg = LrgsConfig.instance();
		String filePath = FILEPATH; //"c:/Projects/work/lrgs/lrgs.conf"; // varies by file conf location
		cfg.setConfigFileName(filePath);

		try
		{
			cfg.loadConfig();
		} catch (IOException e)
		{		
			e.printStackTrace();
		}
		// Used during the test
		dateFormatFromConfig = cfg.sqlReadDateFormat;
		timeZoneFromConfig = cfg.sqlTimeZone;
		// Create LrgsDatabase object and call connect method
		lrgsDB = new LrgsDatabase();
		lrgsDB.connect(credentials);
		System.out.println("============================================================");
		return lrgsDB;
	}
	
	private static void testGetLrgsDatabase(LrgsDatabase lrgsDB)
	{
		System.out.println("============================================================");
		System.out.println("This is the Information from the lrgs_database table.");
		System.out.println("LRGS Database Version = " + lrgsDB.getDatabaseVersion());
		System.out.println("LRGS Database Create Time = " + lrgsDB.getDatabaseCreateTime());
		System.out.println("LRGS Database Created By = " + lrgsDB.getDatabaseCreateBy());
		System.out.println("LRGS Database Description = " + lrgsDB.getDatabaseDescription());
		System.out.println("============================================================");
	}

	/*
	 * Test method for 'lrgs.db.LrgsDatabase.getDataSource(String, String)'
	 */
	private static void testGetDataSource(LrgsDatabase lrgsDB) throws LrgsDatabaseException
	{
		System.out.println("============================================================");
		System.out.println("Test reading one Data Source Object (name1/type1)");
		DataSource dsRead = new DataSource();
		dsRead = lrgsDB.getDataSource("type1", "name1");
		printDataSource(dsRead);		
		System.out.println("============================================================");
	}

	private static void testGetDataSource2(LrgsDatabase lrgsDB) throws LrgsDatabaseException
	{
		System.out.println("============================================================");
		System.out.println("Test reading an invalid Data Source Object");
		DataSource dsRead = new DataSource();
		dsRead = lrgsDB.getDataSource("NOtype", "Noname");
		printDataSource(dsRead);		
		System.out.println("============================================================");
	}
	
	private static void printDataSource(DataSource ds)
	{	
		if (ds != null)
		{
			System.out.println("Data Source Object");
			System.out.println("Data Source ID = " + ds.getDataSourceId());
			System.out.println("Data Source LRGS Host = " + ds.getLrgsHost());
			System.out.println("Data Source Name = " + ds.getDataSourceName());
			System.out.println("Data Source Type = " + ds.getDataSourceType());
		}
		else
			System.out.println("Data Source Object is NULL. Not found on the Database.");
	}
	
	/*
	 * Test method for 'lrgs.db.LrgsDatabase.saveDataSource(DataSource)'
	 */
	private static void testSaveDataSource(LrgsDatabase lrgsDB) throws LrgsDatabaseException
	{
		System.out.println("============================================================");
		//Test Data Source save method
		System.out.println("Adding Data Source records.");
		DataSource dsWrite1 = new DataSource(-1, "localhost", "name1", "type1");			
		lrgsDB.saveDataSource(dsWrite1);
		DataSource dsWrite2 = new DataSource(-1, "localhost", "name2", "type2");			
		lrgsDB.saveDataSource(dsWrite2);
		DataSource dsWrite3 = new DataSource(-1, "localhost", "name3", "type3");			
		lrgsDB.saveDataSource(dsWrite3);

		// verify what we inserted
		testGetDataSources(lrgsDB);
		System.out.println("============================================================");
		// Test the data source update sql, update one of the previous records.
		System.out.println("Updating a Data Source record (change type2/name2 to type4/name4.");
		DataSource dsWrite4 = lrgsDB.getDataSource("type2", "name2"); // get record
		dsWrite4.setDataSourceName("name4");
		dsWrite4.setDataSourceType("type4");
		lrgsDB.saveDataSource(dsWrite4); // update
		// verify what we updated
		testGetDataSources(lrgsDB);
		System.out.println("============================================================");
	}
	
	private static void testGetDataSources(LrgsDatabase lrgsDB) throws LrgsDatabaseException
	{
		System.out.println("============================================================");
		DataSource dsRead;
		List<DataSource> dataSourcesList = lrgsDB.getDataSources(false);
		Iterator dsIterator = dataSourcesList.iterator();
		System.out.println("Reading all Data Source records found on the data_source table." +
							"\nDisplay Multiple Data Source Objects");
		if (!dsIterator.hasNext())
		{
			System.out.println("Multiple Data Source Object list is empty, no records found");
		}
		while (dsIterator.hasNext())
		{
			dsRead = (DataSource)dsIterator.next();
			printDataSource(dsRead);
		}
		System.out.println("============================================================");
	}
	
	private static void testDeleteDataSource(LrgsDatabase lrgsDB) throws LrgsDatabaseException
	{
		System.out.println("============================================================");
		System.out.println("Deleting a Data Source record. (type1, name1)");
		DataSource dsToDelete = new DataSource();
		dsToDelete = lrgsDB.getDataSource("type1", "name1"); // get record
		lrgsDB.deleteDataSource(dsToDelete); // delete record
		// Try to get record again to verify that it was deleted.
		System.out.println("Verify that the Data Source record. (type1, name1) was deleted.");
		dsToDelete = lrgsDB.getDataSource("type1", "name1");
		printDataSource(dsToDelete);
		System.out.println("============================================================");
	}
	
	private static void testDeleteDataSources(LrgsDatabase lrgsDB) throws LrgsDatabaseException
	{
		System.out.println("============================================================");
		System.out.println("Deleting Data Source records left.");
		DataSource dsToDelete = new DataSource();
		dsToDelete = lrgsDB.getDataSource("type3", "name3"); // get record
		lrgsDB.deleteDataSource(dsToDelete); //delete record
		dsToDelete = lrgsDB.getDataSource("type4", "name4"); // get record
		lrgsDB.deleteDataSource(dsToDelete); //delete record		
		System.out.println("============================================================");
	}
	
	private static void testSaveDdsConnectionStats(LrgsDatabase lrgsDB, SimpleDateFormat dateFmt) throws LrgsDatabaseException
	{
		System.out.println("============================================================");
		System.out.println("Saving a couple of Dds Connection stat records");
		DdsConnectionStats ddsConnectionStats;
		/**
		 *	Parameters:
		 * 	int connectionId, Date startTime, Date endTime, String fromIpAddr,
		 *	char successCode, String userName, int msgsReceived, boolean admin_done
		 */
		ddsConnectionStats = new DdsConnectionStats(-1, "localhost", startTime1, endTime1, "192.168.1.47",
				'A', "username", 20, false, 0, null);
		lrgsDB.logDdsConn(ddsConnectionStats);
		
		ddsConnectionStats = new DdsConnectionStats(-1, "localhost", startTime2, endTime2, "192.168.1.48",
				'U', "username2", 21, true, 0, null);
		lrgsDB.logDdsConn(ddsConnectionStats);
		
		ddsConnectionStats = new DdsConnectionStats(-1, "localhost", startTime3, endTime3, "192.168.1.49",
				'U', "username3", 22, false, 0, null);
		lrgsDB.logDdsConn(ddsConnectionStats);
		
		System.out.println("Display the records saved.");
		testGetDdsConnectionStats(lrgsDB, dateFmt);
		
		// Test Update
		System.out.println("Now test updating an inserted record. " +
				"Updating Dds Connection Stat with connection_id = " + ddsConnectionStats.getConnectionId());
		ddsConnectionStats.setFromIpAddr("000.000.0.11");
		ddsConnectionStats.setMsgsReceived(40);
		lrgsDB.logDdsConn(ddsConnectionStats);
		
		System.out.println("============================================================");
	}

	/**
	 * 
	 */
	private static void fillOutDateTimes(SimpleDateFormat dateFmt)
	{	    
		try
		{
			startTime1 = dateFmt.parse("2007-01-01 09:01:10");
			endTime1 = dateFmt.parse("2007-01-01 10:01:10");
			
			startTime2 = dateFmt.parse("2007-01-02 11:01:10");
		    endTime2 = dateFmt.parse("2007-01-02 12:01:10");
		    
		    startTime3 = dateFmt.parse("2007-01-03 12:01:10");
		    endTime3 = dateFmt.parse("2007-01-03 13:01:10");
		    
		    //beforeTime = dateFmt.parse("2007-01-02 12:01:10");
		} catch (ParseException e)
		{
			e.printStackTrace();
		}
	}
	
	private static void testGetDdsConnectionStats(LrgsDatabase lrgsDB, SimpleDateFormat dateFmt) throws LrgsDatabaseException
	{
		System.out.println("============================================================");
		DdsConnectionStats statsRead;
		
		List<DdsConnectionStats> statsList = lrgsDB.getConnectionStats(startTime1, endTime2);
		Iterator statsIterator = statsList.iterator();
		System.out.println("Reading all DDS Connection records found on the " +
							"dds_connection table from ["+ dateFmt.format(startTime1) + 
							"] to [" + dateFmt.format(endTime2) + "].");
		if (!statsIterator.hasNext())
		{
			System.out.println("Multiple Dds Connection Stats Object list is empty, no records found");
		}
		while (statsIterator.hasNext())
		{
			statsRead = (DdsConnectionStats)statsIterator.next();
			printDdsConnectionStats(statsRead, dateFmt);
		}
		System.out.println("============================================================");
	}
	
	private static void testGetDdsConnectionStats2(LrgsDatabase lrgsDB, SimpleDateFormat dateFmt) throws LrgsDatabaseException
	{
		System.out.println("============================================================");
		DdsConnectionStats statsRead;
		
		List<DdsConnectionStats> statsList = lrgsDB.getConnectionStats(startTime1, endTime1);
		Iterator statsIterator = statsList.iterator();
		System.out.println("Reading all DDS Connection records found on the " +
							"dds_connection table from ["+ dateFmt.format(startTime1) + 
							"] to [" + dateFmt.format(endTime1) + "].");
		if (!statsIterator.hasNext())
		{
			System.out.println("Multiple Dds Connection Stats Object list is empty, no records found");
		}
		while (statsIterator.hasNext())
		{
			statsRead = (DdsConnectionStats)statsIterator.next();
			printDdsConnectionStats(statsRead, dateFmt);
		}
		
		statsList = lrgsDB.getConnectionStats(startTime1, null);
		statsIterator = statsList.iterator();
		System.out.println("Reading all DDS Connection records found on the " +
							"dds_connection table from ["+ dateFmt.format(startTime1) + 
							"].");
		if (!statsIterator.hasNext())
		{
			System.out.println("Multiple Dds Connection Stats Object list is empty, no records found");
		}
		while (statsIterator.hasNext())
		{
			statsRead = (DdsConnectionStats)statsIterator.next();
			printDdsConnectionStats(statsRead, dateFmt);
		}
		
		statsList = lrgsDB.getConnectionStats(null, endTime2);
		statsIterator = statsList.iterator();
		System.out.println("Reading all DDS Connection records found on the " +
							"dds_connection table until ["+ dateFmt.format(endTime2) + 
							"].");
		if (!statsIterator.hasNext())
		{
			System.out.println("Multiple Dds Connection Stats Object list is empty, no records found");
		}
		while (statsIterator.hasNext())
		{
			statsRead = (DdsConnectionStats)statsIterator.next();
			printDdsConnectionStats(statsRead, dateFmt);
		}
		
		System.out.println("============================================================");
	}
		
	private static void printDdsConnectionStats(DdsConnectionStats statsRead, SimpleDateFormat dateFmt)
	{
		if (statsRead != null)
		{
			System.out.println("Dds Connection Stats Object");
			System.out.println("Connection ID = " + statsRead.getConnectionId());
			System.out.println("Start Time = " + dateFmt.format(statsRead.getStartTime()));
			System.out.println("End Time = " + dateFmt.format(statsRead.getEndTime()));
			System.out.println("From Ip Address = " + statsRead.getFromIpAddr());
			System.out.println("Success Code = " + statsRead.getSuccessCode());
			System.out.println("User Name = " + statsRead.getUserName());
			System.out.println("Msgs Received = " + statsRead.getMsgsReceived());
			System.out.println("Admin Done = " + statsRead.isAdmin_done());
		}
	}
	
	private static void testDeleteDdsStats(LrgsDatabase lrgsDB, SimpleDateFormat dateFmt) throws LrgsDatabaseException
	{
		System.out.println("============================================================");
		System.out.println("Deleting dds_connection records and dds_period_stats records " +
				"that are before [" + dateFmt.format(endTime2) + "].");
		
		lrgsDB.deleteDdsStatsBefore(endTime2);
		
		// Verify that records before that date were deleted.
		System.out.println("Verify that Dds Connection records before [" +
				dateFmt.format(endTime2) + "] were deleted.");
		testGetDdsConnectionStats(lrgsDB, dateFmt);
		
		System.out.println("Verify that Dds Period Stats records before [" +
				dateFmt.format(endTime2) + "] were deleted.");
		testGetDdsPeriodStats(lrgsDB, dateFmt);
		
		System.out.println("============================================================");
	}
	
	private static void testSaveDdsPeriodStats(LrgsDatabase lrgsDB, SimpleDateFormat dateFmt) throws LrgsDatabaseException
	{
		System.out.println("============================================================");
		System.out.println("Saving a couple of Dds Period stat records");
		DdsPeriodStats ddsPeriodStats;
		/**
		 *	Parameters:
		 * 	Date startTime, char periodDuration, int numAuth, int numUnAuth,
			int badPasswords, int badUsernames, int maxClients, int minClients,
			int aveClients, int msgsDelivered
		 */
		ddsPeriodStats = new DdsPeriodStats(startTime1, "localhost", 'H', 1, 2, 3, 4 , 5, 6, 7, 8);
		lrgsDB.logDdsPeriodStats(ddsPeriodStats);
		ddsPeriodStats = new DdsPeriodStats(startTime2, "localhost", 'H', 8, 7, 6, 5 , 4, 3, 2, 1);
		lrgsDB.logDdsPeriodStats(ddsPeriodStats);
		ddsPeriodStats = new DdsPeriodStats(endTime1, "localhost", 'H', 1, 2, 3, 4 , 5, 6, 7, 8);
		lrgsDB.logDdsPeriodStats(ddsPeriodStats);
		ddsPeriodStats = new DdsPeriodStats(endTime2, "localhost", 'H', 8, 7, 6, 5 , 4, 3, 2, 1);
		lrgsDB.logDdsPeriodStats(ddsPeriodStats);		
		ddsPeriodStats = new DdsPeriodStats(startTime3, "localhost", 'H', 1, 2, 3, 4 , 5, 6, 7, 8);
		lrgsDB.logDdsPeriodStats(ddsPeriodStats);
		
		System.out.println("Display the records saved.");
		testGetDdsPeriodStats(lrgsDB, dateFmt);
		// Test Update
		System.out.println("Now test updating an inserted record. " +
				"Updating Dds Period Stat where Start_Time = " + dateFmt.format(endTime1));
		ddsPeriodStats = new DdsPeriodStats(endTime1, "localhost", 'H', 10, 20, 30, 40 , 50, 60, 70, 80);
		lrgsDB.logDdsPeriodStats(ddsPeriodStats);
		System.out.println("============================================================");
	}
	
	private static void testGetDdsPeriodStats(LrgsDatabase lrgsDB, SimpleDateFormat dateFmt) throws LrgsDatabaseException
	{
		System.out.println("============================================================");
		DdsPeriodStats statsRead;
		
		List<DdsPeriodStats> statsList = lrgsDB.getPeriodStats(startTime1, endTime2);
		Iterator statsIterator = statsList.iterator();
		System.out.println("Reading all DDS Period Stat records found on the " +
							"dds_period_stats table from ["+ dateFmt.format(startTime1) + 
							"] to [" + dateFmt.format(endTime2) + "].");
		if (!statsIterator.hasNext())
		{
			System.out.println("Multiple Dds Period Stats Object list is empty, no records found");
		}
		while (statsIterator.hasNext())
		{
			statsRead = (DdsPeriodStats)statsIterator.next();
			printDdsPeriodStats(statsRead, dateFmt);
		}
		
		statsList = lrgsDB.getPeriodStats(startTime1, null);
		statsIterator = statsList.iterator();
		System.out.println("Reading all DDS Period Stat records found on the " +
							"dds_period_stats table from ["+ dateFmt.format(startTime1) + "].");
		if (!statsIterator.hasNext())
		{
			System.out.println("Multiple Dds Period Stats Object list is empty, no records found");
		}
		while (statsIterator.hasNext())
		{
			statsRead = (DdsPeriodStats)statsIterator.next();
			printDdsPeriodStats(statsRead, dateFmt);
		}
		
		statsList = lrgsDB.getPeriodStats(null, endTime2);
		statsIterator = statsList.iterator();
		System.out.println("Reading all DDS Period Stat records found on the " +
							"dds_period_stats table until ["+ dateFmt.format(endTime2) + "].");
		if (!statsIterator.hasNext())
		{
			System.out.println("Multiple Dds Period Stats Object list is empty, no records found");
		}
		while (statsIterator.hasNext())
		{
			statsRead = (DdsPeriodStats)statsIterator.next();
			printDdsPeriodStats(statsRead, dateFmt);
		}
		System.out.println("============================================================");
	}
	
	private static void printDdsPeriodStats(DdsPeriodStats statsRead, SimpleDateFormat dateFmt)
	{
		if (statsRead != null)
		{
			System.out.println("Dds Period Stats Object");
			System.out.println("Start Time = " + dateFmt.format(statsRead.getStartTime()));
			System.out.println("Period Duration = " + statsRead.getPeriodDuration());
			System.out.println("Num Auth = " + statsRead.getNumAuth());
			System.out.println("Num Unauth = " + statsRead.getNumUnAuth());
			System.out.println("Bad Passwords = " + statsRead.getBadPasswords());
			System.out.println("Bad UserNames = " + statsRead.getBadUsernames());
			System.out.println("Max Clients = " + statsRead.getMaxClients());
			System.out.println("Min Clients = " + statsRead.getMinClients());
			System.out.println("Ave Clients = " + statsRead.getAveClients());
			System.out.println("Msgs Delivered = " + statsRead.getMsgsDelivered());
		}
	}
	
	private static void testSaveOutages(LrgsDatabase lrgsDB, SimpleDateFormat dateFmt) throws LrgsDatabaseException
	{
		System.out.println("============================================================");
		System.out.println("Saving Outage records (system_outage, domsat_gap, damsnt_outage)");
		Outage outage1;
		Outage outage2;
		Outage outage3;
		/**
		 *	Parameters:
		 * 	int outageId, Date beginTime, Date endTime, char outageType,
			char statusCode, int sourceId, int dcpAddress, int beginSeq, int endSeq
		 */
		// Save System Outage
		outage1 = new Outage(-1, startTime1, endTime1, 'S', 'a', 1, 2, 3, 4);
		lrgsDB.saveOutage(outage1);
		
		// Save Domsat Gap
		outage2 = new Outage(-1, startTime2, endTime2, 'G', 'b', 5, 6, 7, 8);
		lrgsDB.saveOutage(outage2);
		
		// Save Damsnt Outage
		outage3 = new Outage(-1, startTime3, endTime3, 'C', 'c', 9, 10, 11, 12);
		lrgsDB.saveOutage(outage3);
		
		System.out.println("Display the records saved.");
		testGetOutages(lrgsDB, dateFmt);
		// Test Update
		System.out.println("Now test updating outage records. ");
		System.out.println("Updating Outage where outageID = " + outage1.getOutageId());
		// Update System Outage
		outage1 = new Outage(outage1.getOutageId(), startTime1, endTime1, 'S', 'd', 10, 20, 30, 40);
		lrgsDB.saveOutage(outage1);
		// Update Domsat Gap
		System.out.println("Updating Outage where outageID = " + outage2.getOutageId());
		outage2 = new Outage(outage2.getOutageId(), startTime2, endTime2, 'G', 'e', 50, 60, 70, 80);
		lrgsDB.saveOutage(outage2);
		// Update Damsnt Outage
		System.out.println("Updating Outage where outageID = " + outage3.getOutageId());
		outage3 = new Outage(outage3.getOutageId(), startTime3, endTime3, 'C', 'f', 90, 100, 110, 120);
		lrgsDB.saveOutage(outage3);
		
		System.out.println("============================================================");
	}
	
	private static void testGetOutages(LrgsDatabase lrgsDB, SimpleDateFormat dateFmt) throws LrgsDatabaseException
	{
		System.out.println("============================================================");
		Outage outage;
		
		List<Outage> outageList = lrgsDB.getOutages(startTime1, endTime3);
		Iterator outageIterator = outageList.iterator();
		System.out.println("Reading all Outage records found on the " +
							"system_outage/domsat_gap/damsnt_outage tables from ["+ dateFmt.format(startTime1) + 
							"] to [" + dateFmt.format(endTime3) + "].");
		if (!outageIterator.hasNext())
		{
			System.out.println("Multiple Outage Object list is empty, no records found");
		}
		while (outageIterator.hasNext())
		{
			outage = (Outage)outageIterator.next();
			printOutages(outage, dateFmt);
		}
		
		outageList = lrgsDB.getOutages(startTime1, null);
		outageIterator = outageList.iterator();
		System.out.println("Reading all Outage records found on the " +
							"system_outage/domsat_gap/damsnt_outage tables from ["+ dateFmt.format(startTime1) + 
							"].");
		if (!outageIterator.hasNext())
		{
			System.out.println("Multiple Outage Object list is empty, no records found");
		}
		while (outageIterator.hasNext())
		{
			outage = (Outage)outageIterator.next();
			printOutages(outage, dateFmt);
		}
		
		outageList = lrgsDB.getOutages(null, endTime3);
		outageIterator = outageList.iterator();
		System.out.println("Reading all Outage records found on the " +
							"system_outage/domsat_gap/damsnt_outage tables until ["+ dateFmt.format(endTime3) + 
							"].");
		if (!outageIterator.hasNext())
		{
			System.out.println("Multiple Outage Object list is empty, no records found");
		}
		while (outageIterator.hasNext())
		{
			outage = (Outage)outageIterator.next();
			printOutages(outage, dateFmt);
		}
		System.out.println("============================================================");
	}
	
	private static void printOutages(Outage outage, SimpleDateFormat dateFmt)
	{
		if (outage != null)
		{
			
			System.out.println("Outage Object");
			System.out.println("Outage Id = " + outage.getOutageId());
			System.out.println("Begin Time = " + dateFmt.format(outage.getBeginTime()));
			System.out.println("End Time = " + dateFmt.format(outage.getEndTime()));
			System.out.println("Outage Type = " + printFullOutageTypeName(outage.getOutageType()));
			System.out.println("Status Code = " + outage.getStatusCode());
			System.out.println("Source ID = " + outage.getSourceId());
			System.out.println("Dcp Address = " + outage.getDcpAddress());
			System.out.println("Begin Seq = " + outage.getBeginSeq());
			System.out.println("End Seq = " + outage.getEndSeq());
		}
	}
	
	private static String printFullOutageTypeName(char type)
	{   String retValue = "";
		if (type == LrgsConstants.systemOutageType)
		{
			retValue = "System Outage";
		}
		else if (type == LrgsConstants.domsatGapOutageType)
		{
			retValue = "Domsat Gap";
		}
		else if (type == LrgsConstants.damsntOutageType)
		{
			retValue = "Damsnt Outage";
		}
		return retValue;
	}
	
	private static void testDeleteOutages(LrgsDatabase lrgsDB, SimpleDateFormat dateFmt) throws LrgsDatabaseException
	{
		System.out.println("============================================================");
		Outage outage;
		
		List<Outage> outageList = lrgsDB.getOutages(startTime1, endTime3);
		Iterator outageIterator = outageList.iterator();
		System.out.println("Read Outages from " +
							"system_outage/domsat_gap/damsnt_outage table from ["+ dateFmt.format(startTime1) + 
							"] to [" + dateFmt.format(endTime3) + "]. and delete all of them.");
		if (!outageIterator.hasNext())
		{
			System.out.println("Multiple Outage Object list is empty, no records found");
		}
		while (outageIterator.hasNext())
		{
			outage = (Outage)outageIterator.next();
			printOutages(outage, dateFmt);
			// Delete the Outage record.
			System.out.println("Deleting outage record [" + outage.getOutageId() + "] from [" +
					printFullOutageTypeName(outage.getOutageType()) +"].");
			lrgsDB.deleteOutage(outage);			
		}
		System.out.println("============================================================");
	}
	
	private static void testGetAllDdsConnectionStats(LrgsDatabase lrgsDB, SimpleDateFormat dateFmt) throws LrgsDatabaseException
	{
		System.out.println("============================================================");
		DdsConnectionStats statsRead;
		// startTime and endTime are null.
		List<DdsConnectionStats> statsList = lrgsDB.getConnectionStats(null, null);
		Iterator statsIterator = statsList.iterator();
		System.out.println("Reading all DDS Connection records found on the " +
							"dds_connection table.");
		if (!statsIterator.hasNext())
		{
			System.out.println("Multiple Dds Connection Stats Object list is empty, no records found");
		}
		while (statsIterator.hasNext())
		{
			statsRead = (DdsConnectionStats)statsIterator.next();
			printDdsConnectionStats(statsRead, dateFmt);
		}
		
		System.out.println("============================================================");
	}
	
	private static void testGetAllDdsPeriodStats(LrgsDatabase lrgsDB, SimpleDateFormat dateFmt) throws LrgsDatabaseException
	{
		System.out.println("============================================================");
		DdsPeriodStats statsRead;
		
		List<DdsPeriodStats> statsList = lrgsDB.getPeriodStats(null, null);
		Iterator statsIterator = statsList.iterator();
		System.out.println("Reading all DDS Period Stat records found on the " +
							"dds_period_stats table.");
		if (!statsIterator.hasNext())
		{
			System.out.println("Multiple Dds Period Stats Object list is empty, no records found");
		}
		while (statsIterator.hasNext())
		{
			statsRead = (DdsPeriodStats)statsIterator.next();
			printDdsPeriodStats(statsRead, dateFmt);
		}
		System.out.println("============================================================");
	}
	
	private static void testGetAllOutages(LrgsDatabase lrgsDB, SimpleDateFormat dateFmt) throws LrgsDatabaseException
	{
		System.out.println("============================================================");
		Outage outage;
		
		List<Outage> outageList = lrgsDB.getOutages(null, null);
		Iterator outageIterator = outageList.iterator();
		System.out.println("Reading all Outage records found on the " +
							"system_outage/domsat_gap/damsnt_outage tables.");
		if (!outageIterator.hasNext())
		{
			System.out.println("Multiple Outage Object list is empty, no records found");
		}
		while (outageIterator.hasNext())
		{
			outage = (Outage)outageIterator.next();
			printOutages(outage, dateFmt);
		}
		System.out.println("============================================================");
	}
}
