package org.opendcs.util.threading;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ThreadFactory that allows providing a specific name for the thread
 * NamedThreadFactory
 */
public final class NamedThreadFactory implements ThreadFactory
{
    private final String prefix;
    private final AtomicInteger threadNum = new AtomicInteger(0);

    public NamedThreadFactory(String prefix)
    {
        this.prefix = prefix + "-%d";
    }

    @Override
    public Thread newThread(Runnable r) 
    {
        int num = threadNum.incrementAndGet();
        return new Thread(r, String.format(prefix, num));
    }
}
