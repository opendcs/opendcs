/*
 * Copyright 2025 The OpenDCS Consortium and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.opendcs.algorithms;

import java.util.Date;

import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbCompException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalCodes;
import opendcs.dai.TimeSeriesDAI;

public final class TimeSeriesUtil
{
	private TimeSeriesUtil()
	{
		throw new AssertionError("Utility class");
	}

	public static void extendTimeSeries(TimeSeriesDAI timeSeriesDAI, CTimeSeries timeSeries, Date start, Date end) throws DbCompException
	{
		if(timeSeries.findWithin(start, 0) != null && timeSeries.findWithin(end, 0) != null)
		{
			return;
		}
		try
		{
			timeSeriesDAI.fillTimeSeries(timeSeries, start, end);
		}
		catch(DbIoException | BadTimeSeriesException e)
		{
			throw new DbCompException("Could not retrieve time series: " + timeSeries.getTimeSeriesIdentifier(), e);
		}
	}
}
