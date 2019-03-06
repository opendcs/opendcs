package opendcs.dbupdate;

import ilex.util.EnvExpander;
import ilex.util.Logger;

import java.io.Console;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import opendcs.opentsdb.OpenTsdb;
import decodes.db.Database;
import decodes.sql.DbKey;
import decodes.sql.DecodesDatabaseVersion;
import decodes.sql.SqlDatabaseIO;
import decodes.tsdb.BadConnectException;
import decodes.tsdb.TsdbAppTemplate;
import decodes.tsdb.TsdbDatabaseVersion;
import decodes.util.CmdLineArgs;

public class DbUpdate extends TsdbAppTemplate
{
	private String username = null;
	private char []password = null;
	
	public DbUpdate(String logname)
	{
		super(logname);
	}

	@Override
	protected void runApp() throws Exception
	{
		System.out.println("Init done.");
		Database.getDb().networkListList.read();

		System.out.println("TSDB Database is currently " + theDb.getTsdbVersion());
		System.out.println("DECODES Database is currently " + theDb.getDecodesDatabaseVersion());

		if (theDb.getTsdbVersion() < TsdbDatabaseVersion.VERSION_9)
		{
			System.out.println("This utility cannot be used on database versions before" +
				" version " + TsdbDatabaseVersion.VERSION_9 + ".");
			System.out.println("Your TSDB database is version " + theDb.getTsdbVersion()+ ".");
			System.out.println("You should create a new database using the scripts in the 'schema'"
				+ " directory that came with OpenDCS.");
			System.out.println("Then export records from the old database and import to the new.");
			System.exit(1);
		}

		String schemaDir = EnvExpander.expand("$DCSTOOL_HOME/schema")
			+ (theDb.isCwms() ? "/cwms30" : 
			   theDb.isOracle() ? "/opendcs-oracle" : "/opendcs-pg");
		System.out.println("Schema dir is '" + schemaDir + "'");

		// For Oracle, we'll need the table space originally defined on db creation.
		String tableSpaceSpec = null;
		if (theDb.isOracle())
		{
			LineNumberReader lnr = new LineNumberReader(
				new FileReader(schemaDir + "/defines.sh"));
			String line;
			while((line = lnr.readLine()) != null)
			{
				if (line.indexOf("TBL_SPACE_DATA=") >= 0)
				{
					tableSpaceSpec = 
						"tablespace " + line.substring(line.indexOf('=') + 1).trim();
					break;
				}
			}
			lnr.close();
		}

		if (theDb.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_11)
		{
			System.out.println("");
			System.out.println("Updating to Database Version 11.");

			if (!theDb.isCwms()) // CWMS doesn't support DCP Monitor Schema
			{
				SQLReader sqlReader = new SQLReader(schemaDir + "/dcp_trans_expanded.sql");
				ArrayList<String> queries = sqlReader.createQueries();
				for(String q : queries)
				{
					if (tableSpaceSpec != null)
					{
						// Oracle will have a table space definition that we need to substitute.
						int tblSpecIdx = q.indexOf("&TBL_SPACE_SPEC");
						if (tblSpecIdx != -1 && tableSpaceSpec != null)
							q = q.substring(0, tblSpecIdx) + tableSpaceSpec;
					}
					sql(q);
				}
			}
			
			// The DACQ_EVENT table was not used before 11. Drop it and recreate below.
			sql("DROP TABLE DACQ_EVENT");
			
			
			SQLReader sqlReader = new SQLReader(schemaDir + "/opendcs.sql");
			ArrayList<String> queries = sqlReader.createQueries();
			for(String q : queries)
				if (q.contains("CP_COMPOSITE_") // New tables CP_COMPOSITE_DIAGRAM and CP_COMPOSITE_MEMBER
				 || q.contains("DACQ_EVENT")    // Modified & previously unused DACQ_EVENT was dropped above
				 || q.contains("SERIAL_PORT_STATUS")) // New table for serial port status
				{
					if (tableSpaceSpec != null)
					{
						// Oracle will have a table space definition that we need to substitute.
						int tblSpecIdx = q.indexOf("&TBL_SPACE_SPEC");
						if (tblSpecIdx != -1 && tableSpaceSpec != null)
							q = q.substring(0, tblSpecIdx) + tableSpaceSpec;
					}
					sql(q);
				}
		
			if (!theDb.isOracle())
			{
				// NetworkListEntry now has columns PLATFORM_NAME and DESCRIPTION
				sql("ALTER TABLE NETWORKLISTENTRY ADD COLUMN PLATFORM_NAME VARCHAR(24)");
				sql("ALTER TABLE NETWORKLISTENTRY ADD COLUMN DESCRIPTION VARCHAR(80)");
				
				// TransportMedium has several new columns to support data loggers via modem & network.
				sql("ALTER TABLE TRANSPORTMEDIUM ADD COLUMN LOGGERTYPE VARCHAR(24)");
				sql("ALTER TABLE TRANSPORTMEDIUM ADD COLUMN BAUD INT");
				sql("ALTER TABLE TRANSPORTMEDIUM ADD COLUMN STOPBITS INT");
				sql("ALTER TABLE TRANSPORTMEDIUM ADD COLUMN PARITY VARCHAR(1)");
				sql("ALTER TABLE TRANSPORTMEDIUM ADD COLUMN DATABITS INT");
				sql("ALTER TABLE TRANSPORTMEDIUM ADD COLUMN DOLOGIN VARCHAR(5)");
				sql("ALTER TABLE TRANSPORTMEDIUM ADD COLUMN USERNAME VARCHAR(32)");
				sql("ALTER TABLE TRANSPORTMEDIUM ADD COLUMN PASSWORD VARCHAR(32)");
				
				sql("CREATE SEQUENCE DACQ_EVENTIdSeq");
			}
			else
			{
				sql("ALTER TABLE NETWORKLISTENTRY ADD PLATFORM_NAME VARCHAR2(24)");
				sql("ALTER TABLE NETWORKLISTENTRY ADD DESCRIPTION VARCHAR2(80)");
				
				sql("ALTER TABLE TRANSPORTMEDIUM ADD LOGGERTYPE VARCHAR2(24)");
				sql("ALTER TABLE TRANSPORTMEDIUM ADD BAUD INT");
				sql("ALTER TABLE TRANSPORTMEDIUM ADD STOPBITS INT");
				sql("ALTER TABLE TRANSPORTMEDIUM ADD PARITY VARCHAR2(1)");
				sql("ALTER TABLE TRANSPORTMEDIUM ADD DATABITS INT");
				sql("ALTER TABLE TRANSPORTMEDIUM ADD DOLOGIN VARCHAR2(5)");
				sql("ALTER TABLE TRANSPORTMEDIUM ADD USERNAME VARCHAR2(32)");
				sql("ALTER TABLE TRANSPORTMEDIUM ADD PASSWORD VARCHAR2(32)");
				
				sql("CREATE SEQUENCE DACQ_EVENTIdSeq nocache");
			}
			
			// Remove season stuff. We are now using Enum for this.
			sql("ALTER TABLE CP_COMPUTATION DROP COLUMN SEASON_ID");
			sql("DROP TABLE SEASON");

			// Can't insert the Season enum here. It will have to be done with dbimport.
			// And for CWMS, it will have to be done separately for each office ID.
//			DbKey seasonKey = theDb.getKeyGenerator().getKey("ENUM", theDb.getConnection());
//			sql("INSERT INTO ENUM VALUES(" + seasonKey + ", 'Season', "
//				+ "NULL, 'Seasons for Conditional Processing')");
		}
		
		if (theDb.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_12)
		{
			System.out.println("");
			System.out.println("Updating to Database Version 12.");

			if (theDb.isOracle())
				sql("ALTER TABLE NETWORKLISTENTRY MODIFY PLATFORM_NAME VARCHAR2(64)");
			else
				sql("ALTER TABLE NETWORKLISTENTRY ALTER COLUMN PLATFORM_NAME TYPE VARCHAR(64)");
		}
		
		if (theDb.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_13)
		{
			System.out.println("");
			System.out.println("Updating to Database Version 13.");

			SQLReader sqlReader = new SQLReader(schemaDir + "/opendcs.sql");
			ArrayList<String> queries = sqlReader.createQueries();
			for(String q : queries)
				if (q.contains("CP_ALGO_SCRIPT")
				 || (theDb.isCwms() && q.contains("DACQ_EVENTIDSEQ"))
				 || (theDb.isCwms() && q.contains("SCHEDULE_ENTRY_STATUSIDSEQ")))
				{
					if (tableSpaceSpec != null)
					{
						// Oracle will have a table space definition that we need to substitute.
						int tblSpecIdx = q.indexOf("&TBL_SPACE_SPEC");
						if (tblSpecIdx != -1 && tableSpaceSpec != null)
							q = q.substring(0, tblSpecIdx) + tableSpaceSpec;
					}
					sql(q);
				}
			if (theDb.isCwms())
			{
				sql("DROP PUBLIC SYNONYM CP_COMPOSITE_DIAGRAM");
				sql("DROP PUBLIC SYNONYM CP_COMPOSITE_MEMBER");
				sql("GRANT SELECT,INSERT,UPDATE,DELETE ON CP_ALGO_SCRIPT TO CCP_USERS");
				sql("GRANT SELECT ON DACQ_EVENTIDSEQ TO CCP_USERS");
				sql("GRANT SELECT ON SCHEDULE_ENTRY_STATUSIDSEQ TO CCP_USERS");
				sql("CREATE PUBLIC SYNONYM CP_ALGO_SCRIPT FOR CCP.CP_ALGO_SCRIPT");
				sql("CREATE PUBLIC SYNONYM DACQ_EVENTIDSEQ FOR CCP.DACQ_EVENTIDSEQ");
				sql("CREATE PUBLIC SYNONYM SCHEDULE_ENTRY_STATUSIDSEQ FOR CCP.SCHEDULE_ENTRY_STATUSIDSEQ");
			}
			sql("DROP TABLE CP_COMPOSITE_DIAGRAM");
			sql("DROP TABLE CP_COMPOSITE_MEMBER");

		}
		if (theDb.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_14)
		{
			// Only CWMS changes were made for 14 and the update is handled by a script.
		}
		if (theDb.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_15)
		{
			System.out.println("");
			System.out.println("Updating to Database Version 15.");

			sql("DELETE FROM DACQ_EVENT");
			if (!theDb.isOracle()) // I.E. PostgreSQL
			{
				// NetworkListEntry now has columns PLATFORM_NAME and DESCRIPTION
				sql("ALTER TABLE DACQ_EVENT ADD COLUMN LOADING_APPLICATION_ID INT");
				sql("ALTER TABLE DACQ_EVENT ADD FOREIGN KEY (LOADING_APPLICATION_ID) "
					+ "REFERENCES HDB_LOADING_APPLICATION (LOADING_APPLICATION_ID) "
					+ "ON UPDATE RESTRICT ON DELETE RESTRICT");
				sql("CREATE SEQUENCE ALARM_GROUPIdSeq");
				sql("CREATE SEQUENCE ALARM_DEFIdSeq");
			}
			else // Oracle
			{
				sql("ALTER TABLE DACQ_EVENT ADD LOADING_APPLICATION_ID INT");
				sql("ALTER TABLE DACQ_EVENT ADD CONSTRAINT DACQ_EVENT_FKLA "
					+ "FOREIGN KEY (LOADING_APPLICATION_ID) REFERENCES "
					+ "HDB_LOADING_APPLICATION(LOADING_APPLICATION_ID)");
				sql("CREATE SEQUENCE ALARM_GROUPIdSeq nocache");
				sql("CREATE SEQUENCE ALARM_DEFIdSeq nocache");
			}
			if (!theDb.isCwms() && !theDb.isHdb())
			{
				SQLReader sqlReader = new SQLReader(schemaDir + "/alarm.sql");
				ArrayList<String> queries = sqlReader.createQueries();
				for(String q : queries)
				{
					if (tableSpaceSpec != null)
					{
						// Oracle will have a table space definition that we need to substitute.
						int tblSpecIdx = q.indexOf("&TBL_SPACE_SPEC");
						if (tblSpecIdx != -1)
							q = q.substring(0, tblSpecIdx) + tableSpaceSpec;
					}
					sql(q);
				}
			}
		}
		if (theDb.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_16)
		{
			System.out.println("");
			System.out.println("Updating to Database Version 16.");
			if (theDb instanceof OpenTsdb)
			{
				// version 16 contains many updates for OpenTSDB
				
				if (theDb.isOracle())
					sql("CREATE SEQUENCE TS_SPECIdSeq nocache");
				else
					sql("CREATE SEQUENCE TS_SPECIdSeq");
			
				sql("DROP TABLE TSDB_DATA_SOURCE CASCADE");
				SQLReader sqlReader = new SQLReader(schemaDir + "/opendcs.sql");
				ArrayList<String> queries = sqlReader.createQueries();
				for(String q : queries)
					if (q.contains("TSDB_DATA_SOURCE"))
					{
						if (tableSpaceSpec != null)
						{
							// Oracle will have a table space definition that we need to substitute.
							int tblSpecIdx = q.indexOf("&TBL_SPACE_SPEC");
							if (tblSpecIdx != -1 && tableSpaceSpec != null)
								q = q.substring(0, tblSpecIdx) + tableSpaceSpec;
						}
						sql(q);
					}
				if (theDb.isOracle())
					sql("CREATE SEQUENCE TSDB_DATA_SOURCEIdSeq nocache");
				else
					sql("CREATE SEQUENCE TSDB_DATA_SOURCEIdSeq");

			
				// Add data_entry_time to string and number data tables
				String cs = theDb.isOracle() ? "" : "COLUMN ";
				String dt = theDb.isOracle() ? "NUMBER(19)" : "BIGINT";
				NumberFormat suffixFmt = NumberFormat.getIntegerInstance();
				suffixFmt.setMinimumIntegerDigits(4);
				suffixFmt.setGroupingUsed(false);
				ResultSet rs = theDb.doQuery("select max(TABLE_NUM) from STORAGE_TABLE_LIST");
				if (rs.next())
				{
					int max = rs.getInt(1);
					for(int tn = 0; tn <= max; tn++)
					{
						String suffix = suffixFmt.format(tn);
						sql("ALTER TABLE TS_NUM_" + suffix + " ADD "
							+ cs + "DATA_ENTRY_TIME " + dt + " NOT NULL");
						sql("ALTER TABLE TS_STRING_" + suffix + " ADD "
							+ cs + "DATA_ENTRY_TIME " + dt + " NOT NULL");
						String s = "CREATE INDEX TS_NUM_" + suffix + "_ENTRY_IDX ON TS_NUM_" + suffix 
							+ "(DATA_ENTRY_TIME)";
						if (tableSpaceSpec != null)
							s = s + " " + tableSpaceSpec;
						sql(s);
						s = "CREATE INDEX TS_STRING_" + suffix + "_ENTRY_IDX ON TS_STRING_" + suffix 
							+ "(DATA_ENTRY_TIME)";
						if (tableSpaceSpec != null)
							s = s + " " + tableSpaceSpec;
						sql(s);
	
					}
				}
			}
		}
		
		if (theDb.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_17)
		{
			System.out.println("");
			System.out.println("Updating to Database Version 17.");
			sql("ALTER TABLE ALARM_DEF RENAME TO ALARM_EVENT");
			sql("ALTER TABLE ALARM_EVENT RENAME COLUMN ALARM_DEF_ID TO ALARM_EVENT_ID");
			
			SQLReader sqlReader = new SQLReader(schemaDir + "/alarm.sql");
			ArrayList<String> queries = sqlReader.createQueries();
			for(String q : queries)
				if (q.toUpperCase().contains("TABLE ALARM_CURRENT")
				 || q.toUpperCase().contains("TABLE ALARM_HISTORY")
				 || q.toUpperCase().contains("TABLE ALARM_LIMIT_SET")
				 || q.toUpperCase().contains("TABLE ALARM_SCREENING")
				 || q.toUpperCase().contains("AS_LAST_MODIFIED"))
					sql(q);
		}

		// Update DECODES Database Version
		sql("UPDATE DECODESDATABASEVERSION SET VERSION_NUM = " + DecodesDatabaseVersion.DECODES_DB_17);
		theDb.setDecodesDatabaseVersion(DecodesDatabaseVersion.DECODES_DB_17, "");
		((SqlDatabaseIO)Database.getDb().getDbIo()).setDecodesDatabaseVersion(
			DecodesDatabaseVersion.DECODES_DB_17, "");
		// Update TSDB_DATABASE_VERSION.
		String desc = "Updated on " + new Date();
		sql("UPDATE TSDB_DATABASE_VERSION SET DB_VERSION = " + TsdbDatabaseVersion.VERSION_17
			+ ", DESCRIPTION = '" + desc + "'");
		theDb.setTsdbVersion(TsdbDatabaseVersion.VERSION_17, desc);
		
		// Rewrite the netlists with the changes.
		Database.getDb().networkListList.write();
	}

	private void sql(String query)
	{
		System.out.println("Executing: " + query);
		try
		{
			theDb.doModify(query);
		}
		catch (Exception ex)
		{
			System.out.println("ERROR: " + ex);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
		throws Exception
	{
		DbUpdate app = new DbUpdate("dbupdate.log");
		app.execute(args);
	}

	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		appNameArg.setDefaultValue("utility");
		// Use console to ask for user name.
		Console console = System.console();
		console.writer().println("Enter user name and password for the CP/DECODES schema owner account.");
		console.writer().print("CP schema owner user name: ");
		console.writer().flush();
		username = console.readLine();
		console.writer().print("Password: ");
		console.writer().flush();
		password = console.readPassword();
	}

	/**
	 * Ask user for username & password for database connection.
	 * Then connect.
	 * Use console.
	 */
	@Override
	public void tryConnect()
		throws BadConnectException
	{
		// Connect to the database!
		Properties props = new Properties();
		props.setProperty("username", username);
		props.setProperty("password", new String(password));

		String nm = appNameArg.getValue();
		Logger.instance().info("Connecting to TSDB as user '" + username + "'");
		setAppId(theDb.connect(nm, props));
	}
}
