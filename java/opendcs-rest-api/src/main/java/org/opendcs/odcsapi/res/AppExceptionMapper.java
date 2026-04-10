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

import decodes.tsdb.ConstraintException;
import decodes.tsdb.TsdbException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.opendcs.odcsapi.beans.Status;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.spi.LoggingEventBuilder;


@Provider
public final class AppExceptionMapper implements ExceptionMapper<Throwable>
{

	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private static final String INTERNAL_ERROR = "There was an error.  Please contact your sys admin.";

	@Override
	public Response toResponse(Throwable ex)
	{
		return switch(ex)
		{
			case WebAppException webAppException -> handle(webAppException);
			case ConstraintException constraintException -> handle(constraintException);
			case TsdbException tsdbException -> handle(tsdbException);
			case DbException dbException -> handle(dbException);
			case WebApplicationException webApplicationException -> handle(webApplicationException);
			case UnsupportedOperationException unsupportedOperationException -> handle(unsupportedOperationException);
			case null, default -> handle(ex);
		};
	}

	private static Response handle(Throwable ex)
	{
		log.atWarn().setCause(ex).log("Unknown Error");
		Status status = new Status("Bad Request.  There was an issue with the request, please try again or contact your system administrator.");
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
				.entity(status)
				.build();
	}

	private static Response handle(UnsupportedOperationException wae)
	{
		log.atWarn().setCause(wae).log("Unsupported endpoint");
		return Response.status(Response.Status.NOT_IMPLEMENTED)
				.entity(new Status(wae.getMessage()))
				.build();
	}

	private static Response handle(WebApplicationException wae)
	{
		log.atWarn().setCause(wae).log("Error in request");
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
		log.atWarn().setCause(dbex).log(returnErrMsg);
		return Response.status(Response.Status.BAD_REQUEST)
				.entity(new Status(returnErrMsg))
				.build();
	}

	private static Response handle(TsdbException dbex)
	{
		LoggingEventBuilder le = log.atWarn().setCause(dbex);
		if(dbex.getCause() != null)
		{
			String tempCause = dbex.getCause().toString().toLowerCase();
			if(tempCause.contains("value too long"))
			{
				le.log("There was an error saving this.  " +
						"The most likely error is that there is an attempt to save a value that exceeds the allowed length.  " +
						"Please contact your system administrator for more information.");
			}
			else
			{
				le.log("Unexpected DbIoException thrown from request");
			}
			
		}

		return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
				.entity(new Status(INTERNAL_ERROR))
				.build();
	}

	private static Response handle(ConstraintException dbex)
	{
		LoggingEventBuilder le = log.atWarn().setCause(dbex);
		if(dbex.getCause() != null)
		{
			String tempCause = dbex.getCause().toString().toLowerCase();
			if(tempCause.contains("duplicate key value violates unique constraint"))
			{
				le.log("There was an error saving this.  The most likely error is that there is a duplicate key value. " +
					   "Please contact your system administrator for more information.");
			}	
		}
		else
		{
			le.log("Violated constraint exception thrown from request");
		}
		return Response.status(Response.Status.BAD_REQUEST)
				.entity(new Status(INTERNAL_ERROR))
				.build();
	}

	private static Response handle(WebAppException wae)
	{
		LoggingEventBuilder le = (wae.getStatus() >= 500 ? log.atWarn() : log.atInfo() ).setCause(wae);
		if(wae.getStatus() >= 500)
		{
			le.log("Server error");
		}
		else
		{
			le.log("Client error");
		}
		return Response.status(wae.getStatus())
				.entity(new Status(wae.getMessage()))
				.build();
	}

}
