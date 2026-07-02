package org.opendcs.odcsapi.filters;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(0) // We want this even before authentication
public class W3CTraceFilter implements ContainerRequestFilter, ContainerResponseFilter
{
    public static final String SPAN_KEY = "span";
    public static final String SCOPE_KEY = "scope";
    public static final ContextKey<String> TRACE_PARENT = ContextKey.named("traceparent");
    public static final Pattern TRACE_PARENT_MATCHER =
        Pattern.compile("[a-z0-9]{2}-[a-z0-9]{32}-[a-z0-9]{16}-[a-z0-9]{2}");

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException
    {
        var scope = requestContext.getProperty(SCOPE_KEY);
        if (scope != null && scope instanceof Scope s)
        {
            s.close();
        }

        var span = requestContext.getProperty(SPAN_KEY);
        if (span != null && span instanceof Span s)
        {
            s.end();
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException
    {
        var spanBuilder = GlobalOpenTelemetry.getTracer("cwms-data-api")
            // using the full URI here results in too high of cardinality so spans can't be grouped
            // endpoints can/should add the route as attributes to the current span.
            .spanBuilder(requestContext.getMethod())
            .setSpanKind(SpanKind.SERVER);
        var provided = requestContext.getHeaderString(TRACE_PARENT.toString());
        if (provided != null && !provided.isEmpty() && TRACE_PARENT_MATCHER.matcher(provided).matches())
            {
            var propagator = GlobalOpenTelemetry.getPropagators().getTextMapPropagator();
            var ctx = propagator.extract(Context.current(), provided, new TraceGetter());
            spanBuilder.setParent(ctx);
        }
        var span = spanBuilder.startSpan();
        var scope = span.makeCurrent();
        requestContext.setProperty(SPAN_KEY, span);
        requestContext.setProperty(SCOPE_KEY, scope);
    }

    /**
     * A simple wrapper to just get the value in the required way.
     */
    private static class TraceGetter implements TextMapGetter<String>
    {
        @Override
        public Iterable<String> keys(@Nonnull String carrier)
        {
            return List.of(TRACE_PARENT.toString());
        }

        @Override
        @Nullable
        public String get(@Nullable String carrier, @Nonnull String key)
        {
            if (TRACE_PARENT.toString().equalsIgnoreCase(key))
            {
                return carrier;
            }
            else
            {
                return null;
            }
        }
    }

}
