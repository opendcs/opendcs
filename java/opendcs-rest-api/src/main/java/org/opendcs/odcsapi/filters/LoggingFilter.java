package org.opendcs.odcsapi.filters;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(0) // We want this even before authentication
public final class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter
{
    public static final String CONTEXT_TRACE_ID = "traceID";
    public static final String HEADER_TRACE_ID = "X-Trace-ID";

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException
    {
        MDC.remove(CONTEXT_TRACE_ID);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException
    {
        var xTraceId = requestContext.getHeaderString(HEADER_TRACE_ID);
        String traceId = null;
        if (xTraceId == null || xTraceId.isBlank())
        {
            traceId = UUID.randomUUID().toString();
        }
        else
        {
            traceId = validate(xTraceId); //well that needs some validation.
        }
        MDC.put(CONTEXT_TRACE_ID, traceId);
    }

    private static String validate(String id) throws IOException
    {
        if (id.matches("[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}"))
        {
            return id;
        }
        else
        {
            throw new IOException("Trace id '" + id + "' is not a valid UUIDish value.");
        }
    }
}
