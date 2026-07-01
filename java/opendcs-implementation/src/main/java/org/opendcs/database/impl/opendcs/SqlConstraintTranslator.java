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
* License for the specific language governing permissions and limitations under
* the License.
*/
package org.opendcs.database.impl.opendcs;

import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDataConstraintException;
import org.opendcs.database.api.OpenDcsDataException;

import java.sql.SQLException;

/**
 * Translates JDBI/JDBC exceptions into typed OpenDCS exceptions so callers
 * can return correct HTTP status codes without inspecting raw SQLExceptions.
 */
public final class SqlConstraintTranslator
{

    private SqlConstraintTranslator()
    {
    }

    /**
     * Wraps a JDBI RuntimeException (or any exception) into the most specific
     * OpenDcsDataException subtype:
     * - {@link OpenDcsDataConstraintException} for integrity constraint violations
     *   (FK, unique, check). Detected via ANSI SQLState class "23" or by Oracle
     *   vendor error code 2292 as a fallback.
     * - {@link OpenDcsDataException} for all other database errors.
     *
     * @param contextMessage human-readable prefix, e.g. "Unable to delete config"
     * @param engine         database engine enum (may assist engine-specific mapping)
     * @param ex             the exception thrown by JDBI or the underlying JDBC driver
     */
    public static OpenDcsDataException translate(String contextMessage, DatabaseEngine engine, Exception ex)
    {
        SQLException sqlEx = findSqlException(ex);
        if (sqlEx != null && isConstraintViolation(engine, sqlEx))
        {
            return new OpenDcsDataConstraintException(contextMessage, ex);
        }
        return new OpenDcsDataException(contextMessage, ex);
    }

    private static SQLException findSqlException(Throwable t)
    {
        while (t != null)
        {
            if (t instanceof SQLException sqlException)
            {
                return sqlException;
            }
            t = t.getCause();
        }
        return null;
    }

    private static boolean isConstraintViolation(DatabaseEngine engine, SQLException ex)
    {
        // ANSI SQL standard: class "23" = Integrity Constraint Violation.
        // Covers Postgres (23503 FK, 23505 unique), HSQLDB (23000, 23505),
        // H2 (23502, 23505), MySQL/ANSI (23000), SQLite (SQLITE_CONSTRAINT).
        String sqlState = ex.getSQLState();
        if (sqlState != null && sqlState.startsWith("23"))
        {
            return true;
        }
        // Oracle's JDBC driver may not always set standard SQLState; fall back
        // to vendor-specific error codes.
        if (engine == DatabaseEngine.ORACLE)
        {
            int errorCode = ex.getErrorCode();
            // ORA-02292: integrity constraint violated - child record found
            // ORA-02291: integrity constraint violated - parent key not found
            if (errorCode == 2292 || errorCode == 2291)
            {
                return true;
            }
        }
        return false;
    }
}
