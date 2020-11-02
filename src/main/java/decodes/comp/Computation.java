/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:20:59  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2004/08/30 23:26:01  mjmaloney
*  javadoc
*
*  Revision 1.4  2004/08/11 21:40:56  mjmaloney
*  Improved javadocs
*
*  Revision 1.3  2004/06/30 20:01:50  mjmaloney
*  Isolated DECODES interface behind IDataCollection and ITimeSeries interfaces.
*
*  Revision 1.2  2004/06/24 18:36:05  mjmaloney
*  Preliminary working version.
*
*  Revision 1.1  2004/06/24 14:29:52  mjmaloney
*  Created.
*
*/
package decodes.comp;

import java.util.Properties;

/**
Defines abstract interface for DECODES computations and provides
a properties storage mechanism.
*/
public abstract class Computation
{
	/** Poperties passed by the resolver */
	protected Properties props;
	
	/** base class constructor */
	public Computation( )
	{
		props = new Properties();
	}

	/**
	 * Abstract apply() method to be implemented by concrete sub classes.
	 * @param msg the data collection
	 */
	public abstract void apply( IDataCollection msg );
	
	/**
	 * @param name the property name.
	 * @return the value for the named property, or null if not defined.
	 */
	public String getProperty( String name )
	{
		return props.getProperty(name);
	}
	
	/**
	 * Sets a property name/value pair.
	 * @param name the property name
	 * @param value the property value
	 */
	public void setProperty( String name, String value )
	{
		props.setProperty(name, value);
	}
}
