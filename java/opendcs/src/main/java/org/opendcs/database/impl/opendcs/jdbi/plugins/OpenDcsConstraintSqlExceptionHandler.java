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
package org.opendcs.database.impl.opendcs.jdbi.plugins;

import java.sql.SQLException;

import org.jdbi.v3.core.statement.SqlExceptionHandler;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.exceptions.data.OpenDcsConstraintException;
import org.opendcs.database.api.exceptions.data.RelatedDataConstraintException;
import org.opendcs.database.api.exceptions.data.UniqueConstraintViolationException;

/**
 * Detects integrity constraint violations (foreign key, unique, check, not-null)
 * reported by the JDBC driver and rethrows them as the appropriate unchecked
 * {@link OpenDcsConstraintException} subtype so callers can return correct HTTP
 * status codes without inspecting raw SQLExceptions.
 *
 * Detected via ANSI SQLState class "23" (Postgres, HSQLDB, H2, MySQL, SQLite), or
 * by Oracle vendor error code as a fallback when the driver doesn't set SQLState.
 *
 * If the exception isn't recognized as a constraint violation, this handler
 * returns without throwing so the next handler in the chain (ultimately
 * {@link OpenDcsBaseSqlExceptionHandler}) can deal with it.
 */
public final class OpenDcsConstraintSqlExceptionHandler implements SqlExceptionHandler
{
    private final DatabaseEngine engine;

    public OpenDcsConstraintSqlExceptionHandler(DatabaseEngine engine)
    {
        this.engine = engine;
    }

    @Override
    public void handle(SQLException ex, StatementContext ctx)
    {
        String sqlState = ex.getSQLState();
        if (sqlState != null && sqlState.startsWith("23"))
        {
            throwForSqlState(sqlState, ex);
        }
        // Oracle's JDBC driver may not always set standard SQLState; fall back
        // to vendor-specific error codes.
        if (engine == DatabaseEngine.ORACLE)
        {
            throwForOracleErrorCode(ex.getErrorCode(), ex);
        }
        // Not a constraint violation we recognize; let the next handler in the
        // chain decide what to do with it.
    }

    private static void throwForSqlState(String sqlState, SQLException ex)
    {
        switch (sqlState)
        {
            // Postgres/H2/HSQLDB unique_violation
            case "23505": throw new UniqueConstraintViolationException(ex);
            // Postgres foreign_key_violation
            case "23503": throw new RelatedDataConstraintException(ex);
            // Other class "23" integrity constraint violations (not-null, check, etc.)
            default: throw new OpenDcsConstraintException(ex);
        }
    }

    private static void throwForOracleErrorCode(int errorCode, SQLException ex)
    {
        switch (errorCode)
        {
            // ORA-00001: unique constraint violated
            case 1: throw new UniqueConstraintViolationException(ex);
            // ORA-02292: integrity constraint violated - child record found
            // ORA-02291: integrity constraint violated - parent key not found
            case 2292:
            case 2291: throw new RelatedDataConstraintException(ex);
            default: // not a constraint code we recognize
        }
    }
}
