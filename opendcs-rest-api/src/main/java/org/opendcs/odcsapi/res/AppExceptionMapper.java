/*
 *  Copyright 2024 OpenDCS Consortium and its Contributors
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

import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.util.ApiHttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Provider
public class AppExceptionMapper implements ExceptionMapper<Throwable>
{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AppExceptionMapper.class);
	
	@Override
	public Response toResponse(Throwable ex)
	{
		if (ex instanceof WebAppException)
		{
			if(((WebAppException) ex).getStatus() >= 500)
			{
				LOGGER.warn("Server error",  ex);
			}
			else
			{
				LOGGER.info("Client error",  ex);
			}
			WebAppException wae = (WebAppException)ex;
			String errmsg = "{ \"status\": " + wae.getStatus() + ", "
				+ "\"message\": \"" + wae.getErrMessage() + "\" }";
			return ApiHttpUtil.createResponse(errmsg, wae.getStatus());
		}
		else if (ex instanceof javax.ws.rs.NotFoundException)
		{
			LOGGER.debug("No data found",  ex);
			String errmsg = "{ \"status\": " + ErrorCodes.NO_SUCH_OBJECT + ", "
					+ "\"message\": \"" + "No Such Method" + "\" }";
			return ApiHttpUtil.createResponse(errmsg, HttpServletResponse.SC_GONE);
		}
		else if (ex instanceof DbException)
		{
			DbException dbex = (DbException)ex;
			
			String msg = "DbException ";
			if (dbex.getModule() != null)
				msg = msg + "in module " + dbex.getModule();
			msg = msg + ": " + ex;
			LOGGER.warn(msg, ex);
			String returnErrMsg = "There was an error.  Please contact your sys admin.";
			if (dbex.getCause() != null)
			{
				String tempCause = dbex.getCause().toString().toLowerCase();
				if (tempCause.contains("duplicate key value violates unique constraint"))
				{
					returnErrMsg = "There was an error saving this.  The most likely error is that there is a duplicate key value.  Please contact your system administrator for more information.";
				}
				else if (tempCause.contains("value too long"))
				{
					returnErrMsg = "There was an error saving this.  The most likely error is that there is an attempt to save a value that exceeds the allowed length.  Please contact your system administrator for more information.";
				}
				LOGGER.warn(returnErrMsg, ex);
			}
			else
			{
				LOGGER.warn(msg, ex);
			}
			String errmsg = "{ \"status\": " + 400 + ", "
					+ "\"message\": \"" + returnErrMsg + "\" }";
			return ApiHttpUtil.createResponse(errmsg, HttpServletResponse.SC_BAD_REQUEST);
		}
		else if( ex instanceof WebApplicationException)
		{
			LOGGER.warn("Error in request", ex);
			String message = ex.getMessage();
			if(ex instanceof InternalServerErrorException)
			{
				message = "Internal Server Error";
			}
			int status = ((WebApplicationException) ex).getResponse().getStatus();
			String errmsg = "{ \"status\": " + status + ", "
					+ "\"message\": \"" + message + "\" }";
			return ApiHttpUtil.createResponse(errmsg, status);
		}
		else
		{
			LOGGER.warn("Unknown Error", ex);
			// Return generic error message
			String errmsg = "{ \"status\": " + 400 + ", "
					+ "\"message\": \"" + "Bad Request.  There was an issue with the request, please try again or contact your system administrator." + "\" }";
			return ApiHttpUtil.createResponse(errmsg, HttpServletResponse.SC_BAD_REQUEST);
		}
	}

}
