package org.opendcs.odcsapi.servlet;

import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class OpenTelemetryInitListener implements ServletContextListener
{
    private static final SdkTracerProvider sdkTracerProvider =
            SdkTracerProvider.builder()
                .build();

    @Override
    public void contextInitialized(ServletContextEvent sce)
    {
        OpenTelemetrySdk.builder()
            .setTracerProvider(sdkTracerProvider)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .buildAndRegisterGlobal();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce)
    {
        sdkTracerProvider.close();
    }
}
