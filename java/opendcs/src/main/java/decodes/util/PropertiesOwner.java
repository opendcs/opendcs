package decodes.util;

/**
 * Used by classes that can take properties to control their actions.
 * They must support this interface returning specifications for their
 * properties. This allows the GUIs to query for property values in an
 * intuitive way.
 * 
 * @author mmaloney Mike Maloney, Cove Software, LLC.
 */
public interface PropertiesOwner
{
	/**
	 * @return specifications of supported properties.
	 */
	public PropertySpec[] getSupportedProps();
	
	/**
	 * @return true if additional unnamed props are allowed, falis if only the
	 * ones returned by getSupportedProps are allowed.
	 */
	public boolean additionalPropsAllowed();
}
