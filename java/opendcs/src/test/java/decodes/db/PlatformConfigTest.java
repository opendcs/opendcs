package decodes.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PlatformConfigTest
{
	@Test
	void clearDecodingDefinitionRemovesSensorsAndScripts()
	{
		PlatformConfig config = new PlatformConfig("test-config");
		config.addSensor(new ConfigSensor(config, 1));
		config.decodesScripts.add(null);

		config.clearDecodingDefinition();

		assertEquals(0, config.getNumSensors());
		assertEquals(0, config.getNumScripts());
	}
}
