package decodes.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.sql.Connection;

import org.jdbi.v3.core.Handle;
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

			Handle handle = tx.connection(Handle.class).orElseThrow();
			insertPlatformWithConfig(handle, "old-sensor");
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
			Handle handle = tx.connection(Handle.class).orElseThrow();
			updateSensorName(handle, "new-sensor");
			tx.commit();
		}

		decodesDb.getDbIo().readPlatformList(decodesDb.platformList);

		Platform rereadPlatform = decodesDb.platformList.getById(platformId);
		assertSame(platform, rereadPlatform);
		assertSame(config, rereadPlatform.getConfig());
		assertEquals("new-sensor", rereadPlatform.getConfig().getSensor(SENSOR_NUMBER).sensorName);
		assertEquals(1, rereadPlatform.getConfig().getNumSensors());
	}

	private void insertPlatformWithConfig(Handle handle, String sensorName)
	{
		handle.createUpdate("INSERT INTO PlatformConfig "
				+ "(ID, Name, Description, EquipmentId) "
				+ "VALUES (:id, :name, :description, NULL)")
			.bind("id", configId)
			.bind("name", "platform-list-io-test-config")
			.bind("description", "platform list io test config")
			.execute();

		handle.createUpdate("INSERT INTO ConfigSensor "
				+ "(ConfigId, SensorNumber, SensorName, RecordingMode, RecordingInterval, TimeOfFirstSample, "
				+ "EquipmentId, AbsoluteMin, AbsoluteMax, Stat_Cd) "
				+ "VALUES (:configId, :sensorNumber, :sensorName, :recordingMode, "
				+ ":recordingInterval, :timeOfFirstSample, NULL, NULL, NULL, NULL)")
			.bind("configId", configId)
			.bind("sensorNumber", SENSOR_NUMBER)
			.bind("sensorName", sensorName)
			.bind("recordingMode", "U")
			.bind("recordingInterval", 0)
			.bind("timeOfFirstSample", 0)
			.execute();

		handle.createUpdate("INSERT INTO Platform "
				+ "(ID, Agency, IsProduction, SiteId, ConfigId, Description, LastModifyTime, Expiration, "
				+ "PlatformDesignator) "
				+ "VALUES (:id, :agency, :isProduction, NULL, :configId, :description, "
				+ "NULL, NULL, NULL)")
			.bind("id", platformId)
			.bind("agency", "TEST")
			.bind("isProduction", "true")
			.bind("configId", configId)
			.bind("description", "platform list io test platform")
			.execute();
	}

	private void updateSensorName(Handle handle, String sensorName)
	{
		handle.createUpdate("UPDATE ConfigSensor SET SensorName = :sensorName "
				+ "WHERE ConfigId = :configId AND SensorNumber = :sensorNumber")
			.bind("sensorName", sensorName)
			.bind("configId", configId)
			.bind("sensorNumber", SENSOR_NUMBER)
			.execute();
	}

	private void cleanupFixtureRows()
		throws Exception
	{
		if (platformId == null || configId == null)
			return;

		try (DataTransaction tx = openDcsDatabase.newTransaction())
		{
			Handle handle = tx.connection(Handle.class).orElseThrow();
			deleteById(handle, "PlatformSensorProperty", "PlatformID", platformId);
			deleteById(handle, "PlatformSensor", "PlatformID", platformId);
			deleteById(handle, "TransportMedium", "PlatformID", platformId);
			deleteById(handle, "Platform", "ID", platformId);
			deleteById(handle, "ConfigSensorDataType", "ConfigID", configId);
			deleteById(handle, "ConfigSensorProperty", "ConfigID", configId);
			deleteById(handle, "ConfigSensor", "ConfigID", configId);
			deleteById(handle, "PlatformConfig", "ID", configId);
			tx.commit();
		}
	}

	private static void deleteById(Handle handle, String table, String column, DbKey id)
	{
		handle.createUpdate("DELETE FROM " + table + " WHERE " + column + " = :id")
			.bind("id", id)
			.execute();
	}
}
