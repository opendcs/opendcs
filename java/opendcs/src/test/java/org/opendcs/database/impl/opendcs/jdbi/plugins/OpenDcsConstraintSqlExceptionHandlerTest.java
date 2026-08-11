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

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.exceptions.data.OpenDcsConstraintException;
import org.opendcs.database.api.exceptions.data.RelatedDataConstraintException;
import org.opendcs.database.api.exceptions.data.UniqueConstraintViolationException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class OpenDcsConstraintSqlExceptionHandlerTest
{
	private final OpenDcsConstraintSqlExceptionHandler handler = new OpenDcsConstraintSqlExceptionHandler();

	@Test
	void testForeignKeyViolationIsRelatedDataConstraintException()
	{
		SQLException sqlEx = new SQLException("violates foreign key constraint", "23503");

		RelatedDataConstraintException result = assertThrows(RelatedDataConstraintException.class,
				() -> handler.handle(sqlEx, null));

		assertSame(sqlEx, result.getCause());
	}

	@Test
	void testUniqueViolationIsUniqueConstraintViolationException()
	{
		SQLException sqlEx = new SQLException("duplicate key value", "23505");

		assertThrows(UniqueConstraintViolationException.class, () -> handler.handle(sqlEx, null));
	}

	@Test
	void testOtherConstraintSqlStatesAreGenericConstraintException()
	{
		assertThrows(OpenDcsConstraintException.class,
				() -> handler.handle(new SQLException("m", "23000"), null));
		assertThrows(OpenDcsConstraintException.class,
				() -> handler.handle(new SQLException("m", "23502"), null));
	}

	@Test
	void testNonConstraintSqlStateDoesNotThrow()
	{
		SQLException sqlEx = new SQLException("connection failure", "08001");

		assertDoesNotThrow(() -> handler.handle(sqlEx, null));
	}

	@Test
	void testNullSqlStateDoesNotThrow()
	{
		SQLException sqlEx = new SQLException("unknown", (String) null);

		assertDoesNotThrow(() -> handler.handle(sqlEx, null));
	}

	@Test
	void testVendorErrorCodeAloneIsNotEnoughWithoutSqlState()
	{
		// This handler is engine-agnostic; a vendor-specific error code (e.g. Oracle's
		// ORA-02292) with no ANSI SQLState set must be left for a per-implementation
		// handler to recognize, not guessed at here.
		SQLException sqlEx = new SQLException("child record found", null, 2292);

		assertDoesNotThrow(() -> handler.handle(sqlEx, null));
	}
}
