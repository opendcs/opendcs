/*
* $Id$
*/
package ilex.util;

import java.util.Enumeration;

/**
* Objects that can hold properties implement this interface.
*/
public interface HasProperties
{
	/** 
	 * Adds a property to this object's meta-data.
	 * @param name the property name.
	 * @param value the property value.
	 */
	public void setProperty(String name, String value);

	/**
	 * Retrieve a property by name.
	 * @param name the property name.
	 * @return value of name property, or null if not defined.
	 */
	public String getProperty(String name);

	/**
	 * @return enumeration of all names in the property set.
	 */
	public Enumeration getPropertyNames();

	/**
	 * Removes a property assignment.
	 * @param name the property name.
	 */
	public void rmProperty(String name);
}
