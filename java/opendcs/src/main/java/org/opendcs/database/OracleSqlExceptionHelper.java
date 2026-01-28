/*
 * Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
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
package org.opendcs.database;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Utility class for filtering and categorizing Oracle SQL exceptions.
 *
 * This helps distinguish between:
 * - Connection-level errors (socket/network issues) that indicate the connection is compromised
 * - User-defined/business rule errors (ORA >= 20000) that indicate bad input but connection is OK
 * - Other system errors
 */
public final class OracleSqlExceptionHelper
{
    /**
     * Oracle error codes >= 20000 are user-defined (typically business rule violations).
     * These indicate the input/request is bad but the connection is still valid.
     */
    public static final int USER_DEFINED_ERROR_THRESHOLD = 20000;

    private OracleSqlExceptionHelper()
    {
        // Utility class - prevent instantiation
    }

    /**
     * Checks if the exception indicates a connection-level error.
     * These errors suggest the database connection may be compromised and
     * should typically result in throwing DbIoException.
     *
     * @param ex the SQLException to check
     * @return true if this appears to be a connection/network error
     */
    public static boolean isConnectionError(SQLException ex)
    {
        if (ex == null)
        {
            return false;
        }
        String msg = ex.getMessage();
        if (msg == null)
        {
            msg = ex.toString();
        }
        return msg.contains("read from socket")
            || msg.contains("connection is closed")
            || msg.contains("Connection reset")
            || msg.contains("Broken pipe")
            || msg.contains("Socket closed");
    }

    /**
     * Checks if the exception is a user-defined Oracle error (ORA >= 20000).
     * These are typically CWMS business rule violations where the input is bad
     * but the connection is still valid.
     *
     * @param ex the SQLException to check
     * @return true if this is a user-defined error (ORA code >= 20000)
     */
    public static boolean isUserDefinedError(SQLException ex)
    {
        Optional<Integer> errorCode = extractOracleErrorCode(ex);
        return errorCode.isPresent() && errorCode.get() >= USER_DEFINED_ERROR_THRESHOLD;
    }

    /**
     * Extracts the Oracle error code from a SQLException.
     * First tries SQLException.getErrorCode(), then falls back to parsing
     * the "ORA-NNNNN" pattern from the exception message.
     *
     * @param ex the SQLException to extract the code from
     * @return Optional containing the error code, or empty if not found
     */
    public static Optional<Integer> extractOracleErrorCode(SQLException ex)
    {
        if (ex == null)
        {
            return Optional.empty();
        }

        // First try the standard JDBC method
        int errorCode = ex.getErrorCode();
        if (errorCode != 0)
        {
            return Optional.of(errorCode);
        }

        // Fall back to parsing the ORA- pattern from the message
        // This handles cases where the error code isn't properly propagated
        String exs = ex.toString();
        int oraidx = exs.indexOf("ORA-");
        if (oraidx >= 0)
        {
            exs = exs.substring(oraidx + 4);
            int intlen = 0;
            for (; intlen < exs.length() && Character.isDigit(exs.charAt(intlen)); intlen++)
            {
                // Count digits
            }
            if (intlen > 0)
            {
                try
                {
                    return Optional.of(Integer.parseInt(exs.substring(0, intlen)));
                }
                catch (NumberFormatException ex2)
                {
                    // Fall through and return empty
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Determines if a SQLException represents a "recoverable" error where
     * the operation failed but the connection is still valid.
     * This includes user-defined errors (business rules) but excludes
     * connection-level errors.
     *
     * @param ex the SQLException to check
     * @return true if the error is recoverable (connection still OK)
     */
    public static boolean isRecoverableError(SQLException ex)
    {
        if (isConnectionError(ex))
        {
            return false;
        }
        return isUserDefinedError(ex);
    }
}
