/*
* Where Applicable, Copyright 2026 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package org.opendcs.utils;

import java.util.Map;

import opendcs.util.functional.ThrowingFunction;

/**
 * Utility methods for working with exceptions.
 */
public final class ExceptionUtil
{
    private ExceptionUtil() { /* static utility */ }

    /**
     * Walks the cause chain, looking for an instance of targetType.
     * Returns that exception's message if found, otherwise returns the message of the
     * root.
     *
     * @param t          the throwable
     * @param targetType the exception type to look for
     * @return hopefully the most informative message found in the cause chain
     */
    public static String getCauseMessage(Throwable t, Class<? extends Throwable> targetType)
    {
        Throwable current = t;
        Throwable root = t;
        while (current != null)
        {
            if (targetType.isInstance(current))
            {
                return current.getMessage();
            }
            root = current;
            current = current.getCause();
        }
        return root.getMessage();
    }

    /**
     * For situtations where the solution is to just forward the exception
     * @param <K> Map Key Type
     * @param <T> Map value Type
     * @param <E> Expected Exception
     * @param collection collection on which to operate
     * @param key the key
     * @param supplier actually action to be performed on compute if absent
     * @param exceptionType class instance of the type of exception 
     * @return
     * @throws E
     */
    public static <K,T,E extends Exception> T wrappedComputeIfAbsent(Map<K,T> collection, K key, ThrowingFunction<K,T,E> supplier, Class<E> exceptionType) throws E
    {
        try
        {
            return collection.computeIfAbsent(key, newKey ->
            {
                try
                {
                    return supplier.accept(newKey);
                }
                catch (Exception ex)
                {
                    throw new WrappedException(ex);
                }
            });
        }
        catch (RuntimeException ex)
        {
            if (exceptionType.isInstance(ex))
            {
                throw (E)ex.getCause();
            }
            else
            {
                throw ex;
            }
        }

    }


    private static class WrappedException extends RuntimeException
    {
        public WrappedException(Throwable cause)
        {
            super(cause);
        }
    }
}
