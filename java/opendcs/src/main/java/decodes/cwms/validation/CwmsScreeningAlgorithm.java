/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package decodes.cwms.validation;

import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.cwms.validation.dao.ScreeningDAI;
import decodes.cwms.validation.dao.TsidScreeningAssignment;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.cwms.CwmsTimeSeriesDb;
import org.opendcs.annotations.algorithm.Algorithm;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

@Algorithm(description = "This class extends ScreeningAlgorithm. It supplies the code to "
+ "read/write screenings read from the CWMS database.")
public class CwmsScreeningAlgorithm	extends ScreeningAlgorithm
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
			log.atWarn().setCause(ex).log("Error while reading screening for '{}'", tsid.getUniqueString());
			return null;
		}
		finally
		{
			if (screeningDAO != null)
				screeningDAO.close();
		}
	}
}
