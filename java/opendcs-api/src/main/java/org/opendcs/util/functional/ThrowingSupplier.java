package org.opendcs.util.functional;

/**
 * Functional interface that allows throwing an exception
 */
@FunctionalInterface
public interface ThrowingSupplier<R,E extends Exception>
{
    R get() throws E;
}
