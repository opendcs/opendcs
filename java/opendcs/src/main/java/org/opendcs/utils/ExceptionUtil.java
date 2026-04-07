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
}
