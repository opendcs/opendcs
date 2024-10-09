/**
 * $Id$
 * 
 * Copyright 2015 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * 
 * $Log$
 * Revision 1.1  2015/09/17 17:44:56  mmaloney
 * CWMS Screening I/O and Algorithm
 *
 */
package decodes.cwms.validation;

import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.cwms.validation.Screening;
import decodes.cwms.validation.dao.ScreeningDAI;
import decodes.cwms.validation.dao.TsidScreeningAssignment;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.cwms.CwmsTimeSeriesDb;

/**
This class extends ScreeningAlgorithm. It supplies the code to 
read/write screenings read from the CWMS database.
 */
public class CwmsScreeningAlgorithm
	extends ScreeningAlgorithm
{
	@Override
	protected Screening getScreening(TimeSeriesIdentifier tsid)
		throws DbCompException
	{
		ScreeningDAI screeningDAO = null;
		try
		{
			screeningDAO = ((CwmsTimeSeriesDb)tsdb).makeScreeningDAO();
			TsidScreeningAssignment tsa = screeningDAO.getScreeningForTS(tsid);
			return tsa != null && tsa.isActive() ? tsa.getScreening() : null;
		}
		catch (DbIoException ex)
		{
			warning("Error while reading screening for '" + tsid.getUniqueString() + "': " + ex);
			return null;
		}
		finally
		{
			if (screeningDAO != null)
				screeningDAO.close();
		}
	}
}
