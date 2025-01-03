/*
 * Where Applicable, Copyright 2024 The OpenDCS Consortium or it's contributors.
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
package decodes.cwms.resevapcalc;

import decodes.tsdb.DbCompException;

/**
 * ResEvapException is used to communicate errors occurring in the
 * use of the java ResEvap computations
 *
 * @author Richard Rachiele
 * @version 1.1 June  2015
 */

public class ResEvapException extends DbCompException
{

    /**
     * Constructs a new <code>ResEvapException</code> and copies the
     * message from <code>e</code>.<p>
     *
     * @param e a <code>java.lang.Exception</code>.
     */
    public ResEvapException(Exception e)
    {
        super(e.toString());
    }

    /**
     * Constructs an <code>ResEvapException</code> with the specified
     * detail message.<p>
     *
     * @param message the detail message.
     */
    public ResEvapException(String message)
    {
        super(message);
    }

    public ResEvapException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
