package decodes.util;

/**
 * This interface is used by entities that use properties that are assigned
 * to a sensor. The property can be set in either the Config Sensor or
 * Platform Sensor record.
 */
public interface SensorPropertiesOwner
{
	/**
	 * @return specifications of supported sensor properties.
	 */
	public PropertySpec[] getSensorProps();

}
