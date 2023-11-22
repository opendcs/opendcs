package org.opendcs.utils.logging;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.Logger;
import org.slf4j.helpers.BasicMDCAdapter;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

public class OpenDcsSLF4JProvider implements SLF4JServiceProvider
{
    private MDCAdapter mdc;
    private IMarkerFactory markerFactory;
    private ILoggerFactory loggerFactory;

    @Override
    public ILoggerFactory getLoggerFactory()
    {
        return loggerFactory;
    }

    @Override
    public MDCAdapter getMDCAdapter()
    {
        return mdc;
    }

    @Override
    public IMarkerFactory getMarkerFactory()
    {
        return markerFactory;
    }

    @Override
    public String getRequestedApiVersion() {
        return "2.0.99";
    }

    @Override
    public void initialize()
    {
        mdc = new BasicMDCAdapter();
        markerFactory = new BasicMarkerFactory();
        loggerFactory = new ILoggerFactory() {

            @Override
            public Logger getLogger(String arg)
            {
                return new SLF4JLogger(arg,ilex.util.Logger.instance());
            }
            
        };
    }
    
}
