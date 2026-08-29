package org.opendcs.util.functional;

/**
 * Function interface to allow for checked exceptions.
 */
@FunctionalInterface
public interface ThrowingConsumer<T,E extends Exception>
{
    void accept(T value) throws E;
}
