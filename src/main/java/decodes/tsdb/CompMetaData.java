/*
* $Id$
*/
package decodes.tsdb;

/**
Interface for the top-level objects which can be saved-to or read-from
an SQL or XML medium.
*/
public interface CompMetaData
{
	/** @return the type of object. */
	public String getObjectType();

	/** @return a printable name for this object. */
	public String getObjectName();

	public default String typeString()
	{
		return String.format("CompMetaData(type='%s',name='%s')", getObjectType(), getObjectName());
	}
}
