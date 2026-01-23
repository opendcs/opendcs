/*
 *  Copyright 2025 OpenDCS Consortium and its Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.res;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import decodes.cwms.CwmsTsId;
import decodes.sql.DbKey;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.TimeSeriesIdentifier;
import ilex.var.TimedVariable;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.ServletContext;
import javax.sql.DataSource;

import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;

import decodes.db.Database;
import decodes.db.DatabaseIO;
import decodes.tsdb.TimeSeriesDb;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.odcsapi.beans.ApiTimeSeriesData;
import org.opendcs.odcsapi.beans.ApiTimeSeriesIdentifier;
import org.opendcs.odcsapi.beans.ApiTimeSeriesValue;
import org.opendcs.odcsapi.dao.OpenDcsDatabaseFactory;

import static org.opendcs.odcsapi.res.DataSourceContextCreator.DATA_SOURCE_ATTRIBUTE_KEY;
import static org.opendcs.odcsapi.util.ApiConstants.ORGANIZATION_HEADER;

@OpenAPIDefinition(
		info = @Info(
				title = "OpenDCS - Swagger",
				description = "OpenDCS Rest API is web application that provides access to the OpenDCS database using JSON (Java Script Object Notation). " +
						"Source and documentation may be found here: [Github Documentation](https://github.com/opendcs/rest_api)",
				version = "0.0.3"
		)
)
public class OpenDcsResource
{
	private static final String UNSUPPORTED_OPERATION_MESSAGE = "Endpoint is unsupported by the OpenDCS REST API.";

	@HeaderParam(ORGANIZATION_HEADER)
	@Parameter(description = "Organization ID for the request", required = true)
	protected String organizationId;

	@Context
	protected ContainerRequestContext request;

	@Context
	protected ServletContext context;

	protected final synchronized OpenDcsDatabase createDb()
	{
		DataSource dataSource = getDataSource();
		return OpenDcsDatabaseFactory.createDb(dataSource, organizationId);
	}

	protected final DataSource getDataSource()
	{
		DataSource dataSource = (DataSource) context.getAttribute(DATA_SOURCE_ATTRIBUTE_KEY);
		if(dataSource == null)
		{
			throw new IllegalStateException("DataSource not found in ServletContext.");
		}
		return dataSource;
	}

	protected final DatabaseIO getLegacyDatabase()
	{
		return createDb().getLegacyDatabase(Database.class)
				.map(db -> 
				{
					// if you hate this line... good. but it can't go away until we've made all the new DAOs.
					// or someone goes through and makes sure Database is always injected. (NOTE: The DAOs will be easier.)
					Database.setDb(db);
					return db.getDbIo();
				})
				.orElseThrow(() -> new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE));
	}

	protected TimeSeriesDb getLegacyTimeseriesDB()
	{
		getLegacyDatabase(); // make sure Database.setDb is called because way too many things use it.
		return createDb().getLegacyDatabase(TimeSeriesDb.class)
				.orElseThrow(() -> new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE));
	}

	static ApiTimeSeriesData dataMap(CTimeSeries cts, Date start, Date end)
	{
		ApiTimeSeriesData ret = new ApiTimeSeriesData();
		ret.setTsid(map(cts.getTimeSeriesIdentifier()));
		ret.setValues(map(cts, start, end));
		return ret;
	}

	static List<ApiTimeSeriesValue> map(CTimeSeries cts, Date start, Date end)
	{
		List<ApiTimeSeriesValue> ret = new ArrayList<>();
		Date current = start;
		TimedVariable tv = cts.findWithin(current, 5);
		if (tv != null && !tv.getTime().before(current))
		{
			current = processSample(tv, current, end, ret);
		}

		while (current.before(end) || current.equals(end))
		{
			TimedVariable value = cts.findNext(current);

			if (value == null)
			{
				break;
			}
			current = processSample(value, current, end, ret);
		}
		return ret;
	}

	static Date processSample(TimedVariable value, Date current, Date end, List<ApiTimeSeriesValue> ret)
	{
		double val = Double.parseDouble(value.valueString());
		ApiTimeSeriesValue apiValue = new ApiTimeSeriesValue(value.getTime(), val, value.getFlags());
		ret.add(apiValue);
		if (current.equals(end))
		{
			return Date.from(end.toInstant().plusSeconds(1));
		}
		else
		{
			return value.getTime();
		}
	}

	static ApiTimeSeriesIdentifier map(TimeSeriesIdentifier tsid)
	{
		if (tsid instanceof CwmsTsId)
		{
			CwmsTsId cTsId = (CwmsTsId)tsid;
			ApiTimeSeriesIdentifier ret = new ApiTimeSeriesIdentifier();
			if(tsid.getKey() != null)
			{
				ret.setKey(cTsId.getKey().getValue());
			}
			else
			{
				ret.setKey(DbKey.NullKey.getValue());
			}
			ret.setUniqueString(cTsId.getUniqueString());
			ret.setDescription(cTsId.getDescription());
			ret.setStorageUnits(cTsId.getStorageUnits());
			ret.setActive(cTsId.isActive());
			return ret;
		}
		else
		{
			// Active flag is not set here because it is not part of the TimeSeriesIdentifier
			ApiTimeSeriesIdentifier ret = new ApiTimeSeriesIdentifier();
			if(tsid.getKey() != null)
			{
				ret.setKey(tsid.getKey().getValue());
			}
			else
			{
				ret.setKey(DbKey.NullKey.getValue());
			}
			ret.setUniqueString(tsid.getUniqueString());
			ret.setDescription(tsid.getDescription());
			ret.setStorageUnits(tsid.getStorageUnits());
			return ret;
		}
	}
}
