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
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.exceptions.data.OpenDcsConstraintException;
import org.opendcs.database.api.exceptions.data.RelatedDataConstraintException;
import org.opendcs.database.api.exceptions.data.UniqueConstraintViolationException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class OpenDcsConstraintSqlExceptionHandlerTest
{
	@Test
	void testPostgresForeignKeyViolationIsRelatedDataConstraintException()
	{
		var handler = new OpenDcsConstraintSqlExceptionHandler(DatabaseEngine.POSTGRES);
		SQLException sqlEx = new SQLException("violates foreign key constraint", "23503");

		RelatedDataConstraintException result = assertThrows(RelatedDataConstraintException.class,
				() -> handler.handle(sqlEx, null));

		assertSame(sqlEx, result.getCause());
	}

	@Test
	void testPostgresUniqueViolationIsUniqueConstraintViolationException()
	{
		var handler = new OpenDcsConstraintSqlExceptionHandler(DatabaseEngine.POSTGRES);
		SQLException sqlEx = new SQLException("duplicate key value", "23505");

		assertThrows(UniqueConstraintViolationException.class, () -> handler.handle(sqlEx, null));
	}

	@Test
	void testHsqldbAndH2OtherConstraintSqlStatesAreGenericConstraintException()
	{
		var hsqldb = new OpenDcsConstraintSqlExceptionHandler(DatabaseEngine.HSQLDB);
		var h2 = new OpenDcsConstraintSqlExceptionHandler(DatabaseEngine.H2);

		assertThrows(OpenDcsConstraintException.class,
				() -> hsqldb.handle(new SQLException("m", "23000"), null));
		assertThrows(OpenDcsConstraintException.class,
				() -> h2.handle(new SQLException("m", "23502"), null));
	}

	@Test
	void testNonConstraintSqlStateDoesNotThrow()
	{
		var handler = new OpenDcsConstraintSqlExceptionHandler(DatabaseEngine.POSTGRES);
		SQLException sqlEx = new SQLException("connection failure", "08001");

		assertDoesNotThrow(() -> handler.handle(sqlEx, null));
	}

	@Test
	void testNullSqlStateDoesNotThrowForNonOracleEngine()
	{
		var handler = new OpenDcsConstraintSqlExceptionHandler(DatabaseEngine.GENERIC_SQL);
		SQLException sqlEx = new SQLException("unknown", (String) null);

		assertDoesNotThrow(() -> handler.handle(sqlEx, null));
	}

	@Test
	void testOracleUniqueConstraintErrorCodeIsUniqueConstraintViolationException()
	{
		var handler = new OpenDcsConstraintSqlExceptionHandler(DatabaseEngine.ORACLE);
		SQLException sqlEx = new SQLException("ORA-00001: unique constraint violated", null, 1);

		assertThrows(UniqueConstraintViolationException.class, () -> handler.handle(sqlEx, null));
	}

	@Test
	void testOracleChildRecordFoundErrorCodeIsRelatedDataConstraintException()
	{
		var handler = new OpenDcsConstraintSqlExceptionHandler(DatabaseEngine.ORACLE);
		SQLException sqlEx = new SQLException("ORA-02292: integrity constraint violated - child record found",
				null, 2292);

		assertThrows(RelatedDataConstraintException.class, () -> handler.handle(sqlEx, null));
	}

	@Test
	void testOracleParentKeyNotFoundErrorCodeIsRelatedDataConstraintException()
	{
		var handler = new OpenDcsConstraintSqlExceptionHandler(DatabaseEngine.ORACLE);
		SQLException sqlEx = new SQLException("ORA-02291: integrity constraint violated - parent key not found",
				null, 2291);

		assertThrows(RelatedDataConstraintException.class, () -> handler.handle(sqlEx, null));
	}

	@Test
	void testOracleUnrelatedErrorCodeDoesNotThrow()
	{
		var handler = new OpenDcsConstraintSqlExceptionHandler(DatabaseEngine.ORACLE);
		SQLException sqlEx = new SQLException("some other error", null, 12345);

		assertDoesNotThrow(() -> handler.handle(sqlEx, null));
	}

	@Test
	void testOracleErrorCodeIgnoredForNonOracleEngine()
	{
		// same vendor code as an Oracle FK violation, but a non-Oracle engine should
		// not fall back to vendor-code matching
		var handler = new OpenDcsConstraintSqlExceptionHandler(DatabaseEngine.POSTGRES);
		SQLException sqlEx = new SQLException("unrelated", null, 2292);

		assertDoesNotThrow(() -> handler.handle(sqlEx, null));
	}
}
