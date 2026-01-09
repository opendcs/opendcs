package org.opendcs.utils.logging;

import org.slf4j.Logger;
import org.slf4j.MDC;
import org.slf4j.MDC.MDCCloseable;

public final class MDCTimer implements AutoCloseable
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    private final MDCCloseable mdc;
    private final long start;
    private final String name;

    private MDCTimer(String name)
    {
        mdc = MDC.putCloseable("timer", name);
        this.start = System.currentTimeMillis();
        this.name = name;
        log.info("Timer {} started {}", name, start);
    }

    public static MDCTimer startTimer(String name)
    {
        return new MDCTimer(name);
    }

    @Override
    public void close() throws Exception
    {
        long diff = System.currentTimeMillis() - start;
        log.info("Timer {} ended in {} ms", name, diff);
        mdc.close();
    }
    
}
