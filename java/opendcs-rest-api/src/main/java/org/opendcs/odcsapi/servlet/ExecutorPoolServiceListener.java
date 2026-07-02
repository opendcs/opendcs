package org.opendcs.odcsapi.servlet;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.opendcs.util.threading.NamedThreadFactory;

import io.opentelemetry.context.Context;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Holds any thread pools. NOTE: future work will allow configuration of 
 * number of threads per pool. For now, defaulting to 20 for simplicity.
 * ExecutorPoolLifeCycle, allows for 10 computations (per computation
 * is 1 thread for the sse handler and 1 thread for the computation.)
 * 
 * Any executor pool added should be wrapped in Context.taskWrapping
 * to properly propagate trace contexts
 */
@WebListener
public class ExecutorPoolServiceListener implements ServletContextListener
{
    public static final String COMPUATION_SERVICE = "odcs.executors.computations";

    @Override
    public void contextInitialized(ServletContextEvent sce)
    {
        var ctx = sce.getServletContext();
        ctx.setAttribute(COMPUATION_SERVICE, Context.taskWrapping(Executors.newFixedThreadPool(10, new NamedThreadFactory("computations"))));
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce)
    {
        var ctx = sce.getServletContext();
        var computationThreadPool = (ExecutorService)ctx.getAttribute(COMPUATION_SERVICE);
        if (computationThreadPool != null)
        {
            computationThreadPool.shutdown();
        }
    }
}
