package org.opendcs.util.functional;

@FunctionalInterface
public interface ThrowingRunnable<E extends Exception>
{
    void run() throws E;
}
