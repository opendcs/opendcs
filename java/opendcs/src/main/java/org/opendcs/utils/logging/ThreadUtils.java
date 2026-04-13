package org.opendcs.utils.logging;

import java.util.function.Supplier;

import org.slf4j.MDC;

/**
 * Utility functions to handling logging context in various Thread environments.
 */
public final class ThreadUtils
{
    private ThreadUtils()
    {
        /* utility class */
    }

    /**
	 * Propgate the MDC context into the given task for things like the Trace ID.
	 * @param <T>
	 * @param task
	 * @return
	 */
	public static <T> Supplier<T> propegate(Supplier<T> task)
	{
		final var currentContext = MDC.getCopyOfContextMap();
		return () ->
		{
			MDC.setContextMap(currentContext);
			try
			{
				return task.get();
			}
			finally
			{
				MDC.clear();
			}
		};
	}
}
