/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:20:59  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/08/30 23:26:01  mjmaloney
*  javadoc
*
*  Revision 1.2  2004/08/11 21:40:57  mjmaloney
*  Improved javadocs
*
*  Revision 1.1  2004/06/30 20:01:50  mjmaloney
*  Isolated DECODES interface behind IDataCollection and ITimeSeries interfaces.
*
*/
package decodes.comp;

import java.util.Iterator;

/**
Defines the interface that collections of ITimeSeries objects must support
in order to use the computation infrastructure.
*/
public interface IDataCollection
{
	/**
	* @return an iterator for accessing the collection of ITimeSeries objects.
	*/
	public Iterator getAllTimeSeries();

	/**
	* Finds the ITimeSeries object corresponding to the passed sensor ID.
	*
	* @param sensorId  Unique sensor ID for a time series.
	* @return ITimeSeries object or null if no match found.
	*/
	public ITimeSeries getITimeSeries(int sensorId);

	/**
	* Collection must support capability to create & add new time series.
	* @param sensorId the sensor identifier
	* @param name the sensor name
	*/
	public ITimeSeries newTimeSeries(int sensorId, String name);

	/**
	* Removes a time series from the collection.
	* @param ts the time series object
	*/
	public void rmTimeSeries(ITimeSeries ts);
}
