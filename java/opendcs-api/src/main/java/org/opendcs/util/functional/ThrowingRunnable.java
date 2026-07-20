package org.opendcs.util.functional;

/**
 * Intentionally duplicated for demonstration purposes.
 * If the concepts that follow are agreed upon will migrate all
 * independent variants of to this location.
 *
 * ThrowingRunnable
 * @param <E>
 */
@FunctionalInterface
public interface ThrowingRunnable<E extends Exception>
{
    void run() throws E;
}
