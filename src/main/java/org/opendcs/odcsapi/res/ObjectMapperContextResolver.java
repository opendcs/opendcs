package org.opendcs.odcsapi.res;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;

@Provider
public class ObjectMapperContextResolver implements ContextResolver<ObjectMapper>
{
	private final ObjectMapper mapper;

    public ObjectMapperContextResolver() 
    {
        this.mapper = createObjectMapper();
    }

    @Override
    public ObjectMapper getContext(Class<?> type) 
    {
        return mapper;
    }

    private ObjectMapper createObjectMapper() 
    {
    	System.out.println("createObjectMapper");
        ObjectMapper mapper = new ObjectMapper();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'[z]");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        mapper.setDateFormat(sdf);
        return mapper;
    }
}
