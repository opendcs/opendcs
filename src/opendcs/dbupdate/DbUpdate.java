package opendcs.dbupdate;

import ilex.util.Logger;

import java.io.Console;
import java.util.Date;
import java.util.Properties;

import lrgs.gui.DecodesInterface;

import decodes.db.Database;
import decodes.sql.DecodesDatabaseVersion;
import decodes.tsdb.BadConnectException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TsdbAppTemplate;
import decodes.tsdb.TsdbDatabaseVersion;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;

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

		if (theDb.getDecodesDatabaseVersion() == DecodesDatabaseVersion.DECODES_DB_10)
		{
			System.out.println("TSDB Database is currently " + theDb.getTsdbVersion());
			System.out.println("DECODES Database is currently " + theDb.getDecodesDatabaseVersion());
			sql("ALTER TABLE NETWORKLISTENTRY ADD COLUMN PLATFORM_NAME VARCHAR(24)");
			sql("ALTER TABLE NETWORKLISTENTRY ADD COLUMN DESCRIPTION VARCHAR(80)");

			// Update TSDB_DATABASE_VERSION. Note that separate DECODES DB Version is not used
			// for 10 and later.
			String desc = "Updated on " + new Date();
			sql("UPDATE TSDB_DATABASE_VERSION SET DB_VERSION = " + TsdbDatabaseVersion.VERSION_10
				+ ", DESCRIPTION = '" + desc + "'");
			theDb.setTsdbVersion(TsdbDatabaseVersion.VERSION_10, desc);
			sql("UPDATE DECODESDATABASEVERSION SET VERSION_NUM = " + DecodesDatabaseVersion.DECODES_DB_11);
			theDb.setDecodesDatabaseVersion(DecodesDatabaseVersion.DECODES_DB_11, "");
		}
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
	public boolean tryConnect()
	{
		// Connect to the database!
		Properties props = new Properties();
		props.setProperty("username", username);
		props.setProperty("password", new String(password));

		String nm = appNameArg.getValue();
		Logger.instance().info("Connecting to TSDB as user '" + username + "'");
		try
		{
			appId = theDb.connect(nm, props);
			return true;
		}
		catch(BadConnectException ex)
		{
			badConnect(nm, ex);
			return false;
		}
	}
}
