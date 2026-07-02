package org.opendcs.odcsapi.servlet;

import java.util.concurrent.ExecutorService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;

/**
 * Injectable helper to retrieve appropriate ExecutorService
 * ExecutorPoolService
 */
@ApplicationScoped
public final class ExecutorPoolService 
{
    @Inject // NOSONAR. Needs to stay this way given "ApplicationScoped."
    private ServletContext context;

    /**
     * Retrieve the appropriate ExecutorService to run computations
     * @return
     */
    public ExecutorService getComputationExecutor()
    {
        return (ExecutorService)context.getAttribute(ExecutorPoolServiceListener.COMPUATION_SERVICE);
    }    
    
}
