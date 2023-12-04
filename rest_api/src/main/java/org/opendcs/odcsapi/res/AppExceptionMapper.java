package org.opendcs.odcsapi.res;

import java.util.logging.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiHttpUtil;

import javax.servlet.http.HttpServletResponse;


@Provider
public class AppExceptionMapper implements ExceptionMapper<Throwable>
{
	@Override
	public Response toResponse(Throwable ex)
	{
		if (ex instanceof WebAppException)
		{
			WebAppException wae = (WebAppException)ex;
			String errmsg = "{ \"status\": " + wae.getStatus() + ", "
				+ "\"message\": \"" + wae.getErrMessage() + "\" }";
			//return Response.status(wae.getStatus()).entity(errmsg).build();
			return ApiHttpUtil.createResponse(errmsg, wae.getStatus());
		}
//		else if (ex instanceof QueryParamException)
//		{
//			QueryParamException qpe = (QueryParamException)ex;
//			// Return generic error message
//			String errmsg = "{ \"status\": " + ErrorCodes.NOT_ALLOWED + ", "
//					+ "\"message\": \"" + "Bad Query Param" + "\" }";
//
//			return Response.status(500).entity(errmsg).build();
//
//		}
		else if (ex instanceof javax.ws.rs.NotFoundException)
		{
			String errmsg = "{ \"status\": " + ErrorCodes.NO_SUCH_OBJECT + ", "
					+ "\"message\": \"" + "No Such Method" + "\" }";

			//return Response.status(500).entity(errmsg).build();
			return ApiHttpUtil.createResponse(errmsg, HttpServletResponse.SC_GONE);
		}
		else if (ex instanceof DbException)
		{
			DbException dbex = (DbException)ex;
			
			String msg = "DbException ";
			if (dbex.getModule() != null)
				msg = msg + "in module " + dbex.getModule();
			msg = msg + ": " + ex;
			Logger.getLogger(ApiConstants.loggerName).info("\t" + "*****BEFORE FIRST WARNING MESSAGE*****");
			Logger.getLogger(ApiConstants.loggerName).info("\t" + "*****ex getMessage: " + ex.getMessage());
			Logger.getLogger(ApiConstants.loggerName).info("\t" + "*****ex getlocalized message: " + ex.getLocalizedMessage());
			Logger.getLogger(ApiConstants.loggerName).info("\t" + "*****ex getcause: " + ex.getCause());
			Logger.getLogger(ApiConstants.loggerName).warning(msg);
			for(StackTraceElement elem : ex.getStackTrace())
			{
				Logger.getLogger(ApiConstants.loggerName).info("\t" + "*****BEFORE*****");
				Logger.getLogger(ApiConstants.loggerName).warning("\t" + elem.toString());
				Logger.getLogger(ApiConstants.loggerName).info("\t" + "*****AFTER*****");
			}
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
				Logger.getLogger(ApiConstants.loggerName).warning("Caused by : " + dbex.getCause());
				for(StackTraceElement elem : dbex.getCause().getStackTrace())
					Logger.getLogger(ApiConstants.loggerName).warning("\t" + elem.toString());
			}

			// Return generic error message
			/* Mike Maloneys default
			String errmsg = "{ \"status\": " + 500 + ", "
					+ "\"message\": \"" + "Database Error -- contact sysadmin." + "\" }";
			*/
			String errmsg = "{ \"status\": " + 400 + ", "
					+ "\"message\": \"" + returnErrMsg + "\" }";
			//return Response.status(500).entity(errmsg).build();
			return ApiHttpUtil.createResponse(errmsg, HttpServletResponse.SC_BAD_REQUEST);
		}
		else
		{
			// Put complete trace info in the log.
			Logger.getLogger(ApiConstants.loggerName).warning("Unexpected Exception: " + ex);
			for(StackTraceElement elem : ex.getStackTrace())
				Logger.getLogger(ApiConstants.loggerName).warning("\t" + elem.toString());

			// Return generic error message
			String errmsg = "{ \"status\": " + 400 + ", "
					+ "\"message\": \"" + "Bad Request.  There was an issue with the request, please try again or contact your system administrator." + "\" }";
			//Response.status(500).entity(errmsg).build();
			return ApiHttpUtil.createResponse(errmsg, HttpServletResponse.SC_BAD_REQUEST);
		}
	}

}
