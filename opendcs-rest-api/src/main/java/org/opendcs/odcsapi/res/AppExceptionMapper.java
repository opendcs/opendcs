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

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import decodes.tsdb.ConstraintException;
import decodes.tsdb.TsdbException;
import org.opendcs.odcsapi.beans.Status;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Provider
public final class AppExceptionMapper implements ExceptionMapper<Throwable>
{

	private static final Logger LOGGER = LoggerFactory.getLogger(AppExceptionMapper.class);
	private static final String INTERNAL_ERROR = "There was an error.  Please contact your sys admin.";

	@Override
	public Response toResponse(Throwable ex)
	{
		Response retval;
		if(ex instanceof WebAppException)
		{
			retval = handle((WebAppException) ex);
		}
		else if(ex instanceof ConstraintException)
		{
			retval = handle((ConstraintException) ex);
		}
		else if(ex instanceof TsdbException)
		{
			retval = handle((TsdbException) ex);
		}
		else if(ex instanceof DbException)
		{
			retval = handle((DbException) ex);
		}
		else if(ex instanceof WebApplicationException)
		{
			retval = handle((WebApplicationException) ex);
		}
		else if(ex instanceof UnsupportedOperationException)
		{
			retval = handle((UnsupportedOperationException) ex);
		}
		else
		{
			retval = handle(ex);
		}
		return retval;
	}

	private static Response handle(Throwable ex)
	{
		LOGGER.warn("Unknown Error", ex);
		Status status = new Status("Bad Request.  There was an issue with the request, please try again or contact your system administrator.");
		return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
				.entity(status)
				.build();
	}

	private static Response handle(UnsupportedOperationException wae)
	{
		LOGGER.warn("Unsupported endpoint", wae);
		return Response.status(HttpServletResponse.SC_NOT_IMPLEMENTED)
				.entity(new Status(wae.getMessage()))
				.build();
	}

	private static Response handle(WebApplicationException wae)
	{
		LOGGER.warn("Error in request", wae);
		String message = wae.getMessage();
		if(wae instanceof InternalServerErrorException)
		{
			message = "Internal Server Error";
		}
		int status = wae.getResponse().getStatus();
		return Response.status(status)
				.entity(new Status(message))
				.build();
	}

	private static Response handle(DbException dbex)
	{
		String returnErrMsg = INTERNAL_ERROR;
		if(dbex.getCause() != null)
		{
			String tempCause = dbex.getCause().toString().toLowerCase();
			if(tempCause.contains("duplicate key value violates unique constraint"))
			{
				returnErrMsg = "There was an error saving this.  The most likely error is that there is a duplicate key value.  Please contact your system administrator for more information.";
			}
			else if(tempCause.contains("value too long"))
			{
				returnErrMsg = "There was an error saving this.  " +
						"The most likely error is that there is an attempt to save a value that exceeds the allowed length.  " +
						"Please contact your system administrator for more information.";
			}
		}
		LOGGER.warn(returnErrMsg, dbex);
		return Response.status(HttpServletResponse.SC_BAD_REQUEST)
				.entity(new Status(returnErrMsg))
				.build();
	}

	private static Response handle(TsdbException dbex)
	{
		LOGGER.warn("Unexpected DbIoException thrown from request", dbex);
		String returnErrMsg = INTERNAL_ERROR;
		if(dbex.getCause() != null)
		{
			String tempCause = dbex.getCause().toString().toLowerCase();
			if(tempCause.contains("value too long"))
			{
				returnErrMsg = "There was an error saving this.  " +
						"The most likely error is that there is an attempt to save a value that exceeds the allowed length.  " +
						"Please contact your system administrator for more information.";
			}
			LOGGER.warn(returnErrMsg, dbex);
		}
		return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
				.entity(new Status(returnErrMsg))
				.build();
	}

	private static Response handle(ConstraintException dbex)
	{
		String returnErrMsg = INTERNAL_ERROR;
		if(dbex.getCause() != null)
		{
			String tempCause = dbex.getCause().toString().toLowerCase();
			if(tempCause.contains("duplicate key value violates unique constraint"))
			{
				returnErrMsg = "There was an error saving this.  The most likely error is that there is a duplicate key value.  Please contact your system administrator for more information.";
			}
			LOGGER.warn(returnErrMsg, dbex);
		}
		else
		{
			LOGGER.warn("Violated constraint exception thrown from request", dbex);
		}
		return Response.status(HttpServletResponse.SC_BAD_REQUEST)
				.entity(new Status(returnErrMsg))
				.build();
	}

	private static Response handle(WebAppException wae)
	{
		if(wae.getStatus() >= 500)
		{
			LOGGER.warn("Server error", wae);
		}
		else
		{
			LOGGER.info("Client error", wae);
		}
		return Response.status(wae.getStatus())
				.entity(new Status(wae.getMessage()))
				.build();
	}

}
