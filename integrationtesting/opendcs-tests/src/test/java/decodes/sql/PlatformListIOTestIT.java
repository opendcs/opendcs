package decodes.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.db.Database;
import decodes.db.Platform;
import decodes.db.PlatformConfig;

@EnableIfTsDb
class PlatformListIOTestIT extends AppTestBase
{
	private static final int SENSOR_NUMBER = 1;

	@ConfiguredField
	private OpenDcsDatabase openDcsDatabase;

	@ConfiguredField
	private Database decodesDb;

	private Database oldDb;
	private DbKey platformId;
	private DbKey configId;

	@BeforeEach
	void setUp()
		throws Exception
	{
		oldDb = Database.getDb();
		Database.setDb(decodesDb);
		decodesDb.platformList.clear();
		decodesDb.platformConfigList.clear();

		try (DataTransaction tx = openDcsDatabase.newTransaction())
		{
			Connection conn = tx.connection(Connection.class).orElseThrow();
			KeyGenerator keyGenerator = tx.getContext().getGenerator(KeyGenerator.class).orElseThrow();
			platformId = keyGenerator.getKey("Platform", conn);
			configId = keyGenerator.getKey("PlatformConfig", conn);

			insertPlatformWithConfig(conn, "old-sensor");
			tx.commit();
		}
	}

	@AfterEach
	void tearDown()
		throws Exception
	{
		try
		{
			cleanupFixtureRows();
			decodesDb.platformList.clear();
			decodesDb.platformConfigList.clear();
		}
		finally
		{
			Database.setDb(oldDb);
		}
	}

	@Test
	void readPlatformListRefreshesExistingConfigDefinition()
		throws Exception
	{
		decodesDb.getDbIo().readPlatformList(decodesDb.platformList);

		Platform platform = decodesDb.platformList.getById(platformId);
		assertNotNull(platform);
		PlatformConfig config = platform.getConfig();
		assertNotNull(config);
		assertEquals("old-sensor", config.getSensor(SENSOR_NUMBER).sensorName);

		try (DataTransaction tx = openDcsDatabase.newTransaction())
		{
			Connection conn = tx.connection(Connection.class).orElseThrow();
			updateSensorName(conn, "new-sensor");
			tx.commit();
		}

		decodesDb.getDbIo().readPlatformList(decodesDb.platformList);

		Platform rereadPlatform = decodesDb.platformList.getById(platformId);
		assertSame(platform, rereadPlatform);
		assertSame(config, rereadPlatform.getConfig());
		assertEquals("new-sensor", rereadPlatform.getConfig().getSensor(SENSOR_NUMBER).sensorName);
		assertEquals(1, rereadPlatform.getConfig().getNumSensors());
	}

	private void insertPlatformWithConfig(Connection conn, String sensorName)
		throws Exception
	{
		try (PreparedStatement stmt = conn.prepareStatement(
			"INSERT INTO PlatformConfig (ID, Name, Description, EquipmentId) VALUES (?, ?, ?, NULL)"))
		{
			stmt.setLong(1, configId.getValue());
			stmt.setString(2, "platform-list-io-test-config");
			stmt.setString(3, "platform list io test config");
			stmt.executeUpdate();
		}

		try (PreparedStatement stmt = conn.prepareStatement(
			"INSERT INTO ConfigSensor "
				+ "(ConfigId, SensorNumber, SensorName, RecordingMode, RecordingInterval, TimeOfFirstSample, "
				+ "EquipmentId, AbsoluteMin, AbsoluteMax, Stat_Cd) "
				+ "VALUES (?, ?, ?, ?, ?, ?, NULL, NULL, NULL, NULL)"))
		{
			stmt.setLong(1, configId.getValue());
			stmt.setInt(2, SENSOR_NUMBER);
			stmt.setString(3, sensorName);
			stmt.setString(4, "U");
			stmt.setInt(5, 0);
			stmt.setInt(6, 0);
			stmt.executeUpdate();
		}

		try (PreparedStatement stmt = conn.prepareStatement(
			"INSERT INTO Platform "
				+ "(ID, Agency, IsProduction, SiteId, ConfigId, Description, LastModifyTime, Expiration, "
				+ "PlatformDesignator) "
				+ "VALUES (?, ?, ?, NULL, ?, ?, ?, NULL, NULL)"))
		{
			stmt.setLong(1, platformId.getValue());
			stmt.setString(2, "TEST");
			stmt.setString(3, "true");
			stmt.setLong(4, configId.getValue());
			stmt.setString(5, "platform list io test platform");
			stmt.setTimestamp(6, Timestamp.from(Instant.now()));
			stmt.executeUpdate();
		}
	}

	private void updateSensorName(Connection conn, String sensorName)
		throws Exception
	{
		try (PreparedStatement stmt = conn.prepareStatement(
			"UPDATE ConfigSensor SET SensorName = ? WHERE ConfigId = ? AND SensorNumber = ?"))
		{
			stmt.setString(1, sensorName);
			stmt.setLong(2, configId.getValue());
			stmt.setInt(3, SENSOR_NUMBER);
			stmt.executeUpdate();
		}
	}

	private void cleanupFixtureRows()
		throws Exception
	{
		if (platformId == null || configId == null)
			return;

		try (DataTransaction tx = openDcsDatabase.newTransaction())
		{
			Connection conn = tx.connection(Connection.class).orElseThrow();
			deleteById(conn, "PlatformSensorProperty", "PlatformID", platformId);
			deleteById(conn, "PlatformSensor", "PlatformID", platformId);
			deleteById(conn, "TransportMedium", "PlatformID", platformId);
			deleteById(conn, "Platform", "ID", platformId);
			deleteById(conn, "ConfigSensorDataType", "ConfigID", configId);
			deleteById(conn, "ConfigSensorProperty", "ConfigID", configId);
			deleteById(conn, "ConfigSensor", "ConfigID", configId);
			deleteById(conn, "PlatformConfig", "ID", configId);
			tx.commit();
		}
	}

	private static void deleteById(Connection conn, String table, String column, DbKey id)
		throws Exception
	{
		try (PreparedStatement stmt = conn.prepareStatement(
			"DELETE FROM " + table + " WHERE " + column + " = ?"))
		{
			stmt.setLong(1, id.getValue());
			stmt.executeUpdate();
		}
	}
}
