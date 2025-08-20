package decodes.util;

import java.util.Collection;

/**
 * Dynamic Properties are not hard-coded into the owning class.
 * Example: the python properties in a Python Algorithm.
 * The Properties Editor dialogs check to see if the owner implements
 * DynamicPropertiesOwner, and if so, allows the user to edit the
 * description field as well as name (for new props) and value.
 */
public interface DynamicPropertiesOwner
{
	/**
	 * @return true if dynamic properties are allowed within this object.
	 */
	public boolean dynamicPropsAllowed();
	
	/**
	 * @return a collection of all dynamic prop specs defined.
	 */
	public Collection<PropertySpec> getDynamicPropSpecs();
	
	/**
	 * Called if the user changes a description to a dynamic property
	 * @param propName the property name
	 * @param description the new description
	 */
	public void setDynamicPropDescription(String propName, String description);
	
	/**
	 * Returns the description for a specific dynamic property, or null
	 * if no description defined or if propName is unrecognized.
	 * @param propName the property name.
	 * @return the description or null
	 */
	public String getDynamicPropDescription(String propName);
	
	
}
