package opendcs.opentsdb.hydrojson;

import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

import opendcs.opentsdb.hydrojson.errorhandling.WebAppExceptionMapper;

@ApplicationPath("/")
public class RestServices
	extends ResourceConfig
{
	public RestServices()
	{
        packages("com.fasterxml.jackson.jaxrs.json");
        packages("opendcs.opentsdb.hydrojson");
        register(WebAppExceptionMapper.class);
	}
}
