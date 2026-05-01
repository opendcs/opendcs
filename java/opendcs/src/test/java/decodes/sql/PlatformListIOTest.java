package decodes.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendcs.database.SimpleDataSource;

import decodes.db.Database;
import decodes.db.Platform;
import decodes.db.PlatformConfig;
import decodes.util.DecodesSettings;

class PlatformListIOTest
{
	private static final String DB_NAME = "platformListIoTest";
	private static final String DERBY_JDBC_URL = "jdbc:derby:memory:" + DB_NAME;

	private Database oldDb;
	private Database db;
	private String jdbcUrl;
	private Properties jdbcProperties;
	private boolean externalDatabase;

	@BeforeEach
	void setUp()
		throws Exception
	{
		oldDb = Database.getDb();
		jdbcUrl = System.getProperty("platformListIoTest.jdbcUrl", DERBY_JDBC_URL + ";create=true");
		externalDatabase = System.getProperty("platformListIoTest.jdbcUrl") != null;
		jdbcProperties = jdbcProperties();
		try (Connection conn = DriverManager.getConnection(jdbcUrl, jdbcProperties))
		{
			createSchema(conn);
			insertPlatformWithConfig(conn, "old-sensor");
		}

		DecodesSettings settings = new DecodesSettings();
		settings.editDatabaseTypeCode = DecodesSettings.DB_SQL;
		settings.editDatabaseLocation = jdbcUrl;

		SqlDatabaseIO dbio = new SqlDatabaseIO(new SimpleDataSource(jdbcUrl, jdbcProperties), settings);
		db = new Database(true);
		db.setDbIo(dbio);
		Database.setDb(db);
	}

	@AfterEach
	void tearDown()
		throws Exception
	{
		Database.setDb(oldDb);
		if (externalDatabase)
		{
			try (Connection conn = DriverManager.getConnection(jdbcUrl, jdbcProperties))
			{
				dropSchemaObjects(conn);
			}
			return;
		}
		try
		{
			DriverManager.getConnection(DERBY_JDBC_URL + ";drop=true");
		}
		catch (SQLException ex)
		{
			if (!"08006".equals(ex.getSQLState()))
				throw ex;
		}
	}

	@Test
	void readPlatformListRefreshesExistingConfigDefinition()
		throws Exception
	{
		db.getDbIo().readPlatformList(db.platformList);

		Platform platform = db.platformList.getById(DbKey.createDbKey(100L));
		assertNotNull(platform);
		PlatformConfig config = platform.getConfig();
		assertNotNull(config);
		assertEquals("old-sensor", config.getSensor(1).sensorName);

		try (Connection conn = DriverManager.getConnection(jdbcUrl, jdbcProperties);
			 Statement stmt = conn.createStatement())
		{
			stmt.executeUpdate("UPDATE ConfigSensor SET SensorName = 'new-sensor' "
				+ "WHERE ConfigId = 200 AND SensorNumber = 1");
		}

		db.getDbIo().readPlatformList(db.platformList);

		Platform rereadPlatform = db.platformList.getById(DbKey.createDbKey(100L));
		assertSame(platform, rereadPlatform);
		assertSame(config, rereadPlatform.getConfig());
		assertEquals("new-sensor", rereadPlatform.getConfig().getSensor(1).sensorName);
		assertEquals(1, rereadPlatform.getConfig().getNumSensors());
	}

	private static void createSchema(Connection conn)
		throws SQLException
	{
		dropSchemaObjects(conn);
		try (Statement stmt = conn.createStatement())
		{
			stmt.executeUpdate("CREATE TABLE DecodesDatabaseVersion "
				+ "(VersionNum INT NOT NULL, Options VARCHAR(64))");
			stmt.executeUpdate("INSERT INTO DecodesDatabaseVersion VALUES (7, '')");
			stmt.executeUpdate("CREATE TABLE Tsdb_Database_Version "
				+ "(VersionNum INT NOT NULL, Description VARCHAR(64))");
			stmt.executeUpdate("INSERT INTO Tsdb_Database_Version VALUES (2, '')");
			stmt.executeUpdate("CREATE TABLE PlatformConfig "
				+ "(ID INT NOT NULL, Name VARCHAR(64) NOT NULL, Description VARCHAR(400), EquipmentId INT)");
			stmt.executeUpdate("CREATE TABLE Platform "
				+ "(ID INT NOT NULL, Agency VARCHAR(64), IsProduction VARCHAR(5), SiteId INT, "
				+ "ConfigId INT, Description VARCHAR(400), LastModifyTime VARCHAR(32), Expiration VARCHAR(32), "
				+ "PlatformDesignator VARCHAR(24))");
			stmt.executeUpdate("CREATE TABLE ConfigSensor "
				+ "(ConfigId INT NOT NULL, SensorNumber INT NOT NULL, SensorName VARCHAR(64) NOT NULL, "
				+ "RecordingMode CHAR(1) NOT NULL, RecordingInterval INT, TimeOfFirstSample INT, "
				+ "EquipmentId INT, AbsoluteMin DOUBLE PRECISION, AbsoluteMax DOUBLE PRECISION, Stat_Cd VARCHAR(5))");
			stmt.executeUpdate("CREATE TABLE ConfigSensorProperty "
				+ "(ConfigId INT NOT NULL, SensorNumber INT NOT NULL, Prop_Name VARCHAR(24) NOT NULL, "
				+ "Prop_Value VARCHAR(240) NOT NULL)");
			stmt.executeUpdate("CREATE TABLE ConfigSensorDataType "
				+ "(ConfigId INT NOT NULL, SensorNumber INT NOT NULL, DataTypeId INT NOT NULL)");
			stmt.executeUpdate("CREATE TABLE DataType "
				+ "(ID INT NOT NULL, Standard VARCHAR(24) NOT NULL, Code VARCHAR(24) NOT NULL)");
			stmt.executeUpdate("CREATE TABLE DecodesScript "
				+ "(ID INT NOT NULL, ConfigId INT NOT NULL, Name VARCHAR(64), Type VARCHAR(64), DataOrder CHAR(1))");
			stmt.executeUpdate("CREATE TABLE TransportMedium "
				+ "(PlatformId INT NOT NULL, MediumType VARCHAR(24) NOT NULL, MediumId VARCHAR(64) NOT NULL, "
				+ "ScriptName VARCHAR(64), ChannelNum INT, AssignedTime INT, TransmitWindow INT, "
				+ "TransmitInterval INT, EquipmentId INT, TimeAdjustment INT, Preamble CHAR(1), TimeZone VARCHAR(64))");
		}
	}

	private static void insertPlatformWithConfig(Connection conn, String sensorName)
		throws SQLException
	{
		try (Statement stmt = conn.createStatement())
		{
			stmt.executeUpdate("INSERT INTO PlatformConfig VALUES (200, 'test-config', 'test config', NULL)");
			stmt.executeUpdate("INSERT INTO Platform VALUES "
				+ "(100, 'TEST', 'true', NULL, 200, 'test platform', '2026-01-01 00:00:00', NULL, NULL)");
			stmt.executeUpdate("INSERT INTO ConfigSensor VALUES "
				+ "(200, 1, '" + sensorName + "', 'U', 0, 0, NULL, NULL, NULL, NULL)");
		}
	}

	private static Properties jdbcProperties()
	{
		Properties props = new Properties();
		String user = System.getProperty("platformListIoTest.user");
		String password = System.getProperty("platformListIoTest.password");
		if (user != null)
		{
			props.setProperty("user", user);
			props.setProperty("username", user);
		}
		if (password != null)
		{
			props.setProperty("password", password);
		}
		return props;
	}

	private static void dropSchemaObjects(Connection conn)
		throws SQLException
	{
		String[] tables = {
			"TransportMedium",
			"DecodesScript",
			"DataType",
			"ConfigSensorDataType",
			"ConfigSensorProperty",
			"ConfigSensor",
			"Platform",
			"PlatformConfig",
			"Tsdb_Database_Version",
			"DecodesDatabaseVersion"
		};
		try (Statement stmt = conn.createStatement())
		{
			for (String table : tables)
			{
				try
				{
					stmt.executeUpdate("DROP TABLE " + table);
				}
				catch (SQLException ex)
				{
					// Missing tables are fine during setup and cleanup.
				}
			}
		}
	}
}
