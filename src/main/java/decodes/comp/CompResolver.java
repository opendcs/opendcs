/**
 * @(#) CompResolver.java
 */
/*
*  $Id$
*
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.1  2008/04/04 18:20:59  cvs
*  Added legacy code to repository
*
*  Revision 1.6  2004/08/30 23:26:01  mjmaloney
*  javadoc
*
*  Revision 1.5  2004/08/24 14:31:28  mjmaloney
*  Added javadocs
*
*  Revision 1.4  2004/08/11 21:40:56  mjmaloney
*  Improved javadocs
*
*  Revision 1.3  2004/06/30 20:01:49  mjmaloney
*  Isolated DECODES interface behind IDataCollection and ITimeSeries interfaces.
*
*  Revision 1.2  2004/06/24 18:36:05  mjmaloney
*  Preliminary working version.
*
*  Revision 1.1  2004/06/24 14:29:51  mjmaloney
*  Created.
*
*/
package decodes.comp;

import decodes.comp.Computation;
import decodes.datasource.RawMessage;
import decodes.db.RoutingSpec;
import decodes.decoder.DecodedMessage;
import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;

import ilex.util.HasProperties;
import ilex.util.PropertiesUtil;

import java.util.Enumeration;
import java.util.Properties;

/**
* Defines base class interface for all CompResolvers.
* A CompResolver examines a DecodedMessage and looks for
* computations which can be performed.
*/
public abstract class CompResolver
	implements PropertiesOwner, HasProperties
{
	/**
	* Properties that affect the operation of the resolver. These are set
	* from the configuration file.
	*/
	protected Properties props;
	
	protected PropertySpec baseClassProps[] = 
	{
		new PropertySpec("NetworkList", PropertySpec.STRING, 
			"Name of DECODES network list. Comps will only be executed " +
			"for stations in the list."),
		new PropertySpec("HostName", PropertySpec.HOSTNAME,
			"Comps will only be executed if exeucting on the named host.")
	};
	
	/**
	* Constroctor for CompResolver base class.
	*/
	public CompResolver( )
	{
		props = new Properties();
	}
	
	/**
	* Finds any computations which can proceed on the passed message.
	* @param msg The data collection
	* @return array of concrete Computation objects.
	*/
	public abstract Computation[] resolve( IDataCollection msg );
	
	/**
	  Sets the properties used by this resolver.
	  @param props the properties
	*/
	public void setProperties( Properties props )
	{
		this.props = props;
	}

	/**
	* Called after configuration properties are set, but before any calls to 
	* resolve.
	 * @param routingSpec The routing spec this resolver is running under, or null
	 * if running under the DCP Monitor. Concrete subclass must be able to handle
	 * null!
	*/
	public abstract void init(RoutingSpec routingSpec );
	
	
	/**
	 * Sets a property.
	 * @param name the property name
	 * @param value the property value
	*/
	public void setProperty(String name, String value)
	{
		props.setProperty(name, value);
	}

	/**
	* Finds a free unused sensor number suitable for creating a new
	* time series.
	* @param msg data collection
	* @return the free sensor number.
	*/
	protected int findFreeSensorNum( IDataCollection msg )
	{
		for(int i=100; i<200; i++)
			if (msg.getITimeSeries(i) == null)
				return i;
		return -1;
	}
	
	@Override
	public PropertySpec[] getSupportedProps()
	{
		return baseClassProps;
	}
	
	/**
	 * Returns false.
	 * Subclass resolvers will return specs for all their properties.
	 * A resolver should always know what properties it accepts.
	 * If it doesn't, it can override this method.
	 */
	@Override
	public boolean additionalPropsAllowed()
	{
		return false;
	}
	
	@Override
	public String getProperty(String name)
	{
		return PropertiesUtil.getIgnoreCase(props, name);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Enumeration getPropertyNames()
	{
		return props.keys();
	}

	@Override
	public void rmProperty(String name)
	{
		PropertiesUtil.rmIgnoreCase(props, name);
	}
	
	/**
	 * If resolver uses any resources that need to be closed, it can override
	 * this method. It is called once when the CompProcessor shuts down.
	 */
	public void shutdown()
	{
		// base class does nothing
	}
	
	/**
	 * Return platform medium ID if one is assigned.
	 * @param msg
	 * @return
	 */
	public String getPlatformContext(IDataCollection msg)
	{
		if (!(msg instanceof DecodedMessage))
			return "Unknown_Context";
		DecodedMessage dmsg = (DecodedMessage)msg;
		if (dmsg.getRawMessage() == null)
			return "Unknown_DCP";
		RawMessage rmsg = dmsg.getRawMessage();
		return rmsg.getMediumId();
	}
}
