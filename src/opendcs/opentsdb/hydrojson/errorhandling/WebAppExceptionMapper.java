package opendcs.opentsdb.hydrojson.errorhandling;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import opendcs.opentsdb.hydrojson.errorhandling.ErrorMessage;
import opendcs.opentsdb.hydrojson.errorhandling.WebAppException;

public class WebAppExceptionMapper 
	implements ExceptionMapper<WebAppException>
{

	@Override
	public Response toResponse(WebAppException wae)
	{
		return 
			Response.status(wae.getStatus())
				.entity(new ErrorMessage(wae))
				.type(MediaType.APPLICATION_JSON).
				build();
	}
}
