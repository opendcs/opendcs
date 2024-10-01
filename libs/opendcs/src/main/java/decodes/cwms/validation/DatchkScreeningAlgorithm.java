/**
 * $Id$
 * 
 * Copyright 2015 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * 
 * $Log$
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.2  2011/04/01 20:34:51  mmaloney
 * dev
 *
 * Revision 1.1  2011/03/23 20:31:12  mmaloney
 * First prototype
 *
 */
package decodes.cwms.validation;

import decodes.tsdb.DbCompException;
import decodes.cwms.validation.Screening;
import decodes.tsdb.TimeSeriesIdentifier;

/**
This class extends ScreeningAlgorithm. It supplies the code to 
read/write and cache screenings read from DATCHK files.
 */
public class DatchkScreeningAlgorithm
	extends ScreeningAlgorithm
{
	/** Overloaded from ScreeningAlgorithm. */
	protected Screening getScreening(TimeSeriesIdentifier tsid)
		throws DbCompException
	{
		DatchkReader datchkReader = DatchkReader.instance();
		return datchkReader.getScreening(tsid);
	}
}
