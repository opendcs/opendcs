package org.opendcs.odcsapi.res;

import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationPath("/")
public class RestServices
	extends ResourceConfig
{
	ObjectMapper mapper;
	
	public RestServices()
	{
		System.out.println("Initializing odcsapi RestServices...");
        packages("com.fasterxml.jackson.jaxrs.json");
        packages("opendcs.opentsdb.hydrojson");
	}
}
