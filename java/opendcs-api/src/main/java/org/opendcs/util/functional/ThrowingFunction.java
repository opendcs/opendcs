package org.opendcs.util.functional;

/**
 * function interface to allow returning result with a checked exception.
 */
@FunctionalInterface
public interface ThrowingFunction<T,R,E extends Exception>
{
    R accept(T value) throws E;
}
