package org.opendcs.odcsapi.filters;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.propagation.TextMapGetter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;

@WebFilter
@Priority(0) // We want this even before authentication
public class W3CTraceFilter implements Filter
{
    public static final ContextKey<String> TRACE_PARENT = ContextKey.named("traceparent");
    public static final Pattern TRACE_PARENT_MATCHER =
        Pattern.compile("[a-z0-9]{2}-[a-z0-9]{32}-[a-z0-9]{16}-[a-z0-9]{2}");

    @SuppressWarnings("java:S1181") // We're catching Throwable intentionally so it can be recorded in the span.
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException
    {

        var httpRequest = (HttpServletRequest)request;
        var spanBuilder = GlobalOpenTelemetry.getTracer("cwms-data-api")
            // using the full URI here results in too high of cardinality so spans can't be grouped
            // endpoints can/should add the route as attributes to the current span.
            .spanBuilder(httpRequest.getMethod() + " " + request.getServletContext())
            .setSpanKind(SpanKind.SERVER);
        var provided = httpRequest.getHeader(TRACE_PARENT.toString());
        if (provided != null && !provided.isEmpty() && TRACE_PARENT_MATCHER.matcher(provided).matches())
            {
            var propagator = GlobalOpenTelemetry.getPropagators().getTextMapPropagator();
            var ctx = propagator.extract(Context.current(), provided, new TraceGetter());
            spanBuilder.setParent(ctx);
        }

        var span = spanBuilder.startSpan();
        try (var scope = span.makeCurrent())
        {
            chain.doFilter(request, response);
        }
        catch (Throwable t)
        {
            span.recordException(t);
            span.setStatus(StatusCode.ERROR);
            throw t;
        }
        finally 
        {
            span.end();
        }
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
